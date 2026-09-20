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
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 *  net.minecraft.class_3302$class_4045
 *  net.minecraft.class_3695
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.cobbleclub.client.render;

import com.cobbleclub.client.CobbleClubClient;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ArmorEquipmentLoader
implements IdentifiableResourceReloadListener {
    private static volatile Map<Key, Entry> registry = Map.of();
    private static final String EQUIPMENT_DIR = "equipment";
    private static final String BRIDGE_DIR = "armor_bridge";

    @Nullable
    public static Entry lookup(Identifier item, int customModelData) {
        return registry.get(new Key(item, customModelData));
    }

    public void load(ResourceManager rm) {
        HashMap assets = new HashMap();
        rm.findResources(EQUIPMENT_DIR, path -> path.getPath().endsWith(".json")).keySet().forEach(id -> {
            try (InputStream stream = ((Resource)rm.getResource(id).orElseThrow()).getInputStream();){
                EquipmentJson json = (EquipmentJson)EquipmentJson.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)JsonParser.parseReader((Reader)new InputStreamReader(stream))).result().orElseThrow();
                assets.put(ArmorEquipmentLoader.assetId(id), new Entry(ArmorEquipmentLoader.resolve(rm, json, "humanoid"), ArmorEquipmentLoader.resolve(rm, json, "humanoid_leggings")));
            }
            catch (Exception e) {
                CobbleClubClient.LOGGER.error("Failed loading armor equipment JSON: {}", id, (Object)e);
            }
        });
        HashMap next = new HashMap();
        rm.findResources(BRIDGE_DIR, path -> path.getPath().endsWith(".json")).keySet().forEach(id -> {
            try (InputStream stream = ((Resource)rm.getResource(id).orElseThrow()).getInputStream();){
                BridgeJson json = (BridgeJson)BridgeJson.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)JsonParser.parseReader((Reader)new InputStreamReader(stream))).result().orElseThrow();
                json.bridge().forEach((itemStr, cmdMap) -> {
                    Identifier item = Identifier.tryParse((String)itemStr);
                    if (item == null) {
                        CobbleClubClient.LOGGER.warn("Armor bridge item id '{}' is invalid", itemStr);
                    } else {
                        cmdMap.forEach((cmd, assetId) -> {
                            Entry entry = (Entry)assets.get(assetId);
                            if (entry == null) {
                                CobbleClubClient.LOGGER.warn("Armor bridge {} cmd {} references unknown equipment asset '{}'", new Object[]{itemStr, cmd, assetId});
                            } else {
                                try {
                                    next.put(new Key(item, Integer.parseInt(cmd.trim())), entry);
                                }
                                catch (NumberFormatException var8) {
                                    CobbleClubClient.LOGGER.warn("Armor bridge key '{}' is not an integer", cmd);
                                }
                            }
                        });
                    }
                });
            }
            catch (Exception e) {
                CobbleClubClient.LOGGER.error("Failed loading armor bridge JSON: {}", id, (Object)e);
            }
        });
        registry = Map.copyOf(next);
        CobbleClubClient.LOGGER.info("Loaded {} custom armor mappings", (Object)registry.size());
    }

    private static String assetId(Identifier file) {
        String path = file.getPath();
        path = path.substring(EQUIPMENT_DIR.length() + 1, path.length() - ".json".length());
        String var10000 = file.getNamespace();
        return var10000 + ":" + path;
    }

    @Nullable
    private static Identifier resolve(ResourceManager rm, EquipmentJson json, String layerType) {
        List<LayerJson> layers = json.layers().get(layerType);
        if (layers != null && !layers.isEmpty()) {
            Identifier texture = layers.get(0).texture();
            Identifier path = Identifier.of((String)texture.getNamespace(), (String)("textures/entity/equipment/" + layerType + "/" + texture.getPath() + ".png"));
            return rm.getResource(path).isPresent() ? path : null;
        }
        return null;
    }

    @NotNull
    public CompletableFuture<Void> reload(ResourceReloader.Synchronizer preparationBarrier, ResourceManager resourceManager, Profiler preparationsProfiler, Profiler applyProfiler, Executor preparationExecutor, Executor applyExecutor) {
        CompletableFuture<Void> var10000 = CompletableFuture.runAsync(() -> this.load(resourceManager), preparationExecutor);
        Objects.requireNonNull(preparationBarrier);
        return var10000.thenCompose(arg_0 -> ((ResourceReloader.Synchronizer)preparationBarrier).whenPrepared(arg_0));
    }

    @NotNull
    public String getName() {
        return "cobbleclub";
    }

    public Identifier getFabricId() {
        return Identifier.of((String)"cobbleclub", (String)"armor_equipment_loader");
    }

    @Environment(value=EnvType.CLIENT)
    public record Key(Identifier item, int customModelData) {
    }

    @Environment(value=EnvType.CLIENT)
    public record Entry(@Nullable Identifier body, @Nullable Identifier leggings) {
    }

    @Environment(value=EnvType.CLIENT)
    private record EquipmentJson(Map<String, List<LayerJson>> layers) {
        static final Codec<EquipmentJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.unboundedMap(Codec.STRING, LayerJson.CODEC.listOf()).fieldOf("layers").forGetter(EquipmentJson::layers)).apply(instance, EquipmentJson::new));
    }

    @Environment(value=EnvType.CLIENT)
    private record LayerJson(Identifier texture) {
        static final Codec<LayerJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(Identifier.CODEC.fieldOf("texture").forGetter(LayerJson::texture)).apply(instance, LayerJson::new));
    }

    @Environment(value=EnvType.CLIENT)
    private record BridgeJson(Map<String, Map<String, String>> bridge) {
        static final Codec<BridgeJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING)).fieldOf("bridge").forGetter(BridgeJson::bridge)).apply(instance, BridgeJson::new));
    }
}

