package com.cobbleclub.client.contracts;

import com.cobbleclub.client.ui.ClubPalette;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class ContractsScreen extends Screen {
    private static final int PANEL_W = 448;
    private static final int PANEL_H = 250;
    private static final int LEFT_W = 192;
    private static final int ROW_H = 25;
    private static final int VISIBLE_ROWS = 6;

    private ContractsState state;
    private Tab tab = Tab.REFRESHING;
    private int selectedIndex;
    private int scrollRow;

    private ThemedButton refreshingTab;
    private ThemedButton milestonesTab;
    private ThemedButton journeyTab;
    private ThemedButton claimButton;

    public ContractsScreen(ContractsState state) {
        super(Text.literal("CobbleClub Contracts"));
        this.state = state;
    }

    public void applyState(ContractsState state) {
        String selectedId = selected() == null ? null : selected().id();
        this.state = state;
        List<ContractsState.Entry> entries = entries();

        if (selectedId != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (selectedId.equals(entries.get(i).id())) {
                    selectedIndex = i;
                    clampScroll();
                    return;
                }
            }
        }

        selectedIndex = MathHelper.clamp(selectedIndex, 0, Math.max(0, entries.size() - 1));
        clampScroll();
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();

        int tabGap = 4;
        int tabW = (PANEL_W - 16 - tabGap * 2) / 3;
        int tabY = y + 30;

        refreshingTab = addDrawableChild(new ThemedButton(
                x + 8, tabY, tabW, 20, Text.literal("Refreshing"),
                button -> setTab(Tab.REFRESHING)
        ));
        milestonesTab = addDrawableChild(new ThemedButton(
                x + 8 + tabW + tabGap, tabY, tabW, 20, Text.literal("Milestones"),
                button -> setTab(Tab.MILESTONES)
        ));
        journeyTab = addDrawableChild(new ThemedButton(
                x + 8 + (tabW + tabGap) * 2, tabY, tabW, 20, Text.literal("Journey"),
                button -> setTab(Tab.JOURNEY)
        ));

        claimButton = addDrawableChild(new ThemedButton(
                x + 202, y + 218, 112, 22, Text.literal("Claim Reward"),
                ThemedButton.Variant.GREEN,
                button -> {
                    ContractsState.Entry entry = selected();
                    if (entry != null && entry.claimable()) {
                        ContractsNetworking.claim(entry.id());
                    }
                }
        ));

        addDrawableChild(new ThemedButton(
                x + 318, y + 218, 58, 22, Text.literal("Refresh"),
                button -> ContractsNetworking.refresh()
        ));

        addDrawableChild(new ThemedButton(
                x + 380, y + 218, 60, 22, Text.literal("Close"),
                button -> close()
        ));
    }

    private void setTab(Tab tab) {
        if (tab == this.tab) return;
        this.tab = tab;
        this.selectedIndex = 0;
        this.scrollRow = 0;
        PreviewUi.playClick();
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int top() {
        return (height - PANEL_H) / 2;
    }

    private List<ContractsState.Entry> entries() {
        return state.entries(tab.key);
    }

    private ContractsState.Entry selected() {
        List<ContractsState.Entry> entries = entries();
        return selectedIndex >= 0 && selectedIndex < entries.size() ? entries.get(selectedIndex) : null;
    }

    private int maxScroll() {
        return Math.max(0, entries().size() - VISIBLE_ROWS);
    }

    private void clampScroll() {
        scrollRow = MathHelper.clamp(scrollRow, 0, maxScroll());
        if (selectedIndex < scrollRow) scrollRow = selectedIndex;
        if (selectedIndex >= scrollRow + VISIBLE_ROWS) {
            scrollRow = Math.max(0, selectedIndex - VISIBLE_ROWS + 1);
        }
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB5000000);
        Starfield.draw(g, 0, 0, width, height, System.currentTimeMillis(), 48, 771129L, 0.34f);

        int x = left();
        int y = top();

        g.fill(x - 3, y - 3, x + PANEL_W + 3, y + PANEL_H + 3, ClubPalette.SHADOW);
        g.fillGradient(x, y, x + PANEL_W, y + PANEL_H, ClubPalette.PANEL_TOP, ClubPalette.PANEL_BOTTOM);
        g.drawBorder(x - 1, y - 1, PANEL_W + 2, PANEL_H + 2, ClubPalette.PANEL_BORDER_OUT);
        g.drawBorder(x, y, PANEL_W, PANEL_H, ClubPalette.PANEL_BORDER_IN);

        g.fillGradient(x, y, x + PANEL_W, y + 25, ClubPalette.HEADER_TOP, ClubPalette.HEADER_BOTTOM);
        g.fill(x, y + 24, x + PANEL_W, y + 25, ClubPalette.ACCENT);
        g.drawCenteredTextWithShadow(textRenderer, "CobbleClub · Contracts", x + PANEL_W / 2, y + 8, ClubPalette.TITLE_COLOR);

        drawTabUnderline(g, x, y);

        int listX = x + 8;
        int listY = y + 58;
        int listH = VISIBLE_ROWS * ROW_H;
        int detailX = x + 202;
        int detailY = y + 58;
        int detailW = 238;
        int detailH = 154;

        g.fill(listX, listY, listX + LEFT_W, listY + listH, ClubPalette.CONTENT_BG);
        g.drawBorder(listX, listY, LEFT_W, listH, ClubPalette.PANEL_BORDER_IN);

        g.fill(detailX, detailY, detailX + detailW, detailY + detailH, ClubPalette.CONTENT_BG);
        g.drawBorder(detailX, detailY, detailW, detailH, ClubPalette.PANEL_BORDER_IN);

        renderList(g, mouseX, mouseY, listX, listY);
        renderDetails(g, detailX, detailY, detailW, detailH);

        for (Element child : children()) {
            if (child instanceof Drawable drawable) {
                drawable.render(g, mouseX, mouseY, delta);
            }
        }

        updateClaimButton();

        String footer;
        if (!state.notice.isBlank()) {
            footer = state.notice;
        } else if (tab == Tab.REFRESHING) {
            footer = "Refreshes in " + state.refreshText + " · rewards pay automatically";
        } else {
            footer = "Balance: " + state.balanceText;
        }

        int footerColor = state.notice.isBlank()
                ? ClubPalette.MUTED_TEXT
                : state.error ? ClubPalette.NEGATIVE : ClubPalette.POSITIVE;

        float footerScale = 0.72f;
        g.getMatrices().push();
        g.getMatrices().scale(footerScale, footerScale, 1.0f);
        g.drawTextWithShadow(
                textRenderer,
                trimToWidth(footer, (int)((LEFT_W - 4) / footerScale)),
                (int)((x + 10) / footerScale),
                (int)((y + 224) / footerScale),
                footerColor
        );
        g.getMatrices().pop();
    }

    private void drawTabUnderline(DrawContext g, int x, int y) {
        int gap = 4;
        int tabW = (PANEL_W - 16 - gap * 2) / 3;
        int index = tab == Tab.REFRESHING ? 0 : tab == Tab.MILESTONES ? 1 : 2;
        int tabX = x + 8 + index * (tabW + gap);
        g.fill(tabX + 5, y + 49, tabX + tabW - 5, y + 51, 0xFFD0D0D0);
    }

    private void renderList(DrawContext g, int mouseX, int mouseY, int x, int y) {
        List<ContractsState.Entry> entries = entries();

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scrollRow + row;
            if (index >= entries.size()) break;

            ContractsState.Entry entry = entries.get(index);
            int rowY = y + row * ROW_H;
            boolean hover = mouseX >= x + 1 && mouseX < x + LEFT_W - 7
                    && mouseY >= rowY + 1 && mouseY < rowY + ROW_H;
            boolean selected = index == selectedIndex;

            int fill = selected ? 0xFF515151 : hover ? ClubPalette.ROW_HOVER : ClubPalette.ROW_BASE;
            g.fill(x + 1, rowY + 1, x + LEFT_W - 7, rowY + ROW_H, fill);

            if (selected) {
                g.fill(x + 1, rowY + 1, x + 4, rowY + ROW_H, 0xFFD0D0D0);
            }

            int titleColor = entry.locked() ? 0xFF9A9A9A : ClubPalette.TEXT;
            g.drawTextWithShadow(textRenderer, trimToWidth(entry.title(), LEFT_W - 24), x + 8, rowY + 4, titleColor);

            float small = 0.66f;
            g.getMatrices().push();
            g.getMatrices().scale(small, small, 1.0f);
            g.drawTextWithShadow(
                    textRenderer,
                    trimToWidth(entry.status(), (int)((LEFT_W - 24) / small)),
                    (int)((x + 8) / small),
                    (int)((rowY + 15) / small),
                    statusColor(entry)
            );
            g.getMatrices().pop();
        }

        if (entries.size() > VISIBLE_ROWS) {
            int trackX = x + LEFT_W - 5;
            int trackY = y + 2;
            int trackH = VISIBLE_ROWS * ROW_H - 4;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, ClubPalette.SCROLL_TRACK);

            int max = maxScroll();
            int thumbH = Math.max(16, trackH * VISIBLE_ROWS / entries.size());
            int thumbY = trackY + (trackH - thumbH) * scrollRow / Math.max(1, max);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ClubPalette.SCROLL_THUMB);
        }
    }

    private void renderDetails(DrawContext g, int x, int y, int w, int h) {
        ContractsState.Entry entry = selected();
        if (entry == null) {
            g.drawCenteredTextWithShadow(textRenderer, "No contracts available", x + w / 2, y + h / 2, ClubPalette.MUTED_TEXT);
            return;
        }

        g.drawTextWithShadow(textRenderer, trimToWidth(entry.title(), w - 16), x + 8, y + 7, ClubPalette.TITLE_COLOR);

        float subtitleScale = 0.68f;
        g.getMatrices().push();
        g.getMatrices().scale(subtitleScale, subtitleScale, 1.0f);
        g.drawTextWithShadow(
                textRenderer,
                trimToWidth(entry.subtitle(), (int)((w - 16) / subtitleScale)),
                (int)((x + 8) / subtitleScale),
                (int)((y + 19) / subtitleScale),
                ClubPalette.MUTED_TEXT
        );
        g.getMatrices().pop();

        int cursorY = y + 31;
        cursorY = drawWrapped(g, entry.description(), x + 8, cursorY, w - 16, 0xFFCCCCCC, 2);

        cursorY += 3;
        int objectiveLimit = Math.min(3, entry.objectives().size());
        for (int i = 0; i < objectiveLimit; i++) {
            ContractsState.Objective objective = entry.objectives().get(i);
            int color = objective.complete() ? ClubPalette.POSITIVE : 0xFFD0D0D0;
            String line = (objective.complete() ? "✓ " : "• ")
                    + objective.label() + "  " + formatNumber(objective.current()) + "/" + formatNumber(objective.target());
            g.drawTextWithShadow(textRenderer, trimToWidth(line, w - 16), x + 8, cursorY, color);
            cursorY += 12;
        }

        int rewardY = y + h - 38;
        g.drawTextWithShadow(textRenderer, "Reward", x + 8, rewardY, 0xFFE4E4E4);

        float rewardScale = 0.68f;
        g.getMatrices().push();
        g.getMatrices().scale(rewardScale, rewardScale, 1.0f);
        g.drawTextWithShadow(
                textRenderer,
                trimToWidth(entry.reward(), (int)((w - 16) / rewardScale)),
                (int)((x + 8) / rewardScale),
                (int)((rewardY + 11) / rewardScale),
                ClubPalette.MUTED_TEXT
        );
        g.getMatrices().pop();

        int barX = x + 8;
        int barY = y + h - 13;
        int barW = w - 16;
        g.fill(barX, barY, barX + barW, barY + 6, 0xFF181818);
        g.fill(barX + 1, barY + 1, barX + barW - 1, barY + 5, 0xFF454545);
        int fill = (int)Math.round((barW - 2) * entry.ratio());
        if (fill > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 5,
                    entry.claimed() ? 0xFF60926A : entry.claimable() ? 0xFF77A681 : 0xFF929292);
        }
    }

    private void updateClaimButton() {
        if (claimButton == null) return;
        ContractsState.Entry entry = selected();
        if (entry == null) {
            claimButton.active = false;
            claimButton.setMessage(Text.literal("Claim Reward"));
            return;
        }

        claimButton.active = entry.claimable();
        String label;
        if (entry.autoReward()) {
            label = entry.claimed() ? "Paid" : "Auto Reward";
        } else if (entry.claimed()) {
            label = "Claimed";
        } else if (entry.locked()) {
            label = "Locked";
        } else if (entry.claimable()) {
            label = "Claim Reward";
        } else {
            label = "In Progress";
        }
        claimButton.setMessage(Text.literal(label));
    }

    private int statusColor(ContractsState.Entry entry) {
        if (entry.claimed()) return ClubPalette.POSITIVE;
        if (entry.claimable()) return 0xFF9FD4A8;
        if (entry.locked()) return 0xFF909090;
        if ("RCT PATH PENDING".equals(entry.status())) return ClubPalette.WARNING;
        return ClubPalette.MUTED_TEXT;
    }

    private int drawWrapped(DrawContext g, String text, int x, int y, int width, int color, int maxLines) {
        if (text == null || text.isBlank()) return y;
        List<String> lines = wrap(text, width);
        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            String line = lines.get(i);
            if (i == count - 1 && lines.size() > count) {
                line = trimToWidth(line + "…", width);
            }
            g.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 11;
        }
        return y;
    }

    private List<String> wrap(String text, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && textRenderer.getWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                if (!current.isEmpty()) current.append(' ');
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) return "";
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        int limit = Math.max(0, maxWidth - textRenderer.getWidth(ellipsis));
        return textRenderer.trimToWidth(text, limit) + ellipsis;
    }

    private static String formatNumber(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, value));
    }

    private int rowAt(double mouseX, double mouseY) {
        int x = left() + 8;
        int y = top() + 58;
        if (mouseX < x + 1 || mouseX >= x + LEFT_W - 7 || mouseY < y || mouseY >= y + VISIBLE_ROWS * ROW_H) {
            return -1;
        }
        int row = (int)((mouseY - y) / ROW_H);
        int index = scrollRow + row;
        return index >= 0 && index < entries().size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = rowAt(mouseX, mouseY);
        if (button == 0 && index >= 0) {
            selectedIndex = index;
            PreviewUi.playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = left() + 8;
        int y = top() + 58;
        if (mouseX >= x && mouseX < x + LEFT_W && mouseY >= y && mouseY < y + VISIBLE_ROWS * ROW_H) {
            scrollRow = MathHelper.clamp(scrollRow - (int)Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Tab {
        REFRESHING("refreshing"),
        MILESTONES("milestones"),
        JOURNEY("journey");

        final String key;

        Tab(String key) {
            this.key = key;
        }
    }
}
