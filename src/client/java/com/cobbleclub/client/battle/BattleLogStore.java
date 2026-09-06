package com.cobbleclub.client.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class BattleLogStore {
   private static final int MAX_ENTRIES = 300;
   private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList();
   private static final AtomicInteger REVISION = new AtomicInteger();

   private BattleLogStore() {
   }

   public static void add(Text display, String key) {
      boolean turnMarker = key != null && key.equals("cobblemon.battle.turn");
      ENTRIES.add(new Entry(BattleState.turnNumber, BattleLogColorizer.colorize(display, key), turnMarker));

      while(ENTRIES.size() > 300) {
         ENTRIES.remove(0);
      }

      REVISION.incrementAndGet();
   }

   public static int revision() {
      return REVISION.get();
   }

   public static List<Entry> snapshot() {
      return new ArrayList(ENTRIES);
   }

   public static void clear() {
      ENTRIES.clear();
      REVISION.incrementAndGet();
   }

   @Environment(EnvType.CLIENT)
   public static record Entry(int turn, Text text, boolean turnMarker) {
   }
}
