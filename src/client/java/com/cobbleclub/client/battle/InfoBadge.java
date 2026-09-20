/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 */
package com.cobbleclub.client.battle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(value=EnvType.CLIENT)
public final class InfoBadge {
    public static final int SIZE = 8;
    private static final int[] FRAMES = new int[]{32, 24, 16, 8};
    private static final Identifier[] TEXTURES = new Identifier[FRAMES.length];

    private InfoBadge() {
    }

    public static void draw(DrawContext graphics, int x, int y, boolean hovered, float alpha) {
        int i = InfoBadge.frameIndexFor(MinecraftClient.getInstance().getWindow().getScaleFactor());
        int frame = FRAMES[i];
        graphics.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.drawTexture(TEXTURES[i], x, y, 8, 8, hovered ? (float)frame : 0.0f, 0.0f, frame, frame, frame * 2, frame);
        graphics.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static int frameIndexFor(double guiScale) {
        int i;
        int physical = 8 * Math.max(1, (int)Math.round(guiScale));
        int dense = -1;
        for (i = 0; i < FRAMES.length; ++i) {
            if (FRAMES[i] < physical) continue;
            dense = i;
        }
        if (dense >= 0) {
            return dense;
        }
        for (i = 0; i < FRAMES.length; ++i) {
            if (physical % FRAMES[i] != 0 || physical / FRAMES[i] > 2) continue;
            return i;
        }
        return 0;
    }

    public static boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= (double)x && mouseX <= (double)(x + 8) && mouseY >= (double)y && mouseY <= (double)(y + 8);
    }

    static {
        for (int i = 0; i < FRAMES.length; ++i) {
            int var10003 = FRAMES[i];
            InfoBadge.TEXTURES[i] = Identifier.of((String)"cobbleclub", (String)("textures/gui/battle/info_badge_" + var10003 + ".png"));
        }
    }
}

