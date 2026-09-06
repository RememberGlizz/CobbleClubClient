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
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ArmorEquipmentLoader implements IdentifiableResourceReloadListener {
   private static volatile Map<Key, Entry> registry = Map.of();
   private static final String EQUIPMENT_DIR = "equipment";
   private static final String BRIDGE_DIR = "armor_bridge";

   public static @Nullable Entry lookup(Identifier item, int customModelData) {
      return (Entry)registry.get(new Key(item, customModelData));
   }

   public void load(ResourceManager rm) {
      Map<String, Entry> assets = new HashMap();
      rm.findResources("equipment", (path) -> path.getPath().endsWith(".json")).keySet().forEach((id) -> {
         try {
            InputStream stream = ((Resource)rm.getResource(id).orElseThrow()).getInputStream();

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
      rm.findResources("armor_bridge", (path) -> path.getPath().endsWith(".json")).keySet().forEach((id) -> {
         try {
            InputStream stream = ((Resource)rm.getResource(id).orElseThrow()).getInputStream();

            try {
               BridgeJson json = (BridgeJson)ArmorEquipmentLoader.BridgeJson.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(stream))).result().orElseThrow();
               json.bridge().forEach((itemStr, cmdMap) -> {
                  Identifier item = Identifier.tryParse(itemStr);
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

   private static String assetId(Identifier file) {
      String path = file.getPath();
      path = path.substring("equipment".length() + 1, path.length() - ".json".length());
      String var10000 = file.getNamespace();
      return var10000 + ":" + path;
   }

   private static @Nullable Identifier resolve(ResourceManager rm, EquipmentJson json, String layerType) {
      List<LayerJson> layers = (List)json.layers().get(layerType);
      if (layers != null && !layers.isEmpty()) {
         Identifier texture = ((LayerJson)layers.get(0)).texture();
         Identifier path = Identifier.of(texture.getNamespace(), "textures/entity/equipment/" + layerType + "/" + texture.getPath() + ".png");
         return rm.getResource(path).isPresent() ? path : null;
      } else {
         return null;
      }
   }

   public @NotNull CompletableFuture<Void> reload(ResourceReloader.Synchronizer preparationBarrier, ResourceManager resourceManager, Profiler preparationsProfiler, Profiler applyProfiler, Executor preparationExecutor, Executor applyExecutor) {
      CompletableFuture var10000 = CompletableFuture.runAsync(() -> this.load(resourceManager), preparationExecutor);
      Objects.requireNonNull(preparationBarrier);
      return var10000.thenCompose(preparationBarrier::whenPrepared);
   }

   public @NotNull String getName() {
      return "cobbleclub";
   }

   public Identifier getFabricId() {
      return Identifier.of("cobbleclub", "armor_equipment_loader");
   }

   @Environment(EnvType.CLIENT)
   public static record Key(Identifier item, int customModelData) {
   }

   @Environment(EnvType.CLIENT)
   public static record Entry(@Nullable Identifier body, @Nullable Identifier leggings) {
   }

   @Environment(EnvType.CLIENT)
   private static record LayerJson(Identifier texture) {
      static final Codec<LayerJson> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Identifier.CODEC.fieldOf("texture").forGetter(LayerJson::texture)).apply(instance, LayerJson::new));
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
