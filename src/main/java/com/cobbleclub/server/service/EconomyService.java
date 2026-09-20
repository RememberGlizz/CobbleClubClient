/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public final class EconomyService {
    private static int saveTicks;

    private EconomyService() {
    }

    public static void initialize(MinecraftServer server) {
    }

    public static PlayerDataStore.PlayerData data(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        data.lastKnownName = player.getGameProfile().getName();
        if (!data.economyInitialized) {
            data.balance = Math.max(0L, CobbleClubServer.config().startingBalance);
            data.gems = Math.max(0L, CobbleClubServer.config().startingGems);
            data.economyInitialized = true;
        }
        return data;
    }

    public static long balance(ServerPlayerEntity player) {
        return EconomyService.data((ServerPlayerEntity)player).balance;
    }

    public static void deposit(ServerPlayerEntity player, long amount) {
        if (amount <= 0L) {
            return;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.balance = EconomyService.safeAdd(data.balance, amount);
        ++data.revision;
        PlayerDataStore.save();
    }

    public static void deposit(UUID uuid, String fallbackName, long amount) {
        if (uuid == null || amount <= 0L) {
            return;
        }
        PlayerDataStore.PlayerData data = PlayerDataStore.get(uuid);
        data.normalize();
        if (fallbackName != null && !fallbackName.isBlank()) {
            data.lastKnownName = fallbackName;
        }
        if (!data.economyInitialized) {
            data.balance = Math.max(0L, CobbleClubServer.config().startingBalance);
            data.gems = Math.max(0L, CobbleClubServer.config().startingGems);
            data.economyInitialized = true;
        }
        data.balance = EconomyService.safeAdd(data.balance, amount);
        ++data.revision;
        PlayerDataStore.save();
    }

    public static boolean withdraw(ServerPlayerEntity player, long amount) {
        if (amount < 0L) {
            return false;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        if (data.balance < amount) {
            return false;
        }
        data.balance -= amount;
        ++data.revision;
        PlayerDataStore.save();
        return true;
    }

    public static void setBalance(ServerPlayerEntity player, long amount) {
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.balance = Math.max(0L, amount);
        ++data.revision;
        PlayerDataStore.save();
    }

    public static long takeUpTo(ServerPlayerEntity player, long amount) {
        if (amount <= 0L) {
            return EconomyService.balance(player);
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.balance = Math.max(0L, data.balance - amount);
        ++data.revision;
        PlayerDataStore.save();
        return data.balance;
    }

    public static long gems(ServerPlayerEntity player) {
        return Math.max(0L, EconomyService.data((ServerPlayerEntity)player).gems);
    }

    public static void depositGems(ServerPlayerEntity player, long amount) {
        if (amount <= 0L) {
            return;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.gems = EconomyService.safeAdd(data.gems, amount);
        ++data.revision;
        PlayerDataStore.save();
    }

    public static boolean withdrawGems(ServerPlayerEntity player, long amount) {
        if (amount < 0L) {
            return false;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        if (data.gems < amount) {
            return false;
        }
        data.gems -= amount;
        ++data.revision;
        PlayerDataStore.save();
        return true;
    }

    public static boolean pay(ServerPlayerEntity sender, ServerPlayerEntity recipient, long amount) {
        if (sender == recipient || amount <= 0L || !EconomyService.withdraw(sender, amount)) {
            return false;
        }
        EconomyService.deposit(recipient, amount);
        sender.sendMessage((Text)Text.literal((String)("Paid " + recipient.getGameProfile().getName() + " " + EconomyService.format(amount) + ".")), false);
        recipient.sendMessage((Text)Text.literal((String)(sender.getGameProfile().getName() + " paid you " + EconomyService.format(amount) + ".")), false);
        return true;
    }

    public static boolean claimDaily(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        if (!config.dailyRewardsEnabled) {
            return false;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        long now = System.currentTimeMillis();
        if (now - data.lastDailyClaimMillis < Math.max(1L, config.dailyCooldownSeconds) * 1000L) {
            return false;
        }
        data.lastDailyClaimMillis = now;
        data.balance = EconomyService.safeAdd(data.balance, Math.max(0L, config.dailyMoney));
        data.bonusClaimBlocks = EconomyService.safeIntAdd(data.bonusClaimBlocks, Math.max(0, config.dailyClaimBlocks));
        data.gems = EconomyService.safeAdd(data.gems, Math.max(0, config.dailyGems));
        ++data.revision;
        PlayerDataStore.save();
        player.sendMessage((Text)Text.literal((String)("Daily reward: " + EconomyService.format(config.dailyMoney) + ", " + config.dailyGems + " gems and " + config.dailyClaimBlocks + " claim blocks.")), false);
        return true;
    }

    public static boolean dailyAvailable(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        return config.dailyRewardsEnabled && System.currentTimeMillis() - EconomyService.data((ServerPlayerEntity)player).lastDailyClaimMillis >= Math.max(1L, config.dailyCooldownSeconds) * 1000L;
    }

    public static long secondsUntilNextPlaytimeReward(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        if (config.claimBlocksPerReward <= 0 && config.gemsPerPlaytimeReward <= 0 || config.claimBlockRewardIntervalSeconds <= 0) {
            return -1L;
        }
        long intervalTicks = (long)config.claimBlockRewardIntervalSeconds * 20L;
        long elapsed = Math.floorMod(EconomyService.data((ServerPlayerEntity)player).rewardedPlayTicks, intervalTicks);
        long ticksLeft = elapsed == 0L && EconomyService.data((ServerPlayerEntity)player).rewardedPlayTicks > 0L ? intervalTicks : intervalTicks - elapsed;
        return Math.max(1L, (ticksLeft + 19L) / 20L);
    }

    public static void tick(MinecraftServer server) {
        ServerConfig config = CobbleClubServer.config();
        int interval = Math.max(1, config.claimBlockRewardIntervalSeconds) * 20;
        boolean dirty = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDataStore.PlayerData data = EconomyService.data(player);
            ++data.rewardedPlayTicks;
            ++data.moneyRewardedPlayTicks;
            if ((config.claimBlocksPerReward > 0 || config.gemsPerPlaytimeReward > 0) && data.rewardedPlayTicks % (long)interval == 0L) {
                data.bonusClaimBlocks = EconomyService.safeIntAdd(data.bonusClaimBlocks, Math.max(0, config.claimBlocksPerReward));
                data.gems = EconomyService.safeAdd(data.gems, Math.max(0, config.gemsPerPlaytimeReward));
                ++data.revision;
                String reward = config.gemsPerPlaytimeReward + " gems";
                if (config.claimBlocksPerReward > 0) {
                    reward = reward + " and " + config.claimBlocksPerReward + " claim blocks";
                }
                player.sendMessage((Text)Text.literal((String)("CobbleClub playtime reward: +" + reward + ".")), true);
                dirty = true;
            }
            long moneyInterval = (long)Math.max(1, config.playtimeMoneyIntervalSeconds) * 20L;
            if (config.playtimeMoneyReward <= 0L || data.moneyRewardedPlayTicks % moneyInterval != 0L) continue;
            data.balance = EconomyService.safeAdd(data.balance, config.playtimeMoneyReward);
            ++data.revision;
            player.sendMessage((Text)Text.literal((String)("CobbleClub playtime reward: +" + EconomyService.format(config.playtimeMoneyReward) + ".")), true);
            dirty = true;
        }
        if (++saveTicks >= 1200) {
            saveTicks = 0;
            dirty = true;
        }
        if (dirty) {
            PlayerDataStore.save();
        }
    }

    public static void handleDeath(ServerPlayerEntity player) {
        long penalty = Math.max(0L, CobbleClubServer.config().deathMoneyPenalty);
        if (penalty == 0L || EconomyService.balance(player) <= penalty) {
            return;
        }
        if (EconomyService.withdraw(player, penalty)) {
            player.sendMessage((Text)Text.literal((String)("Death penalty: -" + EconomyService.format(penalty) + ".")), false);
        }
    }

    public static List<BalanceEntry> topBalances(int limit) {
        ArrayList<BalanceEntry> entries = new ArrayList<BalanceEntry>();
        for (Map.Entry<String, PlayerDataStore.PlayerData> row : PlayerDataStore.all().entrySet()) {
            PlayerDataStore.PlayerData data = row.getValue();
            if (data == null) continue;
            data.normalize();
            if (!data.economyInitialized) continue;
            Object name = data.lastKnownName;
            if (name == null || ((String)name).isBlank()) {
                try {
                    String compact = UUID.fromString(row.getKey()).toString().replace("-", "");
                    name = "Player-" + compact.substring(0, 6);
                }
                catch (Exception ignored) {
                    name = "Unknown Player";
                }
            }
            entries.add(new BalanceEntry((String)name, Math.max(0L, data.balance)));
        }
        entries.sort(Comparator.comparingLong(BalanceEntry::balance).reversed().thenComparing(BalanceEntry::name, String.CASE_INSENSITIVE_ORDER));
        if (entries.size() > Math.max(1, limit)) {
            return List.copyOf(entries.subList(0, Math.max(1, limit)));
        }
        return List.copyOf(entries);
    }

    public static String format(long amount) {
        ServerConfig config = CobbleClubServer.config();
        return config.currencySymbol + String.format("%,d", Math.max(0L, amount)) + " " + config.currencyName;
    }

    public static String formatGems(long amount) {
        return String.format("%,d gem%s", Math.max(0L, amount), amount == 1L ? "" : "s");
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, left + right);
    }

    private static int safeIntAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, left + right);
    }

    public record BalanceEntry(String name, long balance) {
    }
}

