package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattleSide;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

@Environment(EnvType.CLIENT)
public final class BattleMessageParser {
   private static final String TURN = "cobblemon.battle.turn";
   private static final String USED_MOVE = "cobblemon.battle.used_move";
   private static final String ABILITY_PREFIX = "cobblemon.battle.ability.";
   private static final String ITEM_PREFIX = "cobblemon.battle.item.";
   private static final String ENDITEM_PREFIX = "cobblemon.battle.enditem.";
   private static final String MOVE_KEY_PREFIX = "cobblemon.move.";

   private BattleMessageParser() {
   }

   public static void accept(Text message) {
      TextContent var2 = message.getContent();
      if (var2 instanceof TranslatableTextContent tc) {
         String var5 = tc.getKey();
         Object[] args = tc.getArgs();
         if ("cobblemon.battle.turn".equals(var5)) {
            Integer turn = intArg(args, 0);
            if (turn != null) {
               BattleState.turnNumber = turn;
            }

         } else {
            if (var5.startsWith("cobblemon.battle.used_move")) {
               recordMove(args);
            } else if (var5.startsWith("cobblemon.battle.ability.")) {
               recordAbility(var5.substring("cobblemon.battle.ability.".length()), args);
            } else if (var5.startsWith("cobblemon.battle.item.")) {
               recordItem(args, false);
            } else if (var5.startsWith("cobblemon.battle.enditem.")) {
               recordItem(args, true);
            }

         }
      }
   }

   private static void recordAbility(String suffix, Object[] args) {
      if (args.length >= 1 && !suffix.equals("replace")) {
         UUID holder = matchActiveByName(componentString(args[0]));
         if (holder != null) {
            String abilityName;
            if (suffix.equals("generic")) {
               if (args.length < 2) {
                  return;
               }

               abilityName = componentString(args[1]);
            } else {
               abilityName = Text.translatable("cobblemon.ability." + suffix).getString();
            }

            BattleState.revealedFor(holder).recordAbility(abilityName);
         }
      }
   }

   private static void recordItem(Object[] args, boolean consumed) {
      if (args.length >= 1) {
         UUID holder = matchActiveByName(componentString(args[0]));
         if (holder != null) {
            RevealedBattleInfo info = BattleState.revealedFor(holder);
            if (args.length >= 2) {
               info.recordHeldItem(componentString(args[1]));
            }

            if (consumed) {
               info.consumeHeldItem();
            }

         }
      }
   }

   private static void recordMove(Object[] args) {
      if (args.length >= 2) {
         UUID user = matchActiveByName(componentString(args[0]));
         if (user != null) {
            String moveId = translatableSuffix(args[1], "cobblemon.move.");
            String moveName = componentString(args[1]);
            if (moveId == null) {
               moveId = moveName.toLowerCase(Locale.ROOT).replace(' ', '_');
            }

            BattleState.revealedFor(user).recordMoveUse(moveId, moveName, (String)null);
         }
      }
   }

   private static UUID matchActiveByName(String name) {
      if (name != null && !name.isBlank()) {
         String target = name.trim().toLowerCase(Locale.ROOT);
         ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
         if (battle == null) {
            return null;
         } else {
            for(ClientBattleSide side : battle.getSides()) {
               for(ActiveClientBattlePokemon active : side.getActiveClientBattlePokemon()) {
                  ClientBattlePokemon pokemon = active.getBattlePokemon();
                  if (pokemon != null) {
                     String display = pokemon.getDisplayName().getString().trim().toLowerCase(Locale.ROOT);
                     String species = pokemon.getSpecies().getName().trim().toLowerCase(Locale.ROOT);
                     if (nameMatches(target, display) || nameMatches(target, species)) {
                        return pokemon.getUuid();
                     }
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean nameMatches(String messageName, String tileName) {
      return !tileName.isEmpty() && (messageName.equals(tileName) || messageName.endsWith("'s " + tileName));
   }

   private static String componentString(Object arg) {
      if (arg instanceof Text component) {
         return component.getString();
      } else {
         return arg == null ? "" : String.valueOf(arg);
      }
   }

   private static String translatableSuffix(Object arg, String prefix) {
      if (arg instanceof Text component) {
         TextContent var4 = component.getContent();
         if (var4 instanceof TranslatableTextContent tc) {
            String key = tc.getKey();
            if (key != null && key.startsWith(prefix)) {
               return key.substring(prefix.length());
            }
         }
      }

      return null;
   }

   private static Integer intArg(Object[] args, int index) {
      if (args != null && index < args.length) {
         Object arg = args[index];
         if (arg instanceof Number) {
            Number number = (Number)arg;
            return number.intValue();
         } else {
            try {
               return Integer.parseInt(componentString(arg).trim());
            } catch (NumberFormatException var4) {
               return null;
            }
         }
      } else {
         return null;
      }
   }
}
