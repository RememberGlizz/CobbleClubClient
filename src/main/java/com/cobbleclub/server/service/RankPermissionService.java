package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Optional LuckPerms bridge kept reflective so LuckPerms is not a startup dependency. */
public final class RankPermissionService {
    private static boolean warnedUnavailable;

    private RankPermissionService() {}

    public static Boolean check(ServerPlayerEntity player, String permission) {
        if (permission == null || permission.isBlank()) return null;
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get").invoke(null);
            Class<?> luckPermsType = Class.forName("net.luckperms.api.LuckPerms");
            Object userManager = luckPermsType.getMethod("getUserManager").invoke(luckPerms);
            Class<?> userManagerType = Class.forName("net.luckperms.api.model.user.UserManager");
            Object user = userManagerType.getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUuid());
            if (user == null) return null;
            Class<?> userType = Class.forName("net.luckperms.api.model.user.User");
            Object cachedData = userType.getMethod("getCachedData").invoke(user);
            Class<?> cachedDataType = Class.forName("net.luckperms.api.cacheddata.CachedDataManager");
            Object permissionData = cachedDataType.getMethod("getPermissionData").invoke(cachedData);
            Class<?> permissionDataType = Class.forName("net.luckperms.api.cacheddata.CachedPermissionData");
            Object tristate = permissionDataType.getMethod("checkPermission", String.class).invoke(permissionData, permission);
            Class<?> tristateType = Class.forName("net.luckperms.api.util.Tristate");
            String value = String.valueOf(tristate).toUpperCase(java.util.Locale.ROOT);
            if (value.contains("TRUE")) return Boolean.TRUE;
            if (value.contains("FALSE")) return Boolean.FALSE;
            return null;
        } catch (ClassNotFoundException unavailable) {
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                CobbleClubServer.LOGGER.info("LuckPerms is not installed; CobbleClub permission nodes use their built-in defaults and OP fallbacks.");
            }
            return null;
        } catch (ReflectiveOperationException failure) {
            CobbleClubServer.LOGGER.warn("Could not query LuckPerms permission {} for {}", permission, player.getGameProfile().getName(), failure);
            return null;
        }
    }

    public static boolean has(ServerPlayerEntity player, String permission) {
        return Boolean.TRUE.equals(check(player, permission));
    }

    /** Returns the LuckPerms primary group without introducing a hard LuckPerms dependency. */
    public static String primaryGroup(ServerPlayerEntity player) {
        if (player == null) return null;
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get").invoke(null);
            Class<?> luckPermsType = Class.forName("net.luckperms.api.LuckPerms");
            Object userManager = luckPermsType.getMethod("getUserManager").invoke(luckPerms);
            Class<?> userManagerType = Class.forName("net.luckperms.api.model.user.UserManager");
            Object user = userManagerType.getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUuid());
            if (user == null) return null;
            Class<?> userType = Class.forName("net.luckperms.api.model.user.User");
            Object value = userType.getMethod("getPrimaryGroup").invoke(user);
            return value == null ? null : String.valueOf(value).trim().toLowerCase(java.util.Locale.ROOT);
        } catch (ClassNotFoundException unavailable) {
            return null;
        } catch (ReflectiveOperationException failure) {
            CobbleClubServer.LOGGER.warn("Could not query LuckPerms primary group for {}", player.getGameProfile().getName(), failure);
            return null;
        }
    }
}

