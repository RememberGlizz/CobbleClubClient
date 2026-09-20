/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.types.ElementalType
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 */
package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.types.ElementalType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(value=EnvType.CLIENT)
public final class TypeIconRenderer {
    private static final Identifier TYPES = Identifier.of((String)"cobblemon", (String)"textures/gui/types.png");
    private static final Identifier TYPES_SMALL = Identifier.of((String)"cobblemon", (String)"textures/gui/types_small.png");
    private static final int TYPE_CELL = 36;
    private static final int SMALL_CELL = 18;

    private TypeIconRenderer() {
    }

    public static void drawType(DrawContext graphics, ElementalType type, int x, int y, int size) {
        if (type != null) {
            int physical = (int)Math.round((double)size * MinecraftClient.getInstance().getWindow().getScaleFactor());
            int cell = physical <= 18 ? 18 : 36;
            graphics.drawTexture(cell == 18 ? TYPES_SMALL : TYPES, x, y, size, size, (float)type.getTextureXMultiplier() * (float)cell + 0.1f, 0.0f, cell, cell, cell * 18, cell);
        }
    }

    public static int drawTypes(DrawContext graphics, ElementalType primary, ElementalType secondary, int x, int y, int size, int gap) {
        int drawn = 0;
        if (primary != null) {
            TypeIconRenderer.drawType(graphics, primary, x, y, size);
            ++drawn;
        }
        if (secondary != null) {
            TypeIconRenderer.drawType(graphics, secondary, x + drawn * (size + gap), y, size);
            ++drawn;
        }
        return drawn == 0 ? 0 : drawn * size + (drawn - 1) * gap;
    }
}

