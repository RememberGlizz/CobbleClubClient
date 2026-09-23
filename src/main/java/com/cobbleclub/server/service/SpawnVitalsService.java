package com.cobbleclub.server.service;

import com.cobbleclub.server.data.PlayerDataStore;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * minecraft:overworld is CobbleClub's safe spawn sanctuary.
 *
 * The visible/current vitals are forced full while a player is in spawn, but
 * the values they had before entering are persisted and restored when they
 * leave for any other dimension. This prevents spawn from being used as a free
 * heal or free hunger refill.
 */
public final class SpawnVitalsService {
    private static final String SPAWN_DIMENSION = "minecraft:overworld";

    private SpawnVitalsService() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            update(player);
        }
    }

    private static void update(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();

        boolean inSpawn = SPAWN_DIMENSION.equals(
                player.getServerWorld().getRegistryKey().getValue().toString()
        );

        if (inSpawn) {
            if (!data.spawnVitalsCaptured) {
                HungerManager hunger = player.getHungerManager();
                data.spawnReturnHealth = Math.max(0.0F, player.getHealth());
                data.spawnReturnFoodLevel = hunger.getFoodLevel();
                data.spawnReturnSaturation = hunger.getSaturationLevel();
                data.spawnVitalsCaptured = true;
                ++data.revision;
                PlayerDataStore.save();
            }

            // Spawn is a sanctuary presentation only. Do not mutate the saved
            // return values while the player remains in the overworld.
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            HungerManager hunger = player.getHungerManager();
            if (hunger.getFoodLevel() != 20) {
                hunger.setFoodLevel(20);
            }
            if (hunger.getSaturationLevel() < 20.0F) {
                hunger.setSaturationLevel(20.0F);
            }
            return;
        }

        if (!data.spawnVitalsCaptured) {
            return;
        }

        HungerManager hunger = player.getHungerManager();
        float restoredHealth = Math.max(0.0F, Math.min(player.getMaxHealth(), data.spawnReturnHealth));
        player.setHealth(restoredHealth);
        hunger.setFoodLevel(Math.max(0, Math.min(20, data.spawnReturnFoodLevel)));
        hunger.setSaturationLevel(Math.max(
                0.0F,
                Math.min((float)hunger.getFoodLevel(), data.spawnReturnSaturation)
        ));

        data.spawnVitalsCaptured = false;
        data.spawnReturnHealth = 0.0F;
        data.spawnReturnFoodLevel = 0;
        data.spawnReturnSaturation = 0.0F;
        ++data.revision;
        PlayerDataStore.save();
    }
}
