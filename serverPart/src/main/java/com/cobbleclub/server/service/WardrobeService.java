package com.cobbleclub.server.service;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionType;
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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WardrobeService {
    private WardrobeService() {}

    public static void open(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, Payloads.WardrobeOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        PlayerDataStore.PlayerData data = data(player);
        Map<WardrobeSlot, String> names = new EnumMap<>(WardrobeSlot.class);
        names.put(WardrobeSlot.HELMET, "Hats");
        names.put(WardrobeSlot.BACKPACK, "Wings");
        names.put(WardrobeSlot.BALLOON, "Balloons");
        WardrobeOpenMsg message = new WardrobeOpenMsg(
                2,
                WardrobeSlot.HELMET,
                data.wardrobeHidden,
                CobbleClubServer.config().showLockedWardrobeItems,
                List.of("{\"text\":\"You have not unlocked this cosmetic\",\"color\":\"red\"}"),
                names,
                List.of(0.5, 3.5, 0.5),
                cosmeticEntries(data),
                glowEntries(data),
                presetEntries(data)
        );
        ServerPlayNetworking.send(player, new Payloads.WardrobeOpen(WardrobeProtocol.INSTANCE.encode(message)));
    }

    public static void handle(ServerPlayerEntity player, String json) {
        WardrobeActionMsg message = WardrobeProtocol.INSTANCE.decode(json, WardrobeActionMsg.class);
        if (message == null || message.getProtocolVersion() != 2 || message.getAction() == null) return;
        PlayerDataStore.PlayerData data = data(player);

        switch (message.getAction()) {
            case EQUIP -> equipOrBuy(player, data, message.getCosmeticId());
            case UNEQUIP -> {
                if (message.getSlot() != null) data.equipped.remove(message.getSlot().name());
            }
            case UNEQUIP_ALL -> {
                data.equipped.clear();
                data.glow = null;
            }
            case SET_COLOR -> setColor(data, message.getCosmeticId(), message.getColor());
            case SET_HIDDEN -> {
                if (message.getHidden() != null) data.wardrobeHidden = message.getHidden();
            }
            case SET_GLOW -> setGlow(player, data, message.getGlowId());
            case LOAD_PRESET -> loadPreset(data, message.getPresetIndex());
            case SAVE_PRESET -> savePreset(data, message.getPresetIndex());
            case DELETE_PRESET -> deletePreset(data, message.getPresetIndex());
            case REQUEST_STATE -> { }
        }

        data.revision++;
        PlayerDataStore.save();
        CosmeticVisualService.apply(player);
        sendState(player, data);
    }

    private static void equipOrBuy(ServerPlayerEntity player, PlayerDataStore.PlayerData data, String id) {
        ServerConfig.CosmeticDefinition definition = cosmetic(id);
        WardrobeSlot slot = slot(definition);
        if (definition == null || slot == null) return;

        if (!owned(data, definition)) {
            if (definition.price() <= 0L || !EconomyService.withdraw(player, definition.price())) {
                player.sendMessage(net.minecraft.text.Text.literal("You need " + EconomyService.format(Math.max(0L, definition.price())) + " to unlock " + definition.displayName() + "."), false);
                return;
            }
            data.ownedCosmetics.add(definition.id());
            player.sendMessage(net.minecraft.text.Text.literal("Purchased " + definition.displayName() + " for " + EconomyService.format(definition.price()) + "."), false);
        }
        data.equipped.put(slot.name(), definition.id());
    }

    private static void setColor(PlayerDataStore.PlayerData data, String id, Integer color) {
        ServerConfig.CosmeticDefinition definition = cosmetic(id);
        if (definition != null && owned(data, definition) && definition.dyeable() && color != null
                && !"eevee_explorer_hood".equals(id)) {
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
            if (!owned(data, glow)) {
                if (glow.price() <= 0L || !EconomyService.withdraw(player, glow.price())) {
                    player.sendMessage(net.minecraft.text.Text.literal("You need " + EconomyService.format(Math.max(0L, glow.price())) + " to unlock " + glow.displayName() + "."), false);
                    return;
                }
                data.ownedGlows.add(glow.id());
                player.sendMessage(net.minecraft.text.Text.literal("Purchased " + glow.displayName() + " for " + EconomyService.format(glow.price()) + "."), false);
            }
            data.glow = id;
            return;
        }
    }

    private static void loadPreset(PlayerDataStore.PlayerData data, Integer index) {
        if (!validPreset(index)) return;
        PlayerDataStore.PresetData preset = data.presets.get(index.toString());
        if (preset == null) return;
        data.equipped.clear();
        if (preset.equipped != null) data.equipped.putAll(preset.equipped);
        data.colors.clear();
        if (preset.colors != null) data.colors.putAll(preset.colors);
        data.glow = preset.glow;
    }

    private static void savePreset(PlayerDataStore.PlayerData data, Integer index) {
        if (!validPreset(index)) return;
        PlayerDataStore.PresetData preset = new PlayerDataStore.PresetData();
        preset.equipped.putAll(data.equipped);
        preset.colors.putAll(data.colors);
        preset.glow = data.glow;
        data.presets.put(index.toString(), preset);
    }

    private static void deletePreset(PlayerDataStore.PlayerData data, Integer index) {
        if (validPreset(index)) data.presets.remove(index.toString());
    }

    public static boolean buy(ServerPlayerEntity player, String type, String id) {
        PlayerDataStore.PlayerData data = data(player);
        if ("glow".equalsIgnoreCase(type)) {
            ServerConfig.GlowDefinition definition = glow(id);
            if (definition == null || owned(data, definition) || definition.price() <= 0
                    || !EconomyService.withdraw(player, definition.price())) return false;
            data.ownedGlows.add(definition.id());
            data.revision++;
            PlayerDataStore.save();
            player.sendMessage(net.minecraft.text.Text.literal("Unlocked glow " + definition.displayName() + "."), false);
            return true;
        }
        ServerConfig.CosmeticDefinition definition = cosmetic(id);
        if (definition == null || owned(data, definition) || definition.price() <= 0
                || !EconomyService.withdraw(player, definition.price())) return false;
        data.ownedCosmetics.add(definition.id());
        data.revision++;
        PlayerDataStore.save();
        player.sendMessage(net.minecraft.text.Text.literal("Unlocked cosmetic " + definition.displayName() + "."), false);
        return true;
    }

    public static boolean grant(ServerPlayerEntity player, String type, String id) {
        PlayerDataStore.PlayerData data = data(player);
        boolean changed;
        if ("glow".equalsIgnoreCase(type)) {
            if (glow(id) == null) return false;
            changed = data.ownedGlows.add(id);
        } else {
            if (cosmetic(id) == null) return false;
            changed = data.ownedCosmetics.add(id);
        }
        if (changed) {
            data.revision++;
            PlayerDataStore.save();
        }
        return changed;
    }

    private static boolean validPreset(Integer index) {
        return index != null && index >= 0 && index < 5;
    }

    private static void sendState(ServerPlayerEntity player, PlayerDataStore.PlayerData data) {
        if (!ServerPlayNetworking.canSend(player, Payloads.WardrobeState.ID)) return;
        WardrobeStateMsg state = new WardrobeStateMsg(
                2,
                Math.max(1, data.revision),
                data.wardrobeHidden,
                cosmeticEntries(data),
                glowEntries(data),
                presetEntries(data)
        );
        ServerPlayNetworking.send(player, new Payloads.WardrobeState(WardrobeProtocol.INSTANCE.encode(state)));
    }

    private static List<WardrobeCosmeticEntry> cosmeticEntries(PlayerDataStore.PlayerData data) {
        List<WardrobeCosmeticEntry> result = new ArrayList<>();
        for (ServerConfig.CosmeticDefinition definition : CobbleClubServer.config().cosmetics) {
            WardrobeSlot slot = slot(definition);
            if (definition == null || definition.id() == null || slot == null || definition.material() == null) continue;
            result.add(new WardrobeCosmeticEntry(
                    definition.id(),
                    definition.displayName(),
                    slot,
                    definition.material(),
                    definition.customModelData(),
                    definition.dyeable(),
                    owned(data, definition),
                    definition.id().equals(data.equipped.get(slot.name())),
                    data.colors.get(definition.id()),
                    !owned(data, definition) && definition.price() > 0L
                            ? List.of("{\"text\":\"Click to buy for " + EconomyService.format(definition.price()) + "\",\"color\":\"gold\"}")
                            : safe(definition.lockedLore()),
                    safe(definition.equipLore()),
                    safe(definition.unequipLore()),
                    definition.displayNameJson()
            ));
        }
        return result;
    }

    private static List<WardrobeGlowEntry> glowEntries(PlayerDataStore.PlayerData data) {
        List<WardrobeGlowEntry> result = new ArrayList<>();
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) {
            if (definition == null || definition.id() == null) continue;
            result.add(new WardrobeGlowEntry(
                    definition.id(),
                    definition.displayName(),
                    safe(definition.colors()),
                    owned(data, definition),
                    definition.id().equals(data.glow),
                    safe(definition.equipLore()),
                    safe(definition.unequipLore()),
                    !owned(data, definition) && definition.price() > 0L
                            ? List.of("{\"text\":\"Click to buy for " + EconomyService.format(definition.price()) + "\",\"color\":\"gold\"}")
                            : safe(definition.lockedLore()),
                    definition.displayNameJson()
            ));
        }
        return result;
    }

    private static List<WardrobePresetSummary> presetEntries(PlayerDataStore.PlayerData data) {
        List<WardrobePresetSummary> result = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            PlayerDataStore.PresetData preset = data.presets.get(Integer.toString(index));
            Map<WardrobeSlot, String> preview = new EnumMap<>(WardrobeSlot.class);
            Map<WardrobeSlot, Integer> colors = new EnumMap<>(WardrobeSlot.class);
            if (preset != null && preset.equipped != null) {
                for (Map.Entry<String, String> entry : preset.equipped.entrySet()) {
                    try {
                        WardrobeSlot slot = WardrobeSlot.valueOf(entry.getKey());
                        preview.put(slot, entry.getValue());
                        Integer color = preset.colors == null ? null : preset.colors.get(entry.getValue());
                        if (color != null) colors.put(slot, color);
                    } catch (IllegalArgumentException ignored) {
                    }
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
        if (id == null) return null;
        if ("gengar_grin_mask".equals(id)) id = "gengar-hat";
        for (ServerConfig.CosmeticDefinition definition : CobbleClubServer.config().cosmetics) {
            if (definition != null && id.equals(definition.id())) return definition;
        }
        return null;
    }

    private static WardrobeSlot slot(ServerConfig.CosmeticDefinition definition) {
        if (definition == null || definition.slot() == null) return null;
        try {
            return WardrobeSlot.valueOf(definition.slot().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ServerConfig.GlowDefinition glow(String id) {
        if (id == null) return null;
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) {
            if (definition != null && id.equals(definition.id())) return definition;
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
