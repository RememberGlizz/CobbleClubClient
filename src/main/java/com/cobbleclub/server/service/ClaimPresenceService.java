/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2338
 *  net.minecraft.class_2561
 *  net.minecraft.class_2902$class_2903
 *  net.minecraft.class_3222
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.service.ClaimsService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.world.Heightmap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public final class ClaimPresenceService {
    private static final Map<UUID, String> CURRENT = new HashMap<UUID, String>();
    private static final Map<UUID, BlockPos> LAST_SAFE = new HashMap<UUID, BlockPos>();
    private static int ticks;

    private ClaimPresenceService() {
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % 10 != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ClaimPresenceService.update(player);
        }
    }

    public static void clear() {
        CURRENT.clear();
        LAST_SAFE.clear();
    }

    private static void update(ServerPlayerEntity player) {
        ClaimsStore.ClaimData previous;
        String currentId;
        ClaimsStore.ClaimData claim = ClaimsService.claimAt(player.getServerWorld(), player.getBlockPos());
        String previousId = CURRENT.get(player.getUuid());
        if (ClaimsService.isBanned(player, claim)) {
            BlockPos safe;
            if (!Objects.equals(previousId, claim.id)) {
                player.sendMessage((Text)Text.literal((String)"You are banned from this CobbleClub claim."), false);
            }
            if ((safe = LAST_SAFE.get(player.getUuid())) == null || claim.containsBlock(safe.getX(), safe.getZ())) {
                safe = ClaimPresenceService.outside(claim, player);
            }
            int y = player.getServerWorld().getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, safe.getX(), safe.getZ()) + 1;
            player.teleport(player.getServerWorld(), (double)safe.getX() + 0.5, (double)y, (double)safe.getZ() + 0.5, player.getYaw(), player.getPitch());
            CURRENT.remove(player.getUuid());
            return;
        }
        LAST_SAFE.put(player.getUuid(), player.getBlockPos());
        String string = currentId = claim == null ? null : claim.id;
        if (!CobbleClubServer.config().claimEntryMessages || Objects.equals(previousId, currentId)) {
            return;
        }
        if (previousId != null && (previous = ClaimPresenceService.find(previousId)) != null) {
            ClaimPresenceService.send(player, previous.leaveTitle, previous.leaveSubtitle, "Leaving " + previous.name);
        }
        if (claim != null) {
            ClaimPresenceService.send(player, claim.enterTitle, claim.enterSubtitle, "Entering " + claim.name);
        }
        if (currentId == null) {
            CURRENT.remove(player.getUuid());
        } else {
            CURRENT.put(player.getUuid(), currentId);
        }
    }

    private static BlockPos outside(ClaimsStore.ClaimData claim, ServerPlayerEntity player) {
        int minX = claim.minCx << 4;
        int maxX = (claim.maxCx << 4) + 15;
        int minZ = claim.minCz << 4;
        int maxZ = (claim.maxCz << 4) + 15;
        int x = player.getBlockX();
        int z = player.getBlockZ();
        int left = Math.abs(x - minX);
        int right = Math.abs(maxX - x);
        int top = Math.abs(z - minZ);
        int bottom = Math.abs(maxZ - z);
        int best = Math.min(Math.min(left, right), Math.min(top, bottom));
        if (best == left) {
            x = minX - 2;
        } else if (best == right) {
            x = maxX + 2;
        } else {
            z = best == top ? minZ - 2 : maxZ + 2;
        }
        return new BlockPos(x, player.getBlockY(), z);
    }

    private static ClaimsStore.ClaimData find(String id) {
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (!id.equals(claim.id)) continue;
            return claim;
        }
        return null;
    }

    private static void send(ServerPlayerEntity player, String title, String subtitle, String fallback) {
        String first = title == null || title.isBlank() ? fallback : title;
        String message = subtitle == null || subtitle.isBlank() ? first : first + " \u2014 " + subtitle;
        player.sendMessage((Text)Text.literal((String)message), true);
    }
}

