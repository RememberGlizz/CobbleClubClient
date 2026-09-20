/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2558
 *  net.minecraft.class_2558$class_2559
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 *  net.minecraft.class_3419
 *  net.minecraft.class_5250
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Formatting;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.server.MinecraftServer;

public final class TpaService {
    private static final long REQUEST_TIMEOUT_MS = 60000L;
    private static final long REQUEST_COOLDOWN_MS = 3000L;
    private static final Map<UUID, TeleportRequest> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    private TpaService() {
    }

    public static int request(ServerPlayerEntity requester, String targetName) {
        ServerPlayerEntity existingRequester;
        long last;
        if (requester == null || targetName == null || targetName.isBlank() || requester.getServer() == null) {
            return 0;
        }

        ServerPlayerEntity target = TpaService.online(requester.getServer(), targetName);
        if (target == null) {
            requester.sendMessage(Text.literal("That player is not online.").formatted(Formatting.RED), false);
            return 0;
        }

        if (target.getUuid().equals(requester.getUuid())) {
            requester.sendMessage(Text.literal("You cannot send a teleport request to yourself.").formatted(Formatting.RED), false);
            return 0;
        }

        long now = System.currentTimeMillis();
        long remaining = 3000L - (now - (last = LAST_REQUEST.getOrDefault(requester.getUuid(), 0L)));
        if (remaining > 0L) {
            long seconds = Math.max(1L, (remaining + 999L) / 1000L);
            requester.sendMessage(
                    Text.literal("Please wait " + seconds + "s before sending another TPA request.").formatted(Formatting.YELLOW),
                    false
            );
            return 0;
        }

        TeleportRequest existing = PENDING.get(target.getUuid());
        if (existing != null
                && existing.expiresAt > now
                && (existingRequester = requester.getServer().getPlayerManager().getPlayer(existing.requesterId)) != null
                && !existingRequester.getUuid().equals(requester.getUuid())) {
            requester.sendMessage(
                    Text.literal(
                            target.getGameProfile().getName()
                                    + " already has a pending teleport request. Try again shortly."
                    ).formatted(Formatting.YELLOW),
                    false
            );
            return 0;
        }

        PENDING.put(target.getUuid(), new TeleportRequest(requester.getUuid(), now + 60000L));
        LAST_REQUEST.put(requester.getUuid(), now);

        requester.sendMessage(
                Text.literal("✦ TPA • ")
                        .formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                        .append(
                                Text.literal("Request sent to " + target.getGameProfile().getName() + ".")
                                        .formatted(Formatting.LIGHT_PURPLE)
                        ),
                false
        );

        target.sendMessage(
                Text.literal("✦ TPA REQUEST ✦").formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
                false
        );

        target.sendMessage(
                Text.literal(requester.getGameProfile().getName() + " wants to teleport to you.")
                        .formatted(Formatting.WHITE),
                false
        );

        MutableText actions = Text.literal("  [ACCEPT]").styled(style ->
                style.withColor(Formatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpayes"))
        );

        actions.append(
                Text.literal("    [DENY]").styled(style ->
                        style.withColor(Formatting.RED)
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpano"))
                )
        );

        target.sendMessage(actions, false);

        target.sendMessage(
                Text.literal("Or type /tpayes or /tpano. Request expires in 60 seconds.")
                        .formatted(Formatting.GRAY),
                false
        );

        target.getServerWorld().playSound(
                null,
                target.getBlockPos(),
                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                SoundCategory.PLAYERS,
                0.8f,
                1.35f
        );

        return 1;
    }

    public static int accept(ServerPlayerEntity target) {
        if (target == null || target.getServer() == null) {
            return 0;
        }

        TeleportRequest request = PENDING.remove(target.getUuid());
        if (request == null) {
            target.sendMessage(
                    Text.literal("You do not have a pending teleport request.").formatted(Formatting.YELLOW),
                    false
            );
            return 0;
        }

        if (request.expiresAt <= System.currentTimeMillis()) {
            target.sendMessage(
                    Text.literal("That teleport request has expired.").formatted(Formatting.YELLOW),
                    false
            );
            return 0;
        }

        ServerPlayerEntity requester = target.getServer().getPlayerManager().getPlayer(request.requesterId);
        if (requester == null) {
            target.sendMessage(
                    Text.literal("That player is no longer online.").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        requester.stopRiding();
        requester.teleport(
                target.getServerWorld(),
                target.getX(),
                target.getY(),
                target.getZ(),
                requester.getYaw(),
                requester.getPitch()
        );

        requester.sendMessage(
                Text.literal("✦ TPA • ")
                        .formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                        .append(
                                Text.literal("Teleported to " + target.getGameProfile().getName() + ".")
                                        .formatted(Formatting.GREEN)
                        ),
                false
        );

        target.sendMessage(
                Text.literal("✦ TPA • ")
                        .formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                        .append(
                                Text.literal("Accepted " + requester.getGameProfile().getName() + "'s request.")
                                        .formatted(Formatting.GREEN)
                        ),
                false
        );

        target.getServerWorld().playSound(
                null,
                target.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.8f,
                1.1f
        );

        return 1;
    }

    public static int deny(ServerPlayerEntity target) {
        if (target == null || target.getServer() == null) {
            return 0;
        }

        TeleportRequest request = PENDING.remove(target.getUuid());
        if (request == null) {
            target.sendMessage(
                    Text.literal("You do not have a pending teleport request.").formatted(Formatting.YELLOW),
                    false
            );
            return 0;
        }

        if (request.expiresAt <= System.currentTimeMillis()) {
            target.sendMessage(
                    Text.literal("That teleport request has expired.").formatted(Formatting.YELLOW),
                    false
            );
            return 0;
        }

        ServerPlayerEntity requester = target.getServer().getPlayerManager().getPlayer(request.requesterId);
        if (requester != null) {
            requester.sendMessage(
                    Text.literal(target.getGameProfile().getName() + " denied your teleport request.")
                            .formatted(Formatting.RED),
                    false
            );
        }

        target.sendMessage(
                Text.literal("Teleport request denied.").formatted(Formatting.RED),
                false
        );

        return 1;
    }

    public static void tick(MinecraftServer server) {
        if (server == null || PENDING.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    public static void forget(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        UUID id = player.getUuid();
        PENDING.remove(id);
        PENDING.entrySet().removeIf(entry -> entry.getValue().requesterId.equals(id));
        LAST_REQUEST.remove(id);
    }

    private static ServerPlayerEntity online(MinecraftServer server, String name) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!name.equalsIgnoreCase(player.getGameProfile().getName())) {
                continue;
            }

            return player;
        }

        return null;
    }

    private record TeleportRequest(UUID requesterId, long expiresAt) {
    }
}
