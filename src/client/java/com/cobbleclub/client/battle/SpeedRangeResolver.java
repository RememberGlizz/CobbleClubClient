/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.pokemon.stats.Stat
 *  com.cobblemon.mod.common.api.pokemon.stats.Stats
 *  com.cobblemon.mod.common.client.battle.ClientBattlePokemon
 *  com.cobblemon.mod.common.pokemon.Pokemon
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.BattleUtil;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class SpeedRangeResolver {
    private SpeedRangeResolver() {
    }

    public static SpeedInfo resolve(ClientBattlePokemon pokemon) {
        Pokemon own = BattleUtil.localPartyPokemon(pokemon.getUuid());
        if (own != null) {
            int speed = own.getStat((Stat)Stats.SPEED);
            return new SpeedInfo(true, speed, speed);
        }
        Integer base = (Integer)BattleUtil.form(pokemon).getBaseStats().get(Stats.SPEED);
        if (base == null) {
            return null;
        }
        int level = pokemon.getLevel();
        int maxEvQuarter = BattleUtil.isWildActor(pokemon) ? 0 : 63;
        int min = (int)Math.floor((Math.floor(2.0 * (double)base.intValue() * (double)level / 100.0) + 5.0) * 0.9);
        int max = (int)Math.floor((Math.floor((2.0 * (double)base.intValue() + 31.0 + (double)maxEvQuarter) * (double)level / 100.0) + 5.0) * 1.1);
        return new SpeedInfo(false, min, max);
    }

    @Environment(value=EnvType.CLIENT)
    public record SpeedInfo(boolean exact, int min, int max) {
        public String display() {
            return this.exact ? String.valueOf(this.min) : this.min + "-" + this.max;
        }
    }
}

