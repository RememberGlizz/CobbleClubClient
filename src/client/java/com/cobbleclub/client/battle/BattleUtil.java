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

@Environment(EnvType.CLIENT)
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
      } catch (UninitializedPropertyAccessException var2) {
         return false;
      }
   }

   public static FormData form(ClientBattlePokemon pokemon) {
      return pokemon.getSpecies().getForm(pokemon.getState().getCurrentAspects());
   }

   public static boolean isPlayerSide(ClientBattleSide side) {
      if (side == null) {
         return false;
      } else {
         for(ClientBattleActor actor : side.getActors()) {
            if (isLocalActor(actor)) {
               return true;
            }
         }

         return false;
      }
   }

   public static ClientBattlePokemon firstOpponentActive() {
      ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
      if (battle == null) {
         return null;
      } else {
         for(ClientBattleSide side : battle.getSides()) {
            if (!isPlayerSide(side)) {
               for(ActiveClientBattlePokemon active : side.getActiveClientBattlePokemon()) {
                  if (active.getBattlePokemon() != null) {
                     return active.getBattlePokemon();
                  }
               }
            }
         }

         return null;
      }
   }

   public static ClientBattlePokemon activeByUuid(UUID uuid) {
      ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
      if (battle != null && uuid != null) {
         for(ClientBattleSide side : battle.getSides()) {
            for(ActiveClientBattlePokemon active : side.getActiveClientBattlePokemon()) {
               ClientBattlePokemon pokemon = active.getBattlePokemon();
               if (pokemon != null && uuid.equals(pokemon.getUuid())) {
                  return pokemon;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static Pokemon localPartyPokemon(UUID battlePokemonUuid) {
      if (battlePokemonUuid == null) {
         return null;
      } else {
         try {
            ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
            if (party != null) {
               Pokemon found = party.findByUUID(battlePokemonUuid);
               if (found != null) {
                  return found;
               }
            }
         } catch (Throwable var10) {
         }

         ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
         if (battle == null) {
            return null;
         } else {
            for(ClientBattleSide side : battle.getSides()) {
               for(ClientBattleActor actor : side.getActors()) {
                  for(Pokemon pokemon : actor.getPokemon()) {
                     if (pokemon != null && battlePokemonUuid.equals(pokemon.getUuid())) {
                        return pokemon;
                     }
                  }
               }
            }

            return null;
         }
      }
   }
}
