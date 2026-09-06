package com.cobbleclub.client.claims.net;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record ClaimsMapTilesPayload(String json) implements class_8710 {
   public static final class_8710.class_9154<ClaimsMapTilesPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "claims_map_tiles/v1"));
   public static final class_9139<class_2540, ClaimsMapTilesPayload> STREAM_CODEC = class_9139.method_56437((buf, payload) -> buf.method_10813(ClaimsScreenProtocol.compress(payload.json)), (buf) -> new ClaimsMapTilesPayload(ClaimsScreenProtocol.decompress(buf.method_10803(262144))));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
