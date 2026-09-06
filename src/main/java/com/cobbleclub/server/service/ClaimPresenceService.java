package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.ClaimsStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Tracks claim entry/exit messages and ejects players banned from a claim. */
public final class ClaimPresenceService {
    private static final Map<UUID, String> CURRENT = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST_SAFE = new HashMap<>();
    private static int ticks;

    private ClaimPresenceService() {}

    public static void tick(MinecraftServer server) {
        if (++ticks % 10 != 0) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) update(player);
    }

    public static void clear() {
        CURRENT.clear();
        LAST_SAFE.clear();
    }

    private static void update(ServerPlayerEntity player) {
        ClaimsStore.ClaimData claim = ClaimsService.claimAt(player.getServerWorld(), player.getBlockPos());
        String previousId = CURRENT.get(player.getUuid());
        if (ClaimsService.isBanned(player, claim)) {
            if (!java.util.Objects.equals(previousId, claim.id)) {
                player.sendMessage(Text.literal("You are banned from this CobbleClub claim."), false);
            }
            BlockPos safe = LAST_SAFE.get(player.getUuid());
            if (safe == null || claim.containsBlock(safe.getX(), safe.getZ())) {
                safe = outside(claim, player);
            }
            int y = player.getServerWorld().getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, safe.getX(), safe.getZ()) + 1;
            player.teleport(player.getServerWorld(), safe.getX() + 0.5, y, safe.getZ() + 0.5, player.getYaw(), player.getPitch());
            CURRENT.remove(player.getUuid());
            return;
        }

        LAST_SAFE.put(player.getUuid(), player.getBlockPos());

        String currentId = claim == null ? null : claim.id;
        if (!CobbleClubServer.config().claimEntryMessages || java.util.Objects.equals(previousId, currentId)) return;

        if (previousId != null) {
            ClaimsStore.ClaimData previous = find(previousId);
            if (previous != null) send(player, previous.leaveTitle, previous.leaveSubtitle, "Leaving " + previous.name);
        }
        if (claim != null) send(player, claim.enterTitle, claim.enterSubtitle, "Entering " + claim.name);
        if (currentId == null) CURRENT.remove(player.getUuid());
        else CURRENT.put(player.getUuid(), currentId);
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
        if (best == left) x = minX - 2;
        else if (best == right) x = maxX + 2;
        else if (best == top) z = minZ - 2;
        else z = maxZ + 2;
        return new BlockPos(x, player.getBlockY(), z);
    }
    private static ClaimsStore.ClaimData find(String id) {
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) if (id.equals(claim.id)) return claim;
        return null;
    }

    private static void send(ServerPlayerEntity player, String title, String subtitle, String fallback) {
        String first = title == null || title.isBlank() ? fallback : title;
        String message = subtitle == null || subtitle.isBlank() ? first : first + " — " + subtitle;
        player.sendMessage(Text.literal(message), true);
    }
}
