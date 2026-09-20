/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_332
 */
package com.cobbleclub.client.ui;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(value=EnvType.CLIENT)
public final class Starfield {
    private static final int VIOLET = 12160255;
    private static final int GOLD = 16241514;
    private static final float DRIFT_RATE = 6.0E-6f;

    private Starfield() {
    }

    public static void draw(DrawContext g, int x0, int y0, int x1, int y1, long now, int count, long seed, float baseAlpha) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w > 0 && h > 0) {
            Random r = new Random(seed);
            for (int i = 0; i < count; ++i) {
                float bx = r.nextFloat();
                float by = r.nextFloat();
                int layer = r.nextInt(3);
                float phase = r.nextFloat() * 6.2832f;
                int ctype = r.nextInt(6);
                float drift = (float)(layer + 1) * 6.0E-6f * (float)now;
                float nx = ((bx + drift) % 1.0f + 1.0f) % 1.0f;
                int sx = x0 + (int)(nx * (float)w);
                int sy = y0 + (int)(by * (float)h);
                float tw = (float)Math.sin((double)now * 0.0016 * (double)(0.6f + (float)layer * 0.5f) + (double)phase);
                int a = (int)(baseAlpha * (0.35f + 0.65f * Math.max(0.0f, tw)) * 255.0f);
                if (a <= 6) continue;
                a = Math.min(a, 255);
                int col = ctype == 0 ? 12160255 : (ctype == 1 ? 16241514 : 0xFFFFFF);
                g.fill(sx, sy, sx + 1, sy + 1, a << 24 | col);
                if (layer != 2 || !(tw > 0.85f)) continue;
                int ga = a / 2 << 24 | col;
                g.fill(sx - 1, sy, sx + 2, sy + 1, ga);
                g.fill(sx, sy - 1, sx + 1, sy + 2, ga);
            }
        }
    }
}

