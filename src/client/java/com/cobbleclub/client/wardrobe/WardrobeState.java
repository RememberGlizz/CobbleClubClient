/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeCosmeticEntry
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeGlowEntry
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeOpenMsg
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobePresetSummary
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeStateMsg
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2960
 *  net.minecraft.class_7923
 *  net.minecraft.class_9280
 *  net.minecraft.class_9282
 *  net.minecraft.class_9334
 */
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.DataComponentTypes;

@Environment(value=EnvType.CLIENT)
public final class WardrobeState {
    static final double[] DEFAULT_BALLOON_OFFSET = new double[]{0.72, 0.3, 0.08};
    private static final WardrobeState INSTANCE = new WardrobeState();
    private final Map<String, WardrobeCosmeticEntry> catalog = new LinkedHashMap<String, WardrobeCosmeticEntry>();
    private final Map<WardrobeSlot, String> equippedIds = new EnumMap<WardrobeSlot, String>(WardrobeSlot.class);
    private final Map<String, Integer> colors = new HashMap<String, Integer>();
    private final Map<WardrobeSlot, String> hoverIds = new EnumMap<WardrobeSlot, String>(WardrobeSlot.class);
    private final Map<WardrobeSlot, String> selectedIds = new EnumMap<WardrobeSlot, String>(WardrobeSlot.class);
    private final Map<String, ItemStack> stackCache = new HashMap<String, ItemStack>();
    private boolean hidden;
    private int lastRevision;
    private List<String> lockedTooltip = List.of();
    private Map<WardrobeSlot, String> slotDisplayNames = Map.of();
    private double[] balloonOffset = DEFAULT_BALLOON_OFFSET;
    private List<WardrobeGlowEntry> glows = List.of();
    private List<WardrobePresetSummary> presets = List.of();
    private String hoverGlowId;
    private WardrobePresetSummary presetPreview;

    public static WardrobeState get() {
        return INSTANCE;
    }

    private WardrobeState() {
    }

    public void applyOpen(WardrobeOpenMsg msg) {
        double[] dArray;
        this.hoverIds.clear();
        this.selectedIds.clear();
        this.lastRevision = 0;
        this.hidden = msg.getHidden();
        this.lockedTooltip = msg.getLockedTooltip() != null ? msg.getLockedTooltip() : List.of();
        this.slotDisplayNames = msg.getSlotDisplayNames() != null ? msg.getSlotDisplayNames() : Map.of();
        List offset = msg.getBalloonOffset();
        if (offset != null && offset.size() == 3 && !offset.contains(null)) {
            double[] dArray2 = new double[3];
            dArray2[0] = (Double)offset.get(0);
            dArray2[1] = (Double)offset.get(1);
            dArray = dArray2;
            dArray2[2] = (Double)offset.get(2);
        } else {
            dArray = DEFAULT_BALLOON_OFFSET;
        }
        this.balloonOffset = dArray;
        this.glows = msg.getGlows() != null ? msg.getGlows() : List.of();
        this.presets = msg.getPresets() != null ? msg.getPresets() : List.of();
        this.applyEntries(msg.getEntries());
    }

    public boolean applyState(WardrobeStateMsg msg) {
        if (msg.getRevision() <= this.lastRevision) {
            return false;
        }
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

    private void applyEntries(List<WardrobeCosmeticEntry> entries) {
        this.catalog.clear();
        this.equippedIds.clear();
        this.colors.clear();
        this.stackCache.clear();
        if (entries != null) {
            for (WardrobeCosmeticEntry entry : entries) {
                if (entry == null || entry.getId() == null || entry.getSlot() == null || entry.getMaterial() == null) continue;
                this.catalog.put(entry.getId(), entry);
                if (entry.getEquipped()) {
                    this.equippedIds.put(entry.getSlot(), entry.getId());
                }
                if (entry.getColor() == null) continue;
                this.colors.put(entry.getId(), entry.getColor());
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
        for (WardrobeGlowEntry g : this.glows) {
            if (g == null || !g.getEquipped()) continue;
            return g;
        }
        return null;
    }

    public void setHoverGlow(String idOrNull) {
        this.hoverGlowId = idOrNull;
    }

    public WardrobeGlowEntry outlineGlow() {
        if (this.hoverGlowId != null) {
            for (WardrobeGlowEntry g : this.glows) {
                if (g == null || !this.hoverGlowId.equals(g.getId())) continue;
                return g;
            }
        }
        return this.equippedGlow();
    }

    public List<WardrobeCosmeticEntry> entriesFor(WardrobeSlot slot) {
        ArrayList<WardrobeCosmeticEntry> result = new ArrayList<WardrobeCosmeticEntry>();
        for (WardrobeCosmeticEntry entry : this.catalog.values()) {
            if (entry.getSlot() != slot) continue;
            result.add(entry);
        }
        return result;
    }

    public WardrobeCosmeticEntry entry(String id) {
        return id != null ? this.catalog.get(id) : null;
    }

    public boolean isEquipped(WardrobeCosmeticEntry entry) {
        return entry.getId().equals(this.equippedIds.get(entry.getSlot()));
    }

    public WardrobeCosmeticEntry equippedEntry(WardrobeSlot slot) {
        return this.entry(this.equippedIds.get(slot));
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
        ArrayList<WardrobeGlowEntry> clearedGlows = new ArrayList<WardrobeGlowEntry>();
        for (WardrobeGlowEntry glow : this.glows) {
            if (glow == null) continue;
            clearedGlows.add(new WardrobeGlowEntry(glow.getId(), glow.getDisplayName(), glow.getColors(), glow.getOwned(), false, glow.getEquipLore(), glow.getUnequipLore(), glow.getLockedLore(), glow.getDisplayNameJson()));
        }
        this.glows = List.copyOf(clearedGlows);
    }

    public void setColorLocal(String id, int rgb) {
        this.colors.put(id, rgb & 0xFFFFFF);
    }

    public Integer colorFor(String id) {
        return this.colors.get(id);
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
        return this.selectedIds.get(slot);
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
        WardrobeCosmeticEntry hover = this.entry(this.hoverIds.get(slot));
        if (hover != null) {
            return hover;
        }
        WardrobeCosmeticEntry selected = this.entry(this.selectedIds.get(slot));
        return selected != null ? selected : this.entry(this.equippedIds.get(slot));
    }

    public ItemStack previewStack(WardrobeSlot slot) {
        if (this.hidden) {
            return ItemStack.EMPTY;
        }
        if (this.presetPreview != null) {
            String id;
            Map pv = this.presetPreview.getPreview();
            String string = id = pv != null ? (String)pv.get(slot) : null;
            if (id == null) {
                return ItemStack.EMPTY;
            }
            Map pc = this.presetPreview.getPreviewColors();
            return this.stackForIdWithColor(id, pc != null ? (Integer)pc.get(slot) : null);
        }
        WardrobeCosmeticEntry entry = this.effectivePreview(slot);
        return entry != null ? this.stackFor(entry) : ItemStack.EMPTY;
    }

    public ItemStack stackFor(WardrobeCosmeticEntry entry) {
        Integer color = entry.getDyeable() ? this.colors.get(entry.getId()) : null;
        String var10000 = entry.getId();
        String key = var10000 + "#" + color;
        return this.stackCache.computeIfAbsent(key, k -> WardrobeState.buildStack(entry, color));
    }

    private ItemStack stackForIdWithColor(String id, Integer color) {
        WardrobeCosmeticEntry entry = this.catalog.get(id);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        Integer c = entry.getDyeable() ? color : null;
        return this.stackCache.computeIfAbsent(id + "#" + c, k -> WardrobeState.buildStack(entry, c));
    }

    private static ItemStack buildStack(WardrobeCosmeticEntry entry, Integer color) {
        Identifier materialId = Identifier.tryParse((String)entry.getMaterial());
        if (materialId == null) {
            return ItemStack.EMPTY;
        }
        Item item = (Item)Registries.ITEM.get(materialId);
        ItemStack stack = new ItemStack((ItemConvertible)item);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(entry.getCustomModelData()));
        if (color != null) {
            stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color & 0xFFFFFF, false));
        }
        return stack;
    }

    public List<String> getLockedTooltip() {
        return this.lockedTooltip;
    }

    public String slotDisplayName(WardrobeSlot slot) {
        if (slot == WardrobeSlot.HELMET) {
            return "Hats";
        }
        if (slot == WardrobeSlot.BACKPACK) {
            return "Wings";
        }
        if (slot == WardrobeSlot.BALLOON) {
            return "Balloons";
        }
        String raw = slot.name().toLowerCase(Locale.ROOT);
        char var10000 = Character.toUpperCase(raw.charAt(0));
        return var10000 + raw.substring(1);
    }

    public double[] getBalloonOffset() {
        return this.balloonOffset;
    }
}

