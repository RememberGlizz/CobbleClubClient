package com.cobbleclub.client.dashboard;

import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class DashboardScreen extends Screen {
   private static final int PANEL_W = 460;
   private static final int PANEL_H = 250;
   private DashboardState state;

   public DashboardScreen(DashboardState state) {
      super(Text.literal("CobbleClub"));
      this.state = state;
   }

   public void applyState(DashboardState state) { this.state = state; }

   private int left() { return (this.width - PANEL_W) / 2; }
   private int top() { return (this.height - PANEL_H) / 2; }

   protected void init() {
      int x = this.left() + 8;
      int y = this.top() + 132;
      int gap = 5;
      int width = (PANEL_W - 16 - gap * 2) / 3;
      this.action(x, y, width, "Claims", ThemedButton.Variant.BLUE, "open_claims");
      this.action(x + width + gap, y, width, "Wardrobe", ThemedButton.Variant.DEFAULT, "open_wardrobe");
      this.action(x + (width + gap) * 2, y, width, "Tags", ThemedButton.Variant.DEFAULT, "open_tags");

      y += 22;
      this.action(x, y, width, "Claim Daily", ThemedButton.Variant.GREEN, "daily");
      this.action(x + width + gap, y, width, "Buy Claim Blocks", ThemedButton.Variant.GREEN, "buy_claim_blocks");
      this.action(x + (width + gap) * 2, y, width, "Kits", ThemedButton.Variant.GREEN, "open_kits");

      y += 22;
      this.action(x, y, width, "Pokémon Styles", ThemedButton.Variant.DEFAULT, "open_pokemon");
      this.action(x + width + gap, y, width, "Gear Preview", ThemedButton.Variant.DEFAULT, "open_gear");
      this.action(x + (width + gap) * 2, y, width, "Vote Crate", ThemedButton.Variant.DEFAULT, "open_crate_vote");

      y += 22;
      this.action(x, y, width, "Shiny Crate", ThemedButton.Variant.DEFAULT, "open_crate_shiny");
      this.action(x + width + gap, y, width, "Legendary Crate", ThemedButton.Variant.DEFAULT, "open_crate_legendary");
      this.action(x + (width + gap) * 2, y, width, "Refresh", ThemedButton.Variant.DEFAULT, "refresh");
      this.addDrawableChild(new ThemedButton(this.left() + PANEL_W - 26, this.top() + 2, 20, 16, Text.literal("×"), b -> this.close()));
   }

   private void action(int x, int y, int width, String label, ThemedButton.Variant variant, String action) {
      this.addDrawableChild(new ThemedButton(x, y, width, 20, Text.literal(label), variant, b -> DashboardNetworking.send(action)));
   }

   public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g, mouseX, mouseY, partialTick);
      Starfield.draw(g, 0, 0, this.width, this.height, Util.getMeasuringTimeMs(), 44, 4242L, 0.55F);
      int x = this.left();
      int y = this.top();
      g.fill(x - 3, y - 3, x + PANEL_W + 3, y + PANEL_H + 3, 1426063360);
      g.fillGradient(x, y, x + PANEL_W, y + PANEL_H, -199614136, -200601562);
      g.drawBorder(x - 1, y - 1, PANEL_W + 2, PANEL_H + 2, -16447985);
      g.drawBorder(x, y, PANEL_W, PANEL_H, -13747610);
      g.fillGradient(x, y, x + PANEL_W, y + 21, -14405538, -15459782);
      g.fill(x, y + 21, x + PANEL_W, y + 22, -6467875);
      g.drawCenteredTextWithShadow(this.textRenderer, Text.literal("CobbleClub · " + this.state.playerName), x + PANEL_W / 2, y + 6, -2053377);

      this.card(g, x + 8, y + 29, 218, 95, "CLAIM BLOCKS", true);
      this.card(g, x + 234, y + 29, 218, 95, "REWARDS & KEYS", false);

      for (Element child : this.children()) {
         if (child instanceof Drawable renderable) renderable.render(g, mouseX, mouseY, partialTick);
      }

      if (!this.state.notice.isEmpty()) {
         int color = this.state.error ? -2734768 : -12474273;
         g.drawCenteredTextWithShadow(this.textRenderer, this.state.notice, x + PANEL_W / 2, y + 226, color);
      } else {
         g.drawCenteredTextWithShadow(this.textRenderer, "Open this menu any time with /club", x + PANEL_W / 2, y + 226, -7035976);
      }
   }

   private void card(DrawContext g, int x, int y, int w, int h, String title, boolean claims) {
      g.fillGradient(x, y, x + w, y + h, -14998448, -16315880);
      g.drawBorder(x, y, w, h, -13747610);
      g.drawTextWithShadow(this.textRenderer, title, x + 8, y + 7, -6467875);
      if (claims) {
         int availableColor = this.state.claimRemaining > 0 ? -12474273 : -2734768;
         g.drawTextWithShadow(this.textRenderer, "Available: " + DashboardState.count(this.state.claimRemaining), x + 8, y + 23, availableColor);
         g.drawTextWithShadow(this.textRenderer, "Used: " + DashboardState.count(this.state.claimUsed), x + 8, y + 36, -2962968);
         g.drawTextWithShadow(this.textRenderer, "Total capacity: " + DashboardState.count(this.state.claimTotal), x + 8, y + 49, -2962968);
         long seconds = this.state.secondsToReward();
         String reward = seconds < 0 ? "Playtime earning disabled" : "+" + DashboardState.count(this.state.playtimeRewardAmount) + " gems in " + time(seconds);
         g.drawTextWithShadow(this.textRenderer, "Online reward: " + reward, x + 8, y + 64, -7035976);
         String purchase = this.state.purchaseEnabled
                 ? "+" + DashboardState.count(this.state.purchaseAmount) + " for " + this.state.purchasePriceText
                 : "Purchases disabled";
         g.drawTextWithShadow(this.textRenderer, "Purchase: " + purchase, x + 8, y + 77, -7035976);
      } else {
         g.drawTextWithShadow(this.textRenderer, "CobbleClub balance: " + this.state.balanceText, x + 8, y + 23, -2053377);
         g.drawTextWithShadow(this.textRenderer, "Gems: " + this.state.gemsText, x + 8, y + 36, -12474273);
         String daily = !this.state.dailyEnabled ? "Disabled" : (this.state.dailyAvailable ? "READY TO CLAIM" : "Claimed today");
         g.drawTextWithShadow(this.textRenderer, "Daily: " + daily, x + 8, y + 51, this.state.dailyAvailable ? -12474273 : -7035976);
         g.drawTextWithShadow(this.textRenderer, "Reward: " + this.state.dailyGems + " gems + " + this.state.dailyMoneyText
                 + " + " + this.state.dailyVoteKeys + " vote key", x + 8, y + 64, -2962968);
         g.drawTextWithShadow(this.textRenderer, "Keys — Vote: " + this.state.voteKeys + "  Shiny: " + this.state.shinyKeys + "  Legendary: " + this.state.legendaryKeys, x + 8, y + 79, -7035976);
      }
   }

   private static String time(long seconds) {
      long minutes = seconds / 60L;
      long remainder = seconds % 60L;
      return minutes > 0 ? minutes + "m " + remainder + "s" : remainder + "s";
   }
}
