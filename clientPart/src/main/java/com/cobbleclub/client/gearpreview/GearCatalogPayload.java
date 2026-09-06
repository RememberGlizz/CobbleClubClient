package com.cobbleclub.client.gearpreview;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record GearCatalogPayload(List<GearCatalogSet> sets) implements class_8710 {
   public static final class_8710.class_9154<GearCatalogPayload> TYPE = new class_8710.class_9154(class_2960.method_60655("cobbleclub", "gear_catalog/v1"));
   public static final class_9139<class_9129, GearCatalogPayload> STREAM_CODEC = class_9139.method_56437((buf, payload) -> {
      buf.method_10804(payload.sets().size());

      for(GearCatalogSet set : payload.sets()) {
         buf.method_10814(set.id());
         buf.method_10814(set.displayName());
         buf.method_10804(set.items().size());

         for(class_1799 item : set.items()) {
            class_1799.field_49268.encode(buf, item);
         }
      }

   }, (buf) -> {
      int setCount = buf.method_10816();
      List<GearCatalogSet> sets = new ArrayList(setCount);

      for(int i = 0; i < setCount; ++i) {
         String id = buf.method_19772();
         String displayName = buf.method_19772();
         int itemCount = buf.method_10816();
         List<class_1799> items = new ArrayList(itemCount);

         for(int j = 0; j < itemCount; ++j) {
            items.add((class_1799)class_1799.field_49268.decode(buf));
         }

         sets.add(new GearCatalogSet(id, displayName, items));
      }

      return new GearCatalogPayload(sets);
   });

   public class_8710.class_9154<? extends class_8710> method_56479() {
      return TYPE;
   }

   @Environment(EnvType.CLIENT)
   public static record GearCatalogSet(String id, String displayName, List<class_1799> items) {
   }
}
