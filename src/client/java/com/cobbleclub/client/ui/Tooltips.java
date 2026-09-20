/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 *  net.minecraft.class_5481
 */
package com.cobbleclub.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.OrderedText;

@Environment(value=EnvType.CLIENT)
public final class Tooltips {
    private static final int MAX_WIDTH = 200;
    private static final int MIN_WIDTH = 120;

    private Tooltips() {
    }

    public static void render(DrawContext g, TextRenderer font, List<Text> lines, int mouseX, int mouseY) {
        g.drawOrderedTooltip(font, Tooltips.wrap(font, lines, g.getScaledWindowWidth()), mouseX, mouseY);
    }

    public static void render(DrawContext g, TextRenderer font, Text line, int mouseX, int mouseY) {
        Tooltips.render(g, font, List.of(line), mouseX, mouseY);
    }

    public static void render(DrawContext g, TextRenderer font, ItemStack stack, int mouseX, int mouseY) {
        Tooltips.render(g, font, Screen.getTooltipFromItem((MinecraftClient)MinecraftClient.getInstance(), (ItemStack)stack), mouseX, mouseY);
    }

    private static List<OrderedText> wrap(TextRenderer font, List<Text> lines, int guiWidth) {
        int width = Math.max(120, Math.min(200, guiWidth / 3));
        ArrayList<OrderedText> wrapped = new ArrayList<OrderedText>(lines.size());
        for (Text line : lines) {
            List split = font.wrapLines((StringVisitable)line, width);
            if (split.isEmpty()) {
                wrapped.add(OrderedText.EMPTY);
                continue;
            }
            wrapped.addAll(split);
        }
        return wrapped;
    }
}

