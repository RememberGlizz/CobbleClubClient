package com.cobbleclub.client.sell;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class SellScreen extends Screen {
    private static final int PANEL_W = 500;
    private static final int PANEL_H = 286;
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 4;
    private static final int PAGE_SIZE = GRID_COLS * GRID_ROWS;
    private static final int CELL_W = 78;
    private static final int CELL_H = 44;
    private static final int CELL_GAP = 2;

    private SellState state;
    private String selectedId;
    private String category = "all";
    private int page;
    private boolean pending;

    private TextFieldWidget amountField;
    private ThemedButton sellButton;
    private ThemedButton minusButton;
    private ThemedButton plusButton;
    private ThemedButton allButton;
    private ThemedButton prevButton;
    private ThemedButton nextButton;

    public SellScreen(SellState state) {
        super(Text.literal("CobbleClub Sell Shop"));
        this.state = state;
        PreviewUi.playOpen();
    }

    public void applyState(SellState state) {
        this.state = state;
        this.pending = false;
        if (this.selectedId != null && this.state.find(this.selectedId) == null) this.selectedId = null;
        this.clampPage();
    }

    private int left() { return (this.width - PANEL_W) / 2; }
    private int top() { return (this.height - PANEL_H) / 2; }
    private int gridX() { return this.left() + 9; }
    private int gridY() { return this.top() + 55; }
    private int detailX() { return this.left() + 334; }

    @Override
    protected void init() {
        int detailX = this.detailX();
        int top = this.top();

        this.amountField = new TextFieldWidget(this.textRenderer, detailX + 8, top + 139, 141, 20, Text.literal("Amount"));
        this.amountField.setMaxLength(5);
        this.amountField.setTextPredicate(value -> value.matches("\\d*"));
        this.amountField.setPlaceholder(Text.literal("Amount"));
        this.amountField.setText("1");
        this.addDrawableChild(this.amountField);

        this.minusButton = this.addDrawableChild(new ThemedButton(detailX + 8, top + 163, 42, 18, Text.literal("-1"), b -> changeAmount(-1)));
        this.plusButton = this.addDrawableChild(new ThemedButton(detailX + 54, top + 163, 42, 18, Text.literal("+1"), b -> changeAmount(1)));
        this.allButton = this.addDrawableChild(new ThemedButton(detailX + 100, top + 163, 49, 18, Text.literal("All"), ThemedButton.Variant.BLUE, b -> setAllAmount()));

        this.sellButton = this.addDrawableChild(new ThemedButton(detailX + 8, top + 187, 141, 22, Text.literal("Sell"), ThemedButton.Variant.GREEN, b -> sellSelected()));

        this.prevButton = this.addDrawableChild(new ThemedButton(this.left() + 9, top + 239, 54, 18, Text.literal("< Prev"), b -> {
            if (this.page > 0) this.page--;
        }));
        this.nextButton = this.addDrawableChild(new ThemedButton(this.left() + 270, top + 239, 54, 18, Text.literal("Next >"), b -> {
            if (this.page + 1 < pageCount()) this.page++;
        }));

        this.addDrawableChild(new ThemedButton(detailX + 8, top + 234, 68, 20, Text.literal("Refresh"), b -> {
            this.pending = true;
            SellNetworking.refresh();
        }));
        this.addDrawableChild(new ThemedButton(detailX + 81, top + 234, 68, 20, Text.literal("Close"), b -> this.close()));
    }

    private void changeAmount(int delta) {
        int current = Math.max(0, quantity());
        int next = Math.max(1, current + delta);
        SellState.ItemEntry selected = selected();
        if (selected != null && selected.count > 0) next = Math.min(next, selected.count);
        this.amountField.setText(Integer.toString(next));
    }

    private void setAllAmount() {
        SellState.ItemEntry selected = selected();
        if (selected != null && selected.count > 0) this.amountField.setText(Integer.toString(selected.count));
    }

    private void sellSelected() {
        SellState.ItemEntry selected = selected();
        int amount = quantity();
        if (selected == null || amount <= 0 || amount > selected.count || this.pending) return;
        this.pending = true;
        this.sellButton.active = false;
        SellNetworking.sell(selected.id, amount);
    }

    private int quantity() {
        if (this.amountField == null) return 0;
        try {
            String text = this.amountField.getText();
            return text == null || text.isBlank() ? 0 : Integer.parseInt(text);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private SellState.ItemEntry selected() {
        return this.state == null ? null : this.state.find(this.selectedId);
    }

    private List<SellState.ItemEntry> filtered() {
        if (this.state == null) return List.of();
        if ("all".equals(this.category)) return this.state.items;
        List<SellState.ItemEntry> out = new ArrayList<>();
        for (SellState.ItemEntry item : this.state.items) {
            if (this.category.equals(item.namespace())) out.add(item);
        }
        return out;
    }

    private int pageCount() {
        int size = filtered().size();
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void clampPage() {
        this.page = Math.max(0, Math.min(this.page, pageCount() - 1));
    }

    private List<SellState.ItemEntry> pageItems() {
        List<SellState.ItemEntry> filtered = filtered();
        int start = Math.min(filtered.size(), this.page * PAGE_SIZE);
        int end = Math.min(filtered.size(), start + PAGE_SIZE);
        return filtered.subList(start, end);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.clampPage();
        this.updateControls();

        this.renderBackground(g, mouseX, mouseY, partialTick);
        Starfield.draw(g, 0, 0, this.width, this.height, Util.getMeasuringTimeMs(), 52, 88421L, 0.55F);

        int left = this.left();
        int top = this.top();
        int detailX = this.detailX();

        g.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 1426063360);
        g.fillGradient(left, top, left + PANEL_W, top + PANEL_H, -199614136, -200601562);
        g.drawBorder(left - 1, top - 1, PANEL_W + 2, PANEL_H + 2, -16447985);
        g.drawBorder(left, top, PANEL_W, PANEL_H, -13747610);
        g.fillGradient(left, top, left + PANEL_W, top + 21, -14405538, -15459782);
        g.fill(left, top + 21, left + PANEL_W, top + 22, -6467875);
        g.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), left + PANEL_W / 2, top + 6, -2053377);

        g.drawTextWithShadow(this.textRenderer, "SELL CATALOG", left + 9, top + 29, -6467875);
        g.drawTextWithShadow(this.textRenderer, "Balance: " + this.state.balanceText, detailX + 8, top + 29, -2053377);

        this.renderTabs(g, mouseX, mouseY);
        this.renderCatalog(g, mouseX, mouseY);
        this.renderDetails(g);

        for (Element child : this.children()) {
            if (child instanceof Drawable drawable) drawable.render(g, mouseX, mouseY, partialTick);
        }

        SellState.ItemEntry hovered = hoveredEntry(mouseX, mouseY);
        if (hovered != null && !hovered.stack().isEmpty()) {
            Tooltips.render(g, this.textRenderer, hovered.stack(), mouseX, mouseY);
        }

        if (this.state.notice != null && !this.state.notice.isBlank()) {
            int color = this.state.error ? -2734768 : -12474273;
            String notice = this.textRenderer.trimToWidth(this.state.notice, PANEL_W - 18);
            g.drawCenteredTextWithShadow(this.textRenderer, notice, left + PANEL_W / 2, top + 266, color);
        } else {
            g.drawCenteredTextWithShadow(this.textRenderer, "Click an item, enter how many you want to sell, then confirm.", left + PANEL_W / 2, top + 266, -7035976);
        }
    }

    private void renderTabs(DrawContext g, int mouseX, int mouseY) {
        int x = this.left() + 89;
        int y = this.top() + 27;
        Tab[] tabs = {
                new Tab("all", "All", 50),
                new Tab("minecraft", "Minecraft", 76),
                new Tab("cobblemon", "Cobblemon", 76)
        };
        for (Tab tab : tabs) {
            boolean active = tab.id.equals(this.category);
            boolean hover = PreviewUi.inRect(mouseX, mouseY, x, y, tab.width, 17);
            g.fill(x, y, x + tab.width, y + 17, active ? -14410694 : (hover ? -14936272 : -15528414));
            if (active) g.fill(x, y + 16, x + tab.width, y + 17, -6467875);
            g.drawCenteredTextWithShadow(this.textRenderer, tab.label, x + tab.width / 2, y + 5, active ? -1 : -7035976);
            x += tab.width + 3;
        }
    }

    private void renderCatalog(DrawContext g, int mouseX, int mouseY) {
        int gridX = this.gridX();
        int gridY = this.gridY();
        int gridW = GRID_COLS * CELL_W + (GRID_COLS - 1) * CELL_GAP;
        int gridH = GRID_ROWS * CELL_H + (GRID_ROWS - 1) * CELL_GAP;

        g.fill(gridX - 2, gridY - 2, gridX + gridW + 2, gridY + gridH + 2, -15723477);
        g.drawBorder(gridX - 2, gridY - 2, gridW + 4, gridH + 4, -13747610);

        List<SellState.ItemEntry> items = pageItems();
        for (int i = 0; i < items.size(); i++) {
            SellState.ItemEntry entry = items.get(i);
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int x = gridX + col * (CELL_W + CELL_GAP);
            int y = gridY + row * (CELL_H + CELL_GAP);
            boolean selected = entry.id.equals(this.selectedId);
            boolean hover = PreviewUi.inRect(mouseX, mouseY, x, y, CELL_W, CELL_H);

            g.fillGradient(x, y, x + CELL_W, y + CELL_H, selected ? -15048389 : -15064506, -16315880);
            g.drawBorder(x, y, CELL_W, CELL_H, selected ? -12474273 : (hover ? -6467875 : -13747610));

            ItemStack stack = entry.stack();
            if (!stack.isEmpty()) PreviewUi.renderScaledItem(g, stack, x + 5, y + 7, 1.35F);

            String name = stack.isEmpty() ? entry.id : stack.getName().getString();
            g.drawText(this.textRenderer, this.textRenderer.trimToWidth(name, 49), x + 27, y + 6, -2962968, false);
            g.drawText(this.textRenderer, this.state.shortMoney(entry.price), x + 27, y + 18, -12474273, false);
            g.drawText(this.textRenderer, "Have: " + entry.count, x + 27, y + 30, entry.count > 0 ? -1 : -7035976, false);
        }

        int count = pageCount();
        g.drawCenteredTextWithShadow(this.textRenderer, "Page " + (this.page + 1) + " / " + count, this.left() + 166, this.top() + 244, -7035976);
    }

    private void renderDetails(DrawContext g) {
        int x = this.detailX();
        int y = this.top() + 48;
        int w = 157;
        int h = 181;
        g.fillGradient(x, y, x + w, y + h, -14998448, -16315880);
        g.drawBorder(x, y, w, h, -13747610);
        g.drawTextWithShadow(this.textRenderer, "SELECTED ITEM", x + 8, y + 7, -6467875);

        SellState.ItemEntry selected = selected();
        if (selected == null) {
            g.drawCenteredTextWithShadow(this.textRenderer, "Pick an item", x + w / 2, y + 55, -7035976);
            g.drawCenteredTextWithShadow(this.textRenderer, "from the catalog", x + w / 2, y + 68, -7035976);
            return;
        }

        ItemStack stack = selected.stack();
        if (!stack.isEmpty()) PreviewUi.renderScaledItem(g, stack, x + 13, y + 28, 2.0F);
        String name = stack.isEmpty() ? selected.id : stack.getName().getString();
        g.drawTextWithShadow(this.textRenderer, this.textRenderer.trimToWidth(name, 98), x + 48, y + 28, -1);
        g.drawTextWithShadow(this.textRenderer, this.state.money(selected.price) + " each", x + 48, y + 42, -12474273);
        g.drawTextWithShadow(this.textRenderer, "In inventory: " + selected.count, x + 48, y + 56, selected.count > 0 ? -2962968 : -2734768);

        g.drawTextWithShadow(this.textRenderer, "How many?", x + 8, y + 79, -2962968);
        int amount = quantity();
        long total = amount > 0 ? safeMultiply(selected.price, amount) : 0L;
        g.drawTextWithShadow(this.textRenderer, "Total: " + this.state.money(total), x + 8, y + 137, -2053377);
        if (amount > selected.count) {
            g.drawTextWithShadow(this.textRenderer, "Not enough in inventory", x + 8, y + 151, -2734768);
        } else if (selected.count == 0) {
            g.drawTextWithShadow(this.textRenderer, "You don't have any to sell", x + 8, y + 151, -7035976);
        } else if (this.pending) {
            g.drawTextWithShadow(this.textRenderer, "Processing...", x + 8, y + 151, -7035976);
        }
    }

    private void updateControls() {
        SellState.ItemEntry selected = selected();
        int amount = quantity();
        boolean hasSelection = selected != null;
        boolean hasAny = hasSelection && selected.count > 0;

        this.amountField.setEditable(hasSelection && !this.pending);
        this.minusButton.active = hasAny && !this.pending && amount > 1;
        this.plusButton.active = hasAny && !this.pending && amount < selected.count;
        this.allButton.active = hasAny && !this.pending;
        this.sellButton.active = hasAny && !this.pending && amount > 0 && amount <= selected.count;
        this.sellButton.setMessage(Text.literal(this.pending ? "Processing..." : (amount > 0 ? "Sell " + amount : "Sell")));

        this.prevButton.active = this.page > 0;
        this.nextButton.active = this.page + 1 < pageCount();
    }

    private SellState.ItemEntry hoveredEntry(double mouseX, double mouseY) {
        int gridX = this.gridX();
        int gridY = this.gridY();
        int gridW = GRID_COLS * CELL_W + (GRID_COLS - 1) * CELL_GAP;
        int gridH = GRID_ROWS * CELL_H + (GRID_ROWS - 1) * CELL_GAP;
        if (!PreviewUi.inRect(mouseX, mouseY, gridX, gridY, gridW, gridH)) return null;

        int relX = (int)(mouseX - gridX);
        int relY = (int)(mouseY - gridY);
        int col = relX / (CELL_W + CELL_GAP);
        int row = relY / (CELL_H + CELL_GAP);
        if (col < 0 || col >= GRID_COLS || row < 0 || row >= GRID_ROWS) return null;
        if (relX % (CELL_W + CELL_GAP) >= CELL_W || relY % (CELL_H + CELL_GAP) >= CELL_H) return null;
        int index = row * GRID_COLS + col;
        List<SellState.ItemEntry> items = pageItems();
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            String tab = tabAt(mouseX, mouseY);
            if (tab != null) {
                this.category = tab;
                this.page = 0;
                PreviewUi.playClick();
                return true;
            }

            SellState.ItemEntry clicked = hoveredEntry(mouseX, mouseY);
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
        int x = this.left() + 89;
        int y = this.top() + 27;
        Tab[] tabs = {
                new Tab("all", "All", 50),
                new Tab("minecraft", "Minecraft", 76),
                new Tab("cobblemon", "Cobblemon", 76)
        };
        for (Tab tab : tabs) {
            if (PreviewUi.inRect(mouseX, mouseY, x, y, tab.width, 17)) return tab.id;
            x += tab.width + 3;
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int gridW = GRID_COLS * CELL_W + (GRID_COLS - 1) * CELL_GAP;
        int gridH = GRID_ROWS * CELL_H + (GRID_ROWS - 1) * CELL_GAP;
        if (PreviewUi.inRect(mouseX, mouseY, this.gridX(), this.gridY(), gridW, gridH)) {
            if (verticalAmount < 0 && this.page + 1 < pageCount()) this.page++;
            if (verticalAmount > 0 && this.page > 0) this.page--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        PreviewUi.playClose();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static long safeMultiply(long value, int count) {
        if (value <= 0L || count <= 0) return 0L;
        if (value > Long.MAX_VALUE / count) return Long.MAX_VALUE;
        return value * count;
    }

    private record Tab(String id, String label, int width) {}
}
