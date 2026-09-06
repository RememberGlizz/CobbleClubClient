package com.cobbleclub.client.tags.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record TagsOpenPayload(String json) implements class_8710 {
   public static final class_8710.class_9154<TagsOpenPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "tags_open/v1"));
   public static final class_9139<class_2540, TagsOpenPayload> STREAM_CODEC = class_9139.method_56437((buf, payload) -> buf.method_10788(payload.json, 262144), (buf) -> new TagsOpenPayload(buf.method_10800(262144)));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
