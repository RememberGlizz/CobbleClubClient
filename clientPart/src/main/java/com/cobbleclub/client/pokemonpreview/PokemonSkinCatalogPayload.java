package com.cobbleclub.client.pokemonpreview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
record PokemonSkinCatalogPayload(String json) implements class_8710 {
   static final int MAX_JSON = 262144;
   static final class_8710.class_9154<PokemonSkinCatalogPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "pokemon_skins/v1"));
   static final class_9139<class_9129, PokemonSkinCatalogPayload> STREAM_CODEC = class_9139.method_56437((buf, p) -> buf.method_10788(p.json(), 262144), (buf) -> new PokemonSkinCatalogPayload(buf.method_10800(262144)));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
