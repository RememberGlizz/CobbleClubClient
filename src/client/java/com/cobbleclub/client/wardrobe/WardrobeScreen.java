/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeCosmeticEntry
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeGlowEntry
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobePresetSummary
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_124
 *  net.minecraft.class_156
 *  net.minecraft.class_1767
 *  net.minecraft.class_2561
 *  net.minecraft.class_2561$class_2562
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_4185$class_4241
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  net.minecraft.class_7225$class_7874
 */
package com.cobbleclub.client.wardrobe;

import com.cobbleclub.client.ui.GuiDepth;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeCosmeticEntry;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeGlowEntry;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobePresetSummary;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.DyeColor;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.registry.RegistryWrapper;

@Environment(EnvType.CLIENT)
public class WardrobeScreen extends Screen {
    private static final int PANEL_W = 416;
    private static final int PANEL_H = 224;
    private static final int GRID_COLS = 8;
    private static final int CELL = 24;
    private static final int GRID_ROWS = 5;
    private static final float ICON_SCALE = 1.35f;
    private static final int PRESET_TILE = 16;
    private static final int PRESET_GAP = 2;
    private static final float AUTO_SPIN_DEG_PER_MS = 0.03f;
    private static final long AUTO_SPIN_IDLE_MS = 1500L;
    private static final float MAX_PITCH = 20.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.6f;
    private static final float DEFAULT_ZOOM = 0.82f;
    private static final float ZOOM_STEP = 1.12f;
    private static final int PANEL_TOP = -199614136;
    private static final int PANEL_BOTTOM = -200601562;
    private static final int PANEL_BORDER_OUT = -16447985;
    private static final int PANEL_BORDER_IN = -13747610;
    private static final int HEADER_TOP = -14405538;
    private static final int HEADER_BOTTOM = -15459782;
    private static final int ACCENT = -6467875;
    private static final int ACCENT_DIM = -9815394;
    private static final int STARLIGHT = -535702;
    private static final int EQUIPPED = -12474273;
    private static final int TITLE_COLOR = -2053377;
    private static final int MUTED_TEXT = -7035976;
    private static final int PREVIEW_TOP = -14998448;
    private static final int PREVIEW_BOTTOM = -16315880;
    private static final int SLOT_BASE = -15064506;
    private static final int SLOT_SHADOW = -16118238;
    private static final int SLOT_LIGHT = -13747610;
    private static final int CONTENT_BG = -15723477;
    private static final int SCROLL_TRACK = -15986650;
    private static final int SCROLL_THUMB = -10862962;
    private static final int EQUIPPED_FILL = 675391583;
    private static final int SELECTED_FILL = 681397981;
    private final WardrobeState state = WardrobeState.get();
    private final WardrobePreviewRenderer preview = new WardrobePreviewRenderer();
    private WardrobeSlot activeSlot;
    private boolean glowTab;
    private boolean floatyTab;
    private int scrollRow;
    private float yaw;
    private float pitch;
    private float previewZoom = 0.82f;
    private boolean draggingPreview;
    private long lastInteractMs;
    private long lastFrameMs = Util.getMeasuringTimeMs();
    private String hoverAnimId;
    private long hoverAnimStart;
    private String equipAnimId;
    private long equipAnimStart;
    private float tabIndicatorX = -1.0f;
    private float tabIndicatorW;
    private long gridRevealStart;
    private final Map<WardrobeSlot, ButtonWidget> tabButtons = new EnumMap<>(WardrobeSlot.class);
    private ButtonWidget glowTabButton;
    private ButtonWidget floatyTabButton;
    private List<WardrobeCosmeticEntry> orderedCache;
    private WardrobeSlot orderedCacheSlot;
    private boolean orderedCacheFloatyTab;

    public WardrobeScreen(WardrobeSlot initialSlot) {
        super(Text.literal("Wardrobe"));
        this.activeSlot = initialSlot != null ? initialSlot : WardrobeSlot.HELMET;
        this.gridRevealStart = Util.getMeasuringTimeMs();
        PreviewUi.playOpen();
    }

    private int panelLeft() {
        return (this.width - 416) / 2;
    }

    private int panelTop() {
        return (this.height - 224) / 2;
    }

    private int previewX0() {
        return this.panelLeft() + 8;
    }

    private int previewY0() {
        return this.panelTop() + 22;
    }

    private int previewX1() {
        return this.panelLeft() + 168;
    }

    private int previewY1() {
        return this.panelTop() + 224 - 28;
    }

    private int gridX() {
        return this.panelLeft() + 176;
    }

    private int gridY() {
        return this.panelTop() + 46;
    }

    private int swatchY() {
        return this.gridY() + this.visibleRows() * 24 + 8;
    }

    @Override
    protected void init() {
        this.tabButtons.clear();
        int tabX = this.gridX();
        for (WardrobeSlot slot : WardrobeSlot.values()) {
            if (slot == WardrobeSlot.BALLOON) {
                MutableText floatyLabel = Text.literal("Floaties");
                int fw = this.textRenderer.getWidth(floatyLabel) + 8;
                this.floatyTabButton = new TabButton(tabX, this.panelTop() + 22, fw, 18, floatyLabel, b -> this.selectFloatyTab());
                this.addDrawableChild(this.floatyTabButton);
                tabX += fw + 3;
            }
            MutableText label = Text.literal(this.state.slotDisplayName(slot));
            int w = this.textRenderer.getWidth(label) + 8;
            TabButton tab = new TabButton(tabX, this.panelTop() + 22, w, 18, label, b -> this.selectTab(slot));
            this.tabButtons.put(slot, tab);
            this.addDrawableChild(tab);
            tabX += w + 3;
        }
        if (!this.state.glows().isEmpty()) {
            MutableText glowLabel = Text.literal("Glow");
            this.glowTabButton = new TabButton(tabX, this.panelTop() + 22, this.textRenderer.getWidth(glowLabel) + 8, 18, glowLabel, b -> this.selectGlowTab());
            this.addDrawableChild(this.glowTabButton);
        } else {
            this.glowTabButton = null;
            this.glowTab = false;
        }
        this.updateTabActiveFlags();
        int footerY = this.panelTop() + 224 - 24;
        this.addDrawableChild(new ThemedButton(this.panelLeft() + 416 - 264, footerY, 86, 20, Text.literal("Unequip All"), b -> {
            this.state.unequipAllLocal();
            WardrobeNetworking.sendUnequipAll();
        }));
        this.addDrawableChild(new ThemedButton(this.panelLeft() + 416 - 172, footerY, 106, 20, this.hideLabel(), b -> {
            boolean nowHidden = !this.state.isHidden();
            this.state.setHiddenLocal(nowHidden);
            WardrobeNetworking.sendSetHidden(nowHidden);
            b.setMessage(this.hideLabel());
        }));
        this.addDrawableChild(new ThemedButton(this.panelLeft() + 416 - 60, footerY, 52, 20, Text.literal("Close"), b -> this.close()));
    }

    private void updateTabActiveFlags() {
        this.tabButtons.forEach((s, button) -> {
            button.active = this.glowTab || this.floatyTab || s != this.activeSlot;
        });
        if (this.glowTabButton != null) {
            this.glowTabButton.active = !this.glowTab;
        }
        if (this.floatyTabButton != null) {
            this.floatyTabButton.active = !this.floatyTab;
        }
    }

    private ButtonWidget activeTabButton() {
        return this.glowTab ? this.glowTabButton : (this.floatyTab ? this.floatyTabButton : this.tabButtons.get(this.activeSlot));
    }

    private Text hideLabel() {
        return this.state.isHidden() ? Text.literal("Cosmetics: Hidden").formatted(Formatting.RED) : Text.literal("Cosmetics: Shown").formatted(Formatting.GREEN);
    }

    private void selectTab(WardrobeSlot slot) {
        this.state.setHover(this.activeSlot, null);
        this.state.setHoverGlow(null);
        this.glowTab = false;
        this.floatyTab = false;
        this.activeSlot = slot;
        this.scrollRow = 0;
        this.gridRevealStart = Util.getMeasuringTimeMs();
        this.updateTabActiveFlags();
    }

    private void selectGlowTab() {
        this.state.setHover(this.activeSlot, null);
        this.glowTab = true;
        this.floatyTab = false;
        this.scrollRow = 0;
        this.gridRevealStart = Util.getMeasuringTimeMs();
        this.updateTabActiveFlags();
    }

    private void selectFloatyTab() {
        this.state.setHover(this.activeSlot, null);
        this.state.setHoverGlow(null);
        this.glowTab = false;
        this.floatyTab = true;
        this.activeSlot = WardrobeSlot.BALLOON;
        this.scrollRow = 0;
        this.orderedCache = null;
        this.gridRevealStart = Util.getMeasuringTimeMs();
        this.updateTabActiveFlags();
    }

    private static boolean isFloatyEntry(WardrobeCosmeticEntry entry) {
        if (entry == null || entry.getId() == null) {
            return false;
        }
        return entry.getId().toLowerCase(Locale.ROOT).contains("floaty");
    }

    public void onStateRefreshed() {
        this.orderedCache = null;
        for (WardrobeSlot slot : WardrobeSlot.values()) {
            if (this.state.entry(this.state.getSelected(slot)) != null) continue;
            this.state.setSelected(slot, null);
        }
        this.scrollRow = MathHelper.clamp(this.scrollRow, 0, this.maxScrollRow());
        this.clearAndInit();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int left = this.panelLeft();
        int top = this.panelTop();
        int right = left + 416;
        int bottom = top + 224;
        Starfield.draw(guiGraphics, 0, 0, this.width, this.height, Util.getMeasuringTimeMs(), 44, 1337L, 0.5f);
        guiGraphics.fill(left - 3, top - 3, right + 3, bottom + 3, 0x55000000);
        guiGraphics.fillGradient(left, top, right, bottom, -199614136, -200601562);
        guiGraphics.fillGradient(left, top, right, top + 18, -14405538, -15459782);
        guiGraphics.fill(left, top + 17, right, top + 18, -9815394);
        guiGraphics.drawBorder(left, top, 416, 224, -16447985);
        guiGraphics.drawBorder(left + 1, top + 1, 414, 222, -13747610);
        guiGraphics.drawCenteredTextWithShadow(this.textRenderer, this.title, left + 208, top + 5, -2053377);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        long now = Util.getMeasuringTimeMs();
        long frameDeltaMs = now - this.lastFrameMs;
        if (!this.draggingPreview && now - this.lastInteractMs > 1500L) {
            this.yaw = (this.yaw + (float)frameDeltaMs * 0.03f) % 360.0f;
        }
        this.lastFrameMs = now;
        this.renderTabIndicator(guiGraphics, frameDeltaMs);
        WardrobeCosmeticEntry hovered = this.glowTab ? null : this.hoveredEntry(mouseX, mouseY);
        String hoveredId = hovered != null ? hovered.getId() : null;
        this.state.setHover(this.activeSlot, hoveredId);
        if (!Objects.equals(hoveredId, this.hoverAnimId)) {
            this.hoverAnimId = hoveredId;
            this.hoverAnimStart = now;
        }
        WardrobeGlowEntry hoveredGlow = this.glowTab ? this.hoveredGlow(mouseX, mouseY) : null;
        this.state.setHoverGlow(hoveredGlow != null ? hoveredGlow.getId() : null);
        WardrobePresetSummary hoveredPreset = this.hoveredPreset(mouseX, mouseY);
        this.state.setPresetPreview(hoveredPreset != null && hoveredPreset.getFilled() && !hoveredPreset.getLocked() ? hoveredPreset : null);
        int px0 = this.previewX0();
        int py0 = this.previewY0();
        int px1 = this.previewX1();
        int py1 = this.previewY1();
        guiGraphics.fillGradient(px0, py0, px1, py1, -14998448, -16315880);
        guiGraphics.enableScissor(px0, py0, px1, py1);
        Starfield.draw(guiGraphics, px0, py0, px1, py1, now, 34, 91L, 0.85f);
        guiGraphics.disableScissor();
        guiGraphics.fillGradient(px0, py1 - 46, px1, py1, 0, 0x66000000);
        guiGraphics.fill(px0, py0, px1, py0 + 1, 0x1AFFFFFF);
        guiGraphics.drawBorder(px0, py0, px1 - px0, py1 - py0, -16447985);
        this.preview.updateEquipment(this.state);
        WardrobeGlowEntry outlineGlow = this.state.outlineGlow();
        int glowColor = !this.state.isHidden() && outlineGlow != null ? WardrobeScreen.glowColor(outlineGlow) : -1;
        this.preview.render(guiGraphics, px0, py0, px1, py1, this.yaw, this.pitch, this.previewZoom, glowColor, partialTick);
        if (this.overPreview(mouseX, mouseY)) {
            guiGraphics.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Scroll to zoom"), (px0 + px1) / 2, py1 - 11, -1063348548);
        }
        if (this.glowTab) {
            this.renderGlowGrid(guiGraphics, mouseX, mouseY);
        } else {
            this.renderGrid(guiGraphics, mouseX, mouseY);
            this.renderSwatches(guiGraphics);
        }
        this.renderPresetStrip(guiGraphics, mouseX, mouseY);
        List<Text> tip = null;
        if (hovered != null) {
            tip = this.tooltipFor(hovered);
        } else if (hoveredGlow != null) {
            tip = this.glowTooltip(hoveredGlow);
        } else if (hoveredPreset != null) {
            tip = this.presetTooltip(hoveredPreset);
        }
        if (tip != null) {
            GuiDepth.clearForOverlay(guiGraphics);
            Tooltips.render(guiGraphics, this.textRenderer, tip, mouseX, mouseY);
        }
    }

    private void renderContentPanel(DrawContext g, int x, int y, int gridW, int gridH) {
        g.fill(x - 2, y - 2, x + gridW + 2, y + gridH + 2, -15723477);
        WardrobeScreen.insetBevel(g, x - 2, y - 2, x + gridW + 2, y + gridH + 2, -16118238, -13747610);
    }

    private void renderGrid(DrawContext guiGraphics, int mouseX, int mouseY) {
        List<WardrobeCosmeticEntry> entries = this.orderedEntries(this.activeSlot);
        int x = this.gridX();
        int y = this.gridY();
        int gridW = 192;
        int rows = this.visibleRows();
        int gridH = rows * 24;
        long now = Util.getMeasuringTimeMs();
        float pulse = 0.5f + 0.5f * (float)Math.sin((double)now / 1200.0 * Math.PI * 2.0);
        int firstIndex = this.scrollRow * 8;
        this.renderContentPanel(guiGraphics, x, y, gridW, gridH);
        guiGraphics.enableScissor(x, y, x + gridW, y + gridH);
        for (int i = firstIndex; i < entries.size() && i < firstIndex + 8 * rows; ++i) {
            float hoverT;
            WardrobeCosmeticEntry entry = entries.get(i);
            int row = (i - firstIndex) / 8;
            float reveal = WardrobeScreen.easeOut(MathHelper.clamp((float)((float)(now - this.gridRevealStart - (long)row * 45L) / 170.0f), (float)0.0f, (float)1.0f));
            if (reveal <= 0.02f) continue;
            int cellX = x + (i - firstIndex) % 8 * 24;
            int cellY = y + row * 24 + (int)((1.0f - reveal) * 5.0f);
            int sx0 = cellX + 1;
            int sy0 = cellY + 1;
            int sx1 = cellX + 24 - 1;
            int sy1 = cellY + 24 - 1;
            guiGraphics.fill(sx0, sy0, sx1, sy1, WardrobeScreen.withAlpha(-15064506, reveal));
            WardrobeScreen.insetBevel(guiGraphics, sx0, sy0, sx1, sy1, -16118238, -13747610);
            boolean hoveredCell = reveal >= 1.0f && mouseX >= cellX && mouseX < cellX + 24 && mouseY >= cellY && mouseY < cellY + 24;
            float f = hoverT = hoveredCell ? WardrobeScreen.easeOut(Math.min(1.0f, (float)(now - this.hoverAnimStart) / 120.0f)) : 0.0f;
            if (hoveredCell) {
                guiGraphics.fill(sx0, sy0, sx1, sy1, 0x33FFFFFF);
                int glowA = (int)(153.0f * hoverT) << 24;
                guiGraphics.drawBorder(cellX, cellY, 24, 24, glowA | 0x9D4EDD);
            }
            float scale = 1.35f * (1.0f + 0.08f * hoverT + this.equipBounce(entry.getId(), now));
            PreviewUi.renderScaledItem(guiGraphics, this.state.stackFor(entry), cellX + 12 - 8, cellY + 12 - 8, scale);
            if (!entry.getOwned()) {
                guiGraphics.fill(sx0, sy0, sx1, sy1, 200, -1609298146);
                this.renderLockPip(guiGraphics, cellX + 24 - 7, cellY + 3);
            }
            if (this.state.isEquipped(entry)) {
                guiGraphics.fill(sx0, sy0, sx1, sy1, 675391583);
                guiGraphics.drawBorder(cellX, cellY, 24, 24, WardrobeScreen.withPulse(-12474273, pulse));
                continue;
            }
            if (!entry.getId().equals(this.state.getSelected(this.activeSlot))) continue;
            guiGraphics.fill(sx0, sy0, sx1, sy1, 681397981);
            guiGraphics.drawBorder(cellX, cellY, 24, 24, WardrobeScreen.withPulse(-6467875, pulse));
        }
        guiGraphics.disableScissor();
        int maxScroll = this.maxScrollRow();
        if (maxScroll > 0) {
            int trackX = x + gridW + 2;
            guiGraphics.fill(trackX, y, trackX + 4, y + gridH, -15986650);
            int thumbH = Math.max(10, gridH * 6 / (maxScroll + 6));
            int thumbY = y + (gridH - thumbH) * this.scrollRow / maxScroll;
            guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
            guiGraphics.fill(trackX + 1, thumbY + 1, trackX + 2, thumbY + thumbH - 1, -6467875);
        }
    }

    private void renderLockPip(DrawContext g, int lx, int ly) {
        int shackle = -4607288;
        int body = -535702;
        g.fill(lx + 1, ly, lx + 4, ly + 1, 210, shackle);
        g.fill(lx + 1, ly, lx + 2, ly + 2, 210, shackle);
        g.fill(lx + 3, ly, lx + 4, ly + 2, 210, shackle);
        g.fill(lx, ly + 2, lx + 5, ly + 6, 210, body);
    }

    private float equipBounce(String id, long now) {
        if (!id.equals(this.equipAnimId)) {
            return 0.0f;
        }
        float t = (float)(now - this.equipAnimStart) / 280.0f;
        return t >= 1.0f ? 0.0f : (float)Math.sin((double)t * Math.PI) * 0.22f * (1.0f - t);
    }

    private static int withPulse(int rgb, float pulse) {
        int alpha = 160 + (int)(pulse * 63.0f);
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    private static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private static int withAlpha(int argb, float f) {
        int a = (int)((float)(argb >>> 24 & 0xFF) * MathHelper.clamp((float)f, (float)0.0f, (float)1.0f));
        return a << 24 | argb & 0xFFFFFF;
    }

    private void renderTabIndicator(DrawContext guiGraphics, long frameDeltaMs) {
        ButtonWidget active = this.activeTabButton();
        if (active != null) {
            float targetX = active.getX();
            float targetW = active.getWidth();
            if (this.tabIndicatorX < 0.0f) {
                this.tabIndicatorX = targetX;
                this.tabIndicatorW = targetW;
            } else {
                float factor = 1.0f - (float)Math.exp(-12.0 * (double)frameDeltaMs / 1000.0);
                this.tabIndicatorX += (targetX - this.tabIndicatorX) * factor;
                this.tabIndicatorW += (targetW - this.tabIndicatorW) * factor;
            }
            int by = active.getY() + active.getHeight();
            guiGraphics.fill((int)this.tabIndicatorX, by - 1, (int)(this.tabIndicatorX + this.tabIndicatorW), by + 1, -6467875);
        }
    }

    private void renderSwatches(DrawContext guiGraphics) {
        WardrobeCosmeticEntry target = this.dyeTarget();
        if (target != null) {
            int y = this.swatchY();
            Integer current = this.state.colorFor(target.getId());
            for (DyeColor dye : DyeColor.values()) {
                int x = this.gridX() + dye.ordinal() * 10;
                int color = dye.getFireworkColor() & 0xFFFFFF;
                guiGraphics.fill(x - 1, y - 1, x + 9, y + 9, -16447985);
                guiGraphics.fill(x, y, x + 8, y + 8, 0xFF000000 | color);
                if (current == null || current != color) continue;
                guiGraphics.drawBorder(x - 1, y - 1, 10, 10, -6467875);
            }
        }
    }

    private static int glowColor(WardrobeGlowEntry glow) {
        List<Integer> colors = glow.getColors();
        if (colors != null && !colors.isEmpty()) {
            int idx = colors.size() == 1 ? 0 : (int)(Util.getMeasuringTimeMs() / 1000L % (long)colors.size());
            Integer color = colors.get(idx);
            return color != null ? color & 0xFFFFFF : 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    private void renderGlowGrid(DrawContext guiGraphics, int mouseX, int mouseY) {
        List<WardrobeGlowEntry> glows = this.state.glows();
        int x = this.gridX();
        int y = this.gridY();
        int gridW = 192;
        int rows = this.visibleRows();
        int gridH = rows * 24;
        if (glows.isEmpty()) {
            guiGraphics.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No glows available"), x + gridW / 2, y + gridH / 2 - 4, -7035976);
        } else {
            int firstIndex;
            long now = Util.getMeasuringTimeMs();
            float pulse = 0.5f + 0.5f * (float)Math.sin((double)now / 1200.0 * Math.PI * 2.0);
            this.renderContentPanel(guiGraphics, x, y, gridW, gridH);
            guiGraphics.enableScissor(x, y, x + gridW, y + gridH);
            for (int i = firstIndex = this.scrollRow * 8; i < glows.size() && i < firstIndex + 8 * rows; ++i) {
                WardrobeGlowEntry glow = glows.get(i);
                int row = (i - firstIndex) / 8;
                float reveal = WardrobeScreen.easeOut(MathHelper.clamp((float)((float)(now - this.gridRevealStart - (long)row * 45L) / 170.0f), (float)0.0f, (float)1.0f));
                if (reveal <= 0.02f) continue;
                int cellX = x + (i - firstIndex) % 8 * 24;
                int cellY = y + row * 24 + (int)((1.0f - reveal) * 5.0f);
                int sx0 = cellX + 1;
                int sy0 = cellY + 1;
                int sx1 = cellX + 24 - 1;
                int sy1 = cellY + 24 - 1;
                guiGraphics.fill(sx0, sy0, sx1, sy1, WardrobeScreen.withAlpha(-15064506, reveal));
                WardrobeScreen.insetBevel(guiGraphics, sx0, sy0, sx1, sy1, -16118238, -13747610);
                boolean hoveredCell = reveal >= 1.0f && mouseX >= cellX && mouseX < cellX + 24 && mouseY >= cellY && mouseY < cellY + 24;
                int chipAlpha = Math.round(255.0f * reveal) << 24;
                guiGraphics.fill(cellX + 4, cellY + 4, cellX + 24 - 4, cellY + 24 - 4, chipAlpha | WardrobeScreen.glowColor(glow));
                if (hoveredCell) {
                    guiGraphics.drawBorder(cellX, cellY, 24, 24, -1717743907);
                }
                if (!glow.getOwned()) {
                    guiGraphics.fill(sx0, sy0, sx1, sy1, 200, -1609298146);
                    this.renderLockPip(guiGraphics, cellX + 24 - 7, cellY + 3);
                }
                if (!glow.getEquipped()) continue;
                guiGraphics.drawBorder(cellX, cellY, 24, 24, WardrobeScreen.withPulse(-12474273, pulse));
            }
            guiGraphics.disableScissor();
            int maxScroll = this.maxScrollRow();
            if (maxScroll > 0) {
                int trackX = x + gridW + 2;
                guiGraphics.fill(trackX, y, trackX + 4, y + gridH, -15986650);
                int thumbH = Math.max(10, gridH * 6 / (maxScroll + 6));
                int thumbY = y + (gridH - thumbH) * this.scrollRow / maxScroll;
                guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
                guiGraphics.fill(trackX + 1, thumbY + 1, trackX + 2, thumbY + thumbH - 1, -6467875);
            }
        }
    }

    private WardrobeGlowEntry hoveredGlow(double mouseX, double mouseY) {
        if (!this.glowTab) {
            return null;
        }
        int x = this.gridX();
        int y = this.gridY();
        if (!(mouseX < (double)x || mouseX >= (double)(x + 192) || mouseY < (double)y || mouseY >= (double)(y + this.visibleRows() * 24))) {
            int row = (int)((mouseY - (double)y) / 24.0);
            int col = (int)((mouseX - (double)x) / 24.0);
            int index = (this.scrollRow + row) * 8 + col;
            List<WardrobeGlowEntry> glows = this.state.glows();
            return index < glows.size() ? glows.get(index) : null;
        }
        return null;
    }

    private List<Text> glowTooltip(WardrobeGlowEntry glow) {
        ArrayList<Text> lines = new ArrayList<>();
        lines.add(this.styledName(glow.getDisplayNameJson(), glow.getDisplayName() != null ? glow.getDisplayName() : glow.getId()));
        if (!glow.getOwned()) {
            for (String line : glow.getLockedLore() != null && !glow.getLockedLore().isEmpty() ? glow.getLockedLore() : this.state.getLockedTooltip()) {
                if (line == null) continue;
                lines.add(this.parseTooltipLine(line));
            }
        } else if (glow.getEquipped()) {
            this.addHintLines(lines, glow.getUnequipLore(), "Click to remove");
        } else {
            this.addHintLines(lines, glow.getEquipLore(), "Click to equip");
        }
        return lines;
    }

    private Text styledName(String json, String fallback) {
        return json != null && !json.isEmpty() ? this.parseTooltipLine(json) : Text.literal(fallback);
    }

    private void addHintLines(List<Text> lines, List<String> custom, String fallback) {
        if (custom != null && !custom.isEmpty()) {
            for (String line : custom) {
                if (line == null) continue;
                lines.add(this.parseTooltipLine(line));
            }
        } else {
            lines.add(Text.literal(fallback).formatted(Formatting.GRAY));
        }
    }

    private int presetStripY() {
        return this.panelTop() + 224 - 22;
    }

    private int presetStripX() {
        return this.panelLeft() + 58;
    }

    private void renderPresetStrip(DrawContext g, int mouseX, int mouseY) {
        List<WardrobePresetSummary> presets = this.state.presets();
        if (!presets.isEmpty()) {
            int y = this.presetStripY();
            int stripX = this.presetStripX();
            g.drawText(this.textRenderer, Text.literal("Presets"), this.panelLeft() + 10, y + 4, -7035976, false);
            for (int i = 0; i < presets.size(); ++i) {
                boolean hov;
                WardrobePresetSummary p = presets.get(i);
                int tx = stripX + i * 18;
                hov = mouseX >= tx && mouseX < tx + 16 && mouseY >= y && mouseY < y + 16;
                int bg = p.getLocked() ? -15462626 : (p.getFilled() ? -14405546 : -15199710);
                g.fill(tx, y, tx + 16, y + 16, bg);
                g.fill(tx, y, tx + 16, y + 1, -13161134);
                g.fill(tx, y + 16 - 1, tx + 16, y + 16, -16119790);
                int tc = p.getLocked() ? -10857360 : (p.getFilled() ? -2053377 : -7035976);
                g.drawCenteredTextWithShadow(this.textRenderer, String.valueOf(p.getIndex()), tx + 8, y + 4, tc);
                if (p.getLocked()) {
                    this.renderLockPip(g, tx + 16 - 6, y + 1);
                } else if (p.getFilled()) {
                    g.drawBorder(tx, y, 16, 16, -2131242134);
                }
                if (!hov || p.getLocked()) continue;
                g.drawBorder(tx, y, 16, 16, -6467875);
            }
        }
    }

    private WardrobePresetSummary hoveredPreset(double mouseX, double mouseY) {
        List<WardrobePresetSummary> presets = this.state.presets();
        if (presets.isEmpty()) {
            return null;
        }
        int y = this.presetStripY();
        if (!(mouseY < (double)y) && !(mouseY >= (double)(y + 16))) {
            int stripX = this.presetStripX();
            for (int i = 0; i < presets.size(); ++i) {
                int tx = stripX + i * 18;
                if (!(mouseX >= (double)tx) || !(mouseX < (double)(tx + 16))) continue;
                return presets.get(i);
            }
            return null;
        }
        return null;
    }

    private List<Text> presetTooltip(WardrobePresetSummary p) {
        ArrayList<Text> lines = new ArrayList<>();
        lines.add(Text.literal(("Preset " + p.getIndex())).formatted(Formatting.WHITE));
        if (p.getLocked()) {
            lines.add(Text.literal("Locked \u2014 unlock with a higher rank").formatted(Formatting.RED));
        } else if (p.getFilled()) {
            if (p.getLore() != null) {
                for (String line : p.getLore()) {
                    if (line == null) continue;
                    lines.add(this.parseTooltipLine(line));
                }
            }
            lines.add(Text.empty());
            lines.add(Text.literal("Now previewing").formatted(Formatting.AQUA));
            lines.add(Text.literal("Left Click ").formatted(Formatting.YELLOW).append(Text.literal("to apply").formatted(Formatting.GRAY)));
            lines.add(Text.literal("Right Click ").formatted(Formatting.YELLOW).append(Text.literal("to delete").formatted(Formatting.GRAY)));
        } else {
            lines.add(Text.literal("This slot is empty.").formatted(Formatting.DARK_GRAY));
            lines.add(Text.empty());
            lines.add(Text.literal("Left Click ").formatted(Formatting.YELLOW).append(Text.literal("to save your current look").formatted(Formatting.GRAY)));
        }
        return lines;
    }

    private List<Text> tooltipFor(WardrobeCosmeticEntry entry) {
        ArrayList<Text> lines = new ArrayList<>();
        lines.add(this.styledName(entry.getDisplayNameJson(), entry.getDisplayName() != null ? entry.getDisplayName() : entry.getId()));
        if (!entry.getOwned()) {
            for (String line : entry.getLockedLore() != null && !entry.getLockedLore().isEmpty() ? entry.getLockedLore() : this.state.getLockedTooltip()) {
                if (line == null) continue;
                lines.add(this.parseTooltipLine(line));
            }
        } else if (this.state.isEquipped(entry)) {
            this.addHintLines(lines, entry.getUnequipLore(), "Click to unequip");
        } else {
            this.addHintLines(lines, entry.getEquipLore(), "Click to equip");
        }
        if (WardrobeScreen.isActuallyDyeable(entry)) {
            lines.add(Text.literal("Dyeable").formatted(Formatting.DARK_AQUA));
        }
        return lines;
    }

    private Text parseTooltipLine(String line) {
        if (this.client != null && this.client.world != null) {
            try {
                MutableText parsed = Text.Serialization.fromJson(line, this.client.world.getRegistryManager());
                if (parsed != null) {
                    return parsed;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return Text.literal(line).formatted(Formatting.GRAY);
    }

    private WardrobeCosmeticEntry hoveredEntry(double mouseX, double mouseY) {
        int x = this.gridX();
        int y = this.gridY();
        if (!(mouseX < (double)x || mouseX >= (double)(x + 192) || mouseY < (double)y || mouseY >= (double)(y + this.visibleRows() * 24))) {
            int row = (int)((mouseY - (double)y) / 24.0);
            int col = (int)((mouseX - (double)x) / 24.0);
            int index = (this.scrollRow + row) * 8 + col;
            List<WardrobeCosmeticEntry> entries = this.orderedEntries(this.activeSlot);
            return index < entries.size() ? entries.get(index) : null;
        }
        return null;
    }

    private List<WardrobeCosmeticEntry> orderedEntries(WardrobeSlot slot) {
        if (this.orderedCache == null || this.orderedCacheSlot != slot || this.orderedCacheFloatyTab != this.floatyTab) {
            ArrayList<WardrobeCosmeticEntry> list = new ArrayList<>(this.state.entriesFor(slot));
            if (slot == WardrobeSlot.BALLOON) {
                list.removeIf(entry -> this.floatyTab != WardrobeScreen.isFloatyEntry(entry));
            }
            list.sort(Comparator.comparingInt(e -> e.getOwned() ? 0 : 1));
            this.orderedCache = list;
            this.orderedCacheSlot = slot;
            this.orderedCacheFloatyTab = this.floatyTab;
        }
        return this.orderedCache;
    }

    private static boolean isActuallyDyeable(WardrobeCosmeticEntry entry) {
        return entry != null && entry.getDyeable() && !"eevee_explorer_hood".equals(entry.getId());
    }

    private WardrobeCosmeticEntry dyeTarget() {
        WardrobeCosmeticEntry selected = this.state.entry(this.state.getSelected(this.activeSlot));
        if (WardrobeScreen.isActuallyDyeable(selected)) {
            return selected;
        }
        WardrobeCosmeticEntry equipped = this.state.equippedEntry(this.activeSlot);
        return WardrobeScreen.isActuallyDyeable(equipped) ? equipped : null;
    }

    private int entryCount() {
        return this.glowTab ? this.state.glows().size() : this.orderedEntries(this.activeSlot).size();
    }

    private int visibleRows() {
        return MathHelper.clamp((this.entryCount() + 8 - 1) / 8, 1, 5);
    }

    private int maxScrollRow() {
        int rows = (this.entryCount() + 8 - 1) / 8;
        return Math.max(0, rows - this.visibleRows());
    }

    private boolean overPreview(double mouseX, double mouseY) {
        return mouseX >= (double)this.previewX0() && mouseX < (double)this.previewX1() && mouseY >= (double)this.previewY0() && mouseY < (double)this.previewY1();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        WardrobeCosmeticEntry target;
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        WardrobePresetSummary preset = this.hoveredPreset(mouseX, mouseY);
        if (preset != null) {
            if (!preset.getLocked()) {
                if (button == 0) {
                    PreviewUi.playClick();
                    if (preset.getFilled()) {
                        WardrobeNetworking.sendLoadPreset(preset.getIndex());
                    } else {
                        WardrobeNetworking.sendSavePreset(preset.getIndex());
                    }
                } else if (button == 1 && preset.getFilled()) {
                    PreviewUi.playClick();
                    WardrobeNetworking.sendDeletePreset(preset.getIndex());
                }
            }
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (this.overPreview(mouseX, mouseY)) {
            this.draggingPreview = true;
            this.lastInteractMs = Util.getMeasuringTimeMs();
            return true;
        }
        if (this.glowTab) {
            WardrobeGlowEntry glow = this.hoveredGlow(mouseX, mouseY);
            if (glow != null) {
                PreviewUi.playClick();
                WardrobeNetworking.sendSetGlow(glow.getOwned() && glow.getEquipped() ? "" : glow.getId());
                return true;
            }
            return false;
        }
        WardrobeCosmeticEntry entry = this.hoveredEntry(mouseX, mouseY);
        if (entry != null) {
            PreviewUi.playClick();
            if (!entry.getOwned()) {
                WardrobeNetworking.sendEquip(entry.getId());
            } else if (this.state.isEquipped(entry)) {
                this.state.setSelected(this.activeSlot, null);
                this.state.unequipLocal(this.activeSlot);
                WardrobeNetworking.sendUnequip(this.activeSlot);
            } else {
                this.state.setSelected(this.activeSlot, null);
                this.state.equipLocal(entry);
                this.equipAnimId = entry.getId();
                this.equipAnimStart = Util.getMeasuringTimeMs();
                WardrobeNetworking.sendEquip(entry.getId());
            }
            return true;
        }
        DyeColor swatch = this.hoveredSwatch(mouseX, mouseY);
        if (swatch != null && (target = this.dyeTarget()) != null) {
            PreviewUi.playClick();
            int rgb = swatch.getFireworkColor() & 0xFFFFFF;
            this.state.setColorLocal(target.getId(), rgb);
            if (target.getOwned()) {
                WardrobeNetworking.sendSetColor(target.getId(), rgb);
            }
            return true;
        }
        return false;
    }

    private DyeColor hoveredSwatch(double mouseX, double mouseY) {
        if (this.dyeTarget() == null) {
            return null;
        }
        int y = this.swatchY();
        if (!(mouseY < (double)y) && !(mouseY >= (double)(y + 9))) {
            if (mouseX < (double)this.gridX()) {
                return null;
            }
            int index = (int)((mouseX - (double)this.gridX()) / 10.0);
            return index >= DyeColor.values().length ? null : DyeColor.values()[index];
        }
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingPreview && button == 0) {
            this.draggingPreview = false;
            this.lastInteractMs = Util.getMeasuringTimeMs();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingPreview && button == 0) {
            this.yaw = (this.yaw + (float)dragX) % 360.0f;
            this.pitch = MathHelper.clamp((float)(this.pitch + (float)dragY * 0.5f), (float)-20.0f, (float)20.0f);
            this.lastInteractMs = Util.getMeasuringTimeMs();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.overPreview(mouseX, mouseY) && scrollY != 0.0) {
            float factor = scrollY > 0.0 ? 1.12f : 0.89285713f;
            this.previewZoom = MathHelper.clamp((float)(this.previewZoom * factor), (float)0.5f, (float)2.6f);
            return true;
        }
        int x = this.gridX();
        int y = this.gridY();
        if (mouseX >= (double)x && mouseX < (double)(x + 192 + 6) && mouseY >= (double)y && mouseY < (double)(y + this.visibleRows() * 24)) {
            this.scrollRow = MathHelper.clamp(this.scrollRow - (int)Math.signum(scrollY), 0, this.maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void cleanup() {
        this.state.clearPreviewOverrides();
        this.preview.close();
    }

    @Override
    public void close() {
        PreviewUi.playClose();
        this.cleanup();
        super.close();
    }

    @Override
    public void removed() {
        this.cleanup();
        super.removed();
    }

    private static void insetBevel(DrawContext g, int x0, int y0, int x1, int y1, int shadow, int light) {
        g.fill(x0, y0, x1, y0 + 1, shadow);
        g.fill(x0, y0, x0 + 1, y1, shadow);
        g.fill(x0, y1 - 1, x1, y1, light);
        g.fill(x1 - 1, y0, x1, y1, light);
    }

    @Environment(EnvType.CLIENT)
    private final class TabButton
            extends ButtonWidget {
        private TabButton(int x, int y, int w, int h, Text label, ButtonWidget.PressAction onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        protected void renderWidget(DrawContext g, int mx, int my, float pt) {
            boolean selected = !this.active;
            boolean hover = this.isHovered() && this.active;
            int x0 = this.getX();
            int y0 = this.getY();
            int x1 = x0 + this.getWidth();
            int y1 = y0 + this.getHeight();
            int bg = selected ? -14274468 : (hover ? -14801336 : -15459784);
            g.fill(x0, y0, x1, y1, bg);
            if (!selected) {
                g.fill(x0, y1 - 1, x1, y1, -16118238);
            }
            int tc = selected ? -2053377 : (hover ? -3418650 : -7035976);
            g.drawCenteredTextWithShadow(WardrobeScreen.this.textRenderer, this.getMessage(), (x0 + x1) / 2, y0 + (this.getHeight() - 8) / 2, tc);
        }
    }
}