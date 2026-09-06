package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SpeedRangeResolver {
   private SpeedRangeResolver() {
   }

   public static SpeedInfo resolve(ClientBattlePokemon pokemon) {
      Pokemon own = BattleUtil.localPartyPokemon(pokemon.getUuid());
      if (own != null) {
         int speed = own.getStat(Stats.SPEED);
         return new SpeedInfo(true, speed, speed);
      } else {
         Integer base = (Integer)BattleUtil.form(pokemon).getBaseStats().get(Stats.SPEED);
         if (base == null) {
            return null;
         } else {
            int level = pokemon.getLevel();
            int maxEvQuarter = BattleUtil.isWildActor(pokemon) ? 0 : 63;
            int min = (int)Math.floor((Math.floor((double)2.0F * (double)base * (double)level / (double)100.0F) + (double)5.0F) * 0.9);
            int max = (int)Math.floor((Math.floor(((double)2.0F * (double)base + (double)31.0F + (double)maxEvQuarter) * (double)level / (double)100.0F) + (double)5.0F) * 1.1);
            return new SpeedInfo(false, min, max);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static record SpeedInfo(boolean exact, int min, int max) {
      public String display() {
         return this.exact ? String.valueOf(this.min) : this.min + "-" + this.max;
      }
   }
}
