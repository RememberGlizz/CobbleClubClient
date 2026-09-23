/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.tags.protocol.TagsEntry
 *  com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg
 *  com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_124
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_3532
 *  net.minecraft.class_364
 *  net.minecraft.class_4068
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_746
 */
package com.cobbleclub.client.tags;

import com.cobbleclub.client.tags.TagsNetworking;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import com.cobbleclub.clubhouse.tags.protocol.TagsEntry;
import com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.entity.player.PlayerEntity;

@Environment(value=EnvType.CLIENT)
public class TagsScreen
extends Screen {
    private static final int PANEL_W = 440;
    private static final int PANEL_H = 240;
    private static final int ROW_H = 18;
    private static final int GRID_COLS = 3;
    private static final int CHIP_GAP = 2;
    private static final int SEARCH_W = 120;
    private static final int PANEL_TOP = -196589496;
    private static final int PANEL_BOTTOM = -198036942;
    private static final int PANEL_BORDER_OUT = -15658735;
    private static final int PANEL_BORDER_IN = -7434610;
    private static final int HEADER_TOP = -10855846;
    private static final int HEADER_BOTTOM = -12632257;
    private static final int ACCENT = -4342339;
    private static final int TITLE_COLOR = -723724;
    private static final int MUTED_TEXT = -5197648;
    private static final int SELECTED = -12474273;
    private static final int CONTENT_BG = -14671840;
    private static final int CHIP_BASE = -13421773;
    private static final int CHIP_HOVER = -12105913;
    private static final int UNOWNED_DIM = -1727591912;
    private static final int PREVIEW_TOP = -13421773;
    private static final int PREVIEW_BOTTOM = -15198184;
    private static final int SCROLL_TRACK = -14671840;
    private static final int SCROLL_THUMB = -8947849;
    private final Text titleComponent;
    private final boolean showUnsetButton;
    private final Text unsetLabel;
    private List<TagRow> rows;
    private boolean ownedOnly;
    private int scrollRow;
    private int lastRevision;
    private String search = "";
    private TextFieldWidget searchBox;

    public TagsScreen(TagsOpenMsg msg) {
        super((Text)Text.literal((String)"Tags"));
        this.titleComponent = PreviewUi.deserialize(msg.getTitle(), "Tags");
        this.showUnsetButton = msg.getShowUnsetButton();
        this.unsetLabel = PreviewUi.deserialize(msg.getUnsetLabel(), "Remove Tag");
        this.rows = TagsScreen.deserializeEntries(msg.getEntries());
        PreviewUi.playOpen();
    }

    public void applyState(TagsStateMsg msg) {
        if (msg.getRevision() > this.lastRevision) {
            this.lastRevision = msg.getRevision();
            this.rows = TagsScreen.deserializeEntries(msg.getEntries());
            this.scrollRow = MathHelper.clamp((int)this.scrollRow, (int)0, (int)this.maxScrollRow());
        }
    }

    private static List<TagRow> deserializeEntries(List<TagsEntry> entries) {
        ArrayList<TagRow> out = new ArrayList<TagRow>();
        if (entries == null) {
            return out;
        }
        for (TagsEntry entry : entries) {
            if (entry == null || entry.getId() == null) continue;
            ArrayList<Text> description = new ArrayList<Text>();
            if (entry.getDescription() != null) {
                for (String line : entry.getDescription()) {
                    description.add(PreviewUi.deserialize(line, ""));
                }
            }
            Text tag = PreviewUi.deserialize(entry.getTagJson(), entry.getId());
            out.add(new TagRow(entry.getId(), tag, (tag.getString() + " " + entry.getId()).toLowerCase(Locale.ROOT), description, entry.getOwned(), entry.getActive(), entry.getOwnerCount()));
        }
        return out;
    }

    private List<TagRow> visibleRows() {
        ArrayList<TagRow> out = new ArrayList<TagRow>();
        String query = this.search.trim().toLowerCase(Locale.ROOT);
        for (TagRow row : this.rows) {
            if (this.ownedOnly && !row.owned() || !query.isEmpty() && !row.plain().contains(query)) continue;
            out.add(row);
        }
        return out;
    }

    private int panelLeft() {
        return (this.width - 440) / 2;
    }

    private int panelTop() {
        return (this.height - 240) / 2;
    }

    private int previewY0() {
        return this.panelTop() + 22;
    }

    private int previewY1() {
        return this.previewY0() + 16;
    }

    private int listX0() {
        return this.panelLeft() + 10;
    }

    private int listX1() {
        return this.panelLeft() + 440 - 16;
    }

    private int listY0() {
        return this.previewY1() + 4;
    }

    private int listY1() {
        return this.panelTop() + 240 - 30;
    }

    private int chipW() {
        return (this.listX1() - this.listX0() - 4) / 3;
    }

    private int visibleRowCount() {
        return Math.max(1, (this.listY1() - this.listY0()) / 18);
    }

    private static int gridRows(int count) {
        return (count + 3 - 1) / 3;
    }

    private int maxScrollRow() {
        return Math.max(0, TagsScreen.gridRows(this.visibleRows().size()) - this.visibleRowCount());
    }

    protected void init() {
        this.searchBox = new TextFieldWidget(this.textRenderer, this.panelLeft() + 440 - 8 - 120, this.previewY0(), 120, 16, (Text)Text.literal((String)"Search"));
        this.searchBox.setPlaceholder((Text)Text.literal((String)"Search...").formatted(Formatting.DARK_GRAY));
        this.searchBox.setText(this.search);
        this.searchBox.setChangedListener(value -> {
            this.search = value;
            this.scrollRow = 0;
        });
        this.addDrawableChild(this.searchBox);
        int y = this.panelTop() + 240 - 24;
        int x = this.panelLeft() + 8;
        if (this.showUnsetButton) {
            this.addDrawableChild(new ThemedButton(x, y, 100, 20, this.unsetLabel, b -> {
                this.rows = TagsScreen.withActive(this.rows, null);
                TagsNetworking.sendUnset();
            }));
            x += 106;
        }
        this.addDrawableChild(new ThemedButton(x, y, 110, 20, this.ownedOnlyLabel(), b -> {
            this.ownedOnly = !this.ownedOnly;
            this.scrollRow = 0;
            b.setMessage(this.ownedOnlyLabel());
        }));
        this.addDrawableChild(new ThemedButton(this.panelLeft() + 440 - 60, y, 52, 20, (Text)Text.literal((String)"Close"), b -> this.close()));
    }

    private Text ownedOnlyLabel() {
        return Text.literal((String)("Owned only: " + (this.ownedOnly ? "\u2714" : "\u2717")));
    }

    private static List<TagRow> withActive(List<TagRow> rows, String activeId) {
        ArrayList<TagRow> out = new ArrayList<TagRow>(rows.size());
        for (TagRow row : rows) {
            boolean active = row.id().equals(activeId);
            out.add(active == row.active() ? row : new TagRow(row.id(), row.tag(), row.plain(), row.description(), row.owned(), active, row.ownerCount()));
        }
        return out;
    }

    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMeasuringTimeMs();
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        Starfield.draw(guiGraphics, 0, 0, this.width, this.height, now, 44, 1337L, 0.5f);
        int left = this.panelLeft();
        int top = this.panelTop();
        guiGraphics.fill(left - 3, top - 3, left + 440 + 3, top + 240 + 3, 0x55000000);
        guiGraphics.fillGradient(left, top, left + 440, top + 240, -196589496, -198036942);
        guiGraphics.drawBorder(left - 1, top - 1, 442, 242, -15658735);
        guiGraphics.drawBorder(left, top, 440, 240, -7434610);
        guiGraphics.fillGradient(left, top, left + 440, top + 18, -10855846, -12632257);
        guiGraphics.fill(left, top + 18, left + 440, top + 19, -4342339);
        guiGraphics.drawCenteredTextWithShadow(this.textRenderer, this.titleComponent, left + 220, top + 5, -723724);
        TagRow hovered = this.hoveredRow(mouseX, mouseY);
        this.renderPreviewStrip(guiGraphics, hovered);
        this.renderGrid(guiGraphics, hovered);
        for (Element child : this.children()) {
            if (!(child instanceof Drawable)) continue;
            Drawable renderable = (Drawable)child;
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (hovered != null) {
            Tooltips.render(guiGraphics, this.textRenderer, this.tooltipFor(hovered), mouseX, mouseY);
        }
    }

    private void renderPreviewStrip(DrawContext guiGraphics, TagRow hovered) {
        PlayerEntity player;
        int x0 = this.panelLeft() + 8;
        int x1 = this.panelLeft() + 440 - 8 - 120 - 6;
        guiGraphics.fillGradient(x0, this.previewY0(), x1, this.previewY1(), -13421773, -15198184);
        guiGraphics.drawBorder(x0 - 1, this.previewY0() - 1, x1 - x0 + 2, this.previewY1() - this.previewY0() + 2, -7434610);
        TagRow shown = hovered;
        if (hovered == null) {
            for (TagRow row : this.rows) {
                if (!row.active()) continue;
                shown = row;
                break;
            }
        }
        String name = (player = MinecraftClient.getInstance().player) != null ? player.getGameProfile().getName() : "Player";
        MutableText preview = Text.empty();
        if (shown != null) {
            preview.append(shown.tag()).append(" ");
        }
        preview.append((Text)Text.literal((String)name).formatted(Formatting.WHITE)).append((Text)Text.literal((String)": Hello!").formatted(Formatting.GRAY));
        guiGraphics.enableScissor(x0, this.previewY0(), x1, this.previewY1());
        guiGraphics.drawTextWithShadow(this.textRenderer, (Text)preview, x0 + 5, this.previewY0() + 4, -1);
        guiGraphics.disableScissor();
    }

    private void renderGrid(DrawContext guiGraphics, TagRow hovered) {
        List<TagRow> visible = this.visibleRows();
        int x0 = this.listX0();
        int x1 = this.listX1();
        int y0 = this.listY0();
        int y1 = y0 + this.visibleRowCount() * 18;
        guiGraphics.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, -14671840);
        guiGraphics.drawBorder(x0 - 2, y0 - 2, x1 - x0 + 4, y1 - y0 + 4, -7434610);
        if (visible.isEmpty()) {
            guiGraphics.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.literal((String)"No tags to show"), (x0 + x1) / 2, (y0 + y1) / 2 - 4, -5197648);
        } else {
            int first;
            int chipW = this.chipW();
            guiGraphics.enableScissor(x0, y0, x1, y1);
            for (int i = first = this.scrollRow * 3; i < visible.size() && i < first + 3 * this.visibleRowCount(); ++i) {
                TagRow row = visible.get(i);
                int chipX = x0 + (i - first) % 3 * (chipW + 2);
                int chipY = y0 + (i - first) / 3 * 18;
                guiGraphics.fill(chipX, chipY + 1, chipX + chipW, chipY + 18 - 1, row == hovered ? -12105913 : -13421773);
                guiGraphics.enableScissor(chipX + 1, chipY + 1, chipX + chipW - 1, chipY + 18 - 1);
                guiGraphics.drawTextWithShadow(this.textRenderer, row.tag(), chipX + 5, chipY + 5, -1);
                guiGraphics.disableScissor();
                if (!row.owned()) {
                    guiGraphics.fill(chipX, chipY + 1, chipX + chipW, chipY + 18 - 1, -1727591912);
                }
                if (row.active()) {
                    guiGraphics.drawBorder(chipX, chipY + 1, chipW, 16, -12474273);
                    continue;
                }
                if (row != hovered) continue;
                guiGraphics.drawBorder(chipX, chipY + 1, chipW, 16, -1717743907);
            }
            guiGraphics.disableScissor();
            int maxScroll = this.maxScrollRow();
            if (maxScroll > 0) {
                int trackX = x1 + 4;
                int trackH = y1 - y0;
                guiGraphics.fill(trackX, y0, trackX + 4, y1, -14671840);
                int thumbH = Math.max(10, trackH * this.visibleRowCount() / (maxScroll + this.visibleRowCount()));
                int thumbY = y0 + (trackH - thumbH) * this.scrollRow / maxScroll;
                guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, -8947849);
            }
        }
    }

    private List<Text> tooltipFor(TagRow row) {
        ArrayList<Text> lines = new ArrayList<Text>();
        lines.add(row.tag());
        if (!row.description().isEmpty()) {
            lines.add((Text)Text.empty());
            lines.addAll(row.description());
        }
        lines.add((Text)Text.empty());
        if (row.ownerCount() >= 0) {
            lines.add((Text)Text.literal((String)"Owners: ").formatted(Formatting.GRAY).append((Text)Text.literal((String)String.valueOf(row.ownerCount())).formatted(Formatting.WHITE)));
        }
        lines.add((Text)Text.literal((String)"Active: ").formatted(Formatting.GRAY).append((Text)(row.active() ? Text.literal((String)"Activated").formatted(Formatting.GREEN) : Text.literal((String)"Deactivated").formatted(Formatting.RED))));
        lines.add((Text)Text.literal((String)"Obtained: ").formatted(Formatting.GRAY).append((Text)(row.owned() ? Text.literal((String)"Obtained").formatted(Formatting.GREEN) : Text.literal((String)"Not obtained").formatted(Formatting.RED))));
        return lines;
    }

    private TagRow hoveredRow(double mouseX, double mouseY) {
        int x0 = this.listX0();
        int y0 = this.listY0();
        if (!(mouseX < (double)x0 || mouseX >= (double)this.listX1() || mouseY < (double)y0 || mouseY >= (double)(y0 + this.visibleRowCount() * 18))) {
            int col = (int)((mouseX - (double)x0) / (double)(this.chipW() + 2));
            if (col >= 3) {
                return null;
            }
            int index = (this.scrollRow + (int)((mouseY - (double)y0) / 18.0)) * 3 + col;
            List<TagRow> visible = this.visibleRows();
            return index >= 0 && index < visible.size() ? visible.get(index) : null;
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        TagRow clicked;
        if (this.searchBox != null && this.searchBox.isFocused() && !this.searchBox.isMouseOver(mouseX, mouseY)) {
            this.searchBox.setFocused(false);
        }
        if ((clicked = this.hoveredRow(mouseX, mouseY)) != null) {
            if (clicked.owned() && !clicked.active()) {
                this.rows = TagsScreen.withActive(this.rows, clicked.id());
                TagsNetworking.sendSet(clicked.id());
                PreviewUi.playClick();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x0 = this.listX0();
        int y0 = this.listY0();
        if (mouseX >= (double)(x0 - 2) && mouseX < (double)(this.listX1() + 8) && mouseY >= (double)y0 && mouseY < (double)(y0 + this.visibleRowCount() * 18)) {
            this.scrollRow = MathHelper.clamp((int)(this.scrollRow - (int)Math.signum(scrollY)), (int)0, (int)this.maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
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

    @Environment(value=EnvType.CLIENT)
    private record TagRow(String id, Text tag, String plain, List<Text> description, boolean owned, boolean active, int ownerCount) {
    }
}

