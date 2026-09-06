package com.cobbleclub.client.battle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class BattleLogColorizer {
   private static final int DEFAULT = -2039584;
   private static final int FAINT = -38037;
   private static final int SUPER = -30148;
   private static final int CRIT = -15797;
   private static final int RESIST = -6643457;
   private static final int HEAL = -10035832;
   private static final int ABILITY = -3171329;
   private static final int ITEM = -995200;
   private static final int BOOST = -9776022;
   private static final int UNBOOST = -38294;
   private static final int STATUS = -2504629;
   private static final int FIELD = -10696761;
   private static final int MOVE = -2234625;
   private static final int SWITCH = -5197648;

   private BattleLogColorizer() {
   }

   public static Text colorize(Text original, String key) {
      return original.copy().withColor(colorFor(key));
   }

   public static int colorFor(String key) {
      if (key == null) {
         return -2039584;
      } else if (key.contains("faint")) {
         return -38037;
      } else if (key.contains("supereffective")) {
         return -30148;
      } else if (key.contains("crit")) {
         return -15797;
      } else if (!key.contains("resisted") && !key.contains("notveryeffective") && !key.contains("immune")) {
         if (!key.contains("heal") && !key.contains("drain")) {
            if (key.contains("ability")) {
               return -3171329;
            } else if (key.contains("unboost")) {
               return -38294;
            } else if (key.contains("boost")) {
               return -9776022;
            } else if (key.contains("item")) {
               return -995200;
            } else if (!key.contains("status") && !key.contains("burn") && !key.contains("poison") && !key.contains("paralax") && !key.contains("paralysis") && !key.contains("sleep") && !key.contains("freeze") && !key.contains("confusion")) {
               if (!key.contains("weather") && !key.contains("terrain") && !key.contains("hazard") && !key.contains("screen") && !key.contains("tailwind") && !key.contains("trickroom") && !key.contains("field") && !key.contains("spikes")) {
                  if (key.contains("used_move")) {
                     return -2234625;
                  } else {
                     return !key.contains("switch") && !key.contains("withdraw") && !key.contains("recall") && !key.contains("sentout") ? -2039584 : -5197648;
                  }
               } else {
                  return -10696761;
               }
            } else {
               return -2504629;
            }
         } else {
            return -10035832;
         }
      } else {
         return -6643457;
      }
   }
}
