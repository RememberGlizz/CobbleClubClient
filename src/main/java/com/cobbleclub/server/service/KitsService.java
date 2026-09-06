package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Server-authoritative first-join and LuckPerms rank kits. */
public final class KitsService {
    private static final Gson GSON = new Gson();
    private static final List<String> KIT_IDS = List.of("newb", "ace", "champion", "master", "legend");

    private KitsService() {}

    public static void onJoin(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        // The first-join Newb kit belongs only to the LuckPerms default group.
        if (!config.newbKitEnabled || data.newbKitReceived || !"default".equals(RankAccessService.group(player))) return;

        ItemStack claimTool = ClubItems.stack("claiming_tool", 1);
        if (claimTool.isEmpty()) {
            CobbleClubServer.LOGGER.error("Newb Kit for {} was not marked delivered because the claiming tool could not be created", player.getGameProfile().getName());
            player.sendMessage(Text.literal("Your Newb Kit could not be delivered. Please tell an administrator."), false);
            return;
        }
        Delivery delivery = deliver(player, "newb", config);
        markClaimed(data, "newb");
        PlayerDataStore.save();
        player.sendMessage(Text.literal("Welcome to CobbleClub! Your first Newb Kit was delivered automatically."), false);
        player.sendMessage(Text.literal("The free kit is available again in 12 hours from /kits."), false);
        warnMissing(player, delivery);
    }

    public static void open(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, Payloads.KitsOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerPlayNetworking.send(player, new Payloads.KitsOpen(state(player, "", false)));
    }

    public static void handle(ServerPlayerEntity player, String rawAction) {
        String action = rawAction == null ? "" : rawAction.trim().toLowerCase(Locale.ROOT);
        if ("refresh".equals(action)) {
            sendState(player, "Kit rank access refreshed.", false);
            return;
        }
        if (action.startsWith("reduce:")) {
            reduceCooldown(player, action.substring("reduce:".length()));
            return;
        }
        if (!action.startsWith("claim:")) {
            sendState(player, "Unknown kit action.", true);
            return;
        }

        String kitId = action.substring("claim:".length());
        if (!KIT_IDS.contains(kitId)) {
            sendState(player, "Unknown kit.", true);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        if ("newb".equals(kitId) && !config.newbKitEnabled) {
            sendState(player, "The Newb Kit is currently disabled.", true);
            return;
        }
        if (!unlocked(player, kitId, config)) {
            sendState(player, displayName(kitId) + " Kit requires the " + displayName(kitId) + " premium rank.", true);
            return;
        }

        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        long remaining = remainingSeconds(data, kitId, config);
        if (claimedBefore(data, kitId) && remaining > 0L) {
            sendState(player, displayName(kitId) + " Kit cooldown: " + formatTime(remaining) + " remaining.", true);
            return;
        }

        Delivery delivery = deliver(player, kitId, config);
        markClaimed(data, kitId);
        PlayerDataStore.save();
        String notice = delivery.missing().isEmpty()
                ? displayName(kitId) + " Kit claimed! It will be ready again in " + cooldownLabel(kitId) + "."
                : displayName(kitId) + " claimed, but missing items: " + String.join(", ", delivery.missing());
        sendState(player, notice, !delivery.missing().isEmpty());
        warnMissing(player, delivery);
    }

    public static boolean isClaimingTool(ItemStack stack) {
        return ClubItems.matches(stack, "claiming_tool");
    }

    public static boolean hasClaimingTool(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (isClaimingTool(player.getInventory().getStack(slot))) return true;
        }
        return false;
    }

    /** Gives a replacement only when the player does not already possess one. */
    public static boolean recoverClaimingTool(ServerPlayerEntity player) {
        if (hasClaimingTool(player)) return false;
        return ClubItems.give(player, "claiming_tool", 1);
    }

    private static void reduceCooldown(ServerPlayerEntity player, String kitId) {
        if (!KIT_IDS.contains(kitId)) {
            sendState(player, "Unknown kit.", true);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        if (!unlocked(player, kitId, config)) {
            sendState(player, "You do not have access to the " + displayName(kitId) + " Kit.", true);
            return;
        }
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        long remaining = remainingSeconds(data, kitId, config);
        if (!claimedBefore(data, kitId) || remaining <= 0L) {
            sendState(player, "That kit is already ready; no cooldown purchase is needed.", true);
            return;
        }
        long purchased = Math.max(0L, data.kitCooldownReductions.getOrDefault(kitId, 0L));
        long maximum = reductionMaximum(kitId, config);
        if (purchased >= maximum) {
            sendState(player, "Cooldown reduction cap reached for this " + displayName(kitId) + " Kit cycle.", true);
            return;
        }
        long step = Math.max(1L, config.kitCooldownReductionStepSeconds);
        long reduction = Math.min(Math.min(step, maximum - purchased), remaining);
        long cost = proportionalCost(config.kitCooldownReductionPrice, reduction, step);
        if (reduction <= 0L || !EconomyService.withdraw(player, cost)) {
            sendState(player, "You need " + EconomyService.format(cost) + " to reduce this cooldown.", true);
            return;
        }
        data.kitCooldownReductions.put(kitId, purchased + reduction);
        data.revision++;
        PlayerDataStore.save();
        sendState(player, "Removed " + formatTime(reduction) + " for " + EconomyService.format(cost) + ".", false);
    }

    private static Delivery deliver(ServerPlayerEntity player, String kitId, ServerConfig config) {
        List<String> missing = new ArrayList<>();
        switch (kitId) {
            case "newb" -> {
                giveCustom(player, "claiming_tool", 1, missing);
                giveRegistry(player, "minecraft:diamond_pickaxe", 1, missing);
                giveRegistry(player, config.newbKitPokedexItem, 1, missing);
                giveRegistry(player, config.newbKitPokeBallItem, 20, missing);
                giveRegistry(player, config.newbKitGreatBallItem, 10, missing);
                giveRegistry(player, config.newbKitUltraBallItem, 6, missing);
                giveRegistry(player, "minecraft:cooked_beef", 32, missing);
                giveRegistry(player, config.newbKitPcItem, 1, missing);
                giveRegistry(player, config.newbKitHealingMachineItem, 1, missing);
            }
            case "ace" -> {
                giveKeys(player, "shiny", 2, missing);
                giveRegistry(player, config.kitRareCandyItem, 6, missing);
                giveRegistry(player, config.newbKitUltraBallItem, 6, missing);
                giveRegistry(player, config.kitQuickBallItem, 12, missing);
                giveRegistry(player, "minecraft:experience_bottle", 50, missing);
            }
            case "champion" -> {
                giveKeys(player, "legendary", 1, missing);
                giveKeys(player, "shiny", 1, missing);
                giveRegistry(player, config.kitRareCandyItem, 8, missing);
                giveRegistry(player, config.newbKitUltraBallItem, 8, missing);
                giveRegistry(player, config.kitQuickBallItem, 16, missing);
                giveRegistry(player, "minecraft:experience_bottle", 64, missing);
            }
            case "master" -> {
                giveRegistry(player, config.kitBeastBallItem, 2, missing);
                giveKeys(player, "legendary", 1, missing);
                giveKeys(player, "shiny", 1, missing);
                giveKeys(player, "vote", 1, missing);
                giveRegistry(player, config.kitRareCandyItem, 16, missing);
                giveRegistry(player, config.kitQuickBallItem, 32, missing);
                giveRegistry(player, "minecraft:experience_bottle", 96, missing);
            }
            case "legend" -> {
                giveRegistry(player, config.kitMasterBallItem, 1, missing);
                giveKeys(player, "shiny", 1, missing);
                giveKeys(player, "vote", 3, missing);
                giveRegistry(player, config.kitRareCandyItem, 46, missing);
                giveRegistry(player, config.kitQuickBallItem, 46, missing);
                giveRegistry(player, "minecraft:experience_bottle", 178, missing);
            }
            default -> missing.add("unknown-kit:" + kitId);
        }
        return new Delivery(missing);
    }

    private static void giveKeys(ServerPlayerEntity player, String crate, int amount, List<String> missing) {
        if (!CrateService.giveKeys(player, crate, amount)) missing.add("crate-key:" + crate);
    }

    private static void giveCustom(ServerPlayerEntity player, String id, int amount, List<String> missing) {
        ItemStack stack = ClubItems.stack(id, amount);
        if (stack.isEmpty()) {
            missing.add("cobbleclub:" + id);
            return;
        }
        giveStack(player, stack);
    }

    private static void giveRegistry(ServerPlayerEntity player, String rawId, int amount, List<String> missing) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null || !Registries.ITEM.containsId(id)) {
            missing.add(rawId == null ? "unknown" : rawId);
            return;
        }
        Item item = Registries.ITEM.get(id);
        int left = Math.max(1, amount);
        int max = Math.max(1, item.getMaxCount());
        while (left > 0) {
            int count = Math.min(left, max);
            giveStack(player, new ItemStack(item, count));
            left -= count;
        }
    }

    private static void giveStack(ServerPlayerEntity player, ItemStack stack) {
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) player.dropItem(stack, false);
    }

    private static void warnMissing(ServerPlayerEntity player, Delivery delivery) {
        if (!delivery.missing().isEmpty()) {
            player.sendMessage(Text.literal("Some configured kit items are unavailable: " + String.join(", ", delivery.missing())), false);
        }
    }

    private static void sendState(ServerPlayerEntity player, String notice, boolean error) {
        if (ServerPlayNetworking.canSend(player, Payloads.KitsState.ID)) {
            ServerPlayNetworking.send(player, new Payloads.KitsState(state(player, notice, error)));
        }
    }

    private static String state(ServerPlayerEntity player, String notice, boolean error) {
        ServerConfig config = CobbleClubServer.config();
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        JsonObject root = new JsonObject();
        root.addProperty("balanceText", EconomyService.format(EconomyService.balance(player)));
        root.addProperty("reductionPriceText", EconomyService.format(config.kitCooldownReductionPrice));
        root.addProperty("reductionStepSeconds", Math.max(1L, config.kitCooldownReductionStepSeconds));
        JsonArray kits = new JsonArray();
        for (String kitId : KIT_IDS) {
            long remaining = remainingSeconds(data, kitId, config);
            boolean unlocked = unlocked(player, kitId, config);
            JsonObject kit = new JsonObject();
            kit.addProperty("id", kitId);
            kit.addProperty("displayName", displayName(kitId));
            kit.addProperty("permission", permission(kitId, config));
            kit.addProperty("unlocked", unlocked);
            kit.addProperty("available", unlocked && (!claimedBefore(data, kitId) || remaining <= 0L));
            kit.addProperty("cooldownRemainingSeconds", remaining);
            kit.addProperty("cooldownTotalSeconds", cooldownSeconds(kitId, config));
            kit.addProperty("reductionPurchasedSeconds", Math.max(0L, data.kitCooldownReductions.getOrDefault(kitId, 0L)));
            kit.addProperty("reductionMaxSeconds", reductionMaximum(kitId, config));
            kits.add(kit);
        }
        root.add("kits", kits);
        root.addProperty("luckPermsExpected", true);
        root.addProperty("notice", notice == null ? "" : notice);
        root.addProperty("error", error);
        return GSON.toJson(root);
    }

    private static boolean unlocked(ServerPlayerEntity player, String kitId, ServerConfig config) {
        if ("newb".equals(kitId)) {
            return config.newbKitEnabled && "default".equals(RankAccessService.group(player));
        }
        // Server-authoritative exact-rank gate. A Champion cannot claim Ace/Master/Legend,
        // even if LuckPerms inheritance happens to expose more than one kit permission.
        return kitId.equals(RankAccessService.premiumRank(player))
                && RankPermissionService.has(player, permission(kitId, config));
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
        return "legend".equals(kitId)
                ? Math.max(0L, config.legendKitCooldownReductionMaxSeconds)
                : Math.max(0L, config.kitCooldownReductionMaxSeconds);
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
        data.revision++;
    }

    private static long remainingSeconds(PlayerDataStore.PlayerData data, String kitId, ServerConfig config) {
        if (!claimedBefore(data, kitId)) return 0L;
        long last = data.kitClaimTimes.getOrDefault(kitId,
                "newb".equals(kitId) ? data.lastNewbKitClaimMillis : 0L);
        long reduction = Math.max(0L, data.kitCooldownReductions.getOrDefault(kitId, 0L));
        long remainingMillis = last + Math.max(0L, cooldownSeconds(kitId, config) - reduction) * 1000L - System.currentTimeMillis();
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
        return hours > 0L ? hours + "h " + minutes + "m" : minutes > 0L ? minutes + "m " + secs + "s" : secs + "s";
    }

    private static long proportionalCost(long fullPrice, long reductionSeconds, long stepSeconds) {
        if (fullPrice <= 0L || reductionSeconds <= 0L) return 0L;
        if (reductionSeconds >= stepSeconds) return fullPrice;
        double proportional = Math.ceil((double) fullPrice * (double) reductionSeconds / (double) stepSeconds);
        return proportional >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, (long) proportional);
    }

    private record Delivery(List<String> missing) {}
}
