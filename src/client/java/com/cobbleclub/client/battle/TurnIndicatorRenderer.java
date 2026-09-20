/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.client.CobblemonClient
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_5348
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.BattlePanel;
import com.cobbleclub.client.battle.BattleState;
import com.cobblemon.mod.common.client.CobblemonClient;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StringVisitable;

@Environment(value=EnvType.CLIENT)
public final class TurnIndicatorRenderer {
    private static final int PADDING_X = 6;
    private static final int MARGIN = 5;
    private static final int TEXT_COLOR = -1;

    private TurnIndicatorRenderer() {
    }

    public static Rect currentRect() {
        if (!BattleConfig.get().turnIndicator) {
            return null;
        }
        int turn = BattleState.turnNumber;
        if (turn > 0 && CobblemonClient.INSTANCE.getBattle() != null && BattleState.battleTilesVisible) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return null;
            }
            TextRenderer font = mc.textRenderer;
            int textWidth = font.getWidth((StringVisitable)BattleState.turnLabel(turn));
            int boxWidth = textWidth + 12;
            Objects.requireNonNull(font);
            int boxHeight = 15;
            int screenWidth = mc.getWindow().getScaledWidth();
            int x = Math.max(screenWidth / 2 + 91 + 5, screenWidth - 12 - 140 - boxWidth - 38);
            return new Rect(x, 5, boxWidth, boxHeight);
        }
        return null;
    }

    public static void render(DrawContext graphics) {
        Rect rect = TurnIndicatorRenderer.currentRect();
        if (rect != null) {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            BattlePanel.draw(graphics, rect.x(), rect.y(), rect.width(), rect.height());
            graphics.drawText(font, BattleState.turnLabel(BattleState.turnNumber), rect.x() + 6, rect.y() + 4, -1, true);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Rect(int x, int y, int width, int height) {
    }
}

