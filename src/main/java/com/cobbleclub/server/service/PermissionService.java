/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2168
 *  net.minecraft.class_3222
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.service.RankPermissionService;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PermissionService {
    private PermissionService() {
    }

    public static boolean has(ServerPlayerEntity player, String node, boolean defaultAllowed) {
        if (player == null) {
            return true;
        }
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit : defaultAllowed;
    }

    public static boolean has(ServerCommandSource source, String node, boolean defaultAllowed) {
        if (source == null) {
            return false;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return true;
        }
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit : defaultAllowed;
    }

    public static boolean admin(ServerPlayerEntity player, String node, int fallbackOpLevel) {
        if (player == null) {
            return true;
        }
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit.booleanValue() : player.hasPermissionLevel(fallbackOpLevel);
    }

    public static boolean admin(ServerCommandSource source, String node, int fallbackOpLevel) {
        if (source == null) {
            return false;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return true;
        }
        Boolean explicit = RankPermissionService.check(player, node);
        return explicit != null ? explicit.booleanValue() : source.hasPermissionLevel(fallbackOpLevel);
    }
}

