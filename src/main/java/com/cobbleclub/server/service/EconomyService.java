package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Small built-in economy used by claims, unlocks, daily rewards and crates. */
public final class EconomyService {
    private static int saveTicks;

    private EconomyService() {}

    public static void initialize(MinecraftServer server) {
        // Reserved for optional economy bridges; player wallets remain server-authoritative.
    }

    public static PlayerDataStore.PlayerData data(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        data.lastKnownName = player.getGameProfile().getName();
        if (!data.economyInitialized) {
            data.balance = Math.max(0, CobbleClubServer.config().startingBalance);
            data.gems = Math.max(0, CobbleClubServer.config().startingGems);
            data.economyInitialized = true;
        }
        return data;
    }

    public static long balance(ServerPlayerEntity player) {
        return data(player).balance;
    }

    public static void deposit(ServerPlayerEntity player, long amount) {
        if (amount <= 0) return;
        PlayerDataStore.PlayerData data = data(player);
        data.balance = safeAdd(data.balance, amount);
        data.revision++;
        PlayerDataStore.save();
    }

    public static boolean withdraw(ServerPlayerEntity player, long amount) {
        if (amount < 0) return false;
        PlayerDataStore.PlayerData data = data(player);
        if (data.balance < amount) return false;
        data.balance -= amount;
        data.revision++;
        PlayerDataStore.save();
        return true;
    }

    public static void setBalance(ServerPlayerEntity player, long amount) {
        PlayerDataStore.PlayerData data = data(player);
        data.balance = Math.max(0L, amount);
        data.revision++;
        PlayerDataStore.save();
    }

    public static long takeUpTo(ServerPlayerEntity player, long amount) {
        if (amount <= 0L) return balance(player);
        PlayerDataStore.PlayerData data = data(player);
        data.balance = Math.max(0L, data.balance - amount);
        data.revision++;
        PlayerDataStore.save();
        return data.balance;
    }

    public static long gems(ServerPlayerEntity player) {
        return Math.max(0, data(player).gems);
    }

    public static void depositGems(ServerPlayerEntity player, long amount) {
        if (amount <= 0) return;
        PlayerDataStore.PlayerData data = data(player);
        data.gems = safeAdd(data.gems, amount);
        data.revision++;
        PlayerDataStore.save();
    }

    public static boolean withdrawGems(ServerPlayerEntity player, long amount) {
        if (amount < 0) return false;
        PlayerDataStore.PlayerData data = data(player);
        if (data.gems < amount) return false;
        data.gems -= amount;
        data.revision++;
        PlayerDataStore.save();
        return true;
    }

    public static boolean pay(ServerPlayerEntity sender, ServerPlayerEntity recipient, long amount) {
        if (sender == recipient || amount <= 0 || !withdraw(sender, amount)) return false;
        deposit(recipient, amount);
        sender.sendMessage(Text.literal("Paid " + recipient.getGameProfile().getName() + " " + format(amount) + "."), false);
        recipient.sendMessage(Text.literal(sender.getGameProfile().getName() + " paid you " + format(amount) + "."), false);
        return true;
    }

    public static boolean claimDaily(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        if (!config.dailyRewardsEnabled) return false;
        PlayerDataStore.PlayerData data = data(player);
        long now = System.currentTimeMillis();
        if (now - data.lastDailyClaimMillis < Math.max(1L, config.dailyCooldownSeconds) * 1000L) return false;
        data.lastDailyClaimMillis = now;
        data.balance = safeAdd(data.balance, Math.max(0, config.dailyMoney));
        data.bonusClaimBlocks = safeIntAdd(data.bonusClaimBlocks, Math.max(0, config.dailyClaimBlocks));
        data.gems = safeAdd(data.gems, Math.max(0, config.dailyGems));
        data.revision++;
        PlayerDataStore.save();
        player.sendMessage(Text.literal("Daily reward: " + format(config.dailyMoney) + ", "
                + config.dailyGems + " gems and " + config.dailyClaimBlocks + " claim blocks."), false);
        return true;
    }

    public static boolean dailyAvailable(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        return config.dailyRewardsEnabled && System.currentTimeMillis() - data(player).lastDailyClaimMillis
                >= Math.max(1L, config.dailyCooldownSeconds) * 1000L;
    }

    public static long secondsUntilNextPlaytimeReward(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        if ((config.claimBlocksPerReward <= 0 && config.gemsPerPlaytimeReward <= 0) || config.claimBlockRewardIntervalSeconds <= 0) return -1;
        long intervalTicks = (long) config.claimBlockRewardIntervalSeconds * 20L;
        long elapsed = Math.floorMod(data(player).rewardedPlayTicks, intervalTicks);
        long ticksLeft = elapsed == 0 && data(player).rewardedPlayTicks > 0 ? intervalTicks : intervalTicks - elapsed;
        return Math.max(1, (ticksLeft + 19L) / 20L);
    }

    public static void tick(MinecraftServer server) {
        ServerConfig config = CobbleClubServer.config();
        int interval = Math.max(1, config.claimBlockRewardIntervalSeconds) * 20;
        boolean dirty = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDataStore.PlayerData data = data(player);
            data.rewardedPlayTicks++;
            data.moneyRewardedPlayTicks++;
            if ((config.claimBlocksPerReward > 0 || config.gemsPerPlaytimeReward > 0) && data.rewardedPlayTicks % interval == 0) {
                data.bonusClaimBlocks = safeIntAdd(data.bonusClaimBlocks, Math.max(0, config.claimBlocksPerReward));
                data.gems = safeAdd(data.gems, Math.max(0, config.gemsPerPlaytimeReward));
                data.revision++;
                String reward = config.gemsPerPlaytimeReward + " gems";
                if (config.claimBlocksPerReward > 0) reward += " and " + config.claimBlocksPerReward + " claim blocks";
                player.sendMessage(Text.literal("CobbleClub playtime reward: +" + reward + "."), true);
                dirty = true;
            }
            long moneyInterval = (long)Math.max(1, config.playtimeMoneyIntervalSeconds) * 20L;
            if (config.playtimeMoneyReward > 0 && data.moneyRewardedPlayTicks % moneyInterval == 0L) {
                data.balance = safeAdd(data.balance, config.playtimeMoneyReward);
                data.revision++;
                player.sendMessage(Text.literal("CobbleClub playtime reward: +" + format(config.playtimeMoneyReward) + "."), true);
                dirty = true;
            }
        }
        if (++saveTicks >= 1200) {
            saveTicks = 0;
            dirty = true;
        }
        if (dirty) PlayerDataStore.save();
    }

    public static void handleDeath(ServerPlayerEntity player) {
        long penalty = Math.max(0L, CobbleClubServer.config().deathMoneyPenalty);
        if (penalty == 0L || balance(player) <= penalty) return;
        if (withdraw(player, penalty)) player.sendMessage(Text.literal("Death penalty: -" + format(penalty) + "."), false);
    }


    public static List<BalanceEntry> topBalances(int limit) {
        List<BalanceEntry> entries = new ArrayList<>();
        for (Map.Entry<String, PlayerDataStore.PlayerData> row : PlayerDataStore.all().entrySet()) {
            PlayerDataStore.PlayerData data = row.getValue();
            if (data == null) continue;
            data.normalize();
            if (!data.economyInitialized) continue;
            String name = data.lastKnownName;
            if (name == null || name.isBlank()) {
                try {
                    String compact = UUID.fromString(row.getKey()).toString().replace("-", "");
                    name = "Player-" + compact.substring(0, 6);
                } catch (Exception ignored) {
                    name = "Unknown Player";
                }
            }
            entries.add(new BalanceEntry(name, Math.max(0L, data.balance)));
        }
        entries.sort(Comparator.comparingLong(BalanceEntry::balance).reversed().thenComparing(BalanceEntry::name, String.CASE_INSENSITIVE_ORDER));
        if (entries.size() > Math.max(1, limit)) return List.copyOf(entries.subList(0, Math.max(1, limit)));
        return List.copyOf(entries);
    }

    public record BalanceEntry(String name, long balance) {}

    public static String format(long amount) {
        ServerConfig config = CobbleClubServer.config();
        return config.currencySymbol + String.format("%,d", Math.max(0, amount)) + " " + config.currencyName;
    }

    public static String formatGems(long amount) {
        return String.format("%,d gem%s", Math.max(0, amount), amount == 1 ? "" : "s");
    }

    private static long safeAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return Math.max(0, left + right);
    }

    private static int safeIntAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) return Integer.MAX_VALUE;
        return Math.max(0, left + right);
    }
}
