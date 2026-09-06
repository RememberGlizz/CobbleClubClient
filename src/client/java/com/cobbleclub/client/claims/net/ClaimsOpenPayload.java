package com.cobbleclub.client.claims.net;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record ClaimsOpenPayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<ClaimsOpenPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "claims_open/v1"));
   public static final PacketCodec<PacketByteBuf, ClaimsOpenPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeByteArray(ClaimsScreenProtocol.compress(payload.json)), (buf) -> new ClaimsOpenPayload(ClaimsScreenProtocol.decompress(buf.readByteArray(262144))));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
