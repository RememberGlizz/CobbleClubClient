/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.cobbleclub.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cobbleclub-server.json");
    public int configVersion = 24;
    public boolean storeEnabled = false;
    public String storeApiUrl = "https://store.example.com";
    public String storeBridgeToken = "";
    public int storePollIntervalSeconds = 10;
    public boolean claimsEnabled = true;
    public int initialClaimBlocks = 700;
    public int claimBlocksPerReward = 0;
    public int claimBlockRewardIntervalSeconds = 1800;
    public int gemsPerPlaytimeReward = 2;
    public int claimBlockPurchaseAmount = 256;
    public long claimBlockPurchasePrice = 5L;
    public double claimBlockPurchasePriceMultiplier = 2.7;
    public long claimBlockPurchasePriceCap = Long.MAX_VALUE;
    public int mapRadiusChunks = 24;
    public int maxClaimChunksPerSide = 64;
    public int maxClaimDistanceChunks = 24;
    public boolean operatorsBypassClaims = true;
    public boolean claimEntryMessages = true;
    public boolean economyEnabled = true;
    public String currencyName = "Pok\u00e9Dollars";
    public String currencySymbol = "\u20bd";
    public long startingBalance = 400L;
    public long startingGems = 5L;
    public long playtimeMoneyReward = 150L;
    public int playtimeMoneyIntervalSeconds = 3600;
    public long dailyCooldownSeconds = 43200L;
    public long deathMoneyPenalty = 100L;
    public boolean dailyRewardsEnabled = true;
    public long dailyMoney = 500L;
    public int dailyClaimBlocks = 0;
    public int dailyGems = 5;
    public int dailyVoteKeys = 0;
    public boolean voteRewardsEnabled = true;
    public long voteMoneyReward = 250L;
    public int voteKeysPerVote = 1;
    public List<VoteSite> voteSites = new ArrayList<VoteSite>(List.of(new VoteSite("Planet Minecraft", "https://www.planetminecraft.com/server/cobbleclub-custom-modded-cobblemon/vote/")));
    public boolean newbKitEnabled = true;
    public long newbKitCooldownSeconds = 43200L;
    public String newbKitPokedexItem = "cobblemon:pokedex_red";
    public String newbKitPokeBallItem = "cobblemon:poke_ball";
    public String newbKitGreatBallItem = "cobblemon:great_ball";
    public String newbKitUltraBallItem = "cobblemon:ultra_ball";
    public String newbKitPcItem = "cobblemon:pc";
    public String newbKitHealingMachineItem = "cobblemon:healing_machine";
    public String newbKitTrainerCardItem = "rctmod:trainer_card";
    public long rankKitCooldownSeconds = 64800L;
    public String aceKitPermission = "cobbleclub.kit.ace";
    public String championKitPermission = "cobbleclub.kit.champion";
    public String masterKitPermission = "cobbleclub.kit.master";
    public String legendKitPermission = "cobbleclub.kit.legend";
    public String kitRareCandyItem = "cobblemon:rare_candy";
    public String kitQuickBallItem = "cobblemon:quick_ball";
    public String kitBeastBallItem = "cobblemon:beast_ball";
    public String kitMasterBallItem = "cobblemon:master_ball";
    public long kitCooldownReductionPrice = 500L;
    public long kitCooldownReductionStepSeconds = 3600L;
    public long kitCooldownReductionMaxSeconds = 14400L;
    public long legendKitCooldownReductionMaxSeconds = 21600L;
    public boolean showLockedWardrobeItems = true;
    public boolean scoreboardTagsEnabled = true;
    public boolean broadcastRareCrateWins = true;
    public String pokemonRewardCommand = "pokegive {player} {species} {aspects}";
    public List<TagCategory> tagCategories = new ArrayList<TagCategory>();
    public List<TagDefinition> tags = new ArrayList<TagDefinition>();
    public List<CosmeticDefinition> cosmetics = new ArrayList<CosmeticDefinition>();
    public List<GlowDefinition> glows = new ArrayList<GlowDefinition>();
    public List<GearSetDefinition> gearSets = new ArrayList<GearSetDefinition>();
    public PokemonCatalog pokemonCatalog = new PokemonCatalog();
    public List<CrateDefinition> crates = new ArrayList<CrateDefinition>();
    public List<CrateLocation> crateLocations = new ArrayList<CrateLocation>();

    public static ServerConfig load() {
        ServerConfig config;
        try {
            Files.createDirectories(PATH.getParent(), new FileAttribute[0]);
            if (Files.exists(PATH, new LinkOption[0]) && (config = (ServerConfig)GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), ServerConfig.class)) != null) {
                config.normalize();
                config.save();
                return config;
            }
        }
        catch (Exception config2) {
            // empty catch block
        }
        config = ServerConfig.defaults();
        config.save();
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
        int loadedVersion = this.configVersion;
        if (this.currencyName == null || this.currencyName.isBlank()) {
            this.currencyName = "Pok\u00e9Dollars";
        }
        if (this.currencySymbol == null) {
            this.currencySymbol = "";
        }
        if (this.pokemonRewardCommand == null) {
            this.pokemonRewardCommand = "";
        }
        if (this.newbKitPokedexItem == null || this.newbKitPokedexItem.isBlank()) {
            this.newbKitPokedexItem = "cobblemon:pokedex_red";
        }
        if (this.newbKitPokeBallItem == null || this.newbKitPokeBallItem.isBlank()) {
            this.newbKitPokeBallItem = "cobblemon:poke_ball";
        }
        if (this.newbKitGreatBallItem == null || this.newbKitGreatBallItem.isBlank()) {
            this.newbKitGreatBallItem = "cobblemon:great_ball";
        }
        if (this.newbKitUltraBallItem == null || this.newbKitUltraBallItem.isBlank()) {
            this.newbKitUltraBallItem = "cobblemon:ultra_ball";
        }
        if (this.newbKitPcItem == null || this.newbKitPcItem.isBlank()) {
            this.newbKitPcItem = "cobblemon:pc";
        }
        if (this.newbKitHealingMachineItem == null || this.newbKitHealingMachineItem.isBlank()) {
            this.newbKitHealingMachineItem = "cobblemon:healing_machine";
        }
        if (this.newbKitTrainerCardItem == null || this.newbKitTrainerCardItem.isBlank()) {
            this.newbKitTrainerCardItem = "rctmod:trainer_card";
        }
        if (this.aceKitPermission == null || this.aceKitPermission.isBlank()) {
            this.aceKitPermission = "cobbleclub.kit.ace";
        }
        if (this.championKitPermission == null || this.championKitPermission.isBlank()) {
            this.championKitPermission = "cobbleclub.kit.champion";
        }
        if (this.masterKitPermission == null || this.masterKitPermission.isBlank()) {
            this.masterKitPermission = "cobbleclub.kit.master";
        }
        if (this.legendKitPermission == null || this.legendKitPermission.isBlank()) {
            this.legendKitPermission = "cobbleclub.kit.legend";
        }
        if (this.kitRareCandyItem == null || this.kitRareCandyItem.isBlank()) {
            this.kitRareCandyItem = "cobblemon:rare_candy";
        }
        if (this.kitQuickBallItem == null || this.kitQuickBallItem.isBlank()) {
            this.kitQuickBallItem = "cobblemon:quick_ball";
        }
        if (this.kitBeastBallItem == null || this.kitBeastBallItem.isBlank()) {
            this.kitBeastBallItem = "cobblemon:beast_ball";
        }
        if (this.kitMasterBallItem == null || this.kitMasterBallItem.isBlank()) {
            this.kitMasterBallItem = "cobblemon:master_ball";
        }
        if (this.voteSites == null) {
            this.voteSites = new ArrayList<VoteSite>();
        }
        this.voteSites.removeIf(site -> site == null || site.url == null || site.url.isBlank());
        this.voteSites.forEach(VoteSite::normalize);
        if (this.tagCategories == null) {
            this.tagCategories = new ArrayList<TagCategory>();
        }
        if (this.tags == null) {
            this.tags = new ArrayList<TagDefinition>();
        }
        if (this.cosmetics == null) {
            this.cosmetics = new ArrayList<CosmeticDefinition>();
        }
        if (this.glows == null) {
            this.glows = new ArrayList<GlowDefinition>();
        }
        if (this.gearSets == null) {
            this.gearSets = new ArrayList<GearSetDefinition>();
        }
        if (this.pokemonCatalog == null) {
            this.pokemonCatalog = new PokemonCatalog();
        }
        this.pokemonCatalog.normalize();
        if (this.crates == null) {
            this.crates = new ArrayList<CrateDefinition>();
        }
        this.crates.removeIf(crate -> crate == null);
        this.crates.forEach(CrateDefinition::normalize);
        if (this.crateLocations == null) {
            this.crateLocations = new ArrayList<CrateLocation>();
        }
        if (loadedVersion < 3) {
            ServerConfig.addWardrobeExpansion(this.cosmetics, this.glows);
        }
        if (loadedVersion < 4) {
            this.initialClaimBlocks = 700;
            if (this.claimBlocksPerReward == 256) {
                this.claimBlocksPerReward = 0;
            }
            if (this.claimBlockPurchaseAmount == 4096) {
                this.claimBlockPurchaseAmount = 256;
            }
            if (this.claimBlockPurchasePrice == 2500L) {
                this.claimBlockPurchasePrice = 25L;
            }
            if (this.dailyClaimBlocks == 1024) {
                this.dailyClaimBlocks = 0;
            }
            this.gemsPerPlaytimeReward = Math.max(2, this.gemsPerPlaytimeReward);
            this.dailyGems = Math.max(5, this.dailyGems);
        }
        if (loadedVersion < 5) {
            ServerConfig.addGearExpansion(this.gearSets);
        }
        if (loadedVersion < 6) {
            this.newbKitEnabled = true;
            this.newbKitCooldownSeconds = 43200L;
        }
        if (loadedVersion < 8) {
            this.rankKitCooldownSeconds = 64800L;
        }
        if (loadedVersion < 9) {
            this.kitCooldownReductionPrice = 500L;
            this.kitCooldownReductionStepSeconds = 3600L;
            this.kitCooldownReductionMaxSeconds = 14400L;
            this.legendKitCooldownReductionMaxSeconds = 21600L;
        }
        if (loadedVersion < 10) {
            this.startingBalance = 400L;
            this.startingGems = 5L;
            this.gemsPerPlaytimeReward = 2;
            this.claimBlockRewardIntervalSeconds = 1800;
            this.playtimeMoneyReward = 150L;
            this.playtimeMoneyIntervalSeconds = 3600;
            this.dailyCooldownSeconds = 43200L;
            this.dailyVoteKeys = 0;
            this.deathMoneyPenalty = 100L;
            ServerConfig.replaceCosmetic(this.cosmetics, "pikachu_buddy_balloon", "minecraft:paper", 22201);
            ServerConfig.replaceCosmetic(this.cosmetics, "jigglypuff_moon_balloon", "minecraft:paper", 22202);
            ServerConfig.replaceCosmetic(this.cosmetics, "drifloon_festival_balloon", "minecraft:paper", 22203);
            ServerConfig.replaceCosmetic(this.cosmetics, "pokeball_club_balloon", "minecraft:paper", 22204);
            ServerConfig.replaceCosmetic(this.cosmetics, "club_balloon", "minecraft:paper", 22205);
        }
        if (loadedVersion < 11) {
            ServerConfig.replaceCosmetic(this.cosmetics, "pikachu_buddy_balloon", "minecraft:paper", 22201);
            ServerConfig.replaceCosmetic(this.cosmetics, "jigglypuff_moon_balloon", "minecraft:paper", 22202);
            ServerConfig.replaceCosmetic(this.cosmetics, "drifloon_festival_balloon", "minecraft:paper", 22203);
            ServerConfig.replaceCosmetic(this.cosmetics, "pokeball_club_balloon", "minecraft:paper", 22204);
            ServerConfig.replaceCosmetic(this.cosmetics, "club_balloon", "minecraft:paper", 22205);
        }
        if (loadedVersion < 12) {
            ServerConfig.addFloaties(this.cosmetics);
        }
        if (loadedVersion < 13) {
            ServerConfig.replaceCosmetic(this.cosmetics, "club_crown", "minecraft:golden_helmet", 22005);
        }
        if (loadedVersion < 14) {
            ServerConfig.addHatColorways(this.cosmetics);
        }
        if (loadedVersion < 15) {
            ServerConfig.renameCosmetic(this.cosmetics, "gengar_grin_mask", "gengar-hat", "Gengar Hat");
        }
        if (loadedVersion < 16) {
            ServerConfig.addPokemonFloaties(this.cosmetics);
        }
        if (loadedVersion < 17) {
            ServerConfig.addBalloonVariants(this.cosmetics);
            ServerConfig.ensureRankTags(this.tagCategories, this.tags);
        }
        if (loadedVersion < 18) {
            ServerConfig.renameCosmetic(this.cosmetics, "pikachu_floaty", "squirtle_floaty", "Squirtle Floaty");
        }
        if (loadedVersion < 19) {
            this.dailyVoteKeys = 0;
            this.voteRewardsEnabled = true;
            this.voteMoneyReward = 250L;
            this.voteKeysPerVote = 1;
        }
        if (loadedVersion < 20) {
            if (this.claimBlockPurchasePrice == 25L) {
                this.claimBlockPurchasePrice = 5L;
            }
            this.claimBlockPurchasePriceMultiplier = 1.2;
            this.claimBlockPurchasePriceCap = 2500L;
        }
        if (loadedVersion < 21 && this.voteSites.isEmpty()) {
            this.voteSites.add(new VoteSite("Planet Minecraft", "https://www.planetminecraft.com/server/cobbleclub-custom-modded-cobblemon/vote/"));
        }
        if (loadedVersion < 22) {
            this.claimBlockPurchasePriceMultiplier = 2.7;
            if (this.claimBlockPurchasePriceCap <= 2500L) {
                this.claimBlockPurchasePriceCap = Long.MAX_VALUE;
            }
        }
        if (loadedVersion < 23) {
            ServerConfig.ensureRankTags(this.tagCategories, this.tags);
        }
        if (loadedVersion < 24) {
            ServerConfig.ensureRankTags(this.tagCategories, this.tags);
        }
        this.newbKitCooldownSeconds = Math.max(0L, this.newbKitCooldownSeconds);
        this.rankKitCooldownSeconds = Math.max(0L, this.rankKitCooldownSeconds);
        this.kitCooldownReductionPrice = Math.max(0L, this.kitCooldownReductionPrice);
        this.kitCooldownReductionStepSeconds = Math.max(1L, this.kitCooldownReductionStepSeconds);
        this.kitCooldownReductionMaxSeconds = Math.max(0L, this.kitCooldownReductionMaxSeconds);
        this.legendKitCooldownReductionMaxSeconds = Math.max(this.kitCooldownReductionMaxSeconds, this.legendKitCooldownReductionMaxSeconds);
        this.voteMoneyReward = Math.max(0L, this.voteMoneyReward);
        this.voteKeysPerVote = Math.max(0, this.voteKeysPerVote);
        this.claimBlockPurchasePrice = Math.max(1L, this.claimBlockPurchasePrice);
        if (!Double.isFinite(this.claimBlockPurchasePriceMultiplier)) {
            this.claimBlockPurchasePriceMultiplier = 2.7;
        }
        this.claimBlockPurchasePriceMultiplier = Math.max(1.0, Math.min(10.0, this.claimBlockPurchasePriceMultiplier));
        this.claimBlockPurchasePriceCap = Math.max(this.claimBlockPurchasePrice, this.claimBlockPurchasePriceCap);
        this.configVersion = Math.max(24, this.configVersion);
    }

    private static ServerConfig defaults() {
        ServerConfig config = new ServerConfig();
        config.tagCategories.add(new TagCategory("general", "General"));
        config.tagCategories.add(new TagCategory("achievements", "Achievements"));
        config.tags.add(new TagDefinition("trainer", "{\"text\":\"\ue100\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Default CobbleClub player tag", "gray")), "general", true, 0L));
        config.tags.add(new TagDefinition("shiny_hunter", "{\"text\":\"\ue108\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Premium tag \u00b7 purchase for 50,000 Pok\u00e9Dollars", "gray")), "achievements", false, 50000L));
        config.cosmetics.add(new CosmeticDefinition("club_crown", "Club Crown", ServerConfig.json("Club Crown", "gold"), "HELMET", "minecraft:golden_helmet", 22005, false, true, List.of(ServerConfig.json("Unlock this cosmetic first", "red")), List.of(ServerConfig.json("Click to wear the CobbleClub crown", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        config.cosmetics.add(new CosmeticDefinition("club_wings", "Club Wings", ServerConfig.json("Club Wings", "light_purple"), "BACKPACK", "minecraft:elytra", 0, true, false, List.of(ServerConfig.json("Purchase for 12,500 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Click to equip", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 12500L));
        config.cosmetics.add(new CosmeticDefinition("club_balloon", "Club Balloon", ServerConfig.json("Club Balloon", "aqua"), "BALLOON", "minecraft:paper", 22205, true, false, List.of(ServerConfig.json("Purchase for 10,000 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Click to equip", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 10000L));
        config.glows.add(new GlowDefinition("club_purple", "Club Purple", ServerConfig.json("Club Purple", "light_purple"), List.of(Integer.valueOf(10182117), Integer.valueOf(13073919), Integer.valueOf(8072383)), true, List.of(ServerConfig.json("Unlock this glow first", "red")), List.of(ServerConfig.json("Click to enable", "green")), List.of(ServerConfig.json("Click to disable", "yellow")), 0L));
        ServerConfig.addWardrobeExpansion(config.cosmetics, config.glows);
        ServerConfig.addFloaties(config.cosmetics);
        ServerConfig.addHatColorways(config.cosmetics);
        ServerConfig.renameCosmetic(config.cosmetics, "gengar_grin_mask", "gengar-hat", "Gengar Hat");
        ServerConfig.addPokemonFloaties(config.cosmetics);
        ServerConfig.addBalloonVariants(config.cosmetics);
        ServerConfig.ensureRankTags(config.tagCategories, config.tags);
        config.gearSets.add(new GearSetDefinition("adventure", ServerConfig.json("CobbleClub Adventure Gear", "light_purple"), List.of("minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots")));
        ServerConfig.addGearExpansion(config.gearSets);
        PokemonFamily favorites = new PokemonFamily();
        favorites.id = "club_favorites";
        favorites.displayName = "CobbleClub Favorites";
        favorites.shortName = "Favorites";
        favorites.skinAspect = "cobbleclub";
        favorites.entries.add(PokemonEntry.of("pikachu", "CobbleClub Pikachu"));
        favorites.entries.add(PokemonEntry.of("eevee", "CobbleClub Eevee"));
        config.pokemonCatalog.families.add(favorites);
        config.crates.add(CrateDefinition.vote());
        config.crates.add(CrateDefinition.shiny());
        config.crates.add(CrateDefinition.legendary());
        return config;
    }

    private static void addWardrobeExpansion(List<CosmeticDefinition> cosmetics, List<GlowDefinition> glows) {
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("pika_spark_cap", "Pika Spark Cap", ServerConfig.json("Pika Spark Cap", "yellow"), "HELMET", "minecraft:leather_helmet", 22001, false, true, List.of(ServerConfig.json("A bright electric trainer cap", "gray")), List.of(ServerConfig.json("Equip the electric-inspired cap", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("eevee_explorer_hood", "Eevee Explorer Hood", ServerConfig.json("Eevee Explorer Hood", "gold"), "HELMET", "minecraft:leather_helmet", 22002, false, true, List.of(ServerConfig.json("A fixed-texture evolution-adventure hood", "gray")), List.of(ServerConfig.json("Equip the evolution-inspired hood", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("gengar-hat", "Gengar Hat", ServerConfig.json("Gengar Hat", "dark_purple"), "HELMET", "minecraft:leather_helmet", 22003, false, false, List.of(ServerConfig.json("Click to buy or use /wardrobe buy cosmetic gengar-hat", "red")), List.of(ServerConfig.json("Equip the ghost-inspired grin", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 6500L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("lucario_aura_crest", "Lucario Aura Crest", ServerConfig.json("Lucario Aura Crest", "blue"), "HELMET", "minecraft:leather_helmet", 22004, false, false, List.of(ServerConfig.json("Buy with /wardrobe buy cosmetic lucario_aura_crest", "red")), List.of(ServerConfig.json("Equip the aura-inspired crest", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 8000L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("charizard_flame_pack", "Charizard Flame Wings", ServerConfig.json("Charizard Flame Wings", "gold"), "BACKPACK", "minecraft:elytra", 22101, false, true, List.of(ServerConfig.json("A fiery dragon-inspired travel pack", "gray")), List.of(ServerConfig.json("Equip the flame pack", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("bulbasaur_sprout_pack", "Bulbasaur Sprout Wings", ServerConfig.json("Bulbasaur Sprout Wings", "green"), "BACKPACK", "minecraft:elytra", 22104, false, true, List.of(ServerConfig.json("A leafy partner-inspired pack", "gray")), List.of(ServerConfig.json("Equip the sprout pack", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("sylveon_ribbon_pack", "Sylveon Ribbon Wings", ServerConfig.json("Sylveon Ribbon Wings", "light_purple"), "BACKPACK", "minecraft:elytra", 22102, false, false, List.of(ServerConfig.json("Buy with /wardrobe buy cosmetic sylveon_ribbon_pack", "red")), List.of(ServerConfig.json("Equip the ribbon-inspired pack", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 9000L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("lucario_aura_pack", "Lucario Aura Wings", ServerConfig.json("Lucario Aura Wings", "aqua"), "BACKPACK", "minecraft:elytra", 22103, false, false, List.of(ServerConfig.json("Buy with /wardrobe buy cosmetic lucario_aura_pack", "red")), List.of(ServerConfig.json("Equip the aura pack", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 9000L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("pikachu_buddy_balloon", "Pikachu Buddy Balloon", ServerConfig.json("Pikachu Buddy Balloon", "yellow"), "BALLOON", "minecraft:paper", 22201, false, true, List.of(ServerConfig.json("A cheerful electric-inspired balloon", "gray")), List.of(ServerConfig.json("Float the buddy balloon", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("jigglypuff_moon_balloon", "Jigglypuff Moon Balloon", ServerConfig.json("Jigglypuff Moon Balloon", "light_purple"), "BALLOON", "minecraft:paper", 22202, false, true, List.of(ServerConfig.json("A round song-partner-inspired balloon", "gray")), List.of(ServerConfig.json("Float the moon balloon", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("drifloon_festival_balloon", "Drifloon Festival Balloon", ServerConfig.json("Drifloon Festival Balloon", "dark_purple"), "BALLOON", "minecraft:paper", 22203, false, false, List.of(ServerConfig.json("Buy with /wardrobe buy cosmetic drifloon_festival_balloon", "red")), List.of(ServerConfig.json("Float the ghost-inspired balloon", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 7000L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("pokeball_club_balloon", "Pok\u00e9 Ball Club Balloon", ServerConfig.json("Pok\u00e9 Ball Club Balloon", "red"), "BALLOON", "minecraft:paper", 22204, false, false, List.of(ServerConfig.json("Buy with /wardrobe buy cosmetic pokeball_club_balloon", "red")), List.of(ServerConfig.json("Float the trainer balloon", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 7500L));
        ServerConfig.addIfMissing(glows, new GlowDefinition("spark_yellow", "Spark Yellow", ServerConfig.json("Spark Yellow", "yellow"), List.of(Integer.valueOf(16638023), Integer.valueOf(16096779), Integer.valueOf(16775086)), true, List.of(ServerConfig.json("Electric-inspired glow", "gray")), List.of(ServerConfig.json("Enable Spark Yellow", "green")), List.of(ServerConfig.json("Disable glow", "yellow")), 0L));
        ServerConfig.addIfMissing(glows, new GlowDefinition("aura_blue", "Aura Blue", ServerConfig.json("Aura Blue", "aqua"), List.of(Integer.valueOf(3718648), Integer.valueOf(2450411), Integer.valueOf(9684477)), true, List.of(ServerConfig.json("Aura-inspired glow", "gray")), List.of(ServerConfig.json("Enable Aura Blue", "green")), List.of(ServerConfig.json("Disable glow", "yellow")), 0L));
        ServerConfig.addIfMissing(glows, new GlowDefinition("fairy_ribbon", "Fairy Ribbon", ServerConfig.json("Fairy Ribbon", "light_purple"), List.of(Integer.valueOf(16361684), Integer.valueOf(12891645), Integer.valueOf(16502760)), false, List.of(ServerConfig.json("Buy with /wardrobe buy glow fairy_ribbon", "red")), List.of(ServerConfig.json("Enable Fairy Ribbon", "green")), List.of(ServerConfig.json("Disable glow", "yellow")), 6000L));
    }

    private static void addHatColorways(List<CosmeticDefinition> cosmetics) {
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("gengar_grin_crimson", "Gengar Hat - Crimson", ServerConfig.json("Gengar Hat - Crimson", "red"), "HELMET", "minecraft:leather_helmet", 22006, false, false, List.of(ServerConfig.json("Crimson colorway of the Gengar hat", "gray")), List.of(ServerConfig.json("Equip the Crimson Gengar hat", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 6500L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("gengar_grin_azure", "Gengar Hat - Azure", ServerConfig.json("Gengar Hat - Azure", "blue"), "HELMET", "minecraft:leather_helmet", 22007, false, false, List.of(ServerConfig.json("Azure colorway of the Gengar hat", "gray")), List.of(ServerConfig.json("Equip the Azure Gengar hat", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 6500L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("gengar_grin_toxic", "Gengar Hat - Toxic", ServerConfig.json("Gengar Hat - Toxic", "green"), "HELMET", "minecraft:leather_helmet", 22008, false, false, List.of(ServerConfig.json("Toxic green colorway of the Gengar hat", "gray")), List.of(ServerConfig.json("Equip the Toxic Gengar hat", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 6500L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("froggy_hat_blue", "Froggy Hat - Blue", ServerConfig.json("Froggy Hat - Blue", "blue"), "HELMET", "minecraft:leather_helmet", 22009, false, true, List.of(ServerConfig.json("Blue colorway of the Froggy hat", "gray")), List.of(ServerConfig.json("Equip the Blue Froggy hat", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("froggy_hat_pink", "Froggy Hat - Pink", ServerConfig.json("Froggy Hat - Pink", "light_purple"), "HELMET", "minecraft:leather_helmet", 22010, false, true, List.of(ServerConfig.json("Pink colorway of the Froggy hat", "gray")), List.of(ServerConfig.json("Equip the Pink Froggy hat", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("froggy_hat_purple", "Froggy Hat - Purple", ServerConfig.json("Froggy Hat - Purple", "dark_purple"), "HELMET", "minecraft:leather_helmet", 22011, false, true, List.of(ServerConfig.json("Purple colorway of the Froggy hat", "gray")), List.of(ServerConfig.json("Equip the Purple Froggy hat", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
    }

    private static void addFloaties(List<CosmeticDefinition> cosmetics) {
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("duck_floaty", "Duck Floaty", ServerConfig.json("Duck Floaty", "yellow"), "BALLOON", "minecraft:paper", 22301, false, true, List.of(ServerConfig.json("A cheerful pool floaty", "gray")), List.of(ServerConfig.json("Equip the Duck Floaty", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("dino_floaty", "Dino Floaty", ServerConfig.json("Dino Floaty", "green"), "BALLOON", "minecraft:paper", 22302, false, true, List.of(ServerConfig.json("A playful dinosaur floaty", "gray")), List.of(ServerConfig.json("Equip the Dino Floaty", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 0L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("flamingo_floaty", "Flamingo Floaty", ServerConfig.json("Flamingo Floaty", "light_purple"), "BALLOON", "minecraft:paper", 22303, false, false, List.of(ServerConfig.json("Buy with /wardrobe buy cosmetic flamingo_floaty", "red")), List.of(ServerConfig.json("Equip the Flamingo Floaty", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 5000L));
    }

    private static void addPokemonFloaties(List<CosmeticDefinition> cosmetics) {
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("squirtle_floaty", "Squirtle Floaty", ServerConfig.json("Squirtle Floaty", "aqua"), "BALLOON", "minecraft:paper", 22304, false, false, List.of(ServerConfig.json("Click to buy for 7,500 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Equip the Squirtle Floaty", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 7500L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("gengar_floaty", "Gengar Floaty", ServerConfig.json("Gengar Floaty", "dark_purple"), "BALLOON", "minecraft:paper", 22305, false, false, List.of(ServerConfig.json("Click to buy for 8,000 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Equip the Gengar Floaty", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 8000L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("eevee_floaty", "Eevee Floaty", ServerConfig.json("Eevee Floaty", "gold"), "BALLOON", "minecraft:paper", 22306, false, false, List.of(ServerConfig.json("Click to buy for 7,500 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Equip the Eevee Floaty", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 7500L));
    }

    private static void addBalloonVariants(List<CosmeticDefinition> cosmetics) {
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("master_ball_balloon", "Master Ball Balloon", ServerConfig.json("Master Ball Balloon", "dark_purple"), "BALLOON", "minecraft:paper", 22206, false, false, List.of(ServerConfig.json("Click to buy for 12,500 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Equip the Master Ball Balloon", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 12500L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("premier_ball_balloon", "Premier Ball Balloon", ServerConfig.json("Premier Ball Balloon", "white"), "BALLOON", "minecraft:paper", 22207, false, false, List.of(ServerConfig.json("Click to buy for 7,500 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Equip the Premier Ball Balloon", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 7500L));
        ServerConfig.addIfMissing(cosmetics, new CosmeticDefinition("ultra_ball_balloon", "Ultra Ball Balloon", ServerConfig.json("Ultra Ball Balloon", "yellow"), "BALLOON", "minecraft:paper", 22208, false, false, List.of(ServerConfig.json("Click to buy for 10,000 Pok\u00e9Dollars", "red")), List.of(ServerConfig.json("Equip the Ultra Ball Balloon", "green")), List.of(ServerConfig.json("Click to remove", "yellow")), 10000L));
    }

    private static void ensureRankTags(List<TagCategory> categories, List<TagDefinition> tags) {
        if (categories.stream().noneMatch(c -> c != null && "ranks".equals(c.id()))) {
            categories.add(new TagCategory("ranks", "Ranks"));
        }
        if (categories.stream().noneMatch(c -> c != null && "staff".equals(c.id()))) {
            categories.add(new TagCategory("staff", "Staff"));
        }
        if (categories.stream().noneMatch(c -> c != null && "achievements".equals(c.id()))) {
            categories.add(new TagCategory("achievements", "Achievements"));
        }
        ServerConfig.upsertTag(tags, new TagDefinition("trainer", "{\"text\":\"\ue100\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Default tag \u00b7 available to every rank", "gray")), "general", true, 0L));
        ServerConfig.upsertTag(tags, new TagDefinition("newb", "{\"text\":\"\ue101\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Free alternate tag for the default rank", "gray")), "general", true, 0L));
        ServerConfig.upsertTag(tags, new TagDefinition("shiny_hunter", "{\"text\":\"\ue108\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Premium tag \u00b7 purchase for 50,000 Pok\u00e9Dollars", "gray")), "achievements", false, 50000L));
        ServerConfig.upsertTag(tags, new TagDefinition("ace", "{\"text\":\"\ue102\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Unlocked automatically by the Ace LuckPerms rank", "gray")), "ranks", false, 0L));
        ServerConfig.upsertTag(tags, new TagDefinition("champion", "{\"text\":\"\ue103\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Unlocked automatically by the Champion LuckPerms rank", "gray")), "ranks", false, 0L));
        ServerConfig.upsertTag(tags, new TagDefinition("master", "{\"text\":\"\ue104\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Unlocked automatically by the Master LuckPerms rank", "gray")), "ranks", false, 0L));
        ServerConfig.upsertTag(tags, new TagDefinition("legend", "{\"text\":\"\ue105\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("Unlocked automatically by the Legend LuckPerms rank", "gray")), "ranks", false, 0L));
        ServerConfig.upsertTag(tags, new TagDefinition("mod", "{\"text\":\"\ue106\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("CobbleClub moderation team", "gray")), "staff", false, 0L));
        ServerConfig.upsertTag(tags, new TagDefinition("admin", "{\"text\":\"\ue107\",\"font\":\"cobbleclub:ranks\",\"color\":\"white\"}", List.of(ServerConfig.json("CobbleClub administration team", "gray")), "staff", false, 0L));
    }

    private static void upsertTag(List<TagDefinition> tags, TagDefinition value) {
        for (int i = 0; i < tags.size(); ++i) {
            TagDefinition old = tags.get(i);
            if (old == null || !value.id().equals(old.id())) continue;
            tags.set(i, value);
            return;
        }
        tags.add(value);
    }

    private static void renameCosmetic(List<CosmeticDefinition> definitions, String oldId, String newId, String newName) {
        for (int index = 0; index < definitions.size(); ++index) {
            CosmeticDefinition old = definitions.get(index);
            if (old == null || !oldId.equals(old.id())) continue;
            definitions.set(index, new CosmeticDefinition(newId, newName, ServerConfig.json(newName, "dark_purple"), old.slot(), old.material(), old.customModelData(), old.dyeable(), old.ownedByDefault(), old.lockedLore(), old.equipLore(), old.unequipLore(), old.price()));
            return;
        }
    }

    private static void addIfMissing(List<CosmeticDefinition> definitions, CosmeticDefinition addition) {
        if (definitions.stream().noneMatch(existing -> existing != null && addition.id().equals(existing.id()))) {
            definitions.add(addition);
        }
    }

    private static void replaceCosmetic(List<CosmeticDefinition> definitions, String id, String material, int customModelData) {
        for (int index = 0; index < definitions.size(); ++index) {
            CosmeticDefinition old = definitions.get(index);
            if (old == null || !id.equals(old.id())) continue;
            definitions.set(index, new CosmeticDefinition(old.id(), old.displayName(), old.displayNameJson(), old.slot(), material, customModelData, old.dyeable(), old.ownedByDefault(), old.lockedLore(), old.equipLore(), old.unequipLore(), old.price()));
            return;
        }
    }

    private static void addIfMissing(List<GlowDefinition> definitions, GlowDefinition addition) {
        if (definitions.stream().noneMatch(existing -> existing != null && addition.id().equals(existing.id()))) {
            definitions.add(addition);
        }
    }

    private static void addGearExpansion(List<GearSetDefinition> gearSets) {
        ServerConfig.addGearIfMissing(gearSets, new GearSetDefinition("spark_vanguard", ServerConfig.json("Spark Vanguard Armor", "yellow"), List.of("cobbleclub:spark_helmet", "cobbleclub:spark_chestplate", "cobbleclub:spark_leggings", "cobbleclub:spark_boots")));
        ServerConfig.addGearIfMissing(gearSets, new GearSetDefinition("spectral_phantom", ServerConfig.json("Spectral Phantom Armor", "dark_purple"), List.of("cobbleclub:spectral_helmet", "cobbleclub:spectral_chestplate", "cobbleclub:spectral_leggings", "cobbleclub:spectral_boots")));
        ServerConfig.addGearIfMissing(gearSets, new GearSetDefinition("aura_guardian", ServerConfig.json("Aura Guardian Armor", "aqua"), List.of("cobbleclub:aura_helmet", "cobbleclub:aura_chestplate", "cobbleclub:aura_leggings", "cobbleclub:aura_boots")));
        ServerConfig.addGearIfMissing(gearSets, new GearSetDefinition("fairy_bloom", ServerConfig.json("Fairy Bloom Armor", "light_purple"), List.of("cobbleclub:fairy_helmet", "cobbleclub:fairy_chestplate", "cobbleclub:fairy_leggings", "cobbleclub:fairy_boots")));
        ServerConfig.addGearIfMissing(gearSets, new GearSetDefinition("elemental_tools", ServerConfig.json("CobbleClub Elemental Arsenal", "gold"), List.of("cobbleclub:lightning_saber", "cobbleclub:shadow_scythe", "cobbleclub:aura_pickaxe", "cobbleclub:ember_axe")));
    }

    private static void addGearIfMissing(List<GearSetDefinition> definitions, GearSetDefinition addition) {
        if (definitions.stream().noneMatch(existing -> existing != null && addition.id().equals(existing.id()))) {
            definitions.add(addition);
        }
    }

    private static String json(String text, String color) {
        return "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"color\":\"" + color + "\"}";
    }

    public static final class VoteSite {
        public String name;
        public String url;

        public VoteSite() {
        }

        public VoteSite(String name, String url) {
            this.name = name;
            this.url = url;
        }

        private void normalize() {
            if (this.name == null || this.name.isBlank()) {
                this.name = "Vote Site";
            }
            if (this.url != null) {
                this.url = this.url.trim();
            }
        }
    }

    public static final class PokemonCatalog {
        public List<PokemonFamily> families = new ArrayList<PokemonFamily>();
        public Map<String, Integer> sortKeys = new LinkedHashMap<String, Integer>();
        public List<String> idleFly = new ArrayList<String>();
        public List<String> formAspects = new ArrayList<String>();
        public List<String> formButtons = new ArrayList<String>();
        public Map<String, List<String>> scopedForms = new LinkedHashMap<String, List<String>>();

        private void normalize() {
            if (this.families == null) {
                this.families = new ArrayList<PokemonFamily>();
            }
            if (this.sortKeys == null) {
                this.sortKeys = new LinkedHashMap<String, Integer>();
            }
            if (this.idleFly == null) {
                this.idleFly = new ArrayList<String>();
            }
            if (this.formAspects == null) {
                this.formAspects = new ArrayList<String>();
            }
            if (this.formButtons == null) {
                this.formButtons = new ArrayList<String>();
            }
            if (this.scopedForms == null) {
                this.scopedForms = new LinkedHashMap<String, List<String>>();
            }
            for (PokemonFamily family : this.families) {
                if (family == null) continue;
                family.normalize();
            }
        }
    }

    public record TagCategory(String id, String displayName) {
    }

    public record TagDefinition(String id, String tagJson, List<String> description, String category, boolean ownedByDefault, long price) {
    }

    public record CosmeticDefinition(String id, String displayName, String displayNameJson, String slot, String material, int customModelData, boolean dyeable, boolean ownedByDefault, List<String> lockedLore, List<String> equipLore, List<String> unequipLore, long price) {
    }

    public record GlowDefinition(String id, String displayName, String displayNameJson, List<Integer> colors, boolean ownedByDefault, List<String> lockedLore, List<String> equipLore, List<String> unequipLore, long price) {
    }

    public record GearSetDefinition(String id, String displayNameJson, List<String> items) {
    }

    public static final class PokemonFamily {
        public String id;
        public String displayName;
        public String shortName;
        public String skinAspect;
        public List<PokemonEntry> entries = new ArrayList<PokemonEntry>();

        private void normalize() {
            if (this.entries == null) {
                this.entries = new ArrayList<PokemonEntry>();
            }
            this.entries.removeIf(entry -> entry == null);
            this.entries.forEach(PokemonEntry::normalize);
        }
    }

    public static final class PokemonEntry {
        public String species;
        public List<String> aspects = new ArrayList<String>();
        public String displayName;

        public static PokemonEntry of(String species, String displayName) {
            PokemonEntry entry = new PokemonEntry();
            entry.species = species;
            entry.displayName = displayName;
            return entry;
        }

        private void normalize() {
            if (this.aspects == null) {
                this.aspects = new ArrayList<String>();
            }
        }
    }

    public static final class CrateDefinition {
        public String id;
        public String title;
        public String gradient = "purple";
        public String keyId;
        public String keyDisplayName;
        public boolean canTest;
        public boolean shinyPreview;
        public boolean broadcastWins;
        public List<String> formAspects = new ArrayList<String>();
        public List<String> formButtons = new ArrayList<String>();
        public Map<String, List<String>> scopedForms = new LinkedHashMap<String, List<String>>();
        public List<CratePrize> prizes = new ArrayList<CratePrize>();

        private void normalize() {
            if (this.id == null) {
                this.id = "crate";
            }
            if (this.title == null) {
                this.title = ServerConfig.json("CobbleClub Crate", "light_purple");
            }
            if (this.gradient == null) {
                this.gradient = "purple";
            }
            if (this.keyId == null || this.keyId.isBlank()) {
                this.keyId = this.id;
            }
            if (this.keyDisplayName == null || this.keyDisplayName.isBlank()) {
                this.keyDisplayName = this.id + " key";
            }
            if (this.formAspects == null) {
                this.formAspects = new ArrayList<String>();
            }
            if (this.formButtons == null) {
                this.formButtons = new ArrayList<String>();
            }
            if (this.scopedForms == null) {
                this.scopedForms = new LinkedHashMap<String, List<String>>();
            }
            if (this.prizes == null) {
                this.prizes = new ArrayList<CratePrize>();
            }
            this.prizes.removeIf(prize -> prize == null);
            this.prizes.forEach(CratePrize::normalize);
        }

        private static CrateDefinition base(String id, String name, String color) {
            CrateDefinition crate = new CrateDefinition();
            crate.id = id;
            crate.title = ServerConfig.json(name, color);
            crate.keyId = id;
            crate.keyDisplayName = name + " Key";
            crate.shinyPreview = true;
            return crate;
        }

        static CrateDefinition vote() {
            CrateDefinition crate = CrateDefinition.base("vote", "CobbleClub Vote Crate", "aqua");
            crate.prizes.add(CratePrize.item("16 Rare Candy", "cobblemon:rare_candy", 16, 45.0));
            crate.prizes.add(CratePrize.money("2,500 Pok\u00e9Dollars", 2500L, 30.0));
            crate.prizes.add(CratePrize.claimBlocks("4,096 Claim Blocks", 4096, 20.0));
            crate.prizes.add(CratePrize.pokemon("Eevee", "eevee", List.of(), 5.0));
            return crate;
        }

        static CrateDefinition shiny() {
            CrateDefinition crate = CrateDefinition.base("shiny", "CobbleClub Shiny Crate", "light_purple");
            crate.broadcastWins = true;
            crate.prizes.add(CratePrize.pokemon("Shiny Pikachu", "pikachu", List.of("shiny"), 50.0));
            crate.prizes.add(CratePrize.pokemon("Shiny Eevee", "eevee", List.of("shiny"), 50.0));
            return crate;
        }

        static CrateDefinition legendary() {
            CrateDefinition crate = CrateDefinition.base("legendary", "CobbleClub Legendary Crate", "gold");
            crate.broadcastWins = true;
            crate.prizes.add(CratePrize.pokemon("Mew", "mew", List.of(), 50.0));
            crate.prizes.add(CratePrize.pokemon("Celebi", "celebi", List.of(), 50.0));
            return crate;
        }
    }

    public static final class CratePrize {
        public String displayName;
        public String shinyDisplayName;
        public String species;
        public List<String> aspects = new ArrayList<String>();
        public String material;
        public int customModelData;
        public int shinyCustomModelData;
        public boolean shinyEligible;
        public int amount = 1;
        public List<String> lore = new ArrayList<String>();
        public double chance;
        public String wornSlot;
        public List<String> evolutions = new ArrayList<String>();
        public String rewardType = "ITEM";
        public String rewardId;
        public long rewardAmount = 1L;
        public List<String> commands = new ArrayList<String>();
        public boolean broadcast;

        private void normalize() {
            if (this.displayName == null) {
                this.displayName = "Reward";
            }
            if (this.aspects == null) {
                this.aspects = new ArrayList<String>();
            }
            if (this.lore == null) {
                this.lore = new ArrayList<String>();
            }
            if (this.evolutions == null) {
                this.evolutions = new ArrayList<String>();
            }
            if (this.commands == null) {
                this.commands = new ArrayList<String>();
            }
            if (this.rewardType == null) {
                String string = this.rewardType = this.species == null ? "ITEM" : "POKEMON";
            }
            if (this.rewardId == null) {
                String string = this.rewardId = this.species == null ? this.material : this.species;
            }
            if (this.rewardAmount <= 0L) {
                this.rewardAmount = Math.max(1, this.amount);
            }
            if (this.amount <= 0) {
                this.amount = (int)Math.min(Integer.MAX_VALUE, this.rewardAmount);
            }
        }

        static CratePrize item(String name, String itemId, int amount, double chance) {
            CratePrize prize = new CratePrize();
            prize.displayName = name;
            prize.material = itemId;
            prize.amount = amount;
            prize.rewardType = "ITEM";
            prize.rewardId = itemId;
            prize.rewardAmount = amount;
            prize.chance = chance;
            return prize;
        }

        static CratePrize money(String name, long amount, double chance) {
            CratePrize prize = new CratePrize();
            prize.displayName = name;
            prize.material = "minecraft:gold_ingot";
            prize.rewardType = "MONEY";
            prize.rewardAmount = amount;
            prize.chance = chance;
            return prize;
        }

        static CratePrize claimBlocks(String name, int amount, double chance) {
            CratePrize prize = new CratePrize();
            prize.displayName = name;
            prize.material = "minecraft:golden_shovel";
            prize.rewardType = "CLAIM_BLOCKS";
            prize.rewardAmount = amount;
            prize.chance = chance;
            return prize;
        }

        static CratePrize pokemon(String name, String species, List<String> aspects, double chance) {
            CratePrize prize = new CratePrize();
            prize.displayName = name;
            prize.species = species;
            prize.aspects = new ArrayList<String>(aspects);
            prize.material = "cobblemon:poke_ball";
            prize.rewardType = "POKEMON";
            prize.rewardId = species;
            prize.chance = chance;
            return prize;
        }
    }

    public record CrateLocation(String dimension, int x, int y, int z, String crateId) {
    }
}

