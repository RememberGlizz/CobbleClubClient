package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FeatherboardService {
    private static final int SNAPSHOT_INTERVAL_TICKS = 100;
    private static int ticks;

    private FeatherboardService() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ++ticks < SNAPSHOT_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        sendSnapshots(server);
    }

    public static void playerJoined(MinecraftServer server) {
        if (server == null) return;
        broadcastOnline(server, null);
    }

    public static void playerDisconnected(MinecraftServer server, UUID leavingPlayer) {
        if (server == null) return;
        broadcastOnline(server, leavingPlayer);
    }

    private static void sendSnapshots(MinecraftServer server) {
        float mspt = Math.max(0.0f, server.getAverageTickTime());
        float tps = mspt <= 0.0f ? 20.0f : Math.min(20.0f, 1000.0f / Math.max(50.0f, mspt));
        int online = server.getPlayerManager().getPlayerList().size();

        Map<String, Integer> usedByOwner = new HashMap<>();
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (claim == null || claim.ownerUuid == null) continue;
            usedByOwner.merge(claim.ownerUuid, Math.max(0, claim.area()), FeatherboardService::safeAdd);
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!ServerPlayNetworking.canSend(player, Payloads.FeatherboardState.ID)) {
                continue;
            }

            PlayerDataStore.PlayerData data = EconomyService.data(player);
            data.normalize();

            int used = usedByOwner.getOrDefault(player.getUuidAsString(), 0);
            long totalLong = (long)Math.max(0, CobbleClubServer.config().initialClaimBlocks)
                    + (long)Math.max(0, data.bonusClaimBlocks);
            int total = (int)Math.min(Integer.MAX_VALUE, totalLong);
            int remaining = Math.max(0, total - used);

            String activeTag = data.activeTag == null || data.activeTag.isBlank()
                    ? RankAccessService.defaultTag(player)
                    : data.activeTag;
            String world = player.getServerWorld().getRegistryKey().getValue().toString();

            Payloads.FeatherboardState state = new Payloads.FeatherboardState(
                    player.getGameProfile().getName(),
                    activeTag,
                    world,
                    Math.max(0L, data.balance),
                    Math.max(0L, data.gems),
                    remaining,
                    Math.max(0L, data.pokemonCatches),
                    Math.max(0L, data.shinyPokemonCatches),
                    online,
                    tps,
                    mspt
            );
            ServerPlayNetworking.send(player, (CustomPayload)state);
        }
    }

    private static void broadcastOnline(MinecraftServer server, UUID leavingPlayer) {
        int online = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (leavingPlayer != null && leavingPlayer.equals(player.getUuid())) {
                continue;
            }
            ++online;
        }

        Payloads.FeatherboardOnline payload = new Payloads.FeatherboardOnline(online);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (leavingPlayer != null && leavingPlayer.equals(player.getUuid())) {
                continue;
            }
            if (ServerPlayNetworking.canSend(player, Payloads.FeatherboardOnline.ID)) {
                ServerPlayNetworking.send(player, (CustomPayload)payload);
            }
        }
    }

    private static int safeAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) return Integer.MAX_VALUE;
        return Math.max(0, left + right);
    }
}
