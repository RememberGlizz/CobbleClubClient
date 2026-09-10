package com.cobbleclub.server.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Settings for active PokéDollar earning and the server-backed sell list. */
public final class ActivityEconomyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cobbleclub-activity-economy.json");
    private static final Set<String> AUTO_SELL_BLOCKED = Set.of(
            "minecraft:air",
            "minecraft:barrier",
            "minecraft:bedrock",
            "minecraft:budding_amethyst",
            "minecraft:chain_command_block",
            "minecraft:command_block",
            "minecraft:command_block_minecart",
            "minecraft:debug_stick",
            "minecraft:end_portal_frame",
            "minecraft:jigsaw",
            "minecraft:knowledge_book",
            "minecraft:light",
            "minecraft:reinforced_deepslate",
            "minecraft:repeating_command_block",
            "minecraft:spawner",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:trial_spawner",
            "minecraft:vault"
    );

    public boolean enabled = true;

    public long normalCatchReward = 25L;
    public long shinyCatchBonus = 400L;
    public long legendaryCatchBonus = 800L;
    public long pveBattleWinReward = 50L;

    public int contractRefreshHours = 6;
    public long apricornSellPrice = 3L;
    public long berrySellPrice = 4L;

    // Items with an explicit price always use it. A price of 0 disables that item.
    public Map<String, Long> sellPrices = new LinkedHashMap<>();

    // Adds ordinary stackable items from these namespaces to the shop at a conservative fallback price.
    // Add another mod id here later if you want its normal items to appear too.
    public boolean autoSellCatalog = true;
    public boolean autoSellStackableOnly = true;
    public Map<String, Long> fallbackNamespaceSellPrices = new LinkedHashMap<>();

    public static ActivityEconomyConfig load() {
        ActivityEconomyConfig config = null;
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                config = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), ActivityEconomyConfig.class);
            }
        } catch (Exception ignored) {
        }

        if (config == null) config = defaults();
        config.normalize();

        // Save only the human-editable settings. Generated catalog entries are runtime-only.
        config.save();
        config.populateAutoSellPrices();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private void normalize() {
        normalCatchReward = Math.max(0L, normalCatchReward);
        shinyCatchBonus = Math.max(0L, shinyCatchBonus);
        legendaryCatchBonus = Math.max(0L, legendaryCatchBonus);
        pveBattleWinReward = Math.max(0L, pveBattleWinReward);
        contractRefreshHours = Math.max(1, contractRefreshHours);
        apricornSellPrice = Math.max(0L, apricornSellPrice);
        berrySellPrice = Math.max(0L, berrySellPrice);
        if (sellPrices == null) sellPrices = new LinkedHashMap<>();
        sellPrices.replaceAll((id, price) -> price == null ? 0L : Math.max(0L, price));
        defaults().sellPrices.forEach(sellPrices::putIfAbsent);

        if (fallbackNamespaceSellPrices == null) fallbackNamespaceSellPrices = new LinkedHashMap<>();
        fallbackNamespaceSellPrices.replaceAll((id, price) -> price == null ? 0L : Math.max(0L, price));
        defaults().fallbackNamespaceSellPrices.forEach(fallbackNamespaceSellPrices::putIfAbsent);
    }

    private void populateAutoSellPrices() {
        if (!autoSellCatalog) return;

        for (Identifier id : Registries.ITEM.getIds()) {
            String rawId = id.toString();
            if (sellPrices.containsKey(rawId) || blockedFromAutoSell(id)) continue;

            long price = generatedPrice(id);
            if (price > 0L) sellPrices.put(rawId, price);
        }
    }

    private long generatedPrice(Identifier id) {
        String namespace = id.getNamespace();
        String path = id.getPath();

        if ("cobblemon".equals(namespace)) {
            if (path.endsWith("_apricorn")) return apricornSellPrice;
            if (path.endsWith("_berry")) return berrySellPrice;
        }

        Long fallback = fallbackNamespaceSellPrices.get(namespace);
        if (fallback == null || fallback <= 0L) return 0L;

        Item item = Registries.ITEM.get(id);
        if (item == null) return 0L;
        ItemStack sample = new ItemStack(item);
        if (sample.isEmpty()) return 0L;
        if (autoSellStackableOnly && sample.getMaxCount() <= 1) return 0L;
        return fallback;
    }

    private static boolean blockedFromAutoSell(Identifier id) {
        String rawId = id.toString();
        String path = id.getPath();
        return AUTO_SELL_BLOCKED.contains(rawId)
                || path.endsWith("_spawn_egg")
                || path.contains("debug")
                || path.contains("creative_only");
    }

    private static ActivityEconomyConfig defaults() {
        ActivityEconomyConfig config = new ActivityEconomyConfig();

        // Farming / renewable materials
        config.sellPrices.put("minecraft:wheat", 2L);
        config.sellPrices.put("minecraft:carrot", 2L);
        config.sellPrices.put("minecraft:potato", 2L);
        config.sellPrices.put("minecraft:beetroot", 2L);
        config.sellPrices.put("minecraft:pumpkin", 3L);
        config.sellPrices.put("minecraft:melon_slice", 1L);
        config.sellPrices.put("minecraft:sugar_cane", 2L);
        config.sellPrices.put("minecraft:cactus", 2L);
        config.sellPrices.put("minecraft:kelp", 1L);
        config.sellPrices.put("minecraft:sweet_berries", 2L);

        // Fishing
        config.sellPrices.put("minecraft:cod", 3L);
        config.sellPrices.put("minecraft:salmon", 4L);
        config.sellPrices.put("minecraft:tropical_fish", 5L);
        config.sellPrices.put("minecraft:pufferfish", 5L);

        // Common drops
        config.sellPrices.put("minecraft:rotten_flesh", 1L);
        config.sellPrices.put("minecraft:bone", 2L);
        config.sellPrices.put("minecraft:string", 2L);
        config.sellPrices.put("minecraft:spider_eye", 2L);
        config.sellPrices.put("minecraft:gunpowder", 3L);
        config.sellPrices.put("minecraft:slime_ball", 4L);
        config.sellPrices.put("minecraft:ender_pearl", 8L);
        config.sellPrices.put("minecraft:blaze_rod", 10L);

        // Mining / resource world
        config.sellPrices.put("minecraft:coal", 2L);
        config.sellPrices.put("minecraft:raw_copper", 2L);
        config.sellPrices.put("minecraft:raw_iron", 4L);
        config.sellPrices.put("minecraft:raw_gold", 6L);
        config.sellPrices.put("minecraft:copper_ingot", 3L);
        config.sellPrices.put("minecraft:iron_ingot", 5L);
        config.sellPrices.put("minecraft:gold_ingot", 8L);
        config.sellPrices.put("minecraft:redstone", 2L);
        config.sellPrices.put("minecraft:lapis_lazuli", 3L);
        config.sellPrices.put("minecraft:quartz", 3L);
        config.sellPrices.put("minecraft:amethyst_shard", 3L);
        config.sellPrices.put("minecraft:diamond", 35L);
        config.sellPrices.put("minecraft:emerald", 30L);

        // Conservative automatic prices. Exact entries above still win.
        config.fallbackNamespaceSellPrices.put("minecraft", 1L);
        config.fallbackNamespaceSellPrices.put("cobblemon", 2L);
        config.fallbackNamespaceSellPrices.put("cobblefurnies", 2L);

        return config;
    }
}
