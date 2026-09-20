/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionMsg
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeCosmeticEntry
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeGlowEntry
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeOpenMsg
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobePresetSummary
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeStateMsg
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_8710
 */
package com.cobbleclub.server.service;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeCosmeticEntry;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeGlowEntry;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeOpenMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobePresetSummary;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeStateMsg;
import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import com.cobbleclub.server.service.CosmeticVisualService;
import com.cobbleclub.server.service.EconomyService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.CustomPayload;

public final class WardrobeService {
    private WardrobeService() {
    }

    public static void open(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.WardrobeOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        PlayerDataStore.PlayerData data = WardrobeService.data(player);
        EnumMap<WardrobeSlot, String> names = new EnumMap<WardrobeSlot, String>(WardrobeSlot.class);
        names.put(WardrobeSlot.HELMET, "Hats");
        names.put(WardrobeSlot.BACKPACK, "Wings");
        names.put(WardrobeSlot.BALLOON, "Balloons");
        WardrobeOpenMsg message = new WardrobeOpenMsg(2, WardrobeSlot.HELMET, data.wardrobeHidden, CobbleClubServer.config().showLockedWardrobeItems, List.of("{\"text\":\"You have not unlocked this cosmetic\",\"color\":\"red\"}"), names, List.of(Double.valueOf(0.5), Double.valueOf(3.5), Double.valueOf(0.5)), WardrobeService.cosmeticEntries(data), WardrobeService.glowEntries(data), WardrobeService.presetEntries(data));
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.WardrobeOpen(WardrobeProtocol.INSTANCE.encode((Object)message)));
    }

    public static void handle(ServerPlayerEntity player, String json) {
        WardrobeActionMsg message = (WardrobeActionMsg)WardrobeProtocol.INSTANCE.decode(json, WardrobeActionMsg.class);
        if (message == null || message.getProtocolVersion() != 2 || message.getAction() == null) {
            return;
        }
        PlayerDataStore.PlayerData data = WardrobeService.data(player);
        switch (message.getAction()) {
            case EQUIP: {
                WardrobeService.equipOrBuy(player, data, message.getCosmeticId());
                break;
            }
            case UNEQUIP: {
                if (message.getSlot() == null) break;
                data.equipped.remove(message.getSlot().name());
                break;
            }
            case UNEQUIP_ALL: {
                data.equipped.clear();
                data.glow = null;
                break;
            }
            case SET_COLOR: {
                WardrobeService.setColor(data, message.getCosmeticId(), message.getColor());
                break;
            }
            case SET_HIDDEN: {
                if (message.getHidden() == null) break;
                data.wardrobeHidden = message.getHidden();
                break;
            }
            case SET_GLOW: {
                WardrobeService.setGlow(player, data, message.getGlowId());
                break;
            }
            case LOAD_PRESET: {
                WardrobeService.loadPreset(data, message.getPresetIndex());
                break;
            }
            case SAVE_PRESET: {
                WardrobeService.savePreset(data, message.getPresetIndex());
                break;
            }
            case DELETE_PRESET: {
                WardrobeService.deletePreset(data, message.getPresetIndex());
                break;
            }
        }
        ++data.revision;
        PlayerDataStore.save();
        CosmeticVisualService.apply(player);
        WardrobeService.sendState(player, data);
    }

    private static void equipOrBuy(ServerPlayerEntity player, PlayerDataStore.PlayerData data, String id) {
        ServerConfig.CosmeticDefinition definition = WardrobeService.cosmetic(id);
        WardrobeSlot slot = WardrobeService.slot(definition);
        if (definition == null || slot == null) {
            return;
        }
        if (!WardrobeService.owned(data, definition)) {
            if (definition.price() <= 0L || !EconomyService.withdraw(player, definition.price())) {
                player.sendMessage((Text)Text.literal((String)("You need " + EconomyService.format(Math.max(0L, definition.price())) + " to unlock " + definition.displayName() + ".")), false);
                return;
            }
            data.ownedCosmetics.add(definition.id());
            player.sendMessage((Text)Text.literal((String)("Purchased " + definition.displayName() + " for " + EconomyService.format(definition.price()) + ".")), false);
        }
        data.equipped.put(slot.name(), definition.id());
    }

    private static void setColor(PlayerDataStore.PlayerData data, String id, Integer color) {
        ServerConfig.CosmeticDefinition definition = WardrobeService.cosmetic(id);
        if (definition != null && WardrobeService.owned(data, definition) && definition.dyeable() && color != null && !"eevee_explorer_hood".equals(id)) {
            data.colors.put(id, color & 0xFFFFFF);
        }
    }

    private static void setGlow(ServerPlayerEntity player, PlayerDataStore.PlayerData data, String id) {
        if (id == null || id.isBlank()) {
            data.glow = null;
            return;
        }
        for (ServerConfig.GlowDefinition glow : CobbleClubServer.config().glows) {
            if (glow == null || !id.equals(glow.id())) continue;
            if (!WardrobeService.owned(data, glow)) {
                if (glow.price() <= 0L || !EconomyService.withdraw(player, glow.price())) {
                    player.sendMessage((Text)Text.literal((String)("You need " + EconomyService.format(Math.max(0L, glow.price())) + " to unlock " + glow.displayName() + ".")), false);
                    return;
                }
                data.ownedGlows.add(glow.id());
                player.sendMessage((Text)Text.literal((String)("Purchased " + glow.displayName() + " for " + EconomyService.format(glow.price()) + ".")), false);
            }
            data.glow = id;
            return;
        }
    }

    private static void loadPreset(PlayerDataStore.PlayerData data, Integer index) {
        if (!WardrobeService.validPreset(index)) {
            return;
        }
        PlayerDataStore.PresetData preset = data.presets.get(index.toString());
        if (preset == null) {
            return;
        }
        data.equipped.clear();
        if (preset.equipped != null) {
            data.equipped.putAll(preset.equipped);
        }
        data.colors.clear();
        if (preset.colors != null) {
            data.colors.putAll(preset.colors);
        }
        data.glow = preset.glow;
    }

    private static void savePreset(PlayerDataStore.PlayerData data, Integer index) {
        if (!WardrobeService.validPreset(index)) {
            return;
        }
        PlayerDataStore.PresetData preset = new PlayerDataStore.PresetData();
        preset.equipped.putAll(data.equipped);
        preset.colors.putAll(data.colors);
        preset.glow = data.glow;
        data.presets.put(index.toString(), preset);
    }

    private static void deletePreset(PlayerDataStore.PlayerData data, Integer index) {
        if (WardrobeService.validPreset(index)) {
            data.presets.remove(index.toString());
        }
    }

    public static boolean buy(ServerPlayerEntity player, String type, String id) {
        PlayerDataStore.PlayerData data = WardrobeService.data(player);
        if ("glow".equalsIgnoreCase(type)) {
            ServerConfig.GlowDefinition definition = WardrobeService.glow(id);
            if (definition == null || WardrobeService.owned(data, definition) || definition.price() <= 0L || !EconomyService.withdraw(player, definition.price())) {
                return false;
            }
            data.ownedGlows.add(definition.id());
            ++data.revision;
            PlayerDataStore.save();
            player.sendMessage((Text)Text.literal((String)("Unlocked glow " + definition.displayName() + ".")), false);
            return true;
        }
        ServerConfig.CosmeticDefinition definition = WardrobeService.cosmetic(id);
        if (definition == null || WardrobeService.owned(data, definition) || definition.price() <= 0L || !EconomyService.withdraw(player, definition.price())) {
            return false;
        }
        data.ownedCosmetics.add(definition.id());
        ++data.revision;
        PlayerDataStore.save();
        player.sendMessage((Text)Text.literal((String)("Unlocked cosmetic " + definition.displayName() + ".")), false);
        return true;
    }

    public static boolean grant(ServerPlayerEntity player, String type, String id) {
        boolean changed;
        PlayerDataStore.PlayerData data = WardrobeService.data(player);
        if ("glow".equalsIgnoreCase(type)) {
            if (WardrobeService.glow(id) == null) {
                return false;
            }
            changed = data.ownedGlows.add(id);
        } else {
            if (WardrobeService.cosmetic(id) == null) {
                return false;
            }
            changed = data.ownedCosmetics.add(id);
        }
        if (changed) {
            ++data.revision;
            PlayerDataStore.save();
        }
        return changed;
    }

    private static boolean validPreset(Integer index) {
        return index != null && index >= 0 && index < 5;
    }

    private static void sendState(ServerPlayerEntity player, PlayerDataStore.PlayerData data) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.WardrobeState.ID)) {
            return;
        }
        WardrobeStateMsg state = new WardrobeStateMsg(2, Math.max(1, data.revision), data.wardrobeHidden, WardrobeService.cosmeticEntries(data), WardrobeService.glowEntries(data), WardrobeService.presetEntries(data));
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.WardrobeState(WardrobeProtocol.INSTANCE.encode((Object)state)));
    }

    private static List<WardrobeCosmeticEntry> cosmeticEntries(PlayerDataStore.PlayerData data) {
        ArrayList<WardrobeCosmeticEntry> result = new ArrayList<WardrobeCosmeticEntry>();
        for (ServerConfig.CosmeticDefinition definition : CobbleClubServer.config().cosmetics) {
            WardrobeSlot slot = WardrobeService.slot(definition);
            if (definition == null || definition.id() == null || slot == null || definition.material() == null) continue;
            result.add(new WardrobeCosmeticEntry(definition.id(), definition.displayName(), slot, definition.material(), definition.customModelData(), definition.dyeable(), WardrobeService.owned(data, definition), definition.id().equals(data.equipped.get(slot.name())), data.colors.get(definition.id()), !WardrobeService.owned(data, definition) && definition.price() > 0L ? List.of("{\"text\":\"Click to buy for " + EconomyService.format(definition.price()) + "\",\"color\":\"gold\"}") : WardrobeService.safe(definition.lockedLore()), WardrobeService.safe(definition.equipLore()), WardrobeService.safe(definition.unequipLore()), definition.displayNameJson()));
        }
        return result;
    }

    private static List<WardrobeGlowEntry> glowEntries(PlayerDataStore.PlayerData data) {
        ArrayList<WardrobeGlowEntry> result = new ArrayList<WardrobeGlowEntry>();
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) {
            if (definition == null || definition.id() == null) continue;
            result.add(new WardrobeGlowEntry(definition.id(), definition.displayName(), WardrobeService.safe(definition.colors()), WardrobeService.owned(data, definition), definition.id().equals(data.glow), WardrobeService.safe(definition.equipLore()), WardrobeService.safe(definition.unequipLore()), !WardrobeService.owned(data, definition) && definition.price() > 0L ? List.of("{\"text\":\"Click to buy for " + EconomyService.format(definition.price()) + "\",\"color\":\"gold\"}") : WardrobeService.safe(definition.lockedLore()), definition.displayNameJson()));
        }
        return result;
    }

    private static List<WardrobePresetSummary> presetEntries(PlayerDataStore.PlayerData data) {
        ArrayList<WardrobePresetSummary> result = new ArrayList<WardrobePresetSummary>();
        for (int index = 0; index < 5; ++index) {
            PlayerDataStore.PresetData preset = data.presets.get(Integer.toString(index));
            EnumMap<WardrobeSlot, String> preview = new EnumMap<WardrobeSlot, String>(WardrobeSlot.class);
            EnumMap<WardrobeSlot, Integer> colors = new EnumMap<WardrobeSlot, Integer>(WardrobeSlot.class);
            if (preset != null && preset.equipped != null) {
                for (Map.Entry<String, String> entry : preset.equipped.entrySet()) {
                    try {
                        WardrobeSlot slot = WardrobeSlot.valueOf((String)entry.getKey());
                        preview.put(slot, entry.getValue());
                        Integer color = preset.colors == null ? null : preset.colors.get(entry.getValue());
                        if (color == null) continue;
                        colors.put(slot, color);
                    }
                    catch (IllegalArgumentException illegalArgumentException) {}
                }
            }
            result.add(new WardrobePresetSummary(index, preset != null, false, List.of(), preview, colors));
        }
        return result;
    }

    private static PlayerDataStore.PlayerData data(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        return data;
    }

    private static ServerConfig.CosmeticDefinition cosmetic(String id) {
        if (id == null) {
            return null;
        }
        if ("gengar_grin_mask".equals(id)) {
            id = "gengar-hat";
        }
        for (ServerConfig.CosmeticDefinition definition : CobbleClubServer.config().cosmetics) {
            if (definition == null || !id.equals(definition.id())) continue;
            return definition;
        }
        return null;
    }

    private static WardrobeSlot slot(ServerConfig.CosmeticDefinition definition) {
        if (definition == null || definition.slot() == null) {
            return null;
        }
        try {
            return WardrobeSlot.valueOf((String)definition.slot().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ServerConfig.GlowDefinition glow(String id) {
        if (id == null) {
            return null;
        }
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) {
            if (definition == null || !id.equals(definition.id())) continue;
            return definition;
        }
        return null;
    }

    private static boolean owned(PlayerDataStore.PlayerData data, ServerConfig.CosmeticDefinition definition) {
        return definition.ownedByDefault() || data.ownedCosmetics.contains(definition.id());
    }

    private static boolean owned(PlayerDataStore.PlayerData data, ServerConfig.GlowDefinition definition) {
        return definition.ownedByDefault() || data.ownedGlows.contains(definition.id());
    }

    private static <T> List<T> safe(List<T> value) {
        return value == null ? List.of() : value;
    }
}

