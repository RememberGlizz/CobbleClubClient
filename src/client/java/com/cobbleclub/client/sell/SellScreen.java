/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_156
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_4068
 *  net.minecraft.class_437
 */
package com.cobbleclub.client.sell;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class SellScreen
        extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 220;
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 3;
    private static final int PAGE_SIZE = 12;
    private static final int CELL_W = 54;
    private static final int CELL_H = 38;
    private static final int CELL_GAP = 2;
    private SellState state;
    private String selectedId;
    private String category = "owned";
    private SortMode sortMode = SortMode.OWNED;
    private int scrollRow;
    private boolean pending;
    private TextFieldWidget searchField;
    private TextFieldWidget amountField;
    private ThemedButton sellButton;
    private ThemedButton oneButton;
    private ThemedButton sixteenButton;
    private ThemedButton thirtyTwoButton;
    private ThemedButton halfButton;
    private ThemedButton allButton;
    private ThemedButton sortButton;
    private SellState filteredState;
    private String filteredKey = "";
    private List<SellState.ItemEntry> filteredCache = List.of();

    public SellScreen(SellState state) {
        super(Text.literal("CobbleClub Sell Shop"));
        this.state = state;
        PreviewUi.playOpen();
    }

    public void applyState(SellState state) {
        this.state = state;
        this.pending = false;
        this.invalidateFilter();
        if (this.selectedId != null && this.state.find(this.selectedId) == null) {
            this.selectedId = null;
        }
        this.clampScroll();
    }

    private int left() {
        return (this.width - 360) / 2;
    }

    private int top() {
        return (this.height - 220) / 2;
    }

    private int gridX() {
        return this.left() + 9;
    }

    private int gridY() {
        return this.top() + 65;
    }

    private int detailX() {
        return this.left() + 238;
    }

    protected void init() {
        int detailX = this.detailX();
        int top = this.top();
        String previousSearch = this.searchField == null ? "" : this.searchField.getText();
        this.searchField = new TextFieldWidget(this.textRenderer, this.left() + 9, top + 44, 143, 17, Text.literal("Search items"));
        this.searchField.setMaxLength(64);
        this.searchField.setPlaceholder(Text.literal("Search items..."));
        this.searchField.setText(previousSearch);
        this.addDrawableChild(this.searchField);
        this.sortButton = this.addDrawableChild(new ThemedButton(this.left() + 156, top + 44, 73, 17, Text.literal(this.sortMode.label), ThemedButton.Variant.BLUE, b -> this.cycleSort()));
        this.amountField = new TextFieldWidget(this.textRenderer, detailX + 7, top + 119, 108, 17, Text.literal("Amount"));
        this.amountField.setMaxLength(5);
        this.amountField.setTextPredicate(value -> value.matches("\\d*"));
        this.amountField.setPlaceholder(Text.literal("Amount"));
        this.amountField.setText("1");
        this.addDrawableChild(this.amountField);
        this.oneButton = this.addDrawableChild(new ThemedButton(detailX + 7, top + 139, 20, 15, Text.literal("1"), b -> this.setAmount(1)));
        this.sixteenButton = this.addDrawableChild(new ThemedButton(detailX + 30, top + 139, 23, 15, Text.literal("16"), b -> this.setAmount(16)));
        this.thirtyTwoButton = this.addDrawableChild(new ThemedButton(detailX + 56, top + 139, 23, 15, Text.literal("32"), b -> this.setAmount(32)));
        this.halfButton = this.addDrawableChild(new ThemedButton(detailX + 82, top + 139, 33, 15, Text.literal("Half"), b -> this.setHalfAmount()));
        this.allButton = this.addDrawableChild(new ThemedButton(detailX + 7, top + 157, 108, 15, Text.literal("All"), ThemedButton.Variant.BLUE, b -> this.setAllAmount()));
        this.sellButton = this.addDrawableChild(new ThemedButton(detailX + 7, top + 184, 108, 15, Text.literal("Sell"), ThemedButton.Variant.GREEN, b -> this.sellSelected()));
        this.addDrawableChild(new ThemedButton(detailX + 7, top + 202, 52, 14, Text.literal("Refresh"), b -> {
            this.pending = true;
            SellNetworking.refresh();
        }));
        this.addDrawableChild(new ThemedButton(detailX + 63, top + 202, 52, 14, Text.literal("Close"), b -> this.close()));
        this.invalidateFilter();
    }

    private void setAllAmount() {
        SellState.ItemEntry selected = this.selected();
        if (selected != null && selected.count > 0) {
            this.amountField.setText(Integer.toString(selected.count));
        }
    }

    private void setHalfAmount() {
        SellState.ItemEntry selected = this.selected();
        if (selected != null && selected.count > 0) {
            this.setAmount(Math.max(1, selected.count / 2));
        }
    }

    private void setAmount(int amount) {
        SellState.ItemEntry selected = this.selected();
        if (selected != null && selected.count > 0) {
            this.amountField.setText(Integer.toString(Math.min(amount, selected.count)));
        }
    }

    private void cycleSort() {
        this.sortMode = this.sortMode.next();
        this.sortButton.setMessage(Text.literal(this.sortMode.label));
        this.scrollRow = 0;
        this.invalidateFilter();
    }

    private void sellSelected() {
        SellState.ItemEntry selected = this.selected();
        int amount = this.quantity();
        if (selected == null || amount <= 0 || amount > selected.count || this.pending) {
            return;
        }
        this.pending = true;
        this.sellButton.active = false;
        SellNetworking.sell(selected.id, amount);
    }

    private int quantity() {
        if (this.amountField == null) {
            return 0;
        }
        try {
            String text = this.amountField.getText();
            return text == null || text.isBlank() ? 0 : Integer.parseInt(text);
        }
        catch (Exception ignored) {
            return 0;
        }
    }

    private SellState.ItemEntry selected() {
        return this.state == null ? null : this.state.find(this.selectedId);
    }

    private String searchQuery() {
        if (this.searchField == null || this.searchField.getText() == null) {
            return "";
        }
        return this.searchField.getText().trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private void invalidateFilter() {
        this.filteredState = null;
        this.filteredKey = "";
        this.filteredCache = List.of();
    }

    private List<SellState.ItemEntry> filtered() {
        if (this.state == null) {
            return List.of();
        }
        String query = this.searchQuery();
        String key = this.category + "\u0000" + query;
        if (this.filteredState == this.state && key.equals(this.filteredKey)) {
            return this.filteredCache;
        }
        ArrayList<SellState.ItemEntry> out = new ArrayList<>();
        for (SellState.ItemEntry item2 : this.state.items) {
            if ("owned".equals(this.category) && item2.count <= 0 || "mods".equals(this.category) && ("minecraft".equals(item2.namespace()) || "cobblemon".equals(item2.namespace())) || !"owned".equals(this.category) && !"all".equals(this.category) && !"mods".equals(this.category) && !this.category.equals(item2.namespace()) || !query.isBlank() && !item2.searchText().contains(query)) continue;
            out.add(item2);
        }
        this.filteredState = this.state;
        this.filteredKey = key;
        Comparator<SellState.ItemEntry> comparator = switch (this.sortMode) {
            case NAME -> Comparator.comparing(
                    SellState.ItemEntry::displayName,
                    String.CASE_INSENSITIVE_ORDER
            );
            case PRICE_HIGH -> Comparator
                    .comparingLong((SellState.ItemEntry item) -> item.price)
                    .reversed()
                    .thenComparing(SellState.ItemEntry::displayName);
            case PRICE_LOW -> Comparator
                    .comparingLong((SellState.ItemEntry item) -> item.price)
                    .thenComparing(SellState.ItemEntry::displayName);
            case OWNED -> Comparator
                    .comparingInt((SellState.ItemEntry item) -> item.count)
                    .reversed()
                    .thenComparing(SellState.ItemEntry::displayName);
        };
        out.sort(comparator);
        this.filteredCache = List.copyOf(out);
        return this.filteredCache;
    }

    private int maxScrollRow() {
        int totalRows = (this.filtered().size() + 4 - 1) / 4;
        return Math.max(0, totalRows - 3);
    }

    private void clampScroll() {
        this.scrollRow = Math.max(0, Math.min(this.scrollRow, this.maxScrollRow()));
    }

    private List<SellState.ItemEntry> pageItems() {
        List<SellState.ItemEntry> filtered = this.filtered();
        int start = Math.min(filtered.size(), this.scrollRow * 4);
        int end = Math.min(filtered.size(), start + 12);
        return filtered.subList(start, end);
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.clampScroll();
        this.updateControls();
        this.renderBackground(g, mouseX, mouseY, partialTick);
        Starfield.draw(g, 0, 0, this.width, this.height, Util.getMeasuringTimeMs(), 52, 88421L, 0.55f);
        int left = this.left();
        int top = this.top();
        int detailX = this.detailX();
        g.fill(left - 3, top - 3, left + 360 + 3, top + 220 + 3, 0x55000000);
        g.fillGradient(left, top, left + 360, top + 220, -199614136, -200601562);
        g.drawBorder(left - 1, top - 1, 362, 222, -16447985);
        g.drawBorder(left, top, 360, 220, -13747610);
        g.fillGradient(left, top, left + 360, top + 21, -14405538, -15459782);
        g.fill(left, top + 21, left + 360, top + 22, -6467875);
        g.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), left + 180, top + 6, -2053377);
        String balanceLine = "Balance: " + this.state.balanceText;
        int balanceTextWidth = Math.max(1, this.textRenderer.getWidth(balanceLine));
        int balanceMaxWidth = 116;
        float balanceScale = Math.min(0.82f, (float)balanceMaxWidth / (float)balanceTextWidth);
        int balanceRight = left + 360 - 7;
        g.getMatrices().push();
        g.getMatrices().translate((float)balanceRight, (float)(top + 28), 0.0f);
        g.getMatrices().scale(balanceScale, balanceScale, 1.0f);
        g.drawTextWithShadow(this.textRenderer, balanceLine, -balanceTextWidth, 0, -2053377);
        g.getMatrices().pop();
        this.renderTabs(g, mouseX, mouseY);
        this.renderCatalog(g, mouseX, mouseY);
        this.renderDetails(g);
        for (Element child : this.children()) {
            if (!(child instanceof Drawable)) continue;
            Drawable drawable = (Drawable)child;
            drawable.render(g, mouseX, mouseY, partialTick);
        }
        SellState.ItemEntry hovered = this.hoveredEntry(mouseX, mouseY);
        if (hovered != null && !hovered.stack().isEmpty()) {
            Tooltips.render(g, this.textRenderer, hovered.stack(), mouseX, mouseY);
        }
        if (this.state.notice != null && !this.state.notice.isBlank()) {
            int color = this.state.error ? -2734768 : -12474273;
            String notice = this.textRenderer.trimToWidth(this.state.notice, 234);
            g.drawCenteredTextWithShadow(this.textRenderer, notice, left + 119, top + 207, color);
        } else {
            String footer = this.filtered().size() + " sellable item" + (this.filtered().size() == 1 ? "" : "s");
            g.drawCenteredTextWithShadow(this.textRenderer, footer + " \u2022 Scroll", left + 119, top + 207, -7035976);
        }
    }

    private List<Tab> tabs() {
        ArrayList<Tab> tabs = new ArrayList<>();
        tabs.add(new Tab("owned", "Inventory", 58));
        tabs.add(new Tab("all", "All", 28));
        if (this.state == null || this.state.hasNamespace("minecraft")) {
            tabs.add(new Tab("minecraft", "MC", 30));
        }
        if (this.state == null || this.state.hasNamespace("cobblemon")) {
            tabs.add(new Tab("cobblemon", "Cobblemon", 58));
        }
        tabs.add(new Tab("mods", "Mods", 34));
        return tabs;
    }

    private void renderTabs(DrawContext g, int mouseX, int mouseY) {
        int x = this.left() + 9;
        int y = this.top() + 24;
        for (Tab tab : this.tabs()) {
            boolean active = tab.id.equals(this.category);
            boolean hover = PreviewUi.inRect(mouseX, mouseY, x, y, tab.width, 17);
            g.fill(x, y, x + tab.width, y + 17, active ? -14410694 : (hover ? -14936272 : -15528414));
            if (active) {
                g.fill(x, y + 16, x + tab.width, y + 17, -6467875);
            }
            g.drawCenteredTextWithShadow(this.textRenderer, tab.label, x + tab.width / 2, y + 5, active ? -1 : -7035976);
            x += tab.width + 3;
        }
    }

    private void renderCatalog(DrawContext g, int mouseX, int mouseY) {
        int gridX = this.gridX();
        int gridY = this.gridY();
        int gridW = 222;
        int gridH = 118;
        g.fill(gridX - 2, gridY - 2, gridX + gridW + 2, gridY + gridH + 2, -15723477);
        g.drawBorder(gridX - 2, gridY - 2, gridW + 4, gridH + 4, -13747610);
        List<SellState.ItemEntry> items = this.pageItems();
        if (items.isEmpty()) {
            g.drawCenteredTextWithShadow(this.textRenderer, "owned".equals(this.category) ? "No sellable items in inventory" : "No items match your search", gridX + gridW / 2, gridY + gridH / 2 - 4, -7035976);
        }
        for (int i = 0; i < items.size(); ++i) {
            SellState.ItemEntry entry = items.get(i);
            int col = i % 4;
            int row = i / 4;
            int x = gridX + col * 56;
            int y = gridY + row * 40;
            boolean selected = entry.id.equals(this.selectedId);
            boolean hover = PreviewUi.inRect(mouseX, mouseY, x, y, 54, 38);
            g.fillGradient(x, y, x + 54, y + 38, selected ? -15048389 : -15064506, -16315880);
            g.drawBorder(x, y, 54, 38, selected ? -12474273 : (hover ? -6467875 : -13747610));
            ItemStack stack = entry.stack();
            if (!stack.isEmpty()) {
                PreviewUi.renderScaledItem(g, stack, x + 3, y + 7, 1.05f);
            }
            g.drawText(this.textRenderer, this.textRenderer.trimToWidth(entry.displayName(), 33), x + 21, y + 4, -2962968, false);
            g.drawText(this.textRenderer, this.state.shortMoney(entry.price), x + 21, y + 15, -12474273, false);
            g.drawText(this.textRenderer, "x" + entry.count, x + 21, y + 27, entry.count > 0 ? -1 : -7035976, false);
        }
        int max = this.maxScrollRow();
        if (max > 0) {
            int trackX = gridX + gridW + 4;
            g.fill(trackX, gridY, trackX + 2, gridY + gridH, -15723477);
            int thumbH = Math.max(12, gridH * 3 / (3 + max));
            int thumbY = gridY + (gridH - thumbH) * this.scrollRow / max;
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, -6467875);
        }
    }

    private void renderDetails(DrawContext g) {
        int x = this.detailX();
        int y = this.top() + 43;
        int w = 114;
        int h = 157;
        g.fillGradient(x, y, x + w, y + h, -14998448, -16315880);
        g.drawBorder(x, y, w, h, -13747610);
        g.drawTextWithShadow(this.textRenderer, "SELECTED ITEM", x + 8, y + 7, -6467875);
        SellState.ItemEntry selected = this.selected();
        if (selected == null) {
            g.drawCenteredTextWithShadow(this.textRenderer, "Pick an item", x + w / 2, y + 52, -7035976);
            g.drawCenteredTextWithShadow(this.textRenderer, "from the catalog", x + w / 2, y + 65, -7035976);
            return;
        }
        ItemStack stack = selected.stack();
        if (!stack.isEmpty()) {
            PreviewUi.renderScaledItem(g, stack, x + 8, y + 23, 1.35f);
        }
        g.drawTextWithShadow(this.textRenderer, this.textRenderer.trimToWidth(selected.displayName(), 72), x + 35, y + 25, -1);
        g.drawTextWithShadow(this.textRenderer, this.state.shortMoney(selected.price) + " each", x + 35, y + 38, -12474273);
        g.drawTextWithShadow(this.textRenderer, "Have: " + selected.count, x + 35, y + 51, selected.count > 0 ? -2962968 : -2734768);
        g.drawTextWithShadow(this.textRenderer, "Amount", x + 7, y + 65, -2962968);
        int amount = this.quantity();
        long total = amount > 0 ? SellScreen.safeMultiply(selected.price, amount) : 0L;
        String totalText = "Total: " + this.state.shortMoney(total);
        int totalColor = -2053377;
        if (amount > selected.count) {
            totalText = "Not enough items";
            totalColor = -2734768;
        } else if (selected.count == 0) {
            totalText = "None in inventory";
            totalColor = -7035976;
        } else if (this.pending) {
            totalText = "Processing...";
            totalColor = -7035976;
        }
        g.drawTextWithShadow(this.textRenderer, totalText, x + 7, y + 130, totalColor);
    }

    private void updateControls() {
        SellState.ItemEntry selected = this.selected();
        int amount = this.quantity();
        boolean hasSelection = selected != null;
        boolean hasAny = hasSelection && selected.count > 0;
        this.amountField.setEditable(hasSelection && !this.pending);
        this.oneButton.active = hasAny && !this.pending;
        this.sixteenButton.active = hasAny && !this.pending;
        this.thirtyTwoButton.active = hasAny && !this.pending;
        this.halfButton.active = hasAny && !this.pending;
        this.allButton.active = hasAny && !this.pending;
        this.sellButton.active = hasAny && !this.pending && amount > 0 && amount <= selected.count;
        this.sellButton.setMessage(Text.literal(this.pending ? "Processing..." : (amount > 0 ? "Sell " + amount : "Sell")));
    }

    private SellState.ItemEntry hoveredEntry(double mouseX, double mouseY) {
        int gridH;
        int gridW;
        int gridY;
        int gridX = this.gridX();
        if (!PreviewUi.inRect(mouseX, mouseY, gridX, gridY = this.gridY(), gridW = 222, gridH = 118)) {
            return null;
        }
        int relX = (int)(mouseX - (double)gridX);
        int relY = (int)(mouseY - (double)gridY);
        int col = relX / 56;
        int row = relY / 40;
        if (col < 0 || col >= 4 || row < 0 || row >= 3) {
            return null;
        }
        if (relX % 56 >= 54 || relY % 40 >= 38) {
            return null;
        }
        int index = row * 4 + col;
        List<SellState.ItemEntry> items = this.pageItems();
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            String tab = this.tabAt(mouseX, mouseY);
            if (tab != null) {
                this.category = tab;
                this.scrollRow = 0;
                this.invalidateFilter();
                PreviewUi.playClick();
                return true;
            }
            SellState.ItemEntry clicked = this.hoveredEntry(mouseX, mouseY);
            if (clicked != null) {
                this.selectedId = clicked.id;
                this.pending = false;
                this.amountField.setText("1");
                this.setFocused(this.amountField);
                PreviewUi.playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String tabAt(double mouseX, double mouseY) {
        int x = this.left() + 9;
        int y = this.top() + 24;
        for (Tab tab : this.tabs()) {
            if (PreviewUi.inRect(mouseX, mouseY, x, y, tab.width, 17)) {
                return tab.id;
            }
            x += tab.width + 3;
        }
        return null;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int gridW = 222;
        int gridH = 118;
        if (PreviewUi.inRect(mouseX, mouseY, this.gridX(), this.gridY(), gridW, gridH)) {
            if (verticalAmount < 0.0 && this.scrollRow < this.maxScrollRow()) {
                ++this.scrollRow;
            }
            if (verticalAmount > 0.0 && this.scrollRow > 0) {
                --this.scrollRow;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = (this.searchField != null && this.searchField.isFocused())
                || (this.amountField != null && this.amountField.isFocused());
        if (!typing && this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void close() {
        PreviewUi.playClose();
        super.close();
    }

    public boolean shouldPause() {
        return false;
    }

    private static long safeMultiply(long value, int count) {
        if (value <= 0L || count <= 0) {
            return 0L;
        }
        if (value > Long.MAX_VALUE / (long)count) {
            return Long.MAX_VALUE;
        }
        return value * (long)count;
    }

    @Environment(EnvType.CLIENT)
    private static enum SortMode {
        OWNED("Owned first"),
        NAME("Name A-Z"),
        PRICE_HIGH("Price high"),
        PRICE_LOW("Price low");

        private final String label;

        private SortMode(String label) {
            this.label = label;
        }

        private SortMode next() {
            SortMode[] values = SortMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    @Environment(EnvType.CLIENT)
    private record Tab(String id, String label, int width) {
    }
}