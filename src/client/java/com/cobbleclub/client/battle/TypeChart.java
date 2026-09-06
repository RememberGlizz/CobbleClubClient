package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.types.ElementalType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TypeChart {
   private static final Map<String, Map<String, Float>> CHART = new HashMap();

   private TypeChart() {
   }

   private static void row(String attacker, String entries) {
      Map<String, Float> map = new HashMap();

      for(String pair : entries.split(" ")) {
         int colon = pair.indexOf(58);
         map.put(pair.substring(0, colon), Float.parseFloat(pair.substring(colon + 1)));
      }

      CHART.put(attacker, map);
   }

   public static float multiplier(ElementalType attacker, ElementalType defender1, ElementalType defender2) {
      if (attacker == null) {
         return 1.0F;
      } else {
         Map<String, Float> row = (Map)CHART.get(attacker.getName().toLowerCase(Locale.ROOT));
         return row == null ? 1.0F : against(row, defender1) * against(row, defender2);
      }
   }

   private static float against(Map<String, Float> row, ElementalType defender) {
      return defender == null ? 1.0F : (Float)row.getOrDefault(defender.getName().toLowerCase(Locale.ROOT), 1.0F);
   }

   static {
      row("normal", "rock:0.5 ghost:0 steel:0.5");
      row("fire", "fire:0.5 water:0.5 grass:2 ice:2 bug:2 rock:0.5 dragon:0.5 steel:2");
      row("water", "fire:2 water:0.5 grass:0.5 ground:2 rock:2 dragon:0.5");
      row("electric", "water:2 electric:0.5 grass:0.5 ground:0 flying:2 dragon:0.5");
      row("grass", "fire:0.5 water:2 grass:0.5 poison:0.5 ground:2 flying:0.5 bug:0.5 rock:2 dragon:0.5 steel:0.5");
      row("ice", "fire:0.5 water:0.5 grass:2 ice:0.5 ground:2 flying:2 dragon:2 steel:0.5");
      row("fighting", "normal:2 ice:2 poison:0.5 flying:0.5 psychic:0.5 bug:0.5 rock:2 ghost:0 dark:2 steel:2 fairy:0.5");
      row("poison", "grass:2 poison:0.5 ground:0.5 rock:0.5 ghost:0.5 steel:0 fairy:2");
      row("ground", "fire:2 electric:2 grass:0.5 poison:2 flying:0 bug:0.5 rock:2 steel:2");
      row("flying", "electric:0.5 grass:2 fighting:2 bug:2 rock:0.5 steel:0.5");
      row("psychic", "fighting:2 poison:2 psychic:0.5 dark:0 steel:0.5");
      row("bug", "fire:0.5 grass:2 fighting:0.5 poison:0.5 flying:0.5 psychic:2 ghost:0.5 dark:2 steel:0.5 fairy:0.5");
      row("rock", "fire:2 ice:2 fighting:0.5 ground:0.5 flying:2 bug:2 steel:0.5");
      row("ghost", "normal:0 psychic:2 ghost:2 dark:0.5");
      row("dragon", "dragon:2 steel:0.5 fairy:0");
      row("dark", "fighting:0.5 psychic:2 ghost:2 dark:0.5 fairy:0.5");
      row("steel", "fire:0.5 water:0.5 electric:0.5 ice:2 rock:2 steel:0.5 fairy:2");
      row("fairy", "fire:0.5 fighting:2 poison:0.5 dragon:2 dark:2 steel:0.5");
   }
}
