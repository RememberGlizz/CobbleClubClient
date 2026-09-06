package com.cobbleclub.client.wardrobe.net;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record WardrobeStatePayload(String json) implements class_8710 {
   public static final class_8710.class_9154<WardrobeStatePayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "wardrobe_state/v2"));
   public static final class_9139<class_2540, WardrobeStatePayload> STREAM_CODEC = class_9139.method_56437((buf, payload) -> buf.method_10813(WardrobeProtocol.compress(payload.json)), (buf) -> new WardrobeStatePayload(WardrobeProtocol.decompress(buf.method_10803(262144))));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
