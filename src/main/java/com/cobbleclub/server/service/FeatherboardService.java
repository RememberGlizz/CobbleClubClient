package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FeatherboardService {
    private static int ticks;

    private FeatherboardService() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ++ticks < 40) {
            return;
        }
        ticks = 0;

        float mspt = Math.max(0.0f, server.getAverageTickTime());
        float tps = mspt <= 0.0f ? 20.0f : Math.min(20.0f, 1000.0f / Math.max(50.0f, mspt));
        int online = server.getPlayerManager().getPlayerList().size();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!ServerPlayNetworking.canSend(player, Payloads.FeatherboardState.ID)) {
                continue;
            }

            PlayerDataStore.PlayerData data = EconomyService.data(player);
            data.normalize();

            int used = ClaimsStore.all().stream()
                    .filter(claim -> claim != null && player.getUuidAsString().equals(claim.ownerUuid))
                    .mapToInt(ClaimsStore.ClaimData::area)
                    .sum();
            long totalLong = (long)Math.max(0, CobbleClubServer.config().initialClaimBlocks)
                    + (long)Math.max(0, data.bonusClaimBlocks);
            int total = (int)Math.min(Integer.MAX_VALUE, totalLong);
            int remaining = Math.max(0, total - used);

            String rank = data.lastTagRank == null || data.lastTagRank.isBlank() ? "newb" : data.lastTagRank;
            String world = player.getServerWorld().getRegistryKey().getValue().toString();

            Payloads.FeatherboardState state = new Payloads.FeatherboardState(
                    player.getGameProfile().getName(),
                    rank,
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
}
