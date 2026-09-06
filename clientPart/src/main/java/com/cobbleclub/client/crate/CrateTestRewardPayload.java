package com.cobbleclub.client.crate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
record CrateTestRewardPayload(String crateId, int prizeIndex, boolean shiny) implements class_8710 {
   static final int MAX_ID = 256;
   static final class_8710.class_9154<CrateTestRewardPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "crate_test_reward/v1"));
   static final class_9139<class_9129, CrateTestRewardPayload> STREAM_CODEC = class_9139.method_56437((buf, p) -> {
      buf.method_10788(p.crateId(), 256);
      buf.method_53002(p.prizeIndex());
      buf.method_52964(p.shiny());
   }, (buf) -> new CrateTestRewardPayload(buf.method_10800(256), buf.readInt(), buf.readBoolean()));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
