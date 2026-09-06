package com.cobbleclub.client.kits.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record KitsOpenPayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<KitsOpenPayload> TYPE = new CustomPayload.Id<>(Identifier.of("cobbleclub", "kits_open/v1"));
   public static final PacketCodec<PacketByteBuf, KitsOpenPayload> STREAM_CODEC = PacketCodec.ofStatic(
           (buf, payload) -> buf.writeString(payload.json, 32768),
           buf -> new KitsOpenPayload(buf.readString(32768))
   );
   public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
}
