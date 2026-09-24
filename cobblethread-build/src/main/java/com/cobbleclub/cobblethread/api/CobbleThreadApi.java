package com.cobbleclub.cobblethread.api;

import com.cobbleclub.cobblethread.CobbleThread;
import com.cobbleclub.cobblethread.runtime.WorldOwnership;
import com.cobbleclub.cobblethread.runtime.WorldScheduler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Public hooks used by CobbleThread compatibility adapters. */
public final class CobbleThreadApi {
    private CobbleThreadApi() {}

    public static ResourceKey<Level> currentOwnedWorld() {
        return WorldOwnership.currentWorld();
    }

    public static void requireOwnership(ServerLevel world) {
        WorldScheduler scheduler = CobbleThread.RUNTIME.scheduler(world.getServer());
        if (scheduler != null && scheduler.strictOwnership()) {
            WorldOwnership.require(world.dimension());
        }
    }

    public static void deferToCompatibilityLane(MinecraftServer server, Runnable action) {
        WorldScheduler scheduler = CobbleThread.RUNTIME.scheduler(server);
        if (scheduler == null || !scheduler.isWorkerThread()) {
            action.run();
        } else {
            scheduler.deferToServer(action);
        }
    }
}
