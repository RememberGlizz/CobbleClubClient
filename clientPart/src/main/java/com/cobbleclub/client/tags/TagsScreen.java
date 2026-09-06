package com.cobbleclub.client.tags;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import com.cobbleclub.clubhouse.tags.protocol.TagsEntry;
import com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_124;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public class TagsScreen extends class_437 {
   private static final int PANEL_W = 440;
   private static final int PANEL_H = 240;
   private static final int ROW_H = 18;
   private static final int GRID_COLS = 3;
   private static final int CHIP_GAP = 2;
   private static final int SEARCH_W = 120;
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
   private static final int CHIP_BASE = -15064506;
   private static final int CHIP_HOVER = -14405546;
   private static final int UNOWNED_DIM = -1727591912;
   private static final int PREVIEW_TOP = -14998448;
   private static final int PREVIEW_BOTTOM = -16315880;
   private static final int SCROLL_TRACK = -15986650;
   private static final int SCROLL_THUMB = -10862962;
   private final class_2561 titleComponent;
   private final boolean showUnsetButton;
   private final class_2561 unsetLabel;
   private List<TagRow> rows;
   private boolean ownedOnly;
   private int scrollRow;
   private int lastRevision;
   private String search = "";
   private class_342 searchBox;

   public TagsScreen(TagsOpenMsg msg) {
      super(class_2561.method_43470("Tags"));
      this.titleComponent = PreviewUi.deserialize(msg.getTitle(), "Tags");
      this.showUnsetButton = msg.getShowUnsetButton();
      this.unsetLabel = PreviewUi.deserialize(msg.getUnsetLabel(), "Remove Tag");
      this.rows = deserializeEntries(msg.getEntries());
      PreviewUi.playOpen();
   }

   public void applyState(TagsStateMsg msg) {
      if (msg.getRevision() > this.lastRevision) {
         this.lastRevision = msg.getRevision();
         this.rows = deserializeEntries(msg.getEntries());
         this.scrollRow = class_3532.method_15340(this.scrollRow, 0, this.maxScrollRow());
      }
   }

   private static List<TagRow> deserializeEntries(List<TagsEntry> entries) {
      List<TagRow> out = new ArrayList();
      if (entries == null) {
         return out;
      } else {
         for(TagsEntry entry : entries) {
            if (entry != null && entry.getId() != null) {
               List<class_2561> description = new ArrayList();
               if (entry.getDescription() != null) {
                  for(String line : entry.getDescription()) {
                     description.add(PreviewUi.deserialize(line, ""));
                  }
               }

               class_2561 tag = PreviewUi.deserialize(entry.getTagJson(), entry.getId());
               out.add(new TagRow(entry.getId(), tag, (tag.getString() + " " + entry.getId()).toLowerCase(Locale.ROOT), description, entry.getOwned(), entry.getActive(), entry.getOwnerCount()));
            }
         }

         return out;
      }
   }

   private List<TagRow> visibleRows() {
      List<TagRow> out = new ArrayList();
      String query = this.search.trim().toLowerCase(Locale.ROOT);

      for(TagRow row : this.rows) {
         if ((!this.ownedOnly || row.owned()) && (query.isEmpty() || row.plain().contains(query))) {
            out.add(row);
         }
      }

      return out;
   }

   private int panelLeft() {
      return (this.field_22789 - 440) / 2;
   }

   private int panelTop() {
      return (this.field_22790 - 240) / 2;
   }

   private int previewY0() {
      return this.panelTop() + 22;
   }

   private int previewY1() {
      return this.previewY0() + 16;
   }

   private int listX0() {
      return this.panelLeft() + 10;
   }

   private int listX1() {
      return this.panelLeft() + 440 - 16;
   }

   private int listY0() {
      return this.previewY1() + 4;
   }

   private int listY1() {
      return this.panelTop() + 240 - 30;
   }

   private int chipW() {
      return (this.listX1() - this.listX0() - 4) / 3;
   }

   private int visibleRowCount() {
      return Math.max(1, (this.listY1() - this.listY0()) / 18);
   }

   private static int gridRows(int count) {
      return (count + 3 - 1) / 3;
   }

   private int maxScrollRow() {
      return Math.max(0, gridRows(this.visibleRows().size()) - this.visibleRowCount());
   }

   protected void method_25426() {
      this.searchBox = new class_342(this.field_22793, this.panelLeft() + 440 - 8 - 120, this.previewY0(), 120, 16, class_2561.method_43470("Search"));
      this.searchBox.method_47404(class_2561.method_43470("Search...").method_27692(class_124.field_1063));
      this.searchBox.method_1852(this.search);
      this.searchBox.method_1863((value) -> {
         this.search = value;
         this.scrollRow = 0;
      });
      this.method_37063(this.searchBox);
      int y = this.panelTop() + 240 - 24;
      int x = this.panelLeft() + 8;
      if (this.showUnsetButton) {
         this.method_37063(new ThemedButton(x, y, 100, 20, this.unsetLabel, (b) -> {
            this.rows = withActive(this.rows, (String)null);
            TagsNetworking.sendUnset();
         }));
         x += 106;
      }

      this.method_37063(new ThemedButton(x, y, 110, 20, this.ownedOnlyLabel(), (b) -> {
         this.ownedOnly = !this.ownedOnly;
         this.scrollRow = 0;
         b.method_25355(this.ownedOnlyLabel());
      }));
      this.method_37063(new ThemedButton(this.panelLeft() + 440 - 60, y, 52, 20, class_2561.method_43470("Close"), (b) -> this.method_25419()));
   }

   private class_2561 ownedOnlyLabel() {
      return class_2561.method_43470("Owned only: " + (this.ownedOnly ? "✔" : "✗"));
   }

   private static List<TagRow> withActive(List<TagRow> rows, String activeId) {
      List<TagRow> out = new ArrayList(rows.size());

      for(TagRow row : rows) {
         boolean active = row.id().equals(activeId);
         out.add(active == row.active() ? row : new TagRow(row.id(), row.tag(), row.plain(), row.description(), row.owned(), active, row.ownerCount()));
      }

      return out;
   }

   public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
      long now = class_156.method_658();
      this.method_25420(guiGraphics, mouseX, mouseY, partialTick);
      Starfield.draw(guiGraphics, 0, 0, this.field_22789, this.field_22790, now, 44, 1337L, 0.5F);
      int left = this.panelLeft();
      int top = this.panelTop();
      guiGraphics.method_25294(left - 3, top - 3, left + 440 + 3, top + 240 + 3, 1426063360);
      guiGraphics.method_25296(left, top, left + 440, top + 240, -199614136, -200601562);
      guiGraphics.method_49601(left - 1, top - 1, 442, 242, -16447985);
      guiGraphics.method_49601(left, top, 440, 240, -13747610);
      guiGraphics.method_25296(left, top, left + 440, top + 18, -14405538, -15459782);
      guiGraphics.method_25294(left, top + 18, left + 440, top + 19, -6467875);
      guiGraphics.method_27534(this.field_22793, this.titleComponent, left + 220, top + 5, -2053377);
      TagRow hovered = this.hoveredRow((double)mouseX, (double)mouseY);
      this.renderPreviewStrip(guiGraphics, hovered);
      this.renderGrid(guiGraphics, hovered);

      for(class_364 child : this.method_25396()) {
         if (child instanceof class_4068 renderable) {
            renderable.method_25394(guiGraphics, mouseX, mouseY, partialTick);
         }
      }

      if (hovered != null) {
         Tooltips.render(guiGraphics, this.field_22793, this.tooltipFor(hovered), mouseX, mouseY);
      }

   }

   private void renderPreviewStrip(class_332 guiGraphics, TagRow hovered) {
      int x0 = this.panelLeft() + 8;
      int x1 = this.panelLeft() + 440 - 8 - 120 - 6;
      guiGraphics.method_25296(x0, this.previewY0(), x1, this.previewY1(), -14998448, -16315880);
      guiGraphics.method_49601(x0 - 1, this.previewY0() - 1, x1 - x0 + 2, this.previewY1() - this.previewY0() + 2, -13747610);
      TagRow shown = hovered;
      if (hovered == null) {
         for(TagRow row : this.rows) {
            if (row.active()) {
               shown = row;
               break;
            }
         }
      }

      class_746 player = class_310.method_1551().field_1724;
      String name = player != null ? player.method_7334().getName() : "Player";
      class_5250 preview = class_2561.method_43473();
      if (shown != null) {
         preview.method_10852(shown.tag()).method_27693(" ");
      }

      preview.method_10852(class_2561.method_43470(name).method_27692(class_124.field_1068)).method_10852(class_2561.method_43470(": Hello!").method_27692(class_124.field_1080));
      guiGraphics.method_44379(x0, this.previewY0(), x1, this.previewY1());
      guiGraphics.method_27535(this.field_22793, preview, x0 + 5, this.previewY0() + 4, -1);
      guiGraphics.method_44380();
   }

   private void renderGrid(class_332 guiGraphics, TagRow hovered) {
      List<TagRow> visible = this.visibleRows();
      int x0 = this.listX0();
      int x1 = this.listX1();
      int y0 = this.listY0();
      int y1 = y0 + this.visibleRowCount() * 18;
      guiGraphics.method_25294(x0 - 2, y0 - 2, x1 + 2, y1 + 2, -15723477);
      guiGraphics.method_49601(x0 - 2, y0 - 2, x1 - x0 + 4, y1 - y0 + 4, -13747610);
      if (visible.isEmpty()) {
         guiGraphics.method_27534(this.field_22793, class_2561.method_43470("No tags to show"), (x0 + x1) / 2, (y0 + y1) / 2 - 4, -7035976);
      } else {
         int chipW = this.chipW();
         guiGraphics.method_44379(x0, y0, x1, y1);
         int first = this.scrollRow * 3;

         for(int i = first; i < visible.size() && i < first + 3 * this.visibleRowCount(); ++i) {
            TagRow row = (TagRow)visible.get(i);
            int chipX = x0 + (i - first) % 3 * (chipW + 2);
            int chipY = y0 + (i - first) / 3 * 18;
            guiGraphics.method_25294(chipX, chipY + 1, chipX + chipW, chipY + 18 - 1, row == hovered ? -14405546 : -15064506);
            guiGraphics.method_44379(chipX + 1, chipY + 1, chipX + chipW - 1, chipY + 18 - 1);
            guiGraphics.method_27535(this.field_22793, row.tag(), chipX + 5, chipY + 5, -1);
            guiGraphics.method_44380();
            if (!row.owned()) {
               guiGraphics.method_25294(chipX, chipY + 1, chipX + chipW, chipY + 18 - 1, -1727591912);
            }

            if (row.active()) {
               guiGraphics.method_49601(chipX, chipY + 1, chipW, 16, -12474273);
            } else if (row == hovered) {
               guiGraphics.method_49601(chipX, chipY + 1, chipW, 16, -1717743907);
            }
         }

         guiGraphics.method_44380();
         int maxScroll = this.maxScrollRow();
         if (maxScroll > 0) {
            int trackX = x1 + 4;
            int trackH = y1 - y0;
            guiGraphics.method_25294(trackX, y0, trackX + 4, y1, -15986650);
            int thumbH = Math.max(10, trackH * this.visibleRowCount() / (maxScroll + this.visibleRowCount()));
            int thumbY = y0 + (trackH - thumbH) * this.scrollRow / maxScroll;
            guiGraphics.method_25294(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
         }

      }
   }

   private List<class_2561> tooltipFor(TagRow row) {
      List<class_2561> lines = new ArrayList();
      lines.add(row.tag());
      if (!row.description().isEmpty()) {
         lines.add(class_2561.method_43473());
         lines.addAll(row.description());
      }

      lines.add(class_2561.method_43473());
      if (row.ownerCount() >= 0) {
         lines.add(class_2561.method_43470("Owners: ").method_27692(class_124.field_1080).method_10852(class_2561.method_43470(String.valueOf(row.ownerCount())).method_27692(class_124.field_1068)));
      }

      lines.add(class_2561.method_43470("Active: ").method_27692(class_124.field_1080).method_10852(row.active() ? class_2561.method_43470("Activated").method_27692(class_124.field_1060) : class_2561.method_43470("Deactivated").method_27692(class_124.field_1061)));
      lines.add(class_2561.method_43470("Obtained: ").method_27692(class_124.field_1080).method_10852(row.owned() ? class_2561.method_43470("Obtained").method_27692(class_124.field_1060) : class_2561.method_43470("Not obtained").method_27692(class_124.field_1061)));
      return lines;
   }

   private TagRow hoveredRow(double mouseX, double mouseY) {
      int x0 = this.listX0();
      int y0 = this.listY0();
      if (!(mouseX < (double)x0) && !(mouseX >= (double)this.listX1()) && !(mouseY < (double)y0) && !(mouseY >= (double)(y0 + this.visibleRowCount() * 18))) {
         int col = (int)((mouseX - (double)x0) / (double)(this.chipW() + 2));
         if (col >= 3) {
            return null;
         } else {
            int index = (this.scrollRow + (int)((mouseY - (double)y0) / (double)18.0F)) * 3 + col;
            List<TagRow> visible = this.visibleRows();
            return index >= 0 && index < visible.size() ? (TagRow)visible.get(index) : null;
         }
      } else {
         return null;
      }
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (this.searchBox != null && this.searchBox.method_25370() && !this.searchBox.method_25405(mouseX, mouseY)) {
         this.searchBox.method_25365(false);
      }

      TagRow clicked = this.hoveredRow(mouseX, mouseY);
      if (clicked != null) {
         if (clicked.owned() && !clicked.active()) {
            this.rows = withActive(this.rows, clicked.id());
            TagsNetworking.sendSet(clicked.id());
            PreviewUi.playClick();
         }

         return true;
      } else {
         return super.method_25402(mouseX, mouseY, button);
      }
   }

   public boolean method_25401(double mouseX, double mouseY, double scrollX, double scrollY) {
      int x0 = this.listX0();
      int y0 = this.listY0();
      if (mouseX >= (double)(x0 - 2) && mouseX < (double)(this.listX1() + 8) && mouseY >= (double)y0 && mouseY < (double)(y0 + this.visibleRowCount() * 18)) {
         this.scrollRow = class_3532.method_15340(this.scrollRow - (int)Math.signum(scrollY), 0, this.maxScrollRow());
         return true;
      } else {
         return super.method_25401(mouseX, mouseY, scrollX, scrollY);
      }
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (this.searchBox != null && this.searchBox.method_25370()) {
         return super.method_25404(keyCode, scanCode, modifiers);
      } else if (this.field_22787.field_1690.field_1822.method_1417(keyCode, scanCode)) {
         this.method_25419();
         return true;
      } else {
         return super.method_25404(keyCode, scanCode, modifiers);
      }
   }

   public void method_25419() {
      PreviewUi.playClose();
      super.method_25419();
   }

   public boolean method_25421() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   private static record TagRow(String id, class_2561 tag, String plain, List<class_2561> description, boolean owned, boolean active, int ownerCount) {
   }
}
