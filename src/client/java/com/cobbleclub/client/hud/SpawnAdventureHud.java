package com.cobbleclub.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class SpawnAdventureHud {
    private static final String SPAWN_DIMENSION = "minecraft:overworld";
    private static final String LEFT_TEXT = "type /wild to find a world";
    private static final String RIGHT_TEXT = "and start your adventure";
    private static final float SCALE = 0.55F;
    private static final int XP_GAP = 12;

    private SpawnAdventureHud() {
    }

    public static void init() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void render(DrawContext g) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || client.world == null
                || client.options.hudHidden
                || !SPAWN_DIMENSION.equals(client.world.getRegistryKey().getValue().toString())) {
            return;
        }

        TextRenderer font = client.textRenderer;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int centerX = screenW / 2;

        float leftVisualW = font.getWidth(LEFT_TEXT) * SCALE;
        int y = screenH - 36;

        // The vanilla XP level stays untouched in the center. The two phrases
        // are independently centered against either side of that number.
        float leftX = centerX - XP_GAP - leftVisualW;
        float rightX = centerX + XP_GAP;

        drawGradient(
                g,
                font,
                LEFT_TEXT,
                leftX,
                y,
                0xA672E6FF,
                0xA6C7CBD2
        );
        drawGradient(
                g,
                font,
                RIGHT_TEXT,
                rightX,
                y,
                0xA6C7CBD2,
                0xA6FFD76A
        );
    }

    private static void drawGradient(
            DrawContext g,
            TextRenderer font,
            String text,
            float screenX,
            float screenY,
            int from,
            int to
    ) {
        g.getMatrices().push();
        g.getMatrices().translate(screenX, screenY, 0.0F);
        g.getMatrices().scale(SCALE, SCALE, 1.0F);

        int x = 0;
        int[] codePoints = text.codePoints().toArray();
        for (int i = 0; i < codePoints.length; ++i) {
            String glyphString = new String(Character.toChars(codePoints[i]));
            Text glyph = Text.literal(glyphString);
            float t = codePoints.length <= 1 ? 0.0F : (float)i / (float)(codePoints.length - 1);
            int color = lerpColor(from, to, t);
            g.drawText(font, glyph, x, 0, color, false);
            x += font.getWidth(glyph);
        }

        g.getMatrices().pop();
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int a = (int)(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int)(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = (int)(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = (int)((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
