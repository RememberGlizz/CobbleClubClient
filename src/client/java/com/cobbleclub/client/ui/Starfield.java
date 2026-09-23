package com.cobbleclub.client.ui;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

/**
 * Retains the existing shared background hook without the old purple/gold starfield.
 * Screens keep their exact layout; the backdrop is now a restrained neutral Minecraft-style texture.
 */
@Environment(EnvType.CLIENT)
public final class Starfield {
    private Starfield() {
    }

    public static void draw(DrawContext g, int x0, int y0, int x1, int y1, long now, int count, long seed, float baseAlpha) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0 || count <= 0) {
            return;
        }

        Random r = new Random(seed);
        int dots = Math.max(8, count / 2);
        int maxAlpha = Math.max(12, Math.min(42, (int)(baseAlpha * 58.0f)));

        for (int i = 0; i < dots; ++i) {
            int sx = x0 + r.nextInt(w);
            int sy = y0 + r.nextInt(h);
            int shade = 92 + r.nextInt(46);
            int alpha = Math.max(8, maxAlpha - r.nextInt(Math.max(1, maxAlpha / 2)));
            int rgb = shade << 16 | shade << 8 | shade;
            g.fill(sx, sy, sx + 1, sy + 1, alpha << 24 | rgb);
        }
    }
}
