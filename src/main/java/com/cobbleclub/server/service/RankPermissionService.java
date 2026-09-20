/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3222
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RankPermissionService {
    private static boolean warnedUnavailable;

    private RankPermissionService() {
    }

    public static Boolean check(ServerPlayerEntity player, String permission) {
        if (permission == null || permission.isBlank()) {
            return null;
        }
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get", new Class[0]).invoke(null, new Object[0]);
            Class<?> luckPermsType = Class.forName("net.luckperms.api.LuckPerms");
            Object userManager = luckPermsType.getMethod("getUserManager", new Class[0]).invoke(luckPerms, new Object[0]);
            Class<?> userManagerType = Class.forName("net.luckperms.api.model.user.UserManager");
            Object user = userManagerType.getMethod("getUser", UUID.class).invoke(userManager, player.getUuid());
            if (user == null) {
                return null;
            }
            Class<?> userType = Class.forName("net.luckperms.api.model.user.User");
            Object cachedData = userType.getMethod("getCachedData", new Class[0]).invoke(user, new Object[0]);
            Class<?> cachedDataType = Class.forName("net.luckperms.api.cacheddata.CachedDataManager");
            Object permissionData = cachedDataType.getMethod("getPermissionData", new Class[0]).invoke(cachedData, new Object[0]);
            Class<?> permissionDataType = Class.forName("net.luckperms.api.cacheddata.CachedPermissionData");
            Object tristate = permissionDataType.getMethod("checkPermission", String.class).invoke(permissionData, permission);
            Class<?> tristateType = Class.forName("net.luckperms.api.util.Tristate");
            String value = String.valueOf(tristate).toUpperCase(Locale.ROOT);
            if (value.contains("TRUE")) {
                return Boolean.TRUE;
            }
            if (value.contains("FALSE")) {
                return Boolean.FALSE;
            }
            return null;
        }
        catch (ClassNotFoundException unavailable) {
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                CobbleClubServer.LOGGER.info("LuckPerms is not installed; CobbleClub permission nodes use their built-in defaults and OP fallbacks.");
            }
            return null;
        }
        catch (ReflectiveOperationException failure) {
            CobbleClubServer.LOGGER.warn("Could not query LuckPerms permission {} for {}", new Object[]{permission, player.getGameProfile().getName(), failure});
            return null;
        }
    }

    public static boolean has(ServerPlayerEntity player, String permission) {
        return Boolean.TRUE.equals(RankPermissionService.check(player, permission));
    }

    public static String primaryGroup(ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get", new Class[0]).invoke(null, new Object[0]);
            Class<?> luckPermsType = Class.forName("net.luckperms.api.LuckPerms");
            Object userManager = luckPermsType.getMethod("getUserManager", new Class[0]).invoke(luckPerms, new Object[0]);
            Class<?> userManagerType = Class.forName("net.luckperms.api.model.user.UserManager");
            Object user = userManagerType.getMethod("getUser", UUID.class).invoke(userManager, player.getUuid());
            if (user == null) {
                return null;
            }
            Class<?> userType = Class.forName("net.luckperms.api.model.user.User");
            Object value = userType.getMethod("getPrimaryGroup", new Class[0]).invoke(user, new Object[0]);
            return value == null ? null : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        }
        catch (ClassNotFoundException unavailable) {
            return null;
        }
        catch (ReflectiveOperationException failure) {
            CobbleClubServer.LOGGER.warn("Could not query LuckPerms primary group for {}", (Object)player.getGameProfile().getName(), (Object)failure);
            return null;
        }
    }
}

