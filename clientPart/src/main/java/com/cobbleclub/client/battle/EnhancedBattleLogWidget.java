package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_408;
import net.minecraft.class_437;
import net.minecraft.class_5481;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class EnhancedBattleLogWidget {
   private static final int MIN_W = 90;
   private static final int MIN_H = 40;
   private static final int RESIZE_HANDLE = 10;
   private static final int PAD_X = 3;
   private static final int PAD_Y = 2;
   private static final int ENTRY_GAP = 3;
   private static final int RULE_GAP = 4;
   private static final int TEXT_COLOR = -1;
   private static final int SCROLL_TRACK = 419430399;
   private static final int SCROLL_THUMB = 1509949439;
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
   private static int cachedRevision = -1;
   private static int cachedWidth = -1;
   private static List<Row> cachedRows = List.of();

   private EnhancedBattleLogWidget() {
   }

   private static int lineHeight() {
      Objects.requireNonNull(class_310.method_1551().field_1772);
      return 9 + 2;
   }

   private static Rect rect() {
      BattleConfig c = BattleConfig.get();
      class_1041 window = class_310.method_1551().method_22683();
      int screenW = window.method_4486();
      int screenH = window.method_4502();
      int w = Math.min(c.logWidth, screenW);
      int h = Math.min(c.logHeight, screenH);
      int freeW = Math.max(0, screenW - w);
      int freeH = Math.max(0, screenH - h);
      int lift = c.hideNativeLog ? 45 : 135;
      int x = c.logAnchorX >= (double)0.0F ? (int)Math.round(c.logAnchorX * (double)freeW) : freeW - 2;
      int y = c.logAnchorY >= (double)0.0F ? (int)Math.round(c.logAnchorY * (double)freeH) : freeH - lift;
      return new Rect(class_3532.method_15340(x, 0, freeW), class_3532.method_15340(y, 0, freeH), w, h);
   }

   private static void setAnchor(int x, int y, int w, int h) {
      class_1041 window = class_310.method_1551().method_22683();
      int freeW = Math.max(0, window.method_4486() - w);
      int freeH = Math.max(0, window.method_4502() - h);
      BattleConfig c = BattleConfig.get();
      c.logAnchorX = freeW == 0 ? (double)0.0F : class_3532.method_15350((double)x / (double)freeW, (double)0.0F, (double)1.0F);
      c.logAnchorY = freeH == 0 ? (double)0.0F : class_3532.method_15350((double)y / (double)freeH, (double)0.0F, (double)1.0F);
   }

   public static void renderUnderBattle(class_332 graphics) {
      if (CobblemonClient.INSTANCE.getBattle() != null) {
         class_437 screen = class_310.method_1551().field_1755;
         if (screen != null && !(screen instanceof BattleGUI)) {
            if (screen instanceof class_408) {
               render(graphics, true);
            }
         } else {
            render(graphics, false);
         }

      }
   }

   private static void render(class_332 graphics, boolean dimmed) {
      BattleConfig config = BattleConfig.get();
      if (config.enhancedLog) {
         class_327 font = class_310.method_1551().field_1772;
         Rect rect = rect();
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
            List<Row> rows = layout(font, contentW);
            if (scrollOffset > 0) {
               scrollOffset += Math.max(0, rows.size() - lastTotalRows);
            }

            lastTotalRows = rows.size();
            graphics.method_44379(x + inset, contentY, x + w - inset, contentBottom);

            int maxScroll;
            try {
               maxScroll = drawRows(graphics, font, rows, contentX, contentY, contentBottom, contentW, scrollOffset);
            } finally {
               graphics.method_44380();
            }

            scrollOffset = class_3532.method_15340(scrollOffset, 0, maxScroll);
            if (maxScroll > 0) {
               int trackH = contentBottom - contentY - 10;
               drawScrollbar(graphics, x + w - inset - 2, contentY, trackH, rows.size(), maxScroll);
            }
         }

         drawHandle(graphics, x + w - inset, y + h - inset);
         if (dimmed) {
            graphics.method_25294(x, y, x + w, y + h, -1728053248);
         }

      }
   }

   private static void drawHandle(class_332 graphics, int rx, int ry) {
      for(int line = 0; line < 3; ++line) {
         int len = 2 + line * 2;

         for(int i = 0; i <= len; ++i) {
            int px = rx - 2 - i;
            int py = ry - 2 - (len - i);
            graphics.method_25294(px, py, px + 1, py + 1, -7500403);
         }
      }

   }

   private static List<Row> layout(class_327 font, int width) {
      int revision = BattleLogStore.revision();
      if (revision == cachedRevision && width == cachedWidth) {
         return cachedRows;
      } else {
         List<Row> rows = new ArrayList();

         for(BattleLogStore.Entry entry : BattleLogStore.snapshot()) {
            if (entry.turnMarker()) {
               rows.add(separatorRow(entry.turn()));
            } else {
               boolean gapAbove = !rows.isEmpty() && !((Row)rows.get(rows.size() - 1)).separator();
               boolean firstLine = true;

               for(class_5481 seq : font.method_1728(entry.text(), Math.max(1, width))) {
                  rows.add(new Row(seq, false, firstLine && gapAbove));
                  firstLine = false;
               }
            }
         }

         cachedRevision = revision;
         cachedWidth = width;
         cachedRows = rows;
         return rows;
      }
   }

   private static Row separatorRow(int turn) {
      return turn <= 0 ? new Row((class_5481)null, true, false) : new Row(BattleState.turnLabel(turn).method_30937(), true, false);
   }

   private static int rowHeight(Row row, int lineH) {
      return row.separator() ? lineH + 2 : lineH + (row.entryStart() ? 3 : 0);
   }

   private static int drawRows(class_332 graphics, class_327 font, List<Row> rows, int x, int top, int bottom, int width, int scrollOffset) {
      int lineH = lineHeight();
      int total = rows.size();
      int totalHeight = 0;

      for(Row row : rows) {
         totalHeight += rowHeight(row, lineH);
      }

      bottom = Math.min(bottom, top + totalHeight);
      int available = bottom - top;
      int acc = 0;
      int fit = 0;

      for(int i = total - 1; i >= 0; --i) {
         acc += rowHeight((Row)rows.get(i), lineH);
         if (acc > available && fit > 0) {
            break;
         }

         ++fit;
      }

      int maxScroll = Math.max(0, total - fit);
      scrollOffset = class_3532.method_15340(scrollOffset, 0, maxScroll);
      int rowY = bottom;

      for(int i = total - 1 - scrollOffset; i >= 0; --i) {
         Row row = (Row)rows.get(i);
         int rh = rowHeight(row, lineH);
         rowY -= rh;
         if (rowY + rh <= top) {
            break;
         }

         drawRow(graphics, font, row, x, rowY, rh, width);
      }

      return maxScroll;
   }

   private static void drawRow(class_332 graphics, class_327 font, Row row, int x, int rowY, int rh, int width) {
      if (row.separator()) {
         drawSeparator(graphics, font, row, x, rowY, rh, width);
      } else {
         graphics.method_35720(font, row.seq(), x, rowY + (row.entryStart() ? 3 : 0), -1);
      }

   }

   private static void drawSeparator(class_332 graphics, class_327 font, Row row, int x, int rowY, int rh, int width) {
      int lineY = rowY + rh / 2;
      if (row.seq() == null) {
         graphics.method_25294(x, lineY, x + width, lineY + 1, -6052957);
      } else {
         int labelWidth = font.method_30880(row.seq());
         int textX = x + (width - labelWidth) / 2;
         graphics.method_25294(x, lineY, Math.max(x, textX - 4), lineY + 1, -6052957);
         graphics.method_25294(Math.min(x + width, textX + labelWidth + 4), lineY, x + width, lineY + 1, -6052957);
         class_5481 var10002 = row.seq();
         Objects.requireNonNull(font);
         graphics.method_35720(font, var10002, textX, rowY + (rh - 9) / 2 + 1, -1);
      }
   }

   private static void drawScrollbar(class_332 graphics, int trackX, int trackY, int trackH, int total, int maxScroll) {
      if (trackH >= 4) {
         graphics.method_25294(trackX, trackY, trackX + 1, trackY + trackH, 419430399);
         int fit = Math.max(1, total - maxScroll);
         int thumbH = class_3532.method_15340(trackH * fit / Math.max(1, total), 6, trackH);
         float scrolled = maxScroll == 0 ? 0.0F : (float)scrollOffset / (float)maxScroll;
         int thumbY = trackY + Math.round((float)(trackH - thumbH) * (1.0F - scrolled));
         graphics.method_25294(trackX, thumbY, trackX + 1, thumbY + thumbH, 1509949439);
      }
   }

   public static boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (BattleConfig.get().enhancedLog && button == 0) {
         Rect rect = rect();
         int x = rect.x();
         int y = rect.y();
         int w = rect.w();
         int h = rect.h();
         if (!(mouseX < (double)x) && !(mouseX > (double)(x + w)) && !(mouseY < (double)y) && !(mouseY > (double)(y + h))) {
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
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean mouseDragged(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         class_310 mc = class_310.method_1551();
         int screenW = mc.method_22683().method_4486();
         int screenH = mc.method_22683().method_4502();
         BattleConfig config = BattleConfig.get();
         if (dragging) {
            Rect rect = rect();
            setAnchor(class_3532.method_15340((int)(mouseX - grabDx), 0, Math.max(0, screenW - rect.w())), class_3532.method_15340((int)(mouseY - grabDy), 0, Math.max(0, screenH - rect.h())), rect.w(), rect.h());
            return true;
         } else if (resizing) {
            config.logWidth = class_3532.method_15340((int)(mouseX + grabDx - (double)pinnedX), 90, Math.max(90, screenW - pinnedX));
            config.logHeight = class_3532.method_15340((int)(mouseY + grabDy - (double)pinnedY), 40, Math.max(40, screenH - pinnedY));
            setAnchor(pinnedX, pinnedY, config.logWidth, config.logHeight);
            return true;
         } else {
            return false;
         }
      }
   }

   public static boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
      if (BattleConfig.get().enhancedLog && vertical != (double)0.0F) {
         Rect rect = rect();
         if (!(mouseX < (double)rect.x()) && !(mouseX > (double)(rect.x() + rect.w())) && !(mouseY < (double)rect.y()) && !(mouseY > (double)(rect.y() + rect.h()))) {
            scrollOffset = Math.max(0, scrollOffset + accumulateScroll(vertical));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int accumulateScroll(double vertical) {
      scrollAccum += vertical;
      int notches = (int)scrollAccum;
      scrollAccum -= (double)notches;
      return notches * 2;
   }

   public static void resetScroll() {
      scrollOffset = 0;
      scrollAccum = (double)0.0F;
   }

   public static void tick() {
      if ((dragging || resizing) && GLFW.glfwGetMouseButton(class_310.method_1551().method_22683().method_4490(), 0) == 0) {
         dragging = false;
         resizing = false;
         BattleConfig.save();
      }

   }

   @Environment(EnvType.CLIENT)
   private static record Row(class_5481 seq, boolean separator, boolean entryStart) {
   }

   @Environment(EnvType.CLIENT)
   private static record Rect(int x, int y, int w, int h) {
   }
}
