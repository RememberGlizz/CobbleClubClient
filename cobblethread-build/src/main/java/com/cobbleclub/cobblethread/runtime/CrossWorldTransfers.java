package com.cobbleclub.cobblethread.runtime;

import com.cobbleclub.cobblethread.CobbleThread;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;

/** Two-phase hand-off for dimension changes initiated during an owned world tick. */
public final class CrossWorldTransfers {
    private CrossWorldTransfers() {}

    /**
     * @return true when the current call was deferred and its caller should
     *         cancel the original changeDimension invocation for this tick.
     */
    public static boolean deferIfNeeded(Entity entity, DimensionTransition transition) {
        MinecraftServer server = transition.newLevel().getServer();
        WorldScheduler scheduler = CobbleThread.RUNTIME.scheduler(server);
        if (scheduler == null || !scheduler.isWorkerThread()) return false;

        ResourceKey<Level> source = entity.level().dimension();
        ResourceKey<Level> destination = transition.newLevel().dimension();
        if (source.equals(destination)) return false;

        // A worker never blocks waiting for the server thread: the server is
        // waiting at the world barrier. Queue a second phase instead.
        scheduler.deferTransfer(() -> {
            // Another server-lane action may already have removed or moved it.
            if (entity.isRemoved() || !entity.level().dimension().equals(source)) return;
            entity.changeDimension(transition);
        });
        return true;
    }
}
