/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.pokemon.Pokemon
 *  com.google.gson.JsonParser
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 *  net.minecraft.class_3302$class_4045
 *  net.minecraft.class_3695
 *  org.jetbrains.annotations.NotNull
 */
package com.cobbleclub.client.rp;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.sizer.LayerCodec;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

@Environment(value=EnvType.CLIENT)
public class LayerDataLoader
implements IdentifiableResourceReloadListener {
    public static final HashMap<String, SizerRegStruct> LAYER_REGISTRY = new HashMap();
    private static final String DIRECTORY = "pl_sizer";

    public static LayerCodec.Settings getSettings(Pokemon pokemon, String aspect) {
        String name = pokemon.getSpecies().getResourceIdentifier().getPath();
        String formName = pokemon.getForm().getName();
        SizerRegStruct reg = LAYER_REGISTRY.get(name);
        return reg == null ? null : reg.getSettings(formName, aspect);
    }

    public void load() {
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        LAYER_REGISTRY.clear();
        for (Identifier id : rm.findResources(DIRECTORY, path -> path.getPath().endsWith(".json")).keySet()) {
            try {
                InputStream stream = ((Resource)rm.getResource(id).get()).getInputStream();
                try {
                    LayerCodec codec = (LayerCodec)LayerCodec.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)JsonParser.parseReader((Reader)new InputStreamReader(stream))).result().orElseThrow();
                    LAYER_REGISTRY.putIfAbsent(codec.pokemon(), new SizerRegStruct());
                    SizerRegStruct sizerRegStruct = LAYER_REGISTRY.get(codec.pokemon());
                    for (String form : codec.size_config().keySet()) {
                        for (Map.Entry<String, LayerCodec.Settings> aspectSetting : codec.size_config().get(form).entrySet()) {
                            sizerRegStruct.addForm(form, aspectSetting.getKey(), aspectSetting.getValue());
                        }
                    }
                }
                finally {
                    if (stream == null) continue;
                    stream.close();
                }
            }
            catch (Exception e) {
                CobbleClubClient.LOGGER.error("Failed loading layer JSON: {}", (Object)id, (Object)e);
            }
        }
        CobbleClubClient.LOGGER.info("Loaded {} custom layer data", (Object)LAYER_REGISTRY.size());
    }

    @NotNull
    public CompletableFuture<Void> reload(ResourceReloader.Synchronizer preparationBarrier, ResourceManager resourceManager, Profiler preparationsProfiler, Profiler applyProfiler, Executor preparationExecutor, Executor applyExecutor) {
        CompletableFuture<Void> var10000 = CompletableFuture.runAsync(this::load, preparationExecutor);
        Objects.requireNonNull(preparationBarrier);
        return ((CompletableFuture)var10000.thenCompose(arg_0 -> ((ResourceReloader.Synchronizer)preparationBarrier).whenPrepared(arg_0))).thenRunAsync(() -> {}, applyExecutor);
    }

    @NotNull
    public String getName() {
        return "cobbleclub";
    }

    public Identifier getFabricId() {
        return Identifier.of((String)"cobbleclub", (String)"layer_data");
    }

    @Environment(value=EnvType.CLIENT)
    public static class SizerRegStruct {
        private final HashMap<String, HashMap<String, LayerCodec.Settings>> settingsHashMap = new HashMap();

        public void addForm(String formName, String aspect, LayerCodec.Settings settings) {
            HashMap<String, LayerCodec.Settings> map = this.settingsHashMap.getOrDefault(formName, new HashMap());
            map.put(aspect, settings);
            this.settingsHashMap.putIfAbsent(formName, map);
        }

        public LayerCodec.Settings getSettings(String formName, String aspect) {
            return (LayerCodec.Settings)this.settingsHashMap.getOrDefault(formName, new HashMap()).get(aspect);
        }
    }
}

