package com.cobbleclub.cobblethread.runtime;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class WorldOwnership {
    private static final ThreadLocal<ResourceKey<Level>> CURRENT_WORLD = new ThreadLocal<>();

    private WorldOwnership() {}

    static void enter(ResourceKey<Level> world) {
        if (CURRENT_WORLD.get() != null) {
            throw new IllegalStateException("CobbleThread worker already owns " + CURRENT_WORLD.get().location());
        }
        CURRENT_WORLD.set(world);
    }

    static void exit(ResourceKey<Level> world) {
        ResourceKey<Level> current = CURRENT_WORLD.get();
        if (!world.equals(current)) {
            throw new IllegalStateException("CobbleThread ownership exit mismatch: expected "
                + world.location() + ", found " + (current == null ? "none" : current.location()));
        }
        CURRENT_WORLD.remove();
    }

    public static ResourceKey<Level> currentWorld() {
        return CURRENT_WORLD.get();
    }

    public static boolean isWorldWorker() {
        return CURRENT_WORLD.get() != null;
    }

    public static boolean owns(ResourceKey<Level> world) {
        return world.equals(CURRENT_WORLD.get());
    }

    public static void require(ResourceKey<Level> world) {
        ResourceKey<Level> current = CURRENT_WORLD.get();
        if (current != null && !current.equals(world)) {
            throw new IllegalStateException("Illegal cross-world access: " + current.location()
                + " worker attempted to mutate " + world.location());
        }
    }
}
