package com.cobbleclub.client.rp;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.sizer.LayerCodec;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_3302;
import net.minecraft.class_3695;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class LayerDataLoader implements IdentifiableResourceReloadListener {
   public static final HashMap<String, SizerRegStruct> LAYER_REGISTRY = new HashMap();
   private static final String DIRECTORY = "pl_sizer";

   public static LayerCodec.Settings getSettings(Pokemon pokemon, String aspect) {
      String name = pokemon.getSpecies().getResourceIdentifier().method_12832();
      String formName = pokemon.getForm().getName();
      SizerRegStruct reg = (SizerRegStruct)LAYER_REGISTRY.get(name);
      return reg == null ? null : reg.getSettings(formName, aspect);
   }

   public void load() {
      class_3300 rm = class_310.method_1551().method_1478();
      LAYER_REGISTRY.clear();

      for(class_2960 id : rm.method_14488("pl_sizer", (path) -> path.method_12832().endsWith(".json")).keySet()) {
         try {
            InputStream stream = ((class_3298)rm.method_14486(id).get()).method_14482();

            try {
               LayerCodec codec = (LayerCodec)LayerCodec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(stream))).result().orElseThrow();
               LAYER_REGISTRY.putIfAbsent(codec.pokemon(), new SizerRegStruct());
               SizerRegStruct sizerRegStruct = (SizerRegStruct)LAYER_REGISTRY.get(codec.pokemon());

               for(String form : codec.size_config().keySet()) {
                  for(Map.Entry<String, LayerCodec.Settings> aspectSetting : codec.size_config().get(form).entrySet()) {
                     sizerRegStruct.addForm(form, aspectSetting.getKey(), aspectSetting.getValue());
                  }
               }
            } catch (Throwable var13) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }
               }

               throw var13;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception e) {
            CobbleClubClient.LOGGER.error("Failed loading layer JSON: {}", id, e);
         }
      }

      CobbleClubClient.LOGGER.info("Loaded {} custom layer data", LAYER_REGISTRY.size());
   }

   public @NotNull CompletableFuture<Void> method_25931(class_3302.class_4045 preparationBarrier, class_3300 resourceManager, class_3695 preparationsProfiler, class_3695 applyProfiler, Executor preparationExecutor, Executor applyExecutor) {
      CompletableFuture var10000 = CompletableFuture.runAsync(this::load, preparationExecutor);
      Objects.requireNonNull(preparationBarrier);
      return var10000.thenCompose(preparationBarrier::method_18352).thenRunAsync(() -> {
      }, applyExecutor);
   }

   public @NotNull String method_22322() {
      return "cobbleclub";
   }

   public class_2960 getFabricId() {
      return class_2960.method_60655("cobbleclub", "layer_data");
   }

   @Environment(EnvType.CLIENT)
   public static class SizerRegStruct {
      private final HashMap<String, HashMap<String, LayerCodec.Settings>> settingsHashMap = new HashMap();

      public void addForm(String formName, String aspect, LayerCodec.Settings settings) {
         HashMap<String, LayerCodec.Settings> map = (HashMap)this.settingsHashMap.getOrDefault(formName, new HashMap());
         map.put(aspect, settings);
         this.settingsHashMap.putIfAbsent(formName, map);
      }

      public LayerCodec.Settings getSettings(String formName, String aspect) {
         return (LayerCodec.Settings)((HashMap)this.settingsHashMap.getOrDefault(formName, new HashMap())).get(aspect);
      }
   }
}
