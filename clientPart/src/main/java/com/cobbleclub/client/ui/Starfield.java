package com.cobbleclub.client.ui;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public final class Starfield {
   private static final int VIOLET = 12160255;
   private static final int GOLD = 16241514;
   private static final float DRIFT_RATE = 6.0E-6F;

   private Starfield() {
   }

   public static void draw(class_332 g, int x0, int y0, int x1, int y1, long now, int count, long seed, float baseAlpha) {
      int w = x1 - x0;
      int h = y1 - y0;
      if (w > 0 && h > 0) {
         Random r = new Random(seed);

         for(int i = 0; i < count; ++i) {
            float bx = r.nextFloat();
            float by = r.nextFloat();
            int layer = r.nextInt(3);
            float phase = r.nextFloat() * 6.2832F;
            int ctype = r.nextInt(6);
            float drift = (float)(layer + 1) * 6.0E-6F * (float)now;
            float nx = ((bx + drift) % 1.0F + 1.0F) % 1.0F;
            int sx = x0 + (int)(nx * (float)w);
            int sy = y0 + (int)(by * (float)h);
            float tw = (float)Math.sin((double)now * 0.0016 * (double)(0.6F + (float)layer * 0.5F) + (double)phase);
            int a = (int)(baseAlpha * (0.35F + 0.65F * Math.max(0.0F, tw)) * 255.0F);
            if (a > 6) {
               a = Math.min(a, 255);
               int col = ctype == 0 ? 12160255 : (ctype == 1 ? 16241514 : 16777215);
               g.method_25294(sx, sy, sx + 1, sy + 1, a << 24 | col);
               if (layer == 2 && tw > 0.85F) {
                  int ga = a / 2 << 24 | col;
                  g.method_25294(sx - 1, sy, sx + 2, sy + 1, ga);
                  g.method_25294(sx, sy - 1, sx + 1, sy + 2, ga);
               }
            }
         }

      }
   }
}
