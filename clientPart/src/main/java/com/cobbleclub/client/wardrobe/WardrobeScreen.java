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
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_124;
import net.minecraft.class_156;
import net.minecraft.class_1767;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_2561.class_2562;

@Environment(EnvType.CLIENT)
public class WardrobeScreen extends class_437 {
   private static final int PANEL_W = 416;
   private static final int PANEL_H = 224;
   private static final int GRID_COLS = 8;
   private static final int CELL = 24;
   private static final int GRID_ROWS = 5;
   private static final float ICON_SCALE = 1.35F;
   private static final int PRESET_TILE = 16;
   private static final int PRESET_GAP = 2;
   private static final float AUTO_SPIN_DEG_PER_MS = 0.03F;
   private static final long AUTO_SPIN_IDLE_MS = 1500L;
   private static final float MAX_PITCH = 20.0F;
   private static final float MIN_ZOOM = 0.5F;
   private static final float MAX_ZOOM = 2.6F;
   private static final float DEFAULT_ZOOM = 0.82F;
   private static final float ZOOM_STEP = 1.12F;
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
   private float previewZoom = 0.82F;
   private boolean draggingPreview;
   private long lastInteractMs;
   private long lastFrameMs = class_156.method_658();
   private String hoverAnimId;
   private long hoverAnimStart;
   private String equipAnimId;
   private long equipAnimStart;
   private float tabIndicatorX = -1.0F;
   private float tabIndicatorW;
   private long gridRevealStart;
   private final Map<WardrobeSlot, class_4185> tabButtons = new EnumMap(WardrobeSlot.class);
   private class_4185 glowTabButton;
   private class_4185 floatyTabButton;
   private List<WardrobeCosmeticEntry> orderedCache;
   private WardrobeSlot orderedCacheSlot;
   private boolean orderedCacheFloatyTab;

   public WardrobeScreen(WardrobeSlot initialSlot) {
      super(class_2561.method_43470("Wardrobe"));
      this.activeSlot = initialSlot != null ? initialSlot : WardrobeSlot.HELMET;
      this.gridRevealStart = class_156.method_658();
      PreviewUi.playOpen();
   }

   private int panelLeft() {
      return (this.field_22789 - PANEL_W) / 2;
   }

   private int panelTop() {
      return (this.field_22790 - PANEL_H) / 2;
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
      return this.panelTop() + PANEL_H - 28;
   }

   private int gridX() {
      return this.panelLeft() + 176;
   }

   private int gridY() {
      return this.panelTop() + 46;
   }

   private int swatchY() {
      return this.gridY() + this.visibleRows() * CELL + 8;
   }

   protected void method_25426() {
      this.tabButtons.clear();
      int tabX = this.gridX();

      for(WardrobeSlot slot : WardrobeSlot.values()) {
         if (slot == WardrobeSlot.BALLOON) {
            class_2561 floatyLabel = class_2561.method_43470("Floaties");
            int fw = this.field_22793.method_27525(floatyLabel) + 8;
            this.floatyTabButton = new TabButton(tabX, this.panelTop() + 22, fw, 18, floatyLabel, (b) -> this.selectFloatyTab());
            this.method_37063(this.floatyTabButton);
            tabX += fw + 3;
         }
         class_2561 label = class_2561.method_43470(this.state.slotDisplayName(slot));
         int w = this.field_22793.method_27525(label) + 8;
         TabButton tab = new TabButton(tabX, this.panelTop() + 22, w, 18, label, (b) -> this.selectTab(slot));
         this.tabButtons.put(slot, tab);
         this.method_37063(tab);
         tabX += w + 3;
      }

      if (!this.state.glows().isEmpty()) {
         class_2561 glowLabel = class_2561.method_43470("Glow");
         this.glowTabButton = new TabButton(tabX, this.panelTop() + 22, this.field_22793.method_27525(glowLabel) + 8, 18, glowLabel, (b) -> this.selectGlowTab());
         this.method_37063(this.glowTabButton);
      } else {
         this.glowTabButton = null;
         this.glowTab = false;
      }

      this.updateTabActiveFlags();
      int footerY = this.panelTop() + PANEL_H - 24;
      this.method_37063(new ThemedButton(this.panelLeft() + PANEL_W - 264, footerY, 86, 20, class_2561.method_43470("Unequip All"), (b) -> {
         this.state.unequipAllLocal();
         WardrobeNetworking.sendUnequipAll();
      }));
      this.method_37063(new ThemedButton(this.panelLeft() + PANEL_W - 172, footerY, 106, 20, this.hideLabel(), (b) -> {
         boolean nowHidden = !this.state.isHidden();
         this.state.setHiddenLocal(nowHidden);
         WardrobeNetworking.sendSetHidden(nowHidden);
         b.method_25355(this.hideLabel());
      }));
      this.method_37063(new ThemedButton(this.panelLeft() + PANEL_W - 60, footerY, 52, 20, class_2561.method_43470("Close"), (b) -> this.method_25419()));
   }

   private void updateTabActiveFlags() {
      this.tabButtons.forEach((s, button) -> button.field_22763 = this.glowTab || this.floatyTab || s != this.activeSlot);
      if (this.glowTabButton != null) {
         this.glowTabButton.field_22763 = !this.glowTab;
      }
      if (this.floatyTabButton != null) {
         this.floatyTabButton.field_22763 = !this.floatyTab;
      }

   }

   private class_4185 activeTabButton() {
      return this.glowTab ? this.glowTabButton : (this.floatyTab ? this.floatyTabButton : (class_4185)this.tabButtons.get(this.activeSlot));
   }

   private class_2561 hideLabel() {
      return this.state.isHidden() ? class_2561.method_43470("Cosmetics: Hidden").method_27692(class_124.field_1061) : class_2561.method_43470("Cosmetics: Shown").method_27692(class_124.field_1060);
   }

   private void selectTab(WardrobeSlot slot) {
      this.state.setHover(this.activeSlot, (String)null);
      this.state.setHoverGlow((String)null);
      this.glowTab = false;
      this.floatyTab = false;
      this.activeSlot = slot;
      this.scrollRow = 0;
      this.gridRevealStart = class_156.method_658();
      this.updateTabActiveFlags();
   }

   private void selectGlowTab() {
      this.state.setHover(this.activeSlot, (String)null);
      this.glowTab = true;
      this.floatyTab = false;
      this.scrollRow = 0;
      this.gridRevealStart = class_156.method_658();
      this.updateTabActiveFlags();
   }


   private void selectFloatyTab() {
      this.state.setHover(this.activeSlot, (String)null);
      this.state.setHoverGlow((String)null);
      this.glowTab = false;
      this.floatyTab = true;
      this.activeSlot = WardrobeSlot.BALLOON;
      this.scrollRow = 0;
      this.orderedCache = null;
      this.gridRevealStart = class_156.method_658();
      this.updateTabActiveFlags();
   }

   private static boolean isFloatyEntry(WardrobeCosmeticEntry entry) {
      if (entry == null || entry.getId() == null) return false;
      return entry.getId().toLowerCase(java.util.Locale.ROOT).contains("floaty");
   }

   public void onStateRefreshed() {
      this.orderedCache = null;

      for(WardrobeSlot slot : WardrobeSlot.values()) {
         if (this.state.entry(this.state.getSelected(slot)) == null) {
            this.state.setSelected(slot, (String)null);
         }
      }

      this.scrollRow = class_3532.method_15340(this.scrollRow, 0, this.maxScrollRow());
      this.method_41843();
   }

   public boolean method_25421() {
      return false;
   }

   public void method_25420(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.method_25420(guiGraphics, mouseX, mouseY, partialTick);
      int left = this.panelLeft();
      int top = this.panelTop();
      int right = left + PANEL_W;
      int bottom = top + PANEL_H;
      Starfield.draw(guiGraphics, 0, 0, this.field_22789, this.field_22790, class_156.method_658(), 44, 1337L, 0.5F);
      guiGraphics.method_25294(left - 3, top - 3, right + 3, bottom + 3, 1426063360);
      guiGraphics.method_25296(left, top, right, bottom, -199614136, -200601562);
      guiGraphics.method_25296(left, top, right, top + 18, -14405538, -15459782);
      guiGraphics.method_25294(left, top + 17, right, top + 18, -9815394);
      guiGraphics.method_49601(left, top, PANEL_W, PANEL_H, -16447985);
      guiGraphics.method_49601(left + 1, top + 1, PANEL_W - 2, PANEL_H - 2, -13747610);
      guiGraphics.method_27534(this.field_22793, this.field_22785, left + PANEL_W / 2, top + 5, -2053377);
   }

   public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.method_25394(guiGraphics, mouseX, mouseY, partialTick);
      long now = class_156.method_658();
      long frameDeltaMs = now - this.lastFrameMs;
      if (!this.draggingPreview && now - this.lastInteractMs > 1500L) {
         this.yaw = (this.yaw + (float)frameDeltaMs * 0.03F) % 360.0F;
      }

      this.lastFrameMs = now;
      this.renderTabIndicator(guiGraphics, frameDeltaMs);
      WardrobeCosmeticEntry hovered = this.glowTab ? null : this.hoveredEntry((double)mouseX, (double)mouseY);
      String hoveredId = hovered != null ? hovered.getId() : null;
      this.state.setHover(this.activeSlot, hoveredId);
      if (!Objects.equals(hoveredId, this.hoverAnimId)) {
         this.hoverAnimId = hoveredId;
         this.hoverAnimStart = now;
      }

      WardrobeGlowEntry hoveredGlow = this.glowTab ? this.hoveredGlow((double)mouseX, (double)mouseY) : null;
      this.state.setHoverGlow(hoveredGlow != null ? hoveredGlow.getId() : null);
      WardrobePresetSummary hoveredPreset = this.hoveredPreset((double)mouseX, (double)mouseY);
      this.state.setPresetPreview(hoveredPreset != null && hoveredPreset.getFilled() && !hoveredPreset.getLocked() ? hoveredPreset : null);
      int px0 = this.previewX0();
      int py0 = this.previewY0();
      int px1 = this.previewX1();
      int py1 = this.previewY1();
      guiGraphics.method_25296(px0, py0, px1, py1, -14998448, -16315880);
      guiGraphics.method_44379(px0, py0, px1, py1);
      Starfield.draw(guiGraphics, px0, py0, px1, py1, now, 34, 91L, 0.85F);
      guiGraphics.method_44380();
      guiGraphics.method_25296(px0, py1 - 46, px1, py1, 0, 1711276032);
      guiGraphics.method_25294(px0, py0, px1, py0 + 1, 452984831);
      guiGraphics.method_49601(px0, py0, px1 - px0, py1 - py0, -16447985);
      this.preview.updateEquipment(this.state);
      WardrobeGlowEntry outlineGlow = this.state.outlineGlow();
      int glowColor = !this.state.isHidden() && outlineGlow != null ? glowColor(outlineGlow) : -1;
      this.preview.render(guiGraphics, px0, py0, px1, py1, this.yaw, this.pitch, this.previewZoom, glowColor, partialTick);
      if (this.overPreview((double)mouseX, (double)mouseY)) {
         guiGraphics.method_27534(this.field_22793, class_2561.method_43470("Scroll to zoom"), (px0 + px1) / 2, py1 - 11, -1063348548);
      }

      if (this.glowTab) {
         this.renderGlowGrid(guiGraphics, mouseX, mouseY);
      } else {
         this.renderGrid(guiGraphics, mouseX, mouseY);
         this.renderSwatches(guiGraphics);
      }

      this.renderPresetStrip(guiGraphics, mouseX, mouseY);
      List<class_2561> tip = null;
      if (hovered != null) {
         tip = this.tooltipFor(hovered);
      } else if (hoveredGlow != null) {
         tip = this.glowTooltip(hoveredGlow);
      } else if (hoveredPreset != null) {
         tip = this.presetTooltip(hoveredPreset);
      }

      if (tip != null) {
         GuiDepth.clearForOverlay(guiGraphics);
         Tooltips.render(guiGraphics, this.field_22793, tip, mouseX, mouseY);
      }

   }

   private void renderContentPanel(class_332 g, int x, int y, int gridW, int gridH) {
      g.method_25294(x - 2, y - 2, x + gridW + 2, y + gridH + 2, -15723477);
      insetBevel(g, x - 2, y - 2, x + gridW + 2, y + gridH + 2, -16118238, -13747610);
   }

   private void renderGrid(class_332 guiGraphics, int mouseX, int mouseY) {
      List<WardrobeCosmeticEntry> entries = this.orderedEntries(this.activeSlot);
      int x = this.gridX();
      int y = this.gridY();
      int gridW = GRID_COLS * CELL;
      int rows = this.visibleRows();
      int gridH = rows * CELL;
      long now = class_156.method_658();
      float pulse = 0.5F + 0.5F * (float)Math.sin((double)now / (double)1200.0F * Math.PI * (double)2.0F);
      int firstIndex = this.scrollRow * GRID_COLS;
      this.renderContentPanel(guiGraphics, x, y, gridW, gridH);
      guiGraphics.method_44379(x, y, x + gridW, y + gridH);

      for(int i = firstIndex; i < entries.size() && i < firstIndex + GRID_COLS * rows; ++i) {
         WardrobeCosmeticEntry entry = (WardrobeCosmeticEntry)entries.get(i);
         int row = (i - firstIndex) / GRID_COLS;
         float reveal = easeOut(class_3532.method_15363((float)(now - this.gridRevealStart - (long)row * 45L) / 170.0F, 0.0F, 1.0F));
         if (!(reveal <= 0.02F)) {
            int cellX = x + (i - firstIndex) % GRID_COLS * CELL;
            int cellY = y + row * CELL + (int)((1.0F - reveal) * 5.0F);
            int sx0 = cellX + 1;
            int sy0 = cellY + 1;
            int sx1 = cellX + CELL - 1;
            int sy1 = cellY + CELL - 1;
            guiGraphics.method_25294(sx0, sy0, sx1, sy1, withAlpha(-15064506, reveal));
            insetBevel(guiGraphics, sx0, sy0, sx1, sy1, -16118238, -13747610);
            boolean hoveredCell = reveal >= 1.0F && mouseX >= cellX && mouseX < cellX + CELL && mouseY >= cellY && mouseY < cellY + CELL;
            float hoverT = hoveredCell ? easeOut(Math.min(1.0F, (float)(now - this.hoverAnimStart) / 120.0F)) : 0.0F;
            if (hoveredCell) {
               guiGraphics.method_25294(sx0, sy0, sx1, sy1, 872415231);
               int glowA = (int)(153.0F * hoverT) << 24;
               guiGraphics.method_49601(cellX, cellY, CELL, CELL, glowA | 10309341);
            }

            float scale = 1.35F * (1.0F + 0.08F * hoverT + this.equipBounce(entry.getId(), now));
            PreviewUi.renderScaledItem(guiGraphics, this.state.stackFor(entry), cellX + CELL / 2 - 8, cellY + CELL / 2 - 8, scale);
            if (!entry.getOwned()) {
               guiGraphics.method_51737(sx0, sy0, sx1, sy1, 200, -1609298146);
               this.renderLockPip(guiGraphics, cellX + CELL - 7, cellY + 3);
            }

            if (this.state.isEquipped(entry)) {
               guiGraphics.method_25294(sx0, sy0, sx1, sy1, 675391583);
               guiGraphics.method_49601(cellX, cellY, CELL, CELL, withPulse(-12474273, pulse));
            } else if (entry.getId().equals(this.state.getSelected(this.activeSlot))) {
               guiGraphics.method_25294(sx0, sy0, sx1, sy1, 681397981);
               guiGraphics.method_49601(cellX, cellY, CELL, CELL, withPulse(-6467875, pulse));
            }
         }
      }

      guiGraphics.method_44380();
      int maxScroll = this.maxScrollRow();
      if (maxScroll > 0) {
         int trackX = x + gridW + 2;
         guiGraphics.method_25294(trackX, y, trackX + 4, y + gridH, -15986650);
         int thumbH = Math.max(10, gridH * 6 / (maxScroll + 6));
         int thumbY = y + (gridH - thumbH) * this.scrollRow / maxScroll;
         guiGraphics.method_25294(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
         guiGraphics.method_25294(trackX + 1, thumbY + 1, trackX + 2, thumbY + thumbH - 1, -6467875);
      }

   }

   private void renderLockPip(class_332 g, int lx, int ly) {
      int shackle = -4607288;
      int body = -535702;
      g.method_51737(lx + 1, ly, lx + 4, ly + 1, 210, shackle);
      g.method_51737(lx + 1, ly, lx + 2, ly + 2, 210, shackle);
      g.method_51737(lx + 3, ly, lx + 4, ly + 2, 210, shackle);
      g.method_51737(lx, ly + 2, lx + 5, ly + 6, 210, body);
   }

   private float equipBounce(String id, long now) {
      if (!id.equals(this.equipAnimId)) {
         return 0.0F;
      } else {
         float t = (float)(now - this.equipAnimStart) / 280.0F;
         return t >= 1.0F ? 0.0F : (float)Math.sin((double)t * Math.PI) * 0.22F * (1.0F - t);
      }
   }

   private static int withPulse(int rgb, float pulse) {
      int alpha = 160 + (int)(pulse * 63.0F);
      return alpha << 24 | rgb & 16777215;
   }

   private static float easeOut(float t) {
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   private static int withAlpha(int argb, float f) {
      int a = (int)((float)(argb >>> 24 & 255) * class_3532.method_15363(f, 0.0F, 1.0F));
      return a << 24 | argb & 16777215;
   }

   private void renderTabIndicator(class_332 guiGraphics, long frameDeltaMs) {
      class_4185 active = this.activeTabButton();
      if (active != null) {
         float targetX = (float)active.method_46426();
         float targetW = (float)active.method_25368();
         if (this.tabIndicatorX < 0.0F) {
            this.tabIndicatorX = targetX;
            this.tabIndicatorW = targetW;
         } else {
            float factor = 1.0F - (float)Math.exp((double)-12.0F * (double)frameDeltaMs / (double)1000.0F);
            this.tabIndicatorX += (targetX - this.tabIndicatorX) * factor;
            this.tabIndicatorW += (targetW - this.tabIndicatorW) * factor;
         }

         int by = active.method_46427() + active.method_25364();
         guiGraphics.method_25294((int)this.tabIndicatorX, by - 1, (int)(this.tabIndicatorX + this.tabIndicatorW), by + 1, -6467875);
      }
   }

   private void renderSwatches(class_332 guiGraphics) {
      WardrobeCosmeticEntry target = this.dyeTarget();
      if (target != null) {
         int y = this.swatchY();
         Integer current = this.state.colorFor(target.getId());

         for(class_1767 dye : class_1767.values()) {
            int x = this.gridX() + dye.ordinal() * 10;
            int color = dye.method_7790() & 16777215;
            guiGraphics.method_25294(x - 1, y - 1, x + 9, y + 9, -16447985);
            guiGraphics.method_25294(x, y, x + 8, y + 8, -16777216 | color);
            if (current != null && current == color) {
               guiGraphics.method_49601(x - 1, y - 1, 10, 10, -6467875);
            }
         }

      }
   }

   private static int glowColor(WardrobeGlowEntry glow) {
      List<Integer> colors = glow.getColors();
      if (colors != null && !colors.isEmpty()) {
         int idx = colors.size() == 1 ? 0 : (int)(class_156.method_658() / 1000L % (long)colors.size());
         Integer color = (Integer)colors.get(idx);
         return color != null ? color & 16777215 : 16777215;
      } else {
         return 16777215;
      }
   }

   private void renderGlowGrid(class_332 guiGraphics, int mouseX, int mouseY) {
      List<WardrobeGlowEntry> glows = this.state.glows();
      int x = this.gridX();
      int y = this.gridY();
      int gridW = GRID_COLS * CELL;
      int rows = this.visibleRows();
      int gridH = rows * CELL;
      if (glows.isEmpty()) {
         guiGraphics.method_27534(this.field_22793, class_2561.method_43470("No glows available"), x + gridW / 2, y + gridH / 2 - 4, -7035976);
      } else {
         long now = class_156.method_658();
         float pulse = 0.5F + 0.5F * (float)Math.sin((double)now / (double)1200.0F * Math.PI * (double)2.0F);
         this.renderContentPanel(guiGraphics, x, y, gridW, gridH);
         guiGraphics.method_44379(x, y, x + gridW, y + gridH);
         int firstIndex = this.scrollRow * GRID_COLS;

         for(int i = firstIndex; i < glows.size() && i < firstIndex + GRID_COLS * rows; ++i) {
            WardrobeGlowEntry glow = (WardrobeGlowEntry)glows.get(i);
            int row = (i - firstIndex) / GRID_COLS;
            float reveal = easeOut(class_3532.method_15363((float)(now - this.gridRevealStart - (long)row * 45L) / 170.0F, 0.0F, 1.0F));
            if (!(reveal <= 0.02F)) {
               int cellX = x + (i - firstIndex) % GRID_COLS * CELL;
               int cellY = y + row * CELL + (int)((1.0F - reveal) * 5.0F);
               int sx0 = cellX + 1;
               int sy0 = cellY + 1;
               int sx1 = cellX + CELL - 1;
               int sy1 = cellY + CELL - 1;
               guiGraphics.method_25294(sx0, sy0, sx1, sy1, withAlpha(-15064506, reveal));
               insetBevel(guiGraphics, sx0, sy0, sx1, sy1, -16118238, -13747610);
               boolean hoveredCell = reveal >= 1.0F && mouseX >= cellX && mouseX < cellX + CELL && mouseY >= cellY && mouseY < cellY + CELL;
               int chipAlpha = Math.round(255.0F * reveal) << 24;
               guiGraphics.method_25294(cellX + 4, cellY + 4, cellX + CELL - 4, cellY + CELL - 4, chipAlpha | glowColor(glow));
               if (hoveredCell) {
                  guiGraphics.method_49601(cellX, cellY, CELL, CELL, -1717743907);
               }

               if (!glow.getOwned()) {
                  guiGraphics.method_51737(sx0, sy0, sx1, sy1, 200, -1609298146);
                  this.renderLockPip(guiGraphics, cellX + CELL - 7, cellY + 3);
               }

               if (glow.getEquipped()) {
                  guiGraphics.method_49601(cellX, cellY, CELL, CELL, withPulse(-12474273, pulse));
               }
            }
         }

         guiGraphics.method_44380();
         int maxScroll = this.maxScrollRow();
         if (maxScroll > 0) {
            int trackX = x + gridW + 2;
            guiGraphics.method_25294(trackX, y, trackX + 4, y + gridH, -15986650);
            int thumbH = Math.max(10, gridH * 6 / (maxScroll + 6));
            int thumbY = y + (gridH - thumbH) * this.scrollRow / maxScroll;
            guiGraphics.method_25294(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
            guiGraphics.method_25294(trackX + 1, thumbY + 1, trackX + 2, thumbY + thumbH - 1, -6467875);
         }

      }
   }

   private WardrobeGlowEntry hoveredGlow(double mouseX, double mouseY) {
      if (!this.glowTab) {
         return null;
      } else {
         int x = this.gridX();
         int y = this.gridY();
         if (!(mouseX < (double)x) && !(mouseX >= (double)(x + GRID_COLS * CELL)) && !(mouseY < (double)y) && !(mouseY >= (double)(y + this.visibleRows() * CELL))) {
            int col = (int)((mouseX - (double)x) / (double)CELL);
            int row = (int)((mouseY - (double)y) / (double)CELL);
            int index = (this.scrollRow + row) * GRID_COLS + col;
            List<WardrobeGlowEntry> glows = this.state.glows();
            return index < glows.size() ? (WardrobeGlowEntry)glows.get(index) : null;
         } else {
            return null;
         }
      }
   }

   private List<class_2561> glowTooltip(WardrobeGlowEntry glow) {
      List<class_2561> lines = new ArrayList();
      lines.add(this.styledName(glow.getDisplayNameJson(), glow.getDisplayName() != null ? glow.getDisplayName() : glow.getId()));
      if (!glow.getOwned()) {
         for(String line : glow.getLockedLore() != null && !glow.getLockedLore().isEmpty() ? glow.getLockedLore() : this.state.getLockedTooltip()) {
            if (line != null) {
               lines.add(this.parseTooltipLine(line));
            }
         }
      } else if (glow.getEquipped()) {
         this.addHintLines(lines, glow.getUnequipLore(), "Click to remove");
      } else {
         this.addHintLines(lines, glow.getEquipLore(), "Click to equip");
      }

      return lines;
   }

   private class_2561 styledName(String json, String fallback) {
      return (class_2561)(json != null && !json.isEmpty() ? this.parseTooltipLine(json) : class_2561.method_43470(fallback));
   }

   private void addHintLines(List<class_2561> lines, List<String> custom, String fallback) {
      if (custom != null && !custom.isEmpty()) {
         for(String line : custom) {
            if (line != null) {
               lines.add(this.parseTooltipLine(line));
            }
         }
      } else {
         lines.add(class_2561.method_43470(fallback).method_27692(class_124.field_1080));
      }

   }

   private int presetStripY() {
      return this.panelTop() + PANEL_H - 22;
   }

   private int presetStripX() {
      return this.panelLeft() + 58;
   }

   private void renderPresetStrip(class_332 g, int mouseX, int mouseY) {
      List<WardrobePresetSummary> presets = this.state.presets();
      if (!presets.isEmpty()) {
         int y = this.presetStripY();
         int stripX = this.presetStripX();
         g.method_51439(this.field_22793, class_2561.method_43470("Presets"), this.panelLeft() + 10, y + 4, -7035976, false);

         for(int i = 0; i < presets.size(); ++i) {
            WardrobePresetSummary p = (WardrobePresetSummary)presets.get(i);
            int tx = stripX + i * 18;
            boolean hov = mouseX >= tx && mouseX < tx + 16 && mouseY >= y && mouseY < y + 16;
            int bg = p.getLocked() ? -15462626 : (p.getFilled() ? -14405546 : -15199710);
            g.method_25294(tx, y, tx + 16, y + 16, bg);
            g.method_25294(tx, y, tx + 16, y + 1, -13161134);
            g.method_25294(tx, y + 16 - 1, tx + 16, y + 16, -16119790);
            int tc = p.getLocked() ? -10857360 : (p.getFilled() ? -2053377 : -7035976);
            g.method_25300(this.field_22793, String.valueOf(p.getIndex()), tx + 8, y + 4, tc);
            if (p.getLocked()) {
               this.renderLockPip(g, tx + 16 - 6, y + 1);
            } else if (p.getFilled()) {
               g.method_49601(tx, y, 16, 16, -2131242134);
            }

            if (hov && !p.getLocked()) {
               g.method_49601(tx, y, 16, 16, -6467875);
            }
         }

      }
   }

   private WardrobePresetSummary hoveredPreset(double mouseX, double mouseY) {
      List<WardrobePresetSummary> presets = this.state.presets();
      if (presets.isEmpty()) {
         return null;
      } else {
         int y = this.presetStripY();
         if (!(mouseY < (double)y) && !(mouseY >= (double)(y + 16))) {
            int stripX = this.presetStripX();

            for(int i = 0; i < presets.size(); ++i) {
               int tx = stripX + i * 18;
               if (mouseX >= (double)tx && mouseX < (double)(tx + 16)) {
                  return (WardrobePresetSummary)presets.get(i);
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   private List<class_2561> presetTooltip(WardrobePresetSummary p) {
      List<class_2561> lines = new ArrayList();
      lines.add(class_2561.method_43470("Preset " + p.getIndex()).method_27692(class_124.field_1068));
      if (p.getLocked()) {
         lines.add(class_2561.method_43470("Locked — unlock with a higher rank").method_27692(class_124.field_1061));
      } else if (p.getFilled()) {
         if (p.getLore() != null) {
            for(String line : p.getLore()) {
               if (line != null) {
                  lines.add(this.parseTooltipLine(line));
               }
            }
         }

         lines.add(class_2561.method_43473());
         lines.add(class_2561.method_43470("Now previewing").method_27692(class_124.field_1075));
         lines.add(class_2561.method_43470("Left Click ").method_27692(class_124.field_1054).method_10852(class_2561.method_43470("to apply").method_27692(class_124.field_1080)));
         lines.add(class_2561.method_43470("Right Click ").method_27692(class_124.field_1054).method_10852(class_2561.method_43470("to delete").method_27692(class_124.field_1080)));
      } else {
         lines.add(class_2561.method_43470("This slot is empty.").method_27692(class_124.field_1063));
         lines.add(class_2561.method_43473());
         lines.add(class_2561.method_43470("Left Click ").method_27692(class_124.field_1054).method_10852(class_2561.method_43470("to save your current look").method_27692(class_124.field_1080)));
      }

      return lines;
   }

   private List<class_2561> tooltipFor(WardrobeCosmeticEntry entry) {
      List<class_2561> lines = new ArrayList();
      lines.add(this.styledName(entry.getDisplayNameJson(), entry.getDisplayName() != null ? entry.getDisplayName() : entry.getId()));
      if (!entry.getOwned()) {
         for(String line : entry.getLockedLore() != null && !entry.getLockedLore().isEmpty() ? entry.getLockedLore() : this.state.getLockedTooltip()) {
            if (line != null) {
               lines.add(this.parseTooltipLine(line));
            }
         }
      } else if (this.state.isEquipped(entry)) {
         this.addHintLines(lines, entry.getUnequipLore(), "Click to unequip");
      } else {
         this.addHintLines(lines, entry.getEquipLore(), "Click to equip");
      }

      if (isActuallyDyeable(entry)) {
         lines.add(class_2561.method_43470("Dyeable").method_27692(class_124.field_1062));
      }

      return lines;
   }

   private class_2561 parseTooltipLine(String line) {
      if (this.field_22787 != null && this.field_22787.field_1687 != null) {
         try {
            class_2561 parsed = class_2562.method_10877(line, this.field_22787.field_1687.method_30349());
            if (parsed != null) {
               return parsed;
            }
         } catch (Exception var3) {
         }
      }

      return class_2561.method_43470(line).method_27692(class_124.field_1080);
   }

   private WardrobeCosmeticEntry hoveredEntry(double mouseX, double mouseY) {
      int x = this.gridX();
      int y = this.gridY();
      if (!(mouseX < (double)x) && !(mouseX >= (double)(x + GRID_COLS * CELL)) && !(mouseY < (double)y) && !(mouseY >= (double)(y + this.visibleRows() * CELL))) {
         int col = (int)((mouseX - (double)x) / (double)CELL);
         int row = (int)((mouseY - (double)y) / (double)CELL);
         int index = (this.scrollRow + row) * GRID_COLS + col;
         List<WardrobeCosmeticEntry> entries = this.orderedEntries(this.activeSlot);
         return index < entries.size() ? (WardrobeCosmeticEntry)entries.get(index) : null;
      } else {
         return null;
      }
   }

   private List<WardrobeCosmeticEntry> orderedEntries(WardrobeSlot slot) {
      if (this.orderedCache == null || this.orderedCacheSlot != slot || this.orderedCacheFloatyTab != this.floatyTab) {
         List<WardrobeCosmeticEntry> list = new ArrayList(this.state.entriesFor(slot));
         if (slot == WardrobeSlot.BALLOON) {
            list.removeIf(entry -> this.floatyTab != isFloatyEntry(entry));
         }
         list.sort(Comparator.comparingInt((e) -> e.getOwned() ? 0 : 1));
         this.orderedCache = list;
         this.orderedCacheSlot = slot;
         this.orderedCacheFloatyTab = this.floatyTab;
      }

      return this.orderedCache;
   }

   private static boolean isActuallyDyeable(WardrobeCosmeticEntry entry) {
      // Eevee Explorer Hood has a fixed authored texture; never show/send dye controls for it,
      // even if an older server config still marks it dyeable.
      return entry != null && entry.getDyeable() && !"eevee_explorer_hood".equals(entry.getId());
   }

   private WardrobeCosmeticEntry dyeTarget() {
      WardrobeCosmeticEntry selected = this.state.entry(this.state.getSelected(this.activeSlot));
      if (isActuallyDyeable(selected)) {
         return selected;
      } else {
         WardrobeCosmeticEntry equipped = this.state.equippedEntry(this.activeSlot);
         return isActuallyDyeable(equipped) ? equipped : null;
      }
   }

   private int entryCount() {
      return this.glowTab ? this.state.glows().size() : this.orderedEntries(this.activeSlot).size();
   }

   private int visibleRows() {
      return class_3532.method_15340((this.entryCount() + GRID_COLS - 1) / GRID_COLS, 1, GRID_ROWS);
   }

   private int maxScrollRow() {
      int rows = (this.entryCount() + GRID_COLS - 1) / GRID_COLS;
      return Math.max(0, rows - this.visibleRows());
   }

   private boolean overPreview(double mouseX, double mouseY) {
      return mouseX >= (double)this.previewX0() && mouseX < (double)this.previewX1() && mouseY >= (double)this.previewY0() && mouseY < (double)this.previewY1();
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (super.method_25402(mouseX, mouseY, button)) {
         return true;
      } else {
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
         } else if (button != 0) {
            return false;
         } else if (this.overPreview(mouseX, mouseY)) {
            this.draggingPreview = true;
            this.lastInteractMs = class_156.method_658();
            return true;
         } else if (this.glowTab) {
            WardrobeGlowEntry glow = this.hoveredGlow(mouseX, mouseY);
            if (glow != null) {
               PreviewUi.playClick();
               WardrobeNetworking.sendSetGlow(glow.getOwned() && glow.getEquipped() ? "" : glow.getId());

               return true;
            } else {
               return false;
            }
         } else {
            WardrobeCosmeticEntry entry = this.hoveredEntry(mouseX, mouseY);
            if (entry != null) {
               PreviewUi.playClick();
               if (!entry.getOwned()) {
                  // Server-authoritative buy + equip. If funds are insufficient, the server
                  // leaves ownership unchanged and returns an updated wardrobe state.
                  WardrobeNetworking.sendEquip(entry.getId());
               } else if (this.state.isEquipped(entry)) {
                  this.state.setSelected(this.activeSlot, (String)null);
                  this.state.unequipLocal(this.activeSlot);
                  WardrobeNetworking.sendUnequip(this.activeSlot);
               } else {
                  this.state.setSelected(this.activeSlot, (String)null);
                  this.state.equipLocal(entry);
                  this.equipAnimId = entry.getId();
                  this.equipAnimStart = class_156.method_658();
                  WardrobeNetworking.sendEquip(entry.getId());
               }

               return true;
            } else {
               class_1767 swatch = this.hoveredSwatch(mouseX, mouseY);
               if (swatch != null) {
                  WardrobeCosmeticEntry target = this.dyeTarget();
                  if (target != null) {
                     PreviewUi.playClick();
                     int rgb = swatch.method_7790() & 16777215;
                     this.state.setColorLocal(target.getId(), rgb);
                     if (target.getOwned()) {
                        WardrobeNetworking.sendSetColor(target.getId(), rgb);
                     }

                     return true;
                  }
               }

               return false;
            }
         }
      }
   }

   private class_1767 hoveredSwatch(double mouseX, double mouseY) {
      if (this.dyeTarget() == null) {
         return null;
      } else {
         int y = this.swatchY();
         if (!(mouseY < (double)y) && !(mouseY >= (double)(y + 9))) {
            if (mouseX < (double)this.gridX()) {
               return null;
            } else {
               int index = (int)((mouseX - (double)this.gridX()) / (double)10.0F);
               return index >= class_1767.values().length ? null : class_1767.values()[index];
            }
         } else {
            return null;
         }
      }
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (this.field_22787.field_1690.field_1822.method_1417(keyCode, scanCode)) {
         this.method_25419();
         return true;
      } else {
         return super.method_25404(keyCode, scanCode, modifiers);
      }
   }

   public boolean method_25406(double mouseX, double mouseY, int button) {
      if (this.draggingPreview && button == 0) {
         this.draggingPreview = false;
         this.lastInteractMs = class_156.method_658();
         return true;
      } else {
         return super.method_25406(mouseX, mouseY, button);
      }
   }

   public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingPreview && button == 0) {
         this.yaw = (this.yaw + (float)dragX) % 360.0F;
         this.pitch = class_3532.method_15363(this.pitch + (float)dragY * 0.5F, -20.0F, 20.0F);
         this.lastInteractMs = class_156.method_658();
         return true;
      } else {
         return super.method_25403(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean method_25401(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.overPreview(mouseX, mouseY) && scrollY != (double)0.0F) {
         float factor = scrollY > (double)0.0F ? 1.12F : 0.89285713F;
         this.previewZoom = class_3532.method_15363(this.previewZoom * factor, 0.5F, 2.6F);
         return true;
      } else {
         int x = this.gridX();
         int y = this.gridY();
         if (mouseX >= (double)x && mouseX < (double)(x + GRID_COLS * CELL + 6) && mouseY >= (double)y && mouseY < (double)(y + this.visibleRows() * CELL)) {
            this.scrollRow = class_3532.method_15340(this.scrollRow - (int)Math.signum(scrollY), 0, this.maxScrollRow());
            return true;
         } else {
            return super.method_25401(mouseX, mouseY, scrollX, scrollY);
         }
      }
   }

   private void cleanup() {
      this.state.clearPreviewOverrides();
      this.preview.close();
   }

   public void method_25419() {
      PreviewUi.playClose();
      this.cleanup();
      super.method_25419();
   }

   public void method_25432() {
      this.cleanup();
      super.method_25432();
   }

   private static void insetBevel(class_332 g, int x0, int y0, int x1, int y1, int shadow, int light) {
      g.method_25294(x0, y0, x1, y0 + 1, shadow);
      g.method_25294(x0, y0, x0 + 1, y1, shadow);
      g.method_25294(x0, y1 - 1, x1, y1, light);
      g.method_25294(x1 - 1, y0, x1, y1, light);
   }

   @Environment(EnvType.CLIENT)
   private final class TabButton extends class_4185 {
      private TabButton(int x, int y, int w, int h, class_2561 label, class_4185.class_4241 onPress) {
         super(x, y, w, h, label, onPress, field_40754);
      }

      protected void method_48579(class_332 g, int mx, int my, float pt) {
         boolean selected = !this.field_22763;
         boolean hover = this.method_49606() && this.field_22763;
         int x0 = this.method_46426();
         int y0 = this.method_46427();
         int x1 = x0 + this.method_25368();
         int y1 = y0 + this.method_25364();
         int bg = selected ? -14274468 : (hover ? -14801336 : -15459784);
         g.method_25294(x0, y0, x1, y1, bg);
         if (!selected) {
            g.method_25294(x0, y1 - 1, x1, y1, -16118238);
         }

         int tc = selected ? -2053377 : (hover ? -3418650 : -7035976);
         g.method_27534(WardrobeScreen.this.field_22793, this.method_25369(), (x0 + x1) / 2, y0 + (this.method_25364() - 8) / 2, tc);
      }
   }
}
