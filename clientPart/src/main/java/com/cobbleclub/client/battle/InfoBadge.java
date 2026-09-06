package com.cobbleclub.client.battle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public final class InfoBadge {
   public static final int SIZE = 8;
   private static final int[] FRAMES = new int[]{32, 24, 16, 8};
   private static final class_2960[] TEXTURES;

   private InfoBadge() {
   }

   public static void draw(class_332 graphics, int x, int y, boolean hovered, float alpha) {
      int i = frameIndexFor(class_310.method_1551().method_22683().method_4495());
      int frame = FRAMES[i];
      graphics.method_51422(1.0F, 1.0F, 1.0F, alpha);
      graphics.method_25293(TEXTURES[i], x, y, 8, 8, hovered ? (float)frame : 0.0F, 0.0F, frame, frame, frame * 2, frame);
      graphics.method_51422(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static int frameIndexFor(double guiScale) {
      int physical = 8 * Math.max(1, (int)Math.round(guiScale));
      int dense = -1;

      for(int i = 0; i < FRAMES.length; ++i) {
         if (FRAMES[i] >= physical) {
            dense = i;
         }
      }

      if (dense >= 0) {
         return dense;
      } else {
         for(int i = 0; i < FRAMES.length; ++i) {
            if (physical % FRAMES[i] == 0 && physical / FRAMES[i] <= 2) {
               return i;
            }
         }

         return 0;
      }
   }

   public static boolean isOver(double mouseX, double mouseY, int x, int y) {
      return mouseX >= (double)x && mouseX <= (double)(x + 8) && mouseY >= (double)y && mouseY <= (double)(y + 8);
   }

   static {
      TEXTURES = new class_2960[FRAMES.length];

      for(int i = 0; i < FRAMES.length; ++i) {
         int var10003 = FRAMES[i];
         TEXTURES[i] = class_2960.method_60655("cobbleclub", "textures/gui/battle/info_badge_" + var10003 + ".png");
      }

   }
}
