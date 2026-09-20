/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.battles.model.actor.ActorType
 *  com.cobblemon.mod.common.client.CobblemonClient
 *  com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon
 *  com.cobblemon.mod.common.client.battle.ClientBattle
 *  com.cobblemon.mod.common.client.battle.ClientBattleActor
 *  com.cobblemon.mod.common.client.battle.ClientBattlePokemon
 *  com.cobblemon.mod.common.client.battle.ClientBattleSide
 *  com.cobblemon.mod.common.client.storage.ClientParty
 *  com.cobblemon.mod.common.pokemon.FormData
 *  com.cobblemon.mod.common.pokemon.Pokemon
 *  kotlin.UninitializedPropertyAccessException
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_310
 */
package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattleActor;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattleSide;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.UUID;
import kotlin.UninitializedPropertyAccessException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(value=EnvType.CLIENT)
public final class BattleUtil {
    private BattleUtil() {
    }

    public static boolean isLocalActor(ClientBattleActor actor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && actor != null && mc.player.getUuid().equals(actor.getUuid());
    }

    public static boolean isWildActor(ClientBattlePokemon pokemon) {
        try {
            return pokemon.getActor().getType() == ActorType.WILD;
        }
        catch (UninitializedPropertyAccessException var2) {
            return false;
        }
    }

    public static FormData form(ClientBattlePokemon pokemon) {
        return pokemon.getSpecies().getForm(pokemon.getState().getCurrentAspects());
    }

    public static boolean isPlayerSide(ClientBattleSide side) {
        if (side == null) {
            return false;
        }
        for (ClientBattleActor actor : side.getActors()) {
            if (!BattleUtil.isLocalActor(actor)) continue;
            return true;
        }
        return false;
    }

    public static ClientBattlePokemon firstOpponentActive() {
        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) {
            return null;
        }
        for (ClientBattleSide side : battle.getSides()) {
            if (BattleUtil.isPlayerSide(side)) continue;
            for (ActiveClientBattlePokemon active : side.getActiveClientBattlePokemon()) {
                if (active.getBattlePokemon() == null) continue;
                return active.getBattlePokemon();
            }
        }
        return null;
    }

    public static ClientBattlePokemon activeByUuid(UUID uuid) {
        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle != null && uuid != null) {
            for (ClientBattleSide side : battle.getSides()) {
                for (ActiveClientBattlePokemon active : side.getActiveClientBattlePokemon()) {
                    ClientBattlePokemon pokemon = active.getBattlePokemon();
                    if (pokemon == null || !uuid.equals(pokemon.getUuid())) continue;
                    return pokemon;
                }
            }
            return null;
        }
        return null;
    }

    public static Pokemon localPartyPokemon(UUID battlePokemonUuid) {
        if (battlePokemonUuid == null) {
            return null;
        }
        try {
            Pokemon found;
            ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
            if (party != null && (found = party.findByUUID(battlePokemonUuid)) != null) {
                return found;
            }
        }
        catch (Throwable party) {
            // empty catch block
        }
        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) {
            return null;
        }
        for (ClientBattleSide side : battle.getSides()) {
            for (ClientBattleActor actor : side.getActors()) {
                for (Pokemon pokemon : actor.getPokemon()) {
                    if (pokemon == null || !battlePokemonUuid.equals(pokemon.getUuid())) continue;
                    return pokemon;
                }
            }
        }
        return null;
    }
}

