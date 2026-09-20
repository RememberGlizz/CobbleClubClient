/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_4185
 *  net.minecraft.class_4185$class_4241
 *  net.minecraft.class_5348
 *  net.minecraft.class_7919
 *  net.minecraft.class_8666
 */
package com.cobbleclub.client.wardrobe;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.StringVisitable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.ButtonTextures;

@Environment(value=EnvType.CLIENT)
public class WardrobeButton
extends ButtonWidget {
    public static final int HEIGHT = 20;
    private static final int PAD_X = 5;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEXT_GAP = 3;
    private static final int LABEL_COLOR = -2570241;
    private static final int TOOLTIP_TITLE_COLOR = 13215999;
    private static final int TOOLTIP_DESC_COLOR = 0xA8A8B8;
    private static final ItemStack ICON = new ItemStack((ItemConvertible)Items.ARMOR_STAND);
    private static final Text LABEL = Text.translatable((String)"cobbleclub.wardrobe.button");
    private static final ButtonTextures BUTTON_SPRITES = new ButtonTextures(Identifier.ofVanilla((String)"widget/button"), Identifier.ofVanilla((String)"widget/button_disabled"), Identifier.ofVanilla((String)"widget/button_highlighted"));

    public WardrobeButton(ButtonWidget.PressAction onPress) {
        super(0, 0, WardrobeButton.width(MinecraftClient.getInstance().textRenderer), 20, LABEL, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.setTooltip(Tooltip.of((Text)Text.translatable((String)"cobbleclub.wardrobe.button").withColor(13215999).append((Text)Text.literal((String)"\n")).append((Text)Text.translatable((String)"cobbleclub.wardrobe.tooltip").withColor(0xA8A8B8))));
    }

    private static int width(TextRenderer font) {
        return 24 + font.getWidth((StringVisitable)LABEL) + 5;
    }

    protected void renderWidget(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        graphics.drawGuiTexture(BUTTON_SPRITES.get(this.active, this.isSelected()), this.getX(), this.getY(), this.getWidth(), 20);
        graphics.drawItem(ICON, this.getX() + 5, this.getY() + 2);
        Text var10002 = LABEL;
        int var10003 = this.getX() + 5 + 16 + 3;
        int var10004 = this.getY();
        Objects.requireNonNull(font);
        graphics.drawText(font, var10002, var10003, var10004 + 5 + 1, -2570241, true);
    }
}

