/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 */
package com.cobbleclub.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(value=EnvType.CLIENT)
public final class GuiDepth {
    private GuiDepth() {
    }

    public static void clearForOverlay(DrawContext guiGraphics) {
        guiGraphics.draw();
        RenderSystem.clear((int)256, (boolean)MinecraftClient.IS_SYSTEM_MAC);
    }
}

