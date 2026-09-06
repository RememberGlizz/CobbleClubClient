package com.cobbleclub.client.kits.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record KitsActionPayload(String action) implements CustomPayload {
   public static final CustomPayload.Id<KitsActionPayload> TYPE = new CustomPayload.Id<>(Identifier.of("cobbleclub", "kits_action/v1"));
   public static final PacketCodec<PacketByteBuf, KitsActionPayload> STREAM_CODEC = PacketCodec.ofStatic(
           (buf, payload) -> buf.writeString(payload.action, 64),
           buf -> new KitsActionPayload(buf.readString(64))
   );
   public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
}
