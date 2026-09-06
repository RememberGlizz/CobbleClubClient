package com.cobbleclub.client.pokemonpreview;

import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

@Environment(EnvType.CLIENT)
public final class PokemonPreviewNetworking {
   private static final Gson GSON = new Gson();

   private PokemonPreviewNetworking() {
   }

   public static void init() {
      PayloadTypeRegistry.playS2C().register(PokemonSkinCatalogPayload.TYPE, PokemonSkinCatalogPayload.STREAM_CODEC);
      ClientPlayNetworking.registerGlobalReceiver(PokemonSkinCatalogPayload.TYPE, (payload, context) -> {
         try {
            PokemonSkinCatalog catalog = (PokemonSkinCatalog)GSON.fromJson(payload.json(), PokemonSkinCatalog.class);
            if (catalog != null) {
               context.client().setScreen(new PokemonPreviewScreen(catalog));
            }
         } catch (Exception var3) {
         }

      });
   }
}
