package com.cobbleclub.client.wardrobe.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record CosmeticStatePayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<CosmeticStatePayload> TYPE = new CustomPayload.Id<>(Identifier.of("cobbleclub", "cosmetic_state/v1"));
   public static final PacketCodec<PacketByteBuf, CosmeticStatePayload> STREAM_CODEC = PacketCodec.ofStatic(
         (buf, payload) -> buf.writeString(payload.json(), 16384),
         buf -> new CosmeticStatePayload(buf.readString(16384))
   );

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
