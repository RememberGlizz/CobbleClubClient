package com.cobbleclub.cobblethread.config;

import com.cobbleclub.cobblethread.CobbleThread;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record CobbleThreadConfig(boolean enabled, int workers, boolean strictOwnership) {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cobblethread.json");

    public static CobbleThreadConfig load() {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        CobbleThreadConfig defaults = new CobbleThreadConfig(true, defaultWorkers(), true);

        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                CobbleThreadConfig loaded = gson.fromJson(reader, CobbleThreadConfig.class);
                if (loaded != null && loaded.workers > 0) return loaded;
            } catch (Exception exception) {
                CobbleThread.LOGGER.error("Could not read {}", PATH, exception);
            }
        }

        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                gson.toJson(defaults, writer);
            }
        } catch (IOException exception) {
            CobbleThread.LOGGER.error("Could not write default {}", PATH, exception);
        }
        return defaults;
    }

    public int effectiveWorkers() {
        return Math.max(1, Math.min(workers, Math.max(1, Runtime.getRuntime().availableProcessors() - 1)));
    }

    private static int defaultWorkers() {
        return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    }
}
