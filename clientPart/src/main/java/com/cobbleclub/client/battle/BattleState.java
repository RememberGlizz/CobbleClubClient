package com.cobbleclub.client.battle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;

@Environment(EnvType.CLIENT)
public final class BattleState {
   public static volatile int turnNumber = 0;
   public static volatile boolean battleTilesVisible = false;
   private static final Map<UUID, RevealedBattleInfo> REVEALED = new ConcurrentHashMap();
   private static final Map<UUID, TileRect> TILE_RECTS = new ConcurrentHashMap();

   private BattleState() {
   }

   public static class_2561 turnLabel(int turn) {
      return class_2561.method_43469("cobbleclub.battle.turn_counter", new Object[]{turn});
   }

   public static RevealedBattleInfo revealedFor(UUID uuid) {
      return (RevealedBattleInfo)REVEALED.computeIfAbsent(uuid, (k) -> new RevealedBattleInfo());
   }

   public static RevealedBattleInfo revealedOrNull(UUID uuid) {
      return uuid == null ? null : (RevealedBattleInfo)REVEALED.get(uuid);
   }

   public static void putTileRect(UUID uuid, TileRect rect) {
      if (uuid != null) {
         TILE_RECTS.put(uuid, rect);
      }

   }

   public static Map<UUID, TileRect> tileRects() {
      return TILE_RECTS;
   }

   public static void reset() {
      turnNumber = 0;
      battleTilesVisible = false;
      REVEALED.clear();
      TILE_RECTS.clear();
   }

   @Environment(EnvType.CLIENT)
   public static final class TileRect {
      public final float x;
      public final float y;
      public final float width;
      public final float height;
      public final boolean opponent;

      public TileRect(float x, float y, float width, float height, boolean opponent) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.opponent = opponent;
      }
   }
}
