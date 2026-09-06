package com.cobbleclub.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Human-editable CobbleClub settings. All catalogs are deliberately data driven so
 * a server owner can theme rewards without rebuilding the mod.
 */
public final class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cobbleclub-server.json");

    public int configVersion = 17;

    public boolean claimsEnabled = true;
    public int initialClaimBlocks = 700;
    public int claimBlocksPerReward = 0;
    public int claimBlockRewardIntervalSeconds = 1800;
    public int gemsPerPlaytimeReward = 2;
    public int claimBlockPurchaseAmount = 256;
    public long claimBlockPurchasePrice = 25;
    public int mapRadiusChunks = 24;
    public int maxClaimChunksPerSide = 64;
    public int maxClaimDistanceChunks = 24;
    public boolean operatorsBypassClaims = true;
    public boolean claimEntryMessages = true;

    public boolean economyEnabled = true;
    public String currencyName = "PokéDollars";
    public String currencySymbol = "₽";
    public long startingBalance = 400;
    public long startingGems = 5;
    public long playtimeMoneyReward = 150;
    public int playtimeMoneyIntervalSeconds = 3600;
    public long dailyCooldownSeconds = 43_200L;
    public long deathMoneyPenalty = 100;

    public boolean dailyRewardsEnabled = true;
    public long dailyMoney = 500;
    public int dailyClaimBlocks = 0;
    public int dailyGems = 5;
    public int dailyVoteKeys = 1;

    public boolean newbKitEnabled = true;
    public long newbKitCooldownSeconds = 43_200L;
    public String newbKitPokedexItem = "cobblemon:pokedex_red";
    public String newbKitPokeBallItem = "cobblemon:poke_ball";
    public String newbKitGreatBallItem = "cobblemon:great_ball";
    public String newbKitUltraBallItem = "cobblemon:ultra_ball";
    public String newbKitPcItem = "cobblemon:pc";
    public String newbKitHealingMachineItem = "cobblemon:healing_machine";
    public long rankKitCooldownSeconds = 64_800L;
    public String aceKitPermission = "cobbleclub.kit.ace";
    public String championKitPermission = "cobbleclub.kit.champion";
    public String masterKitPermission = "cobbleclub.kit.master";
    public String legendKitPermission = "cobbleclub.kit.legend";
    public String kitRareCandyItem = "cobblemon:rare_candy";
    public String kitQuickBallItem = "cobblemon:quick_ball";
    public String kitBeastBallItem = "cobblemon:beast_ball";
    public String kitMasterBallItem = "cobblemon:master_ball";
    public long kitCooldownReductionPrice = 500L;
    public long kitCooldownReductionStepSeconds = 3_600L;
    public long kitCooldownReductionMaxSeconds = 14_400L;
    public long legendKitCooldownReductionMaxSeconds = 21_600L;

    public boolean showLockedWardrobeItems = true;
    public boolean scoreboardTagsEnabled = true;
    public boolean broadcastRareCrateWins = true;
    public String pokemonRewardCommand = "pokegive {player} {species} {aspects}";

    public List<TagCategory> tagCategories = new ArrayList<>();
    public List<TagDefinition> tags = new ArrayList<>();
    public List<CosmeticDefinition> cosmetics = new ArrayList<>();
    public List<GlowDefinition> glows = new ArrayList<>();
    public List<GearSetDefinition> gearSets = new ArrayList<>();
    public PokemonCatalog pokemonCatalog = new PokemonCatalog();
    public List<CrateDefinition> crates = new ArrayList<>();
    public List<CrateLocation> crateLocations = new ArrayList<>();

    public static ServerConfig load() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                ServerConfig config = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), ServerConfig.class);
                if (config != null) {
                    config.normalize();
                    config.save();
                    return config;
                }
            }
        } catch (Exception ignored) {
        }

        ServerConfig config = defaults();
        config.save();
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
        int loadedVersion = configVersion;
        if (currencyName == null || currencyName.isBlank()) currencyName = "PokéDollars";
        if (currencySymbol == null) currencySymbol = "";
        if (pokemonRewardCommand == null) pokemonRewardCommand = "";
        if (newbKitPokedexItem == null || newbKitPokedexItem.isBlank()) newbKitPokedexItem = "cobblemon:pokedex_red";
        if (newbKitPokeBallItem == null || newbKitPokeBallItem.isBlank()) newbKitPokeBallItem = "cobblemon:poke_ball";
        if (newbKitGreatBallItem == null || newbKitGreatBallItem.isBlank()) newbKitGreatBallItem = "cobblemon:great_ball";
        if (newbKitUltraBallItem == null || newbKitUltraBallItem.isBlank()) newbKitUltraBallItem = "cobblemon:ultra_ball";
        if (newbKitPcItem == null || newbKitPcItem.isBlank()) newbKitPcItem = "cobblemon:pc";
        if (newbKitHealingMachineItem == null || newbKitHealingMachineItem.isBlank()) newbKitHealingMachineItem = "cobblemon:healing_machine";
        if (aceKitPermission == null || aceKitPermission.isBlank()) aceKitPermission = "cobbleclub.kit.ace";
        if (championKitPermission == null || championKitPermission.isBlank()) championKitPermission = "cobbleclub.kit.champion";
        if (masterKitPermission == null || masterKitPermission.isBlank()) masterKitPermission = "cobbleclub.kit.master";
        if (legendKitPermission == null || legendKitPermission.isBlank()) legendKitPermission = "cobbleclub.kit.legend";
        if (kitRareCandyItem == null || kitRareCandyItem.isBlank()) kitRareCandyItem = "cobblemon:rare_candy";
        if (kitQuickBallItem == null || kitQuickBallItem.isBlank()) kitQuickBallItem = "cobblemon:quick_ball";
        if (kitBeastBallItem == null || kitBeastBallItem.isBlank()) kitBeastBallItem = "cobblemon:beast_ball";
        if (kitMasterBallItem == null || kitMasterBallItem.isBlank()) kitMasterBallItem = "cobblemon:master_ball";
        if (tagCategories == null) tagCategories = new ArrayList<>();
        if (tags == null) tags = new ArrayList<>();
        if (cosmetics == null) cosmetics = new ArrayList<>();
        if (glows == null) glows = new ArrayList<>();
        if (gearSets == null) gearSets = new ArrayList<>();
        if (pokemonCatalog == null) pokemonCatalog = new PokemonCatalog();
        pokemonCatalog.normalize();
        if (crates == null) crates = new ArrayList<>();
        crates.removeIf(crate -> crate == null);
        crates.forEach(CrateDefinition::normalize);
        if (crateLocations == null) crateLocations = new ArrayList<>();
        if (loadedVersion < 3) addWardrobeExpansion(cosmetics, glows);
        if (loadedVersion < 4) {
            initialClaimBlocks = 700;
            if (claimBlocksPerReward == 256) claimBlocksPerReward = 0;
            if (claimBlockPurchaseAmount == 4096) claimBlockPurchaseAmount = 256;
            if (claimBlockPurchasePrice == 2500) claimBlockPurchasePrice = 25;
            if (dailyClaimBlocks == 1024) dailyClaimBlocks = 0;
            gemsPerPlaytimeReward = Math.max(2, gemsPerPlaytimeReward);
            dailyGems = Math.max(5, dailyGems);
        }
        if (loadedVersion < 5) addGearExpansion(gearSets);
        if (loadedVersion < 6) {
            newbKitEnabled = true;
            newbKitCooldownSeconds = 43_200L;
        }
        if (loadedVersion < 8) rankKitCooldownSeconds = 64_800L;
        if (loadedVersion < 9) {
            kitCooldownReductionPrice = 500L;
            kitCooldownReductionStepSeconds = 3_600L;
            kitCooldownReductionMaxSeconds = 14_400L;
            legendKitCooldownReductionMaxSeconds = 21_600L;
        }
        if (loadedVersion < 10) {
            startingBalance = 400;
            startingGems = 5;
            gemsPerPlaytimeReward = 2;
            claimBlockRewardIntervalSeconds = 1800;
            playtimeMoneyReward = 150;
            playtimeMoneyIntervalSeconds = 3600;
            dailyCooldownSeconds = 43_200L;
            dailyVoteKeys = 0;
            deathMoneyPenalty = 100;
            replaceCosmetic(cosmetics, "pikachu_buddy_balloon", "minecraft:paper", 22201);
            replaceCosmetic(cosmetics, "jigglypuff_moon_balloon", "minecraft:paper", 22202);
            replaceCosmetic(cosmetics, "drifloon_festival_balloon", "minecraft:paper", 22203);
            replaceCosmetic(cosmetics, "pokeball_club_balloon", "minecraft:paper", 22204);
            replaceCosmetic(cosmetics, "club_balloon", "minecraft:paper", 22205);
        }
        if (loadedVersion < 11) {
            replaceCosmetic(cosmetics, "pikachu_buddy_balloon", "minecraft:paper", 22201);
            replaceCosmetic(cosmetics, "jigglypuff_moon_balloon", "minecraft:paper", 22202);
            replaceCosmetic(cosmetics, "drifloon_festival_balloon", "minecraft:paper", 22203);
            replaceCosmetic(cosmetics, "pokeball_club_balloon", "minecraft:paper", 22204);
            replaceCosmetic(cosmetics, "club_balloon", "minecraft:paper", 22205);
        }
        if (loadedVersion < 12) {
            addFloaties(cosmetics);
        }
        if (loadedVersion < 13) {
            // Club Crown now uses a dedicated 3D client model instead of the flat vanilla helmet icon.
            replaceCosmetic(cosmetics, "club_crown", "minecraft:golden_helmet", 22005);
        }
        if (loadedVersion < 14) {
            addHatColorways(cosmetics);
        }
        if (loadedVersion < 15) {
            renameCosmetic(cosmetics, "gengar_grin_mask", "gengar-hat", "Gengar Hat");
        }
        if (loadedVersion < 16) {
            addPokemonFloaties(cosmetics);
        }
        if (loadedVersion < 17) {
            addBalloonVariants(cosmetics);
            ensureRankTags(tagCategories, tags);
        }
        newbKitCooldownSeconds = Math.max(0L, newbKitCooldownSeconds);
        rankKitCooldownSeconds = Math.max(0L, rankKitCooldownSeconds);
        kitCooldownReductionPrice = Math.max(0L, kitCooldownReductionPrice);
        kitCooldownReductionStepSeconds = Math.max(1L, kitCooldownReductionStepSeconds);
        kitCooldownReductionMaxSeconds = Math.max(0L, kitCooldownReductionMaxSeconds);
        legendKitCooldownReductionMaxSeconds = Math.max(kitCooldownReductionMaxSeconds, legendKitCooldownReductionMaxSeconds);
        configVersion = Math.max(17, configVersion);
    }

    private static ServerConfig defaults() {
        ServerConfig config = new ServerConfig();
        config.tagCategories.add(new TagCategory("general", "General"));
        config.tagCategories.add(new TagCategory("achievements", "Achievements"));
        config.tags.add(new TagDefinition(
                "trainer", "{\"text\":\"[Trainer]\",\"color\":\"light_purple\",\"bold\":true}",
                List.of(json("Default CobbleClub player tag", "gray")), "general", true, 0
        ));
        config.tags.add(new TagDefinition(
                "shiny_hunter", "{\"text\":\"[Shiny Hunter]\",\"color\":\"aqua\",\"bold\":true}",
                List.of(json("Premium tag · purchase for 50,000 PokéDollars", "gray")), "achievements", false, 50000
        ));

        config.cosmetics.add(new CosmeticDefinition(
                "club_crown", "Club Crown", json("Club Crown", "gold"), "HELMET",
                "minecraft:golden_helmet", 22005, false, true,
                List.of(json("Unlock this cosmetic first", "red")),
                List.of(json("Click to wear the CobbleClub crown", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        config.cosmetics.add(new CosmeticDefinition(
                "club_wings", "Club Wings", json("Club Wings", "light_purple"), "BACKPACK",
                "minecraft:elytra", 0, true, false,
                List.of(json("Purchase for 12,500 PokéDollars", "red")),
                List.of(json("Click to equip", "green")), List.of(json("Click to remove", "yellow")), 12500
        ));
        config.cosmetics.add(new CosmeticDefinition(
                "club_balloon", "Club Balloon", json("Club Balloon", "aqua"), "BALLOON",
                "minecraft:paper", 22205, true, false,
                List.of(json("Purchase for 10,000 PokéDollars", "red")),
                List.of(json("Click to equip", "green")), List.of(json("Click to remove", "yellow")), 10000
        ));
        config.glows.add(new GlowDefinition(
                "club_purple", "Club Purple", json("Club Purple", "light_purple"),
                List.of(0x9B5DE5, 0xC77DFF, 0x7B2CBF), true,
                List.of(json("Unlock this glow first", "red")),
                List.of(json("Click to enable", "green")), List.of(json("Click to disable", "yellow")), 0
        ));
        addWardrobeExpansion(config.cosmetics, config.glows);
        addFloaties(config.cosmetics);
        addHatColorways(config.cosmetics);
        renameCosmetic(config.cosmetics, "gengar_grin_mask", "gengar-hat", "Gengar Hat");
        addPokemonFloaties(config.cosmetics);
        addBalloonVariants(config.cosmetics);
        ensureRankTags(config.tagCategories, config.tags);

        config.gearSets.add(new GearSetDefinition(
                "adventure", json("CobbleClub Adventure Gear", "light_purple"),
                List.of("minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots")
        ));
        addGearExpansion(config.gearSets);

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

    /** Added once when a version-2 config is upgraded; IDs keep the migration repeat-safe. */
    private static void addWardrobeExpansion(List<CosmeticDefinition> cosmetics, List<GlowDefinition> glows) {
        addIfMissing(cosmetics, new CosmeticDefinition(
                "pika_spark_cap", "Pika Spark Cap", json("Pika Spark Cap", "yellow"), "HELMET",
                "minecraft:leather_helmet", 22001, false, true,
                List.of(json("A bright electric trainer cap", "gray")),
                List.of(json("Equip the electric-inspired cap", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "eevee_explorer_hood", "Eevee Explorer Hood", json("Eevee Explorer Hood", "gold"), "HELMET",
                "minecraft:leather_helmet", 22002, false, true,
                List.of(json("A fixed-texture evolution-adventure hood", "gray")),
                List.of(json("Equip the evolution-inspired hood", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "gengar-hat", "Gengar Hat", json("Gengar Hat", "dark_purple"), "HELMET",
                "minecraft:leather_helmet", 22003, false, false,
                List.of(json("Click to buy or use /wardrobe buy cosmetic gengar-hat", "red")),
                List.of(json("Equip the ghost-inspired grin", "green")),
                List.of(json("Click to remove", "yellow")), 6500
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "lucario_aura_crest", "Lucario Aura Crest", json("Lucario Aura Crest", "blue"), "HELMET",
                "minecraft:leather_helmet", 22004, false, false,
                List.of(json("Buy with /wardrobe buy cosmetic lucario_aura_crest", "red")),
                List.of(json("Equip the aura-inspired crest", "green")),
                List.of(json("Click to remove", "yellow")), 8000
        ));

        addIfMissing(cosmetics, new CosmeticDefinition(
                "charizard_flame_pack", "Charizard Flame Wings", json("Charizard Flame Wings", "gold"), "BACKPACK",
                "minecraft:elytra", 22101, false, true,
                List.of(json("A fiery dragon-inspired travel pack", "gray")),
                List.of(json("Equip the flame pack", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "bulbasaur_sprout_pack", "Bulbasaur Sprout Wings", json("Bulbasaur Sprout Wings", "green"), "BACKPACK",
                "minecraft:elytra", 22104, false, true,
                List.of(json("A leafy partner-inspired pack", "gray")),
                List.of(json("Equip the sprout pack", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "sylveon_ribbon_pack", "Sylveon Ribbon Wings", json("Sylveon Ribbon Wings", "light_purple"), "BACKPACK",
                "minecraft:elytra", 22102, false, false,
                List.of(json("Buy with /wardrobe buy cosmetic sylveon_ribbon_pack", "red")),
                List.of(json("Equip the ribbon-inspired pack", "green")),
                List.of(json("Click to remove", "yellow")), 9000
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "lucario_aura_pack", "Lucario Aura Wings", json("Lucario Aura Wings", "aqua"), "BACKPACK",
                "minecraft:elytra", 22103, false, false,
                List.of(json("Buy with /wardrobe buy cosmetic lucario_aura_pack", "red")),
                List.of(json("Equip the aura pack", "green")),
                List.of(json("Click to remove", "yellow")), 9000
        ));

        addIfMissing(cosmetics, new CosmeticDefinition(
                "pikachu_buddy_balloon", "Pikachu Buddy Balloon", json("Pikachu Buddy Balloon", "yellow"), "BALLOON",
                "minecraft:paper", 22201, false, true,
                List.of(json("A cheerful electric-inspired balloon", "gray")),
                List.of(json("Float the buddy balloon", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "jigglypuff_moon_balloon", "Jigglypuff Moon Balloon", json("Jigglypuff Moon Balloon", "light_purple"), "BALLOON",
                "minecraft:paper", 22202, false, true,
                List.of(json("A round song-partner-inspired balloon", "gray")),
                List.of(json("Float the moon balloon", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "drifloon_festival_balloon", "Drifloon Festival Balloon", json("Drifloon Festival Balloon", "dark_purple"), "BALLOON",
                "minecraft:paper", 22203, false, false,
                List.of(json("Buy with /wardrobe buy cosmetic drifloon_festival_balloon", "red")),
                List.of(json("Float the ghost-inspired balloon", "green")),
                List.of(json("Click to remove", "yellow")), 7000
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "pokeball_club_balloon", "Poké Ball Club Balloon", json("Poké Ball Club Balloon", "red"), "BALLOON",
                "minecraft:paper", 22204, false, false,
                List.of(json("Buy with /wardrobe buy cosmetic pokeball_club_balloon", "red")),
                List.of(json("Float the trainer balloon", "green")),
                List.of(json("Click to remove", "yellow")), 7500
        ));

        addIfMissing(glows, new GlowDefinition(
                "spark_yellow", "Spark Yellow", json("Spark Yellow", "yellow"),
                List.of(0xFDE047, 0xF59E0B, 0xFFF7AE), true,
                List.of(json("Electric-inspired glow", "gray")),
                List.of(json("Enable Spark Yellow", "green")), List.of(json("Disable glow", "yellow")), 0
        ));
        addIfMissing(glows, new GlowDefinition(
                "aura_blue", "Aura Blue", json("Aura Blue", "aqua"),
                List.of(0x38BDF8, 0x2563EB, 0x93C5FD), true,
                List.of(json("Aura-inspired glow", "gray")),
                List.of(json("Enable Aura Blue", "green")), List.of(json("Disable glow", "yellow")), 0
        ));
        addIfMissing(glows, new GlowDefinition(
                "fairy_ribbon", "Fairy Ribbon", json("Fairy Ribbon", "light_purple"),
                List.of(0xF9A8D4, 0xC4B5FD, 0xFBCFE8), false,
                List.of(json("Buy with /wardrobe buy glow fairy_ribbon", "red")),
                List.of(json("Enable Fairy Ribbon", "green")), List.of(json("Disable glow", "yellow")), 6000
        ));
    }

    private static void addHatColorways(List<CosmeticDefinition> cosmetics) {
        addIfMissing(cosmetics, new CosmeticDefinition(
                "gengar_grin_crimson", "Gengar Hat - Crimson", json("Gengar Hat - Crimson", "red"), "HELMET",
                "minecraft:leather_helmet", 22006, false, false,
                List.of(json("Crimson colorway of the Gengar hat", "gray")),
                List.of(json("Equip the Crimson Gengar hat", "green")),
                List.of(json("Click to remove", "yellow")), 6500
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "gengar_grin_azure", "Gengar Hat - Azure", json("Gengar Hat - Azure", "blue"), "HELMET",
                "minecraft:leather_helmet", 22007, false, false,
                List.of(json("Azure colorway of the Gengar hat", "gray")),
                List.of(json("Equip the Azure Gengar hat", "green")),
                List.of(json("Click to remove", "yellow")), 6500
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "gengar_grin_toxic", "Gengar Hat - Toxic", json("Gengar Hat - Toxic", "green"), "HELMET",
                "minecraft:leather_helmet", 22008, false, false,
                List.of(json("Toxic green colorway of the Gengar hat", "gray")),
                List.of(json("Equip the Toxic Gengar hat", "green")),
                List.of(json("Click to remove", "yellow")), 6500
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "froggy_hat_blue", "Froggy Hat - Blue", json("Froggy Hat - Blue", "blue"), "HELMET",
                "minecraft:leather_helmet", 22009, false, true,
                List.of(json("Blue colorway of the Froggy hat", "gray")),
                List.of(json("Equip the Blue Froggy hat", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "froggy_hat_pink", "Froggy Hat - Pink", json("Froggy Hat - Pink", "light_purple"), "HELMET",
                "minecraft:leather_helmet", 22010, false, true,
                List.of(json("Pink colorway of the Froggy hat", "gray")),
                List.of(json("Equip the Pink Froggy hat", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "froggy_hat_purple", "Froggy Hat - Purple", json("Froggy Hat - Purple", "dark_purple"), "HELMET",
                "minecraft:leather_helmet", 22011, false, true,
                List.of(json("Purple colorway of the Froggy hat", "gray")),
                List.of(json("Equip the Purple Froggy hat", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
    }

    private static void addFloaties(List<CosmeticDefinition> cosmetics) {
        // Protocol v2 has HELMET/BACKPACK/BALLOON wearable slots. Floaties intentionally
        // use BALLOON internally; the 2.6 client separates CMD 22301-22303 into its
        // dedicated Floaties category and renders them around the player waist.
        addIfMissing(cosmetics, new CosmeticDefinition(
                "duck_floaty", "Duck Floaty", json("Duck Floaty", "yellow"), "BALLOON",
                "minecraft:paper", 22301, false, true,
                List.of(json("A cheerful pool floaty", "gray")),
                List.of(json("Equip the Duck Floaty", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "dino_floaty", "Dino Floaty", json("Dino Floaty", "green"), "BALLOON",
                "minecraft:paper", 22302, false, true,
                List.of(json("A playful dinosaur floaty", "gray")),
                List.of(json("Equip the Dino Floaty", "green")),
                List.of(json("Click to remove", "yellow")), 0
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "flamingo_floaty", "Flamingo Floaty", json("Flamingo Floaty", "light_purple"), "BALLOON",
                "minecraft:paper", 22303, false, false,
                List.of(json("Buy with /wardrobe buy cosmetic flamingo_floaty", "red")),
                List.of(json("Equip the Flamingo Floaty", "green")),
                List.of(json("Click to remove", "yellow")), 5000
        ));
    }

    private static void addPokemonFloaties(List<CosmeticDefinition> cosmetics) {
        addIfMissing(cosmetics, new CosmeticDefinition(
                "pikachu_floaty", "Pikachu Floaty", json("Pikachu Floaty", "yellow"), "BALLOON",
                "minecraft:paper", 22304, false, false,
                List.of(json("Click to buy for 7,500 PokéDollars", "red")),
                List.of(json("Equip the Pikachu Floaty", "green")),
                List.of(json("Click to remove", "yellow")), 7500
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "gengar_floaty", "Gengar Floaty", json("Gengar Floaty", "dark_purple"), "BALLOON",
                "minecraft:paper", 22305, false, false,
                List.of(json("Click to buy for 8,000 PokéDollars", "red")),
                List.of(json("Equip the Gengar Floaty", "green")),
                List.of(json("Click to remove", "yellow")), 8000
        ));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "eevee_floaty", "Eevee Floaty", json("Eevee Floaty", "gold"), "BALLOON",
                "minecraft:paper", 22306, false, false,
                List.of(json("Click to buy for 7,500 PokéDollars", "red")),
                List.of(json("Equip the Eevee Floaty", "green")),
                List.of(json("Click to remove", "yellow")), 7500
        ));
    }


    private static void addBalloonVariants(List<CosmeticDefinition> cosmetics) {
        addIfMissing(cosmetics, new CosmeticDefinition(
                "master_ball_balloon", "Master Ball Balloon", json("Master Ball Balloon", "dark_purple"), "BALLOON",
                "minecraft:paper", 22206, false, false,
                List.of(json("Click to buy for 12,500 PokéDollars", "red")),
                List.of(json("Equip the Master Ball Balloon", "green")), List.of(json("Click to remove", "yellow")), 12500));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "premier_ball_balloon", "Premier Ball Balloon", json("Premier Ball Balloon", "white"), "BALLOON",
                "minecraft:paper", 22207, false, false,
                List.of(json("Click to buy for 7,500 PokéDollars", "red")),
                List.of(json("Equip the Premier Ball Balloon", "green")), List.of(json("Click to remove", "yellow")), 7500));
        addIfMissing(cosmetics, new CosmeticDefinition(
                "ultra_ball_balloon", "Ultra Ball Balloon", json("Ultra Ball Balloon", "yellow"), "BALLOON",
                "minecraft:paper", 22208, false, false,
                List.of(json("Click to buy for 10,000 PokéDollars", "red")),
                List.of(json("Equip the Ultra Ball Balloon", "green")), List.of(json("Click to remove", "yellow")), 10000));
    }

    private static void ensureRankTags(List<TagCategory> categories, List<TagDefinition> tags) {
        if (categories.stream().noneMatch(c -> c != null && "ranks".equals(c.id()))) categories.add(new TagCategory("ranks", "Ranks"));
        if (categories.stream().noneMatch(c -> c != null && "staff".equals(c.id()))) categories.add(new TagCategory("staff", "Staff"));
        upsertTag(tags, new TagDefinition("trainer", "{\"text\":\"[Trainer]\",\"color\":\"light_purple\",\"bold\":true}",
                List.of(json("Default tag · available to every rank", "gray")), "general", true, 0));
        upsertTag(tags, new TagDefinition("newb", "{\"text\":\"[Newb]\",\"color\":\"green\",\"bold\":true}",
                List.of(json("Free alternate tag for the default rank", "gray")), "general", true, 0));
        upsertTag(tags, new TagDefinition("shiny_hunter", "{\"text\":\"[Shiny Hunter]\",\"color\":\"aqua\",\"bold\":true}",
                List.of(json("Premium tag · purchase for 50,000 PokéDollars", "gray")), "achievements", false, 50000));
        upsertTag(tags, new TagDefinition("ace", "{\"text\":\"[ACE]\",\"color\":\"gold\",\"bold\":true}",
                List.of(json("Unlocked automatically by the Ace LuckPerms rank", "gray")), "ranks", false, 0));
        upsertTag(tags, new TagDefinition("champion", "{\"text\":\"[CHAMPION]\",\"color\":\"aqua\",\"bold\":true}",
                List.of(json("Unlocked automatically by the Champion LuckPerms rank", "gray")), "ranks", false, 0));
        upsertTag(tags, new TagDefinition("master", "{\"text\":\"[MASTER]\",\"color\":\"dark_purple\",\"bold\":true}",
                List.of(json("Unlocked automatically by the Master LuckPerms rank", "gray")), "ranks", false, 0));
        upsertTag(tags, new TagDefinition("legend", "{\"text\":\"[LEGEND]\",\"color\":\"light_purple\",\"bold\":true}",
                List.of(json("Unlocked automatically by the Legend LuckPerms rank", "gray")), "ranks", false, 0));
        upsertTag(tags, new TagDefinition("mod", "{\"text\":\"[MOD]\",\"color\":\"green\",\"bold\":true}",
                List.of(json("CobbleClub moderation team", "gray")), "staff", false, 0));
        upsertTag(tags, new TagDefinition("admin", "{\"text\":\"[ADMIN]\",\"color\":\"red\",\"bold\":true}",
                List.of(json("CobbleClub administration team", "gray")), "staff", false, 0));
    }

    private static void upsertTag(List<TagDefinition> tags, TagDefinition value) {
        for (int i = 0; i < tags.size(); i++) {
            TagDefinition old = tags.get(i);
            if (old != null && value.id().equals(old.id())) { tags.set(i, value); return; }
        }
        tags.add(value);
    }

    private static void renameCosmetic(List<CosmeticDefinition> definitions, String oldId, String newId, String newName) {
        for (int index = 0; index < definitions.size(); index++) {
            CosmeticDefinition old = definitions.get(index);
            if (old != null && oldId.equals(old.id())) {
                definitions.set(index, new CosmeticDefinition(newId, newName, json(newName, "dark_purple"), old.slot(),
                        old.material(), old.customModelData(), old.dyeable(), old.ownedByDefault(), old.lockedLore(), old.equipLore(), old.unequipLore(), old.price()));
                return;
            }
        }
    }

    private static void addIfMissing(List<CosmeticDefinition> definitions, CosmeticDefinition addition) {
        if (definitions.stream().noneMatch(existing -> existing != null && addition.id().equals(existing.id()))) definitions.add(addition);
    }

    private static void replaceCosmetic(List<CosmeticDefinition> definitions, String id, String material, int customModelData) {
        for (int index = 0; index < definitions.size(); index++) {
            CosmeticDefinition old = definitions.get(index);
            if (old != null && id.equals(old.id())) {
                definitions.set(index, new CosmeticDefinition(old.id(), old.displayName(), old.displayNameJson(), old.slot(),
                        material, customModelData, old.dyeable(), old.ownedByDefault(), old.lockedLore(), old.equipLore(), old.unequipLore(), old.price()));
                return;
            }
        }
    }

    private static void addIfMissing(List<GlowDefinition> definitions, GlowDefinition addition) {
        if (definitions.stream().noneMatch(existing -> existing != null && addition.id().equals(existing.id()))) definitions.add(addition);
    }

    private static void addGearExpansion(List<GearSetDefinition> gearSets) {
        addGearIfMissing(gearSets, new GearSetDefinition("spark_vanguard", json("Spark Vanguard Armor", "yellow"),
                List.of("cobbleclub:spark_helmet", "cobbleclub:spark_chestplate", "cobbleclub:spark_leggings", "cobbleclub:spark_boots")));
        addGearIfMissing(gearSets, new GearSetDefinition("spectral_phantom", json("Spectral Phantom Armor", "dark_purple"),
                List.of("cobbleclub:spectral_helmet", "cobbleclub:spectral_chestplate", "cobbleclub:spectral_leggings", "cobbleclub:spectral_boots")));
        addGearIfMissing(gearSets, new GearSetDefinition("aura_guardian", json("Aura Guardian Armor", "aqua"),
                List.of("cobbleclub:aura_helmet", "cobbleclub:aura_chestplate", "cobbleclub:aura_leggings", "cobbleclub:aura_boots")));
        addGearIfMissing(gearSets, new GearSetDefinition("fairy_bloom", json("Fairy Bloom Armor", "light_purple"),
                List.of("cobbleclub:fairy_helmet", "cobbleclub:fairy_chestplate", "cobbleclub:fairy_leggings", "cobbleclub:fairy_boots")));
        addGearIfMissing(gearSets, new GearSetDefinition("elemental_tools", json("CobbleClub Elemental Arsenal", "gold"),
                List.of("cobbleclub:lightning_saber", "cobbleclub:shadow_scythe", "cobbleclub:aura_pickaxe", "cobbleclub:ember_axe")));
    }

    private static void addGearIfMissing(List<GearSetDefinition> definitions, GearSetDefinition addition) {
        if (definitions.stream().noneMatch(existing -> existing != null && addition.id().equals(existing.id()))) definitions.add(addition);
    }

    private static String json(String text, String color) {
        return "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"color\":\"" + color + "\"}";
    }

    public record TagCategory(String id, String displayName) {}

    public record TagDefinition(
            String id, String tagJson, List<String> description, String category,
            boolean ownedByDefault, long price
    ) {}

    public record CosmeticDefinition(
            String id, String displayName, String displayNameJson, String slot, String material,
            int customModelData, boolean dyeable, boolean ownedByDefault,
            List<String> lockedLore, List<String> equipLore, List<String> unequipLore, long price
    ) {}

    public record GlowDefinition(
            String id, String displayName, String displayNameJson, List<Integer> colors,
            boolean ownedByDefault, List<String> lockedLore, List<String> equipLore,
            List<String> unequipLore, long price
    ) {}

    public record GearSetDefinition(String id, String displayNameJson, List<String> items) {}

    public record CrateLocation(String dimension, int x, int y, int z, String crateId) {}

    public static final class PokemonCatalog {
        public List<PokemonFamily> families = new ArrayList<>();
        public Map<String, Integer> sortKeys = new LinkedHashMap<>();
        public List<String> idleFly = new ArrayList<>();
        public List<String> formAspects = new ArrayList<>();
        public List<String> formButtons = new ArrayList<>();
        public Map<String, List<String>> scopedForms = new LinkedHashMap<>();

        private void normalize() {
            if (families == null) families = new ArrayList<>();
            if (sortKeys == null) sortKeys = new LinkedHashMap<>();
            if (idleFly == null) idleFly = new ArrayList<>();
            if (formAspects == null) formAspects = new ArrayList<>();
            if (formButtons == null) formButtons = new ArrayList<>();
            if (scopedForms == null) scopedForms = new LinkedHashMap<>();
            for (PokemonFamily family : families) if (family != null) family.normalize();
        }
    }

    public static final class PokemonFamily {
        public String id;
        public String displayName;
        public String shortName;
        public String skinAspect;
        public List<PokemonEntry> entries = new ArrayList<>();

        private void normalize() {
            if (entries == null) entries = new ArrayList<>();
            entries.removeIf(entry -> entry == null);
            entries.forEach(PokemonEntry::normalize);
        }
    }

    public static final class PokemonEntry {
        public String species;
        public List<String> aspects = new ArrayList<>();
        public String displayName;

        public static PokemonEntry of(String species, String displayName) {
            PokemonEntry entry = new PokemonEntry();
            entry.species = species;
            entry.displayName = displayName;
            return entry;
        }

        private void normalize() {
            if (aspects == null) aspects = new ArrayList<>();
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
        public List<String> formAspects = new ArrayList<>();
        public List<String> formButtons = new ArrayList<>();
        public Map<String, List<String>> scopedForms = new LinkedHashMap<>();
        public List<CratePrize> prizes = new ArrayList<>();

        private void normalize() {
            if (id == null) id = "crate";
            if (title == null) title = json("CobbleClub Crate", "light_purple");
            if (gradient == null) gradient = "purple";
            if (keyId == null || keyId.isBlank()) keyId = id;
            if (keyDisplayName == null || keyDisplayName.isBlank()) keyDisplayName = id + " key";
            if (formAspects == null) formAspects = new ArrayList<>();
            if (formButtons == null) formButtons = new ArrayList<>();
            if (scopedForms == null) scopedForms = new LinkedHashMap<>();
            if (prizes == null) prizes = new ArrayList<>();
            prizes.removeIf(prize -> prize == null);
            prizes.forEach(CratePrize::normalize);
        }

        private static CrateDefinition base(String id, String name, String color) {
            CrateDefinition crate = new CrateDefinition();
            crate.id = id;
            crate.title = json(name, color);
            crate.keyId = id;
            crate.keyDisplayName = name + " Key";
            crate.shinyPreview = true;
            return crate;
        }

        static CrateDefinition vote() {
            CrateDefinition crate = base("vote", "CobbleClub Vote Crate", "aqua");
            crate.prizes.add(CratePrize.item("16 Rare Candy", "cobblemon:rare_candy", 16, 45));
            crate.prizes.add(CratePrize.money("2,500 PokéDollars", 2500, 30));
            crate.prizes.add(CratePrize.claimBlocks("4,096 Claim Blocks", 4096, 20));
            crate.prizes.add(CratePrize.pokemon("Eevee", "eevee", List.of(), 5));
            return crate;
        }

        static CrateDefinition shiny() {
            CrateDefinition crate = base("shiny", "CobbleClub Shiny Crate", "light_purple");
            crate.broadcastWins = true;
            crate.prizes.add(CratePrize.pokemon("Shiny Pikachu", "pikachu", List.of("shiny"), 50));
            crate.prizes.add(CratePrize.pokemon("Shiny Eevee", "eevee", List.of("shiny"), 50));
            return crate;
        }

        static CrateDefinition legendary() {
            CrateDefinition crate = base("legendary", "CobbleClub Legendary Crate", "gold");
            crate.broadcastWins = true;
            crate.prizes.add(CratePrize.pokemon("Mew", "mew", List.of(), 50));
            crate.prizes.add(CratePrize.pokemon("Celebi", "celebi", List.of(), 50));
            return crate;
        }
    }

    public static final class CratePrize {
        public String displayName;
        public String shinyDisplayName;
        public String species;
        public List<String> aspects = new ArrayList<>();
        public String material;
        public int customModelData;
        public int shinyCustomModelData;
        public boolean shinyEligible;
        public int amount = 1;
        public List<String> lore = new ArrayList<>();
        public double chance;
        public String wornSlot;
        public List<String> evolutions = new ArrayList<>();
        public String rewardType = "ITEM";
        public String rewardId;
        public long rewardAmount = 1;
        public List<String> commands = new ArrayList<>();
        public boolean broadcast;

        private void normalize() {
            if (displayName == null) displayName = "Reward";
            if (aspects == null) aspects = new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            if (evolutions == null) evolutions = new ArrayList<>();
            if (commands == null) commands = new ArrayList<>();
            if (rewardType == null) rewardType = species == null ? "ITEM" : "POKEMON";
            if (rewardId == null) rewardId = species == null ? material : species;
            if (rewardAmount <= 0) rewardAmount = Math.max(1, amount);
            if (amount <= 0) amount = (int) Math.min(Integer.MAX_VALUE, rewardAmount);
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
            prize.aspects = new ArrayList<>(aspects);
            prize.material = "cobblemon:poke_ball";
            prize.rewardType = "POKEMON";
            prize.rewardId = species;
            prize.chance = chance;
            return prize;
        }
    }
}
