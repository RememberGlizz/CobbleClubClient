package com.cobbleclub.server.service;

import com.cobbleclub.clubhouse.claims.protocol.BudgetInfo;
import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.network.Payloads;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

/** Live, client-visible home screen for the CobbleClub server systems. */
public final class DashboardService {
    private static final Gson GSON = new Gson();

    private DashboardService() {}

    public static void open(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, Payloads.DashboardOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerPlayNetworking.send(player, new Payloads.DashboardOpen(state(player, "", false)));
    }

    public static void handle(ServerPlayerEntity player, String rawAction) {
        String action = rawAction == null ? "" : rawAction.trim().toLowerCase(Locale.ROOT);
        switch (action) {
            case "refresh" -> sendState(player, "Dashboard refreshed.", false);
            case "daily" -> {
                boolean claimed = EconomyService.claimDaily(player);
                sendState(player, claimed ? "Daily reward claimed!" : "Your daily reward is already claimed for today.", !claimed);
            }
            case "buy_claim_blocks" -> {
                String message = ClaimsService.purchaseBlocks(player);
                boolean error = message.startsWith("Buying") || message.startsWith("You need");
                sendState(player, message, error);
            }
            case "open_claims" -> ClaimsService.open(player);
            case "open_kits" -> KitsService.open(player);
            case "open_tags" -> TagsService.open(player);
            case "open_wardrobe" -> WardrobeService.open(player);
            case "open_pokemon" -> CatalogService.openPokemonSkins(player);
            case "open_gear" -> CatalogService.openGear(player);
            case "open_crate_vote" -> CatalogService.openCrate(player, "vote");
            case "open_crate_shiny" -> CatalogService.openCrate(player, "shiny");
            case "open_crate_legendary" -> CatalogService.openCrate(player, "legendary");
            default -> sendState(player, "Unknown CobbleClub action.", true);
        }
    }

    private static void sendState(ServerPlayerEntity player, String notice, boolean error) {
        if (ServerPlayNetworking.canSend(player, Payloads.DashboardState.ID)) {
            ServerPlayNetworking.send(player, new Payloads.DashboardState(state(player, notice, error)));
        }
    }

    private static String state(ServerPlayerEntity player, String notice, boolean error) {
        ServerConfig config = CobbleClubServer.config();
        BudgetInfo budget = ClaimsService.budget(player);
        int remaining = budget.getRemaining();
        int used = Math.max(0, budget.getTotal() - remaining);

        JsonObject root = new JsonObject();
        root.addProperty("playerName", player.getGameProfile().getName());
        root.addProperty("balance", EconomyService.balance(player));
        root.addProperty("balanceText", EconomyService.format(EconomyService.balance(player)));
        root.addProperty("gemsText", EconomyService.formatGems(EconomyService.gems(player)));
        root.addProperty("claimTotal", budget.getTotal());
        root.addProperty("claimRemaining", remaining);
        root.addProperty("claimUsed", used);
        root.addProperty("playtimeRewardAmount", Math.max(0, config.gemsPerPlaytimeReward));
        root.addProperty("playtimeRewardSeconds", EconomyService.secondsUntilNextPlaytimeReward(player));
        root.addProperty("playtimeIntervalSeconds", Math.max(0, config.claimBlockRewardIntervalSeconds));
        root.addProperty("dailyEnabled", config.dailyRewardsEnabled);
        root.addProperty("dailyAvailable", EconomyService.dailyAvailable(player));
        root.addProperty("dailyMoneyText", EconomyService.format(config.dailyMoney));
        root.addProperty("dailyClaimBlocks", Math.max(0, config.dailyClaimBlocks));
        root.addProperty("dailyGems", Math.max(0, config.dailyGems));
        root.addProperty("dailyVoteKeys", Math.max(0, config.dailyVoteKeys));
        root.addProperty("purchaseEnabled", config.economyEnabled && config.claimBlockPurchaseAmount > 0 && config.claimBlockPurchasePrice > 0);
        root.addProperty("purchaseAmount", Math.max(0, config.claimBlockPurchaseAmount));
        root.addProperty("purchasePriceText", EconomyService.formatGems(config.claimBlockPurchasePrice));
        root.addProperty("voteKeys", CrateService.keys(player, "vote"));
        root.addProperty("shinyKeys", CrateService.keys(player, "shiny"));
        root.addProperty("legendaryKeys", CrateService.keys(player, "legendary"));
        root.addProperty("notice", notice == null ? "" : notice);
        root.addProperty("error", error);
        return GSON.toJson(root);
    }
}
