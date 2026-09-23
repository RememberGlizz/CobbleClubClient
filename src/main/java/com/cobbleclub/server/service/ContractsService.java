package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.ContractsPayloads;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class ContractsService {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Set<String> READY_RCT_REGIONS = Set.of("Kanto", "Johto", "Hoenn", "Sinnoh");

    private static final List<MilestoneDef> MILESTONES = List.of(
            new MilestoneDef("milestone:catch_25", "Collector I", "Catch 25 Pokémon.", Metric.CATCHES, 25L, 2000L, 2L, null, 0),
            new MilestoneDef("milestone:catch_100", "Collector II", "Catch 100 Pokémon.", Metric.CATCHES, 100L, 7500L, 5L, null, 0),
            new MilestoneDef("milestone:catch_250", "Collector III", "Catch 250 Pokémon.", Metric.CATCHES, 250L, 20000L, 10L, "shiny", 1),
            new MilestoneDef("milestone:catch_500", "Collector IV", "Catch 500 Pokémon.", Metric.CATCHES, 500L, 50000L, 20L, "legendary", 1),
            new MilestoneDef("milestone:shiny_1", "Shiny Hunter I", "Catch your first shiny Pokémon.", Metric.SHINIES, 1L, 5000L, 3L, null, 0),
            new MilestoneDef("milestone:shiny_5", "Shiny Hunter II", "Catch 5 shiny Pokémon.", Metric.SHINIES, 5L, 20000L, 8L, "shiny", 1),
            new MilestoneDef("milestone:shiny_15", "Shiny Hunter III", "Catch 15 shiny Pokémon.", Metric.SHINIES, 15L, 75000L, 20L, "legendary", 1),
            new MilestoneDef("milestone:defeat_25", "Trainer I", "Defeat 25 Pokémon.", Metric.DEFEATS, 25L, 2500L, 2L, null, 0),
            new MilestoneDef("milestone:defeat_100", "Trainer II", "Defeat 100 Pokémon.", Metric.DEFEATS, 100L, 10000L, 6L, null, 0),
            new MilestoneDef("milestone:defeat_250", "Trainer III", "Defeat 250 Pokémon.", Metric.DEFEATS, 250L, 30000L, 12L, "shiny", 1),
            new MilestoneDef("milestone:defeat_500", "Trainer IV", "Defeat 500 Pokémon.", Metric.DEFEATS, 500L, 80000L, 25L, "legendary", 1)
    );

    private static final List<JourneyDef> JOURNEY = buildJourney();

    private ContractsService() {
    }

    public static void open(ServerPlayerEntity player) {
        send(player, "", false, true);
    }

    public static void handleAction(ServerPlayerEntity player, String rawJson) {
        if (player == null) {
            return;
        }
        try {
            JsonElement parsed = JsonParser.parseString(rawJson == null ? "{}" : rawJson);
            JsonObject json = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            String action = json.has("action") ? json.get("action").getAsString() : "";

            if ("refresh".equalsIgnoreCase(action)) {
                send(player, "", false, false);
                return;
            }

            if (!"claim".equalsIgnoreCase(action)) {
                send(player, "Unknown contracts action.", true, false);
                return;
            }

            String id = json.has("id") ? json.get("id").getAsString() : "";
            if (id.isBlank()) {
                send(player, "Select a completed contract first.", true, false);
                return;
            }

            String message = claim(player, id);
            boolean error = message.startsWith("!");
            send(player, error ? message.substring(1) : message, error, false);
        } catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not process contract action for {}", player.getGameProfile().getName(), error);
            send(player, "That contract action could not be completed.", true, false);
        }
    }

    private static String claim(ServerPlayerEntity player, String id) {
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.normalize();

        if (data.claimedContractRewards.contains(id)) {
            return "!That reward has already been claimed.";
        }

        for (MilestoneDef def : MILESTONES) {
            if (!def.id.equals(id)) continue;
            long current = metric(data, def.metric);
            if (current < def.target) {
                return "!That milestone is not complete yet.";
            }

            data.claimedContractRewards.add(id);
            ++data.revision;
            PlayerDataStore.save();
            grantStandard(player, def.money, def.gems, 0, def.keyId, def.keyCount);
            return "Reward claimed: " + def.title + ".";
        }

        for (JourneyDef def : JOURNEY) {
            if (!def.id.equals(id)) continue;
            JourneyStatus status = journeyStatus(data, def);
            if (!status.claimable) {
                return status.regionReady
                        ? "!Finish every objective and the previous Journey contract first."
                        : "!That regional RCT pathway is not available on this server yet.";
            }

            data.claimedContractRewards.add(id);
            ++data.revision;
            PlayerDataStore.save();
            grantStandard(player, def.money, def.gems, def.rareCandies, def.keyId, def.keyCount);
            return "Journey reward claimed: " + def.title + ".";
        }

        if ("journey:grand_master".equals(id)) {
            if (!finalClaimable(data)) {
                return "!Complete the full 32-badge Journey and final requirements first.";
            }

            // Save the one-time claim before executing the high-value commands.
            data.claimedContractRewards.add(id);
            ++data.revision;
            PlayerDataStore.save();

            EconomyService.deposit(player, 500000L);
            EconomyService.depositGems(player, 50L);
            giveItem(player, "cobblemon:master_ball", 1);
            CrateService.giveKeys(player, "shiny", 5);
            CrateService.giveKeys(player, "legendary", 3);

            // Uses the same configured pokegive bridge already used by CobbleClub crates.
            CrateService.givePokemonReward(player, "lucario", List.of("shiny", "mega"));
            CrateService.givePokemonReward(player, "gengar", List.of("shiny", "alpha"));
            CrateService.givePokemonReward(player, "mewtwo", List.of("shiny"));

            player.sendMessage(
                    Text.literal("✦ GRAND MASTER COMPLETE ✦")
                            .formatted(Formatting.GOLD, Formatting.BOLD),
                    false
            );
            return "Grand Master rewards delivered.";
        }

        return "!That contract no longer exists.";
    }

    public static JsonObject state(ServerPlayerEntity player, String notice, boolean error) {
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.normalize();

        JsonObject root = new JsonObject();
        root.addProperty("notice", notice == null ? "" : notice);
        root.addProperty("error", error);
        root.addProperty("balanceText", EconomyService.format(EconomyService.balance(player)));
        root.addProperty("refreshText", ActivityEconomyService.contractRefreshText(player));
        root.add("refreshing", ActivityEconomyService.refreshingContractsJson(player));
        root.add("milestones", milestoneJson(data));
        root.add("journey", journeyJson(data));
        return root;
    }

    private static void send(ServerPlayerEntity player, String notice, boolean error, boolean open) {
        String json = GSON.toJson(state(player, notice, error));
        if (open) {
            if (ServerPlayNetworking.canSend(player, ContractsPayloads.Open.ID)) {
                ServerPlayNetworking.send(player, new ContractsPayloads.Open(json));
            }
        } else if (ServerPlayNetworking.canSend(player, ContractsPayloads.State.ID)) {
            ServerPlayNetworking.send(player, new ContractsPayloads.State(json));
        }
    }

    private static JsonArray milestoneJson(PlayerDataStore.PlayerData data) {
        JsonArray out = new JsonArray();
        for (MilestoneDef def : MILESTONES) {
            long current = metric(data, def.metric);
            boolean claimed = data.claimedContractRewards.contains(def.id);
            boolean complete = current >= def.target;

            JsonObject entry = baseEntry(
                    def.id,
                    def.title,
                    "Permanent Milestone",
                    def.description,
                    rewardText(def.money, def.gems, 0, def.keyId, def.keyCount),
                    claimed ? "CLAIMED" : complete ? "REWARD READY" : "IN PROGRESS",
                    Math.min(current, def.target),
                    def.target,
                    complete && !claimed,
                    claimed,
                    false,
                    false
            );
            JsonArray objectives = new JsonArray();
            objectives.add(objective(metricLabel(def.metric), Math.min(current, def.target), def.target));
            entry.add("objectives", objectives);
            out.add(entry);
        }
        return out;
    }

    private static JsonArray journeyJson(PlayerDataStore.PlayerData data) {
        JsonArray out = new JsonArray();

        for (JourneyDef def : JOURNEY) {
            JourneyStatus js = journeyStatus(data, def);
            boolean claimed = data.claimedContractRewards.contains(def.id);
            int done = 0;
            if (data.pokemonCatches >= def.requiredCatches) done++;
            if (data.pokemonDefeats >= def.requiredDefeats) done++;
            if (data.rctMilestones.contains(def.milestone)) done++;

            String status;
            if (claimed) {
                status = "CLAIMED";
            } else if (!js.regionReady) {
                status = "RCT PATH PENDING";
            } else if (!js.previousClaimed) {
                status = "LOCKED";
            } else if (js.claimable) {
                status = "REWARD READY";
            } else {
                status = "ACTIVE";
            }

            JsonObject entry = baseEntry(
                    def.id,
                    def.title,
                    def.subtitle(),
                    def.description,
                    rewardText(def.money, def.gems, def.rareCandies, def.keyId, def.keyCount),
                    status,
                    done,
                    3,
                    js.claimable,
                    claimed,
                    !js.regionReady || !js.previousClaimed,
                    false
            );
            entry.addProperty("region", def.region);
            entry.addProperty("badgeNumber", def.badgeNumber);

            JsonArray objectives = new JsonArray();
            objectives.add(objective("Catch Pokémon", Math.min(data.pokemonCatches, def.requiredCatches), def.requiredCatches));
            objectives.add(objective("Defeat Pokémon", Math.min(data.pokemonDefeats, def.requiredDefeats), def.requiredDefeats));
            objectives.add(objective(def.rctObjective, data.rctMilestones.contains(def.milestone) ? 1 : 0, 1));
            entry.add("objectives", objectives);
            out.add(entry);
        }

        boolean finalClaimed = data.claimedContractRewards.contains("journey:grand_master");
        boolean finalReady = finalClaimable(data);
        JsonObject finale = baseEntry(
                "journey:grand_master",
                "CobbleClub Grand Master",
                "Final Journey Contract",
                "Complete all 32 badge contracts, catch 1,000 Pokémon and defeat 700 Pokémon. This is the permanent capstone of the regional Journey.",
                "₽500,000 PokéDollars · 50 Gems · Master Ball · 5 Shiny Keys · 3 Legendary Keys · 3 Shiny Pokémon (Mega Lucario, Alpha Gengar, Mewtwo)",
                finalClaimed ? "CLAIMED" : finalReady ? "REWARD READY" : "LOCKED",
                finalProgress(data),
                34,
                finalReady && !finalClaimed,
                finalClaimed,
                !finalReady,
                false
        );
        JsonArray finalObjectives = new JsonArray();
        finalObjectives.add(objective("Badge contracts claimed", badgeClaims(data), 32));
        finalObjectives.add(objective("Catch Pokémon", Math.min(data.pokemonCatches, 1000L), 1000L));
        finalObjectives.add(objective("Defeat Pokémon", Math.min(data.pokemonDefeats, 700L), 700L));
        finale.add("objectives", finalObjectives);
        finale.addProperty("region", "All Regions");
        finale.addProperty("badgeNumber", 0);
        out.add(finale);

        return out;
    }

    private static JourneyStatus journeyStatus(PlayerDataStore.PlayerData data, JourneyDef def) {
        boolean regionReady = READY_RCT_REGIONS.contains(def.region);
        boolean previousClaimed = def.requiresClaim == null || data.claimedContractRewards.contains(def.requiresClaim);
        boolean objectives = data.pokemonCatches >= def.requiredCatches
                && data.pokemonDefeats >= def.requiredDefeats
                && data.rctMilestones.contains(def.milestone);
        boolean claimed = data.claimedContractRewards.contains(def.id);
        return new JourneyStatus(regionReady, previousClaimed, regionReady && previousClaimed && objectives && !claimed);
    }

    private static boolean finalClaimable(PlayerDataStore.PlayerData data) {
        return badgeClaims(data) >= 32
                && data.pokemonCatches >= 1000L
                && data.pokemonDefeats >= 700L
                && !data.claimedContractRewards.contains("journey:grand_master");
    }

    private static int badgeClaims(PlayerDataStore.PlayerData data) {
        int count = 0;
        for (JourneyDef def : JOURNEY) {
            if (def.badgeNumber > 0 && data.claimedContractRewards.contains(def.id)) {
                count++;
            }
        }
        return count;
    }

    private static int finalProgress(PlayerDataStore.PlayerData data) {
        int done = Math.min(32, badgeClaims(data));
        if (data.pokemonCatches >= 1000L) done++;
        if (data.pokemonDefeats >= 700L) done++;
        return done;
    }

    private static JsonObject baseEntry(
            String id,
            String title,
            String subtitle,
            String description,
            String reward,
            String status,
            long progress,
            long target,
            boolean claimable,
            boolean claimed,
            boolean locked,
            boolean autoReward
    ) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("title", title);
        entry.addProperty("subtitle", subtitle);
        entry.addProperty("description", description);
        entry.addProperty("reward", reward);
        entry.addProperty("status", status);
        entry.addProperty("progress", Math.max(0L, progress));
        entry.addProperty("target", Math.max(1L, target));
        entry.addProperty("claimable", claimable);
        entry.addProperty("claimed", claimed);
        entry.addProperty("locked", locked);
        entry.addProperty("autoReward", autoReward);
        return entry;
    }

    private static JsonObject objective(String label, long current, long target) {
        JsonObject objective = new JsonObject();
        objective.addProperty("label", label);
        objective.addProperty("current", Math.max(0L, current));
        objective.addProperty("target", Math.max(1L, target));
        objective.addProperty("complete", current >= target);
        return objective;
    }

    private static long metric(PlayerDataStore.PlayerData data, Metric metric) {
        return switch (metric) {
            case CATCHES -> data.pokemonCatches;
            case SHINIES -> data.shinyPokemonCatches;
            case DEFEATS -> data.pokemonDefeats;
        };
    }

    private static String metricLabel(Metric metric) {
        return switch (metric) {
            case CATCHES -> "Pokémon caught";
            case SHINIES -> "Shiny Pokémon caught";
            case DEFEATS -> "Pokémon defeated";
        };
    }

    private static void grantStandard(
            ServerPlayerEntity player,
            long money,
            long gems,
            int rareCandies,
            String keyId,
            int keyCount
    ) {
        if (money > 0L) EconomyService.deposit(player, money);
        if (gems > 0L) EconomyService.depositGems(player, gems);
        if (rareCandies > 0) giveItem(player, "cobblemon:rare_candy", rareCandies);
        if (keyId != null && keyCount > 0) CrateService.giveKeys(player, keyId, keyCount);
    }

    private static void giveItem(ServerPlayerEntity player, String rawId, int amount) {
        if (amount <= 0) {
            return;
        }
        Identifier id = Identifier.tryParse(rawId);
        if (id == null || !Registries.ITEM.containsId(id)) {
            player.sendMessage(Text.literal("Reward item is not installed: " + rawId).formatted(Formatting.YELLOW), false);
            return;
        }
        Item item = Registries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            player.sendMessage(Text.literal("Reward item is not installed: " + rawId).formatted(Formatting.YELLOW), false);
            return;
        }

        int remaining = amount;
        while (remaining > 0) {
            int give = Math.min(item.getMaxCount(), remaining);
            ItemStack stack = new ItemStack(item, give);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            remaining -= give;
        }
    }

    private static String rewardText(long money, long gems, int rareCandies, String keyId, int keyCount) {
        ArrayList<String> parts = new ArrayList<>();
        if (money > 0L) parts.add(EconomyService.format(money));
        if (gems > 0L) parts.add(gems + " Gems");
        if (rareCandies > 0) parts.add(rareCandies + " Rare Candy");
        if (keyId != null && keyCount > 0) {
            String label = titleCase(keyId) + " Key";
            parts.add(keyCount + "× " + label + (keyCount == 1 ? "" : "s"));
        }
        return parts.isEmpty() ? "Progress reward" : String.join(" · ", parts);
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isBlank()) return "";
        StringBuilder out = new StringBuilder();
        for (String part : raw.split("[ _-]+")) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    private static List<JourneyDef> buildJourney() {
        ArrayList<JourneyDef> list = new ArrayList<>();

        String previous = null;

        previous = badge(list, "kanto", 1, "Boulder Badge", "Brock", "kanto:brock", previous, 10, 5, 1500, 1, 2, null, 0);
        previous = badge(list, "kanto", 2, "Cascade Badge", "Misty", "kanto:misty", previous, 20, 12, 2500, 2, 3, null, 0);
        previous = badge(list, "kanto", 3, "Thunder Badge", "Lt. Surge", "kanto:surge", previous, 30, 20, 3500, 3, 4, null, 0);
        previous = badge(list, "kanto", 4, "Rainbow Badge", "Erika", "kanto:erika", previous, 45, 30, 5000, 4, 5, null, 0);
        previous = badge(list, "kanto", 5, "Soul Badge", "Koga", "kanto:koga", previous, 60, 40, 6500, 5, 6, "vote", 1);
        previous = badge(list, "kanto", 6, "Marsh Badge", "Sabrina", "kanto:sabrina", previous, 75, 55, 8000, 6, 8, null, 0);
        previous = badge(list, "kanto", 7, "Volcano Badge", "Blaine", "kanto:blaine", previous, 90, 70, 10000, 8, 10, "shiny", 1);
        previous = badge(list, "kanto", 8, "Earth Badge", "Giovanni", "kanto:giovanni", previous, 110, 90, 15000, 10, 12, "shiny", 1);

        previous = league(list, "kanto:lorelei_contract", "Kanto League · Lorelei", "Lorelei", "kanto:lorelei", previous, 120, 100, 10000, 6, 6, null, 0);
        previous = league(list, "kanto:bruno_contract", "Kanto League · Bruno", "Bruno", "kanto:bruno", previous, 130, 110, 12000, 7, 7, null, 0);
        previous = league(list, "kanto:agatha_contract", "Kanto League · Agatha", "Agatha", "kanto:agatha", previous, 140, 120, 15000, 8, 8, "shiny", 1);
        previous = league(list, "kanto:lance_contract", "Kanto League · Lance", "Lance", "kanto:lance", previous, 150, 130, 20000, 10, 10, null, 0);
        previous = league(list, "kanto:champion_contract", "Kanto Champion", "Champion Blue", "kanto:champion", previous, 160, 140, 50000, 20, 16, "legendary", 1);

        previous = badge(list, "johto", 9, "Zephyr Badge", "Falkner", "johto:falkner", previous, 180, 155, 20000, 10, 10, null, 0);
        previous = badge(list, "johto", 10, "Hive Badge", "Bugsy", "johto:bugsy", previous, 200, 170, 22000, 11, 10, null, 0);
        previous = badge(list, "johto", 11, "Plain Badge", "Whitney", "johto:whitney", previous, 220, 185, 24000, 12, 12, "vote", 1);
        previous = badge(list, "johto", 12, "Fog Badge", "Morty", "johto:morty", previous, 240, 200, 26000, 13, 12, null, 0);
        previous = badge(list, "johto", 13, "Storm Badge", "Chuck", "johto:chuck", previous, 260, 220, 28000, 14, 14, "shiny", 1);
        previous = badge(list, "johto", 14, "Mineral Badge", "Jasmine", "johto:jasmine", previous, 280, 240, 30000, 15, 14, null, 0);
        previous = badge(list, "johto", 15, "Glacier Badge", "Pryce", "johto:pryce", previous, 300, 260, 33000, 16, 16, "shiny", 1);
        previous = badge(list, "johto", 16, "Rising Badge", "Clair", "johto:clair", previous, 320, 280, 38000, 18, 18, "legendary", 1);

        previous = badge(list, "hoenn", 17, "Stone Badge", "Roxanne", "hoenn:roxanne", previous, 340, 300, 42000, 18, 18, null, 0);
        previous = badge(list, "hoenn", 18, "Knuckle Badge", "Brawly", "hoenn:brawly", previous, 365, 320, 45000, 19, 18, "vote", 1);
        previous = badge(list, "hoenn", 19, "Dynamo Badge", "Wattson", "hoenn:wattson", previous, 390, 340, 48000, 20, 20, null, 0);
        previous = badge(list, "hoenn", 20, "Heat Badge", "Flannery", "hoenn:flannery", previous, 415, 360, 52000, 21, 20, "shiny", 1);
        previous = badge(list, "hoenn", 21, "Balance Badge", "Norman", "hoenn:norman", previous, 440, 385, 56000, 22, 22, null, 0);
        previous = badge(list, "hoenn", 22, "Feather Badge", "Winona", "hoenn:winona", previous, 465, 410, 60000, 23, 22, "shiny", 1);
        previous = badge(list, "hoenn", 23, "Mind Badge", "Tate & Liza", "hoenn:tate_liza", previous, 495, 440, 65000, 24, 24, "legendary", 1);
        previous = badge(list, "hoenn", 24, "Rain Badge", "Wallace", "hoenn:wallace", previous, 525, 470, 72000, 26, 24, "legendary", 1);

        previous = badge(list, "sinnoh", 25, "Coal Badge", "Roark", "sinnoh:roark", previous, 555, 500, 76000, 26, 24, null, 0);
        previous = badge(list, "sinnoh", 26, "Forest Badge", "Gardenia", "sinnoh:gardenia", previous, 590, 525, 80000, 28, 26, "shiny", 1);
        previous = badge(list, "sinnoh", 27, "Cobble Badge", "Maylene", "sinnoh:maylene", previous, 625, 550, 85000, 30, 26, null, 0);
        previous = badge(list, "sinnoh", 28, "Fen Badge", "Crasher Wake", "sinnoh:wake", previous, 660, 580, 90000, 32, 28, "shiny", 1);
        previous = badge(list, "sinnoh", 29, "Relic Badge", "Fantina", "sinnoh:fantina", previous, 700, 610, 96000, 34, 28, "legendary", 1);
        previous = badge(list, "sinnoh", 30, "Mine Badge", "Byron", "sinnoh:byron", previous, 735, 640, 102000, 36, 30, "shiny", 2);
        previous = badge(list, "sinnoh", 31, "Icicle Badge", "Candice", "sinnoh:candice", previous, 770, 670, 110000, 38, 32, "legendary", 1);
        badge(list, "sinnoh", 32, "Beacon Badge", "Volkner", "sinnoh:volkner", previous, 800, 700, 120000, 40, 36, "legendary", 2);

        return List.copyOf(list);
    }

    private static String badge(
            List<JourneyDef> list,
            String region,
            int badgeNumber,
            String badgeName,
            String leader,
            String milestone,
            String requires,
            long catches,
            long defeats,
            long money,
            long gems,
            int candy,
            String key,
            int keyCount
    ) {
        String id = "journey:badge_" + badgeNumber;
        String regionName = titleCase(region);
        String description = badgeNumber <= 8
                ? "Advance through the CobbleClub " + regionName + " RCT pathway. Build your Pokédex, win battles and defeat " + leader + " for the " + badgeName + "."
                : "Continue the 32-badge regional Journey through " + regionName + ". Complete the activity goals and defeat " + leader + " in the RCT pathway.";
        list.add(new JourneyDef(
                id,
                badgeNumber,
                regionName,
                badgeName,
                "Defeat " + leader,
                description,
                milestone,
                "Defeat " + leader + " in RCT",
                requires,
                catches,
                defeats,
                money,
                gems,
                candy,
                key,
                keyCount
        ));
        return id;
    }

    private static String league(
            List<JourneyDef> list,
            String id,
            String title,
            String opponent,
            String milestone,
            String requires,
            long catches,
            long defeats,
            long money,
            long gems,
            int candy,
            String key,
            int keyCount
    ) {
        list.add(new JourneyDef(
                "journey:" + id,
                0,
                "Kanto",
                title,
                "Indigo League",
                "Push beyond the eight Kanto badges and clear the next stage of the Indigo League.",
                milestone,
                "Defeat " + opponent + " in RCT",
                requires,
                catches,
                defeats,
                money,
                gems,
                candy,
                key,
                keyCount
        ));
        return "journey:" + id;
    }

    private enum Metric {
        CATCHES,
        SHINIES,
        DEFEATS
    }

    private record MilestoneDef(
            String id,
            String title,
            String description,
            Metric metric,
            long target,
            long money,
            long gems,
            String keyId,
            int keyCount
    ) {
    }

    private record JourneyDef(
            String id,
            int badgeNumber,
            String region,
            String title,
            String stage,
            String description,
            String milestone,
            String rctObjective,
            String requiresClaim,
            long requiredCatches,
            long requiredDefeats,
            long money,
            long gems,
            int rareCandies,
            String keyId,
            int keyCount
    ) {
        String subtitle() {
            return badgeNumber > 0
                    ? region + " · Badge " + badgeNumber + " of 32 · " + stage
                    : region + " · " + stage;
        }
    }

    private record JourneyStatus(boolean regionReady, boolean previousClaimed, boolean claimable) {
    }
}
