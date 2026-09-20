/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.CobblemonSounds
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1109
 *  net.minecraft.class_1113
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_2561$class_2562
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 *  net.minecraft.class_5250
 *  net.minecraft.class_6880
 *  net.minecraft.class_7225$class_7874
 */
package com.cobbleclub.client.ui;

import com.cobblemon.mod.common.CobblemonSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryWrapper;

@Environment(value=EnvType.CLIENT)
public final class PreviewUi {
    private PreviewUi() {
    }

    public static RegistryWrapper.WrapperLookup registry() {
        return MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getRegistryManager() : null;
    }

    public static Text deserialize(String json, String fallback) {
        RegistryWrapper.WrapperLookup reg = PreviewUi.registry();
        if (json != null && reg != null) {
            try {
                MutableText parsed = Text.Serialization.fromJson((String)json, (RegistryWrapper.WrapperLookup)reg);
                if (parsed != null) {
                    return parsed;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return Text.literal((String)(fallback != null ? fallback : ""));
    }

    public static void playClick() {
        MinecraftClient.getInstance().getSoundManager().play((SoundInstance)PositionedSoundInstance.master((RegistryEntry)SoundEvents.UI_BUTTON_CLICK, (float)1.0f));
    }

    public static void playOpen() {
        MinecraftClient.getInstance().getSoundManager().play((SoundInstance)PositionedSoundInstance.master((SoundEvent)CobblemonSounds.PC_ON, (float)0.8f, (float)0.3f));
    }

    public static void playClose() {
        MinecraftClient.getInstance().getSoundManager().play((SoundInstance)PositionedSoundInstance.master((SoundEvent)CobblemonSounds.PC_OFF, (float)1.0f, (float)0.3f));
    }

    public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
    }

    public static void renderScaledItem(DrawContext g, ItemStack stack, int itemX, int itemY, float scale) {
        float cx = itemX + 8;
        float cy = itemY + 8;
        g.getMatrices().push();
        g.getMatrices().translate((double)cx, (double)cy, 0.0);
        g.getMatrices().scale(scale, scale, 1.0f);
        g.getMatrices().translate((double)(-cx), (double)(-cy), 0.0);
        g.drawItem(stack, itemX, itemY);
        g.getMatrices().pop();
    }
}

