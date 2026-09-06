package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class ClubScrollbar {
   public static final int WIDTH = 3;
   private static final int GRAB_PAD = 2;

   private ClubScrollbar() {
   }

   public static void draw(DrawContext g, int x, int y0, int y1, int scroll, int maxScroll) {
      draw(g, x, y0, y1, scroll, maxScroll, y1 - y0);
   }

   public static void draw(DrawContext g, int x, int y0, int y1, int scroll, int maxScroll, int visible) {
      if (maxScroll > 0) {
         int trackH = y1 - y0;
         g.fill(x, y0, x + 3, y1, -15986650);
         int thumbH = thumbH(trackH, maxScroll, visible);
         int thumbY = y0 + (trackH - thumbH) * scroll / maxScroll;
         g.fill(x, thumbY, x + 3, thumbY + thumbH, -10862962);
      }
   }

   public static boolean contains(double mx, double my, int x, int y0, int y1) {
      return mx >= (double)(x - 2) && mx < (double)(x + 3 + 2) && my >= (double)y0 && my < (double)y1;
   }

   public static int scrollAt(double my, int y0, int y1, int maxScroll) {
      return scrollAt(my, y0, y1, maxScroll, y1 - y0);
   }

   public static int scrollAt(double my, int y0, int y1, int maxScroll, int visible) {
      if (maxScroll <= 0) {
         return 0;
      } else {
         int trackH = y1 - y0;
         int thumbH = thumbH(trackH, maxScroll, visible);
         int travel = trackH - thumbH;
         return travel <= 0 ? 0 : MathHelper.clamp((int)Math.round((my - (double)y0 - (double)thumbH / (double)2.0F) * (double)maxScroll / (double)travel), 0, maxScroll);
      }
   }

   private static int thumbH(int trackH, int maxScroll, int visible) {
      return Math.max(8, trackH * visible / (visible + maxScroll));
   }
}
