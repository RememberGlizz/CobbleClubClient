package com.cobbleclub.client.chestshop;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class ChestShopPriceScreen extends Screen {
    private static final int PANEL_W = 304;
    private static final int PANEL_H = 158;

    private final ItemStack item;
    private final long minimum;
    private final long recommended;
    private final String currencySymbol;

    private TextFieldWidget priceField;
    private ThemedButton save;
    private boolean sent;

    public ChestShopPriceScreen(
            ItemStack item,
            long minimum,
            long recommended,
            String currencySymbol,
            String title
    ) {
        super(Text.literal(
                title == null || title.isBlank()
                        ? "Set Shop Price"
                        : title
        ));

        this.item = item == null
                ? ItemStack.EMPTY
                : item.copyWithCount(1);

        this.minimum = Math.max(1L, minimum);
        this.recommended = Math.max(this.minimum, recommended);
        this.currencySymbol = currencySymbol == null ? "" : currencySymbol;
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int l = this.left();
        int t = this.top();

        this.priceField = new TextFieldWidget(
                this.textRenderer,
                l + 92,
                t + 83,
                118,
                19,
                Text.literal("Price per item")
        );

        this.priceField.setMaxLength(10);
        this.priceField.setTextPredicate(s -> s.matches("\\d*"));
        this.priceField.setText(Long.toString(this.recommended));

        this.addDrawableChild(this.priceField);
        this.setInitialFocus(this.priceField);

        this.save = this.addDrawableChild(
                new ThemedButton(
                        l + 74,
                        t + 116,
                        72,
                        20,
                        Text.literal("Save Shop"),
                        ThemedButton.Variant.GREEN,
                        b -> this.submit()
                )
        );

        this.addDrawableChild(
                new ThemedButton(
                        l + 155,
                        t + 116,
                        72,
                        20,
                        Text.literal("Cancel"),
                        ThemedButton.Variant.RED,
                        b -> this.close()
                )
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (this.save != null) {
            this.save.active = this.validPrice() >= this.minimum;
        }
    }

    private long validPrice() {
        try {
            return this.priceField == null || this.priceField.getText().isBlank()
                    ? 0L
                    : Long.parseLong(this.priceField.getText());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private void submit() {
        long price = this.validPrice();

        if (price < this.minimum || this.sent) {
            return;
        }

        this.sent = true;
        this.save.active = false;

        PreviewUi.playClick();
        ChestShopNetworking.setPrice(price);
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
                38,
                88991L,
                0.48f
        );

        int l = this.left();
        int t = this.top();

        g.fillGradient(
                l,
                t,
                l + PANEL_W,
                t + PANEL_H,
                -266858202,
                -267515369
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
                t + 27,
                -14018744,
                -15200211
        );

        g.drawCenteredTextWithShadow(
                this.textRenderer,
                this.getTitle(),
                l + 152,
                t + 9,
                -989697
        );

        g.fill(
                l + 30,
                t + 44,
                l + 74,
                t + 88,
                -14674641
        );

        g.drawBorder(
                l + 30,
                t + 44,
                44,
                44,
                -9153900
        );

        if (!this.item.isEmpty()) {
            PreviewUi.renderScaledItem(
                    g,
                    this.item,
                    l + 44,
                    t + 58,
                    1.15f
            );
        }

        g.drawTextWithShadow(
                this.textRenderer,
                this.item.isEmpty()
                        ? "Item"
                        : this.textRenderer.trimToWidth(
                                this.item.getName().getString(),
                                185
                        ),
                l + 88,
                t + 47,
                -1
        );

        g.drawTextWithShadow(
                this.textRenderer,
                "Minimum: "
                        + this.currencySymbol
                        + String.format("%,d", this.minimum),
                l + 88,
                t + 62,
                -12438
        );

        g.drawTextWithShadow(
                this.textRenderer,
                "Suggested: "
                        + this.currencySymbol
                        + String.format("%,d", this.recommended),
                l + 88,
                t + 73,
                -7479883
        );

        g.drawTextWithShadow(
                this.textRenderer,
                "Price each",
                l + 30,
                t + 88,
                -4344118
        );

        long current = this.validPrice();

        if (current > 0L && current < this.minimum) {
            g.drawCenteredTextWithShadow(
                    this.textRenderer,
                    "Price cannot be below the /sell value.",
                    l + 152,
                    t + 105,
                    -37251
            );
        }

        super.render(
                g,
                mouseX,
                mouseY,
                delta
        );
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
