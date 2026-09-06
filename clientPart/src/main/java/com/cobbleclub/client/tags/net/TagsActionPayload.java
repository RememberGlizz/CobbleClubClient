package com.cobbleclub.client.tags.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record TagsActionPayload(String json) implements class_8710 {
   public static final class_8710.class_9154<TagsActionPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "tags_action/v1"));
   public static final class_9139<class_2540, TagsActionPayload> STREAM_CODEC = class_9139.method_56437((buf, payload) -> buf.method_10788(payload.json, 4096), (buf) -> new TagsActionPayload(buf.method_10800(4096)));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
