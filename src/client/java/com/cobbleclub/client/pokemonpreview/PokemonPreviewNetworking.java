package com.cobbleclub.client.pokemonpreview;

import com.cobbleclub.server.network.Payloads;
import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public final class PokemonPreviewNetworking {
   private static final Gson GSON = new Gson();

   private PokemonPreviewNetworking() {}

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(Payloads.PokemonSkins.ID, (payload, context) -> {
         try {
            PokemonSkinCatalog catalog = GSON.fromJson(payload.json(), PokemonSkinCatalog.class);
            if (catalog != null) context.client().setScreen(new PokemonPreviewScreen(catalog));
         } catch (Exception ignored) {
         }
      });
   }
}
