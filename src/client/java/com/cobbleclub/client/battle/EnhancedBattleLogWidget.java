/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.client.CobblemonClient
 *  com.cobblemon.mod.common.client.gui.battle.BattleGUI
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1041
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_408
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 *  net.minecraft.class_5481
 *  org.lwjgl.glfw.GLFW
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.BattleLogStore;
import com.cobbleclub.client.battle.BattlePanel;
import com.cobbleclub.client.battle.BattleState;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.OrderedText;
import org.lwjgl.glfw.GLFW;

@Environment(value=EnvType.CLIENT)
public final class EnhancedBattleLogWidget {
    private static final int MIN_W = 90;
    private static final int MIN_H = 40;
    private static final int RESIZE_HANDLE = 10;
    private static final int PAD_X = 3;
    private static final int PAD_Y = 2;
    private static final int ENTRY_GAP = 3;
    private static final int RULE_GAP = 4;
    private static final int TEXT_COLOR = -1;
    private static final int SCROLL_TRACK = 0x18FFFFFF;
    private static final int SCROLL_THUMB = 0x59FFFFFF;
    private static final int DIM_OVERLAY = -1728053248;
    private static boolean dragging;
    private static boolean resizing;
    private static double grabDx;
    private static double grabDy;
    private static int pinnedX;
    private static int pinnedY;
    private static int scrollOffset;
    private static double scrollAccum;
    private static int lastTotalRows;
    private static int cachedRevision;
    private static int cachedWidth;
    private static List<Row> cachedRows;

    private EnhancedBattleLogWidget() {
    }

    private static int lineHeight() {
        Objects.requireNonNull(MinecraftClient.getInstance().textRenderer);
        return 11;
    }

    private static Rect rect() {
        BattleConfig c = BattleConfig.get();
        Window window = MinecraftClient.getInstance().getWindow();
        int screenW = window.getScaledWidth();
        int screenH = window.getScaledHeight();
        int w = Math.min(c.logWidth, screenW);
        int h = Math.min(c.logHeight, screenH);
        int freeW = Math.max(0, screenW - w);
        int freeH = Math.max(0, screenH - h);
        int lift = c.hideNativeLog ? 45 : 135;
        int x = c.logAnchorX >= 0.0 ? (int)Math.round(c.logAnchorX * (double)freeW) : freeW - 2;
        int y = c.logAnchorY >= 0.0 ? (int)Math.round(c.logAnchorY * (double)freeH) : freeH - lift;
        return new Rect(MathHelper.clamp((int)x, (int)0, (int)freeW), MathHelper.clamp((int)y, (int)0, (int)freeH), w, h);
    }

    private static void setAnchor(int x, int y, int w, int h) {
        Window window = MinecraftClient.getInstance().getWindow();
        int freeW = Math.max(0, window.getScaledWidth() - w);
        int freeH = Math.max(0, window.getScaledHeight() - h);
        BattleConfig c = BattleConfig.get();
        c.logAnchorX = freeW == 0 ? 0.0 : MathHelper.clamp((double)((double)x / (double)freeW), (double)0.0, (double)1.0);
        c.logAnchorY = freeH == 0 ? 0.0 : MathHelper.clamp((double)((double)y / (double)freeH), (double)0.0, (double)1.0);
    }

    public static void renderUnderBattle(DrawContext graphics) {
        if (CobblemonClient.INSTANCE.getBattle() != null) {
            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen != null && !(screen instanceof BattleGUI)) {
                if (screen instanceof ChatScreen) {
                    EnhancedBattleLogWidget.render(graphics, true);
                }
            } else {
                EnhancedBattleLogWidget.render(graphics, false);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void render(DrawContext graphics, boolean dimmed) {
        BattleConfig config = BattleConfig.get();
        if (config.enhancedLog) {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            Rect rect = EnhancedBattleLogWidget.rect();
            int x = rect.x();
            int y = rect.y();
            int w = rect.w();
            int h = rect.h();
            BattlePanel.window(graphics, x, y, w, h);
            int inset = 5;
            int contentX = x + inset + 3;
            int contentY = y + inset + 2;
            int contentW = w - (inset + 3) * 2;
            int contentBottom = y + h - inset - 2;
            int var10000 = contentBottom - contentY;
            Objects.requireNonNull(font);
            if (var10000 >= 9) {
                int maxScroll;
                List<Row> rows = EnhancedBattleLogWidget.layout(font, contentW);
                if (scrollOffset > 0) {
                    scrollOffset += Math.max(0, rows.size() - lastTotalRows);
                }
                lastTotalRows = rows.size();
                graphics.enableScissor(x + inset, contentY, x + w - inset, contentBottom);
                try {
                    maxScroll = EnhancedBattleLogWidget.drawRows(graphics, font, rows, contentX, contentY, contentBottom, contentW, scrollOffset);
                }
                finally {
                    graphics.disableScissor();
                }
                scrollOffset = MathHelper.clamp((int)scrollOffset, (int)0, (int)maxScroll);
                if (maxScroll > 0) {
                    int trackH = contentBottom - contentY - 10;
                    EnhancedBattleLogWidget.drawScrollbar(graphics, x + w - inset - 2, contentY, trackH, rows.size(), maxScroll);
                }
            }
            EnhancedBattleLogWidget.drawHandle(graphics, x + w - inset, y + h - inset);
            if (dimmed) {
                graphics.fill(x, y, x + w, y + h, -1728053248);
            }
        }
    }

    private static void drawHandle(DrawContext graphics, int rx, int ry) {
        for (int line = 0; line < 3; ++line) {
            int len = 2 + line * 2;
            for (int i = 0; i <= len; ++i) {
                int px = rx - 2 - i;
                int py = ry - 2 - (len - i);
                graphics.fill(px, py, px + 1, py + 1, -7500403);
            }
        }
    }

    private static List<Row> layout(TextRenderer font, int width) {
        int revision = BattleLogStore.revision();
        if (revision == cachedRevision && width == cachedWidth) {
            return cachedRows;
        }
        ArrayList<Row> rows = new ArrayList<Row>();
        for (BattleLogStore.Entry entry : BattleLogStore.snapshot()) {
            if (entry.turnMarker()) {
                rows.add(EnhancedBattleLogWidget.separatorRow(entry.turn()));
                continue;
            }
            boolean gapAbove = !rows.isEmpty() && !((Row)rows.get(rows.size() - 1)).separator();
            boolean firstLine = true;
            for (OrderedText seq : font.wrapLines((StringVisitable)entry.text(), Math.max(1, width))) {
                rows.add(new Row(seq, false, firstLine && gapAbove));
                firstLine = false;
            }
        }
        cachedRevision = revision;
        cachedWidth = width;
        cachedRows = rows;
        return rows;
    }

    private static Row separatorRow(int turn) {
        return turn <= 0 ? new Row(null, true, false) : new Row(BattleState.turnLabel(turn).asOrderedText(), true, false);
    }

    private static int rowHeight(Row row, int lineH) {
        return row.separator() ? lineH + 2 : lineH + (row.entryStart() ? 3 : 0);
    }

    private static int drawRows(DrawContext graphics, TextRenderer font, List<Row> rows, int x, int top, int bottom, int width, int scrollOffset) {
        Row row;
        int rh;
        int lineH = EnhancedBattleLogWidget.lineHeight();
        int total = rows.size();
        int totalHeight = 0;
        for (Row row2 : rows) {
            totalHeight += EnhancedBattleLogWidget.rowHeight(row2, lineH);
        }
        bottom = Math.min(bottom, top + totalHeight);
        int available = bottom - top;
        int acc = 0;
        int fit = 0;
        for (int i = total - 1; i >= 0 && ((acc += EnhancedBattleLogWidget.rowHeight(rows.get(i), lineH)) <= available || fit <= 0); ++fit, --i) {
        }
        int maxScroll = Math.max(0, total - fit);
        scrollOffset = MathHelper.clamp((int)scrollOffset, (int)0, (int)maxScroll);
        int rowY = bottom;
        for (int i = total - 1 - scrollOffset; i >= 0 && (rowY -= (rh = EnhancedBattleLogWidget.rowHeight(row = rows.get(i), lineH))) + rh > top; --i) {
            EnhancedBattleLogWidget.drawRow(graphics, font, row, x, rowY, rh, width);
        }
        return maxScroll;
    }

    private static void drawRow(DrawContext graphics, TextRenderer font, Row row, int x, int rowY, int rh, int width) {
        if (row.separator()) {
            EnhancedBattleLogWidget.drawSeparator(graphics, font, row, x, rowY, rh, width);
        } else {
            graphics.drawTextWithShadow(font, row.seq(), x, rowY + (row.entryStart() ? 3 : 0), -1);
        }
    }

    private static void drawSeparator(DrawContext graphics, TextRenderer font, Row row, int x, int rowY, int rh, int width) {
        int lineY = rowY + rh / 2;
        if (row.seq() == null) {
            graphics.fill(x, lineY, x + width, lineY + 1, -6052957);
        } else {
            int labelWidth = font.getWidth(row.seq());
            int textX = x + (width - labelWidth) / 2;
            graphics.fill(x, lineY, Math.max(x, textX - 4), lineY + 1, -6052957);
            graphics.fill(Math.min(x + width, textX + labelWidth + 4), lineY, x + width, lineY + 1, -6052957);
            OrderedText var10002 = row.seq();
            Objects.requireNonNull(font);
            graphics.drawTextWithShadow(font, var10002, textX, rowY + (rh - 9) / 2 + 1, -1);
        }
    }

    private static void drawScrollbar(DrawContext graphics, int trackX, int trackY, int trackH, int total, int maxScroll) {
        if (trackH >= 4) {
            graphics.fill(trackX, trackY, trackX + 1, trackY + trackH, 0x18FFFFFF);
            int fit = Math.max(1, total - maxScroll);
            int thumbH = MathHelper.clamp((int)(trackH * fit / Math.max(1, total)), (int)6, (int)trackH);
            float scrolled = maxScroll == 0 ? 0.0f : (float)scrollOffset / (float)maxScroll;
            int thumbY = trackY + Math.round((float)(trackH - thumbH) * (1.0f - scrolled));
            graphics.fill(trackX, thumbY, trackX + 1, thumbY + thumbH, 0x59FFFFFF);
        }
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (BattleConfig.get().enhancedLog && button == 0) {
            Rect rect = EnhancedBattleLogWidget.rect();
            int x = rect.x();
            int y = rect.y();
            int w = rect.w();
            int h = rect.h();
            if (!(mouseX < (double)x || mouseX > (double)(x + w) || mouseY < (double)y || mouseY > (double)(y + h))) {
                pinnedX = x;
                pinnedY = y;
                if (mouseX >= (double)(x + w - 10) && mouseY >= (double)(y + h - 10)) {
                    resizing = true;
                    grabDx = (double)(x + w) - mouseX;
                    grabDy = (double)(y + h) - mouseY;
                } else {
                    dragging = true;
                    grabDx = mouseX - (double)x;
                    grabDy = mouseY - (double)y;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public static boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        BattleConfig config = BattleConfig.get();
        if (dragging) {
            Rect rect = EnhancedBattleLogWidget.rect();
            EnhancedBattleLogWidget.setAnchor(MathHelper.clamp((int)((int)(mouseX - grabDx)), (int)0, (int)Math.max(0, screenW - rect.w())), MathHelper.clamp((int)((int)(mouseY - grabDy)), (int)0, (int)Math.max(0, screenH - rect.h())), rect.w(), rect.h());
            return true;
        }
        if (resizing) {
            config.logWidth = MathHelper.clamp((int)((int)(mouseX + grabDx - (double)pinnedX)), (int)90, (int)Math.max(90, screenW - pinnedX));
            config.logHeight = MathHelper.clamp((int)((int)(mouseY + grabDy - (double)pinnedY)), (int)40, (int)Math.max(40, screenH - pinnedY));
            EnhancedBattleLogWidget.setAnchor(pinnedX, pinnedY, config.logWidth, config.logHeight);
            return true;
        }
        return false;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (BattleConfig.get().enhancedLog && vertical != 0.0) {
            Rect rect = EnhancedBattleLogWidget.rect();
            if (!(mouseX < (double)rect.x() || mouseX > (double)(rect.x() + rect.w()) || mouseY < (double)rect.y() || mouseY > (double)(rect.y() + rect.h()))) {
                scrollOffset = Math.max(0, scrollOffset + EnhancedBattleLogWidget.accumulateScroll(vertical));
                return true;
            }
            return false;
        }
        return false;
    }

    private static int accumulateScroll(double vertical) {
        int notches = (int)(scrollAccum += vertical);
        scrollAccum -= (double)notches;
        return notches * 2;
    }

    public static void resetScroll() {
        scrollOffset = 0;
        scrollAccum = 0.0;
    }

    public static void tick() {
        if ((dragging || resizing) && GLFW.glfwGetMouseButton((long)MinecraftClient.getInstance().getWindow().getHandle(), (int)0) == 0) {
            dragging = false;
            resizing = false;
            BattleConfig.save();
        }
    }

    static {
        cachedRevision = -1;
        cachedWidth = -1;
        cachedRows = List.of();
    }

    @Environment(value=EnvType.CLIENT)
    private record Rect(int x, int y, int w, int h) {
    }

    @Environment(value=EnvType.CLIENT)
    private record Row(OrderedText seq, boolean separator, boolean entryStart) {
    }
}

