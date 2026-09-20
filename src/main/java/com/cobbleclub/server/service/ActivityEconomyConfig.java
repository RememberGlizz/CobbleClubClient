/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2960
 *  net.minecraft.class_7923
 */
package com.cobbleclub.server.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public final class ActivityEconomyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cobbleclub-activity-economy.json");
    private static final Set<String> AUTO_SELL_BLOCKED = Set.of("minecraft:air", "minecraft:barrier", "minecraft:bedrock", "minecraft:budding_amethyst", "minecraft:chain_command_block", "minecraft:command_block", "minecraft:command_block_minecart", "minecraft:debug_stick", "minecraft:end_portal_frame", "minecraft:jigsaw", "minecraft:knowledge_book", "minecraft:light", "minecraft:reinforced_deepslate", "minecraft:repeating_command_block", "minecraft:spawner", "minecraft:structure_block", "minecraft:structure_void", "minecraft:trial_spawner", "minecraft:vault");
    public boolean enabled = true;
    public int pricingVersion = 0;
    public long normalCatchReward = 25L;
    public long shinyCatchBonus = 400L;
    public long legendaryCatchBonus = 800L;
    public long pveBattleWinReward = 50L;
    public int contractRefreshHours = 6;
    public long apricornSellPrice = 15L;
    public long berrySellPrice = 20L;
    public Map<String, Long> sellPrices = new LinkedHashMap<String, Long>();
    public boolean autoSellCatalog = true;
    public boolean autoSellStackableOnly = true;
    public Map<String, Long> fallbackNamespaceSellPrices = new LinkedHashMap<String, Long>();

    public static ActivityEconomyConfig load() {
        ActivityEconomyConfig config = null;
        try {
            Files.createDirectories(PATH.getParent(), new FileAttribute[0]);
            if (Files.exists(PATH, new LinkOption[0])) {
                config = (ActivityEconomyConfig)GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), ActivityEconomyConfig.class);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (config == null) {
            config = ActivityEconomyConfig.defaults();
        }
        config.normalize();
        config.save();
        config.populateAutoSellPrices();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent(), new FileAttribute[0]);
            Files.writeString(PATH, (CharSequence)GSON.toJson((Object)this), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private void normalize() {
        int loadedPricingVersion = this.pricingVersion;
        if (this.sellPrices == null) {
            this.sellPrices = new LinkedHashMap<String, Long>();
        }
        if (this.fallbackNamespaceSellPrices == null) {
            this.fallbackNamespaceSellPrices = new LinkedHashMap<String, Long>();
        }
        if (loadedPricingVersion < 2) {
            this.migrateLegacyPricing();
            this.pricingVersion = 2;
        }
        if (loadedPricingVersion < 3) {
            this.migrateEconomyV3();
            this.pricingVersion = 3;
        }
        this.normalCatchReward = Math.max(0L, this.normalCatchReward);
        this.shinyCatchBonus = Math.max(0L, this.shinyCatchBonus);
        this.legendaryCatchBonus = Math.max(0L, this.legendaryCatchBonus);
        this.pveBattleWinReward = Math.max(0L, this.pveBattleWinReward);
        this.contractRefreshHours = Math.max(1, this.contractRefreshHours);
        this.apricornSellPrice = Math.max(0L, this.apricornSellPrice);
        this.berrySellPrice = Math.max(0L, this.berrySellPrice);
        this.sellPrices.replaceAll((id, price) -> price == null ? 0L : Math.max(0L, price));
        ActivityEconomyConfig.defaults().sellPrices.forEach(this.sellPrices::putIfAbsent);
        this.fallbackNamespaceSellPrices.replaceAll((id, price) -> price == null ? 0L : Math.max(0L, price));
        ActivityEconomyConfig.defaults().fallbackNamespaceSellPrices.forEach(this.fallbackNamespaceSellPrices::putIfAbsent);
    }

    private void populateAutoSellPrices() {
        for (Identifier id : Registries.ITEM.getIds()) {
            long price;
            String rawId = id.toString();
            if (this.sellPrices.containsKey(rawId) || ActivityEconomyConfig.blockedFromAutoSell(id) || (price = this.generatedPrice(id)) <= 0L) continue;
            this.sellPrices.put(rawId, price);
        }
    }

    private long generatedPrice(Identifier id) {
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("minecraft".equals(namespace)) {
            return this.vanillaGeneratedPrice(path);
        }
        if ("cobblemon".equals(namespace)) {
            return this.cobblemonGeneratedPrice(path);
        }
        if ("pokeblocks".equals(namespace)) {
            if (!ActivityEconomyConfig.isPokedollCollectible(path)) {
                return 0L;
            }
            long price = ActivityEconomyConfig.pokeblocksPrice(path);
            if (price <= 0L) {
                return 0L;
            }
            Item item = (Item)Registries.ITEM.get(id);
            if (item == null) {
                return 0L;
            }
            ItemStack sample = new ItemStack((ItemConvertible)item);
            if (sample.isEmpty()) {
                return 0L;
            }
            return price;
        }
        if (!this.autoSellCatalog) {
            return 0L;
        }
        Long fallback = this.fallbackNamespaceSellPrices.get(namespace);
        Item item = (Item)Registries.ITEM.get(id);
        if (item == null) {
            return 0L;
        }
        ItemStack sample = new ItemStack((ItemConvertible)item);
        if (sample.isEmpty()) {
            return 0L;
        }
        if (this.autoSellStackableOnly && sample.getMaxCount() <= 1) {
            return 0L;
        }
        if (fallback != null && fallback > 0L) {
            return fallback;
        }
        return ActivityEconomyConfig.sideModGeneratedPrice(path);
    }

    private static long sideModGeneratedPrice(String path) {
        if (path == null || path.isBlank()) {
            return 0L;
        }
        if (ActivityEconomyConfig.containsAny(path, "coin", "currency", "token", "voucher", "ticket", "_key", "raid_pass", "battle_pass", "badge", "medal", "permit", "license", "admin", "creative", "debug", "placeholder")) {
            return 0L;
        }
        if (ActivityEconomyConfig.containsAny(path, "ore", "ingot", "crystal", "shard", "pearl", "gem")) {
            return 35L;
        }
        if (ActivityEconomyConfig.containsAny(path, "food", "meal", "sandwich", "curry", "soup", "stew", "cake", "pie", "juice", "tea", "coffee", "berry", "fruit")) {
            return 15L;
        }
        if (ActivityEconomyConfig.containsAny(path, "machine", "computer", "storage", "backpack", "waystone")) {
            return 75L;
        }
        if (ActivityEconomyConfig.containsAny(path, "chair", "table", "desk", "shelf", "cabinet", "lamp", "sofa", "furniture", "decor", "brick", "plank", "tile", "block")) {
            return 10L;
        }
        return 12L;
    }

    private long vanillaGeneratedPrice(String path) {
        if (path == null || path.isBlank()) {
            return 0L;
        }
        if (path.equals("dragon_egg")) {
            return 50000L;
        }
        if (path.equals("nether_star")) {
            return 5000L;
        }
        if (path.equals("beacon")) {
            return 8500L;
        }
        if (path.equals("elytra")) {
            return 6000L;
        }
        if (path.equals("totem_of_undying")) {
            return 3500L;
        }
        if (path.equals("heart_of_the_sea") || path.equals("conduit")) {
            return 1800L;
        }
        if (path.equals("echo_shard")) {
            return 350L;
        }
        if (path.equals("ancient_debris")) {
            return 1250L;
        }
        if (path.equals("netherite_ingot")) {
            return 5500L;
        }
        if (path.equals("netherite_block")) {
            return 40000L;
        }
        if (path.equals("diamond_block")) {
            return 4500L;
        }
        if (path.equals("emerald_block")) {
            return 3200L;
        }
        if (path.equals("gold_block")) {
            return 900L;
        }
        if (path.equals("iron_block")) {
            return 540L;
        }
        if (path.equals("coal_block")) {
            return 180L;
        }
        if (path.equals("lapis_block")) {
            return 225L;
        }
        if (path.equals("redstone_block")) {
            return 135L;
        }
        if (path.equals("raw_iron_block")) {
            return 400L;
        }
        if (path.equals("raw_gold_block")) {
            return 700L;
        }
        if (path.equals("raw_copper_block")) {
            return 180L;
        }
        if (path.startsWith("netherite_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 2800L;
        }
        if (path.startsWith("diamond_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 650L;
        }
        if (path.startsWith("iron_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 110L;
        }
        if (path.startsWith("golden_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 90L;
        }
        if (path.startsWith("chainmail_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 100L;
        }
        if (path.startsWith("leather_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 30L;
        }
        if (path.startsWith("wooden_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 6L;
        }
        if (path.startsWith("stone_") && ActivityEconomyConfig.equipmentPath(path)) {
            return 10L;
        }
        if (ActivityEconomyConfig.containsAny(path, "music_disc", "banner_pattern", "smithing_template")) {
            return 300L;
        }
        if (ActivityEconomyConfig.containsAny(path, "shulker_box")) {
            return 350L;
        }
        if (path.equals("enchanted_golden_apple")) {
            return 2500L;
        }
        if (path.equals("golden_apple")) {
            return 250L;
        }
        if (path.equals("experience_bottle")) {
            return 75L;
        }
        if (path.equals("enchanted_book")) {
            return 150L;
        }
        if (path.equals("saddle") || path.equals("name_tag")) {
            return 125L;
        }
        if (path.equals("trident")) {
            return 1500L;
        }
        if (path.equals("mace")) {
            return 3000L;
        }
        if (ActivityEconomyConfig.containsAny(path, "sapling", "leaves", "seed", "seeds", "flower", "tulip", "dandelion", "poppy", "orchid", "allium", "azure_bluet", "cornflower", "lily", "mushroom", "kelp", "bamboo", "cactus", "sugar_cane", "vine", "moss", "azalea")) {
            return 3L;
        }
        if (ActivityEconomyConfig.containsAny(path, "log", "wood", "planks", "stem", "hyphae")) {
            return 5L;
        }
        if (ActivityEconomyConfig.containsAny(path, "stone", "cobblestone", "deepslate", "tuff", "calcite", "granite", "diorite", "andesite", "sand", "gravel", "dirt", "mud", "clay", "terracotta")) {
            return 2L;
        }
        if (ActivityEconomyConfig.containsAny(path, "glass", "brick", "concrete", "wool")) {
            return 4L;
        }
        if (ActivityEconomyConfig.containsAny(path, "cooked_", "bread", "cake", "pie", "stew", "soup")) {
            return 12L;
        }
        if (ActivityEconomyConfig.containsAny(path, "beef", "porkchop", "chicken", "mutton", "rabbit")) {
            return 8L;
        }
        if (ActivityEconomyConfig.containsAny(path, "apple", "melon", "pumpkin", "carrot", "potato", "beetroot", "wheat")) {
            return 6L;
        }
        if (ActivityEconomyConfig.containsAny(path, "potion", "splash_potion", "lingering_potion")) {
            return 60L;
        }
        if (path.equals("ender_eye")) {
            return 125L;
        }
        if (path.equals("ender_pearl")) {
            return 75L;
        }
        if (ActivityEconomyConfig.containsAny(path, "prismarine_crystals", "prismarine_shard")) {
            return 20L;
        }
        return 5L;
    }

    private long cobblemonGeneratedPrice(String path) {
        if (path == null || path.isBlank()) {
            return 0L;
        }
        if (path.endsWith("_apricorn")) {
            return this.apricornSellPrice;
        }
        if (path.endsWith("_berry")) {
            return this.berrySellPrice;
        }
        if (path.equals("master_ball")) {
            return 25000L;
        }
        if (path.equals("beast_ball") || path.contains("cherish_ball")) {
            return 1200L;
        }
        if (path.endsWith("_ball") || path.contains("poke_ball")) {
            if (ActivityEconomyConfig.containsAny(path, "ultra", "quick", "dusk", "timer", "repeat", "level", "love", "heavy", "friend", "moon", "dream", "safari", "sport")) {
                return 180L;
            }
            if (ActivityEconomyConfig.containsAny(path, "great", "luxury", "premier", "heal", "net", "nest", "dive")) {
                return 100L;
            }
            return 50L;
        }
        if (path.contains("rare_candy")) {
            return 600L;
        }
        if (path.contains("exp_candy_xl")) {
            return 750L;
        }
        if (path.contains("exp_candy_l")) {
            return 400L;
        }
        if (path.contains("exp_candy_m")) {
            return 200L;
        }
        if (path.contains("exp_candy_s")) {
            return 90L;
        }
        if (path.contains("exp_candy")) {
            return 40L;
        }
        if (ActivityEconomyConfig.containsAny(path, "ability_patch", "gold_bottle_cap")) {
            return 2500L;
        }
        if (ActivityEconomyConfig.containsAny(path, "ability_capsule", "bottle_cap")) {
            return 1000L;
        }
        if (ActivityEconomyConfig.containsAny(path, "mint", "vitamin", "protein", "iron", "calcium", "zinc", "carbos", "hp_up")) {
            return 300L;
        }
        if (ActivityEconomyConfig.containsAny(path, "fire_stone", "water_stone", "thunder_stone", "leaf_stone", "moon_stone", "sun_stone", "shiny_stone", "dusk_stone", "dawn_stone", "ice_stone")) {
            return 750L;
        }
        if (ActivityEconomyConfig.containsAny(path, "linking_cord", "kings_rock", "metal_coat", "dragon_scale", "upgrade", "dubious_disc", "protector", "electirizer", "magmarizer", "reaper_cloth", "razor_claw", "razor_fang", "prism_scale", "whipped_dream", "sachet", "sweet_apple", "tart_apple")) {
            return 900L;
        }
        if (ActivityEconomyConfig.containsAny(path, "fossil", "amber")) {
            return 1000L;
        }
        if (ActivityEconomyConfig.containsAny(path, "mega_stone", "mega_", "key_stone")) {
            return 2500L;
        }
        if (ActivityEconomyConfig.containsAny(path, "tm", "technical_machine", "tr_")) {
            return 450L;
        }
        if (ActivityEconomyConfig.containsAny(path, "max_revive", "full_restore")) {
            return 350L;
        }
        if (ActivityEconomyConfig.containsAny(path, "revive", "hyper_potion")) {
            return 225L;
        }
        if (ActivityEconomyConfig.containsAny(path, "super_potion")) {
            return 125L;
        }
        if (ActivityEconomyConfig.containsAny(path, "potion", "antidote", "awakening", "burn_heal", "ice_heal", "paralyze_heal")) {
            return 60L;
        }
        if (ActivityEconomyConfig.containsAny(path, "healing_machine", "pc")) {
            return 1200L;
        }
        if (ActivityEconomyConfig.containsAny(path, "pasture", "fossil_machine")) {
            return 1800L;
        }
        if (ActivityEconomyConfig.containsAny(path, "lure", "incense", "choice_", "life_orb", "leftovers", "focus_sash", "assault_vest")) {
            return 450L;
        }
        return 25L;
    }

    private static boolean equipmentPath(String path) {
        return ActivityEconomyConfig.containsAny(path, "_sword", "_pickaxe", "_axe", "_shovel", "_hoe", "_helmet", "_chestplate", "_leggings", "_boots");
    }

    private void migrateLegacyPricing() {
        ActivityEconomyConfig balanced = ActivityEconomyConfig.defaults();
        Map<String, Long> legacy = ActivityEconomyConfig.legacySellPrices();
        for (Map.Entry<String, Long> entry : balanced.sellPrices.entrySet()) {
            String id = entry.getKey();
            long newPrice = entry.getValue();
            Long current = this.sellPrices.get(id);
            Long oldDefault = legacy.get(id);
            if (current != null && (oldDefault == null || current.longValue() != oldDefault.longValue())) continue;
            this.sellPrices.put(id, newPrice);
        }
        this.migrateFallback("minecraft", 1L);
        this.migrateFallback("cobblemon", 2L);
        this.migrateFallback("cobblefurnies", 2L);
        if (this.apricornSellPrice == 3L) {
            this.apricornSellPrice = balanced.apricornSellPrice;
        }
        if (this.berrySellPrice == 4L) {
            this.berrySellPrice = balanced.berrySellPrice;
        }
    }

    private void migrateFallback(String namespace, long legacyPrice) {
        Long current = this.fallbackNamespaceSellPrices.get(namespace);
        if (current == null || current == legacyPrice) {
            this.fallbackNamespaceSellPrices.put(namespace, 0L);
        }
    }

    private void migrateEconomyV3() {
        ActivityEconomyConfig v3 = ActivityEconomyConfig.defaults();
        Map<String, Long> v2 = ActivityEconomyConfig.economyV2SellPrices();
        for (Map.Entry<String, Long> entry : v3.sellPrices.entrySet()) {
            Long current = this.sellPrices.get(entry.getKey());
            Long oldDefault = v2.get(entry.getKey());
            if (current != null && (oldDefault == null || current.longValue() != oldDefault.longValue())) continue;
            this.sellPrices.put(entry.getKey(), entry.getValue());
        }
        if (this.apricornSellPrice == 5L) {
            this.apricornSellPrice = v3.apricornSellPrice;
        }
        if (this.berrySellPrice == 7L) {
            this.berrySellPrice = v3.berrySellPrice;
        }
    }

    private static Map<String, Long> economyV2SellPrices() {
        String[] entries;
        LinkedHashMap<String, Long> prices = new LinkedHashMap<String, Long>();
        for (String entry : entries = new String[]{"wheat:3", "carrot:3", "potato:3", "beetroot:3", "pumpkin:6", "melon_slice:1", "sugar_cane:3", "cactus:3", "kelp:1", "sweet_berries:2", "bamboo:1", "honeycomb:5", "cod:5", "salmon:7", "tropical_fish:10", "pufferfish:12", "rotten_flesh:2", "bone:4", "string:4", "spider_eye:4", "gunpowder:6", "slime_ball:8", "ender_pearl:20", "blaze_rod:25", "ghast_tear:40", "phantom_membrane:18", "coal:4", "raw_copper:4", "raw_iron:9", "raw_gold:15", "copper_ingot:5", "iron_ingot:12", "gold_ingot:20", "redstone:3", "lapis_lazuli:5", "quartz:5", "amethyst_shard:8", "diamond:150", "emerald:100", "netherite_scrap:300"}) {
            int split = entry.lastIndexOf(58);
            prices.put("minecraft:" + entry.substring(0, split), Long.parseLong(entry.substring(split + 1)));
        }
        return prices;
    }

    private static Map<String, Long> legacySellPrices() {
        LinkedHashMap<String, Long> legacy = new LinkedHashMap<String, Long>();
        legacy.put("minecraft:wheat", 2L);
        legacy.put("minecraft:carrot", 2L);
        legacy.put("minecraft:potato", 2L);
        legacy.put("minecraft:beetroot", 2L);
        legacy.put("minecraft:pumpkin", 3L);
        legacy.put("minecraft:melon_slice", 1L);
        legacy.put("minecraft:sugar_cane", 2L);
        legacy.put("minecraft:cactus", 2L);
        legacy.put("minecraft:kelp", 1L);
        legacy.put("minecraft:sweet_berries", 2L);
        legacy.put("minecraft:cod", 3L);
        legacy.put("minecraft:salmon", 4L);
        legacy.put("minecraft:tropical_fish", 5L);
        legacy.put("minecraft:pufferfish", 5L);
        legacy.put("minecraft:rotten_flesh", 1L);
        legacy.put("minecraft:bone", 2L);
        legacy.put("minecraft:string", 2L);
        legacy.put("minecraft:spider_eye", 2L);
        legacy.put("minecraft:gunpowder", 3L);
        legacy.put("minecraft:slime_ball", 4L);
        legacy.put("minecraft:ender_pearl", 8L);
        legacy.put("minecraft:blaze_rod", 10L);
        legacy.put("minecraft:coal", 2L);
        legacy.put("minecraft:raw_copper", 2L);
        legacy.put("minecraft:raw_iron", 4L);
        legacy.put("minecraft:raw_gold", 6L);
        legacy.put("minecraft:copper_ingot", 3L);
        legacy.put("minecraft:iron_ingot", 5L);
        legacy.put("minecraft:gold_ingot", 8L);
        legacy.put("minecraft:redstone", 2L);
        legacy.put("minecraft:lapis_lazuli", 3L);
        legacy.put("minecraft:quartz", 3L);
        legacy.put("minecraft:amethyst_shard", 3L);
        legacy.put("minecraft:diamond", 35L);
        legacy.put("minecraft:emerald", 30L);
        return legacy;
    }

    private static boolean isPokedollCollectible(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return false;
        }
        String path = rawPath.toLowerCase(Locale.ROOT);
        return path.contains("pokedoll") || path.equals("applin_basket") || path.equals("shiny_applin_basket") || path.equals("magikarp_fishbowl") || path.equals("shiny_magikarp_fishbowl") || path.equals("eiscue_head_pile") || path.equals("shiny_eiscue_head_pile") || path.equals("luvdisc_cushion");
    }

    private static long pokeblocksPrice(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return 0L;
        }
        String path = rawPath.toLowerCase(Locale.ROOT);
        long base = ActivityEconomyConfig.containsAny(path, "venusaur", "blastoise", "gengar", "corviknight", "gholdengo", "kyogre", "trevenant", "marshadow", "skibidi_mewlet", "snorunt_family") ? 7500L : (ActivityEconomyConfig.containsAny(path, "calyrex", "ampharos", "applin", "sableye", "absol", "ivysaur", "magikarp", "stonjourner", "tropius", "palossand", "mimikyu", "riolu", "froslass", "beartic") ? 3600L : (ActivityEconomyConfig.containsAny(path, "flaaffy", "snorlax", "sentret", "furret", "munchlax", "rabsca", "wartortle", "quagsire", "corvisquire", "eevee", "cloyster", "wailord", "phantump", "glalie", "eiscue", "frigibax", "posing_bulbasaur") ? 2250L : (ActivityEconomyConfig.containsAny(path, "lickitung", "mareep", "dolliv", "arboliva", "wooper", "gastly", "shellder", "sandygast", "pumpkaboo", "bellossom", "piloswine", "delibird", "cubchoo", "cetoddle") ? 1350L : (ActivityEconomyConfig.containsAny(path, "bulbasaur", "charmander", "squirtle", "smoliv", "rellor", "happiny", "swinub", "drifloon", "rookidee", "wailmer", "rowlet", "treecko", "snorunt", "spheal", "luvdisc") ? 750L : 600L))));
        if (path.contains("shiny")) {
            base *= 2L;
        }
        if (path.contains("gigantic") || path.contains("giant")) {
            base *= 4L;
        }
        if (path.contains("animated")) {
            base = Math.max(base, base * 3L / 2L);
        }
        return base;
    }

    private static boolean containsAny(String value, String ... needles) {
        for (String needle : needles) {
            if (!value.contains(needle)) continue;
            return true;
        }
        return false;
    }

    private static boolean blockedFromAutoSell(Identifier id) {
        String rawId = id.toString();
        String path = id.getPath();
        return AUTO_SELL_BLOCKED.contains(rawId) || path.endsWith("_spawn_egg") || path.contains("debug") || path.contains("creative_only");
    }

    private static ActivityEconomyConfig defaults() {
        ActivityEconomyConfig config = new ActivityEconomyConfig();
        config.pricingVersion = 3;
        config.apricornSellPrice = 15L;
        config.berrySellPrice = 20L;
        config.sellPrices.put("minecraft:wheat", 8L);
        config.sellPrices.put("minecraft:carrot", 8L);
        config.sellPrices.put("minecraft:potato", 8L);
        config.sellPrices.put("minecraft:beetroot", 8L);
        config.sellPrices.put("minecraft:pumpkin", 15L);
        config.sellPrices.put("minecraft:melon_slice", 3L);
        config.sellPrices.put("minecraft:sugar_cane", 8L);
        config.sellPrices.put("minecraft:cactus", 8L);
        config.sellPrices.put("minecraft:kelp", 3L);
        config.sellPrices.put("minecraft:sweet_berries", 6L);
        config.sellPrices.put("minecraft:bamboo", 3L);
        config.sellPrices.put("minecraft:honeycomb", 15L);
        config.sellPrices.put("minecraft:cod", 20L);
        config.sellPrices.put("minecraft:salmon", 28L);
        config.sellPrices.put("minecraft:tropical_fish", 45L);
        config.sellPrices.put("minecraft:pufferfish", 55L);
        config.sellPrices.put("minecraft:rotten_flesh", 6L);
        config.sellPrices.put("minecraft:bone", 12L);
        config.sellPrices.put("minecraft:string", 12L);
        config.sellPrices.put("minecraft:spider_eye", 12L);
        config.sellPrices.put("minecraft:gunpowder", 25L);
        config.sellPrices.put("minecraft:slime_ball", 30L);
        config.sellPrices.put("minecraft:ender_pearl", 75L);
        config.sellPrices.put("minecraft:blaze_rod", 100L);
        config.sellPrices.put("minecraft:ghast_tear", 200L);
        config.sellPrices.put("minecraft:phantom_membrane", 80L);
        config.sellPrices.put("minecraft:coal", 20L);
        config.sellPrices.put("minecraft:raw_copper", 18L);
        config.sellPrices.put("minecraft:raw_iron", 45L);
        config.sellPrices.put("minecraft:raw_gold", 75L);
        config.sellPrices.put("minecraft:copper_ingot", 22L);
        config.sellPrices.put("minecraft:iron_ingot", 60L);
        config.sellPrices.put("minecraft:gold_ingot", 100L);
        config.sellPrices.put("minecraft:redstone", 15L);
        config.sellPrices.put("minecraft:lapis_lazuli", 25L);
        config.sellPrices.put("minecraft:quartz", 25L);
        config.sellPrices.put("minecraft:amethyst_shard", 40L);
        config.sellPrices.put("minecraft:diamond", 500L);
        config.sellPrices.put("minecraft:emerald", 350L);
        config.sellPrices.put("minecraft:netherite_scrap", 1400L);
        config.fallbackNamespaceSellPrices.put("minecraft", 0L);
        config.fallbackNamespaceSellPrices.put("cobblemon", 0L);
        config.fallbackNamespaceSellPrices.put("cobblefurnies", 0L);
        return config;
    }
}

