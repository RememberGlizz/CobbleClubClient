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
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_4068
 *  net.minecraft.class_4185
 *  net.minecraft.class_4185$class_4241
 *  net.minecraft.class_437
 */
package com.cobbleclub.client.wild;

import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.wild.WildNetworking;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;

@Environment(value=EnvType.CLIENT)
public final class WildScreen
extends Screen {
    private static final int PANEL_W = 402;
    private static final int PANEL_H = 211;
    private static final int BUTTON_W = 118;
    private static final int BUTTON_H = 36;
    private static final int GAP_X = 8;
    private static final int GAP_Y = 6;
    private static final List<String> CANONICAL_WORLDS = List.of("purpleworld", "redworld", "orangeworld", "yellowworld", "resource", "blueworld", "pinkworld", "greenworld", "cyanworld");
    private final String subtitle;
    private final List<String> worlds;
    private int left;
    private int top;

    public WildScreen(String subtitle, List<String> worlds) {
        super((Text)Text.literal((String)"CobbleClub Wild Worlds"));
        this.subtitle = subtitle == null ? "" : subtitle;
        this.worlds = WildScreen.mergedWorlds(worlds);
    }

    private static List<String> mergedWorlds(List<String> advertised) {
        LinkedHashSet<String> merged = new LinkedHashSet<String>(CANONICAL_WORLDS);
        if (advertised != null) {
            for (String world : advertised) {
                if (world == null || world.isBlank()) continue;
                merged.add(world.trim());
            }
        }
        return List.copyOf(new ArrayList<String>(merged));
    }

    protected void init() {
        this.left = (this.width - 402) / 2;
        this.top = (this.height - 211) / 2;
        int gridW = 370;
        int x0 = this.left + (402 - gridW) / 2;
        int y0 = this.top + 58;
        if (this.worlds.isEmpty()) {
            return;
        }
        for (int i = 0; i < this.worlds.size(); ++i) {
            String world = this.worlds.get(i);
            int col = i % 3;
            int row = i / 3;
            WorldStyle style = WildScreen.styleFor(world);
            this.addWorld(x0 + col * 126, y0 + row * 42, style.label(), world, style.accent(), style.fill(), style.icon());
        }
    }

    private void addWorld(int x, int y, String label, String world, int accent, int fill, ItemStack icon) {
        this.addDrawableChild(new WorldButton(x, y, 118, 36, (Text)Text.literal((String)label), accent, fill, icon, button -> {
            WildNetworking.select(world);
            if (this.client != null) {
                this.client.setScreen(null);
            }
        }));
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0x52000000);
        Starfield.draw(g, 0, 0, this.width, this.height, System.currentTimeMillis(), 105, 202093036L, 0.52f);
        g.fill(this.left, this.top, this.left + 402, this.top + 211, -196589496);
        g.fill(this.left + 1, this.top + 1, this.left + 402 - 1, this.top + 2, -7434610);
        g.drawBorder(this.left, this.top, 402, 211, -15658735);
        g.fill(this.left + 14, this.top + 47, this.left + 402 - 14, this.top + 48, -10461088);
        int count = this.worlds.size();
        g.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.literal((String)("\u2726 COBBLECLUB WILD WORLDS v5 \u2022 " + count + " \u2726")), this.left + 201, this.top + 16, -723724);
        Object line = this.subtitle.isBlank() ? count + " managed wild worlds" : this.subtitle;
        g.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.literal((String)line), this.left + 201, this.top + 33, -5197648);
        String footer = this.worlds.isEmpty() ? "No managed wild worlds are currently advertised by the server" : "Pick a world \u2022 safe random teleport \u2022 3 second warmup";
        g.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.literal((String)footer), this.left + 201, this.top + 211 - 17, -5197648);
        for (Element child : this.children()) {
            if (!(child instanceof Drawable)) continue;
            Drawable drawable = (Drawable)child;
            drawable.render(g, mouseX, mouseY, delta);
        }
    }

    public boolean shouldPause() {
        return false;
    }

    private static WorldStyle styleFor(String world) {
        String value;
        return switch (value = world == null ? "" : world.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "")) {
            case "purple", "purpleworld" -> new WorldStyle("Purple World", -7513112, -14083005, new ItemStack((ItemConvertible)Items.PURPLE_WOOL));
            case "red", "redworld" -> new WorldStyle("Red World", -1877405, -12379100, new ItemStack((ItemConvertible)Items.RED_WOOL));
            case "orange", "orangeworld" -> new WorldStyle("Orange World", -1009078, -12375782, new ItemStack((ItemConvertible)Items.ORANGE_WOOL));
            case "yellow", "yellowworld" -> new WorldStyle("Yellow World", -993956, -12634084, new ItemStack((ItemConvertible)Items.YELLOW_WOOL));
            case "resource", "resources", "resourceworld" -> new WorldStyle("Resource World", -3550757, -14341323, new ItemStack((ItemConvertible)Items.LIGHT_GRAY_WOOL));
            case "blue", "blueworld" -> new WorldStyle("Blue World", -10966799, -15061949, new ItemStack((ItemConvertible)Items.BLUE_WOOL));
            case "pink", "pinkworld" -> new WorldStyle("Pink World", -885320, -12312520, new ItemStack((ItemConvertible)Items.PINK_WOOL));
            case "green", "greenworld" -> new WorldStyle("Green World", -10366325, -14992086, new ItemStack((ItemConvertible)Items.GREEN_WOOL));
            case "cyan", "cyanworld" -> new WorldStyle("Cyan World", -10624792, -15123390, new ItemStack((ItemConvertible)Items.CYAN_WOOL));
            default -> new WorldStyle(WildScreen.prettyName(world), -6519353, -14147535, new ItemStack((ItemConvertible)Items.WHITE_WOOL));
        };
    }

    private static String prettyName(String world) {
        if (world == null || world.isBlank()) {
            return "Wild World";
        }
        String value = world.replace('_', ' ').replace('-', ' ').trim();
        StringBuilder out = new StringBuilder();
        boolean upper = true;
        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c)) {
                out.append(' ');
                upper = true;
                continue;
            }
            out.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return out.toString();
    }

    @Environment(value=EnvType.CLIENT)
    private record WorldStyle(String label, int accent, int fill, ItemStack icon) {
    }

    @Environment(value=EnvType.CLIENT)
    private static final class WorldButton
    extends ButtonWidget {
        private final int accent;
        private final int fill;
        private final ItemStack icon;

        private WorldButton(int x, int y, int width, int height, Text message, int accent, int fill, ItemStack icon, ButtonWidget.PressAction action) {
            super(x, y, width, height, message, action, DEFAULT_NARRATION_SUPPLIER);
            this.accent = accent;
            this.fill = fill;
            this.icon = icon;
        }

        protected void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
            boolean hover = this.isHovered() && this.active;
            int x0 = this.getX();
            int y0 = this.getY();
            int x1 = x0 + this.getWidth();
            int y1 = y0 + this.getHeight();
            int base = hover ? 0xFF777777 : 0xFF666666;
            int edge = hover ? 0xFFE0E0E0 : 0xFF171717;
            g.fill(x0, y0, x1, y1, edge);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, base);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0xFFA8A8A8);
            g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, 0xFFA8A8A8);
            g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, 0xFF343434);
            g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, 0xFF343434);
            g.fill(x0 + 7, y1 - 5, x1 - 7, y1 - 3, this.accent);
            g.drawItem(this.icon, x0 + 8, y0 + 10);
            g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(), x0 + 29, y0 + 14, -1);
        }

        private static int lighten(int color) {
            int a = color & 0xFF000000;
            int r = Math.min(255, (color >> 16 & 0xFF) + 18);
            int g = Math.min(255, (color >> 8 & 0xFF) + 18);
            int b = Math.min(255, (color & 0xFF) + 18);
            return a | r << 16 | g << 8 | b;
        }

        private static int darken(int color) {
            int a = color & 0xFF000000;
            int r = (color >> 16 & 0xFF) * 3 / 4;
            int g = (color >> 8 & 0xFF) * 3 / 4;
            int b = (color & 0xFF) * 3 / 4;
            return a | r << 16 | g << 8 | b;
        }
    }
}

