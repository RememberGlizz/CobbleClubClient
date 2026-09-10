package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Active earning methods that do not depend on other players being online. */
public final class ActivityEconomyService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, ContractProgress> CONTRACTS = new HashMap<>();

    private static ActivityEconomyConfig config;
    private static Path storePath;
    private static boolean dirty;
    private static int saveTicks;

    private ActivityEconomyService() {}

    public static void initialize() {
        config = ActivityEconomyConfig.load();
    }

    public static void load(MinecraftServer server) {
        storePath = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("activity-economy.json");
        CONTRACTS.clear();
        try {
            if (Files.exists(storePath)) {
                StoreFile file = GSON.fromJson(Files.readString(storePath, StandardCharsets.UTF_8), StoreFile.class);
                if (file != null && file.players != null) CONTRACTS.putAll(file.players);
            }
        } catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not load activity economy data", error);
        }
        CONTRACTS.values().removeIf(progress -> progress == null);
        dirty = false;
        saveTicks = 0;
    }

    public static void tick(MinecraftServer server) {
        if (++saveTicks < 1200) return;
        saveTicks = 0;
        if (dirty) save();
    }

    public static void save() {
        if (storePath == null) return;
        try {
            Files.createDirectories(storePath.getParent());
            StoreFile file = new StoreFile();
            file.players = new LinkedHashMap<>(CONTRACTS);
            Files.writeString(storePath, GSON.toJson(file), StandardCharsets.UTF_8);
            dirty = false;
        } catch (IOException error) {
            CobbleClubServer.LOGGER.warn("Could not save activity economy data", error);
        }
    }

    public static int sellHand(ServerPlayerEntity player) {
        if (!enabled(player)) return 0;
        ItemStack stack = player.getMainHandStack();
        long price = priceFor(stack);
        if (stack.isEmpty() || price <= 0L) {
            player.sendMessage(Text.literal("That item cannot be sold. Use /sell prices to see some examples.").formatted(Formatting.YELLOW), false);
            return 0;
        }

        int count = stack.getCount();
        long payout = safeMultiply(price, count);
        stack.decrement(count);
        creditDeferred(player, payout);
        progress(player, ContractType.SELL, count);
        PlayerDataStore.save();
        save();

        player.sendMessage(Text.literal("Sold " + count + " item" + (count == 1 ? "" : "s") + " for " + EconomyService.format(payout) + ".").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int sellAll(ServerPlayerEntity player) {
        if (!enabled(player)) return 0;

        long payout = 0L;
        int sold = 0;
        // Main inventory only. Armor and offhand are intentionally left alone.
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            long price = priceFor(stack);
            if (price <= 0L) continue;

            int count = stack.getCount();
            payout = safeAdd(payout, safeMultiply(price, count));
            sold = safeIntAdd(sold, count);
            stack.decrement(count);
        }

        if (sold == 0 || payout <= 0L) {
            player.sendMessage(Text.literal("You do not have any sellable items in your main inventory.").formatted(Formatting.YELLOW), false);
            return 0;
        }

        creditDeferred(player, payout);
        progress(player, ContractType.SELL, sold);
        PlayerDataStore.save();
        save();

        player.sendMessage(Text.literal("Sold " + sold + " items for " + EconomyService.format(payout) + ".").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int showSellInfo(ServerPlayerEntity player) {
        if (!enabled(player)) return 0;
        player.sendMessage(Text.literal("CobbleClub Sell Shop").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("/sell hand").formatted(Formatting.YELLOW)
                .append(Text.literal(" - sell the stack in your main hand").formatted(Formatting.GRAY)), false);
        player.sendMessage(Text.literal("/sell all").formatted(Formatting.YELLOW)
                .append(Text.literal(" - sell all supported items in your main inventory").formatted(Formatting.GRAY)), false);
        player.sendMessage(Text.literal("/sell prices").formatted(Formatting.YELLOW)
                .append(Text.literal(" - show common sell prices").formatted(Formatting.GRAY)), false);
        return 1;
    }

    public static int showPrices(ServerPlayerEntity player) {
        if (!enabled(player)) return 0;
        player.sendMessage(Text.literal("Common sell prices").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Iron " + money(priceFor("minecraft:iron_ingot"))
                + "  Gold " + money(priceFor("minecraft:gold_ingot"))
                + "  Diamond " + money(priceFor("minecraft:diamond"))
                + "  Emerald " + money(priceFor("minecraft:emerald"))).formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Coal " + money(priceFor("minecraft:coal"))
                + "  Redstone " + money(priceFor("minecraft:redstone"))
                + "  Gunpowder " + money(priceFor("minecraft:gunpowder"))
                + "  Blaze Rod " + money(priceFor("minecraft:blaze_rod"))).formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Crops/fish are supported, Cobblemon apricorns sell for " + money(config.apricornSellPrice)
                + " each and berries sell for " + money(config.berrySellPrice) + " each.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Prices can be changed by the server as the economy develops.").formatted(Formatting.DARK_GRAY), false);
        return 1;
    }

    public static int showContracts(ServerPlayerEntity player) {
        if (!enabled(player)) return 0;
        ContractProgress progress = progressFor(player);
        ContractSet set = contractSet(player.getUuid(), progress.cycle);

        player.sendMessage(Text.literal("Trainer Contracts").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal(" • refresh in " + refreshText(progress.cycle)).formatted(Formatting.GRAY)), false);
        sendContract(player, "Catch Pokémon", progress.catches, set.catchTarget, set.catchReward, (progress.completedMask & 1) != 0);
        sendContract(player, "Win PvE battles", progress.battles, set.battleTarget, set.battleReward, (progress.completedMask & 2) != 0);
        sendContract(player, "Sell items", progress.soldItems, set.sellTarget, set.sellReward, (progress.completedMask & 4) != 0);
        player.sendMessage(Text.literal("Contracts pay automatically when completed.").formatted(Formatting.DARK_GRAY), false);
        return 1;
    }

    public static void onPokemonCaptured(PokemonCapturedEvent event) {
        if (config == null || !config.enabled || event == null || event.getPlayer() == null) return;
        ServerPlayerEntity player = event.getPlayer();

        long payout = config.normalCatchReward;
        if (event.getPokemon().getShiny()) payout = safeAdd(payout, config.shinyCatchBonus);
        if (event.getPokemon().isLegendary() || event.getPokemon().isMythical()) payout = safeAdd(payout, config.legendaryCatchBonus);

        if (payout > 0L) {
            creditDeferred(player, payout);
            player.sendMessage(Text.literal("+" + EconomyService.format(payout) + " • Pokémon caught").formatted(Formatting.GREEN), true);
        }
        progress(player, ContractType.CATCH, 1);
    }

    public static void onBattleVictory(BattleVictoryEvent event) {
        if (config == null || !config.enabled || event == null || event.getWasWildCapture()) return;
        boolean pve = event.getLosers().stream().anyMatch(actor -> actor.getType() != ActorType.PLAYER);
        if (!pve) return;

        for (var actor : event.getWinners()) {
            if (!(actor instanceof PlayerBattleActor playerActor)) continue;
            ServerPlayerEntity player = playerActor.getEntity();
            if (player == null) continue;

            if (config.pveBattleWinReward > 0L) {
                creditDeferred(player, config.pveBattleWinReward);
                player.sendMessage(Text.literal("+" + EconomyService.format(config.pveBattleWinReward) + " • Battle win").formatted(Formatting.GREEN), true);
            }
            progress(player, ContractType.BATTLE, 1);
        }
    }

    private static boolean enabled(ServerPlayerEntity player) {
        if (config == null) config = ActivityEconomyConfig.load();
        if (config.enabled) return true;
        player.sendMessage(Text.literal("The activity economy is currently disabled.").formatted(Formatting.RED), false);
        return false;
    }

    private static long priceFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return priceFor(id);
    }

    private static long priceFor(String id) {
        return priceFor(Identifier.tryParse(id));
    }

    private static long priceFor(Identifier id) {
        if (id == null) return 0L;
        Long exact = config.sellPrices.get(id.toString());
        if (exact != null) return Math.max(0L, exact);
        if ("cobblemon".equals(id.getNamespace())) {
            if (id.getPath().endsWith("_apricorn")) return config.apricornSellPrice;
            if (id.getPath().endsWith("_berry")) return config.berrySellPrice;
        }
        return 0L;
    }

    private static void progress(ServerPlayerEntity player, ContractType type, int amount) {
        if (amount <= 0) return;
        ContractProgress progress = progressFor(player);
        ContractSet set = contractSet(player.getUuid(), progress.cycle);

        int target;
        long reward;
        int bit;
        int before;
        int after;

        switch (type) {
            case CATCH -> {
                target = set.catchTarget;
                reward = set.catchReward;
                bit = 1;
                before = progress.catches;
                progress.catches = Math.min(target, safeIntAdd(progress.catches, amount));
                after = progress.catches;
            }
            case BATTLE -> {
                target = set.battleTarget;
                reward = set.battleReward;
                bit = 2;
                before = progress.battles;
                progress.battles = Math.min(target, safeIntAdd(progress.battles, amount));
                after = progress.battles;
            }
            case SELL -> {
                target = set.sellTarget;
                reward = set.sellReward;
                bit = 4;
                before = progress.soldItems;
                progress.soldItems = Math.min(target, safeIntAdd(progress.soldItems, amount));
                after = progress.soldItems;
            }
            default -> throw new IllegalStateException("Unexpected contract type");
        }

        if (after != before) dirty = true;
        if (after >= target && (progress.completedMask & bit) == 0) {
            progress.completedMask |= bit;
            dirty = true;
            creditDeferred(player, reward);
            player.sendMessage(Text.literal("Contract complete: +" + EconomyService.format(reward) + ".").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        }
    }

    private static ContractProgress progressFor(ServerPlayerEntity player) {
        ContractProgress progress = CONTRACTS.computeIfAbsent(player.getUuidAsString(), ignored -> new ContractProgress());
        long cycle = currentCycle();
        if (progress.cycle != cycle) {
            progress.cycle = cycle;
            progress.catches = 0;
            progress.battles = 0;
            progress.soldItems = 0;
            progress.completedMask = 0;
            dirty = true;
        }
        return progress;
    }

    private static ContractSet contractSet(UUID uuid, long cycle) {
        long seed = uuid.getMostSignificantBits()
                ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 17)
                ^ (cycle * 0x9E3779B97F4A7C15L);
        Random random = new Random(seed);

        int catchTarget = 5 + random.nextInt(5);
        int battleTarget = 4 + random.nextInt(4);
        int sellTarget = 96 + random.nextInt(5) * 32;

        long catchReward = 300L + catchTarget * 30L;
        long battleReward = 350L + battleTarget * 40L;
        long sellReward = 250L + sellTarget * 2L;
        return new ContractSet(catchTarget, catchReward, battleTarget, battleReward, sellTarget, sellReward);
    }

    private static void sendContract(ServerPlayerEntity player, String name, int progress, int target, long reward, boolean complete) {
        Formatting color = complete ? Formatting.GREEN : Formatting.WHITE;
        String marker = complete ? "✓ " : "• ";
        player.sendMessage(Text.literal(marker + name + "  " + progress + "/" + target + "  •  " + EconomyService.format(reward)).formatted(color), false);
    }

    private static long currentCycle() {
        long interval = Math.max(1L, config.contractRefreshHours) * 3_600_000L;
        return Math.floorDiv(System.currentTimeMillis(), interval);
    }

    private static String refreshText(long cycle) {
        long interval = Math.max(1L, config.contractRefreshHours) * 3_600_000L;
        long millis = Math.max(0L, ((cycle + 1L) * interval) - System.currentTimeMillis());
        long minutes = (millis + 59_999L) / 60_000L;
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        return hours > 0 ? hours + "h " + remainingMinutes + "m" : remainingMinutes + "m";
    }

    private static void creditDeferred(ServerPlayerEntity player, long amount) {
        if (amount <= 0L) return;
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.balance = safeAdd(data.balance, amount);
        data.revision++;
    }

    private static String money(long amount) {
        return EconomyService.format(Math.max(0L, amount));
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return Math.max(0L, left + right);
    }

    private static long safeMultiply(long value, int count) {
        if (value <= 0L || count <= 0) return 0L;
        if (value > Long.MAX_VALUE / count) return Long.MAX_VALUE;
        return value * count;
    }

    private static int safeIntAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) return Integer.MAX_VALUE;
        return Math.max(0, left + right);
    }

    private enum ContractType {
        CATCH,
        BATTLE,
        SELL
    }

    private record ContractSet(
            int catchTarget,
            long catchReward,
            int battleTarget,
            long battleReward,
            int sellTarget,
            long sellReward
    ) {}

    private static final class ContractProgress {
        long cycle = Long.MIN_VALUE;
        int catches;
        int battles;
        int soldItems;
        int completedMask;
    }

    private static final class StoreFile {
        Map<String, ContractProgress> players = new LinkedHashMap<>();
    }
}
