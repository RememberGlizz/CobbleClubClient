/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.cobbleclub.client.ui;

import com.cobbleclub.client.CobbleClubClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(value=EnvType.CLIENT)
public final class ClientJsonConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ClientJsonConfig() {
    }

    public static Path file(String name) {
        return FabricLoader.getInstance().getConfigDir().resolve(name);
    }

    public static <T> T load(Path file, Class<T> type, Supplier<T> defaults) {
        if (Files.exists(file, new LinkOption[0])) {
            try {
                Object var5;
                block10: {
                    block9: {
                        try (BufferedReader reader = Files.newBufferedReader(file);){
                            Object loaded = GSON.fromJson((Reader)reader, type);
                            if (loaded == null) break block9;
                            var5 = loaded;
                            break block10;
                        }
                    }
                    return defaults.get();
                }
                return (T)var5;
            }
            catch (Exception e) {
                CobbleClubClient.LOGGER.warn("Failed to read {}, using defaults", (Object)file.getFileName(), (Object)e);
                return defaults.get();
            }
        }
        T fresh = defaults.get();
        ClientJsonConfig.save(file, fresh);
        return fresh;
    }

    public static void save(Path file, Object instance) {
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(file, new OpenOption[0]);){
                GSON.toJson(instance, (Appendable)writer);
            }
        }
        catch (Exception e) {
            CobbleClubClient.LOGGER.warn("Failed to write {}", (Object)file.getFileName(), (Object)e);
        }
    }
}

