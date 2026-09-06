package com.cobbleclub.client.claims.panel;

import com.cobbleclub.client.claims.ClaimsState;
import com.cobbleclub.client.ui.ClubScrollbar;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry;
import com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class ClaimListPanel {
   private static final int ROW_H = 30;
   private static final int SUB_ROW_H = 16;
   private static final int HEADER_H = 16;
   private final ClaimsState state;
   private final TextRenderer font;
   private final Consumer<String> onSelect;
   private final BooleanSupplier trusted;
   private int x0;
   private int y0;
   private int x1;
   private int y1;
   private int scroll;

   public ClaimListPanel(ClaimsState state, TextRenderer font, Consumer<String> onSelect, BooleanSupplier trusted) {
      this.state = state;
      this.font = font;
      this.onSelect = onSelect;
      this.trusted = trusted;
   }

   public void setBounds(int x0, int y0, int x1, int y1) {
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
   }

   public boolean contains(double mouseX, double mouseY) {
      return mouseX >= (double)this.x0 && mouseX < (double)this.x1 && mouseY >= (double)this.y0 && mouseY < (double)this.y1;
   }

   private List<ClaimDetailEntry> claims() {
      return this.trusted.getAsBoolean() ? this.state.trustedClaims() : this.state.ownedClaims();
   }

   private List<Row> rows() {
      List<Row> claimRows = new ArrayList();

      for(ClaimDetailEntry claim : this.claims()) {
         claimRows.add(new Row(claim, (SubClaimEntry)null, 30, ClaimsState.standingInside(claim)));
      }

      claimRows.sort(Comparator.comparing((rowx) -> !rowx.standing()));
      List<Row> rows = new ArrayList();

      for(Row row : claimRows) {
         rows.add(row);
         if (row.claim().getSubClaims() != null) {
            for(SubClaimEntry sub : row.claim().getSubClaims()) {
               if (sub != null) {
                  rows.add(new Row(row.claim(), sub, 16, false));
               }
            }
         }
      }

      return rows;
   }

   private int contentHeight(List<Row> rows) {
      int height = 4;

      for(Row row : rows) {
         height += row.height();
      }

      return height;
   }

   private int maxScroll(List<Row> rows) {
      return Math.max(0, this.contentHeight(rows) - (this.y1 - this.y0 - 16));
   }

   private int listTop() {
      return this.y0 + 16;
   }

   public void render(DrawContext g, int mouseX, int mouseY, String selectedClaimId) {
      g.fill(this.x0, this.y0, this.x1, this.y1, -15723477);
      g.drawBorder(this.x0 - 1, this.y0 - 1, this.x1 - this.x0 + 2, this.y1 - this.y0 + 2, -16447985);
      g.drawBorder(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0, -13747610);
      boolean trustedTab = this.trusted.getAsBoolean();
      List<Row> rows = this.rows();
      this.scroll = MathHelper.clamp(this.scroll, 0, this.maxScroll(rows));
      String header = (trustedTab ? "Trusted Claims (" : "Your Claims (") + this.claims().size() + ")";
      g.drawTextWithShadow(this.font, Text.literal(header).formatted(Formatting.GRAY), this.x0 + 8, this.y0 + 5, -7035976);
      g.fill(this.x0 + 1, this.listTop() - 1, this.x1 - 1, this.listTop(), -13747610);
      if (rows.isEmpty()) {
         g.drawCenteredTextWithShadow(this.font, Text.literal(trustedTab ? "No trusted claims" : "No claims yet"), (this.x0 + this.x1) / 2, (this.y0 + this.y1) / 2 - 10, -7035976);
         g.drawCenteredTextWithShadow(this.font, Text.literal(trustedTab ? "Claims others trust you in appear here" : "Claim land on the Map tab").formatted(Formatting.GRAY), (this.x0 + this.x1) / 2, (this.y0 + this.y1) / 2 + 2, -7035976);
      } else {
         g.enableScissor(this.x0 + 1, this.listTop(), this.x1 - 1, this.y1 - 1);
         int y = this.listTop() + 2 - this.scroll;

         for(Row row : rows) {
            if (y + row.height() >= this.listTop() && y <= this.y1) {
               boolean hover = this.contains((double)mouseX, (double)mouseY) && mouseY >= Math.max(y, this.listTop()) && mouseY < y + row.height();
               if (row.sub() == null) {
                  this.renderClaimRow(g, row, y, hover, row.claim().getClaimId().equals(selectedClaimId));
               } else {
                  g.enableScissor(this.x0 + 4, y, this.x1 - 8, y + row.height());
                  TextRenderer var10001 = this.font;
                  String var10002 = row.sub().getName();
                  g.drawTextWithShadow(var10001, Text.literal("└ " + var10002).formatted(Formatting.GRAY), this.x0 + 18, y + 4, hover ? -1 : -7035976);
                  g.disableScissor();
               }
            }

            y += row.height();
         }

         if (!trustedTab && this.maxScroll(rows) == 0 && y + 26 < this.y1) {
            g.drawCenteredTextWithShadow(this.font, Text.literal("Shift+drag on the Map tab to claim more land").formatted(Formatting.GRAY), (this.x0 + this.x1) / 2, y + 12, -7035976);
         }

         g.disableScissor();
         ClubScrollbar.draw(g, this.x1 - 5, this.listTop() + 2, this.y1 - 2, this.scroll, this.maxScroll(rows));
      }
   }

   private void renderClaimRow(DrawContext g, Row row, int y, boolean hover, boolean selected) {
      ClaimDetailEntry claim = row.claim();
      g.fill(this.x0 + 2, y, this.x1 - 7, y + 30 - 2, hover ? -14405546 : -15064506);
      g.fill(this.x0 + 2, y, this.x0 + 5, y + 30 - 2, claim.isOwner() ? -6467875 : -2054081);
      if (selected) {
         g.drawBorder(this.x0 + 2, y, this.x1 - this.x0 - 9, 28, -6467875);
      }

      int w = claim.getBox().getMaxX() - claim.getBox().getMinX() + 1;
      int d = claim.getBox().getMaxZ() - claim.getBox().getMinZ() + 1;
      String stats = ClaimsState.fmtBlocks(claim.getArea()) + " blocks";
      int subs = claim.getSubClaims() != null ? claim.getSubClaims().size() : 0;
      String statsMeta = w + "x" + d + (subs > 0 ? " · " + subs + (subs == 1 ? " sub" : " subs") : "");
      int statsX = this.x1 - 12 - Math.max(this.font.getWidth(stats), this.font.getWidth(statsMeta));
      g.enableScissor(this.x0 + 6, y, statsX - 4, y + 30);
      g.drawTextWithShadow(this.font, Text.literal(claim.getName()), this.x0 + 10, y + 5, -1);
      if (row.standing()) {
         g.drawTextWithShadow(this.font, Text.literal("Standing in this one").formatted(Formatting.GREEN), this.x0 + 10 + this.font.getWidth(claim.getName()) + 8, y + 5, -11141291);
      }

      String zone = claim.getZone() != null ? claim.getZone() : claim.getDimension();
      String meta = claim.isOwner() ? zone : zone + " · " + claim.getOwnerName();
      g.drawTextWithShadow(this.font, Text.literal(meta).formatted(Formatting.GRAY), this.x0 + 10, y + 17, -7035976);
      g.disableScissor();
      g.drawTextWithShadow(this.font, stats, this.x1 - 12 - this.font.getWidth(stats), y + 5, -2053377);
      g.drawTextWithShadow(this.font, statsMeta, this.x1 - 12 - this.font.getWidth(statsMeta), y + 17, -7035976);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.contains(mouseX, mouseY) && !(mouseY < (double)this.listTop())) {
         int y = this.listTop() + 2 - this.scroll;

         for(Row row : this.rows()) {
            if (mouseY >= (double)y && mouseY < (double)(y + row.height())) {
               this.onSelect.accept(row.claim().getClaimId());
               PreviewUi.playClick();
               return true;
            }

            y += row.height();
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
      if (!this.contains(mouseX, mouseY)) {
         return false;
      } else {
         this.scroll = MathHelper.clamp(this.scroll - (int)(scrollY * (double)30.0F), 0, this.maxScroll(this.rows()));
         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   private static record Row(ClaimDetailEntry claim, SubClaimEntry sub, int height, boolean standing) {
   }
}
