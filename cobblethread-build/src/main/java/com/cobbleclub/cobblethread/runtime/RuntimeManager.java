package com.cobbleclub.cobblethread.runtime;

import com.cobbleclub.cobblethread.CobbleThread;
import com.cobbleclub.cobblethread.config.CobbleThreadConfig;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimeManager {
    private final CobbleThreadConfig config;
    private final Map<MinecraftServer, WorldScheduler> schedulers = new ConcurrentHashMap<>();

    public RuntimeManager(CobbleThreadConfig config) {
        this.config = config;
    }

    public void start(MinecraftServer server) {
        if (!config.enabled()) return;
        WorldScheduler scheduler = new WorldScheduler(server, config.effectiveWorkers(), config.strictOwnership());
        WorldScheduler previous = schedulers.putIfAbsent(server, scheduler);
        if (previous == null) {
            CobbleThread.LOGGER.info("Started CobbleThread with {} stable world workers", scheduler.workerCount());
        } else {
            scheduler.close();
        }
    }

    public void stop(MinecraftServer server) {
        WorldScheduler scheduler = schedulers.remove(server);
        if (scheduler != null) scheduler.close();
    }

    public WorldScheduler scheduler(MinecraftServer server) {
        return schedulers.get(server);
    }

    public boolean active(MinecraftServer server) {
        return config.enabled() && schedulers.containsKey(server);
    }
}
