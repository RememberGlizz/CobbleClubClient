package com.cobbleclub.client.crate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
record CratePreviewCatalogPayload(String json) implements CustomPayload {
   static final int MAX_JSON = 262144;
   static final CustomPayload.Id<CratePreviewCatalogPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "crate_preview/v1"));
   static final PacketCodec<RegistryByteBuf, CratePreviewCatalogPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, p) -> buf.writeString(p.json(), 262144), (buf) -> new CratePreviewCatalogPayload(buf.readString(262144)));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
