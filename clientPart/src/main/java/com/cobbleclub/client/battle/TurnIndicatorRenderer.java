package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

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
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 == null) {
               return null;
            } else {
               class_327 font = mc.field_1772;
               int textWidth = font.method_27525(BattleState.turnLabel(turn));
               int boxWidth = textWidth + 12;
               Objects.requireNonNull(font);
               int boxHeight = 9 + 4 + 2;
               int screenWidth = mc.method_22683().method_4486();
               int x = Math.max(screenWidth / 2 + 91 + 5, screenWidth - 12 - 140 - boxWidth - 38);
               return new Rect(x, 5, boxWidth, boxHeight);
            }
         } else {
            return null;
         }
      }
   }

   public static void render(class_332 graphics) {
      Rect rect = currentRect();
      if (rect != null) {
         class_327 font = class_310.method_1551().field_1772;
         BattlePanel.draw(graphics, rect.x(), rect.y(), rect.width(), rect.height());
         graphics.method_51439(font, BattleState.turnLabel(BattleState.turnNumber), rect.x() + 6, rect.y() + 4, -1, true);
      }
   }

   @Environment(EnvType.CLIENT)
   public static record Rect(int x, int y, int width, int height) {
   }
}
