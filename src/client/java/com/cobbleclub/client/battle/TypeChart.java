/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.types.ElementalType
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.types.ElementalType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class TypeChart {
    private static final Map<String, Map<String, Float>> CHART = new HashMap<String, Map<String, Float>>();

    private TypeChart() {
    }

    private static void row(String attacker, String entries) {
        HashMap<String, Float> map = new HashMap<String, Float>();
        for (String pair : entries.split(" ")) {
            int colon = pair.indexOf(58);
            map.put(pair.substring(0, colon), Float.valueOf(Float.parseFloat(pair.substring(colon + 1))));
        }
        CHART.put(attacker, map);
    }

    public static float multiplier(ElementalType attacker, ElementalType defender1, ElementalType defender2) {
        if (attacker == null) {
            return 1.0f;
        }
        Map<String, Float> row = CHART.get(attacker.getName().toLowerCase(Locale.ROOT));
        return row == null ? 1.0f : TypeChart.against(row, defender1) * TypeChart.against(row, defender2);
    }

    private static float against(Map<String, Float> row, ElementalType defender) {
        return defender == null ? 1.0f : row.getOrDefault(defender.getName().toLowerCase(Locale.ROOT), Float.valueOf(1.0f)).floatValue();
    }

    static {
        TypeChart.row("normal", "rock:0.5 ghost:0 steel:0.5");
        TypeChart.row("fire", "fire:0.5 water:0.5 grass:2 ice:2 bug:2 rock:0.5 dragon:0.5 steel:2");
        TypeChart.row("water", "fire:2 water:0.5 grass:0.5 ground:2 rock:2 dragon:0.5");
        TypeChart.row("electric", "water:2 electric:0.5 grass:0.5 ground:0 flying:2 dragon:0.5");
        TypeChart.row("grass", "fire:0.5 water:2 grass:0.5 poison:0.5 ground:2 flying:0.5 bug:0.5 rock:2 dragon:0.5 steel:0.5");
        TypeChart.row("ice", "fire:0.5 water:0.5 grass:2 ice:0.5 ground:2 flying:2 dragon:2 steel:0.5");
        TypeChart.row("fighting", "normal:2 ice:2 poison:0.5 flying:0.5 psychic:0.5 bug:0.5 rock:2 ghost:0 dark:2 steel:2 fairy:0.5");
        TypeChart.row("poison", "grass:2 poison:0.5 ground:0.5 rock:0.5 ghost:0.5 steel:0 fairy:2");
        TypeChart.row("ground", "fire:2 electric:2 grass:0.5 poison:2 flying:0 bug:0.5 rock:2 steel:2");
        TypeChart.row("flying", "electric:0.5 grass:2 fighting:2 bug:2 rock:0.5 steel:0.5");
        TypeChart.row("psychic", "fighting:2 poison:2 psychic:0.5 dark:0 steel:0.5");
        TypeChart.row("bug", "fire:0.5 grass:2 fighting:0.5 poison:0.5 flying:0.5 psychic:2 ghost:0.5 dark:2 steel:0.5 fairy:0.5");
        TypeChart.row("rock", "fire:2 ice:2 fighting:0.5 ground:0.5 flying:2 bug:2 steel:0.5");
        TypeChart.row("ghost", "normal:0 psychic:2 ghost:2 dark:0.5");
        TypeChart.row("dragon", "dragon:2 steel:0.5 fairy:0");
        TypeChart.row("dark", "fighting:0.5 psychic:2 ghost:2 dark:0.5 fairy:0.5");
        TypeChart.row("steel", "fire:0.5 water:0.5 electric:0.5 ice:2 rock:2 steel:0.5 fairy:2");
        TypeChart.row("fairy", "fire:0.5 fighting:2 poison:0.5 dragon:2 dark:2 steel:0.5");
    }
}

