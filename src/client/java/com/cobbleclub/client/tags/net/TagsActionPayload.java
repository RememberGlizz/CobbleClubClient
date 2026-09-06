package com.cobbleclub.client.tags.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record TagsActionPayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<TagsActionPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "tags_action/v1"));
   public static final PacketCodec<PacketByteBuf, TagsActionPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeString(payload.json, 4096), (buf) -> new TagsActionPayload(buf.readString(4096)));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
