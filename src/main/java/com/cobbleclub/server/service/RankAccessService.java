package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;
import java.util.Set;

/** Single source of truth for CobbleClub LuckPerms rank/tag/kit access. */
public final class RankAccessService {
    private static final Set<String> KNOWN = Set.of("default", "ace", "champion", "master", "legend", "mod", "admin");

    private RankAccessService() {}

    /**
     * Prefer the exact LuckPerms primary group. This prevents inherited permissions from
     * accidentally unlocking lower/higher premium kits. Permission-node fallback keeps
     * the bridge functional if a server uses contexts/parents instead of primary groups.
     */
    public static String group(ServerPlayerEntity player) {
        String primary = RankPermissionService.primaryGroup(player);
        if (primary != null) {
            primary = primary.toLowerCase(Locale.ROOT);
            if (KNOWN.contains(primary)) return primary;
        }

        if (RankPermissionService.has(player, "cobbleclub.tag.admin")) return "admin";
        if (RankPermissionService.has(player, "cobbleclub.tag.mod")) return "mod";
        ServerConfig c = CobbleClubServer.config();
        if (RankPermissionService.has(player, c.legendKitPermission)) return "legend";
        if (RankPermissionService.has(player, c.masterKitPermission)) return "master";
        if (RankPermissionService.has(player, c.championKitPermission)) return "champion";
        if (RankPermissionService.has(player, c.aceKitPermission)) return "ace";
        return "default";
    }

    public static String premiumRank(ServerPlayerEntity player) {
        String group = group(player);
        return switch (group) {
            case "ace", "champion", "master", "legend" -> group;
            default -> "";
        };
    }

    public static String defaultTag(ServerPlayerEntity player) {
        return defaultTagForGroup(group(player));
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
        if (tagId == null) return false;
        String group = group(player);
        return switch (tagId) {
            // Default-only free tags.
            case "trainer", "newb" -> "default".equals(group);
            // Shiny Hunter is a purchased cross-rank tag for normal/premium players only.
            case "shiny_hunter" -> switch (group) {
                case "default", "ace", "champion", "master", "legend" -> true;
                default -> false;
            };
            // Every premium/staff rank tag is exact-group-only.
            case "ace", "champion", "master", "legend", "mod", "admin" -> tagId.equals(group);
            // Unrelated event/achievement tags keep their existing ownership rules.
            default -> true;
        };
    }
}
