/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1304
 *  net.minecraft.class_1304$class_1305
 *  net.minecraft.class_156
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_3532
 *  net.minecraft.class_364
 *  net.minecraft.class_4068
 *  net.minecraft.class_437
 *  net.minecraft.class_5151
 *  net.minecraft.class_5250
 */
package com.cobbleclub.client.crate;

import com.cobbleclub.client.crate.CrateCatalog;
import com.cobbleclub.client.crate.CratePreviewNetworking;
import com.cobbleclub.client.furniturepreview.FurniturePreviewRenderer;
import com.cobbleclub.client.gearpreview.GearPreviewRenderer;
import com.cobbleclub.client.pokemonpreview.PokemonFormCatalog;
import com.cobbleclub.client.pokemonpreview.PokemonPreviewRenderer;
import com.cobbleclub.client.ui.ClubScrollbar;
import com.cobbleclub.client.ui.GuiDepth;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import com.cobbleclub.client.wardrobe.WardrobePreviewRenderer;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Util;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Equipment;
import net.minecraft.text.MutableText;

@Environment(value=EnvType.CLIENT)
public class CratePreviewScreen
extends Screen {
    private static final int PANEL_W = 384;
    private static final int PANEL_H = 220;
    private static final int GRID_COLS = 9;
    private static final int CELL = 20;
    private static final int FORM_COLS = 7;
    private static final int MAX_ROWS = 9;
    private static final int SEARCH_CAP = 108;
    private static final int PANEL_TOP = -196589496;
    private static final int PANEL_BOTTOM = -198036942;
    private static final int PANEL_BORDER_OUT = -15658735;
    private static final int PANEL_BORDER_IN = -7434610;
    private static final int HEADER_TOP = -10855846;
    private static final int HEADER_BOTTOM = -12632257;
    private static final int DEFAULT_ACCENT = -4342339;
    private static final int TITLE_COLOR = -723724;
    private static final int SELECTED = -1;
    private static final int TIER_GREEN = -10688932;
    private static final int TIER_BLUE = -11094273;
    private static final int TIER_RED = -893860;
    private static final int NO_HIGHLIGHT = 0;
    private static final int ODDS_NEUTRAL = -3554586;
    private static final int SLOT_BASE = -13421773;
    private static final int PREVIEW_TOP = -13421773;
    private static final int PREVIEW_BOTTOM = -15198184;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.6f;
    private static final float ZOOM_STEP = 1.12f;
    private static final float MAX_PITCH = 40.0f;
    private static final float AUTO_SPIN_DEG_PER_MS = 0.03f;
    private static final long AUTO_SPIN_IDLE_MS = 1500L;
    private final List<PrizeEntry> allPrizes = new ArrayList<PrizeEntry>();
    private final List<GridRow> gridRows = new ArrayList<GridRow>();
    private int[] prizeLine = new int[0];
    private final Map<Integer, ItemStack> stackCache = new HashMap<Integer, ItemStack>();
    private final int accent;
    private final String crateId;
    private final boolean canTest;
    private final WardrobePreviewRenderer wornPreview = new WardrobePreviewRenderer();
    private final GearPreviewRenderer armorPreview = new GearPreviewRenderer();
    private final boolean hasShinyToggle;
    private boolean shiny;
    private TextFieldWidget search;
    private boolean searchVisible;
    private int scrollRow;
    private int selectedEntry = -1;
    private int hoveredEntry = -1;
    private List<PokemonFormCatalog.FormEntry> shownForms = List.of();
    private int shownFormsFor = -1;
    private int hoveredForm = -1;
    private int selectedForm = -1;
    private final Map<String, ItemStack> formStackCache = new HashMap<String, ItemStack>();
    private final Map<EquipmentSlot, PrizeEntry> wornArmor = new EnumMap<EquipmentSlot, PrizeEntry>(EquipmentSlot.class);
    private int hoverSuppressed = -1;
    private List<PrizeEntry> view = new ArrayList<PrizeEntry>();
    private float yaw;
    private float pitch;
    private float zoom = 1.0f;
    private boolean dragging;
    private boolean scrollDragging;
    private long lastInteractMs;
    private long lastFrameMs = Util.getMeasuringTimeMs();

    public CratePreviewScreen(CrateCatalog catalog) {
        super(PreviewUi.deserialize(catalog.title, "Crate"));
        this.accent = CratePreviewScreen.parseAccent(catalog.gradient);
        this.crateId = catalog.id;
        this.canTest = catalog.canTest;
        this.hasShinyToggle = catalog.shinyPreview;
        if (catalog.prizes != null) {
            Map<Double, Rarity> scheme = CratePreviewScreen.buildScheme(catalog.prizes);
            for (int i = 0; i < catalog.prizes.size(); ++i) {
                CrateCatalog.Prize p = catalog.prizes.get(i);
                if (p == null) continue;
                boolean pokemon = p.species != null && !p.species.isBlank();
                Text label = PreviewUi.deserialize(p.displayName, pokemon ? p.species : p.material);
                Text shinyLabel = p.shinyDisplayName != null ? PreviewUi.deserialize(p.shinyDisplayName, "") : null;
                Rarity r = scheme.get(p.chance);
                MutableText paneSummary = Text.literal((String)("Chance: " + CratePreviewScreen.oddsStr(p.chance)));
                ArrayList<Text> tooltip = new ArrayList<Text>();
                tooltip.add(label);
                if (p.lore != null) {
                    for (String line : p.lore) {
                        tooltip.add(PreviewUi.deserialize(line, ""));
                    }
                }
                if (this.canTest) {
                    tooltip.add((Text)Text.literal((String)"Right-click to test reward").withColor(-3116801));
                    if (p.shinyEligible) {
                        tooltip.add((Text)Text.literal((String)"Shift+right-click to test shiny reward").withColor(-11702));
                    }
                }
                ArrayList<Text> shinyTooltip = null;
                if (shinyLabel != null) {
                    shinyTooltip = new ArrayList<Text>(tooltip);
                    shinyTooltip.set(0, shinyLabel);
                }
                this.allPrizes.add(new PrizeEntry(label, shinyLabel, pokemon, p.species, p.aspects != null ? p.aspects : List.of(), p.material, p.customModelData, p.shinyCustomModelData, p.shinyEligible, p.amount, i, r, (Text)paneSummary, tooltip, shinyTooltip, CratePreviewScreen.parseWornSlot(p.wornSlot), p.evolutions));
            }
        }
        this.allPrizes.sort(Comparator.comparingInt(e -> e.rarity().rank()));
        PreviewUi.playOpen();
    }

    public void close() {
        PreviewUi.playClose();
        this.wornPreview.close();
        this.armorPreview.close();
        super.close();
    }

    private static int parseAccent(String gradient) {
        int hash;
        if (gradient != null && (hash = gradient.indexOf(35)) >= 0 && hash + 7 <= gradient.length()) {
            try {
                return 0xFF000000 | Integer.parseInt(gradient.substring(hash + 1, hash + 7), 16);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return -4342339;
    }

    private static String oddsStr(double chance) {
        if (chance <= 0.0) {
            return "1 in ?";
        }
        double odds = 100.0 / chance;
        double rounded = odds >= 100.0 ? (double)Math.round(odds) : (double)Math.round(odds * 10.0) / 10.0;
        String var10000 = rounded % 1.0 == 0.0 ? String.valueOf((long)rounded) : String.valueOf(rounded);
        return "1 in " + var10000;
    }

    private static Map<Double, Rarity> buildScheme(List<CrateCatalog.Prize> prizes) {
        ArrayList<Double> chances = new ArrayList<Double>();
        for (CrateCatalog.Prize p : prizes) {
            if (p == null) continue;
            chances.add(p.chance);
        }
        List<Double> distinct = chances.stream().distinct().sorted(Comparator.reverseOrder()).toList();
        HashMap<Double, Rarity> tiers = new HashMap<Double, Rarity>();
        if (distinct.size() <= 1) {
            for (Double c : distinct) {
                tiers.put(c, new Rarity(0, 0));
            }
        } else if (distinct.size() == 3) {
            int[] colors = new int[]{-10688932, -11094273, -893860};
            for (int i = 0; i < 3; ++i) {
                tiers.put((Double)distinct.get(i), new Rarity(2 - i, colors[i]));
            }
        } else {
            for (Double c : distinct) {
                tiers.put(c, CratePreviewScreen.rarityOf(c));
            }
        }
        return tiers;
    }

    private static Rarity rarityOf(double chance) {
        if (chance >= 20.0) {
            return new Rarity(4, -5197648);
        }
        if (chance >= 5.0) {
            return new Rarity(3, -10688932);
        }
        if (chance >= 1.0) {
            return new Rarity(2, -11094273);
        }
        return chance >= 0.1 ? new Rarity(1, -4169473) : new Rarity(0, -16336);
    }

    private static int oddsArgb(Rarity r) {
        return r.color() == 0 ? -3554586 : r.color();
    }

    private Text shinyLabel() {
        return Text.literal((String)(this.shiny ? "Shiny: On" : "Shiny: Off"));
    }

    private static WardrobeSlot parseWornSlot(String slot) {
        if (slot == null) {
            return null;
        }
        return switch (slot.toLowerCase(Locale.ROOT)) {
            case "helmet", "hat", "head" -> WardrobeSlot.HELMET;
            case "wings", "backpack" -> WardrobeSlot.BACKPACK;
            case "balloon" -> WardrobeSlot.BALLOON;
            default -> null;
        };
    }

    private void rebuildView() {
        int keep = this.selectedEntry >= 0 && this.selectedEntry < this.view.size() ? this.view.get(this.selectedEntry).index() : -1;
        this.view = this.computeView();
        this.buildGridRows();
        this.selectedEntry = this.view.isEmpty() ? -1 : 0;
        for (int i = 0; i < this.view.size(); ++i) {
            if (this.view.get(i).index() != keep) continue;
            this.selectedEntry = i;
            break;
        }
        this.scrollRow = MathHelper.clamp((int)this.scrollRow, (int)0, (int)this.maxScrollRow());
        if (this.selectedEntry >= 0) {
            int rowOf = this.prizeLine[this.selectedEntry];
            if (rowOf < this.scrollRow) {
                this.scrollRow = rowOf;
            } else if (rowOf >= this.scrollRow + this.visibleRows()) {
                this.scrollRow = MathHelper.clamp((int)(rowOf - this.visibleRows() + 1), (int)0, (int)this.maxScrollRow());
            }
        }
        this.hoverSuppressed = -1;
    }

    private void buildGridRows() {
        this.gridRows.clear();
        this.prizeLine = new int[this.view.size()];
        int i = 0;
        boolean firstTier = true;
        while (i < this.view.size()) {
            int rank = this.view.get(i).rarity().rank();
            ArrayList<Integer> group = new ArrayList<Integer>();
            while (i < this.view.size() && this.view.get(i).rarity().rank() == rank) {
                group.add(i);
                ++i;
            }
            if (!firstTier) {
                this.gridRows.add(new GridRow(new int[0]));
            }
            firstTier = false;
            for (int j = 0; j < group.size(); j += 9) {
                int n = Math.min(9, group.size() - j);
                int[] rowPrizes = new int[n];
                for (int k = 0; k < n; ++k) {
                    rowPrizes[k] = (Integer)group.get(j + k);
                    this.prizeLine[((Integer)group.get((int)(j + k))).intValue()] = this.gridRows.size();
                }
                this.gridRows.add(new GridRow(rowPrizes));
            }
        }
    }

    private List<PrizeEntry> computeView() {
        String q;
        String string = q = this.search != null ? this.search.getText().trim().toLowerCase() : "";
        if (q.isEmpty()) {
            return this.allPrizes;
        }
        ArrayList<PrizeEntry> out = new ArrayList<PrizeEntry>();
        for (PrizeEntry e : this.allPrizes) {
            if (!this.labelFor(e).getString().toLowerCase().contains(q)) continue;
            out.add(e);
            if (out.size() < 108) continue;
            break;
        }
        return out;
    }

    private ItemStack stackFor(PrizeEntry e) {
        return this.stackCache.computeIfAbsent(e.index(), k -> {
            ItemStack stack;
            if (e.pokemon()) {
                stack = PokemonPreviewRenderer.modelStack(e.species(), this.shiny ? CratePreviewScreen.withShiny(e.aspects()) : e.aspects());
            } else {
                int cmd = this.shiny && e.shinyCustomModelData() > 0 ? e.shinyCustomModelData() : e.customModelData();
                stack = FurniturePreviewRenderer.stackFor(e.material(), cmd, null);
            }
            if (!stack.isEmpty()) {
                stack.setCount(MathHelper.clamp((int)e.amount(), (int)1, (int)99));
            }
            return stack;
        });
    }

    private static List<String> withShiny(List<String> aspects) {
        if (aspects.contains("shiny")) {
            return aspects;
        }
        ArrayList<String> out = new ArrayList<String>(aspects);
        out.add("shiny");
        return out;
    }

    private Text labelFor(PrizeEntry e) {
        return this.shiny && e.shinyLabel() != null ? e.shinyLabel() : e.label();
    }

    private List<Text> tooltipFor(PrizeEntry e) {
        return this.shiny && e.shinyTooltip() != null ? e.shinyTooltip() : e.tooltip();
    }

    private PrizeEntry shownEntry() {
        int idx = this.hoveredEntry >= 0 ? this.hoveredEntry : this.selectedEntry;
        return idx >= 0 && idx < this.view.size() ? this.view.get(idx) : null;
    }

    private List<PokemonFormCatalog.FormEntry> formsFor(PrizeEntry e) {
        if (e != null && e.pokemon()) {
            if (e.index() != this.shownFormsFor) {
                this.shownForms = PokemonFormCatalog.prizeFormButtons(e.species(), e.aspects(), e.evolutions());
                this.shownFormsFor = e.index();
                this.selectedForm = -1;
            }
            return this.shownForms;
        }
        return List.of();
    }

    private ItemStack formStack(PokemonFormCatalog.FormEntry f) {
        String var10000 = f.getSpecies();
        String key = var10000 + "|" + String.join((CharSequence)",", f.getAspects()) + (this.shiny ? "|shiny" : "");
        return this.formStackCache.computeIfAbsent(key, k -> PokemonPreviewRenderer.modelStack(f.getSpecies(), this.shiny ? CratePreviewScreen.withShiny(f.getAspects()) : f.getAspects()));
    }

    private Text formLabel(PokemonFormCatalog.FormEntry f) {
        return Text.literal((String)(this.shiny ? "Shiny " + f.getLabel() : f.getLabel()));
    }

    private int[] formTilePos(int i, int count) {
        int row = i / 7;
        int col = i % 7;
        int rowCount = Math.min(7, count - row * 7);
        int x = this.previewX1() - 3 - (rowCount - col) * 20;
        int y = this.previewY0() + 3 + row * 20;
        return new int[]{x, y};
    }

    private int formIndexAt(double mouseX, double mouseY, int count) {
        for (int i = 0; i < count; ++i) {
            int[] p = this.formTilePos(i, count);
            if (!(mouseX >= (double)p[0]) || !(mouseX < (double)(p[0] + 20 - 1)) || !(mouseY >= (double)p[1]) || !(mouseY < (double)(p[1] + 20 - 1))) continue;
            return i;
        }
        return -1;
    }

    private EquipmentSlot armorSlotFor(PrizeEntry e) {
        if (!e.pokemon() && e.wornSlot() == null) {
            Equipment equipable = Equipment.fromStack((ItemStack)this.stackFor(e));
            return equipable != null && equipable.getSlotType().getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? equipable.getSlotType() : null;
        }
        return null;
    }

    private void selectView(int idx) {
        this.selectedEntry = idx;
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.lastInteractMs = Util.getMeasuringTimeMs();
    }

    private int panelLeft() {
        return (this.width - 384) / 2;
    }

    private int panelTop() {
        return (this.height - 220) / 2;
    }

    private int previewX0() {
        return this.panelLeft() + 8;
    }

    private int previewY0() {
        return this.panelTop() + 22;
    }

    private int previewX1() {
        return this.panelLeft() + 162;
    }

    private int previewY1() {
        return this.panelTop() + 220 - 38;
    }

    private int gridX() {
        return this.panelLeft() + 172;
    }

    private int searchY() {
        return this.panelTop() + 24;
    }

    private int gridY() {
        return this.searchVisible ? this.searchY() + 16 : this.searchY();
    }

    private int visibleRows() {
        int fit = (this.panelTop() + 220 - 26 - this.gridY()) / 20;
        return MathHelper.clamp((int)this.gridRows.size(), (int)1, (int)Math.max(1, Math.min(9, fit)));
    }

    private int maxScrollRow() {
        return Math.max(0, this.gridRows.size() - this.visibleRows());
    }

    protected void init() {
        this.lastFrameMs = Util.getMeasuringTimeMs();
        this.search = new TextFieldWidget(this.textRenderer, this.gridX(), this.searchY(), 180, 12, (Text)Text.literal((String)"Search"));
        this.search.setPlaceholder((Text)Text.literal((String)"Search prizes\u2026"));
        this.search.setChangedListener(s -> this.rebuildView());
        this.search.setDrawsBackground(true);
        this.rebuildView();
        int fitWithoutSearch = (this.panelTop() + 220 - 26 - this.searchY()) / 20;
        boolean bl = this.searchVisible = this.gridRows.size() > Math.min(9, fitWithoutSearch);
        if (this.searchVisible) {
            this.addDrawableChild(this.search);
        }
        int footerY = this.panelTop() + 220 - 24;
        if (this.hasShinyToggle) {
            this.addDrawableChild(new ThemedButton(this.panelLeft() + 384 - 140, footerY, 74, 20, this.shinyLabel(), b -> {
                this.shiny = !this.shiny;
                this.stackCache.clear();
                this.rebuildView();
                b.setMessage(this.shinyLabel());
            }));
        }
        this.addDrawableChild(new ThemedButton(this.panelLeft() + 384 - 60, footerY, 52, 20, (Text)Text.literal((String)"Close"), b -> this.close()));
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        EquipmentSlot shownArmorSlot;
        long now = Util.getMeasuringTimeMs();
        long dt = now - this.lastFrameMs;
        this.lastFrameMs = now;
        if (!this.dragging && now - this.lastInteractMs > 1500L) {
            this.yaw += 0.03f * (float)dt;
        }
        int n = this.hoveredEntry = !this.dragging && !this.scrollDragging ? this.hoveredIndex(mouseX, mouseY) : -1;
        if (this.hoverSuppressed >= 0 && this.hoveredEntry != this.hoverSuppressed) {
            this.hoverSuppressed = -1;
        }
        this.renderBackground(g, mouseX, mouseY, partialTick);
        int left = this.panelLeft();
        int top = this.panelTop();
        g.fill(left - 1, top - 1, left + 384 + 1, top + 220 + 1, -15658735);
        g.fillGradient(left, top, left + 384, top + 220, -196589496, -198036942);
        g.drawBorder(left, top, 384, 220, -7434610);
        g.fillGradient(left, top, left + 384, top + 18, -10855846, -12632257);
        g.fill(left, top + 18, left + 384, top + 19, this.accent);
        g.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), left + 192, top + 5, -723724);
        int px0 = this.previewX0();
        int py0 = this.previewY0();
        int px1 = this.previewX1();
        int py1 = this.previewY1();
        g.fillGradient(px0, py0, px1, py1, -13421773, -15198184);
        Starfield.draw(g, px0, py0, px1, py1, now, 34, 91L, 0.85f);
        g.drawBorder(px0 - 1, py0 - 1, px1 - px0 + 2, py1 - py0 + 2, -7434610);
        PrizeEntry shown = this.shownEntry();
        List<PokemonFormCatalog.FormEntry> forms = this.formsFor(shown);
        if (!this.dragging) {
            this.hoveredForm = this.formIndexAt(mouseX, mouseY, forms.size());
        }
        EquipmentSlot equipmentSlotTmp = shownArmorSlot = shown != null ? this.armorSlotFor(shown) : null;
        if (shownArmorSlot == null && (shown != null || this.view.isEmpty())) {
            if (shown != null) {
                if (shown.wornSlot() != null) {
                    ItemStack stack = this.stackFor(shown);
                    WardrobeSlot slot = shown.wornSlot();
                    this.wornPreview.updateEquipment(slot == WardrobeSlot.HELMET ? stack : ItemStack.EMPTY, slot == WardrobeSlot.BACKPACK ? stack : ItemStack.EMPTY, slot == WardrobeSlot.BALLOON ? stack : ItemStack.EMPTY);
                    this.wornPreview.render(g, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, -1, partialTick);
                } else if (shown.pokemon()) {
                    List<String> aspects;
                    int activeForm = this.hoveredForm >= 0 ? this.hoveredForm : this.selectedForm;
                    PokemonFormCatalog.FormEntry form = activeForm >= 0 && activeForm < forms.size() ? forms.get(activeForm) : null;
                    String species = form != null ? form.getSpecies() : shown.species();
                    List<String> list = aspects = form != null ? form.getAspects() : shown.aspects();
                    if (this.shiny) {
                        aspects = CratePreviewScreen.withShiny(aspects);
                    }
                    PokemonPreviewRenderer.render(g, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, species, aspects, false, partialTick);
                } else {
                    FurniturePreviewRenderer.renderIcon(g, px0, py0, px1, py1, this.zoom, this.stackFor(shown));
                }
            }
        } else {
            EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
            this.wornArmor.forEach((slotx, e) -> equipment.put((EquipmentSlot)slotx, this.stackFor((PrizeEntry)e)));
            if (shownArmorSlot != null && this.hoverSuppressed < 0) {
                equipment.put(shownArmorSlot, this.stackFor(shown));
            }
            this.armorPreview.render(g, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, equipment);
        }
        if (shown != null) {
            g.drawText(this.textRenderer, shown.paneSummary(), px0, py1 + 4, CratePreviewScreen.oddsArgb(shown.rarity()), false);
        }
        if (!forms.isEmpty()) {
            GuiDepth.clearForOverlay(g);
            for (int i = 0; i < forms.size(); ++i) {
                int[] p = this.formTilePos(i, forms.size());
                boolean sel = i == this.selectedForm;
                g.fill(p[0], p[1], p[0] + 20 - 1, p[1] + 20 - 1, -13421773);
                if (sel) {
                    g.fill(p[0], p[1], p[0] + 20 - 1, p[1] + 20 - 1, 0x40FFFFFF);
                }
                int border = !sel && i != this.hoveredForm ? -7434610 : -1;
                g.drawBorder(p[0], p[1], 19, 19, border);
                g.drawItem(this.formStack(forms.get(i)), p[0] + 2, p[1] + 2);
            }
        }
        this.renderGrid(g, mouseX, mouseY);
        for (Element child : this.children()) {
            if (!(child instanceof Drawable)) continue;
            Drawable renderable = (Drawable)child;
            renderable.render(g, mouseX, mouseY, partialTick);
        }
        if (this.hoveredEntry >= 0 && this.hoveredEntry < this.view.size()) {
            GuiDepth.clearForOverlay(g);
            Tooltips.render(g, this.textRenderer, this.tooltipFor(this.view.get(this.hoveredEntry)), mouseX, mouseY);
        } else if (this.hoveredForm >= 0 && this.hoveredForm < forms.size() && !this.dragging) {
            GuiDepth.clearForOverlay(g);
            Tooltips.render(g, this.textRenderer, this.formLabel(forms.get(this.hoveredForm)), mouseX, mouseY);
        }
    }

    private void renderGrid(DrawContext g, int mouseX, int mouseY) {
        int lineIdx;
        int gx = this.gridX();
        int gy = this.gridY();
        int rows = this.visibleRows();
        for (int r = 0; r < rows && (lineIdx = this.scrollRow + r) < this.gridRows.size(); ++r) {
            GridRow line = this.gridRows.get(lineIdx);
            int ly = gy + r * 20;
            for (int col = 0; col < line.prizes().length; ++col) {
                int border;
                int index = line.prizes()[col];
                int cx = gx + col * 20;
                PrizeEntry e = this.view.get(index);
                boolean sel = index == this.selectedEntry || this.wornArmor.containsValue(e);
                boolean hov = index == this.hoveredEntry;
                g.fill(cx, ly, cx + 20 - 1, ly + 20 - 1, -13421773);
                if (sel) {
                    g.fill(cx, ly, cx + 20 - 1, ly + 20 - 1, 0x40FFFFFF);
                }
                border = sel ? -1 : (hov ? -1 : e.rarity().color());
                if (border != 0) {
                    g.drawBorder(cx, ly, 19, 19, border);
                }
                ItemStack icon = this.stackFor(e);
                g.drawItem(icon, cx + 2, ly + 2);
                g.drawItemInSlot(this.textRenderer, icon, cx + 2, ly + 2);
            }
        }
        ClubScrollbar.draw(g, this.scrollTrackX(), gy, gy + rows * 20, this.scrollRow, this.maxScrollRow(), rows);
    }

    private int hoveredIndex(double mouseX, double mouseY) {
        if (!this.inGrid(mouseX, mouseY)) {
            return -1;
        }
        int lineIdx = this.scrollRow + (int)((mouseY - (double)this.gridY()) / 20.0);
        if (lineIdx >= 0 && lineIdx < this.gridRows.size()) {
            GridRow line = this.gridRows.get(lineIdx);
            int col = (int)((mouseX - (double)this.gridX()) / 20.0);
            return col >= 0 && col < line.prizes().length ? line.prizes()[col] : -1;
        }
        return -1;
    }

    private boolean inPreview(double mouseX, double mouseY) {
        return mouseX >= (double)this.previewX0() && mouseX < (double)this.previewX1() && mouseY >= (double)this.previewY0() && mouseY < (double)this.previewY1();
    }

    private boolean inGrid(double mx, double my) {
        int x = this.gridX();
        int y = this.gridY();
        return mx >= (double)x && mx < (double)(x + 180) && my >= (double)y && my < (double)(y + this.visibleRows() * 20);
    }

    private int scrollTrackX() {
        return this.gridX() + 180 + 2;
    }

    private boolean onScrollbar(double mx, double my) {
        int gy = this.gridY();
        return this.maxScrollRow() > 0 && ClubScrollbar.contains(mx, my, this.scrollTrackX(), gy, gy + this.visibleRows() * 20);
    }

    private void scrollThumbTo(double my) {
        int gy = this.gridY();
        int rows = this.visibleRows();
        this.scrollRow = ClubScrollbar.scrollAt(my, gy, gy + rows * 20, this.maxScrollRow(), rows);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int clicked = this.hoveredIndex(mouseX, mouseY);
        if (clicked < 0) {
            if (button == 0 && this.onScrollbar(mouseX, mouseY)) {
                this.scrollDragging = true;
                this.scrollThumbTo(mouseY);
                return true;
            }
            if (button == 0 && this.inPreview(mouseX, mouseY)) {
                List<PokemonFormCatalog.FormEntry> forms = this.formsFor(this.shownEntry());
                int clickedForm = this.formIndexAt(mouseX, mouseY, forms.size());
                if (clickedForm >= 0) {
                    PreviewUi.playClick();
                    this.selectedForm = clickedForm == this.selectedForm ? -1 : clickedForm;
                }
                this.dragging = true;
                this.lastInteractMs = Util.getMeasuringTimeMs();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        PreviewUi.playClick();
        if (button == 1 && this.canTest) {
            PrizeEntry e = this.view.get(clicked);
            boolean shiny = CratePreviewScreen.hasShiftDown() && e.shinyEligible();
            CratePreviewNetworking.requestTestReward(this.crateId, e.index(), shiny);
        } else {
            PrizeEntry e = this.view.get(clicked);
            EquipmentSlot slot = this.armorSlotFor(e);
            if (slot != null && this.wornArmor.get(slot) == e) {
                this.wornArmor.remove(slot);
                this.selectedEntry = -1;
                this.hoverSuppressed = clicked;
            } else {
                if (slot != null) {
                    this.wornArmor.put(slot, e);
                }
                this.hoverSuppressed = -1;
                this.selectView(clicked);
            }
            this.search.setFocused(false);
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.dragging = false;
            this.scrollDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrollDragging) {
            this.scrollThumbTo(mouseY);
            return true;
        }
        if (this.dragging) {
            this.yaw += (float)dragX;
            this.pitch = MathHelper.clamp((float)(this.pitch - (float)dragY), (float)-40.0f, (float)40.0f);
            this.lastInteractMs = Util.getMeasuringTimeMs();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.inPreview(mouseX, mouseY)) {
            this.zoom = MathHelper.clamp((float)(this.zoom * (scrollY > 0.0 ? 1.12f : 0.89285713f)), (float)0.5f, (float)2.6f);
            this.lastInteractMs = Util.getMeasuringTimeMs();
            return true;
        }
        int max = this.maxScrollRow();
        if (max <= 0 || !this.inGrid(mouseX, mouseY) && !this.onScrollbar(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        this.scrollRow = MathHelper.clamp((int)(this.scrollRow - (int)Math.signum(scrollY)), (int)0, (int)max);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.search != null && this.search.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        int delta = switch (keyCode) {
            case 262 -> 1;
            case 263 -> -1;
            case 264 -> 9;
            case 265 -> -9;
            default -> 0;
        };
        if (delta != 0 && !this.view.isEmpty()) {
            int next;
            int n = next = this.selectedEntry < 0 ? 0 : MathHelper.clamp((int)(this.selectedEntry + delta), (int)0, (int)(this.view.size() - 1));
            if (next != this.selectedEntry) {
                this.selectView(next);
                int rowOf = this.prizeLine[next];
                if (rowOf < this.scrollRow) {
                    this.scrollRow = rowOf;
                } else if (rowOf >= this.scrollRow + this.visibleRows()) {
                    this.scrollRow = rowOf - this.visibleRows() + 1;
                }
                this.scrollRow = MathHelper.clamp((int)this.scrollRow, (int)0, (int)this.maxScrollRow());
                PreviewUi.playClick();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean shouldPause() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    private record Rarity(int rank, int color) {
    }

    @Environment(value=EnvType.CLIENT)
    private record PrizeEntry(Text label, Text shinyLabel, boolean pokemon, String species, List<String> aspects, String material, int customModelData, int shinyCustomModelData, boolean shinyEligible, int amount, int index, Rarity rarity, Text paneSummary, List<Text> tooltip, List<Text> shinyTooltip, WardrobeSlot wornSlot, List<String> evolutions) {
    }

    @Environment(value=EnvType.CLIENT)
    private record GridRow(int[] prizes) {
    }
}

