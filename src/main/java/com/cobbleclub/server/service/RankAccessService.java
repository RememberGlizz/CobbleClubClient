/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3222
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.service.RankPermissionService;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RankAccessService {
    private static final Set<String> KNOWN = Set.of("default", "ace", "champion", "master", "legend", "mod", "admin");

    private RankAccessService() {
    }

    public static String group(ServerPlayerEntity player) {
        String primary = RankPermissionService.primaryGroup(player);
        if (primary != null && KNOWN.contains(primary = primary.toLowerCase(Locale.ROOT))) {
            return primary;
        }
        if (RankPermissionService.has(player, "cobbleclub.tag.admin")) {
            return "admin";
        }
        if (RankPermissionService.has(player, "cobbleclub.tag.mod")) {
            return "mod";
        }
        ServerConfig c = CobbleClubServer.config();
        if (RankPermissionService.has(player, c.legendKitPermission)) {
            return "legend";
        }
        if (RankPermissionService.has(player, c.masterKitPermission)) {
            return "master";
        }
        if (RankPermissionService.has(player, c.championKitPermission)) {
            return "champion";
        }
        if (RankPermissionService.has(player, c.aceKitPermission)) {
            return "ace";
        }
        return "default";
    }

    public static String premiumRank(ServerPlayerEntity player) {
        String group;
        return switch (group = RankAccessService.group(player)) {
            case "ace", "champion", "master", "legend" -> group;
            default -> "";
        };
    }

    public static String defaultTag(ServerPlayerEntity player) {
        return RankAccessService.defaultTagForGroup(RankAccessService.group(player));
    }

    public static String defaultTagForGroup(String group) {
        return switch (group == null ? "default" : group) {
            case "ace" -> "ace";
            case "champion" -> "champion";
            case "master" -> "master";
            case "legend" -> "legend";
            case "mod" -> "mod";
            case "admin" -> "admin";
            default -> "trainer";
        };
    }

    public static boolean canUseRankTag(ServerPlayerEntity player, String tagId) {
        if (tagId == null) {
            return false;
        }
        String group = RankAccessService.group(player);
        return switch (tagId) {
            case "trainer", "newb" -> "default".equals(group);
            case "shiny_hunter" -> {
                switch (group) {
                    case "default": 
                    case "ace": 
                    case "champion": 
                    case "master": 
                    case "legend": {
                        yield true;
                    }
                }
                yield false;
            }
            case "ace", "champion", "master", "legend", "mod", "admin" -> tagId.equals(group);
            default -> true;
        };
    }
}

