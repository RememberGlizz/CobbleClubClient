package com.cobbleclub.client.battle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class InfoBadge {
   public static final int SIZE = 8;
   private static final int[] FRAMES = new int[]{32, 24, 16, 8};
   private static final Identifier[] TEXTURES;

   private InfoBadge() {
   }

   public static void draw(DrawContext graphics, int x, int y, boolean hovered, float alpha) {
      int i = frameIndexFor(MinecraftClient.getInstance().getWindow().getScaleFactor());
      int frame = FRAMES[i];
      graphics.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      graphics.drawTexture(TEXTURES[i], x, y, 8, 8, hovered ? (float)frame : 0.0F, 0.0F, frame, frame, frame * 2, frame);
      graphics.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
      TEXTURES = new Identifier[FRAMES.length];

      for(int i = 0; i < FRAMES.length; ++i) {
         int var10003 = FRAMES[i];
         TEXTURES[i] = Identifier.of("cobbleclub", "textures/gui/battle/info_badge_" + var10003 + ".png");
      }

   }
}
