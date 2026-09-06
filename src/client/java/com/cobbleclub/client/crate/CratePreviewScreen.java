package com.cobbleclub.client.crate;

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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class CratePreviewScreen extends Screen {
   private static final int PANEL_W = 384;
   private static final int PANEL_H = 252;
   private static final int GRID_COLS = 9;
   private static final int CELL = 20;
   private static final int FORM_COLS = 7;
   private static final int MAX_ROWS = 9;
   private static final int SEARCH_CAP = 108;
   private static final int PANEL_TOP = -199614136;
   private static final int PANEL_BOTTOM = -200601562;
   private static final int PANEL_BORDER_OUT = -16447985;
   private static final int PANEL_BORDER_IN = -13747610;
   private static final int HEADER_TOP = -14405538;
   private static final int HEADER_BOTTOM = -15459782;
   private static final int DEFAULT_ACCENT = -6467875;
   private static final int TITLE_COLOR = -2053377;
   private static final int SELECTED = -1;
   private static final int TIER_GREEN = -10688932;
   private static final int TIER_BLUE = -11094273;
   private static final int TIER_RED = -893860;
   private static final int NO_HIGHLIGHT = 0;
   private static final int ODDS_NEUTRAL = -3554586;
   private static final int SLOT_BASE = -15064506;
   private static final int PREVIEW_TOP = -14998448;
   private static final int PREVIEW_BOTTOM = -16315880;
   private static final float MIN_ZOOM = 0.5F;
   private static final float MAX_ZOOM = 2.6F;
   private static final float ZOOM_STEP = 1.12F;
   private static final float MAX_PITCH = 40.0F;
   private static final float AUTO_SPIN_DEG_PER_MS = 0.03F;
   private static final long AUTO_SPIN_IDLE_MS = 1500L;
   private final List<PrizeEntry> allPrizes = new ArrayList();
   private final List<GridRow> gridRows = new ArrayList();
   private int[] prizeLine = new int[0];
   private final Map<Integer, ItemStack> stackCache = new HashMap();
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
   private final Map<String, ItemStack> formStackCache = new HashMap();
   private final Map<EquipmentSlot, PrizeEntry> wornArmor = new EnumMap(EquipmentSlot.class);
   private int hoverSuppressed = -1;
   private List<PrizeEntry> view = new ArrayList();
   private float yaw;
   private float pitch;
   private float zoom = 1.0F;
   private boolean dragging;
   private boolean scrollDragging;
   private long lastInteractMs;
   private long lastFrameMs = Util.getMeasuringTimeMs();

   public CratePreviewScreen(CrateCatalog catalog) {
      super(PreviewUi.deserialize(catalog.title, "Crate"));
      this.accent = parseAccent(catalog.gradient);
      this.crateId = catalog.id;
      this.canTest = catalog.canTest;
      this.hasShinyToggle = catalog.shinyPreview;
      if (catalog.prizes != null) {
         Map<Double, Rarity> scheme = buildScheme(catalog.prizes);

         for(int i = 0; i < catalog.prizes.size(); ++i) {
            CrateCatalog.Prize p = (CrateCatalog.Prize)catalog.prizes.get(i);
            if (p != null) {
               boolean pokemon = p.species != null && !p.species.isBlank();
               Text label = PreviewUi.deserialize(p.displayName, pokemon ? p.species : p.material);
               Text shinyLabel = p.shinyDisplayName != null ? PreviewUi.deserialize(p.shinyDisplayName, "") : null;
               Rarity r = (Rarity)scheme.get(p.chance);
               Text paneSummary = Text.literal("Chance: " + oddsStr(p.chance));
               List<Text> tooltip = new ArrayList();
               tooltip.add(label);
               if (p.lore != null) {
                  for(String line : p.lore) {
                     tooltip.add(PreviewUi.deserialize(line, ""));
                  }
               }

               if (this.canTest) {
                  tooltip.add(Text.literal("Right-click to test reward").withColor(-3116801));
                  if (p.shinyEligible) {
                     tooltip.add(Text.literal("Shift+right-click to test shiny reward").withColor(-11702));
                  }
               }

               List<Text> shinyTooltip = null;
               if (shinyLabel != null) {
                  shinyTooltip = new ArrayList(tooltip);
                  shinyTooltip.set(0, shinyLabel);
               }

               this.allPrizes.add(new PrizeEntry(label, shinyLabel, pokemon, p.species, p.aspects != null ? p.aspects : List.of(), p.material, p.customModelData, p.shinyCustomModelData, p.shinyEligible, p.amount, i, r, paneSummary, tooltip, shinyTooltip, parseWornSlot(p.wornSlot), p.evolutions));
            }
         }
      }

      this.allPrizes.sort(Comparator.comparingInt((e) -> e.rarity().rank()));
      PreviewUi.playOpen();
   }

   public void close() {
      PreviewUi.playClose();
      this.wornPreview.close();
      this.armorPreview.close();
      super.close();
   }

   private static int parseAccent(String gradient) {
      if (gradient != null) {
         int hash = gradient.indexOf(35);
         if (hash >= 0 && hash + 7 <= gradient.length()) {
            try {
               return -16777216 | Integer.parseInt(gradient.substring(hash + 1, hash + 7), 16);
            } catch (NumberFormatException var3) {
            }
         }
      }

      return -6467875;
   }

   private static String oddsStr(double chance) {
      if (chance <= (double)0.0F) {
         return "1 in ?";
      } else {
         double odds = (double)100.0F / chance;
         double rounded = odds >= (double)100.0F ? (double)Math.round(odds) : (double)Math.round(odds * (double)10.0F) / (double)10.0F;
         String var10000 = rounded % (double)1.0F == (double)0.0F ? String.valueOf((long)rounded) : String.valueOf(rounded);
         return "1 in " + var10000;
      }
   }

   private static Map<Double, Rarity> buildScheme(List<CrateCatalog.Prize> prizes) {
      List<Double> chances = new ArrayList();

      for(CrateCatalog.Prize p : prizes) {
         if (p != null) {
            chances.add(p.chance);
         }
      }

      List<Double> distinct = chances.stream().distinct().sorted(Comparator.reverseOrder()).toList();
      Map<Double, Rarity> tiers = new HashMap();
      if (distinct.size() <= 1) {
         for(Double c : distinct) {
            tiers.put(c, new Rarity(0, 0));
         }
      } else if (distinct.size() == 3) {
         int[] colors = new int[]{-10688932, -11094273, -893860};

         for(int i = 0; i < 3; ++i) {
            tiers.put((Double)distinct.get(i), new Rarity(2 - i, colors[i]));
         }
      } else {
         for(Double c : distinct) {
            tiers.put(c, rarityOf(c));
         }
      }

      return tiers;
   }

   private static Rarity rarityOf(double chance) {
      if (chance >= (double)20.0F) {
         return new Rarity(4, -5197648);
      } else if (chance >= (double)5.0F) {
         return new Rarity(3, -10688932);
      } else if (chance >= (double)1.0F) {
         return new Rarity(2, -11094273);
      } else {
         return chance >= 0.1 ? new Rarity(1, -4169473) : new Rarity(0, -16336);
      }
   }

   private static int oddsArgb(Rarity r) {
      return r.color() == 0 ? -3554586 : r.color();
   }

   private Text shinyLabel() {
      return Text.literal(this.shiny ? "Shiny: On" : "Shiny: Off");
   }

   private static WardrobeSlot parseWornSlot(String slot) {
      if (slot == null) {
         return null;
      } else {
         WardrobeSlot var10000;
         switch (slot.toLowerCase(Locale.ROOT)) {
            case "helmet":
            case "hat":
            case "head":
               var10000 = WardrobeSlot.HELMET;
               break;
            case "wings":
            case "backpack":
               var10000 = WardrobeSlot.BACKPACK;
               break;
            case "balloon":
               var10000 = WardrobeSlot.BALLOON;
               break;
            default:
               var10000 = null;
         }

         return var10000;
      }
   }

   private void rebuildView() {
      int keep = this.selectedEntry >= 0 && this.selectedEntry < this.view.size() ? ((PrizeEntry)this.view.get(this.selectedEntry)).index() : -1;
      this.view = this.computeView();
      this.buildGridRows();
      this.selectedEntry = this.view.isEmpty() ? -1 : 0;

      for(int i = 0; i < this.view.size(); ++i) {
         if (((PrizeEntry)this.view.get(i)).index() == keep) {
            this.selectedEntry = i;
            break;
         }
      }

      this.scrollRow = MathHelper.clamp(this.scrollRow, 0, this.maxScrollRow());
      if (this.selectedEntry >= 0) {
         int rowOf = this.prizeLine[this.selectedEntry];
         if (rowOf < this.scrollRow) {
            this.scrollRow = rowOf;
         } else if (rowOf >= this.scrollRow + this.visibleRows()) {
            this.scrollRow = MathHelper.clamp(rowOf - this.visibleRows() + 1, 0, this.maxScrollRow());
         }
      }

      this.hoverSuppressed = -1;
   }

   private void buildGridRows() {
      this.gridRows.clear();
      this.prizeLine = new int[this.view.size()];
      int i = 0;
      boolean firstTier = true;

      while(i < this.view.size()) {
         int rank = ((PrizeEntry)this.view.get(i)).rarity().rank();

         List<Integer> group;
         for(group = new ArrayList(); i < this.view.size() && ((PrizeEntry)this.view.get(i)).rarity().rank() == rank; ++i) {
            group.add(i);
         }

         if (!firstTier) {
            this.gridRows.add(new GridRow(new int[0]));
         }

         firstTier = false;

         for(int j = 0; j < group.size(); j += 9) {
            int n = Math.min(9, group.size() - j);
            int[] rowPrizes = new int[n];

            for(int k = 0; k < n; ++k) {
               rowPrizes[k] = (Integer)group.get(j + k);
               this.prizeLine[(Integer)group.get(j + k)] = this.gridRows.size();
            }

            this.gridRows.add(new GridRow(rowPrizes));
         }
      }

   }

   private List<PrizeEntry> computeView() {
      String q = this.search != null ? this.search.getText().trim().toLowerCase() : "";
      if (q.isEmpty()) {
         return this.allPrizes;
      } else {
         List<PrizeEntry> out = new ArrayList();

         for(PrizeEntry e : this.allPrizes) {
            if (this.labelFor(e).getString().toLowerCase().contains(q)) {
               out.add(e);
               if (out.size() >= 108) {
                  break;
               }
            }
         }

         return out;
      }
   }

   private ItemStack stackFor(PrizeEntry e) {
      return (ItemStack)this.stackCache.computeIfAbsent(e.index(), (k) -> {
         ItemStack stack;
         if (e.pokemon()) {
            stack = PokemonPreviewRenderer.modelStack(e.species(), this.shiny ? withShiny(e.aspects()) : e.aspects());
         } else {
            int cmd = this.shiny && e.shinyCustomModelData() > 0 ? e.shinyCustomModelData() : e.customModelData();
            stack = FurniturePreviewRenderer.stackFor(e.material(), cmd, (Integer)null);
         }

         if (!stack.isEmpty()) {
            stack.setCount(MathHelper.clamp(e.amount(), 1, 99));
         }

         return stack;
      });
   }

   private static List<String> withShiny(List<String> aspects) {
      if (aspects.contains("shiny")) {
         return aspects;
      } else {
         List<String> out = new ArrayList(aspects);
         out.add("shiny");
         return out;
      }
   }

   private Text labelFor(PrizeEntry e) {
      return this.shiny && e.shinyLabel() != null ? e.shinyLabel() : e.label();
   }

   private List<Text> tooltipFor(PrizeEntry e) {
      return this.shiny && e.shinyTooltip() != null ? e.shinyTooltip() : e.tooltip();
   }

   private PrizeEntry shownEntry() {
      int idx = this.hoveredEntry >= 0 ? this.hoveredEntry : this.selectedEntry;
      return idx >= 0 && idx < this.view.size() ? (PrizeEntry)this.view.get(idx) : null;
   }

   private List<PokemonFormCatalog.FormEntry> formsFor(PrizeEntry e) {
      if (e != null && e.pokemon()) {
         if (e.index() != this.shownFormsFor) {
            this.shownForms = PokemonFormCatalog.prizeFormButtons(e.species(), e.aspects(), e.evolutions());
            this.shownFormsFor = e.index();
            this.selectedForm = -1;
         }

         return this.shownForms;
      } else {
         return List.of();
      }
   }

   private ItemStack formStack(PokemonFormCatalog.FormEntry f) {
      String var10000 = f.getSpecies();
      String key = var10000 + "|" + String.join(",", f.getAspects()) + (this.shiny ? "|shiny" : "");
      return (ItemStack)this.formStackCache.computeIfAbsent(key, (k) -> PokemonPreviewRenderer.modelStack(f.getSpecies(), this.shiny ? withShiny(f.getAspects()) : f.getAspects()));
   }

   private Text formLabel(PokemonFormCatalog.FormEntry f) {
      return Text.literal(this.shiny ? "Shiny " + f.getLabel() : f.getLabel());
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
      for(int i = 0; i < count; ++i) {
         int[] p = this.formTilePos(i, count);
         if (mouseX >= (double)p[0] && mouseX < (double)(p[0] + 20 - 1) && mouseY >= (double)p[1] && mouseY < (double)(p[1] + 20 - 1)) {
            return i;
         }
      }

      return -1;
   }

   private EquipmentSlot armorSlotFor(PrizeEntry e) {
      if (!e.pokemon() && e.wornSlot() == null) {
         Equipment equipable = Equipment.fromStack(this.stackFor(e));
         return equipable != null && equipable.getSlotType().getType() == Type.HUMANOID_ARMOR ? equipable.getSlotType() : null;
      } else {
         return null;
      }
   }

   private void selectView(int idx) {
      this.selectedEntry = idx;
      this.yaw = 0.0F;
      this.pitch = 0.0F;
      this.lastInteractMs = Util.getMeasuringTimeMs();
   }

   private int panelLeft() {
      return (this.width - 384) / 2;
   }

   private int panelTop() {
      return (this.height - 252) / 2;
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
      return this.panelTop() + 252 - 38;
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
      int fit = (this.panelTop() + 252 - 26 - this.gridY()) / 20;
      return MathHelper.clamp(this.gridRows.size(), 1, Math.max(1, Math.min(9, fit)));
   }

   private int maxScrollRow() {
      return Math.max(0, this.gridRows.size() - this.visibleRows());
   }

   protected void init() {
      this.lastFrameMs = Util.getMeasuringTimeMs();
      this.search = new TextFieldWidget(this.textRenderer, this.gridX(), this.searchY(), 180, 12, Text.literal("Search"));
      this.search.setPlaceholder(Text.literal("Search prizes…"));
      this.search.setChangedListener((s) -> this.rebuildView());
      this.search.setDrawsBackground(true);
      this.rebuildView();
      int fitWithoutSearch = (this.panelTop() + 252 - 26 - this.searchY()) / 20;
      this.searchVisible = this.gridRows.size() > Math.min(9, fitWithoutSearch);
      if (this.searchVisible) {
         this.addDrawableChild(this.search);
      }

      int footerY = this.panelTop() + 252 - 24;
      if (this.hasShinyToggle) {
         this.addDrawableChild(new ThemedButton(this.panelLeft() + 384 - 140, footerY, 74, 20, this.shinyLabel(), (b) -> {
            this.shiny = !this.shiny;
            this.stackCache.clear();
            this.rebuildView();
            b.setMessage(this.shinyLabel());
         }));
      }

      this.addDrawableChild(new ThemedButton(this.panelLeft() + 384 - 60, footerY, 52, 20, Text.literal("Close"), (b) -> this.close()));
   }

   public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
      long now = Util.getMeasuringTimeMs();
      long dt = now - this.lastFrameMs;
      this.lastFrameMs = now;
      if (!this.dragging && now - this.lastInteractMs > 1500L) {
         this.yaw += 0.03F * (float)dt;
      }

      this.hoveredEntry = !this.dragging && !this.scrollDragging ? this.hoveredIndex((double)mouseX, (double)mouseY) : -1;
      if (this.hoverSuppressed >= 0 && this.hoveredEntry != this.hoverSuppressed) {
         this.hoverSuppressed = -1;
      }

      this.renderBackground(g, mouseX, mouseY, partialTick);
      int left = this.panelLeft();
      int top = this.panelTop();
      g.fill(left - 1, top - 1, left + 384 + 1, top + 252 + 1, -16447985);
      g.fillGradient(left, top, left + 384, top + 252, -199614136, -200601562);
      g.drawBorder(left, top, 384, 252, -13747610);
      g.fillGradient(left, top, left + 384, top + 18, -14405538, -15459782);
      g.fill(left, top + 18, left + 384, top + 19, this.accent);
      g.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), left + 192, top + 5, -2053377);
      int px0 = this.previewX0();
      int py0 = this.previewY0();
      int px1 = this.previewX1();
      int py1 = this.previewY1();
      g.fillGradient(px0, py0, px1, py1, -14998448, -16315880);
      Starfield.draw(g, px0, py0, px1, py1, now, 34, 91L, 0.85F);
      g.drawBorder(px0 - 1, py0 - 1, px1 - px0 + 2, py1 - py0 + 2, -13747610);
      PrizeEntry shown = this.shownEntry();
      List<PokemonFormCatalog.FormEntry> forms = this.formsFor(shown);
      if (!this.dragging) {
         this.hoveredForm = this.formIndexAt((double)mouseX, (double)mouseY, forms.size());
      }

      EquipmentSlot shownArmorSlot = shown != null ? this.armorSlotFor(shown) : null;
      if (shownArmorSlot == null && (shown != null || this.view.isEmpty())) {
         if (shown != null) {
            if (shown.wornSlot() != null) {
               ItemStack stack = this.stackFor(shown);
               WardrobeSlot slot = shown.wornSlot();
               this.wornPreview.updateEquipment(slot == WardrobeSlot.HELMET ? stack : ItemStack.EMPTY, slot == WardrobeSlot.BACKPACK ? stack : ItemStack.EMPTY, slot == WardrobeSlot.BALLOON ? stack : ItemStack.EMPTY);
               this.wornPreview.render(g, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, -1, partialTick);
            } else if (shown.pokemon()) {
               int activeForm = this.hoveredForm >= 0 ? this.hoveredForm : this.selectedForm;
               PokemonFormCatalog.FormEntry form = activeForm >= 0 && activeForm < forms.size() ? (PokemonFormCatalog.FormEntry)forms.get(activeForm) : null;
               String species = form != null ? form.getSpecies() : shown.species();
               List<String> aspects = form != null ? form.getAspects() : shown.aspects();
               if (this.shiny) {
                  aspects = withShiny(aspects);
               }

               PokemonPreviewRenderer.render(g, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, species, aspects, false, partialTick);
            } else {
               FurniturePreviewRenderer.renderIcon(g, px0, py0, px1, py1, this.zoom, this.stackFor(shown));
            }
         }
      } else {
         Map<EquipmentSlot, ItemStack> equipment = new EnumMap(EquipmentSlot.class);
         this.wornArmor.forEach((slotx, e) -> equipment.put(slotx, this.stackFor(e)));
         if (shownArmorSlot != null && this.hoverSuppressed < 0) {
            equipment.put(shownArmorSlot, this.stackFor(shown));
         }

         this.armorPreview.render(g, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, equipment);
      }

      if (shown != null) {
         g.drawText(this.textRenderer, shown.paneSummary(), px0, py1 + 4, oddsArgb(shown.rarity()), false);
      }

      if (!forms.isEmpty()) {
         GuiDepth.clearForOverlay(g);

         for(int i = 0; i < forms.size(); ++i) {
            int[] p = this.formTilePos(i, forms.size());
            boolean sel = i == this.selectedForm;
            g.fill(p[0], p[1], p[0] + 20 - 1, p[1] + 20 - 1, -15064506);
            if (sel) {
               g.fill(p[0], p[1], p[0] + 20 - 1, p[1] + 20 - 1, 1090519039);
            }

            int border = !sel && i != this.hoveredForm ? -13747610 : -1;
            g.drawBorder(p[0], p[1], 19, 19, border);
            g.drawItem(this.formStack((PokemonFormCatalog.FormEntry)forms.get(i)), p[0] + 2, p[1] + 2);
         }
      }

      this.renderGrid(g, mouseX, mouseY);

      for(Element child : this.children()) {
         if (child instanceof Drawable renderable) {
            renderable.render(g, mouseX, mouseY, partialTick);
         }
      }

      if (this.hoveredEntry >= 0 && this.hoveredEntry < this.view.size()) {
         GuiDepth.clearForOverlay(g);
         Tooltips.render(g, this.textRenderer, this.tooltipFor((PrizeEntry)this.view.get(this.hoveredEntry)), mouseX, mouseY);
      } else if (this.hoveredForm >= 0 && this.hoveredForm < forms.size() && !this.dragging) {
         GuiDepth.clearForOverlay(g);
         Tooltips.render(g, this.textRenderer, this.formLabel((PokemonFormCatalog.FormEntry)forms.get(this.hoveredForm)), mouseX, mouseY);
      }

   }

   private void renderGrid(DrawContext g, int mouseX, int mouseY) {
      int gx = this.gridX();
      int gy = this.gridY();
      int rows = this.visibleRows();

      for(int r = 0; r < rows; ++r) {
         int lineIdx = this.scrollRow + r;
         if (lineIdx >= this.gridRows.size()) {
            break;
         }

         GridRow line = (GridRow)this.gridRows.get(lineIdx);
         int ly = gy + r * 20;

         for(int col = 0; col < line.prizes().length; ++col) {
            int index = line.prizes()[col];
            int cx = gx + col * 20;
            PrizeEntry e = (PrizeEntry)this.view.get(index);
            boolean sel = index == this.selectedEntry || this.wornArmor.containsValue(e);
            boolean hov = index == this.hoveredEntry;
            g.fill(cx, ly, cx + 20 - 1, ly + 20 - 1, -15064506);
            if (sel) {
               g.fill(cx, ly, cx + 20 - 1, ly + 20 - 1, 1090519039);
            }

            int border = sel ? -1 : (hov ? -1 : e.rarity().color());
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
      } else {
         int lineIdx = this.scrollRow + (int)((mouseY - (double)this.gridY()) / (double)20.0F);
         if (lineIdx >= 0 && lineIdx < this.gridRows.size()) {
            GridRow line = (GridRow)this.gridRows.get(lineIdx);
            int col = (int)((mouseX - (double)this.gridX()) / (double)20.0F);
            return col >= 0 && col < line.prizes().length ? line.prizes()[col] : -1;
         } else {
            return -1;
         }
      }
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
         } else if (button == 0 && this.inPreview(mouseX, mouseY)) {
            List<PokemonFormCatalog.FormEntry> forms = this.formsFor(this.shownEntry());
            int clickedForm = this.formIndexAt(mouseX, mouseY, forms.size());
            if (clickedForm >= 0) {
               PreviewUi.playClick();
               this.selectedForm = clickedForm == this.selectedForm ? -1 : clickedForm;
            }

            this.dragging = true;
            this.lastInteractMs = Util.getMeasuringTimeMs();
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      } else {
         PreviewUi.playClick();
         if (button == 1 && this.canTest) {
            PrizeEntry e = (PrizeEntry)this.view.get(clicked);
            boolean shiny = hasShiftDown() && e.shinyEligible();
            CratePreviewNetworking.requestTestReward(this.crateId, e.index(), shiny);
         } else {
            PrizeEntry e = (PrizeEntry)this.view.get(clicked);
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
      } else if (this.dragging) {
         this.yaw += (float)dragX;
         this.pitch = MathHelper.clamp(this.pitch - (float)dragY, -40.0F, 40.0F);
         this.lastInteractMs = Util.getMeasuringTimeMs();
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.inPreview(mouseX, mouseY)) {
         this.zoom = MathHelper.clamp(this.zoom * (scrollY > (double)0.0F ? 1.12F : 0.89285713F), 0.5F, 2.6F);
         this.lastInteractMs = Util.getMeasuringTimeMs();
         return true;
      } else {
         int max = this.maxScrollRow();
         if (max <= 0 || !this.inGrid(mouseX, mouseY) && !this.onScrollbar(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
         } else {
            this.scrollRow = MathHelper.clamp(this.scrollRow - (int)Math.signum(scrollY), 0, max);
            return true;
         }
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.search != null && this.search.isFocused()) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
         this.close();
         return true;
      } else {
         byte var10000;
         switch (keyCode) {
            case 262 -> var10000 = 1;
            case 263 -> var10000 = -1;
            case 264 -> var10000 = 9;
            case 265 -> var10000 = -9;
            default -> var10000 = 0;
         }

         int delta = var10000;
         if (delta != 0 && !this.view.isEmpty()) {
            int next = this.selectedEntry < 0 ? 0 : MathHelper.clamp(this.selectedEntry + delta, 0, this.view.size() - 1);
            if (next != this.selectedEntry) {
               this.selectView(next);
               int rowOf = this.prizeLine[next];
               if (rowOf < this.scrollRow) {
                  this.scrollRow = rowOf;
               } else if (rowOf >= this.scrollRow + this.visibleRows()) {
                  this.scrollRow = rowOf - this.visibleRows() + 1;
               }

               this.scrollRow = MathHelper.clamp(this.scrollRow, 0, this.maxScrollRow());
               PreviewUi.playClick();
            }

            return true;
         } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   private static record PrizeEntry(Text label, Text shinyLabel, boolean pokemon, String species, List<String> aspects, String material, int customModelData, int shinyCustomModelData, boolean shinyEligible, int amount, int index, Rarity rarity, Text paneSummary, List<Text> tooltip, List<Text> shinyTooltip, WardrobeSlot wornSlot, List<String> evolutions) {
   }

   @Environment(EnvType.CLIENT)
   private static record Rarity(int rank, int color) {
   }

   @Environment(EnvType.CLIENT)
   private static record GridRow(int[] prizes) {
   }
}
