package com.cobbleclub.client.ui;

import com.cobbleclub.client.CobbleClubClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class ClientJsonConfig {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

   private ClientJsonConfig() {
   }

   public static Path file(String name) {
      return FabricLoader.getInstance().getConfigDir().resolve(name);
   }

   public static <T> T load(Path file, Class<T> type, Supplier<T> defaults) {
      if (Files.exists(file, new LinkOption[0])) {
         try {
            Reader reader = Files.newBufferedReader(file);

            Object var5;
            label55: {
               try {
                  T loaded = (T)GSON.fromJson(reader, type);
                  if (loaded != null) {
                     var5 = loaded;
                     break label55;
                  }
               } catch (Throwable var7) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (reader != null) {
                  reader.close();
               }

               return (T)defaults.get();
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var5;
         } catch (Exception e) {
            CobbleClubClient.LOGGER.warn("Failed to read {}, using defaults", file.getFileName(), e);
            return (T)defaults.get();
         }
      } else {
         T fresh = (T)defaults.get();
         save(file, fresh);
         return fresh;
      }
   }

   public static void save(Path file, Object instance) {
      try {
         Files.createDirectories(file.getParent());
         Writer writer = Files.newBufferedWriter(file);

         try {
            GSON.toJson(instance, writer);
         } catch (Throwable var6) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (Exception e) {
         CobbleClubClient.LOGGER.warn("Failed to write {}", file.getFileName(), e);
      }

   }
}
