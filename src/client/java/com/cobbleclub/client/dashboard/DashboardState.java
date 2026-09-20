/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.dashboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.DecimalFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class DashboardState {
    private static final DecimalFormat COUNT = new DecimalFormat("#,###");
    public final String playerName;
    public final String balanceText;
    public final String gemsText;
    public final int claimTotal;
    public final int claimRemaining;
    public final int claimUsed;
    public final int playtimeRewardAmount;
    public final long playtimeRewardSeconds;
    public final int dailyClaimBlocks;
    public final int dailyGems;
    public final int dailyVoteKeys;
    public final boolean dailyEnabled;
    public final boolean dailyAvailable;
    public final String dailyMoneyText;
    public final boolean purchaseEnabled;
    public final int purchaseAmount;
    public final String purchasePriceText;
    public final int voteKeys;
    public final int shinyKeys;
    public final int legendaryKeys;
    public final String notice;
    public final boolean error;
    private final long receivedAtMillis;

    private DashboardState(JsonObject json) {
        this.playerName = DashboardState.string(json, "playerName", "Trainer");
        this.balanceText = DashboardState.string(json, "balanceText", "0 Pok\u00e9Dollars");
        this.gemsText = DashboardState.string(json, "gemsText", "0 gems");
        this.claimTotal = DashboardState.integer(json, "claimTotal");
        this.claimRemaining = DashboardState.integer(json, "claimRemaining");
        this.claimUsed = DashboardState.integer(json, "claimUsed");
        this.playtimeRewardAmount = DashboardState.integer(json, "playtimeRewardAmount");
        this.playtimeRewardSeconds = DashboardState.number(json, "playtimeRewardSeconds", -1L);
        this.dailyClaimBlocks = DashboardState.integer(json, "dailyClaimBlocks");
        this.dailyGems = DashboardState.integer(json, "dailyGems");
        this.dailyVoteKeys = DashboardState.integer(json, "dailyVoteKeys");
        this.dailyEnabled = DashboardState.bool(json, "dailyEnabled");
        this.dailyAvailable = DashboardState.bool(json, "dailyAvailable");
        this.dailyMoneyText = DashboardState.string(json, "dailyMoneyText", "0 Pok\u00e9Dollars");
        this.purchaseEnabled = DashboardState.bool(json, "purchaseEnabled");
        this.purchaseAmount = DashboardState.integer(json, "purchaseAmount");
        this.purchasePriceText = DashboardState.string(json, "purchasePriceText", "0 Pok\u00e9Dollars");
        this.voteKeys = DashboardState.integer(json, "voteKeys");
        this.shinyKeys = DashboardState.integer(json, "shinyKeys");
        this.legendaryKeys = DashboardState.integer(json, "legendaryKeys");
        this.notice = DashboardState.string(json, "notice", "");
        this.error = DashboardState.bool(json, "error");
        this.receivedAtMillis = System.currentTimeMillis();
    }

    public static DashboardState parse(String raw) {
        try {
            JsonElement element = JsonParser.parseString((String)(raw == null ? "{}" : raw));
            return new DashboardState(element.isJsonObject() ? element.getAsJsonObject() : new JsonObject());
        }
        catch (Exception ignored) {
            return new DashboardState(new JsonObject());
        }
    }

    public long secondsToReward() {
        if (this.playtimeRewardSeconds < 0L) {
            return -1L;
        }
        long elapsed = Math.max(0L, (System.currentTimeMillis() - this.receivedAtMillis) / 1000L);
        return Math.max(0L, this.playtimeRewardSeconds - elapsed);
    }

    public static String count(long value) {
        return COUNT.format(value);
    }

    private static String string(JsonObject json, String key, String fallback) {
        try {
            return json.has(key) ? json.get(key).getAsString() : fallback;
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static int integer(JsonObject json, String key) {
        return (int)DashboardState.number(json, key, 0L);
    }

    private static long number(JsonObject json, String key, long fallback) {
        try {
            return json.has(key) ? json.get(key).getAsLong() : fallback;
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).getAsBoolean();
        }
        catch (Exception ignored) {
            return false;
        }
    }
}

