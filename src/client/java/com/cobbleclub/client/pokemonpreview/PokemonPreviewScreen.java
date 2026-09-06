package com.cobbleclub.client.pokemonpreview;

import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.client.ui.Tooltips;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class PokemonPreviewScreen extends Screen {
   private static final int PANEL_W = 508;
   private static final int PANEL_H = 252;
   private static final int GRID_COLS = 9;
   private static final int CELL = 20;
   private static final int MAX_ROWS = 9;
   private static final int SIDEBAR_W = 104;
   private static final int CAT_ROW_H = 15;
   private static final int SEARCH_CAP = 108;
   private static final int PANEL_TOP = -199614136;
   private static final int PANEL_BOTTOM = -200601562;
   private static final int PANEL_BORDER_OUT = -16447985;
   private static final int PANEL_BORDER_IN = -13747610;
   private static final int HEADER_TOP = -14405538;
   private static final int HEADER_BOTTOM = -15459782;
   private static final int ACCENT = -6467875;
   private static final int TITLE_COLOR = -2053377;
   private static final int MUTED_TEXT = -7035976;
   private static final int SELECTED = -12474273;
   private static final int STAR = -10934;
   private static final int CONTENT_BG = -15723477;
   private static final int SLOT_BASE = -15064506;
   private static final int PREVIEW_TOP = -14998448;
   private static final int PREVIEW_BOTTOM = -16315880;
   private static final int SCROLL_TRACK = -15986650;
   private static final int SCROLL_THUMB = -10862962;
   private static final float MIN_ZOOM = 0.5F;
   private static final float MAX_ZOOM = 2.6F;
   private static final float ZOOM_STEP = 1.12F;
   private static final float MAX_PITCH = 40.0F;
   private static final float AUTO_SPIN_DEG_PER_MS = 0.03F;
   private static final long AUTO_SPIN_IDLE_MS = 1500L;
   private final List<SkinEntry> allEntries = new ArrayList();
   private final List<TabDef> tabs = new ArrayList();
   private final Map<String, ItemStack> stackCache = new HashMap();
   private Map<String, Integer> sortKeys = Map.of();
   private Set<String> idleFly = Set.of();
   private TextFieldWidget search;
   private int activeTab;
   private int scrollRow;
   private int categoryScroll;
   private int selectedEntry = -1;
   private int hoveredEntry = -1;
   private List<SkinEntry> view = new ArrayList();
   private float yaw;
   private float pitch;
   private float zoom = 1.0F;
   private boolean dragging;
   private String lastShownKey;
   private boolean shiny;
   private int sortMode;
   private long lastInteractMs;
   private long lastFrameMs = Util.getMeasuringTimeMs();
   private static final int SORT_W = 42;

   public PokemonPreviewScreen(PokemonSkinCatalog catalog) {
      super(Text.literal("Pokémon Skins"));
      this.buildCatalog(catalog);
      PreviewUi.playOpen();
   }

   public void close() {
      PreviewUi.playClose();
      super.close();
   }

   private void buildCatalog(PokemonSkinCatalog catalog) {
      PokemonFormCatalog.configure(catalog.formAspects, catalog.formButtons, catalog.scopedForms);
      if (catalog.sortKeys != null) {
         this.sortKeys = catalog.sortKeys;
      }

      if (catalog.idleFly != null) {
         this.idleFly = new HashSet(catalog.idleFly);
      }

      Set<String> crateAspects = new HashSet();
      Comparator<SkinEntry> listOrder = Comparator.comparingInt(this::listSortKey).thenComparingInt((ex) -> ex.aspects().size()).thenComparing((ex) -> ex.label().getString());
      if (catalog.families != null) {
         for(PokemonSkinCatalog.Family f : catalog.families) {
            if (f != null && f.entries != null) {
               String famName = PreviewUi.deserialize(f.shortName != null ? f.shortName : f.displayName, f.id).getString().trim();
               String skinAspect = f.skinAspect != null ? f.skinAspect : dominantAspect(f.entries);
               List<SkinEntry> entries = new ArrayList();
               if (skinAspect != null) {
                  crateAspects.add(skinAspect);
                  Set<String> prizeSpecies = new HashSet();

                  for(PokemonSkinCatalog.Entry e : f.entries) {
                     if (e != null && e.species != null) {
                        int ns = e.species.indexOf(58);
                        prizeSpecies.add(ns >= 0 ? e.species.substring(ns + 1) : e.species);
                     }
                  }

                  try {
                     for(PokemonFormCatalog.FormEntry fe : PokemonFormCatalog.skinFamilyEntries(skinAspect, prizeSpecies)) {
                        entries.add(new SkinEntry(fe.getSpecies(), fe.getAspects(), Text.literal(fe.getLabel()), famName));
                     }
                  } catch (Exception var14) {
                  }
               }

               Set<String> present = new HashSet();

               for(SkinEntry e : entries) {
                  present.add(aspectSetKey(e.species(), e.aspects()));
               }

               for(PokemonSkinCatalog.Entry e : f.entries) {
                  if (e != null && e.species != null) {
                     List<String> aspects = e.aspects != null ? e.aspects : List.of();
                     crateAspects.addAll(aspects);
                     if (present.add(aspectSetKey(e.species, aspects))) {
                        entries.add(new SkinEntry(e.species, aspects, PreviewUi.deserialize(e.displayName, e.species), famName));
                     }
                  }
               }

               entries.sort(listOrder);
               Text styled = PreviewUi.deserialize(f.shortName != null ? f.shortName : f.displayName, famName);
               this.tabs.add(new TabDef(famName, styled, entries, true));
               this.allEntries.addAll(entries);
            }
         }
      }

      try {
         for(PokemonFormCatalog.FormFamily ff : PokemonFormCatalog.build(crateAspects)) {
            List<SkinEntry> entries = new ArrayList();

            for(PokemonFormCatalog.FormEntry fe : ff.getEntries()) {
               entries.add(new SkinEntry(fe.getSpecies(), fe.getAspects(), Text.literal(fe.getLabel()), ff.getLabel()));
            }

            entries.sort(listOrder);
            this.tabs.add(new TabDef(ff.getLabel(), (Text)null, entries, false));
            this.allEntries.addAll(entries);
         }
      } catch (Exception var13) {
      }

      this.activeTab = 0;
   }

   private static String aspectSetKey(String species, List<String> aspects) {
      return species + "|" + (String)aspects.stream().sorted().collect(Collectors.joining(","));
   }

   private int listSortKey(SkinEntry e) {
      String path = e.species().contains(":") ? e.species().substring(e.species().indexOf(58) + 1) : e.species();
      Integer key = (Integer)this.sortKeys.get(path);
      if (key != null) {
         return key;
      } else {
         int dex = PokemonPreviewRenderer.dexNumber(e.species());
         return 10000000 + Math.min(dex, 99999) * 100;
      }
   }

   private static String dominantAspect(List<PokemonSkinCatalog.Entry> prizes) {
      Map<String, Integer> counts = new HashMap();

      for(PokemonSkinCatalog.Entry e : prizes) {
         if (e != null && e.aspects != null) {
            for(String a : e.aspects) {
               if (!"normal".equals(a) && !"shiny".equals(a) && !PokemonFormCatalog.isAlternateBase(a)) {
                  counts.merge(a, 1, Integer::sum);
               }
            }
         }
      }

      String best = null;
      int bestN = 0;

      for(Map.Entry<String, Integer> en : counts.entrySet()) {
         if ((Integer)en.getValue() > bestN) {
            bestN = (Integer)en.getValue();
            best = (String)en.getKey();
         }
      }

      return best;
   }

   private static String modelKey(SkinEntry e) {
      String var10000 = e.species();
      return var10000 + "|" + String.join(",", e.aspects());
   }

   private void rebuildView() {
      this.view = this.computeView();
      this.scrollRow = 0;
      this.selectedEntry = this.view.isEmpty() ? -1 : 0;
   }

   private List<SkinEntry> computeView() {
      String q = this.search != null ? this.search.getText().trim().toLowerCase() : "";
      if (q.isEmpty()) {
         return this.sorted(((TabDef)this.tabs.get(this.activeTab)).entries());
      } else {
         List<SkinEntry> out = new ArrayList();

         for(SkinEntry e : this.allEntries) {
            if (e.label().getString().toLowerCase().contains(q) || e.species().toLowerCase().contains(q)) {
               out.add(e);
               if (out.size() >= 108) {
                  break;
               }
            }
         }

         return this.sorted(out);
      }
   }

   private List<SkinEntry> sorted(List<SkinEntry> base) {
      if (this.sortMode == 0) {
         return base;
      } else {
         List<SkinEntry> out = new ArrayList(base);
         if (this.sortMode == 1) {
            out.sort(Comparator.comparing((e) -> e.label().getString().toLowerCase()));
         } else {
            out.sort(Comparator.comparingInt((e) -> PokemonPreviewRenderer.dexNumber(e.species())));
         }

         return out;
      }
   }

   private ItemStack stackFor(SkinEntry e) {
      String var10000 = this.shiny ? "s|" : "";
      String key = var10000 + modelKey(e);
      return (ItemStack)this.stackCache.computeIfAbsent(key, (k) -> PokemonPreviewRenderer.modelStack(e.species(), this.withShiny(e.aspects())));
   }

   private List<String> withShiny(List<String> aspects) {
      if (this.shiny && !aspects.contains("shiny")) {
         List<String> out = new ArrayList(aspects);
         out.add("shiny");
         return out;
      } else {
         return aspects;
      }
   }

   private String sortLabel() {
      String var10000;
      switch (this.sortMode) {
         case 1 -> var10000 = "A-Z";
         case 2 -> var10000 = "Dex";
         default -> var10000 = "List";
      }

      return var10000;
   }

   private int sidebarX() {
      return this.panelLeft() + 204;
   }

   private int visibleCatRows() {
      return (this.previewY1() - this.previewY0()) / 15;
   }

   private int maxCatScroll() {
      return Math.max(0, this.tabs.size() - this.visibleCatRows());
   }

   private int panelLeft() {
      return (this.width - 508) / 2;
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
      return this.panelLeft() + 198;
   }

   private int previewY1() {
      return this.panelTop() + 252 - 28;
   }

   private int sortX() {
      return this.gridX() + 180 - 42;
   }

   private int gridX() {
      return this.panelLeft() + 314;
   }

   private int searchY() {
      return this.panelTop() + 24;
   }

   private int gridY() {
      return this.searchY() + 16;
   }

   private int visibleRows() {
      int rows = (this.view.size() + 9 - 1) / 9;
      int fit = (this.panelTop() + 252 - 28 - this.gridY()) / 20;
      return MathHelper.clamp(rows, 1, Math.max(1, Math.min(9, fit)));
   }

   private int maxScrollRow() {
      int rows = (this.view.size() + 9 - 1) / 9;
      return Math.max(0, rows - this.visibleRows());
   }

   protected void init() {
      this.lastFrameMs = Util.getMeasuringTimeMs();
      this.search = new TextFieldWidget(this.textRenderer, this.gridX(), this.searchY(), 134, 12, Text.literal("Search"));
      this.search.setPlaceholder(Text.literal("Search skins…"));
      this.search.setChangedListener((s) -> this.rebuildView());
      this.search.setDrawsBackground(true);
      this.addDrawableChild(this.search);
      this.addDrawableChild(new ThemedButton(this.panelLeft() + 508 - 118, this.panelTop() + 252 - 24, 52, 20, Text.literal("Reset"), (b) -> {
         this.yaw = 0.0F;
         this.pitch = 0.0F;
         this.zoom = 1.0F;
      }));
      this.addDrawableChild(new ThemedButton(this.panelLeft() + 508 - 60, this.panelTop() + 252 - 24, 52, 20, Text.literal("Close"), (b) -> this.close()));
      this.rebuildView();
   }

   private SkinEntry shownEntry() {
      int idx = this.hoveredEntry >= 0 ? this.hoveredEntry : this.selectedEntry;
      return idx >= 0 && idx < this.view.size() ? (SkinEntry)this.view.get(idx) : null;
   }

   private void selectView(int idx) {
      this.selectedEntry = idx;
      this.yaw = 0.0F;
      this.pitch = 0.0F;
      this.lastInteractMs = Util.getMeasuringTimeMs();
   }

   public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
      long now = Util.getMeasuringTimeMs();
      long dt = now - this.lastFrameMs;
      this.lastFrameMs = now;
      if (!this.dragging && now - this.lastInteractMs > 1500L) {
         this.yaw += 0.03F * (float)dt;
      }

      this.hoveredEntry = this.dragging ? -1 : this.hoveredIndex((double)mouseX, (double)mouseY);
      SkinEntry shown = this.shownEntry();
      String shownKey = shown != null ? modelKey(shown) : null;
      if (!Objects.equals(shownKey, this.lastShownKey)) {
         this.lastShownKey = shownKey;
         this.yaw = 0.0F;
         this.pitch = 0.0F;
         this.lastInteractMs = now;
      }

      this.renderBackground(g, mouseX, mouseY, partialTick);
      Starfield.draw(g, 0, 0, this.width, this.height, now, 44, 7L, 0.5F);
      int left = this.panelLeft();
      int top = this.panelTop();
      g.fill(left - 3, top - 3, left + 508 + 3, top + 252 + 3, 1426063360);
      g.fillGradient(left, top, left + 508, top + 252, -199614136, -200601562);
      g.drawBorder(left - 1, top - 1, 510, 254, -16447985);
      g.drawBorder(left, top, 508, 252, -13747610);
      g.fillGradient(left, top, left + 508, top + 18, -14405538, -15459782);
      g.fill(left, top + 18, left + 508, top + 19, -6467875);
      g.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), left + 254, top + 5, -2053377);
      int px0 = this.previewX0();
      int py0 = this.previewY0();
      int px1 = this.previewX1();
      int py1 = this.previewY1();
      g.fillGradient(px0, py0, px1, py1, -14998448, -16315880);
      Starfield.draw(g, px0, py0, px1, py1, now, 34, 91L, 0.85F);
      g.drawBorder(px0 - 1, py0 - 1, px1 - px0 + 2, py1 - py0 + 2, -13747610);
      if (shown != null) {
         PokemonPreviewRenderer.render(g, px0, py0, px1, py1, this.yaw, this.pitch, this.zoom, shown.species(), this.withShiny(shown.aspects()), this.idleFly.contains(shown.species()), partialTick);
      }

      int stx = px0 + 2;
      int sty = py0 + 2;
      boolean shinyHover = PreviewUi.inRect((double)mouseX, (double)mouseY, stx, sty, 18, 18);
      g.fill(stx, sty, stx + 18, sty + 18, this.shiny ? -856185284 : (shinyHover ? 1728053247 : 1711276032));
      g.drawBorder(stx, sty, 18, 18, this.shiny ? -10934 : -13747610);
      g.drawCenteredTextWithShadow(this.textRenderer, Text.literal("✨"), stx + 9, sty + 5, this.shiny ? -14674944 : -10934);
      g.drawText(this.textRenderer, Text.literal("Drag • Scroll"), px0, py1 + 4, -7035976, false);
      this.renderCategorySidebar(g, mouseX, mouseY);
      this.renderGrid(g, mouseX, mouseY);

      for(Element child : this.children()) {
         if (child instanceof Drawable renderable) {
            renderable.render(g, mouseX, mouseY, partialTick);
         }
      }

      int sortY = this.searchY();
      boolean sortHover = PreviewUi.inRect((double)mouseX, (double)mouseY, this.sortX(), sortY, 42, 12);
      g.fill(this.sortX(), sortY, this.sortX() + 42, sortY + 12, sortHover ? -14274990 : -15328200);
      g.drawBorder(this.sortX(), sortY, 42, 12, this.sortMode == 0 ? -13747610 : -6467875);
      g.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.sortLabel()), this.sortX() + 21, sortY + 2, this.sortMode == 0 ? -7035976 : -2053377);
      if (sortHover) {
         Tooltips.render(g, this.textRenderer, Text.literal("Sort: " + this.sortLabel()), mouseX, mouseY);
      } else if (shinyHover) {
         Tooltips.render(g, this.textRenderer, Text.literal(this.shiny ? "Shiny: on" : "Shiny: off"), mouseX, mouseY);
      } else if (this.hoveredEntry >= 0 && this.hoveredEntry < this.view.size()) {
         SkinEntry e = (SkinEntry)this.view.get(this.hoveredEntry);
         Tooltips.render(g, this.textRenderer, List.of(e.label(), Text.literal("Skin: " + e.source()).formatted(Formatting.DARK_GRAY)), mouseX, mouseY);
      }

   }

   private void renderCategorySidebar(DrawContext g, int mouseX, int mouseY) {
      int x = this.sidebarX();
      int y0 = this.previewY0();
      int y1 = this.previewY1();
      g.fill(x - 1, y0 - 1, x + 104 + 1, y1 + 1, -15723477);
      g.drawBorder(x - 1, y0 - 1, 106, y1 - y0 + 2, -13747610);
      boolean searching = !this.search.getText().isEmpty();
      int rows = this.visibleCatRows();
      int max = this.maxCatScroll();
      g.enableScissor(x, y0, x + 104, y1);

      for(int r = 0; r < rows; ++r) {
         int idx = this.categoryScroll + r;
         if (idx >= this.tabs.size()) {
            break;
         }

         int ry = y0 + r * 15;
         boolean isActive = idx == this.activeTab && !searching;
         boolean hover = PreviewUi.inRect((double)mouseX, (double)mouseY, x, ry, 104, 15);
         boolean crateRow = ((TabDef)this.tabs.get(idx)).crate();
         int base = crateRow ? -15068366 : -15528414;
         int hoverBg = crateRow ? -14411202 : -14936272;
         g.fill(x, ry, x + 104, ry + 15 - 1, isActive ? -14410694 : (hover ? hoverBg : base));
         if (isActive) {
            g.fill(x, ry, x + 2, ry + 15 - 1, -6467875);
         }

         int tint = isActive ? -1 : (hover ? -2962968 : -7035976);
         Text styled = ((TabDef)this.tabs.get(idx)).styled();
         if (styled != null) {
            g.drawText(this.textRenderer, styled, x + 5, ry + 3, -1, false);
         } else {
            g.drawText(this.textRenderer, ((TabDef)this.tabs.get(idx)).name(), x + 5, ry + 3, tint, false);
         }

         String count = String.valueOf(((TabDef)this.tabs.get(idx)).entries().size());
         int countX = x + 104 - this.textRenderer.getWidth(count) - (max > 0 ? 6 : 4);
         g.drawText(this.textRenderer, count, countX, ry + 3, isActive ? -7437640 : -11379078, false);
      }

      g.disableScissor();
      if (max > 0) {
         int trackX = x + 104 - 3;
         int h = y1 - y0;
         g.fill(trackX, y0, trackX + 3, y1, -15986650);
         int thumbH = Math.max(10, h * rows / (max + rows));
         int thumbY = y0 + (h - thumbH) * this.categoryScroll / max;
         g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, -10862962);
      }

   }

   private void renderGrid(DrawContext g, int mouseX, int mouseY) {
      int x = this.gridX();
      int y = this.gridY();
      int rows = this.visibleRows();
      int gridW = 180;
      int gridH = rows * 20;
      g.fill(x - 2, y - 2, x + gridW + 2, y + gridH + 2, -15723477);
      g.drawBorder(x - 2, y - 2, gridW + 4, gridH + 4, -13747610);
      if (this.view.isEmpty()) {
         String msg = this.search.getText().isEmpty() ? "Nothing here yet." : "No matches.";
         g.drawCenteredTextWithShadow(this.textRenderer, Text.literal(msg), x + gridW / 2, y + gridH / 2 - 4, -7035976);
      } else {
         g.enableScissor(x, y, x + gridW, y + gridH);
         int first = this.scrollRow * 9;

         for(int i = first; i < this.view.size() && i < first + 9 * rows; ++i) {
            SkinEntry e = (SkinEntry)this.view.get(i);
            int cellX = x + (i - first) % 9 * 20;
            int cellY = y + (i - first) / 9 * 20;
            g.fill(cellX + 1, cellY + 1, cellX + 20 - 1, cellY + 20 - 1, -15064506);
            ItemStack stack = this.stackFor(e);
            if (!stack.isEmpty()) {
               g.drawItem(stack, cellX + 2, cellY + 2);
            }

            if (i == this.selectedEntry) {
               g.drawBorder(cellX, cellY, 20, 20, -12474273);
            }

            if (mouseX >= cellX && mouseX < cellX + 20 && mouseY >= cellY && mouseY < cellY + 20) {
               g.drawBorder(cellX, cellY, 20, 20, -1717743907);
            }
         }

         g.disableScissor();
         int maxScroll = this.maxScrollRow();
         if (maxScroll > 0) {
            int trackX = x + gridW + 2;
            g.fill(trackX, y, trackX + 4, y + gridH, -15986650);
            int thumbH = Math.max(10, gridH * rows / (maxScroll + rows));
            int thumbY = y + (gridH - thumbH) * this.scrollRow / maxScroll;
            g.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, -10862962);
         }

      }
   }

   private int hoveredIndex(double mouseX, double mouseY) {
      int x = this.gridX();
      int y = this.gridY();
      if (!(mouseX < (double)x) && !(mouseX >= (double)(x + 180)) && !(mouseY < (double)y) && !(mouseY >= (double)(y + this.visibleRows() * 20))) {
         int col = (int)((mouseX - (double)x) / (double)20.0F);
         int row = (int)((mouseY - (double)y) / (double)20.0F);
         int index = (this.scrollRow + row) * 9 + col;
         return index >= 0 && index < this.view.size() ? index : -1;
      } else {
         return -1;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (PreviewUi.inRect(mouseX, mouseY, this.sortX(), this.searchY(), 42, 12)) {
         this.sortMode = (this.sortMode + 1) % 3;
         PreviewUi.playClick();
         this.rebuildView();
         return true;
      } else {
         int cat = this.categoryAt(mouseX, mouseY);
         if (cat < 0) {
            int clicked = this.hoveredIndex(mouseX, mouseY);
            if (clicked >= 0) {
               PreviewUi.playClick();
               this.search.setFocused(false);
               this.selectView(clicked);
               return true;
            } else if (PreviewUi.inRect(mouseX, mouseY, this.previewX0() + 2, this.previewY0() + 2, 18, 18)) {
               this.shiny = !this.shiny;
               this.stackCache.clear();
               PreviewUi.playClick();
               return true;
            } else if (button == 0 && this.inPreview(mouseX, mouseY)) {
               this.dragging = true;
               this.lastInteractMs = Util.getMeasuringTimeMs();
               return true;
            } else {
               return super.mouseClicked(mouseX, mouseY, button);
            }
         } else {
            if (cat != this.activeTab || !this.search.getText().isEmpty()) {
               this.activeTab = cat;
               this.search.setText("");
               this.rebuildView();
               PreviewUi.playClick();
            }

            return true;
         }
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.dragging = false;
      }

      return super.mouseReleased(mouseX, mouseY, button);
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
               this.ensureRowVisible(next / 9);
               PreviewUi.playClick();
            }

            return true;
         } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   private void ensureRowVisible(int row) {
      if (row < this.scrollRow) {
         this.scrollRow = row;
      } else if (row >= this.scrollRow + this.visibleRows()) {
         this.scrollRow = row - this.visibleRows() + 1;
      }

      this.scrollRow = MathHelper.clamp(this.scrollRow, 0, this.maxScrollRow());
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.dragging) {
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
         this.zoom = MathHelper.clamp(this.zoom * (float)Math.pow((double)1.12F, scrollY), 0.5F, 2.6F);
         this.lastInteractMs = Util.getMeasuringTimeMs();
         return true;
      } else if (this.inSidebar(mouseX, mouseY)) {
         this.categoryScroll = MathHelper.clamp(this.categoryScroll - (int)Math.signum(scrollY), 0, this.maxCatScroll());
         return true;
      } else {
         int gx = this.gridX();
         int gy = this.gridY();
         if (mouseX >= (double)gx && mouseX < (double)(gx + 180 + 6) && mouseY >= (double)gy && mouseY < (double)(gy + this.visibleRows() * 20)) {
            this.scrollRow = MathHelper.clamp(this.scrollRow - (int)Math.signum(scrollY), 0, this.maxScrollRow());
            return true;
         } else {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
         }
      }
   }

   private boolean inPreview(double mouseX, double mouseY) {
      return mouseX >= (double)this.previewX0() && mouseX < (double)this.previewX1() && mouseY >= (double)this.previewY0() && mouseY < (double)this.previewY1();
   }

   private boolean inSidebar(double mouseX, double mouseY) {
      return mouseX >= (double)this.sidebarX() && mouseX < (double)(this.sidebarX() + 104) && mouseY >= (double)this.previewY0() && mouseY < (double)this.previewY1();
   }

   private int categoryAt(double mouseX, double mouseY) {
      if (!this.inSidebar(mouseX, mouseY)) {
         return -1;
      } else {
         int r = (int)((mouseY - (double)this.previewY0()) / (double)15.0F);
         int idx = this.categoryScroll + r;
         return idx >= 0 && idx < this.tabs.size() ? idx : -1;
      }
   }

   public boolean shouldPause() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   private static record SkinEntry(String species, List<String> aspects, Text label, String source) {
   }

   @Environment(EnvType.CLIENT)
   private static record TabDef(String name, Text styled, List<SkinEntry> entries, boolean crate) {
   }
}
