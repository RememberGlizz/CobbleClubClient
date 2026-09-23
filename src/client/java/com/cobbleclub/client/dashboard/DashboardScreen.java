/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_4068
 *  net.minecraft.class_437
 */
package com.cobbleclub.client.dashboard;

import com.cobbleclub.client.dashboard.DashboardNetworking;
import com.cobbleclub.client.dashboard.DashboardState;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;

@Environment(value=EnvType.CLIENT)
public final class DashboardScreen
extends Screen {
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 222;
    private DashboardState state;

    public DashboardScreen(DashboardState state) {
        super((Text)Text.literal((String)"CobbleClub"));
        this.state = state;
    }

    public void applyState(DashboardState state) {
        this.state = state;
    }

    private int left() {
        return (this.width - 400) / 2;
    }

    private int top() {
        return (this.height - 222) / 2;
    }

    protected void init() {
        int x = this.left() + 8;
        int y = this.top() + 117;
        int gap = 5;
        int width = (384 - gap * 2) / 3;
        this.action(x, y, width, "Claims", ThemedButton.Variant.BLUE, "open_claims");
        this.action(x + width + gap, y, width, "Wardrobe", ThemedButton.Variant.DEFAULT, "open_wardrobe");
        this.action(x + (width + gap) * 2, y, width, "Tags", ThemedButton.Variant.DEFAULT, "open_tags");
        this.action(x, y += 22, width, "Claim Daily", ThemedButton.Variant.GREEN, "daily");
        this.action(x + width + gap, y, width, "Buy Claim Blocks", ThemedButton.Variant.GREEN, "buy_claim_blocks");
        this.action(x + (width + gap) * 2, y, width, "Kits", ThemedButton.Variant.GREEN, "open_kits");
        this.action(x, y += 22, width, "Pok\u00e9mon Styles", ThemedButton.Variant.DEFAULT, "open_pokemon");
        this.action(x + width + gap, y, width, "Gear Preview", ThemedButton.Variant.DEFAULT, "open_gear");
        this.action(x + (width + gap) * 2, y, width, "Vote Crate", ThemedButton.Variant.DEFAULT, "open_crate_vote");
        this.action(x, y += 22, width, "Shiny Crate", ThemedButton.Variant.DEFAULT, "open_crate_shiny");
        this.action(x + width + gap, y, width, "Legendary Crate", ThemedButton.Variant.DEFAULT, "open_crate_legendary");
        this.action(x + (width + gap) * 2, y, width, "Refresh", ThemedButton.Variant.DEFAULT, "refresh");
        this.addDrawableChild(new ThemedButton(this.left() + 400 - 26, this.top() + 2, 20, 16, (Text)Text.literal((String)"\u00d7"), b -> this.close()));
    }

    private void action(int x, int y, int width, String label, ThemedButton.Variant variant, String action) {
        this.addDrawableChild(new ThemedButton(x, y, width, 20, (Text)Text.literal((String)label), variant, b -> DashboardNetworking.send(action)));
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        Starfield.draw(g, 0, 0, this.width, this.height, Util.getMeasuringTimeMs(), 44, 4242L, 0.55f);
        int x = this.left();
        int y = this.top();
        g.fill(x - 3, y - 3, x + 400 + 3, y + 222 + 3, 0x55000000);
        g.fillGradient(x, y, x + 400, y + 222, -196589496, -198036942);
        g.drawBorder(x - 1, y - 1, 402, 224, -15658735);
        g.drawBorder(x, y, 400, 222, -7434610);
        g.fillGradient(x, y, x + 400, y + 21, -10855846, -12632257);
        g.fill(x, y + 21, x + 400, y + 22, -4342339);
        g.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.literal((String)("CobbleClub \u00b7 " + this.state.playerName)), x + 200, y + 6, -723724);
        this.card(g, x + 8, y + 27, 188, 82, "CLAIM BLOCKS", true);
        this.card(g, x + 204, y + 27, 188, 82, "REWARDS & KEYS", false);
        for (Element child : this.children()) {
            if (!(child instanceof Drawable)) continue;
            Drawable renderable = (Drawable)child;
            renderable.render(g, mouseX, mouseY, partialTick);
        }
        if (!this.state.notice.isEmpty()) {
            int color = this.state.error ? -2734768 : -12474273;
            g.drawCenteredTextWithShadow(this.textRenderer, this.state.notice, x + 200, y + 210, color);
        } else {
            g.drawCenteredTextWithShadow(this.textRenderer, "Open this menu any time with /club", x + 200, y + 210, -5197648);
        }
    }

    private void card(DrawContext g, int x, int y, int w, int h, String title, boolean claims) {
        g.fillGradient(x, y, x + w, y + h, -13421773, -15198184);
        g.drawBorder(x, y, w, h, -7434610);
        g.drawTextWithShadow(this.textRenderer, title, x + 8, y + 7, -4342339);
        if (claims) {
            int availableColor = this.state.claimRemaining > 0 ? -12474273 : -2734768;
            g.drawTextWithShadow(this.textRenderer, "Available: " + DashboardState.count(this.state.claimRemaining), x + 8, y + 23, availableColor);
            g.drawTextWithShadow(this.textRenderer, "Used: " + DashboardState.count(this.state.claimUsed), x + 8, y + 35, -1710619);
            g.drawTextWithShadow(this.textRenderer, "Total: " + DashboardState.count(this.state.claimTotal), x + 8, y + 47, -1710619);
            long seconds = this.state.secondsToReward();
            String reward = seconds < 0L ? "Disabled" : "+" + DashboardState.count(this.state.playtimeRewardAmount) + " gems in " + DashboardScreen.time(seconds);
            g.drawTextWithShadow(this.textRenderer, "Playtime: " + reward, x + 8, y + 59, -5197648);
            String purchase = this.state.purchaseEnabled ? "+" + DashboardState.count(this.state.purchaseAmount) + " for " + this.state.purchasePriceText : "Purchases disabled";
            g.drawTextWithShadow(this.textRenderer, "Buy: " + purchase, x + 8, y + 70, -5197648);
        } else {
            g.drawTextWithShadow(this.textRenderer, "Balance: " + this.state.balanceText, x + 8, y + 23, -723724);
            g.drawTextWithShadow(this.textRenderer, "Gems: " + this.state.gemsText, x + 8, y + 35, -12474273);
            String daily = !this.state.dailyEnabled ? "Disabled" : (this.state.dailyAvailable ? "READY TO CLAIM" : "Claimed today");
            g.drawTextWithShadow(this.textRenderer, "Daily: " + daily, x + 8, y + 47, this.state.dailyAvailable ? -12474273 : -5197648);
            String dailyReward = "Reward: " + this.state.dailyGems + "g + " + this.state.dailyMoneyText;
            if (this.state.dailyVoteKeys > 0) {
                dailyReward = dailyReward + " + " + this.state.dailyVoteKeys + " key" + (this.state.dailyVoteKeys == 1 ? "" : "s");
            }
            g.drawTextWithShadow(this.textRenderer, dailyReward, x + 8, y + 59, -1710619);
            g.drawTextWithShadow(this.textRenderer, "Keys: V " + this.state.voteKeys + "  S " + this.state.shinyKeys + "  L " + this.state.legendaryKeys, x + 8, y + 70, -5197648);
        }
    }

    private static String time(long seconds) {
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        return minutes > 0L ? minutes + "m " + remainder + "s" : remainder + "s";
    }
}

