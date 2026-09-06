package com.cobbleclub.client.dashboard.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record DashboardStatePayload(String json) implements CustomPayload {
   public static final CustomPayload.Id<DashboardStatePayload> TYPE = new CustomPayload.Id<>(Identifier.of("cobbleclub", "dashboard_state/v1"));
   public static final PacketCodec<PacketByteBuf, DashboardStatePayload> STREAM_CODEC = PacketCodec.ofStatic(
           (buf, payload) -> buf.writeString(payload.json, 32768),
           buf -> new DashboardStatePayload(buf.readString(32768))
   );

   public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
}
