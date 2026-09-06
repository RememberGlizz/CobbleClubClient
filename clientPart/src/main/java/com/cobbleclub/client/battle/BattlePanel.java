package com.cobbleclub.client.battle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public final class BattlePanel {
   public static final float Z = 400.0F;
   public static final int OUTLINE = -13684945;
   public static final int FRAME = -7500403;
   public static final int FRAME_HIGHLIGHT = -3750202;
   public static final int FRAME_SHADOW = -10921639;
   public static final int INNER_EDGE = -15987700;
   public static final int INTERIOR = -199681767;
   public static final int RULE = -6052957;
   public static final int WINDOW_INSET = 5;
   public static final int DRAW_INSET = 2;
   public static final int CONTENT_INSET = 4;

   private BattlePanel() {
   }

   public static void window(class_332 graphics, int x, int y, int w, int h) {
      graphics.method_49601(x, y, w, h, -13684945);
      graphics.method_25294(x + 1, y + 1, x + w - 1, y + 3, -7500403);
      graphics.method_25294(x + 1, y + h - 3, x + w - 1, y + h - 1, -7500403);
      graphics.method_25294(x + 1, y + 3, x + 3, y + h - 3, -7500403);
      graphics.method_25294(x + w - 3, y + 3, x + w - 1, y + h - 3, -7500403);
      graphics.method_25294(x + 1, y + 1, x + w - 1, y + 2, -3750202);
      graphics.method_49601(x + 3, y + 3, w - 6, h - 6, -10921639);
      graphics.method_49601(x + 4, y + 4, w - 8, h - 8, -15987700);
      graphics.method_25294(x + 5, y + 5, x + w - 5, y + h - 5, -199681767);
   }

   public static void draw(class_332 graphics, int x, int y, int width, int height) {
      graphics.method_49601(x, y, width, height, -13684945);
      graphics.method_49601(x + 1, y + 1, width - 2, height - 2, -7500403);
      graphics.method_25294(x + 1, y + 1, x + width - 1, y + 2, -3750202);
      graphics.method_25294(x + 2, y + 2, x + width - 2, y + height - 2, -199681767);
   }
}
