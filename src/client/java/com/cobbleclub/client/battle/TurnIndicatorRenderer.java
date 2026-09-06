package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class TurnIndicatorRenderer {
   private static final int PADDING_X = 6;
   private static final int MARGIN = 5;
   private static final int TEXT_COLOR = -1;

   private TurnIndicatorRenderer() {
   }

   public static Rect currentRect() {
      if (!BattleConfig.get().turnIndicator) {
         return null;
      } else {
         int turn = BattleState.turnNumber;
         if (turn > 0 && CobblemonClient.INSTANCE.getBattle() != null && BattleState.battleTilesVisible) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
               return null;
            } else {
               TextRenderer font = mc.textRenderer;
               int textWidth = font.getWidth(BattleState.turnLabel(turn));
               int boxWidth = textWidth + 12;
               Objects.requireNonNull(font);
               int boxHeight = 9 + 4 + 2;
               int screenWidth = mc.getWindow().getScaledWidth();
               int x = Math.max(screenWidth / 2 + 91 + 5, screenWidth - 12 - 140 - boxWidth - 38);
               return new Rect(x, 5, boxWidth, boxHeight);
            }
         } else {
            return null;
         }
      }
   }

   public static void render(DrawContext graphics) {
      Rect rect = currentRect();
      if (rect != null) {
         TextRenderer font = MinecraftClient.getInstance().textRenderer;
         BattlePanel.draw(graphics, rect.x(), rect.y(), rect.width(), rect.height());
         graphics.drawText(font, BattleState.turnLabel(BattleState.turnNumber), rect.x() + 6, rect.y() + 4, -1, true);
      }
   }

   @Environment(EnvType.CLIENT)
   public static record Rect(int x, int y, int width, int height) {
   }
}
