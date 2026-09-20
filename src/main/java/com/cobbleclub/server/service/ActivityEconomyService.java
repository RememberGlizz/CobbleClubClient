/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.battles.model.actor.ActorType
 *  com.cobblemon.mod.common.api.battles.model.actor.BattleActor
 *  com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
 *  com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
 *  com.cobblemon.mod.common.battles.actor.PlayerBattleActor
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_124
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_5218
 *  net.minecraft.class_7923
 *  net.minecraft.class_8710
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.SellPayloads;
import com.cobbleclub.server.service.ActivityEconomyConfig;
import com.cobbleclub.server.service.EconomyService;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Formatting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;

public final class ActivityEconomyService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, ContractProgress> CONTRACTS = new HashMap<>();
    private static ActivityEconomyConfig config;
    private static Path storePath;
    private static boolean dirty;
    private static int saveTicks;

    private ActivityEconomyService() {
    }

    public static void initialize() {
        config = ActivityEconomyConfig.load();
    }

    public static void load(MinecraftServer server) {
        storePath = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("activity-economy.json");
        CONTRACTS.clear();
        try {
            StoreFile file;
            if (Files.exists(storePath) && (file = GSON.fromJson(Files.readString(storePath, StandardCharsets.UTF_8), StoreFile.class)) != null && file.players != null) {
                CONTRACTS.putAll(file.players);
            }
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not load activity economy data", error);
        }
        CONTRACTS.values().removeIf(progress -> progress == null);
        dirty = false;
        saveTicks = 0;
    }

    public static void tick(MinecraftServer server) {
        if (++saveTicks < 1200) {
            return;
        }
        saveTicks = 0;
        if (dirty) {
            ActivityEconomyService.save();
        }
    }

    public static void save() {
        if (storePath == null) {
            return;
        }
        try {
            Files.createDirectories(storePath.getParent());
            StoreFile file = new StoreFile();
            file.players = new LinkedHashMap<>(CONTRACTS);
            Files.writeString(storePath, GSON.toJson(file), StandardCharsets.UTF_8);
            dirty = false;
        }
        catch (IOException error) {
            CobbleClubServer.LOGGER.warn("Could not save activity economy data", error);
        }
    }

    public static int sellHand(ServerPlayerEntity player) {
        if (!ActivityEconomyService.enabled(player)) {
            return 0;
        }
        ItemStack stack = player.getMainHandStack();
        long price = ActivityEconomyService.priceFor(stack);
        if (stack.isEmpty() || price <= 0L) {
            player.sendMessage(Text.literal("That item cannot be sold. Use /sell prices to see some examples.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        int count = stack.getCount();
        long payout = ActivityEconomyService.safeMultiply(price, count);
        stack.decrement(count);
        ActivityEconomyService.creditDeferred(player, payout);
        ActivityEconomyService.progress(player, ContractType.SELL, count);
        PlayerDataStore.save();
        ActivityEconomyService.save();
        player.sendMessage(Text.literal("Sold " + count + " item" + (count == 1 ? "" : "s") + " for " + EconomyService.format(payout) + ".").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int sellAll(ServerPlayerEntity player) {
        if (!ActivityEconomyService.enabled(player)) {
            return 0;
        }
        long payout = 0L;
        int sold = 0;
        for (int slot = 0; slot < 36; ++slot) {
            long price;
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty() || (price = ActivityEconomyService.priceFor(stack)) <= 0L) continue;
            int count = stack.getCount();
            payout = ActivityEconomyService.safeAdd(payout, ActivityEconomyService.safeMultiply(price, count));
            sold = ActivityEconomyService.safeIntAdd(sold, count);
            stack.decrement(count);
        }
        if (sold == 0 || payout <= 0L) {
            player.sendMessage(Text.literal("You do not have any sellable items in your main inventory.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        ActivityEconomyService.creditDeferred(player, payout);
        ActivityEconomyService.progress(player, ContractType.SELL, sold);
        PlayerDataStore.save();
        ActivityEconomyService.save();
        player.sendMessage(Text.literal("Sold " + sold + " items for " + EconomyService.format(payout) + ".").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int showSellInfo(ServerPlayerEntity player) {
        if (!ActivityEconomyService.enabled(player)) {
            return 0;
        }
        if (ServerPlayNetworking.canSend(player, SellPayloads.Open.ID)) {
            ActivityEconomyService.sendSellState(player, "", false, true);
            return 1;
        }
        player.sendMessage(Text.literal("CobbleClub Sell Shop").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("/sell hand").formatted(Formatting.YELLOW).append(Text.literal(" - sell the stack in your main hand").formatted(Formatting.GRAY)), false);
        player.sendMessage(Text.literal("/sell all").formatted(Formatting.YELLOW).append(Text.literal(" - sell all supported items in your main inventory").formatted(Formatting.GRAY)), false);
        player.sendMessage(Text.literal("/sell prices").formatted(Formatting.YELLOW).append(Text.literal(" - show common sell prices").formatted(Formatting.GRAY)), false);
        return 1;
    }

    public static void handleSellAction(ServerPlayerEntity player, String raw) {
        if (!ActivityEconomyService.enabled(player)) {
            return;
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw == null ? "{}" : raw);
            JsonObject json = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            String action = json.has("action") ? json.get("action").getAsString() : "";
            if ("refresh".equalsIgnoreCase(action)) {
                ActivityEconomyService.sendSellState(player, "Shop refreshed.", false, false);
                return;
            }
            if (!"sell".equalsIgnoreCase(action)) {
                ActivityEconomyService.sendSellState(player, "Unknown shop action.", true, false);
                return;
            }
            String rawId = json.has("item") ? json.get("item").getAsString() : "";
            int quantity = json.has("quantity") ? json.get("quantity").getAsInt() : 0;
            Identifier id = Identifier.tryParse(rawId);
            if (id == null || !Registries.ITEM.containsId(id)) {
                ActivityEconomyService.sendSellState(player, "That item is not available in the sell shop.", true, false);
                return;
            }
            if (quantity <= 0 || quantity > 100000) {
                ActivityEconomyService.sendSellState(player, "Enter a valid amount to sell.", true, false);
                return;
            }
            long unitPrice = ActivityEconomyService.priceFor(id);
            if (unitPrice <= 0L) {
                ActivityEconomyService.sendSellState(player, "That item cannot be sold.", true, false);
                return;
            }
            int owned = ActivityEconomyService.countOwned(player, id);
            if (owned < quantity) {
                ActivityEconomyService.sendSellState(player, "You only have " + owned + " of that item in your inventory.", true, false);
                return;
            }
            if (!ActivityEconomyService.removeItems(player, id, quantity)) {
                ActivityEconomyService.sendSellState(player, "Your inventory changed before the sale could finish. Try again.", true, false);
                return;
            }
            long payout = ActivityEconomyService.safeMultiply(unitPrice, quantity);
            ActivityEconomyService.creditDeferred(player, payout);
            ActivityEconomyService.progress(player, ContractType.SELL, quantity);
            PlayerDataStore.save();
            ActivityEconomyService.save();
            Item item = Registries.ITEM.get(id);
            String itemName = item == null ? id.getPath() : new ItemStack(item).getName().getString();
            ActivityEconomyService.sendSellState(player, "Sold " + quantity + " " + itemName + " for " + EconomyService.format(payout) + ".", false, false);
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not process sell shop action for {}", player.getGameProfile().getName(), error);
            ActivityEconomyService.sendSellState(player, "That sale could not be completed. Try again.", true, false);
        }
    }

    private static void sendSellState(ServerPlayerEntity player, String notice, boolean error, boolean open) {
        String json = ActivityEconomyService.sellState(player, notice, error);
        if (open) {
            if (ServerPlayNetworking.canSend(player, SellPayloads.Open.ID)) {
                ServerPlayNetworking.send(player, new SellPayloads.Open(json));
            }
        } else if (ServerPlayNetworking.canSend(player, SellPayloads.State.ID)) {
            ServerPlayNetworking.send(player, new SellPayloads.State(json));
        }
    }

    private static String sellState(ServerPlayerEntity player, String notice, boolean error) {
        JsonObject root = new JsonObject();
        root.addProperty("balanceText", EconomyService.format(EconomyService.balance(player)));
        root.addProperty("currencySymbol", CobbleClubServer.config().currencySymbol);
        root.addProperty("currencyName", CobbleClubServer.config().currencyName);
        root.addProperty("notice", notice == null ? "" : notice);
        root.addProperty("error", error);
        ArrayList<Identifier> ids = new ArrayList<>(Registries.ITEM.getIds());
        ids.removeIf(id -> ActivityEconomyService.priceFor(id) <= 0L);
        ids.sort(
                Comparator.comparingInt((Identifier id) ->
                                "minecraft".equals(id.getNamespace()) ? 0
                                        : "cobblemon".equals(id.getNamespace()) ? 1
                                        : 2
                        )
                        .thenComparing(Identifier::getNamespace)
                        .thenComparing(Identifier::getPath)
        );
        JsonArray items = new JsonArray();
        for (Identifier id2 : ids) {
            JsonObject item = new JsonObject();
            item.addProperty("id", id2.toString());
            item.addProperty("price", ActivityEconomyService.priceFor(id2));
            item.addProperty("count", ActivityEconomyService.countOwned(player, id2));
            items.add(item);
        }
        root.add("items", items);
        return GSON.toJson(root);
    }

    private static int countOwned(ServerPlayerEntity player, Identifier id) {
        Item item = Registries.ITEM.get(id);
        if (item == null) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < 36; ++slot) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty() || stack.getItem() != item) continue;
            total = ActivityEconomyService.safeIntAdd(total, stack.getCount());
        }
        return total;
    }

    private static boolean removeItems(ServerPlayerEntity player, Identifier id, int quantity) {
        Item item = Registries.ITEM.get(id);
        if (item == null || quantity <= 0) {
            return false;
        }
        int remaining = quantity;
        for (int slot = 0; slot < 36 && remaining > 0; ++slot) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty() || stack.getItem() != item) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.decrement(take);
            remaining -= take;
        }
        return remaining == 0;
    }

    public static int showPrices(ServerPlayerEntity player) {
        if (!ActivityEconomyService.enabled(player)) {
            return 0;
        }
        player.sendMessage(Text.literal("Common sell prices").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Iron " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:iron_ingot")) + "  Gold " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:gold_ingot")) + "  Diamond " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:diamond")) + "  Emerald " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:emerald"))).formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Coal " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:coal")) + "  Redstone " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:redstone")) + "  Gunpowder " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:gunpowder")) + "  Blaze Rod " + ActivityEconomyService.money(ActivityEconomyService.priceFor("minecraft:blaze_rod"))).formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Crops/fish are supported, Cobblemon apricorns sell for " + ActivityEconomyService.money(ActivityEconomyService.config.apricornSellPrice) + " each and berries sell for " + ActivityEconomyService.money(ActivityEconomyService.config.berrySellPrice) + " each.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Prices can be changed by the server as the economy develops.").formatted(Formatting.DARK_GRAY), false);
        return 1;
    }

    public static int showContracts(ServerPlayerEntity player) {
        if (!ActivityEconomyService.enabled(player)) {
            return 0;
        }
        ContractProgress progress = ActivityEconomyService.progressFor(player);
        ContractSet set = ActivityEconomyService.contractSet(player.getUuid(), progress.cycle);
        player.sendMessage(Text.literal("Trainer Contracts").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD).append(Text.literal(" \u2022 refresh in " + ActivityEconomyService.refreshText(progress.cycle)).formatted(Formatting.GRAY)), false);
        ActivityEconomyService.sendContract(player, "Catch Pok\u00e9mon", progress.catches, set.catchTarget, set.catchReward, (progress.completedMask & 1) != 0);
        ActivityEconomyService.sendContract(player, "Win PvE battles", progress.battles, set.battleTarget, set.battleReward, (progress.completedMask & 2) != 0);
        ActivityEconomyService.sendContract(player, "Sell items", progress.soldItems, set.sellTarget, set.sellReward, (progress.completedMask & 4) != 0);
        player.sendMessage(Text.literal("Contracts pay automatically when completed.").formatted(Formatting.DARK_GRAY), false);
        return 1;
    }

    public static void onPokemonCaptured(PokemonCapturedEvent event) {
        if (config == null || !ActivityEconomyService.config.enabled || event == null || event.getPlayer() == null) {
            return;
        }
        ServerPlayerEntity player = event.getPlayer();
        long payout = ActivityEconomyService.config.normalCatchReward;
        if (event.getPokemon().getShiny()) {
            payout = ActivityEconomyService.safeAdd(payout, ActivityEconomyService.config.shinyCatchBonus);
        }
        if (event.getPokemon().isLegendary() || event.getPokemon().isMythical()) {
            payout = ActivityEconomyService.safeAdd(payout, ActivityEconomyService.config.legendaryCatchBonus);
        }
        if (payout > 0L) {
            ActivityEconomyService.creditDeferred(player, payout);
            player.sendMessage(Text.literal("+" + EconomyService.format(payout) + " \u2022 Pok\u00e9mon caught").formatted(Formatting.GREEN), true);
        }
        ActivityEconomyService.progress(player, ContractType.CATCH, 1);
    }

    public static void onBattleVictory(BattleVictoryEvent event) {
        if (config == null || !ActivityEconomyService.config.enabled || event == null || event.getWasWildCapture()) {
            return;
        }
        boolean pve = event.getLosers().stream().anyMatch(actor -> actor.getType() != ActorType.PLAYER);
        if (!pve) {
            return;
        }
        for (BattleActor actor2 : event.getWinners()) {
            PlayerBattleActor playerActor;
            ServerPlayerEntity player;
            if (!(actor2 instanceof PlayerBattleActor) || (player = (playerActor = (PlayerBattleActor)actor2).getEntity()) == null) continue;
            if (ActivityEconomyService.config.pveBattleWinReward > 0L) {
                ActivityEconomyService.creditDeferred(player, ActivityEconomyService.config.pveBattleWinReward);
                player.sendMessage(Text.literal("+" + EconomyService.format(ActivityEconomyService.config.pveBattleWinReward) + " \u2022 Battle win").formatted(Formatting.GREEN), true);
            }
            ActivityEconomyService.progress(player, ContractType.BATTLE, 1);
        }
    }

    private static boolean enabled(ServerPlayerEntity player) {
        if (config == null) {
            config = ActivityEconomyConfig.load();
        }
        if (ActivityEconomyService.config.enabled) {
            return true;
        }
        player.sendMessage(Text.literal("The activity economy is currently disabled.").formatted(Formatting.RED), false);
        return false;
    }

    public static long sellPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        return ActivityEconomyService.sellPrice(Registries.ITEM.getId(stack.getItem()));
    }

    public static long sellPrice(Identifier id) {
        if (config == null) {
            config = ActivityEconomyConfig.load();
        }
        if (id == null) {
            return 0L;
        }
        Long exact = ActivityEconomyService.config.sellPrices.get(id.toString());
        if (exact != null) {
            return Math.max(0L, exact);
        }
        if ("cobblemon".equals(id.getNamespace())) {
            if (id.getPath().endsWith("_apricorn")) {
                return ActivityEconomyService.config.apricornSellPrice;
            }
            if (id.getPath().endsWith("_berry")) {
                return ActivityEconomyService.config.berrySellPrice;
            }
        }
        return 0L;
    }

    private static long priceFor(ItemStack stack) {
        return ActivityEconomyService.sellPrice(stack);
    }

    private static long priceFor(String id) {
        return ActivityEconomyService.sellPrice(Identifier.tryParse(id));
    }

    private static long priceFor(Identifier id) {
        return ActivityEconomyService.sellPrice(id);
    }

    private static void progress(ServerPlayerEntity player, ContractType type, int amount) {
        int before;
        int bit;
        long reward;
        int target;
        if (amount <= 0) {
            return;
        }
        ContractProgress progress = ActivityEconomyService.progressFor(player);
        ContractSet set = ActivityEconomyService.contractSet(player.getUuid(), progress.cycle);
        int after = switch (type.ordinal()) {
            case 0 -> {
                target = set.catchTarget;
                reward = set.catchReward;
                bit = 1;
                before = progress.catches;
                yield progress.catches = Math.min(target, ActivityEconomyService.safeIntAdd(progress.catches, amount));
            }
            case 1 -> {
                target = set.battleTarget;
                reward = set.battleReward;
                bit = 2;
                before = progress.battles;
                yield progress.battles = Math.min(target, ActivityEconomyService.safeIntAdd(progress.battles, amount));
            }
            case 2 -> {
                target = set.sellTarget;
                reward = set.sellReward;
                bit = 4;
                before = progress.soldItems;
                yield progress.soldItems = Math.min(target, ActivityEconomyService.safeIntAdd(progress.soldItems, amount));
            }
            default -> throw new IllegalStateException("Unexpected contract type");
        };
        if (after != before) {
            dirty = true;
        }
        if (after >= target && (progress.completedMask & bit) == 0) {
            progress.completedMask |= bit;
            dirty = true;
            ActivityEconomyService.creditDeferred(player, reward);
            player.sendMessage(Text.literal("Contract complete: +" + EconomyService.format(reward) + ".").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        }
    }

    private static ContractProgress progressFor(ServerPlayerEntity player) {
        ContractProgress progress = CONTRACTS.computeIfAbsent(player.getUuidAsString(), ignored -> new ContractProgress());
        long cycle = ActivityEconomyService.currentCycle();
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
        long seed = uuid.getMostSignificantBits() ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 17) ^ cycle * -7046029254386353131L;
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
        String marker = complete ? "\u2713 " : "\u2022 ";
        player.sendMessage(Text.literal(marker + name + "  " + progress + "/" + target + "  \u2022  " + EconomyService.format(reward)).formatted(color), false);
    }

    private static long currentCycle() {
        long interval = Math.max(1L, ActivityEconomyService.config.contractRefreshHours) * 3600000L;
        return Math.floorDiv(System.currentTimeMillis(), interval);
    }

    private static String refreshText(long cycle) {
        long interval = Math.max(1L, ActivityEconomyService.config.contractRefreshHours) * 3600000L;
        long millis = Math.max(0L, (cycle + 1L) * interval - System.currentTimeMillis());
        long minutes = (millis + 59999L) / 60000L;
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        return hours > 0L ? hours + "h " + remainingMinutes + "m" : remainingMinutes + "m";
    }

    private static void creditDeferred(ServerPlayerEntity player, long amount) {
        if (amount <= 0L) {
            return;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.balance = ActivityEconomyService.safeAdd(data.balance, amount);
        ++data.revision;
    }

    private static String money(long amount) {
        return EconomyService.format(Math.max(0L, amount));
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, left + right);
    }

    private static long safeMultiply(long value, int count) {
        if (value <= 0L || count <= 0) {
            return 0L;
        }
        if (value > Long.MAX_VALUE / count) {
            return Long.MAX_VALUE;
        }
        return value * count;
    }

    private static int safeIntAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, left + right);
    }

    private static final class StoreFile {
        Map<String, ContractProgress> players = new LinkedHashMap<>();

        private StoreFile() {
        }
    }

    private enum ContractType {
        CATCH,
        BATTLE,
        SELL;

    }

    private static final class ContractProgress {
        long cycle = Long.MIN_VALUE;
        int catches;
        int battles;
        int soldItems;
        int completedMask;

        private ContractProgress() {
        }
    }

    private record ContractSet(int catchTarget, long catchReward, int battleTarget, long battleReward, int sellTarget, long sellReward) {
    }
}
