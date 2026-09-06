package com.cobbleclub.client.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9135;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record ClientModHandshakePayload(String version) implements class_8710 {
   public static final class_8710.class_9154<ClientModHandshakePayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "handshake/v1"));
   public static final class_9139<class_9129, ClientModHandshakePayload> STREAM_CODEC;

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }

   static {
      STREAM_CODEC = class_9139.method_56434(class_9135.field_48554, ClientModHandshakePayload::version, ClientModHandshakePayload::new);
   }
}
