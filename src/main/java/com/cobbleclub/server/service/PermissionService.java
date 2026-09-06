package com.cobbleclub.server.service;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/** Optional LuckPerms permission bridge with safe defaults when LuckPerms is absent or a node is unset. */
public final class PermissionService {
    private PermissionService() {}

    public static boolean has(ServerPlayerEntity player, String node, boolean defaultAllowed) {
        if (player == null) return true;
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit : defaultAllowed;
    }

    public static boolean has(ServerCommandSource source, String node, boolean defaultAllowed) {
        if (source == null) return false;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return true; // console / command blocks
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit : defaultAllowed;
    }

    public static boolean admin(ServerPlayerEntity player, String node, int fallbackOpLevel) {
        if (player == null) return true;
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit : player.hasPermissionLevel(fallbackOpLevel);
    }

    public static boolean admin(ServerCommandSource source, String node, int fallbackOpLevel) {
        if (source == null) return false;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return true;
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit : source.hasPermissionLevel(fallbackOpLevel);
    }
}
