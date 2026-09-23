package com.cobbleclub.server.service;

import com.cobblemon.mod.common.Cobblemon;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Single-purpose battle state guard for travel commands.
 * Uses Cobblemon's authoritative battle registry, so wild, player and RCT trainer
 * battles are all covered by the same check.
 */
public final class BattleGuard {
    private BattleGuard() {
    }

    public static boolean inBattle(ServerPlayerEntity player) {
        return player != null
                && Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null;
    }

    public static boolean blockCommand(ServerPlayerEntity player, String command) {
        if (!inBattle(player)) {
            return false;
        }
        player.sendMessage(
                Text.literal("You cannot use /" + command + " while in a Pokémon battle.")
                        .formatted(Formatting.RED),
                false
        );
        return true;
    }

    public static boolean blockTeleport(ServerPlayerEntity player) {
        if (!inBattle(player)) {
            return false;
        }
        player.sendMessage(
                Text.literal("You cannot teleport while in a Pokémon battle.")
                        .formatted(Formatting.RED),
                false
        );
        return true;
    }
}
