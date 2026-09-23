package com.cobbleclub.client.chestshop;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.server.chestshop.ChestShopPayloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class ChestShopScreen extends Screen {
    private static final int PANEL_W = 342;
    private static final int PANEL_H = 198;

    private String shopId;
    private ItemStack item;
    private long unitPrice;
    private int stock;
    private String owner;
    private String currencySymbol;
    private boolean manager;
    private String notice;
    private boolean error;

    private TextFieldWidget quantity;
    private ThemedButton buyButton;
    private ThemedButton maxButton;
    private boolean confirmRemove;

    public ChestShopScreen(
            ChestShopPayloads.ShopOpen payload
    ) {
        super(
                Text.literal(
                        "CobbleClub Player Shop"
                )
        );

        this.applyFields(payload);
        PreviewUi.playOpen();
    }

    public boolean matches(String id) {
        return this.shopId != null
                && this.shopId.equals(id);
    }

    public void apply(
            ChestShopPayloads.ShopOpen payload
    ) {
        this.applyFields(payload);

        if (this.quantity != null) {
            int q = Math.max(
                    1,
                    Math.min(
                            this.parseQuantity(),
                            Math.max(1, this.stock)
                    )
            );

            this.quantity.setText(
                    Integer.toString(q)
            );
        }

        this.updateButtons();
    }

    private void applyFields(
            ChestShopPayloads.ShopOpen payload
    ) {
        this.shopId = payload.shopId();

        this.item =
                payload.item() == null
                        ? ItemStack.EMPTY
                        : payload.item().copyWithCount(1);

        this.unitPrice = payload.unitPrice();
        this.stock = Math.max(0, payload.stock());
        this.owner = payload.ownerName();

        this.currencySymbol =
                payload.currencySymbol() == null
                        ? ""
                        : payload.currencySymbol();

        this.manager = payload.manager();
        this.notice = payload.notice();
        this.error = payload.error();
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

        if (!this.manager) {
            this.quantity = new TextFieldWidget(
                    this.textRenderer,
                    l + 188,
                    t + 111,
                    73,
                    18,
                    Text.literal("Quantity")
            );

            this.quantity.setMaxLength(6);
            this.quantity.setTextPredicate(
                    s -> s.matches("\\d*")
            );
            this.quantity.setText("1");

            this.addDrawableChild(this.quantity);

            this.addDrawableChild(
                    new ThemedButton(
                            l + 188,
                            t + 133,
                            34,
                            17,
                            Text.literal("-1"),
                            b -> this.changeQty(-1)
                    )
            );

            this.addDrawableChild(
                    new ThemedButton(
                            l + 226,
                            t + 133,
                            34,
                            17,
                            Text.literal("+1"),
                            b -> this.changeQty(1)
                    )
            );

            this.maxButton = this.addDrawableChild(
                    new ThemedButton(
                            l + 264,
                            t + 133,
                            46,
                            17,
                            Text.literal("Max"),
                            ThemedButton.Variant.BLUE,
                            b -> this.setMax()
                    )
            );

            this.buyButton = this.addDrawableChild(
                    new ThemedButton(
                            l + 188,
                            t + 155,
                            122,
                            20,
                            Text.literal("Buy"),
                            ThemedButton.Variant.GREEN,
                            b -> this.buy()
                    )
            );
        }

        if (this.manager) {
            this.addDrawableChild(
                    new ThemedButton(
                            l + 18,
                            t + 112,
                            76,
                            18,
                            Text.literal("Change Item"),
                            ThemedButton.Variant.BLUE,
                            b -> ChestShopNetworking.action(
                                    this.shopId,
                                    "edit_item",
                                    0
                            )
                    )
            );

            this.addDrawableChild(
                    new ThemedButton(
                            l + 99,
                            t + 112,
                            76,
                            18,
                            Text.literal("Change Price"),
                            ThemedButton.Variant.BLUE,
                            b -> ChestShopNetworking.action(
                                    this.shopId,
                                    "edit_price",
                                    0
                            )
                    )
            );

            this.addDrawableChild(
                    new ThemedButton(
                            l + 18,
                            t + 136,
                            157,
                            18,
                            Text.literal("Refresh Stock"),
                            b -> ChestShopNetworking.action(
                                    this.shopId,
                                    "refresh",
                                    0
                            )
                    )
            );

            this.addDrawableChild(
                    new ThemedButton(
                            l + 18,
                            t + 160,
                            157,
                            18,
                            Text.literal("Remove Shop"),
                            ThemedButton.Variant.RED,
                            b -> this.removeShop()
                    )
            );
        }

        this.addDrawableChild(
                new ThemedButton(
                        l + PANEL_W - 55,
                        t + PANEL_H - 19,
                        43,
                        14,
                        Text.literal("Close"),
                        b -> this.close()
                )
        );

        this.updateButtons();
    }

    private void updateButtons() {
        if (this.buyButton != null) {
            this.buyButton.active =
                    this.stock > 0
                            && this.parseQuantity() > 0;
        }

        if (this.maxButton != null) {
            this.maxButton.active =
                    this.stock > 0;
        }
    }

    private int parseQuantity() {
        try {
            return this.quantity == null
                    || this.quantity.getText().isBlank()
                    ? 0
                    : Integer.parseInt(
                            this.quantity.getText()
                    );
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void changeQty(int delta) {
        int q =
                Math.max(
                        1,
                        this.parseQuantity() + delta
                );

        if (this.stock > 0) {
            q = Math.min(
                    this.stock,
                    q
            );
        }

        this.quantity.setText(
                Integer.toString(q)
        );

        this.updateButtons();
    }

    private void setMax() {
        if (this.stock > 0) {
            this.quantity.setText(
                    Integer.toString(this.stock)
            );
        }

        this.updateButtons();
    }

    private void buy() {
        int q =
                this.parseQuantity();

        if (q <= 0 || this.stock <= 0) {
            return;
        }

        q = Math.min(q, this.stock);

        ChestShopNetworking.action(
                this.shopId,
                "buy",
                q
        );
    }

    private void removeShop() {
        if (!this.confirmRemove) {
            this.confirmRemove = true;
            this.notice =
                    "Click Remove Shop again to confirm.";
            this.error = true;
            return;
        }

        ChestShopNetworking.action(
                this.shopId,
                "remove",
                0
        );
    }

    @Override
    public void tick() {
        super.tick();
        this.updateButtons();
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
                44,
                663120L,
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
                l + 171,
                t + 9,
                -989697
        );

        g.fill(
                l + 18,
                t + 41,
                l + 75,
                t + 98,
                -14608594
        );

        g.drawBorder(
                l + 18,
                t + 41,
                57,
                57,
                -9153900
        );

        if (!this.item.isEmpty()) {
            PreviewUi.renderScaledItem(
                    g,
                    this.item,
                    l + 38,
                    t + 61,
                    1.25f
            );
        }

        g.drawTextWithShadow(
                this.textRenderer,
                this.textRenderer.trimToWidth(
                        this.item.getName().getString(),
                        235
                ),
                l + 88,
                t + 43,
                -1
        );

        g.drawTextWithShadow(
                this.textRenderer,
                "Seller: " + this.owner,
                l + 88,
                t + 58,
                -4344118
        );

        g.drawTextWithShadow(
                this.textRenderer,
                "Price each: "
                        + this.currencySymbol
                        + String.format(
                                "%,d",
                                this.unitPrice
                        ),
                l + 88,
                t + 72,
                -11670
        );

        g.drawTextWithShadow(
                this.textRenderer,
                "In stock: "
                        + String.format(
                                "%,d",
                                this.stock
                        ),
                l + 88,
                t + 86,
                this.stock > 0
                        ? -8527196
                        : -37251
        );

        if (this.manager) {
            g.drawTextWithShadow(
                    this.textRenderer,
                    "SHOP MANAGEMENT",
                    l + 18,
                    t + 103,
                    -4879617
            );

            g.drawTextWithShadow(
                    this.textRenderer,
                    "Normal right click chest = stock inventory",
                    l + 18,
                    t + 183,
                    -7568488
            );
        } else {
            g.drawTextWithShadow(
                    this.textRenderer,
                    "Quantity",
                    l + 188,
                    t + 101,
                    -4344118
            );

            int quantity =
                    this.parseQuantity();

            long total =
                    this.unitPrice > 0L
                            && quantity > 0
                            && this.unitPrice
                                    <= Long.MAX_VALUE
                                    / (long) quantity
                            ? this.unitPrice
                                    * (long) quantity
                            : 0L;

            g.drawTextWithShadow(
                    this.textRenderer,
                    "Total: "
                            + this.currencySymbol
                            + String.format(
                                    "%,d",
                                    total
                            ),
                    l + 188,
                    t + 179,
                    -4344118
            );
        }

        if (this.notice != null
                && !this.notice.isBlank()) {
            g.drawCenteredTextWithShadow(
                    this.textRenderer,
                    this.textRenderer.trimToWidth(
                            this.notice,
                            300
                    ),
                    l + 171,
                    t + 29,
                    this.error
                            ? -36224
                            : -8527196
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
    public boolean shouldPause() {
        return false;
    }
}
