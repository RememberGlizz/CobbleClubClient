package com.cobbleclub.client.wardrobe;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeCosmeticEntry;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeGlowEntry;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeOpenMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobePresetSummary;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeStateMsg;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class WardrobeState {
   static final double[] DEFAULT_BALLOON_OFFSET = new double[]{0.72, 0.30, 0.08};
   private static final WardrobeState INSTANCE = new WardrobeState();
   private final Map<String, WardrobeCosmeticEntry> catalog = new LinkedHashMap();
   private final Map<WardrobeSlot, String> equippedIds = new EnumMap(WardrobeSlot.class);
   private final Map<String, Integer> colors = new HashMap();
   private final Map<WardrobeSlot, String> hoverIds = new EnumMap(WardrobeSlot.class);
   private final Map<WardrobeSlot, String> selectedIds = new EnumMap(WardrobeSlot.class);
   private final Map<String, ItemStack> stackCache = new HashMap();
   private boolean hidden;
   private int lastRevision;
   private List<String> lockedTooltip = List.of();
   private Map<WardrobeSlot, String> slotDisplayNames = Map.of();
   private double[] balloonOffset;
   private List<WardrobeGlowEntry> glows;
   private List<WardrobePresetSummary> presets;
   private String hoverGlowId;
   private WardrobePresetSummary presetPreview;

   public static WardrobeState get() {
      return INSTANCE;
   }

   private WardrobeState() {
      this.balloonOffset = DEFAULT_BALLOON_OFFSET;
      this.glows = List.of();
      this.presets = List.of();
   }

   public void applyOpen(WardrobeOpenMsg msg) {
      this.hoverIds.clear();
      this.selectedIds.clear();
      this.lastRevision = 0;
      this.hidden = msg.getHidden();
      this.lockedTooltip = msg.getLockedTooltip() != null ? msg.getLockedTooltip() : List.of();
      this.slotDisplayNames = msg.getSlotDisplayNames() != null ? msg.getSlotDisplayNames() : Map.of();
      List<Double> offset = msg.getBalloonOffset();
      this.balloonOffset = offset != null && offset.size() == 3 && !offset.contains((Object)null) ? new double[]{(Double)offset.get(0), (Double)offset.get(1), (Double)offset.get(2)} : DEFAULT_BALLOON_OFFSET;
      this.glows = msg.getGlows() != null ? msg.getGlows() : List.of();
      this.presets = msg.getPresets() != null ? msg.getPresets() : List.of();
      this.applyEntries(msg.getEntries());
   }

   public boolean applyState(WardrobeStateMsg msg) {
      if (msg.getRevision() <= this.lastRevision) {
         return false;
      } else {
         this.lastRevision = msg.getRevision();
         this.hidden = msg.getHidden();
         if (msg.getGlows() != null) {
            this.glows = msg.getGlows();
         }

         if (msg.getPresets() != null) {
            this.presets = msg.getPresets();
         }

         this.applyEntries(msg.getEntries());
         return true;
      }
   }

   private void applyEntries(List<WardrobeCosmeticEntry> entries) {
      this.catalog.clear();
      this.equippedIds.clear();
      this.colors.clear();
      this.stackCache.clear();
      if (entries != null) {
         for(WardrobeCosmeticEntry entry : entries) {
            if (entry != null && entry.getId() != null && entry.getSlot() != null && entry.getMaterial() != null) {
               this.catalog.put(entry.getId(), entry);
               if (entry.getEquipped()) {
                  this.equippedIds.put(entry.getSlot(), entry.getId());
               }

               if (entry.getColor() != null) {
                  this.colors.put(entry.getId(), entry.getColor());
               }
            }
         }

      }
   }

   public void reset() {
      this.catalog.clear();
      this.equippedIds.clear();
      this.colors.clear();
      this.stackCache.clear();
      this.hoverIds.clear();
      this.selectedIds.clear();
      this.hidden = false;
      this.lastRevision = 0;
      this.lockedTooltip = List.of();
      this.slotDisplayNames = Map.of();
      this.balloonOffset = DEFAULT_BALLOON_OFFSET;
      this.glows = List.of();
      this.presets = List.of();
      this.hoverGlowId = null;
      this.presetPreview = null;
   }

   public List<WardrobeGlowEntry> glows() {
      return this.glows;
   }

   public List<WardrobePresetSummary> presets() {
      return this.presets;
   }

   public WardrobeGlowEntry equippedGlow() {
      for(WardrobeGlowEntry g : this.glows) {
         if (g != null && g.getEquipped()) {
            return g;
         }
      }

      return null;
   }

   public void setHoverGlow(String idOrNull) {
      this.hoverGlowId = idOrNull;
   }

   public WardrobeGlowEntry outlineGlow() {
      if (this.hoverGlowId != null) {
         for(WardrobeGlowEntry g : this.glows) {
            if (g != null && this.hoverGlowId.equals(g.getId())) {
               return g;
            }
         }
      }

      return this.equippedGlow();
   }

   public List<WardrobeCosmeticEntry> entriesFor(WardrobeSlot slot) {
      List<WardrobeCosmeticEntry> result = new ArrayList();

      for(WardrobeCosmeticEntry entry : this.catalog.values()) {
         if (entry.getSlot() == slot) {
            result.add(entry);
         }
      }

      return result;
   }

   public WardrobeCosmeticEntry entry(String id) {
      return id != null ? (WardrobeCosmeticEntry)this.catalog.get(id) : null;
   }

   public boolean isEquipped(WardrobeCosmeticEntry entry) {
      return entry.getId().equals(this.equippedIds.get(entry.getSlot()));
   }

   public WardrobeCosmeticEntry equippedEntry(WardrobeSlot slot) {
      return this.entry((String)this.equippedIds.get(slot));
   }

   public void equipLocal(WardrobeCosmeticEntry entry) {
      this.equippedIds.put(entry.getSlot(), entry.getId());
   }

   public void unequipLocal(WardrobeSlot slot) {
      this.equippedIds.remove(slot);
      this.hoverIds.remove(slot);
      this.selectedIds.remove(slot);
      this.presetPreview = null;
   }

   public void unequipAllLocal() {
      this.equippedIds.clear();
      this.hoverIds.clear();
      this.selectedIds.clear();
      this.hoverGlowId = null;
      this.presetPreview = null;
      List<WardrobeGlowEntry> clearedGlows = new ArrayList<>();
      for (WardrobeGlowEntry glow : this.glows) {
         if (glow == null) continue;
         clearedGlows.add(new WardrobeGlowEntry(
               glow.getId(), glow.getDisplayName(), glow.getColors(), glow.getOwned(), false,
               glow.getEquipLore(), glow.getUnequipLore(), glow.getLockedLore(), glow.getDisplayNameJson()));
      }
      this.glows = List.copyOf(clearedGlows);
   }

   public void setColorLocal(String id, int rgb) {
      this.colors.put(id, rgb & 16777215);
   }

   public Integer colorFor(String id) {
      return (Integer)this.colors.get(id);
   }

   public void setHiddenLocal(boolean value) {
      this.hidden = value;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setHover(WardrobeSlot slot, String idOrNull) {
      if (idOrNull == null) {
         this.hoverIds.remove(slot);
      } else {
         this.hoverIds.put(slot, idOrNull);
      }

   }

   public void setSelected(WardrobeSlot slot, String idOrNull) {
      if (idOrNull == null) {
         this.selectedIds.remove(slot);
      } else {
         this.selectedIds.put(slot, idOrNull);
      }

   }

   public String getSelected(WardrobeSlot slot) {
      return (String)this.selectedIds.get(slot);
   }

   public void clearPreviewOverrides() {
      this.hoverIds.clear();
      this.selectedIds.clear();
      this.presetPreview = null;
   }

   public void setPresetPreview(WardrobePresetSummary preset) {
      this.presetPreview = preset;
   }

   public WardrobeCosmeticEntry effectivePreview(WardrobeSlot slot) {
      WardrobeCosmeticEntry hover = this.entry((String)this.hoverIds.get(slot));
      if (hover != null) {
         return hover;
      } else {
         WardrobeCosmeticEntry selected = this.entry((String)this.selectedIds.get(slot));
         return selected != null ? selected : this.entry((String)this.equippedIds.get(slot));
      }
   }

   public ItemStack previewStack(WardrobeSlot slot) {
      if (this.hidden) {
         return ItemStack.EMPTY;
      }
      if (this.presetPreview != null) {
         Map<WardrobeSlot, String> pv = this.presetPreview.getPreview();
         String id = pv != null ? (String)pv.get(slot) : null;
         if (id == null) {
            return ItemStack.EMPTY;
         } else {
            Map<WardrobeSlot, Integer> pc = this.presetPreview.getPreviewColors();
            return this.stackForIdWithColor(id, pc != null ? (Integer)pc.get(slot) : null);
         }
      } else {
         WardrobeCosmeticEntry entry = this.effectivePreview(slot);
         return entry != null ? this.stackFor(entry) : ItemStack.EMPTY;
      }
   }

   public ItemStack stackFor(WardrobeCosmeticEntry entry) {
      Integer color = entry.getDyeable() ? (Integer)this.colors.get(entry.getId()) : null;
      String var10000 = entry.getId();
      String key = var10000 + "#" + color;
      return (ItemStack)this.stackCache.computeIfAbsent(key, (k) -> buildStack(entry, color));
   }

   private ItemStack stackForIdWithColor(String id, Integer color) {
      WardrobeCosmeticEntry entry = (WardrobeCosmeticEntry)this.catalog.get(id);
      if (entry == null) {
         return ItemStack.EMPTY;
      } else {
         Integer c = entry.getDyeable() ? color : null;
         return (ItemStack)this.stackCache.computeIfAbsent(id + "#" + c, (k) -> buildStack(entry, c));
      }
   }

   private static ItemStack buildStack(WardrobeCosmeticEntry entry, Integer color) {
      Identifier materialId = Identifier.tryParse(entry.getMaterial());
      if (materialId == null) {
         return ItemStack.EMPTY;
      } else {
         Item item = (Item)Registries.ITEM.get(materialId);
         ItemStack stack = new ItemStack(item);
         if (stack.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(entry.getCustomModelData()));
            if (color != null) {
               stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color & 16777215, false));
            }

            return stack;
         }
      }
   }

   public List<String> getLockedTooltip() {
      return this.lockedTooltip;
   }

   public String slotDisplayName(WardrobeSlot slot) {
      if (slot == WardrobeSlot.HELMET) return "Hats";
      if (slot == WardrobeSlot.BACKPACK) return "Wings";
      if (slot == WardrobeSlot.BALLOON) return "Balloons";
      String raw = slot.name().toLowerCase(Locale.ROOT);
      char var10000 = Character.toUpperCase(raw.charAt(0));
      return var10000 + raw.substring(1);
   }

   public double[] getBalloonOffset() {
      return this.balloonOffset;
   }
}
