package com.cobbleclub.client.dashboard.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record DashboardActionPayload(String action) implements CustomPayload {
   public static final CustomPayload.Id<DashboardActionPayload> TYPE = new CustomPayload.Id<>(Identifier.of("cobbleclub", "dashboard_action/v1"));
   public static final PacketCodec<PacketByteBuf, DashboardActionPayload> STREAM_CODEC = PacketCodec.ofStatic(
           (buf, payload) -> buf.writeString(payload.action, 64),
           buf -> new DashboardActionPayload(buf.readString(64))
   );

   public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
}
