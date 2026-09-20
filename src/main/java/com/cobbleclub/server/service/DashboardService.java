/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.claims.protocol.BudgetInfo
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_3222
 *  net.minecraft.class_8710
 */
package com.cobbleclub.server.service;

import com.cobbleclub.clubhouse.claims.protocol.BudgetInfo;
import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.network.Payloads;
import com.cobbleclub.server.service.CatalogService;
import com.cobbleclub.server.service.ClaimsService;
import com.cobbleclub.server.service.CrateService;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.KitsService;
import com.cobbleclub.server.service.TagsService;
import com.cobbleclub.server.service.WardrobeService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.CustomPayload;

public final class DashboardService {
    private static final Gson GSON = new Gson();

    private DashboardService() {
    }

    public static void open(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.DashboardOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.DashboardOpen(DashboardService.state(player, "", false)));
    }

    public static void handle(ServerPlayerEntity player, String rawAction) {
        String action;
        switch (action = rawAction == null ? "" : rawAction.trim().toLowerCase(Locale.ROOT)) {
            case "refresh": {
                DashboardService.sendState(player, "Dashboard refreshed.", false);
                break;
            }
            case "daily": {
                boolean claimed = EconomyService.claimDaily(player);
                DashboardService.sendState(player, claimed ? "Daily reward claimed!" : "Your daily reward is already claimed for today.", !claimed);
                break;
            }
            case "buy_claim_blocks": {
                String message = ClaimsService.purchaseBlocks(player);
                boolean error = message.startsWith("Buying") || message.startsWith("You need");
                DashboardService.sendState(player, message, error);
                break;
            }
            case "open_claims": {
                ClaimsService.open(player);
                break;
            }
            case "open_kits": {
                KitsService.open(player);
                break;
            }
            case "open_tags": {
                TagsService.open(player);
                break;
            }
            case "open_wardrobe": {
                WardrobeService.open(player);
                break;
            }
            case "open_pokemon": {
                CatalogService.openPokemonSkins(player);
                break;
            }
            case "open_gear": {
                CatalogService.openGear(player);
                break;
            }
            case "open_crate_vote": {
                CatalogService.openCrate(player, "vote");
                break;
            }
            case "open_crate_shiny": {
                CatalogService.openCrate(player, "shiny");
                break;
            }
            case "open_crate_legendary": {
                CatalogService.openCrate(player, "legendary");
                break;
            }
            default: {
                DashboardService.sendState(player, "Unknown CobbleClub action.", true);
            }
        }
    }

    private static void sendState(ServerPlayerEntity player, String notice, boolean error) {
        if (ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.DashboardState.ID)) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.DashboardState(DashboardService.state(player, notice, error)));
        }
    }

    private static String state(ServerPlayerEntity player, String notice, boolean error) {
        ServerConfig config = CobbleClubServer.config();
        BudgetInfo budget = ClaimsService.budget(player);
        int remaining = budget.getRemaining();
        int used = Math.max(0, budget.getTotal() - remaining);
        JsonObject root = new JsonObject();
        root.addProperty("playerName", player.getGameProfile().getName());
        root.addProperty("balance", (Number)EconomyService.balance(player));
        root.addProperty("balanceText", EconomyService.format(EconomyService.balance(player)));
        root.addProperty("gemsText", EconomyService.formatGems(EconomyService.gems(player)));
        root.addProperty("claimTotal", (Number)budget.getTotal());
        root.addProperty("claimRemaining", (Number)remaining);
        root.addProperty("claimUsed", (Number)used);
        root.addProperty("playtimeRewardAmount", (Number)Math.max(0, config.gemsPerPlaytimeReward));
        root.addProperty("playtimeRewardSeconds", (Number)EconomyService.secondsUntilNextPlaytimeReward(player));
        root.addProperty("playtimeIntervalSeconds", (Number)Math.max(0, config.claimBlockRewardIntervalSeconds));
        root.addProperty("dailyEnabled", Boolean.valueOf(config.dailyRewardsEnabled));
        root.addProperty("dailyAvailable", Boolean.valueOf(EconomyService.dailyAvailable(player)));
        root.addProperty("dailyMoneyText", EconomyService.format(config.dailyMoney));
        root.addProperty("dailyClaimBlocks", (Number)Math.max(0, config.dailyClaimBlocks));
        root.addProperty("dailyGems", (Number)Math.max(0, config.dailyGems));
        root.addProperty("dailyVoteKeys", (Number)Math.max(0, config.dailyVoteKeys));
        root.addProperty("purchaseEnabled", Boolean.valueOf(config.economyEnabled && config.claimBlockPurchaseAmount > 0 && ClaimsService.claimBlockPurchasePrice(player) > 0L));
        root.addProperty("purchaseAmount", (Number)Math.max(0, config.claimBlockPurchaseAmount));
        root.addProperty("purchasePriceText", EconomyService.formatGems(ClaimsService.claimBlockPurchasePrice(player)));
        root.addProperty("voteKeys", (Number)CrateService.keys(player, "vote"));
        root.addProperty("shinyKeys", (Number)CrateService.keys(player, "shiny"));
        root.addProperty("legendaryKeys", (Number)CrateService.keys(player, "legendary"));
        root.addProperty("notice", notice == null ? "" : notice);
        root.addProperty("error", Boolean.valueOf(error));
        return GSON.toJson((JsonElement)root);
    }
}

