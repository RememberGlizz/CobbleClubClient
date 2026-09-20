/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.client.CobblemonClient
 *  com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon
 *  com.cobblemon.mod.common.client.battle.ClientBattle
 *  com.cobblemon.mod.common.client.battle.ClientBattlePokemon
 *  com.cobblemon.mod.common.client.battle.ClientBattleSide
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2561
 *  net.minecraft.class_2588
 *  net.minecraft.class_7417
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.BattleState;
import com.cobbleclub.client.battle.RevealedBattleInfo;
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
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.text.TextContent;

@Environment(value=EnvType.CLIENT)
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
        if (var2 instanceof TranslatableTextContent) {
            TranslatableTextContent tc = (TranslatableTextContent)var2;
            String var5 = tc.getKey();
            Object[] args = tc.getArgs();
            if (TURN.equals(var5)) {
                Integer turn = BattleMessageParser.intArg(args, 0);
                if (turn != null) {
                    BattleState.turnNumber = turn;
                }
            } else if (var5.startsWith(USED_MOVE)) {
                BattleMessageParser.recordMove(args);
            } else if (var5.startsWith(ABILITY_PREFIX)) {
                BattleMessageParser.recordAbility(var5.substring(ABILITY_PREFIX.length()), args);
            } else if (var5.startsWith(ITEM_PREFIX)) {
                BattleMessageParser.recordItem(args, false);
            } else if (var5.startsWith(ENDITEM_PREFIX)) {
                BattleMessageParser.recordItem(args, true);
            }
        }
    }

    private static void recordAbility(String suffix, Object[] args) {
        UUID holder;
        if (args.length >= 1 && !suffix.equals("replace") && (holder = BattleMessageParser.matchActiveByName(BattleMessageParser.componentString(args[0]))) != null) {
            String abilityName;
            if (suffix.equals("generic")) {
                if (args.length < 2) {
                    return;
                }
                abilityName = BattleMessageParser.componentString(args[1]);
            } else {
                abilityName = Text.translatable((String)("cobblemon.ability." + suffix)).getString();
            }
            BattleState.revealedFor(holder).recordAbility(abilityName);
        }
    }

    private static void recordItem(Object[] args, boolean consumed) {
        UUID holder;
        if (args.length >= 1 && (holder = BattleMessageParser.matchActiveByName(BattleMessageParser.componentString(args[0]))) != null) {
            RevealedBattleInfo info = BattleState.revealedFor(holder);
            if (args.length >= 2) {
                info.recordHeldItem(BattleMessageParser.componentString(args[1]));
            }
            if (consumed) {
                info.consumeHeldItem();
            }
        }
    }

    private static void recordMove(Object[] args) {
        UUID user;
        if (args.length >= 2 && (user = BattleMessageParser.matchActiveByName(BattleMessageParser.componentString(args[0]))) != null) {
            String moveId = BattleMessageParser.translatableSuffix(args[1], MOVE_KEY_PREFIX);
            String moveName = BattleMessageParser.componentString(args[1]);
            if (moveId == null) {
                moveId = moveName.toLowerCase(Locale.ROOT).replace(' ', '_');
            }
            BattleState.revealedFor(user).recordMoveUse(moveId, moveName, null);
        }
    }

    private static UUID matchActiveByName(String name) {
        if (name != null && !name.isBlank()) {
            String target = name.trim().toLowerCase(Locale.ROOT);
            ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
            if (battle == null) {
                return null;
            }
            for (ClientBattleSide side : battle.getSides()) {
                for (ActiveClientBattlePokemon active : side.getActiveClientBattlePokemon()) {
                    ClientBattlePokemon pokemon = active.getBattlePokemon();
                    if (pokemon == null) continue;
                    String display = pokemon.getDisplayName().getString().trim().toLowerCase(Locale.ROOT);
                    String species = pokemon.getSpecies().getName().trim().toLowerCase(Locale.ROOT);
                    if (!BattleMessageParser.nameMatches(target, display) && !BattleMessageParser.nameMatches(target, species)) continue;
                    return pokemon.getUuid();
                }
            }
            return null;
        }
        return null;
    }

    private static boolean nameMatches(String messageName, String tileName) {
        return !tileName.isEmpty() && (messageName.equals(tileName) || messageName.endsWith("'s " + tileName));
    }

    private static String componentString(Object arg) {
        if (arg instanceof Text) {
            Text component = (Text)arg;
            return component.getString();
        }
        return arg == null ? "" : String.valueOf(arg);
    }

    private static String translatableSuffix(Object arg, String prefix) {
        TranslatableTextContent tc;
        String key;
        Text component;
        TextContent var4;
        if (arg instanceof Text && (var4 = (component = (Text)arg).getContent()) instanceof TranslatableTextContent && (key = (tc = (TranslatableTextContent)var4).getKey()) != null && key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return null;
    }

    private static Integer intArg(Object[] args, int index) {
        if (args != null && index < args.length) {
            Object arg = args[index];
            if (arg instanceof Number) {
                Number number = (Number)arg;
                return number.intValue();
            }
            try {
                return Integer.parseInt(BattleMessageParser.componentString(arg).trim());
            }
            catch (NumberFormatException var4) {
                return null;
            }
        }
        return null;
    }
}

