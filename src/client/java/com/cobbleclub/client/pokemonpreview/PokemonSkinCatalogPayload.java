package com.cobbleclub.client.pokemonpreview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
record PokemonSkinCatalogPayload(String json) implements CustomPayload {
   static final int MAX_JSON = 262144;
   static final CustomPayload.Id<PokemonSkinCatalogPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "pokemon_skins/v1"));
   static final PacketCodec<RegistryByteBuf, PokemonSkinCatalogPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, p) -> buf.writeString(p.json(), 262144), (buf) -> new PokemonSkinCatalogPayload(buf.readString(262144)));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
