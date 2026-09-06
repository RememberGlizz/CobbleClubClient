package com.cobbleclub.client.gearpreview;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1304;
import net.minecraft.class_156;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;
import net.minecraft.class_5151;
import net.minecraft.class_5348;
import net.minecraft.class_5455;
import net.minecraft.class_2561.class_2562;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class GearPreviewScreen extends class_437 {
   private static final int PANEL_W = 456;
   private static final int PANEL_H = 252;
   private static final int GRID_COLS = 9;
   private static final int CELL = 26;
   private static final int MAX_ROWS = 6;
   private static final int TAB_H = 18;
   private static final float ICON_SCALE = 1.35F;
   private static final int PANEL_TOP = -199614136;
   private static final int PANEL_BOTTOM = -200601562;
   private static final int PANEL_BORDER_OUT = -16447985;
   private static final int PANEL_BORDER_IN = -13747610;
   private static final int HEADER_TOP = -14405538;
   private static final int HEADER_BOTTOM = -15459782;
   private static final int ACCENT = -6467875;
   private static final int TITLE_COLOR = -2053377;
   private static final int MUTED_TEXT = -7035976;
   private static final int SELECTED = -12474273;
   private static final int CONTENT_BG = -15723477;
   private static final int SLOT_BASE = -15064506;
   private static final int PREVIEW_TOP = -14998448;
   private static final int PREVIEW_BOTTOM = -16315880;
   private static final int SCROLL_TRACK = -15986650;
   private static final int SCROLL_THUMB = -10862962;
   private static final float MIN_ZOOM = 0.5F;
   private static final float MAX_ZOOM = 2.6F;
   private static final float ZOOM_STEP = 1.12F;
   private static final float MAX_PITCH = 20.0F;
   private static final float AUTO_SPIN_DEG_PER_MS = 0.03F;
   private static final long AUTO_SPIN_IDLE_MS = 1500L;
   private final List<GearCatalogPayload.GearCatalogSet> sets;
   private final List<class_2561> tabLabels;
   private final GearPreviewRenderer preview = new GearPreviewRenderer();
   private final EnumMap<class_1304, class_1799> overrides = new EnumMap(class_1304.class);
   private int activeSet;
   private int scrollRow;
   private class_1799 hovered;
   private boolean hidePlayer;
   private class_1799 focusItem;
   private class_1799 shownItem;
   private final Quaternionf itemOrientation = new Quaternionf();
   private float yaw;
   private float pitch;
   private float zoom = 1.0F;
   private boolean dragging;
   private long lastInteractMs;
   private long lastFrameMs = class_156.method_658();
   private List<TabRect> tabRects = List.of();

   public GearPreviewScreen(List<GearCatalogPayload.GearCatalogSet> sets) {
      super(class_2561.method_43470("Gear Preview"));
      this.sets = sets;
      this.tabLabels = deserializeTabLabels(sets);
      PreviewUi.playOpen();
   }

   private static List<class_2561> deserializeTabLabels(List<GearCatalogPayload.GearCatalogSet> sets) {
      class_5455 registry = class_310.method_1551().field_1687 != null ? class_310.method_1551().field_1687.method_30349() : null;
      List<class_2561> labels = new ArrayList(sets.size());

      for(GearCatalogPayload.GearCatalogSet set : sets) {
         class_2561 label = null;
         if (registry != null) {
            try {
               label = class_2562.method_10877(set.displayName(), registry);
            } catch (Exception var7) {
            }
         }

         labels.add(label != null ? label : class_2561.method_43470(set.id()));
      }

      return labels;
   }

   private List<TabRect> computeTabRects() {
      List<TabRect> rects = new ArrayList(this.sets.size());
      int startX = this.gridX();
      int rightEdge = this.panelLeft() + 456 - 6;
      int x = startX;
      int y = this.panelTop() + 22;

      for(int i = 0; i < this.sets.size(); ++i) {
         int w = this.field_22793.method_27525((class_5348)this.tabLabels.get(i)) + 10;
         if (x > startX && x + w > rightEdge) {
            x = startX;
            y += 18;
         }

         rects.add(new TabRect(i, x, y, w));
         x += w + 3;
      }

      return rects;
   }

   private int tabRows() {
      return this.tabRects.isEmpty() ? 1 : (((TabRect)this.tabRects.get(this.tabRects.size() - 1)).y() - (this.panelTop() + 22)) / 18 + 1;
   }

   private int panelLeft() {
      return (this.field_22789 - 456) / 2;
   }

   private int panelTop() {
      return (this.field_22790 - 252) / 2;
   }

   private int previewX0() {
      return this.panelLeft() + 8;
   }

   private int previewY0() {
      return this.panelTop() + 22;
   }

   private int previewX1() {
      return this.panelLeft() + 198;
   }

   private int previewY1() {
      return this.panelTop() + 252 - 28;
   }

   private int gridX() {
      return this.panelLeft() + 206;
   }

   private int gridY() {
      return this.panelTop() + 46 + (this.tabRows() - 1) * 18;
   }

   private List<class_1799> activeItems() {
      return this.sets.isEmpty() ? List.of() : ((GearCatalogPayload.GearCatalogSet)this.sets.get(this.activeSet)).items();
   }

   private int visibleRows() {
      int rows = (this.activeItems().size() + 9 - 1) / 9;
      return class_3532.method_15340(rows, 1, 6);
   }

   private int maxScrollRow() {
      int rows = (this.activeItems().size() + 9 - 1) / 9;
      return Math.max(0, rows - this.visibleRows());
   }

   protected void method_25426() {
      this.lastFrameMs = class_156.method_658();
      this.tabRects = this.computeTabRects();
      this.method_37063(new ThemedButton(this.panelLeft() + 8, this.panelTop() + 252 - 24, 96, 20, this.hidePlayerLabel(), (b) -> {
         this.hidePlayer = !this.hidePlayer;
         b.method_25355(this.hidePlayerLabel());
      }));
      this.method_37063(new ThemedButton(this.panelLeft() + 456 - 118, this.panelTop() + 252 - 24, 52, 20, class_2561.method_43470("Reset"), (b) -> {
         this.overrides.clear();
         this.focusItem = null;
         this.itemOrientation.identity();
         this.yaw = 0.0F;
         this.pitch = 0.0F;
         this.zoom = 1.0F;
      }));
      this.method_37063(new ThemedButton(this.panelLeft() + 456 - 60, this.panelTop() + 252 - 24, 52, 20, class_2561.method_43470("Close"), (b) -> this.method_25419()));
   }

   private class_2561 hidePlayerLabel() {
      return class_2561.method_43470(this.hidePlayer ? "Show Player" : "Hide Player");
   }

   private static class_1304 slotFor(class_1799 stack) {
      class_5151 equipable = class_5151.method_48957(stack);
      return equipable != null ? equipable.method_7685() : class_1304.field_6173;
   }

   private Map<class_1304, class_1799> resolvedEquipment() {
      EnumMap<class_1304, class_1799> resolved = new EnumMap(this.overrides);
      if (this.hovered != null) {
         resolved.put(slotFor(this.hovered), this.hovered);
      }

      return resolved;
   }

   public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
      long now = class_156.method_658();
      long dt = now - this.lastFrameMs;
      this.lastFrameMs = now;
      if (!this.dragging && !this.hidePlayer && now - this.lastInteractMs > 1500L) {
         this.yaw += 0.03F * (float)dt;
      }

      this.hovered = this.dragging ? null : this.hoveredItem((double)mouseX, (double)mouseY);
      this.method_25420(guiGraphics, mouseX, mouseY, partialTick);
      Starfield.draw(guiGraphics, 0, 0, this.field_22789, this.field_22790, now, 44, 1337L, 0.5F);
      int left = this.panelLeft();
      int top = this.panelTop();
      guiGraphics.method_25294(left - 3, top - 3, left + 456 + 3, top + 252 + 3, 1426063360);
      guiGraphics.method_25296(left, top, left + 456, top + 252, -199614136, -200601562);
      guiGraphics.method_49601(left - 1, top - 1, 458, 254, -16447985);
      guiGraphics.method_49601(left, top, 456, 252, -13747610);
      guiGraphics.method_25296(left, top, left + 456, top + 18, -14405538, -15459782);
      guiGraphics.method_25294(left, top + 18, left + 456, top + 19, -6467875);
      guiGraphics.method_27534(this.field_22793, this.method_25440(), left + 228, top + 5, -2053377);
      int px0 = this.previewX0();
      int py0 = this.previewY0();
      int px1 = this.previewX1();
      int py1 = this.previewY1();
      guiGraphics.method_25296(px0, py0, px1, py1, -14998448, -16315880);
      Starfield.draw(guiGraphics, px0, py0, px1, py1, now, 34, 91L, 0.85F);
      guiGraphics.method_49601(px0 - 1, py0 - 1, px1 - px0 + 2, py1 - py0 + 2, -13747610);
      if (this.hidePlayer) {
         class_1799 focus = this.hovered != null ? this.hovered : this.focusItem;
         if (focus == null || focus.method_7960()) {
            focus = (class_1799)this.resolvedEquipment().getOrDefault(class_1304.field_6173, class_1799.field_8037);
         }

         if (focus != this.shownItem) {
            this.shownItem = focus;
            this.itemOrientation.identity();
         }

         if (!focus.method_7960()) {
            GearPreviewRenderer.renderItem3D(guiGraphics, px0, py0, px1, py1, this.itemOrientation, this.zoom, focus);
         } else {
            guiGraphics.method_27534(this.field_22793, class_2561.method_43470("Select an item"), (px0 + px1) / 2, (py0 + py1) / 2 - 4, -7035976);
         }
      } else {
         this.preview.render(guiGraphics, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, this.resolvedEquipment());
      }

      if (this.inPreview((double)mouseX, (double)mouseY)) {
         guiGraphics.method_27534(this.field_22793, class_2561.method_43470("Drag • Scroll to zoom"), (px0 + px1) / 2, py1 - 11, -1063348548);
      }

      this.renderTabs(guiGraphics, mouseX, mouseY);
      this.renderGrid(guiGraphics, mouseX, mouseY);

      for(class_364 child : this.method_25396()) {
         if (child instanceof class_4068 renderable) {
            renderable.method_25394(guiGraphics, mouseX, mouseY, partialTick);
         }
      }

      if (this.hovered != null) {
         Tooltips.render(guiGraphics, this.field_22793, this.hovered, mouseX, mouseY);
      }

   }

   private void renderTabs(class_332 guiGraphics, int mouseX, int mouseY) {
      for(TabRect tab : this.tabRects) {
         boolean isActive = tab.index() == this.activeSet;
         boolean hover = mouseX >= tab.x() && mouseX < tab.x() + tab.w() && mouseY >= tab.y() && mouseY < tab.y() + 16;
         guiGraphics.method_25294(tab.x(), tab.y(), tab.x() + tab.w(), tab.y() + 16, isActive ? -14410694 : (hover ? -14936272 : -15528414));
         if (isActive) {
            guiGraphics.method_25294(tab.x(), tab.y() + 15, tab.x() + tab.w(), tab.y() + 16, -6467875);
         }

         int tint = isActive ? -1 : (hover ? -2962968 : -7035976);
         guiGraphics.method_51439(this.field_22793, (class_2561)this.tabLabels.get(tab.index()), tab.x() + 5, tab.y() + 4, tint, false);
      }

   }

   private void renderGrid(class_332 guiGraphics, int mouseX, int mouseY) {
      List<class_1799> items = this.activeItems();
      int x = this.gridX();
      int y = this.gridY();
      int rows = this.visibleRows();
      int gridW = 234;
      int gridH = rows * 26;
      guiGraphics.method_25294(x - 2, y - 2, x + gridW + 2, y + gridH + 2, -15723477);
      guiGraphics.method_49601(x - 2, y - 2, gridW + 4, gridH + 4, -13747610);
      guiGraphics.method_44379(x, y, x + gridW, y + gridH);
      int first = this.scrollRow * 9;

      for(int i = first; i < items.size() && i < first + 9 * rows; ++i) {
         class_1799 stack = (class_1799)items.get(i);
         int cellX = x + (i - first) % 9 * 26;
         int cellY = y + (i - first) / 9 * 26;
         guiGraphics.method_25294(cellX + 1, cellY + 1, cellX + 26 - 1, cellY + 26 - 1, -15064506);
         PreviewUi.renderScaledItem(guiGraphics, stack, cellX + 13 - 8, cellY + 13 - 8, 1.35F);
         boolean selected = this.overrides.get(slotFor(stack)) == stack;
         if (selected) {
            guiGraphics.method_49601(cellX, cellY, 26, 26, -12474273);
         }

         if (mouseX >= cellX && mouseX < cellX + 26 && mouseY >= cellY && mouseY < cellY + 26) {
            guiGraphics.method_49601(cellX, cellY, 26, 26, -1717743907);
         }
      }

      guiGraphics.method_44380();
      int maxScroll = this.maxScrollRow();
      if (maxScroll > 0) {
         int trackX = x + gridW + 2;
         guiGraphics.method_25294(trackX, y, trackX + 4, y + gridH, -15986650);
         int thumbH = Math.max(10, gridH * rows / (maxScroll + rows));
         int thumbY = y + (gridH - thumbH) * this.scrollRow / maxScroll;
         guiGraphics.method_25294(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
      }

   }

   private class_1799 hoveredItem(double mouseX, double mouseY) {
      int x = this.gridX();
      int y = this.gridY();
      if (!(mouseX < (double)x) && !(mouseX >= (double)(x + 234)) && !(mouseY < (double)y) && !(mouseY >= (double)(y + this.visibleRows() * 26))) {
         int col = (int)((mouseX - (double)x) / (double)26.0F);
         int row = (int)((mouseY - (double)y) / (double)26.0F);
         int index = (this.scrollRow + row) * 9 + col;
         List<class_1799> items = this.activeItems();
         return index >= 0 && index < items.size() ? (class_1799)items.get(index) : null;
      } else {
         return null;
      }
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      for(TabRect tab : this.tabRects) {
         if (mouseX >= (double)tab.x() && mouseX < (double)(tab.x() + tab.w()) && mouseY >= (double)tab.y() && mouseY < (double)(tab.y() + 16)) {
            if (tab.index() != this.activeSet) {
               this.activeSet = tab.index();
               this.scrollRow = 0;
            }

            return true;
         }
      }

      class_1799 clicked = this.hoveredItem(mouseX, mouseY);
      if (clicked != null) {
         class_1304 slot = slotFor(clicked);
         if (this.overrides.get(slot) == clicked) {
            this.overrides.remove(slot);
         } else {
            this.overrides.put(slot, clicked);
         }

         this.focusItem = clicked;
         return true;
      } else if (button == 0 && this.inPreview(mouseX, mouseY)) {
         this.dragging = true;
         this.lastInteractMs = class_156.method_658();
         return true;
      } else {
         return super.method_25402(mouseX, mouseY, button);
      }
   }

   public boolean method_25406(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.dragging = false;
      }

      return super.method_25406(mouseX, mouseY, button);
   }

   public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.dragging) {
         if (this.hidePlayer) {
            this.itemOrientation.premul((new Quaternionf()).rotationY((float)Math.toRadians(dragX)).rotateX((float)Math.toRadians(-dragY)));
         } else {
            this.yaw += (float)dragX;
            this.pitch = class_3532.method_15363(this.pitch - (float)dragY, -20.0F, 20.0F);
         }

         this.lastInteractMs = class_156.method_658();
         return true;
      } else {
         return super.method_25403(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean method_25401(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.inPreview(mouseX, mouseY)) {
         this.zoom = class_3532.method_15363(this.zoom * (float)Math.pow((double)1.12F, scrollY), 0.5F, 2.6F);
         this.lastInteractMs = class_156.method_658();
         return true;
      } else {
         int gx = this.gridX();
         int gy = this.gridY();
         if (mouseX >= (double)gx && mouseX < (double)(gx + 234 + 6) && mouseY >= (double)gy && mouseY < (double)(gy + this.visibleRows() * 26)) {
            this.scrollRow = class_3532.method_15340(this.scrollRow - (int)Math.signum(scrollY), 0, this.maxScrollRow());
            return true;
         } else {
            return super.method_25401(mouseX, mouseY, scrollX, scrollY);
         }
      }
   }

   private boolean inPreview(double mouseX, double mouseY) {
      return mouseX >= (double)this.previewX0() && mouseX < (double)this.previewX1() && mouseY >= (double)this.previewY0() && mouseY < (double)this.previewY1();
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (this.field_22787.field_1690.field_1822.method_1417(keyCode, scanCode)) {
         this.method_25419();
         return true;
      } else {
         return super.method_25404(keyCode, scanCode, modifiers);
      }
   }

   public void method_25419() {
      PreviewUi.playClose();
      this.preview.close();
      super.method_25419();
   }

   public boolean method_25421() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   private static record TabRect(int index, int x, int y, int w) {
   }
}
