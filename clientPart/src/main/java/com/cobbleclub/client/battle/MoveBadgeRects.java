package com.cobbleclub.client.battle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MoveBadgeRects {
   private static final int MAX = 8;
   private static final int[] XS = new int[8];
   private static final int[] YS = new int[8];
   private static int count;

   private MoveBadgeRects() {
   }

   public static void beginFrame() {
      count = 0;
   }

   public static void record(int x, int y) {
      if (count < 8) {
         XS[count] = x;
         YS[count] = y;
         ++count;
      }

   }

   public static boolean contains(double mouseX, double mouseY) {
      for(int i = 0; i < count; ++i) {
         if (InfoBadge.isOver(mouseX, mouseY, XS[i], YS[i])) {
            return true;
         }
      }

      return false;
   }
}
