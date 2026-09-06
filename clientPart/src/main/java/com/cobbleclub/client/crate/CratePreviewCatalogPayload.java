package com.cobbleclub.client.crate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
record CratePreviewCatalogPayload(String json) implements class_8710 {
   static final int MAX_JSON = 262144;
   static final class_8710.class_9154<CratePreviewCatalogPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "crate_preview/v1"));
   static final class_9139<class_9129, CratePreviewCatalogPayload> STREAM_CODEC = class_9139.method_56437((buf, p) -> buf.method_10788(p.json(), 262144), (buf) -> new CratePreviewCatalogPayload(buf.method_10800(262144)));

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }
}
