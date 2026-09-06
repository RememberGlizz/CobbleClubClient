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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.Text.Serialization;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class GearPreviewScreen extends Screen {
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
   private final List<Text> tabLabels;
   private final GearPreviewRenderer preview = new GearPreviewRenderer();
   private final EnumMap<EquipmentSlot, ItemStack> overrides = new EnumMap(EquipmentSlot.class);
   private int activeSet;
   private int scrollRow;
   private ItemStack hovered;
   private boolean hidePlayer;
   private ItemStack focusItem;
   private ItemStack shownItem;
   private final Quaternionf itemOrientation = new Quaternionf();
   private float yaw;
   private float pitch;
   private float zoom = 1.0F;
   private boolean dragging;
   private long lastInteractMs;
   private long lastFrameMs = Util.getMeasuringTimeMs();
   private List<TabRect> tabRects = List.of();

   public GearPreviewScreen(List<GearCatalogPayload.GearCatalogSet> sets) {
      super(Text.literal("Gear Preview"));
      this.sets = sets;
      this.tabLabels = deserializeTabLabels(sets);
      PreviewUi.playOpen();
   }

   private static List<Text> deserializeTabLabels(List<GearCatalogPayload.GearCatalogSet> sets) {
      DynamicRegistryManager registry = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getRegistryManager() : null;
      List<Text> labels = new ArrayList(sets.size());

      for(GearCatalogPayload.GearCatalogSet set : sets) {
         Text label = null;
         if (registry != null) {
            try {
               label = Serialization.fromJson(set.displayName(), registry);
            } catch (Exception var7) {
            }
         }

         labels.add(label != null ? label : Text.literal(set.id()));
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
         int w = this.textRenderer.getWidth((StringVisitable)this.tabLabels.get(i)) + 10;
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
      return (this.width - 456) / 2;
   }

   private int panelTop() {
      return (this.height - 252) / 2;
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

   private List<ItemStack> activeItems() {
      return this.sets.isEmpty() ? List.of() : ((GearCatalogPayload.GearCatalogSet)this.sets.get(this.activeSet)).items();
   }

   private int visibleRows() {
      int rows = (this.activeItems().size() + 9 - 1) / 9;
      return MathHelper.clamp(rows, 1, 6);
   }

   private int maxScrollRow() {
      int rows = (this.activeItems().size() + 9 - 1) / 9;
      return Math.max(0, rows - this.visibleRows());
   }

   protected void init() {
      this.lastFrameMs = Util.getMeasuringTimeMs();
      this.tabRects = this.computeTabRects();
      this.addDrawableChild(new ThemedButton(this.panelLeft() + 8, this.panelTop() + 252 - 24, 96, 20, this.hidePlayerLabel(), (b) -> {
         this.hidePlayer = !this.hidePlayer;
         b.setMessage(this.hidePlayerLabel());
      }));
      this.addDrawableChild(new ThemedButton(this.panelLeft() + 456 - 118, this.panelTop() + 252 - 24, 52, 20, Text.literal("Reset"), (b) -> {
         this.overrides.clear();
         this.focusItem = null;
         this.itemOrientation.identity();
         this.yaw = 0.0F;
         this.pitch = 0.0F;
         this.zoom = 1.0F;
      }));
      this.addDrawableChild(new ThemedButton(this.panelLeft() + 456 - 60, this.panelTop() + 252 - 24, 52, 20, Text.literal("Close"), (b) -> this.close()));
   }

   private Text hidePlayerLabel() {
      return Text.literal(this.hidePlayer ? "Show Player" : "Hide Player");
   }

   private static EquipmentSlot slotFor(ItemStack stack) {
      Equipment equipable = Equipment.fromStack(stack);
      return equipable != null ? equipable.getSlotType() : EquipmentSlot.MAINHAND;
   }

   private Map<EquipmentSlot, ItemStack> resolvedEquipment() {
      EnumMap<EquipmentSlot, ItemStack> resolved = new EnumMap(this.overrides);
      if (this.hovered != null) {
         resolved.put(slotFor(this.hovered), this.hovered);
      }

      return resolved;
   }

   public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
      long now = Util.getMeasuringTimeMs();
      long dt = now - this.lastFrameMs;
      this.lastFrameMs = now;
      if (!this.dragging && !this.hidePlayer && now - this.lastInteractMs > 1500L) {
         this.yaw += 0.03F * (float)dt;
      }

      this.hovered = this.dragging ? null : this.hoveredItem((double)mouseX, (double)mouseY);
      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      Starfield.draw(guiGraphics, 0, 0, this.width, this.height, now, 44, 1337L, 0.5F);
      int left = this.panelLeft();
      int top = this.panelTop();
      guiGraphics.fill(left - 3, top - 3, left + 456 + 3, top + 252 + 3, 1426063360);
      guiGraphics.fillGradient(left, top, left + 456, top + 252, -199614136, -200601562);
      guiGraphics.drawBorder(left - 1, top - 1, 458, 254, -16447985);
      guiGraphics.drawBorder(left, top, 456, 252, -13747610);
      guiGraphics.fillGradient(left, top, left + 456, top + 18, -14405538, -15459782);
      guiGraphics.fill(left, top + 18, left + 456, top + 19, -6467875);
      guiGraphics.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), left + 228, top + 5, -2053377);
      int px0 = this.previewX0();
      int py0 = this.previewY0();
      int px1 = this.previewX1();
      int py1 = this.previewY1();
      guiGraphics.fillGradient(px0, py0, px1, py1, -14998448, -16315880);
      Starfield.draw(guiGraphics, px0, py0, px1, py1, now, 34, 91L, 0.85F);
      guiGraphics.drawBorder(px0 - 1, py0 - 1, px1 - px0 + 2, py1 - py0 + 2, -13747610);
      if (this.hidePlayer) {
         ItemStack focus = this.hovered != null ? this.hovered : this.focusItem;
         if (focus == null || focus.isEmpty()) {
            focus = (ItemStack)this.resolvedEquipment().getOrDefault(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         }

         if (focus != this.shownItem) {
            this.shownItem = focus;
            this.itemOrientation.identity();
         }

         if (!focus.isEmpty()) {
            GearPreviewRenderer.renderItem3D(guiGraphics, px0, py0, px1, py1, this.itemOrientation, this.zoom, focus);
         } else {
            guiGraphics.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select an item"), (px0 + px1) / 2, (py0 + py1) / 2 - 4, -7035976);
         }
      } else {
         this.preview.render(guiGraphics, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, this.resolvedEquipment());
      }

      if (this.inPreview((double)mouseX, (double)mouseY)) {
         guiGraphics.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Drag • Scroll to zoom"), (px0 + px1) / 2, py1 - 11, -1063348548);
      }

      this.renderTabs(guiGraphics, mouseX, mouseY);
      this.renderGrid(guiGraphics, mouseX, mouseY);

      for(Element child : this.children()) {
         if (child instanceof Drawable renderable) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
         }
      }

      if (this.hovered != null) {
         Tooltips.render(guiGraphics, this.textRenderer, this.hovered, mouseX, mouseY);
      }

   }

   private void renderTabs(DrawContext guiGraphics, int mouseX, int mouseY) {
      for(TabRect tab : this.tabRects) {
         boolean isActive = tab.index() == this.activeSet;
         boolean hover = mouseX >= tab.x() && mouseX < tab.x() + tab.w() && mouseY >= tab.y() && mouseY < tab.y() + 16;
         guiGraphics.fill(tab.x(), tab.y(), tab.x() + tab.w(), tab.y() + 16, isActive ? -14410694 : (hover ? -14936272 : -15528414));
         if (isActive) {
            guiGraphics.fill(tab.x(), tab.y() + 15, tab.x() + tab.w(), tab.y() + 16, -6467875);
         }

         int tint = isActive ? -1 : (hover ? -2962968 : -7035976);
         guiGraphics.drawText(this.textRenderer, (Text)this.tabLabels.get(tab.index()), tab.x() + 5, tab.y() + 4, tint, false);
      }

   }

   private void renderGrid(DrawContext guiGraphics, int mouseX, int mouseY) {
      List<ItemStack> items = this.activeItems();
      int x = this.gridX();
      int y = this.gridY();
      int rows = this.visibleRows();
      int gridW = 234;
      int gridH = rows * 26;
      guiGraphics.fill(x - 2, y - 2, x + gridW + 2, y + gridH + 2, -15723477);
      guiGraphics.drawBorder(x - 2, y - 2, gridW + 4, gridH + 4, -13747610);
      guiGraphics.enableScissor(x, y, x + gridW, y + gridH);
      int first = this.scrollRow * 9;

      for(int i = first; i < items.size() && i < first + 9 * rows; ++i) {
         ItemStack stack = (ItemStack)items.get(i);
         int cellX = x + (i - first) % 9 * 26;
         int cellY = y + (i - first) / 9 * 26;
         guiGraphics.fill(cellX + 1, cellY + 1, cellX + 26 - 1, cellY + 26 - 1, -15064506);
         PreviewUi.renderScaledItem(guiGraphics, stack, cellX + 13 - 8, cellY + 13 - 8, 1.35F);
         boolean selected = this.overrides.get(slotFor(stack)) == stack;
         if (selected) {
            guiGraphics.drawBorder(cellX, cellY, 26, 26, -12474273);
         }

         if (mouseX >= cellX && mouseX < cellX + 26 && mouseY >= cellY && mouseY < cellY + 26) {
            guiGraphics.drawBorder(cellX, cellY, 26, 26, -1717743907);
         }
      }

      guiGraphics.disableScissor();
      int maxScroll = this.maxScrollRow();
      if (maxScroll > 0) {
         int trackX = x + gridW + 2;
         guiGraphics.fill(trackX, y, trackX + 4, y + gridH, -15986650);
         int thumbH = Math.max(10, gridH * rows / (maxScroll + rows));
         int thumbY = y + (gridH - thumbH) * this.scrollRow / maxScroll;
         guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
      }

   }

   private ItemStack hoveredItem(double mouseX, double mouseY) {
      int x = this.gridX();
      int y = this.gridY();
      if (!(mouseX < (double)x) && !(mouseX >= (double)(x + 234)) && !(mouseY < (double)y) && !(mouseY >= (double)(y + this.visibleRows() * 26))) {
         int col = (int)((mouseX - (double)x) / (double)26.0F);
         int row = (int)((mouseY - (double)y) / (double)26.0F);
         int index = (this.scrollRow + row) * 9 + col;
         List<ItemStack> items = this.activeItems();
         return index >= 0 && index < items.size() ? (ItemStack)items.get(index) : null;
      } else {
         return null;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      for(TabRect tab : this.tabRects) {
         if (mouseX >= (double)tab.x() && mouseX < (double)(tab.x() + tab.w()) && mouseY >= (double)tab.y() && mouseY < (double)(tab.y() + 16)) {
            if (tab.index() != this.activeSet) {
               this.activeSet = tab.index();
               this.scrollRow = 0;
            }

            return true;
         }
      }

      ItemStack clicked = this.hoveredItem(mouseX, mouseY);
      if (clicked != null) {
         EquipmentSlot slot = slotFor(clicked);
         if (this.overrides.get(slot) == clicked) {
            this.overrides.remove(slot);
         } else {
            this.overrides.put(slot, clicked);
         }

         this.focusItem = clicked;
         return true;
      } else if (button == 0 && this.inPreview(mouseX, mouseY)) {
         this.dragging = true;
         this.lastInteractMs = Util.getMeasuringTimeMs();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.dragging = false;
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.dragging) {
         if (this.hidePlayer) {
            this.itemOrientation.premul((new Quaternionf()).rotationY((float)Math.toRadians(dragX)).rotateX((float)Math.toRadians(-dragY)));
         } else {
            this.yaw += (float)dragX;
            this.pitch = MathHelper.clamp(this.pitch - (float)dragY, -20.0F, 20.0F);
         }

         this.lastInteractMs = Util.getMeasuringTimeMs();
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.inPreview(mouseX, mouseY)) {
         this.zoom = MathHelper.clamp(this.zoom * (float)Math.pow((double)1.12F, scrollY), 0.5F, 2.6F);
         this.lastInteractMs = Util.getMeasuringTimeMs();
         return true;
      } else {
         int gx = this.gridX();
         int gy = this.gridY();
         if (mouseX >= (double)gx && mouseX < (double)(gx + 234 + 6) && mouseY >= (double)gy && mouseY < (double)(gy + this.visibleRows() * 26)) {
            this.scrollRow = MathHelper.clamp(this.scrollRow - (int)Math.signum(scrollY), 0, this.maxScrollRow());
            return true;
         } else {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
         }
      }
   }

   private boolean inPreview(double mouseX, double mouseY) {
      return mouseX >= (double)this.previewX0() && mouseX < (double)this.previewX1() && mouseY >= (double)this.previewY0() && mouseY < (double)this.previewY1();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
         this.close();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void close() {
      PreviewUi.playClose();
      this.preview.close();
      super.close();
   }

   public boolean shouldPause() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   private static record TabRect(int index, int x, int y, int w) {
   }
}
