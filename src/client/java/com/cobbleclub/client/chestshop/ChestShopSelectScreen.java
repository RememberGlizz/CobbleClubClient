package com.cobbleclub.client.chestshop;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class ChestShopSelectScreen extends Screen {
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 208;
    private static final int COLS = 9;
    private static final int SLOT = 31;

    private static final int[] ORDER = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            0, 1, 2, 3, 4, 5, 6, 7, 8
    };

    private boolean sent;

    public ChestShopSelectScreen(String title) {
        super(
                Text.literal(
                        title == null || title.isBlank()
                                ? "Choose Shop Item"
                                : title
                )
        );

        PreviewUi.playOpen();
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    private int gridX() {
        return this.left() + 25;
    }

    private int gridY() {
        return this.top() + 48;
    }

    @Override
    protected void init() {
        this.addDrawableChild(
                new ThemedButton(
                        this.left() + PANEL_W - 66,
                        this.top() + PANEL_H - 24,
                        54,
                        16,
                        Text.literal("Cancel"),
                        ThemedButton.Variant.RED,
                        b -> this.close()
                )
        );
    }

    @Override
    public void renderBackground(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
    }

    @Override
    public void render(
            DrawContext g,
            int mouseX,
            int mouseY,
            float delta
    ) {
        g.fill(
                0,
                0,
                this.width,
                this.height,
                0x52000000
        );

        Starfield.draw(
                g,
                0,
                0,
                this.width,
                this.height,
                Util.getMeasuringTimeMs(),
                45,
                771923L,
                0.5f
        );

        int l = this.left();
        int t = this.top();

        g.fill(
                l - 3,
                t - 3,
                l + PANEL_W + 3,
                t + PANEL_H + 3,
                0x55000000
        );

        g.fillGradient(
                l,
                t,
                l + PANEL_W,
                t + PANEL_H,
                -266858202,
                -267384038
        );

        g.drawBorder(
                l,
                t,
                PANEL_W,
                PANEL_H,
                -6593537
        );

        g.fillGradient(
                l,
                t,
                l + PANEL_W,
                t + 28,
                -14018744,
                -15200211
        );

        g.drawCenteredTextWithShadow(
                this.textRenderer,
                this.getTitle(),
                l + 165,
                t + 9,
                -989697
        );

        g.drawCenteredTextWithShadow(
                this.textRenderer,
                "Click the exact item or block you want this shop to sell.",
                l + 165,
                t + 32,
                -4344118
        );

        for (int i = 0; i < ORDER.length; ++i) {
            int col = i % COLS;
            int row = i / COLS;

            int x = this.gridX() + col * SLOT;
            int y = this.gridY() + row * SLOT;

            boolean hover =
                    PreviewUi.inRect(
                            mouseX,
                            mouseY,
                            x,
                            y,
                            27,
                            27
                    );

            g.fill(
                    x,
                    y,
                    x + 27,
                    y + 27,
                    hover
                            ? -13360812
                            : -14608594
            );

            g.drawBorder(
                    x,
                    y,
                    27,
                    27,
                    hover
                            ? -3695617
                            : -10467461
            );

            ItemStack stack =
                    this.stack(ORDER[i]);

            if (stack.isEmpty()) {
                continue;
            }

            PreviewUi.renderScaledItem(
                    g,
                    stack,
                    x + 5,
                    y + 5,
                    1.0f
            );

            if (stack.getCount() > 1) {
                g.drawTextWithShadow(
                        this.textRenderer,
                        Integer.toString(stack.getCount()),
                        x + 16,
                        y + 17,
                        -1
                );
            }
        }

        g.drawTextWithShadow(
                this.textRenderer,
                "Nothing is removed from your inventory.",
                l + 13,
                t + PANEL_H - 20,
                -8527963
        );

        super.render(
                g,
                mouseX,
                mouseY,
                delta
        );

        ItemStack hovered =
                this.hoveredStack(
                        mouseX,
                        mouseY
                );

        if (!hovered.isEmpty()) {
            Tooltips.render(
                    g,
                    this.textRenderer,
                    hovered,
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0 && !this.sent) {
            for (int i = 0; i < ORDER.length; ++i) {
                int x =
                        this.gridX()
                                + i % COLS * SLOT;

                int y =
                        this.gridY()
                                + i / COLS * SLOT;

                if (!PreviewUi.inRect(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        27,
                        27
                )) {
                    continue;
                }

                ItemStack stack =
                        this.stack(ORDER[i]);

                if (stack.isEmpty()) {
                    continue;
                }

                this.sent = true;

                PreviewUi.playClick();
                ChestShopNetworking.selectItem(ORDER[i]);

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private ItemStack hoveredStack(
            double mx,
            double my
    ) {
        for (int i = 0; i < ORDER.length; ++i) {
            int x =
                    this.gridX()
                            + i % COLS * SLOT;

            int y =
                    this.gridY()
                            + i / COLS * SLOT;

            if (PreviewUi.inRect(
                    mx,
                    my,
                    x,
                    y,
                    27,
                    27
            )) {
                return this.stack(ORDER[i]);
            }
        }

        return ItemStack.EMPTY;
    }

    private ItemStack stack(int slot) {
        if (this.client == null
                || this.client.player == null
                || slot < 0
                || slot >= this.client.player.getInventory().size()) {
            return ItemStack.EMPTY;
        }

        return this.client.player
                .getInventory()
                .getStack(slot);
    }

    @Override
    public void close() {
        if (!this.sent) {
            ChestShopNetworking.cancelSetup();
        }

        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
