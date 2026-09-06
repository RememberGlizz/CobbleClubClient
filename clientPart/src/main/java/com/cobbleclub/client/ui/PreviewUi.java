package com.cobbleclub.client.ui;

import com.cobblemon.mod.common.CobblemonSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1109;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3417;
import net.minecraft.class_7225;
import net.minecraft.class_2561.class_2562;

@Environment(EnvType.CLIENT)
public final class PreviewUi {
   private PreviewUi() {
   }

   public static class_7225.class_7874 registry() {
      return class_310.method_1551().field_1687 != null ? class_310.method_1551().field_1687.method_30349() : null;
   }

   public static class_2561 deserialize(String json, String fallback) {
      class_7225.class_7874 reg = registry();
      if (json != null && reg != null) {
         try {
            class_2561 parsed = class_2562.method_10877(json, reg);
            if (parsed != null) {
               return parsed;
            }
         } catch (Exception var4) {
         }
      }

      return class_2561.method_43470(fallback != null ? fallback : "");
   }

   public static void playClick() {
      class_310.method_1551().method_1483().method_4873(class_1109.method_47978(class_3417.field_15015, 1.0F));
   }

   public static void playOpen() {
      class_310.method_1551().method_1483().method_4873(class_1109.method_4757(CobblemonSounds.PC_ON, 0.8F, 0.3F));
   }

   public static void playClose() {
      class_310.method_1551().method_1483().method_4873(class_1109.method_4757(CobblemonSounds.PC_OFF, 1.0F, 0.3F));
   }

   public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
      return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
   }

   public static void renderScaledItem(class_332 g, class_1799 stack, int itemX, int itemY, float scale) {
      float cx = (float)(itemX + 8);
      float cy = (float)(itemY + 8);
      g.method_51448().method_22903();
      g.method_51448().method_22904((double)cx, (double)cy, (double)0.0F);
      g.method_51448().method_22905(scale, scale, 1.0F);
      g.method_51448().method_22904((double)(-cx), (double)(-cy), (double)0.0F);
      g.method_51427(stack, itemX, itemY);
      g.method_51448().method_22909();
   }
}
