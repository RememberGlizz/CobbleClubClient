package com.cobbleclub.client.render;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.render.renderTypes.CobbleClubRenderTypes;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class TeraMapLoader implements IdentifiableResourceReloadListener {
   public static final HashMap<String, String> REGISTRY = new HashMap();
   private static final String DIRECTORY = "cobbleclub/tera_map";

   public void load() {
      ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
      REGISTRY.clear();

      for(Identifier id : rm.findResources("cobbleclub/tera_map", (path) -> path.getPath().endsWith(".json")).keySet()) {
         try {
            InputStream stream = ((Resource)rm.getResource(id).get()).getInputStream();

            try {
               TeraMap codec = (TeraMap)TeraMapLoader.TeraMap.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(stream))).result().orElseThrow();
               REGISTRY.putAll(codec.colorMap);
            } catch (Throwable var9) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception e) {
            CobbleClubClient.LOGGER.error("Failed loading tera map JSON: {}", id, e);
         }
      }

      CobbleClubClient.LOGGER.info("Loaded {} custom tera map", REGISTRY.size());
   }

   public static ShaderProgram getColorShaderMap(String color) {
      ShaderProgram var10000;
      switch (color) {
         case "red":
            var10000 = CobbleClubRenderTypes.teraFire;
            break;
         case "blue":
            var10000 = CobbleClubRenderTypes.teraWater;
            break;
         case "green":
            var10000 = CobbleClubRenderTypes.teraGrass;
            break;
         case "yellow":
            var10000 = CobbleClubRenderTypes.teraElectric;
            break;
         case "brown":
            var10000 = CobbleClubRenderTypes.teraGround;
            break;
         case "light_blue":
            var10000 = CobbleClubRenderTypes.teraFlying;
            break;
         case "purple":
            var10000 = CobbleClubRenderTypes.teraDragon;
            break;
         case "pink":
            var10000 = CobbleClubRenderTypes.teraFairy;
            break;
         case "black":
            var10000 = CobbleClubRenderTypes.teraDark;
            break;
         case "gray":
            var10000 = CobbleClubRenderTypes.teraSteel;
            break;
         case "light_grey":
            var10000 = CobbleClubRenderTypes.teraIce;
            break;
         case "orange":
            var10000 = CobbleClubRenderTypes.teraFighting;
            break;
         case "lime":
            var10000 = CobbleClubRenderTypes.teraBug;
            break;
         case "teal":
            var10000 = CobbleClubRenderTypes.teraPoison;
            break;
         case "indigo":
            var10000 = CobbleClubRenderTypes.teraGhost;
            break;
         case "magenta":
            var10000 = CobbleClubRenderTypes.teraPsychic;
            break;
         case "tan":
            var10000 = CobbleClubRenderTypes.teraRock;
            break;
         case "navy":
            var10000 = CobbleClubRenderTypes.teraNormal;
            break;
         case "white":
            var10000 = CobbleClubRenderTypes.teraStellar;
            break;
         default:
            CobbleClubClient.LOGGER.error("Unknown tera shader color '{}'", color);
            var10000 = CobbleClubRenderTypes.teraStellar;
      }

      return var10000;
   }

   public @NotNull CompletableFuture<Void> reload(ResourceReloader.Synchronizer preparationBarrier, ResourceManager resourceManager, Profiler preparationsProfiler, Profiler applyProfiler, Executor preparationExecutor, Executor applyExecutor) {
      CompletableFuture var10000 = CompletableFuture.runAsync(this::load, preparationExecutor);
      Objects.requireNonNull(preparationBarrier);
      return var10000.thenCompose(preparationBarrier::whenPrepared).thenRunAsync(() -> {
      }, applyExecutor);
   }

   public @NotNull String getName() {
      return "cobbleclub";
   }

   public Identifier getFabricId() {
      return Identifier.of("cobbleclub", "tera_map_loader");
   }

   @Environment(EnvType.CLIENT)
   public static record TeraMap(Map<String, String> colorMap) {
      public static Codec<TeraMap> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("aspectShaderMap").forGetter(TeraMap::colorMap)).apply(instance, TeraMap::new));
   }
}
