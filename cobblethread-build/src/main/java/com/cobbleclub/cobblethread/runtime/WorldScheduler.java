package com.cobbleclub.cobblethread.runtime;

import com.cobbleclub.cobblethread.CobbleThread;
import com.cobbleclub.cobblethread.mixin.LevelThreadAccess;
import com.cobbleclub.cobblethread.mixin.ServerChunkCacheThreadAccess;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class WorldScheduler implements AutoCloseable {
    private final MinecraftServer server;
    private final ExecutorService[] workers;
    private final StableLaneAssignments<ResourceKey<Level>> assignments;
    private final ConcurrentLinkedQueue<Runnable> compatibilityLane = new ConcurrentLinkedQueue<>();
    private final AtomicInteger compatibilityQueueDepth = new AtomicInteger();
    private final AtomicLong completedTicks = new AtomicLong();
    private final AtomicLong deferredTransfers = new AtomicLong();
    private final AtomicLong completedTransfers = new AtomicLong();
    private final AtomicLong worldTickSamples = new AtomicLong();
    private final AtomicLong worldTickNanos = new AtomicLong();
    private final AtomicLong maxWorldTickNanos = new AtomicLong();
    private final boolean strictOwnership;

    public WorldScheduler(MinecraftServer server, int workerCount, boolean strictOwnership) {
        this.server = server;
        this.strictOwnership = strictOwnership;
        this.workers = new ExecutorService[workerCount];
        this.assignments = new StableLaneAssignments<>(workerCount);
        for (int lane = 0; lane < workerCount; lane++) {
            int laneId = lane;
            ThreadFactory factory = task -> {
                Thread thread = new Thread(task, "cobblethread-world-" + laneId);
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler((failedThread, error) ->
                    CobbleThread.LOGGER.error("Uncaught exception on {}", failedThread.getName(), error));
                return thread;
            };
            workers[lane] = Executors.newSingleThreadExecutor(factory);
        }
    }

    public void tickWorlds(Iterable<ServerLevel> iterable, BooleanSupplier shouldKeepTicking,
                           int tickCount, PlayerList playerList) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("CobbleThread scheduling must begin on the server thread");
        }

        List<ServerLevel> levels = new ArrayList<>();
        iterable.forEach(levels::add);
        if (levels.isEmpty()) return;

        if (tickCount % 20 == 0) {
            for (ServerLevel level : levels) {
                ClientboundSetTimePacket packet = new ClientboundSetTimePacket(
                    level.getGameTime(), level.getDayTime(),
                    level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));
                playerList.broadcastAll(packet, level.dimension());
            }
        }

        CountDownLatch barrier = new CountDownLatch(levels.size());
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (ServerLevel level : levels) {
            int lane = assignments.laneFor(level.dimension());
            try {
                workers[lane].execute(() -> {
                    try {
                        if (failure.get() == null) tickOne(level, shouldKeepTicking);
                    } catch (Throwable throwable) {
                        failure.compareAndSet(null, throwable);
                    } finally {
                        barrier.countDown();
                    }
                });
            } catch (RejectedExecutionException rejected) {
                failure.compareAndSet(null, rejected);
                barrier.countDown();
            }
        }

        await(barrier);
        drainCompatibilityLane(failure);
        Throwable throwable = failure.get();
        if (throwable != null) {
            throw new RuntimeException("CobbleThread failed while ticking an owned world", throwable);
        }
        completedTicks.incrementAndGet();
    }

    private void tickOne(ServerLevel level, BooleanSupplier shouldKeepTicking) {
        Thread owner = Thread.currentThread();
        Thread oldLevelThread = ((LevelThreadAccess) level).cobblethread$getThread();
        Thread oldChunkThread = ((ServerChunkCacheThreadAccess) level.getChunkSource()).cobblethread$getMainThread();
        long started = System.nanoTime();

        WorldOwnership.enter(level.dimension());
        try {
            ((LevelThreadAccess) level).cobblethread$setThread(owner);
            ((ServerChunkCacheThreadAccess) level.getChunkSource()).cobblethread$setMainThread(owner);
            level.tick(shouldKeepTicking);
        } finally {
            try {
                ((ServerChunkCacheThreadAccess) level.getChunkSource()).cobblethread$setMainThread(oldChunkThread);
            } finally {
                try {
                    ((LevelThreadAccess) level).cobblethread$setThread(oldLevelThread);
                } finally {
                    WorldOwnership.exit(level.dimension());
                    long elapsed = System.nanoTime() - started;
                    worldTickSamples.incrementAndGet();
                    worldTickNanos.addAndGet(elapsed);
                    maxWorldTickNanos.accumulateAndGet(elapsed, Math::max);
                }
            }
        }
    }

    public boolean isWorkerThread() {
        return WorldOwnership.isWorldWorker();
    }

    public void deferToServer(Runnable action) {
        enqueueCompatibility(action);
    }

    public void deferTransfer(Runnable action) {
        deferredTransfers.incrementAndGet();
        enqueueCompatibility(() -> {
            action.run();
            completedTransfers.incrementAndGet();
        });
    }

    private void enqueueCompatibility(Runnable action) {
        compatibilityQueueDepth.incrementAndGet();
        compatibilityLane.add(action);
    }

    public int workerCount() {
        return workers.length;
    }

    public int assignmentCount() {
        return assignments.size();
    }

    public long completedTicks() {
        return completedTicks.get();
    }

    public int compatibilityQueueDepth() {
        return compatibilityQueueDepth.get();
    }

    public long deferredTransfers() {
        return deferredTransfers.get();
    }

    public long completedTransfers() {
        return completedTransfers.get();
    }

    public double averageWorldTickMillis() {
        long samples = worldTickSamples.get();
        return samples == 0 ? 0.0 : (worldTickNanos.get() / 1_000_000.0) / samples;
    }

    public double maxWorldTickMillis() {
        return maxWorldTickNanos.get() / 1_000_000.0;
    }

    public boolean strictOwnership() {
        return strictOwnership;
    }

    private static void await(CountDownLatch barrier) {
        boolean interrupted = false;
        while (true) {
            try {
                barrier.await();
                break;
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private void drainCompatibilityLane(AtomicReference<Throwable> failure) {
        Runnable action;
        while ((action = compatibilityLane.poll()) != null) {
            compatibilityQueueDepth.decrementAndGet();
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        }
    }

    @Override
    public void close() {
        for (ExecutorService worker : workers) worker.shutdown();
        for (ExecutorService worker : workers) {
            try {
                if (!worker.awaitTermination(10, TimeUnit.SECONDS)) worker.shutdownNow();
            } catch (InterruptedException exception) {
                worker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        assignments.clear();
        compatibilityLane.clear();
        compatibilityQueueDepth.set(0);
    }
}
