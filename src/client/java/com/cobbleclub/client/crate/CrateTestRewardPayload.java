package com.cobbleclub.client.crate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
record CrateTestRewardPayload(String crateId, int prizeIndex, boolean shiny) implements CustomPayload {
   static final int MAX_ID = 256;
   static final CustomPayload.Id<CrateTestRewardPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "crate_test_reward/v1"));
   static final PacketCodec<RegistryByteBuf, CrateTestRewardPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, p) -> {
      buf.writeString(p.crateId(), 256);
      buf.writeInt(p.prizeIndex());
      buf.writeBoolean(p.shiny());
   }, (buf) -> new CrateTestRewardPayload(buf.readString(256), buf.readInt(), buf.readBoolean()));

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}
