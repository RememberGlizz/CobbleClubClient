package com.cobbleclub.client.claims.net;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record ClaimsWorldPayload(String json) implements class_8710 {
   public static final class_8710.class_9154<ClaimsWorldPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "claims_world/v1"));
   public static final class_9139<class_2540, ClaimsWorldPayload> STREAM_CODEC = class_9139.method_56437((buf, payload) -> buf.method_10813(ClaimsScreenProtocol.compress(payload.json)), (buf) -> new ClaimsWorldPayload(ClaimsScreenProtocol.decompress(buf.method_10803(32768))));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
