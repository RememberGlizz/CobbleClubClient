package com.cobbleclub.client.claims;

import com.cobbleclub.client.claims.map.ChunkSelection;
import com.cobbleclub.client.claims.map.MapTileCache;
import com.cobbleclub.client.claims.map.MapView;
import com.cobbleclub.client.claims.panel.ClaimDetailPanel;
import com.cobbleclub.client.claims.panel.ClaimListPanel;
import com.cobbleclub.client.ui.ClubScrollbar;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.clubhouse.claims.protocol.BudgetInfo;
import com.cobbleclub.clubhouse.claims.protocol.ChunkRect;
import com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsMapTilesMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsStateMsg;
import com.cobbleclub.clubhouse.claims.protocol.MapClaimEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ClaimsScreen extends Screen {
   private static final int MARGIN = 8;
   private static final int HEADER_H = 24;
   private static final int STATUS_H = 14;
   private static final int ACTION_BAR_H = 22;
   private static final int MAX_PANEL_W = 640;
   private static final int MAX_PANEL_H = 400;
   private final ClaimsState state;
   private final MapTileCache tiles;
   private final MapView mapView;
   private final ClaimListPanel listPanel;
   private final ClaimDetailPanel detailPanel;
   private Tab tab;
   private ThemedButton mapTabButton;
   private ThemedButton claimsTabButton;
   private ThemedButton trustedTabButton;
   private ThemedButton zoomInButton;
   private ThemedButton zoomOutButton;
   private ThemedButton centerButton;
   private ThemedButton confirmButton;
   private ThemedButton cancelButton;
   private ThemedButton helpButton;
   private int budgetTextLeft;
   private static final int CONFIRM_W = 300;
   private static final int CONFIRM_H = 118;
   private boolean confirmActive;
   private Text confirmTitle;
   private Text confirmMessage;
   private Runnable confirmAction;
   private static final int HELP_LINE_H = 11;
   private static final int HELP_TITLE_H = 22;
   private static final int HELP_FOOTER_H = 30;
   private boolean helpActive;
   private int helpScroll;
   private List<OrderedText> helpCache;
   private int helpCacheWidth;
   private TextFieldWidget invMoveFocusedBox;

   public void requestConfirm(Text title, Text message, Runnable action) {
      this.confirmActive = true;
      this.confirmTitle = title;
      this.confirmMessage = message;
      this.confirmAction = action;
      PreviewUi.playClick();
   }

   private void dismissConfirm() {
      this.confirmActive = false;
      this.confirmAction = null;
   }

   private int confirmLeft() {
      return (this.width - 300) / 2;
   }

   private int confirmTop() {
      return (this.height - 118) / 2;
   }

   private Boolean confirmButtonAt(double mouseX, double mouseY) {
      int by = this.confirmTop() + 118 - 34;
      if (!(mouseY < (double)by) && !(mouseY >= (double)(by + 20))) {
         int cx = this.confirmLeft() + 150;
         if (mouseX >= (double)(cx - 96) && mouseX < (double)(cx - 6)) {
            return Boolean.TRUE;
         } else {
            return mouseX >= (double)(cx + 6) && mouseX < (double)(cx + 96) ? Boolean.FALSE : null;
         }
      } else {
         return null;
      }
   }

   public ClaimsScreen(ClaimsState state) {
      super(Text.literal("Claims"));
      this.state = state;
      this.tiles = new MapTileCache(state.dimension, state.playerX >> 4, state.playerZ >> 4, state.mapRadiusChunks);
      this.mapView = new MapView(state, this.tiles);
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      this.listPanel = new ClaimListPanel(state, font, this::selectClaim, () -> this.tab == ClaimsScreen.Tab.TRUSTED);
      this.detailPanel = new ClaimDetailPanel(state, font, this::showOnMap, this::teleportTo, this::requestConfirm);
      this.tab = "MAP".equals(state.initialTab) && state.mapEnabled ? ClaimsScreen.Tab.MAP : ("TRUSTED".equals(state.initialTab) ? ClaimsScreen.Tab.TRUSTED : ClaimsScreen.Tab.CLAIMS);
      ClaimDetailEntry focusClaim = state.focusClaimId != null ? state.claimById(state.focusClaimId) : null;
      if (focusClaim != null) {
         this.selectClaim(state.focusClaimId);
         if (this.tab == ClaimsScreen.Tab.CLAIMS && !focusClaim.isOwner()) {
            this.tab = ClaimsScreen.Tab.TRUSTED;
         }
      }

      if (!state.suppressOpenSound) {
         PreviewUi.playOpen();
      }

   }

   public void applyState(ClaimsStateMsg msg) {
      if (this.state.applyState(msg)) {
         this.detailPanel.reconcile();
      }

   }

   public Element getFocused() {
      TextFieldWidget box = this.detailPanel.focusedEditBox();
      return (Element)(box != null ? box : super.getFocused());
   }

   public void acceptTiles(ClaimsMapTilesMsg msg) {
      this.tiles.accept(msg);
   }

   private void selectClaim(String claimId) {
      this.detailPanel.select(claimId);
   }

   private void teleportTo(ClaimDetailEntry claim) {
      ClaimsNetworking.sendTeleport(claim.getClaimId());
      PreviewUi.playClick();
      this.close();
   }

   private void showOnMap(ClaimDetailEntry claim) {
      if (this.state.mapEnabled) {
         this.tab = ClaimsScreen.Tab.MAP;
         this.mapView.centerOn((double)(claim.getBox().getMinX() + claim.getBox().getMaxX()) / (double)2.0F, (double)(claim.getBox().getMinZ() + claim.getBox().getMaxZ()) / (double)2.0F);
         PreviewUi.playClick();
      }
   }

   private void dismissOverlayPanel() {
      if (this.overlayMode() && this.mapView.selection().isActive()) {
         this.selectClaim((String)null);
      }

   }

   private int panelW() {
      return Math.min(Math.min(this.width - 28, 520), Math.max((int)((double)this.width * 0.60), 340));
   }

   private int panelH() {
      return Math.min(Math.min(this.height - 28, 320), Math.max((int)((double)this.height * 0.60), 220));
   }

   private int panelLeft() {
      return (this.width - this.panelW()) / 2;
   }

   private int panelTop() {
      return (this.height - this.panelH()) / 2;
   }

   private int panelRight() {
      return this.panelLeft() + this.panelW();
   }

   private int panelBottom() {
      return this.panelTop() + this.panelH();
   }

   private int detailPanelWidth() {
      return MathHelper.clamp((int)((double)this.panelW() * 0.38), 190, 260);
   }

   private boolean detailVisible() {
      return this.detailPanel.claim() != null;
   }

   private boolean overlayMode() {
      return this.panelW() < 380;
   }

   private int contentLeft() {
      return this.panelLeft() + 5;
   }

   private int contentTop() {
      return this.panelTop() + 24 + 5;
   }

   private int contentBottom() {
      return this.panelBottom() - 5 - (this.tab == ClaimsScreen.Tab.MAP ? 16 : 0);
   }

   private int mapTop() {
      return this.contentTop() + 22 + 2;
   }

   private int contentRightEdge() {
      boolean docked = this.detailVisible() && !this.overlayMode();
      return docked ? this.panelRight() - 5 - this.detailPanelWidth() - 4 : this.panelRight() - 5;
   }

   private void layout() {
      this.mapView.setBounds(this.contentLeft(), this.mapTop(), this.contentRightEdge(), this.contentBottom());
      this.listPanel.setBounds(this.contentLeft(), this.contentTop(), this.contentRightEdge(), this.contentBottom());
      int detailBottom = Math.min(this.contentBottom(), this.contentTop() + this.detailPanel.desiredHeight());
      if (this.detailVisible() && this.tab != ClaimsScreen.Tab.MAP) {
         // Details replace the list on My Claims/Trusted Claims. Rendering the list behind
         // a detail pane caused text bleed-through on several GUI scales.
         this.detailPanel.setBounds(this.contentLeft(), this.contentTop(), this.panelRight() - 5, this.contentBottom());
      } else {
         this.detailPanel.setBounds(this.panelRight() - 5 - this.detailPanelWidth(), this.contentTop(), this.panelRight() - 5, detailBottom);
      }
   }

   protected void init() {
      this.layout();
      int tabX = this.panelLeft() + 4;
      if (this.state.mapEnabled) {
         this.mapTabButton = (ThemedButton)this.addDrawableChild(new ThemedButton(tabX, this.panelTop() + 2, this.tabWidth("Map"), 20, Text.literal("Map"), (b) -> this.switchTab(ClaimsScreen.Tab.MAP)));
         tabX += this.mapTabButton.getWidth() + 4;
      }

      this.claimsTabButton = (ThemedButton)this.addDrawableChild(new ThemedButton(tabX, this.panelTop() + 2, this.tabWidth("My Claims"), 20, Text.literal("My Claims"), (b) -> this.switchTab(ClaimsScreen.Tab.CLAIMS)));
      tabX += this.claimsTabButton.getWidth() + 4;
      this.trustedTabButton = (ThemedButton)this.addDrawableChild(new ThemedButton(tabX, this.panelTop() + 2, this.tabWidth("Trusted Claims"), 20, Text.literal("Trusted Claims"), (b) -> this.switchTab(ClaimsScreen.Tab.TRUSTED)));
      this.addDrawableChild(new ThemedButton(this.panelRight() - 56, this.panelTop() + 2, 52, 20, Text.literal("Close"), (b) -> this.close()));
      this.zoomInButton = (ThemedButton)this.addDrawableChild(new ThemedButton(0, 0, 16, 16, Text.literal("+"), (b) -> this.mapView.zoomBy(1, this.mapViewCenterX(), this.mapViewCenterY())));
      this.zoomOutButton = (ThemedButton)this.addDrawableChild(new ThemedButton(0, 0, 16, 16, Text.literal("-"), (b) -> this.mapView.zoomBy(-1, this.mapViewCenterX(), this.mapViewCenterY())));
      this.centerButton = (ThemedButton)this.addDrawableChild(new ThemedButton(0, 0, 52, 16, Text.literal("Center"), (b) -> this.mapView.centerOnPlayer()));
      if (!this.state.helpLines.isEmpty()) {
         this.helpButton = (ThemedButton)this.addDrawableChild(new ThemedButton(0, this.panelTop() + 2, 20, 20, Text.literal("?"), (b) -> this.toggleHelp()));
         this.helpButton.setTooltip(Tooltip.of(this.state.screenText("help_title")));
      }

      this.confirmButton = (ThemedButton)this.addDrawableChild(new ThemedButton(0, 0, 24, 18, Text.literal("✔"), ThemedButton.Variant.GREEN, (b) -> this.confirmSelection()));
      this.confirmButton.setTooltip(Tooltip.of(Text.literal("Confirm selection (Enter)")));
      this.cancelButton = (ThemedButton)this.addDrawableChild(new ThemedButton(0, 0, 24, 18, Text.literal("✕"), ThemedButton.Variant.RED, (b) -> this.mapView.selection().clear()));
      this.cancelButton.setTooltip(Tooltip.of(Text.literal("Cancel selection (Esc)")));
      this.detailPanel.createWidgets((x$0) -> {
         ClickableWidget var10000 = (ClickableWidget)this.addDrawableChild(x$0);
      });
   }

   private int tabWidth(String label) {
      return this.textRenderer.getWidth(label) + 14;
   }

   private double mapViewCenterX() {
      return (double)(this.contentLeft() + this.contentRightEdge()) / (double)2.0F;
   }

   private double mapViewCenterY() {
      return (double)(this.mapTop() + this.contentBottom()) / (double)2.0F;
   }

   private void switchTab(Tab newTab) {
      if (this.tab != newTab) {
         this.tab = newTab;
         if (newTab == ClaimsScreen.Tab.MAP) {
            this.selectClaim((String)null);
         }

         PreviewUi.playClick();
      }

   }

   private void buyBlocks() {
      ClaimsNetworking.sendBuyBlocks(this.tab.name());
      PreviewUi.playClick();
   }

   private void confirmSelection() {
      ChunkSelection selection = this.mapView.selection();
      if (selection.isActive() && this.mapView.selectionIssue() == MapView.SelectionIssue.NONE) {
         ChunkRect rect = selection.toRect();
         if (selection.isResize()) {
            ClaimsNetworking.sendResize(selection.resizingClaimId(), rect);
            selection.clear();
            PreviewUi.playClick();
         } else {
            int chunks = (rect.getMaxCx() - rect.getMinCx() + 1) * (rect.getMaxCz() - rect.getMinCz() + 1);
            String chunkLabel = chunks + (chunks == 1 ? " chunk" : " chunks");
            this.requestConfirm(this.state.screenText("confirm_create_title"), this.state.screenText("confirm_create_body", "chunks", chunkLabel, "size", ClaimsState.fmtBlocks(chunks * 256)), () -> {
               this.state.optimisticCreate(rect);
               ClaimsNetworking.sendCreate(rect);
               this.mapView.selection().clear();
            });
         }
      }
   }

   public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
      this.layout();
      this.invMoveFocusedBox = this.detailPanel.focusedEditBox();
      this.renderBackground(g, mouseX, mouseY, partialTick);
      Starfield.draw(g, 0, 0, this.width, this.height, Util.getMeasuringTimeMs(), 44, 9241L, 0.5F);
      g.fill(this.panelLeft() - 3, this.panelTop() - 3, this.panelRight() + 3, this.panelBottom() + 3, 1426063360);
      g.fillGradient(this.panelLeft(), this.panelTop(), this.panelRight(), this.panelBottom(), -199614136, -200601562);
      g.drawBorder(this.panelLeft() - 1, this.panelTop() - 1, this.panelW() + 2, this.panelH() + 2, -16447985);
      g.drawBorder(this.panelLeft(), this.panelTop(), this.panelW(), this.panelH(), -13747610);
      this.renderHeader(g, mouseX, mouseY);
      if (this.tab == ClaimsScreen.Tab.MAP) {
         this.renderActionBar(g);
         this.mapView.render(g, mouseX, mouseY, this.detailPanel.selectedClaimId());
         this.renderStatusStrip(g, mouseX, mouseY);
      } else if (!this.detailVisible()) {
         // Never draw the claims/trusted list underneath an open detail pane.
         this.listPanel.render(g, mouseX, mouseY, this.detailPanel.selectedClaimId());
      }

      if (this.detailVisible()) {
         this.detailPanel.render(g, mouseX, mouseY);
      }

      this.updateFloatingWidgets();
      this.detailPanel.updateWidgets();

      for(Element child : this.children()) {
         if (child instanceof Drawable renderable) {
            renderable.render(g, mouseX, mouseY, partialTick);
         }
      }

      this.renderBanner(g);
      if (this.confirmActive) {
         this.renderConfirmDialog(g, mouseX, mouseY);
      } else if (this.helpActive) {
         this.renderHelpDialog(g, mouseX, mouseY);
      } else {
         this.renderTooltips(g, mouseX, mouseY);
      }

   }

   private void beginModal(DrawContext g, int x, int y, int w, int h) {
      g.getMatrices().push();
      g.getMatrices().translate(0.0F, 0.0F, 400.0F);
      g.fill(0, 0, this.width, this.height, -1342177280);
      g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 1426063360);
      g.fillGradient(x, y, x + w, y + h, -199614136, -200601562);
      g.drawBorder(x - 1, y - 1, w + 2, h + 2, -16447985);
      g.drawBorder(x, y, w, h, -6467875);
   }

   private void renderConfirmDialog(DrawContext g, int mouseX, int mouseY) {
      int x = this.confirmLeft();
      int y = this.confirmTop();
      this.beginModal(g, x, y, 300, 118);
      g.drawCenteredTextWithShadow(this.textRenderer, this.confirmTitle, x + 150, y + 12, -2053377);
      int msgY = y + 34;

      for(OrderedText line : this.textRenderer.wrapLines(this.confirmMessage, 276)) {
         g.drawCenteredTextWithShadow(this.textRenderer, line, x + 150, msgY, -2962968);
         msgY += 11;
      }

      Boolean over = this.confirmButtonAt((double)mouseX, (double)mouseY);
      int by = y + 118 - 34;
      int cx = x + 150;
      this.drawConfirmButton(g, cx - 96, by, 90, "Confirm", -12474273, Boolean.TRUE.equals(over));
      this.drawConfirmButton(g, cx + 6, by, 90, "Cancel", -2734768, Boolean.FALSE.equals(over));
      g.getMatrices().pop();
   }

   private void drawConfirmButton(DrawContext g, int x, int y, int w, String label, int accent, boolean hover) {
      g.fill(x, y, x + w, y + 20, hover ? -14405546 : -15064506);
      g.drawBorder(x, y, w, 20, accent);
      g.drawCenteredTextWithShadow(this.textRenderer, label, x + w / 2, y + 6, hover ? -1 : accent);
   }

   private void renderHeader(DrawContext g, int mouseX, int mouseY) {
      g.fillGradient(this.panelLeft(), this.panelTop(), this.panelRight(), this.panelTop() + 24, -14405538, -15459782);
      g.fill(this.panelLeft(), this.panelTop() + 24, this.panelRight(), this.panelTop() + 24 + 1, -6467875);
      BudgetInfo budget = this.state.budget();
      int remaining = this.state.effectiveRemaining();
      int color = remaining <= 0 ? -2734768 : (remaining < budget.getTotal() / 10 ? -2054081 : -12474273);
      String used = ClaimsState.fmtBlocks(Math.max(0, budget.getTotal() - remaining));
      String text = remaining < 0
              ? "Claim Blocks: " + ClaimsState.fmtBlocks(-remaining) + " over budget"
              : "Claim Blocks: " + ClaimsState.fmtBlocks(remaining) + " left (" + used + " used)";
      int avail = this.budgetTextRight() - this.tabsRight() - 6;
      if (this.textRenderer.getWidth(text) > avail) {
         text = remaining < 0 ? ClaimsState.fmtBlocks(-remaining) + " over" : ClaimsState.fmtBlocks(remaining) + " left";
      }

      if (this.textRenderer.getWidth(text) > avail) {
         text = ClaimsState.fmtBlocks(remaining) + "/" + ClaimsState.fmtBlocks(budget.getTotal());
      }

      this.budgetTextLeft = this.budgetTextRight() - this.textRenderer.getWidth(text);
      boolean buyHover = this.state.canBuyBlocks && this.overBudgetText((double)mouseX, (double)mouseY);
      if (this.state.canBuyBlocks) {
         g.fill(this.budgetTextLeft - 4, this.panelTop() + 5, this.budgetTextRight() + 4, this.panelTop() + 19, buyHover ? -14405546 : -15064506);
         g.drawBorder(this.budgetTextLeft - 4, this.panelTop() + 5, this.budgetTextRight() - this.budgetTextLeft + 8, 14, buyHover ? -6467875 : -13747610);
      }

      g.drawTextWithShadow(this.textRenderer, text, this.budgetTextLeft, this.panelTop() + 8, buyHover ? -1 : color);
      if (this.state.adminTargetName != null) {
         int titleLeft = this.tabsRight() + 6;
         int titleSpan = this.helpButtonLeft() - 6 - titleLeft;
         if (titleSpan >= this.textRenderer.getWidth(this.state.title)) {
            g.drawCenteredTextWithShadow(this.textRenderer, this.state.title, titleLeft + titleSpan / 2, this.panelTop() + 8, -2053377);
         }
      }

      ThemedButton active = this.tab == ClaimsScreen.Tab.MAP ? this.mapTabButton : (this.tab == ClaimsScreen.Tab.CLAIMS ? this.claimsTabButton : this.trustedTabButton);
      if (active != null) {
         g.fill(active.getX() + 2, this.panelTop() + 24 - 2, active.getX() + active.getWidth() - 2, this.panelTop() + 24 - 1, -2053377);
      }

   }

   private int budgetTextRight() {
      return this.panelRight() - 64;
   }

   private boolean overBudgetText(double mouseX, double mouseY) {
      return mouseY >= (double)(this.panelTop() + 4) && mouseY < (double)(this.panelTop() + 20) && mouseX >= (double)(this.budgetTextLeft - 4) && mouseX < (double)(this.budgetTextRight() + 4);
   }

   private int helpButtonLeft() {
      return this.budgetTextLeft - 30;
   }

   private int tabsRight() {
      return this.trustedTabButton == null ? this.panelLeft() : this.trustedTabButton.getX() + this.trustedTabButton.getWidth();
   }

   private void toggleHelp() {
      this.helpActive = !this.helpActive;
      this.helpScroll = 0;
      PreviewUi.playClick();
   }

   private int helpWidth() {
      return Math.min(360, this.width - 40);
   }

   private List<OrderedText> helpLines() {
      if (this.helpCache != null && this.helpCacheWidth == this.helpWidth()) {
         return this.helpCache;
      } else {
         List<OrderedText> lines = new ArrayList();

         for(Text line : this.state.helpLines) {
            if (line.getString().isEmpty()) {
               lines.add(OrderedText.EMPTY);
            } else {
               lines.addAll(this.textRenderer.wrapLines(line, this.helpWidth() - 24));
            }
         }

         this.helpCacheWidth = this.helpWidth();
         this.helpCache = lines;
         return lines;
      }
   }

   private int helpHeight() {
      int wanted = 22 + this.helpLines().size() * 11 + 30;
      return Math.min(wanted, this.height - 40);
   }

   private int helpLeft() {
      return (this.width - this.helpWidth()) / 2;
   }

   private int helpTop() {
      return (this.height - this.helpHeight()) / 2;
   }

   private void renderHelpDialog(DrawContext g, int mouseX, int mouseY) {
      List<OrderedText> lines = this.helpLines();
      int w = this.helpWidth();
      int h = this.helpHeight();
      int x = this.helpLeft();
      int y = this.helpTop();
      int bodyTop = y + 22;
      int bodyBottom = y + h - 30;
      int maxScroll = Math.max(0, lines.size() * 11 - (bodyBottom - bodyTop));
      this.helpScroll = MathHelper.clamp(this.helpScroll, 0, maxScroll);
      this.beginModal(g, x, y, w, h);
      g.drawCenteredTextWithShadow(this.textRenderer, this.state.screenText("help_title"), x + w / 2, y + 7, -2053377);
      g.fill(x + 1, bodyTop - 4, x + w - 1, bodyTop - 3, -6467875);
      g.enableScissor(x + 1, bodyTop, x + w - 1, bodyBottom);
      int lineY = bodyTop - this.helpScroll;

      for(OrderedText line : lines) {
         g.drawTextWithShadow(this.textRenderer, line, x + 12, lineY, -2962968);
         lineY += 11;
      }

      g.disableScissor();
      ClubScrollbar.draw(g, x + w - 6, bodyTop, bodyBottom, this.helpScroll, maxScroll);
      this.drawConfirmButton(g, x + (w - 90) / 2, y + h - 24, 90, "Got it", -6467875, this.helpCloseAt((double)mouseX, (double)mouseY));
      g.getMatrices().pop();
   }

   private boolean helpCloseAt(double mouseX, double mouseY) {
      return PreviewUi.inRect(mouseX, mouseY, this.helpLeft() + (this.helpWidth() - 90) / 2, this.helpTop() + this.helpHeight() - 24, 90, 20);
   }

   private void updateFloatingWidgets() {
      boolean map = this.tab == ClaimsScreen.Tab.MAP;
      int right = this.contentRightEdge();
      this.setFloating(this.zoomInButton, right - 20, this.mapTop() + 4, map);
      this.setFloating(this.zoomOutButton, right - 20, this.mapTop() + 24, map);
      this.setFloating(this.centerButton, right - 58, this.contentBottom() - 20, map);
      this.setFloating(this.helpButton, this.helpButtonLeft(), this.panelTop() + 2, this.helpButtonLeft() > this.tabsRight() + 4);
      ChunkSelection selection = this.mapView.selection();
      boolean confirming = map && selection.isActive();
      int barY = this.contentTop() + 2;
      if (this.cancelButton != null) {
         this.cancelButton.visible = confirming;
         this.cancelButton.active = confirming;
         this.cancelButton.setX(this.selectionButtonsLeft() + 28);
         this.cancelButton.setY(barY);
      }

      if (this.confirmButton != null) {
         this.confirmButton.visible = confirming;
         this.confirmButton.active = confirming && this.mapView.selectionIssue() == MapView.SelectionIssue.NONE;
         this.confirmButton.setVariant(selection.isResize() ? ThemedButton.Variant.BLUE : ThemedButton.Variant.GREEN);
         this.confirmButton.setX(this.selectionButtonsLeft());
         this.confirmButton.setY(barY);
      }

   }

   private int selectionButtonsLeft() {
      return this.contentRightEdge() - 4 - 24 - 4 - 24;
   }

   private void renderActionBar(DrawContext g) {
      int x0 = this.contentLeft();
      int x1 = this.contentRightEdge();
      int y0 = this.contentTop();
      ChunkSelection selection = this.mapView.selection();
      g.fill(x0, y0, x1, y0 + 22, selection.isActive() ? -14405538 : -15459782);
      g.drawBorder(x0, y0, x1 - x0, 22, -13747610);
      int textY = y0 + 7;
      int textLeft = x0 + 6;
      int textRight = (selection.isActive() ? this.selectionButtonsLeft() : x1) - 6;
      g.enableScissor(x0 + 2, y0, textRight + 4, y0 + 22);
      if (!selection.isActive()) {
         g.drawTextWithShadow(this.textRenderer, "Click a chunk or Shift+drag to start a claim", textLeft, textY, -7035976);
         g.disableScissor();
      } else {
         int cost = this.mapView.selectionCost();
         int var10000 = selection.widthChunks();
         String sizeCost = var10000 + "x" + selection.depthChunks() + " Chunks · " + (selection.isResize() && cost >= 0 ? "+" : "") + cost + " Blocks";
         String var14 = selection.isResize() ? "Resizing Claim: " : "New Claim: ";
         String label = var14 + sizeCost;
         switch (this.mapView.selectionIssue()) {
            case OVERLAP -> var14 = " — overlaps a claim";
            case TOO_EXPENSIVE -> var14 = " — not enough blocks";
            case TOO_LARGE -> var14 = " — too large";
            case TOO_FAR -> var14 = " — too far from you";
            default -> var14 = "";
         }

         String issueText = var14;
         int avail = textRight - textLeft;
         if (this.textRenderer.getWidth(label + issueText) > avail) {
            label = sizeCost;
         }

         if (!issueText.isEmpty() && this.textRenderer.getWidth(label + issueText) > avail) {
            g.drawTextWithShadow(this.textRenderer, issueText.substring(3), textLeft, textY, -2734768);
         } else {
            g.drawTextWithShadow(this.textRenderer, label, textLeft, textY, -1);
            if (!issueText.isEmpty()) {
               g.drawTextWithShadow(this.textRenderer, issueText, textLeft + this.textRenderer.getWidth(label), textY, -2734768);
            }
         }

         g.disableScissor();
      }
   }

   private void setFloating(ThemedButton button, int x, int y, boolean visible) {
      if (button != null) {
         button.visible = visible;
         button.active = visible;
         button.setX(x);
         button.setY(y);
      }
   }

   private void renderStatusStrip(DrawContext g, int mouseX, int mouseY) {
      int fy1 = this.panelBottom() - 1;
      int fy0 = fy1 - 14 - 2;
      g.fill(this.panelLeft() + 1, fy0, this.panelRight() - 1, fy1, -15459782);
      g.fill(this.panelLeft() + 1, fy0, this.panelRight() - 1, fy0 + 1, -13747610);
      int y = fy0 + 6;
      String left;
      if (this.mapView.contains((double)mouseX, (double)mouseY)) {
         int bx = this.mapView.cursorBlockX((double)mouseX);
         int bz = this.mapView.cursorBlockZ((double)mouseY);
         left = bx + ", " + bz + "  (chunk " + (bx >> 4) + ", " + (bz >> 4) + ")";
      } else {
         left = this.state.playerX + ", " + this.state.playerZ;
      }

      g.drawTextWithShadow(this.textRenderer, left, this.contentLeft() + 2, y, -7035976);
      g.drawCenteredTextWithShadow(this.textRenderer, "Drag or WASD pans · Scroll zooms", this.panelLeft() + this.panelW() / 2, y, -7035976);
      String zoom = "Zoom " + this.mapView.zoomLabel();
      g.drawTextWithShadow(this.textRenderer, zoom, this.panelRight() - 8 - this.textRenderer.getWidth(zoom), y, -7035976);
   }

   private void renderBanner(DrawContext g) {
      Text banner = this.state.banner();
      if (banner != null) {
         g.getMatrices().push();
         g.getMatrices().translate(0.0F, 0.0F, 300.0F);
         int w = Math.min(this.textRenderer.getWidth(banner) + 16, this.panelW() - 16);
         int x = this.panelLeft() + (this.panelW() - w) / 2;
         int y = this.contentBottom() - 40;
         g.fill(x, y, x + w, y + 16, this.state.bannerIsError() ? -533065192 : -535678948);
         g.drawBorder(x, y, w, 16, this.state.bannerIsError() ? -2734768 : -12474273);
         g.enableScissor(x + 2, y, x + w - 2, y + 16);
         g.drawTextWithShadow(this.textRenderer, banner, x + 8, y + 4, -1);
         g.disableScissor();
         g.getMatrices().pop();
      }
   }

   private void renderTooltips(DrawContext g, int mouseX, int mouseY) {
      if (this.detailVisible() && this.detailPanel.contains((double)mouseX, (double)mouseY)) {
         List<Text> tooltip = this.detailPanel.tooltip();
         if (tooltip != null) {
            g.drawTooltip(this.textRenderer, tooltip, Optional.empty(), mouseX, mouseY);
         }

      } else if (this.overBudgetText((double)mouseX, (double)mouseY)) {
         List<Text> lines = new ArrayList();
         lines.add(Text.literal("Claim Blocks").formatted(Formatting.GOLD));
         int remaining = this.state.effectiveRemaining();
         lines.add(remaining < 0 ? Text.literal("Over budget by ").formatted(Formatting.GRAY).append(Text.literal(ClaimsState.fmtBlocks(-remaining) + " blocks").formatted(Formatting.RED)) : Text.literal("Remaining: ").formatted(Formatting.GRAY).append(Text.literal(ClaimsState.fmtBlocks(remaining) + " blocks").formatted(Formatting.WHITE)));
         lines.add(Text.literal("Used: " + ClaimsState.fmtBlocks(Math.max(0, this.state.budget().getTotal() - remaining)) + " blocks").formatted(Formatting.GRAY));
         lines.add(Text.literal("Total capacity: " + ClaimsState.fmtBlocks(this.state.budget().getTotal()) + " blocks").formatted(Formatting.GRAY));
         if (remaining < 0) {
            lines.add(Text.literal("Delete a claim to free some up").formatted(Formatting.DARK_GRAY));
         }

         if (this.state.canBuyBlocks) {
            lines.add(this.state.hasScreenText("claim_purchase") ? this.state.screenText("claim_purchase") : Text.literal("Click to buy more claim blocks").formatted(Formatting.YELLOW));
         }

         if (this.state.hasScreenText("claim_earn_playtime")) lines.add(this.state.screenText("claim_earn_playtime"));
         if (this.state.hasScreenText("claim_earn_daily")) lines.add(this.state.screenText("claim_earn_daily"));

         g.drawTooltip(this.textRenderer, lines, Optional.empty(), mouseX, mouseY);
      } else {
         if (this.tab == ClaimsScreen.Tab.MAP && this.mapView.contains((double)mouseX, (double)mouseY)) {
            MapClaimEntry hovered = this.mapView.hitClaim((double)mouseX, (double)mouseY);
            if (hovered != null) {
               List<Text> lines = new ArrayList();
               lines.add(Text.literal(hovered.getName() != null ? hovered.getName() : "Claim"));
               lines.add(Text.literal("Owner: ").formatted(Formatting.GRAY).append(Text.literal(hovered.getOwnerName() != null ? hovered.getOwnerName() : "?").formatted(Formatting.WHITE)));
               int w = hovered.getMaxX() - hovered.getMinX() + 1;
               int d = hovered.getMaxZ() - hovered.getMinZ() + 1;
               lines.add(Text.literal(w + "x" + d + " (" + ClaimsState.fmtBlocks(w * d) + " blocks)").formatted(Formatting.GRAY));
               if ("OWN".equals(hovered.getRelation())) {
                  lines.add(Text.literal("Yours").formatted(Formatting.LIGHT_PURPLE));
               } else if ("TRUSTED".equals(hovered.getRelation())) {
                  lines.add(Text.literal("You are trusted here").formatted(Formatting.AQUA));
               } else if ("ADMIN".equals(hovered.getRelation())) {
                  lines.add(Text.literal("Admin claim").formatted(Formatting.RED));
               }

               g.drawTooltip(this.textRenderer, lines, Optional.empty(), mouseX, mouseY);
            }
         }

      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.confirmActive) {
         if (button == 0) {
            Boolean b = this.confirmButtonAt(mouseX, mouseY);
            if (Boolean.TRUE.equals(b)) {
               Runnable action = this.confirmAction;
               this.dismissConfirm();
               if (action != null) {
                  action.run();
               }

               PreviewUi.playClick();
            } else if (Boolean.FALSE.equals(b)) {
               this.dismissConfirm();
               PreviewUi.playClick();
            }
         }

         return true;
      } else if (this.helpActive) {
         boolean inside = PreviewUi.inRect(mouseX, mouseY, this.helpLeft(), this.helpTop(), this.helpWidth(), this.helpHeight());
         if (button == 0 && (!inside || this.helpCloseAt(mouseX, mouseY))) {
            this.helpActive = false;
            PreviewUi.playClick();
         }

         return true;
      } else {
         this.detailPanel.unfocusAll(mouseX, mouseY);
         if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
         } else if ((button == 0 || button == 1) && this.state.canBuyBlocks && this.overBudgetText(mouseX, mouseY)) {
            this.buyBlocks();
            return true;
         } else if (this.detailVisible() && this.detailPanel.contains(mouseX, mouseY)) {
            return this.detailPanel.mouseClicked(mouseX, mouseY, button);
         } else if (this.tab == ClaimsScreen.Tab.MAP && this.mapView.contains(mouseX, mouseY)) {
            this.mapView.mousePressed(mouseX, mouseY, button, hasShiftDown(), this.detailPanel.selectedClaimId());
            this.dismissOverlayPanel();
            return true;
         } else {
            return this.tab != ClaimsScreen.Tab.MAP ? this.listPanel.mouseClicked(mouseX, mouseY, button) : false;
         }
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (!this.confirmActive && !this.helpActive) {
         return this.tab == ClaimsScreen.Tab.MAP && this.mapView.mouseDragged(mouseX, mouseY, button) ? true : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      } else {
         return true;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (!this.confirmActive && !this.helpActive) {
         MapView.ClickResult result = this.mapView.mouseReleased(mouseX, mouseY, button);
         if (result != null && result.claim() != null) {
            this.selectClaim(result.claim().getClaimId());
            PreviewUi.playClick();
         } else if (result != null && result.startedSelection()) {
            this.dismissOverlayPanel();
         }

         return super.mouseReleased(mouseX, mouseY, button);
      } else {
         return true;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.confirmActive) {
         return true;
      } else if (this.helpActive) {
         this.helpScroll -= (int)(scrollY * (double)11.0F * (double)2.0F);
         return true;
      } else if (this.detailVisible() && this.detailPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
         return true;
      } else if (this.tab == ClaimsScreen.Tab.MAP && this.mapView.contains(mouseX, mouseY)) {
         return this.mapView.mouseScrolled(mouseX, mouseY, scrollY);
      } else {
         return this.tab != ClaimsScreen.Tab.MAP && this.listPanel.mouseScrolled(mouseX, mouseY, scrollY) ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   public boolean charTyped(char c, int modifiers) {
      return !this.confirmActive && !this.helpActive ? super.charTyped(c, modifiers) : true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.confirmActive) {
         if (keyCode != 257 && keyCode != 335) {
            if (keyCode == 256) {
               this.dismissConfirm();
               PreviewUi.playClick();
            }
         } else {
            Runnable action = this.confirmAction;
            this.dismissConfirm();
            if (action != null) {
               action.run();
            }

            PreviewUi.playClick();
         }

         return true;
      } else if (this.helpActive) {
         if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
            this.helpActive = false;
            PreviewUi.playClick();
         }

         return true;
      } else if (keyCode == 256 && this.mapView.selection().isActive()) {
         this.mapView.selection().clear();
         return true;
      } else if ((keyCode == 257 || keyCode == 335) && this.tab == ClaimsScreen.Tab.MAP && this.mapView.selection().isActive() && !this.detailPanel.anyEditBoxFocused()) {
         this.confirmSelection();
         return true;
      } else if (!this.detailPanel.anyEditBoxFocused()) {
         if (this.tab == ClaimsScreen.Tab.MAP) {
            switch (keyCode) {
               case 45:
               case 333:
                  this.mapView.zoomBy(-1, this.mapViewCenterX(), this.mapViewCenterY());
                  return true;
               case 61:
               case 334:
                  this.mapView.zoomBy(1, this.mapViewCenterX(), this.mapViewCenterY());
                  return true;
               case 65:
               case 263:
                  this.mapView.keyPan(-1, 0);
                  return true;
               case 68:
               case 262:
                  this.mapView.keyPan(1, 0);
                  return true;
               case 83:
               case 264:
                  this.mapView.keyPan(0, 1);
                  return true;
               case 87:
               case 265:
                  this.mapView.keyPan(0, -1);
                  return true;
            }
         }

         if (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
         } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      } else {
         return (keyCode == 257 || keyCode == 335) && this.detailPanel.submitFocusedEditBox() ? true : super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void close() {
      PreviewUi.playClose();
      super.close();
   }

   public void removed() {
      ClaimsNetworking.sendScreenClosed();
      this.tiles.close();
      super.removed();
   }

   public boolean shouldPause() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   private static enum Tab {
      MAP,
      CLAIMS,
      TRUSTED;

      // $FF: synthetic method
      private static Tab[] $values() {
         return new Tab[]{MAP, CLAIMS, TRUSTED};
      }
   }
}
