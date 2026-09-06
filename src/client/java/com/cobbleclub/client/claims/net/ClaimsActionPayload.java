package com.cobbleclub.client.claims.net;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record ClaimsActionPayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<ClaimsActionPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "claims_action/v1"));
   public static final PacketCodec<PacketByteBuf, ClaimsActionPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeByteArray(ClaimsScreenProtocol.compress(payload.json)), (buf) -> new ClaimsActionPayload(ClaimsScreenProtocol.decompress(buf.readByteArray(8192))));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
