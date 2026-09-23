package com.cobbleclub.client.ui;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

/**
 * Shared CobbleClub menu backdrop.
 *
 * Kept under the original class name so existing screens keep their exact
 * layouts and call sites. The old animated purple star field is intentionally
 * replaced with a restrained Minecraft-style charcoal texture.
 */
@Environment(EnvType.CLIENT)
public final class Starfield {
    private Starfield() {
    }

    public static void draw(
            DrawContext g,
            int x0,
            int y0,
            int x1,
            int y1,
            long now,
            int count,
            long seed,
            float baseAlpha
    ) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) {
            return;
        }

        // Full-screen calls are the backdrop behind a menu. Leave those transparent
        // so the normal Minecraft world/background remains visible instead of being
        // replaced by an opaque charcoal sheet.
        if (x0 == 0 && y0 == 0) {
            return;
        }

        // Embedded preview panes keep the restrained stone texture.
        g.fill(x0, y0, x1, y1, 0xFF232323);
        Random random = new Random(seed);
        int speckles = Math.max(24, Math.min(180, count));
        for (int i = 0; i < speckles; ++i) {
            int sx = x0 + random.nextInt(w);
            int sy = y0 + random.nextInt(h);
            int shade = random.nextBoolean() ? 0xFF2B2B2B : 0xFF1D1D1D;
            g.fill(sx, sy, sx + 1, sy + 1, shade);
        }
    }
}
