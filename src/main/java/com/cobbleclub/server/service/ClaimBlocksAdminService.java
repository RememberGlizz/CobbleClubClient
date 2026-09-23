package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.PlayerDataStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Narrow admin-only claim-block allowance controls.
 * This changes only the existing PlayerDataStore bonusClaimBlocks value and never
 * resizes, removes or otherwise mutates claims.
 */
public final class ClaimBlocksAdminService {
    private ClaimBlocksAdminService() {
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("claimblocks")
                        .requires(source -> PermissionService.admin(source, "cobbleclub.admin.claimblocks", 2))
                        .then(CommandManager.literal("get")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(context -> get(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player")
                                        ))))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> change(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        Mode.ADD
                                                )))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> change(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        Mode.REMOVE
                                                )))))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> change(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        Mode.SET
                                                )))))
        );
    }

    private static int get(ServerCommandSource source, String targetName) {
        ServerPlayerEntity target = online(source, targetName);
        if (target == null) {
            source.sendError(Text.literal("That player is not online."));
            return 0;
        }

        var budget = ClaimsService.budget(target);
        source.sendFeedback(
                () -> Text.literal(target.getGameProfile().getName() + " claim blocks: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(Integer.toString(budget.getTotal())).formatted(Formatting.WHITE, Formatting.BOLD))
                        .append(Text.literal(" total · " + budget.getRemaining() + " remaining").formatted(Formatting.GRAY)),
                false
        );
        return 1;
    }

    private static int change(ServerCommandSource source, String targetName, int amount, Mode mode) {
        ServerPlayerEntity target = online(source, targetName);
        if (target == null) {
            source.sendError(Text.literal("That player is not online."));
            return 0;
        }

        PlayerDataStore.PlayerData data = EconomyService.data(target);
        int initial = Math.max(0, CobbleClubServer.config().initialClaimBlocks);
        int current = total(initial, data.bonusClaimBlocks);
        int next = switch (mode) {
            case ADD -> clamp((long)current + amount);
            case REMOVE -> Math.max(0, current - amount);
            case SET -> amount;
        };

        data.bonusClaimBlocks = next - initial;
        ++data.revision;
        PlayerDataStore.save();

        var budget = ClaimsService.budget(target);
        String verb = switch (mode) {
            case ADD -> "Added " + amount + " claim blocks to ";
            case REMOVE -> "Removed " + amount + " claim blocks from ";
            case SET -> "Set claim blocks for ";
        };

        source.sendFeedback(
                () -> Text.literal(verb + target.getGameProfile().getName() + ". ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal("Total: " + budget.getTotal() + " · Remaining: " + budget.getRemaining())
                                .formatted(Formatting.WHITE)),
                true
        );

        ServerPlayerEntity operator = source.getEntity() instanceof ServerPlayerEntity p ? p : null;
        if (operator != target) {
            target.sendMessage(
                    Text.literal("Your claim block allowance was adjusted by an administrator. ")
                            .formatted(Formatting.GRAY)
                            .append(Text.literal("Total: " + budget.getTotal()).formatted(Formatting.WHITE)),
                    false
            );
        }
        return 1;
    }

    private static ServerPlayerEntity online(ServerCommandSource source, String name) {
        if (source.getServer() == null || name == null) {
            return null;
        }
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            if (name.equalsIgnoreCase(player.getGameProfile().getName())) {
                return player;
            }
        }
        return null;
    }

    private static int total(int initial, int bonus) {
        return clamp((long)initial + bonus);
    }

    private static int clamp(long value) {
        return (int)Math.max(0L, Math.min((long)Integer.MAX_VALUE, value));
    }

    private enum Mode {
        ADD,
        REMOVE,
        SET
    }
}
