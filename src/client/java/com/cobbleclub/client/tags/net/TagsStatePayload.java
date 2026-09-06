package com.cobbleclub.client.tags.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record TagsStatePayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<TagsStatePayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "tags_state/v1"));
   public static final PacketCodec<PacketByteBuf, TagsStatePayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeString(payload.json, 262144), (buf) -> new TagsStatePayload(buf.readString(262144)));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
