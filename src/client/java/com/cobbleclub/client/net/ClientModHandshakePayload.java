package com.cobbleclub.client.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record ClientModHandshakePayload(String version) implements CustomPayload {
   public static final CustomPayload.Id<ClientModHandshakePayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "handshake/v1"));
   public static final PacketCodec<RegistryByteBuf, ClientModHandshakePayload> STREAM_CODEC;

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }

   static {
      STREAM_CODEC = PacketCodec.tuple(PacketCodecs.STRING, ClientModHandshakePayload::version, ClientModHandshakePayload::new);
   }
}
