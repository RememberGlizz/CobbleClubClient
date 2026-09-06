package com.cobbleclub.client.dashboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.DecimalFormat;

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
      this.playerName = string(json, "playerName", "Trainer");
      this.balanceText = string(json, "balanceText", "0 PokéDollars");
      this.gemsText = string(json, "gemsText", "0 gems");
      this.claimTotal = integer(json, "claimTotal");
      this.claimRemaining = integer(json, "claimRemaining");
      this.claimUsed = integer(json, "claimUsed");
      this.playtimeRewardAmount = integer(json, "playtimeRewardAmount");
      this.playtimeRewardSeconds = number(json, "playtimeRewardSeconds", -1L);
      this.dailyClaimBlocks = integer(json, "dailyClaimBlocks");
      this.dailyGems = integer(json, "dailyGems");
      this.dailyVoteKeys = integer(json, "dailyVoteKeys");
      this.dailyEnabled = bool(json, "dailyEnabled");
      this.dailyAvailable = bool(json, "dailyAvailable");
      this.dailyMoneyText = string(json, "dailyMoneyText", "0 PokéDollars");
      this.purchaseEnabled = bool(json, "purchaseEnabled");
      this.purchaseAmount = integer(json, "purchaseAmount");
      this.purchasePriceText = string(json, "purchasePriceText", "0 PokéDollars");
      this.voteKeys = integer(json, "voteKeys");
      this.shinyKeys = integer(json, "shinyKeys");
      this.legendaryKeys = integer(json, "legendaryKeys");
      this.notice = string(json, "notice", "");
      this.error = bool(json, "error");
      this.receivedAtMillis = System.currentTimeMillis();
   }

   public static DashboardState parse(String raw) {
      try {
         JsonElement element = JsonParser.parseString(raw == null ? "{}" : raw);
         return new DashboardState(element.isJsonObject() ? element.getAsJsonObject() : new JsonObject());
      } catch (Exception ignored) {
         return new DashboardState(new JsonObject());
      }
   }

   public long secondsToReward() {
      if (this.playtimeRewardSeconds < 0) return -1;
      long elapsed = Math.max(0L, (System.currentTimeMillis() - this.receivedAtMillis) / 1000L);
      return Math.max(0L, this.playtimeRewardSeconds - elapsed);
   }

   public static String count(long value) { return COUNT.format(value); }

   private static String string(JsonObject json, String key, String fallback) {
      try { return json.has(key) ? json.get(key).getAsString() : fallback; } catch (Exception ignored) { return fallback; }
   }
   private static int integer(JsonObject json, String key) { return (int) number(json, key, 0L); }
   private static long number(JsonObject json, String key, long fallback) {
      try { return json.has(key) ? json.get(key).getAsLong() : fallback; } catch (Exception ignored) { return fallback; }
   }
   private static boolean bool(JsonObject json, String key) {
      try { return json.has(key) && json.get(key).getAsBoolean(); } catch (Exception ignored) { return false; }
   }
}
