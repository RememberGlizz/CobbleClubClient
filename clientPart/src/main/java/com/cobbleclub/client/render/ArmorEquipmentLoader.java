package com.cobbleclub.client.render;

import com.cobbleclub.client.CobbleClubClient;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_3302;
import net.minecraft.class_3695;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ArmorEquipmentLoader implements IdentifiableResourceReloadListener {
   private static volatile Map<Key, Entry> registry = Map.of();
   private static final String EQUIPMENT_DIR = "equipment";
   private static final String BRIDGE_DIR = "armor_bridge";

   public static @Nullable Entry lookup(class_2960 item, int customModelData) {
      return (Entry)registry.get(new Key(item, customModelData));
   }

   public void load(class_3300 rm) {
      Map<String, Entry> assets = new HashMap();
      rm.method_14488("equipment", (path) -> path.method_12832().endsWith(".json")).keySet().forEach((id) -> {
         try {
            InputStream stream = ((class_3298)rm.method_14486(id).orElseThrow()).method_14482();

            try {
               EquipmentJson json = (EquipmentJson)ArmorEquipmentLoader.EquipmentJson.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(stream))).result().orElseThrow();
               assets.put(assetId(id), new Entry(resolve(rm, json, "humanoid"), resolve(rm, json, "humanoid_leggings")));
            } catch (Throwable var7) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception e) {
            CobbleClubClient.LOGGER.error("Failed loading armor equipment JSON: {}", id, e);
         }

      });
      Map<Key, Entry> next = new HashMap();
      rm.method_14488("armor_bridge", (path) -> path.method_12832().endsWith(".json")).keySet().forEach((id) -> {
         try {
            InputStream stream = ((class_3298)rm.method_14486(id).orElseThrow()).method_14482();

            try {
               BridgeJson json = (BridgeJson)ArmorEquipmentLoader.BridgeJson.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(stream))).result().orElseThrow();
               json.bridge().forEach((itemStr, cmdMap) -> {
                  class_2960 item = class_2960.method_12829(itemStr);
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
                           } catch (NumberFormatException var8) {
                              CobbleClubClient.LOGGER.warn("Armor bridge key '{}' is not an integer", cmd);
                           }

                        }
                     });
                  }
               });
            } catch (Throwable var8) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception e) {
            CobbleClubClient.LOGGER.error("Failed loading armor bridge JSON: {}", id, e);
         }

      });
      registry = Map.copyOf(next);
      CobbleClubClient.LOGGER.info("Loaded {} custom armor mappings", registry.size());
   }

   private static String assetId(class_2960 file) {
      String path = file.method_12832();
      path = path.substring("equipment".length() + 1, path.length() - ".json".length());
      String var10000 = file.method_12836();
      return var10000 + ":" + path;
   }

   private static @Nullable class_2960 resolve(class_3300 rm, EquipmentJson json, String layerType) {
      List<LayerJson> layers = (List)json.layers().get(layerType);
      if (layers != null && !layers.isEmpty()) {
         class_2960 texture = ((LayerJson)layers.get(0)).texture();
         class_2960 path = class_2960.method_60655(texture.method_12836(), "textures/entity/equipment/" + layerType + "/" + texture.method_12832() + ".png");
         return rm.method_14486(path).isPresent() ? path : null;
      } else {
         return null;
      }
   }

   public @NotNull CompletableFuture<Void> method_25931(class_3302.class_4045 preparationBarrier, class_3300 resourceManager, class_3695 preparationsProfiler, class_3695 applyProfiler, Executor preparationExecutor, Executor applyExecutor) {
      CompletableFuture var10000 = CompletableFuture.runAsync(() -> this.load(resourceManager), preparationExecutor);
      Objects.requireNonNull(preparationBarrier);
      return var10000.thenCompose(preparationBarrier::method_18352);
   }

   public @NotNull String method_22322() {
      return "cobbleclub";
   }

   public class_2960 getFabricId() {
      return class_2960.method_60655("cobbleclub", "armor_equipment_loader");
   }

   @Environment(EnvType.CLIENT)
   public static record Key(class_2960 item, int customModelData) {
   }

   @Environment(EnvType.CLIENT)
   public static record Entry(@Nullable class_2960 body, @Nullable class_2960 leggings) {
   }

   @Environment(EnvType.CLIENT)
   private static record LayerJson(class_2960 texture) {
      static final Codec<LayerJson> CODEC = RecordCodecBuilder.create((instance) -> instance.group(class_2960.field_25139.fieldOf("texture").forGetter(LayerJson::texture)).apply(instance, LayerJson::new));
   }

   @Environment(EnvType.CLIENT)
   private static record EquipmentJson(Map<String, List<LayerJson>> layers) {
      static final Codec<EquipmentJson> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.unboundedMap(Codec.STRING, ArmorEquipmentLoader.LayerJson.CODEC.listOf()).fieldOf("layers").forGetter(EquipmentJson::layers)).apply(instance, EquipmentJson::new));
   }

   @Environment(EnvType.CLIENT)
   private static record BridgeJson(Map<String, Map<String, String>> bridge) {
      static final Codec<BridgeJson> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING)).fieldOf("bridge").forGetter(BridgeJson::bridge)).apply(instance, BridgeJson::new));
   }
}
