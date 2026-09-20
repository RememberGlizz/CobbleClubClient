/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_7923
 *  net.minecraft.class_8710
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import com.cobbleclub.server.service.ClubItems;
import com.cobbleclub.server.service.CrateService;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.RankAccessService;
import com.cobbleclub.server.service.RankPermissionService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.CustomPayload;

public final class KitsService {
    private static final Gson GSON = new Gson();
    private static final List<String> KIT_IDS = List.of("newb", "ace", "champion", "master", "legend");

    private KitsService() {
    }

    public static void onJoin(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        if (!config.newbKitEnabled || data.newbKitReceived || !"default".equals(RankAccessService.group(player))) {
            return;
        }
        ItemStack claimTool = ClubItems.stack("claiming_tool", 1);
        if (claimTool.isEmpty()) {
            CobbleClubServer.LOGGER.error("Newb Kit for {} was not marked delivered because the claiming tool could not be created", (Object)player.getGameProfile().getName());
            player.sendMessage((Text)Text.literal((String)"Your Newb Kit could not be delivered. Please tell an administrator."), false);
            return;
        }
        Delivery delivery = KitsService.deliver(player, "newb", config);
        KitsService.markClaimed(data, "newb");
        PlayerDataStore.save();
        player.sendMessage((Text)Text.literal((String)"Welcome to CobbleClub! Your first Newb Kit was delivered automatically."), false);
        player.sendMessage((Text)Text.literal((String)"The free kit is available again in 12 hours from /kits."), false);
        KitsService.warnMissing(player, delivery);
    }

    public static void open(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.KitsOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.KitsOpen(KitsService.state(player, "", false)));
    }

    public static void handle(ServerPlayerEntity player, String rawAction) {
        String action;
        String string = action = rawAction == null ? "" : rawAction.trim().toLowerCase(Locale.ROOT);
        if ("refresh".equals(action)) {
            KitsService.sendState(player, "Kit rank access refreshed.", false);
            return;
        }
        if (action.startsWith("reduce:")) {
            KitsService.reduceCooldown(player, action.substring("reduce:".length()));
            return;
        }
        if (!action.startsWith("claim:")) {
            KitsService.sendState(player, "Unknown kit action.", true);
            return;
        }
        String kitId = action.substring("claim:".length());
        if (!KIT_IDS.contains(kitId)) {
            KitsService.sendState(player, "Unknown kit.", true);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        if ("newb".equals(kitId) && !config.newbKitEnabled) {
            KitsService.sendState(player, "The Newb Kit is currently disabled.", true);
            return;
        }
        if (!KitsService.unlocked(player, kitId, config)) {
            KitsService.sendState(player, KitsService.displayName(kitId) + " Kit requires the " + KitsService.displayName(kitId) + " premium rank.", true);
            return;
        }
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        long remaining = KitsService.remainingSeconds(data, kitId, config);
        if (KitsService.claimedBefore(data, kitId) && remaining > 0L) {
            KitsService.sendState(player, KitsService.displayName(kitId) + " Kit cooldown: " + KitsService.formatTime(remaining) + " remaining.", true);
            return;
        }
        Delivery delivery = KitsService.deliver(player, kitId, config);
        KitsService.markClaimed(data, kitId);
        PlayerDataStore.save();
        String notice = delivery.missing().isEmpty() ? KitsService.displayName(kitId) + " Kit claimed! It will be ready again in " + KitsService.cooldownLabel(kitId) + "." : KitsService.displayName(kitId) + " claimed, but missing items: " + String.join((CharSequence)", ", delivery.missing());
        KitsService.sendState(player, notice, !delivery.missing().isEmpty());
        KitsService.warnMissing(player, delivery);
    }

    public static boolean isClaimingTool(ItemStack stack) {
        return ClubItems.matches(stack, "claiming_tool");
    }

    public static boolean hasClaimingTool(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); ++slot) {
            if (!KitsService.isClaimingTool(player.getInventory().getStack(slot))) continue;
            return true;
        }
        return false;
    }

    public static boolean recoverClaimingTool(ServerPlayerEntity player) {
        if (KitsService.hasClaimingTool(player)) {
            return false;
        }
        return ClubItems.give(player, "claiming_tool", 1);
    }

    private static void reduceCooldown(ServerPlayerEntity player, String kitId) {
        long maximum;
        if (!KIT_IDS.contains(kitId)) {
            KitsService.sendState(player, "Unknown kit.", true);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        if (!KitsService.unlocked(player, kitId, config)) {
            KitsService.sendState(player, "You do not have access to the " + KitsService.displayName(kitId) + " Kit.", true);
            return;
        }
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        long remaining = KitsService.remainingSeconds(data, kitId, config);
        if (!KitsService.claimedBefore(data, kitId) || remaining <= 0L) {
            KitsService.sendState(player, "That kit is already ready; no cooldown purchase is needed.", true);
            return;
        }
        long purchased = Math.max(0L, data.kitCooldownReductions.getOrDefault(kitId, 0L));
        if (purchased >= (maximum = KitsService.reductionMaximum(kitId, config))) {
            KitsService.sendState(player, "Cooldown reduction cap reached for this " + KitsService.displayName(kitId) + " Kit cycle.", true);
            return;
        }
        long step = Math.max(1L, config.kitCooldownReductionStepSeconds);
        long reduction = Math.min(Math.min(step, maximum - purchased), remaining);
        long cost = KitsService.proportionalCost(config.kitCooldownReductionPrice, reduction, step);
        if (reduction <= 0L || !EconomyService.withdraw(player, cost)) {
            KitsService.sendState(player, "You need " + EconomyService.format(cost) + " to reduce this cooldown.", true);
            return;
        }
        data.kitCooldownReductions.put(kitId, purchased + reduction);
        ++data.revision;
        PlayerDataStore.save();
        KitsService.sendState(player, "Removed " + KitsService.formatTime(reduction) + " for " + EconomyService.format(cost) + ".", false);
    }

    private static Delivery deliver(ServerPlayerEntity player, String kitId, ServerConfig config) {
        ArrayList<String> missing = new ArrayList<String>();
        switch (kitId) {
            case "newb": {
                KitsService.giveCustom(player, "claiming_tool", 1, missing);
                KitsService.giveRegistry(player, "minecraft:diamond_pickaxe", 1, missing);
                KitsService.giveRegistry(player, "minecraft:fishing_rod", 1, missing);
                KitsService.giveRegistry(player, config.newbKitPokedexItem, 1, missing);
                KitsService.giveRegistry(player, config.newbKitPokeBallItem, 20, missing);
                KitsService.giveRegistry(player, config.newbKitGreatBallItem, 10, missing);
                KitsService.giveRegistry(player, config.newbKitUltraBallItem, 6, missing);
                KitsService.giveRegistry(player, "minecraft:cooked_beef", 32, missing);
                KitsService.giveRegistry(player, config.newbKitPcItem, 1, missing);
                KitsService.giveRegistry(player, config.newbKitHealingMachineItem, 1, missing);
                KitsService.giveRegistry(player, config.newbKitTrainerCardItem, 1, missing);
                break;
            }
            case "ace": {
                KitsService.giveCustom(player, "pond_rod", 1, missing);
                KitsService.giveArmor(player, "spark", missing);
                KitsService.giveKeys(player, "shiny", 2, missing);
                KitsService.giveRegistry(player, config.kitRareCandyItem, 6, missing);
                KitsService.giveRegistry(player, config.newbKitUltraBallItem, 6, missing);
                KitsService.giveRegistry(player, config.kitQuickBallItem, 12, missing);
                KitsService.giveRegistry(player, "minecraft:experience_bottle", 50, missing);
                break;
            }
            case "champion": {
                KitsService.giveCustom(player, "pond_rod", 1, missing);
                KitsService.giveArmor(player, "spectral", missing);
                KitsService.giveKeys(player, "legendary", 1, missing);
                KitsService.giveKeys(player, "shiny", 1, missing);
                KitsService.giveRegistry(player, config.kitRareCandyItem, 8, missing);
                KitsService.giveRegistry(player, config.newbKitUltraBallItem, 8, missing);
                KitsService.giveRegistry(player, config.kitQuickBallItem, 16, missing);
                KitsService.giveRegistry(player, "minecraft:experience_bottle", 64, missing);
                break;
            }
            case "master": {
                KitsService.giveCustom(player, "pond_rod", 1, missing);
                KitsService.giveArmor(player, "aura", missing);
                KitsService.giveRegistry(player, config.kitBeastBallItem, 2, missing);
                KitsService.giveKeys(player, "legendary", 1, missing);
                KitsService.giveKeys(player, "shiny", 1, missing);
                KitsService.giveKeys(player, "vote", 1, missing);
                KitsService.giveRegistry(player, config.kitRareCandyItem, 16, missing);
                KitsService.giveRegistry(player, config.kitQuickBallItem, 32, missing);
                KitsService.giveRegistry(player, "minecraft:experience_bottle", 96, missing);
                break;
            }
            case "legend": {
                KitsService.giveCustom(player, "pond_rod", 1, missing);
                KitsService.giveArmor(player, "fairy", missing);
                KitsService.giveRegistry(player, config.kitMasterBallItem, 1, missing);
                KitsService.giveKeys(player, "shiny", 1, missing);
                KitsService.giveKeys(player, "vote", 3, missing);
                KitsService.giveRegistry(player, config.kitRareCandyItem, 46, missing);
                KitsService.giveRegistry(player, config.kitQuickBallItem, 46, missing);
                KitsService.giveRegistry(player, "minecraft:experience_bottle", 178, missing);
                break;
            }
            default: {
                missing.add("unknown-kit:" + kitId);
            }
        }
        return new Delivery(missing);
    }

    private static void giveKeys(ServerPlayerEntity player, String crate, int amount, List<String> missing) {
        if (!CrateService.giveKeys(player, crate, amount)) {
            missing.add("crate-key:" + crate);
        }
    }

    private static void giveArmor(ServerPlayerEntity player, String set, List<String> missing) {
        if (!ClubItems.giveArmorSet(player, set)) {
            missing.add("armor-set:" + set);
        }
    }

    private static void giveCustom(ServerPlayerEntity player, String id, int amount, List<String> missing) {
        ItemStack stack = ClubItems.stack(id, amount);
        if (stack.isEmpty()) {
            missing.add("cobbleclub:" + id);
            return;
        }
        KitsService.giveStack(player, stack);
    }

    private static void giveRegistry(ServerPlayerEntity player, String rawId, int amount, List<String> missing) {
        int count;
        Identifier id = Identifier.tryParse((String)rawId);
        if (id == null || !Registries.ITEM.containsId(id)) {
            missing.add(rawId == null ? "unknown" : rawId);
            return;
        }
        Item item = (Item)Registries.ITEM.get(id);
        int max = Math.max(1, item.getMaxCount());
        for (int left = Math.max(1, amount); left > 0; left -= count) {
            count = Math.min(left, max);
            KitsService.giveStack(player, new ItemStack((ItemConvertible)item, count));
        }
    }

    private static void giveStack(ServerPlayerEntity player, ItemStack stack) {
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) {
            player.dropItem(stack, false);
        }
    }

    private static void warnMissing(ServerPlayerEntity player, Delivery delivery) {
        if (!delivery.missing().isEmpty()) {
            player.sendMessage((Text)Text.literal((String)("Some configured kit items are unavailable: " + String.join((CharSequence)", ", delivery.missing()))), false);
        }
    }

    private static void sendState(ServerPlayerEntity player, String notice, boolean error) {
        if (ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.KitsState.ID)) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.KitsState(KitsService.state(player, notice, error)));
        }
    }

    private static String state(ServerPlayerEntity player, String notice, boolean error) {
        ServerConfig config = CobbleClubServer.config();
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        JsonObject root = new JsonObject();
        root.addProperty("balanceText", EconomyService.format(EconomyService.balance(player)));
        root.addProperty("reductionPriceText", EconomyService.format(config.kitCooldownReductionPrice));
        root.addProperty("reductionStepSeconds", (Number)Math.max(1L, config.kitCooldownReductionStepSeconds));
        JsonArray kits = new JsonArray();
        for (String kitId : KIT_IDS) {
            long remaining = KitsService.remainingSeconds(data, kitId, config);
            boolean unlocked = KitsService.unlocked(player, kitId, config);
            JsonObject kit = new JsonObject();
            kit.addProperty("id", kitId);
            kit.addProperty("displayName", KitsService.displayName(kitId));
            kit.addProperty("permission", KitsService.permission(kitId, config));
            kit.addProperty("unlocked", Boolean.valueOf(unlocked));
            kit.addProperty("available", Boolean.valueOf(unlocked && (!KitsService.claimedBefore(data, kitId) || remaining <= 0L)));
            kit.addProperty("cooldownRemainingSeconds", (Number)remaining);
            kit.addProperty("cooldownTotalSeconds", (Number)KitsService.cooldownSeconds(kitId, config));
            kit.addProperty("reductionPurchasedSeconds", (Number)Math.max(0L, data.kitCooldownReductions.getOrDefault(kitId, 0L)));
            kit.addProperty("reductionMaxSeconds", (Number)KitsService.reductionMaximum(kitId, config));
            kits.add((JsonElement)kit);
        }
        root.add("kits", (JsonElement)kits);
        root.addProperty("luckPermsExpected", Boolean.valueOf(true));
        root.addProperty("notice", notice == null ? "" : notice);
        root.addProperty("error", Boolean.valueOf(error));
        return GSON.toJson((JsonElement)root);
    }

    private static boolean unlocked(ServerPlayerEntity player, String kitId, ServerConfig config) {
        if ("newb".equals(kitId)) {
            return config.newbKitEnabled && "default".equals(RankAccessService.group(player));
        }
        return kitId.equals(RankAccessService.premiumRank(player)) && RankPermissionService.has(player, KitsService.permission(kitId, config));
    }

    private static String permission(String kitId, ServerConfig config) {
        return switch (kitId) {
            case "ace" -> config.aceKitPermission;
            case "champion" -> config.championKitPermission;
            case "master" -> config.masterKitPermission;
            case "legend" -> config.legendKitPermission;
            default -> "";
        };
    }

    private static long cooldownSeconds(String kitId, ServerConfig config) {
        return "newb".equals(kitId) ? Math.max(0L, config.newbKitCooldownSeconds) : Math.max(0L, config.rankKitCooldownSeconds);
    }

    private static long reductionMaximum(String kitId, ServerConfig config) {
        return "legend".equals(kitId) ? Math.max(0L, config.legendKitCooldownReductionMaxSeconds) : Math.max(0L, config.kitCooldownReductionMaxSeconds);
    }

    private static String cooldownLabel(String kitId) {
        return "newb".equals(kitId) ? "12 hours" : "18 hours";
    }

    private static boolean claimedBefore(PlayerDataStore.PlayerData data, String kitId) {
        return "newb".equals(kitId) ? data.newbKitReceived : data.kitClaimTimes.containsKey(kitId);
    }

    private static void markClaimed(PlayerDataStore.PlayerData data, String kitId) {
        long now = System.currentTimeMillis();
        data.kitClaimTimes.put(kitId, now);
        data.kitCooldownReductions.put(kitId, 0L);
        if ("newb".equals(kitId)) {
            data.newbKitReceived = true;
            data.lastNewbKitClaimMillis = now;
        }
        ++data.revision;
    }

    private static long remainingSeconds(PlayerDataStore.PlayerData data, String kitId, ServerConfig config) {
        if (!KitsService.claimedBefore(data, kitId)) {
            return 0L;
        }
        long last = data.kitClaimTimes.getOrDefault(kitId, "newb".equals(kitId) ? data.lastNewbKitClaimMillis : 0L);
        long reduction = Math.max(0L, data.kitCooldownReductions.getOrDefault(kitId, 0L));
        long remainingMillis = last + Math.max(0L, KitsService.cooldownSeconds(kitId, config) - reduction) * 1000L - System.currentTimeMillis();
        return remainingMillis <= 0L ? 0L : (remainingMillis + 999L) / 1000L;
    }

    private static String displayName(String kitId) {
        return switch (kitId) {
            case "newb" -> "Newb";
            case "ace" -> "Ace";
            case "champion" -> "Champion";
            case "master" -> "Master";
            case "legend" -> "Legend";
            default -> kitId;
        };
    }

    private static String formatTime(long seconds) {
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        return hours > 0L ? hours + "h " + minutes + "m" : (minutes > 0L ? minutes + "m " + secs + "s" : secs + "s");
    }

    private static long proportionalCost(long fullPrice, long reductionSeconds, long stepSeconds) {
        if (fullPrice <= 0L || reductionSeconds <= 0L) {
            return 0L;
        }
        if (reductionSeconds >= stepSeconds) {
            return fullPrice;
        }
        double proportional = Math.ceil((double)fullPrice * (double)reductionSeconds / (double)stepSeconds);
        return proportional >= 9.223372036854776E18 ? Long.MAX_VALUE : Math.max(1L, (long)proportional);
    }

    private record Delivery(List<String> missing) {
    }
}

