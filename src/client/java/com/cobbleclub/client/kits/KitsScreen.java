package com.cobbleclub.client.kits;

import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class KitsScreen extends Screen {
    private static final int PANEL_W = 416;
    private static final int PANEL_H = 224;

    private static final String[] IDS = {
            "newb",
            "ace",
            "champion",
            "master",
            "legend"
    };

    private static final Map<String, String[]> CONTENTS = Map.of(
            "newb",
            new String[]{
                    "1× CobbleClub Claiming Tool",
                    "1× Diamond Pickaxe",
                    "1× Fishing Rod",
                    "1× Red Pokédex",
                    "1× RCT Trainer Card",
                    "20× Poké Balls",
                    "10× Great Balls",
                    "6× Ultra Balls",
                    "32× Steak",
                    "1× PC",
                    "1× Healing Machine",
                    "RTP Bonus: −10s @ 50% Dex · −20s @ 100%"
            },
            "ace",
            new String[]{
                    "Enchanted Spark Armor (Gold)",
                    "2× Shiny Crate Keys",
                    "6× Rare Candy",
                    "6× Ultra Balls",
                    "12× Quick Balls",
                    "50× XP Bottles",
                    "RTP: −30s rank bonus",
                    "Dex: −10s @ 50% · −20s @ 100%"
            },
            "champion",
            new String[]{
                    "Enchanted Spectral (Iron)",
                    "1× Legendary Crate Key",
                    "1× Shiny Crate Key",
                    "8× Rare Candy",
                    "8× Ultra Balls",
                    "16× Quick Balls",
                    "64× XP Bottles",
                    "RTP: −60s rank bonus",
                    "Dex: −10s @ 50% · −20s @ 100%"
            },
            "master",
            new String[]{
                    "Enchanted Aura (Diamond)",
                    "2× Beast Balls",
                    "1× Legendary Crate Key",
                    "1× Shiny Crate Key",
                    "1× Vote Crate Key",
                    "16× Rare Candy",
                    "32× Quick Balls",
                    "96× XP Bottles",
                    "RTP: −90s rank bonus",
                    "Dex: −10s @ 50% · −20s @ 100%"
            },
            "legend",
            new String[]{
                    "Enchanted Fairy (Netherite)",
                    "1× Master Ball",
                    "1× Shiny Crate Key",
                    "3× Vote Crate Keys",
                    "46× Rare Candy",
                    "46× Quick Balls",
                    "178× XP Bottles",
                    "RTP: −120s rank bonus",
                    "Dex: −10s @ 50% · −20s @ 100%"
            }
    );

    private KitsState state;
    private String selected = "newb";
    private ThemedButton claimButton;

    public KitsScreen(KitsState state) {
        super(Text.literal("CobbleClub Kits"));
        this.state = state;
    }

    public void applyState(KitsState state) {
        this.state = state;
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int x = this.left() + 8;
        int tabY = this.top() + 45;
        int gap = 4;
        int tabW = (400 - gap * 4) / 5;

        for (int i = 0; i < IDS.length; ++i) {
            String id = IDS[i];
            String label =
                    Character.toUpperCase(id.charAt(0))
                            + id.substring(1);

            this.addDrawableChild(
                    new ThemedButton(
                            x + i * (tabW + gap),
                            tabY,
                            tabW,
                            20,
                            Text.literal(label),
                            b -> this.selected = id
                    )
            );
        }

        int y = this.top() + 178;
        int buttonGap = 4;
        int width = (400 - buttonGap * 3) / 4;

        this.claimButton = new ThemedButton(
                x,
                y,
                width,
                22,
                Text.literal("Claim Kit"),
                ThemedButton.Variant.GREEN,
                b -> KitsNetworking.send(
                        "claim:" + this.selected
                )
        );

        this.addDrawableChild(this.claimButton);

        this.addDrawableChild(
                new ThemedButton(
                        x + width + buttonGap,
                        y,
                        width,
                        22,
                        Text.literal("−1h Cooldown"),
                        ThemedButton.Variant.BLUE,
                        b -> KitsNetworking.send(
                                "reduce:" + this.selected
                        )
                )
        );

        this.addDrawableChild(
                new ThemedButton(
                        x + (width + buttonGap) * 2,
                        y,
                        width,
                        22,
                        Text.literal("Refresh Ranks"),
                        b -> KitsNetworking.send("refresh")
                )
        );

        this.addDrawableChild(
                new ThemedButton(
                        x + (width + buttonGap) * 3,
                        y,
                        width,
                        22,
                        Text.literal("Close"),
                        b -> this.close()
                )
        );
    }

    @Override
    public void render(
            DrawContext g,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(
                g,
                mouseX,
                mouseY,
                partialTick
        );

        Starfield.draw(
                g,
                0,
                0,
                this.width,
                this.height,
                Util.getMeasuringTimeMs(),
                44,
                8787L,
                0.55f
        );

        int x = this.left();
        int y = this.top();

        g.fill(
                x - 3,
                y - 3,
                x + PANEL_W + 3,
                y + PANEL_H + 3,
                0x55000000
        );

        g.fillGradient(
                x,
                y,
                x + PANEL_W,
                y + PANEL_H,
                -196589496,
                -198036942
        );

        g.drawBorder(
                x - 1,
                y - 1,
                PANEL_W + 2,
                PANEL_H + 2,
                -15658735
        );

        g.drawBorder(
                x,
                y,
                PANEL_W,
                PANEL_H,
                -7434610
        );

        g.fillGradient(
                x,
                y,
                x + PANEL_W,
                y + 22,
                -10855846,
                -12632257
        );

        g.fill(
                x,
                y + 21,
                x + PANEL_W,
                y + 22,
                -4342339
        );

        g.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("CobbleClub · Kits"),
                x + 208,
                y + 7,
                -723724
        );

        KitsState.KitEntry kit =
                this.state.kit(this.selected);

        long remaining =
                kit.secondsRemaining();

        String status =
                !kit.unlocked
                        ? "LOCKED"
                        : remaining <= 0L
                                ? "READY — FREE TO CLAIM"
                                : "COOLDOWN — "
                                        + KitsScreen.time(remaining);

        int statusColor =
                !kit.unlocked
                        ? -2734768
                        : remaining <= 0L
                                ? -12474273
                                : -723724;

        g.drawCenteredTextWithShadow(
                this.textRenderer,
                kit.displayName
                        + " Kit · "
                        + status,
                x + 208,
                y + 29,
                statusColor
        );

        String access =
                "newb".equals(this.selected)
                        ? "Public · first join auto-delivery · 12h cooldown"
                        : "Premium rank kit · 18h cooldown";

        float accessScale = 0.70f;

        g.getMatrices().push();
        g.getMatrices().scale(
                accessScale,
                accessScale,
                1.0f
        );

        g.drawCenteredTextWithShadow(
                this.textRenderer,
                access,
                (int) ((x + 208) / accessScale),
                (int) ((y + 38) / accessScale),
                -5197648
        );

        g.getMatrices().pop();

        if (this.claimButton != null) {
            this.claimButton.active =
                    kit.unlocked
                            && remaining <= 0L;
        }

        int boxY = y + 70;

        g.fillGradient(
                x + 8,
                boxY,
                x + PANEL_W - 8,
                y + 173,
                -13421773,
                -15198184
        );

        g.drawBorder(
                x + 8,
                boxY,
                400,
                103,
                -7434610
        );

        g.drawTextWithShadow(
                this.textRenderer,
                (kit.displayName + " KIT CONTENTS")
                        .toUpperCase(),
                x + 19,
                boxY + 9,
                -4342339
        );

        g.fill(
                x + 208,
                boxY + 24,
                x + 209,
                y + 166,
                -7434610
        );

        String[] items =
                CONTENTS.getOrDefault(
                        this.selected,
                        new String[0]
                );

        int split =
                (items.length + 1) / 2;

        for (int i = 0; i < items.length; ++i) {
            int column =
                    i < split ? 0 : 1;

            int row =
                    column == 0
                            ? i
                            : i - split;

            String line = "• " + items[i];
            int lineX = x + 16 + column * 200;
            int lineY = boxY + 27 + row * ("newb".equals(this.selected) ? 13 : 15);

            if (items[i].startsWith("RTP") || items[i].startsWith("Dex")) {
                float lineScale = 0.78f;
                g.getMatrices().push();
                g.getMatrices().scale(lineScale, lineScale, 1.0f);
                g.drawTextWithShadow(
                        this.textRenderer,
                        line,
                        (int)(lineX / lineScale),
                        (int)(lineY / lineScale),
                        -1710619
                );
                g.getMatrices().pop();
            } else {
                g.drawTextWithShadow(
                        this.textRenderer,
                        line,
                        lineX,
                        lineY,
                        -1710619
                );
            }
        }

        for (Element child : this.children()) {
            if (child instanceof Drawable renderable) {
                renderable.render(
                        g,
                        mouseX,
                        mouseY,
                        partialTick
                );
            }
        }

        if (!this.state.notice.isEmpty()) {
            g.drawCenteredTextWithShadow(
                    this.textRenderer,
                    this.state.notice,
                    x + 208,
                    y + 211,
                    this.state.error
                            ? -2734768
                            : -12474273
            );
        } else {
            String paid = KitsScreen.reductionUsage(
                    kit.reductionPurchasedSeconds,
                    kit.reductionMaxSeconds
            );

            float footerScale = 0.84f;

            String footer =
                    "Cooldown buy: "
                            + this.state.reductionPriceText
                            + " per hour · used "
                            + paid
                            + " · balance "
                            + this.state.balanceText;

            g.getMatrices().push();
            g.getMatrices().scale(
                    footerScale,
                    footerScale,
                    1.0f
            );

            g.drawCenteredTextWithShadow(
                    this.textRenderer,
                    footer,
                    (int) ((x + 208) / footerScale),
                    (int) ((y + 211) / footerScale),
                    -5197648
            );

            g.getMatrices().pop();
        }
    }

    private static String time(long seconds) {
        long hours =
                seconds / 3600L;

        long minutes =
                seconds % 3600L / 60L;

        long secs =
                seconds % 60L;

        return hours > 0L
                ? hours + "h " + minutes + "m"
                : minutes > 0L
                        ? minutes + "m " + secs + "s"
                        : secs + "s";
    }

    private static String shortTime(long seconds) {
        if (seconds % 3600L == 0L) {
            return seconds / 3600L + "h";
        }

        return KitsScreen.time(seconds);
    }

    private static String reductionUsage(long usedSeconds, long maxSeconds) {
        if (usedSeconds % 3600L == 0L && maxSeconds % 3600L == 0L) {
            return usedSeconds / 3600L + "/" + maxSeconds / 3600L + "h";
        }
        return KitsScreen.shortTime(usedSeconds) + "/" + KitsScreen.shortTime(maxSeconds);
    }
}
