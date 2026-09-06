package com.cobbleclub.client.wardrobe.net;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record WardrobeOpenPayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<WardrobeOpenPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "wardrobe_open/v2"));
   public static final PacketCodec<PacketByteBuf, WardrobeOpenPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeByteArray(WardrobeProtocol.compress(payload.json)), (buf) -> new WardrobeOpenPayload(WardrobeProtocol.decompress(buf.readByteArray(262144))));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
