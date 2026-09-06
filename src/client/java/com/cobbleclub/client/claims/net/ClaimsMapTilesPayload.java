package com.cobbleclub.client.claims.net;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record ClaimsMapTilesPayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<ClaimsMapTilesPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "claims_map_tiles/v1"));
   public static final PacketCodec<PacketByteBuf, ClaimsMapTilesPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeByteArray(ClaimsScreenProtocol.compress(payload.json)), (buf) -> new ClaimsMapTilesPayload(ClaimsScreenProtocol.decompress(buf.readByteArray(262144))));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
