/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParser
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 *  net.minecraft.class_3302$class_4045
 *  net.minecraft.class_3695
 *  net.minecraft.class_5944
 *  org.jetbrains.annotations.NotNull
 */
package com.cobbleclub.client.render;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.render.renderTypes.CobbleClubRenderTypes;
import com.google.gson.JsonParser;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.client.gl.ShaderProgram;
import org.jetbrains.annotations.NotNull;

@Environment(value=EnvType.CLIENT)
public class TeraMapLoader
implements IdentifiableResourceReloadListener {
    public static final HashMap<String, String> REGISTRY = new HashMap();
    private static final String DIRECTORY = "cobbleclub/tera_map";

    public void load() {
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        REGISTRY.clear();
        for (Identifier id : rm.findResources(DIRECTORY, path -> path.getPath().endsWith(".json")).keySet()) {
            try {
                InputStream stream = ((Resource)rm.getResource(id).get()).getInputStream();
                try {
                    TeraMap codec = (TeraMap)TeraMap.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)JsonParser.parseReader((Reader)new InputStreamReader(stream))).result().orElseThrow();
                    REGISTRY.putAll(codec.colorMap);
                }
                finally {
                    if (stream == null) continue;
                    stream.close();
                }
            }
            catch (Exception e) {
                CobbleClubClient.LOGGER.error("Failed loading tera map JSON: {}", (Object)id, (Object)e);
            }
        }
        CobbleClubClient.LOGGER.info("Loaded {} custom tera map", (Object)REGISTRY.size());
    }

    public static ShaderProgram getColorShaderMap(String color) {
        return switch (color) {
            case "red" -> CobbleClubRenderTypes.teraFire;
            case "blue" -> CobbleClubRenderTypes.teraWater;
            case "green" -> CobbleClubRenderTypes.teraGrass;
            case "yellow" -> CobbleClubRenderTypes.teraElectric;
            case "brown" -> CobbleClubRenderTypes.teraGround;
            case "light_blue" -> CobbleClubRenderTypes.teraFlying;
            case "purple" -> CobbleClubRenderTypes.teraDragon;
            case "pink" -> CobbleClubRenderTypes.teraFairy;
            case "black" -> CobbleClubRenderTypes.teraDark;
            case "gray" -> CobbleClubRenderTypes.teraSteel;
            case "light_grey" -> CobbleClubRenderTypes.teraIce;
            case "orange" -> CobbleClubRenderTypes.teraFighting;
            case "lime" -> CobbleClubRenderTypes.teraBug;
            case "teal" -> CobbleClubRenderTypes.teraPoison;
            case "indigo" -> CobbleClubRenderTypes.teraGhost;
            case "magenta" -> CobbleClubRenderTypes.teraPsychic;
            case "tan" -> CobbleClubRenderTypes.teraRock;
            case "navy" -> CobbleClubRenderTypes.teraNormal;
            case "white" -> CobbleClubRenderTypes.teraStellar;
            default -> {
                CobbleClubClient.LOGGER.error("Unknown tera shader color '{}'", (Object)color);
                yield CobbleClubRenderTypes.teraStellar;
            }
        };
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
        return Identifier.of((String)"cobbleclub", (String)"tera_map_loader");
    }

    @Environment(value=EnvType.CLIENT)
    public record TeraMap(Map<String, String> colorMap) {
        public static Codec<TeraMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("aspectShaderMap").forGetter(TeraMap::colorMap)).apply(instance, TeraMap::new));
    }
}

