/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class VoteRewardService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PENDING_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("cobbleclub-pending-votes.json");

    private static final Map<String, Integer> PENDING = new LinkedHashMap<>();

    private static MinecraftServer server;
    private static boolean listenerRegistered;
    private static boolean integrationAvailable;

    private VoteRewardService() {
    }

    public static void initialize(MinecraftServer currentServer) {
        server = currentServer;
        VoteRewardService.loadPending();
        VoteRewardService.registerNuVotifierListener();
    }

    public static void shutdown() {
        VoteRewardService.savePending();
        server = null;
    }

    public static void onJoin(ServerPlayerEntity player) {
        int count;
        String key = VoteRewardService.normalize(player.getGameProfile().getName());

        synchronized (PENDING) {
            count = Math.max(0, PENDING.getOrDefault(key, 0));

            if (count > 0) {
                PENDING.remove(key);
                VoteRewardService.savePending();
            }
        }

        for (int i = 0; i < count; ++i) {
            VoteRewardService.reward(player, "offline vote");
        }

        if (count > 0) {
            player.sendMessage(
                    Text.literal(
                            "Claimed " + count + " vote reward"
                                    + (count == 1 ? "" : "s")
                                    + " from while you were offline."
                    ),
                    false
            );
        }
    }

    public static String status() {
        if (!FabricLoader.getInstance().isModLoaded("nuvotifier-fabric")) {
            return "NuVotifier-Fabric is not installed.";
        }

        if (integrationAvailable && listenerRegistered) {
            return "NuVotifier-Fabric connected through VoteListener.EVENT; CobbleClub vote rewards are active.";
        }

        return "NuVotifier-Fabric is installed, but CobbleClub could not attach its vote listener. Check the server log.";
    }

    public static boolean testReward(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }

        VoteRewardService.reward(player, "admin test");
        return true;
    }

    private static void registerNuVotifierListener() {
        if (listenerRegistered) {
            return;
        }

        if (!FabricLoader.getInstance().isModLoaded("nuvotifier-fabric")) {
            integrationAvailable = false;
            CobbleClubServer.LOGGER.info(
                    "NuVotifier-Fabric is not installed; vote rewards are inactive."
            );
            return;
        }

        try {
            NuVotifierVoteBridge.register();
            listenerRegistered = true;
            integrationAvailable = true;

            CobbleClubServer.LOGGER.info(
                    "CobbleClub registered a direct NuVotifier-Fabric VoteListener.EVENT listener. "
                            + "Votes grant vote keys and PokéDollars."
            );
        } catch (Throwable error) {
            listenerRegistered = false;
            integrationAvailable = false;

            CobbleClubServer.LOGGER.error(
                    "NuVotifier-Fabric was found but CobbleClub could not register VoteListener.EVENT",
                    error
            );
        }
    }

    static void receiveNuVotifierVote(String username, String service, String timestamp) {
        if (username == null || username.isBlank()) {
            CobbleClubServer.LOGGER.warn(
                    "Ignored NuVotifier vote with no username (service={}, timestamp={})",
                    service,
                    timestamp
            );
            return;
        }

        MinecraftServer current = server;
        if (current == null) {
            return;
        }

        String cleanService = service == null || service.isBlank()
                ? "vote site"
                : service.trim();

        current.execute(() ->
                VoteRewardService.handleVote(username.trim(), cleanService)
        );
    }

    private static void handleVote(String username, String service) {
        ServerConfig config = CobbleClubServer.config();
        if (!config.voteRewardsEnabled) {
            return;
        }

        MinecraftServer current = server;
        if (current == null) {
            return;
        }

        ServerPlayerEntity player = current.getPlayerManager().getPlayer(username);
        if (player != null) {
            VoteRewardService.reward(player, service);
            return;
        }

        synchronized (PENDING) {
            String key = VoteRewardService.normalize(username);

            PENDING.put(
                    key,
                    VoteRewardService.safeAdd(
                            PENDING.getOrDefault(key, 0),
                            1
                    )
            );

            VoteRewardService.savePending();
        }

        CobbleClubServer.LOGGER.info(
                "Queued CobbleClub vote reward for offline player {} from {}",
                username,
                service
        );
    }

    private static void reward(ServerPlayerEntity player, String service) {
        ServerConfig config = CobbleClubServer.config();
        if (!config.voteRewardsEnabled) {
            return;
        }

        int keys = Math.max(0, config.voteKeysPerVote);
        long money = Math.max(0L, config.voteMoneyReward);

        if (keys > 0) {
            CrateService.giveKeys(player, "vote", keys);
        }

        if (money > 0L) {
            EconomyService.deposit(player, money);
        }

        StringBuilder message = new StringBuilder("Thanks for voting");

        if (service != null
                && !service.isBlank()
                && !"offline vote".equals(service)) {
            message.append(" on ").append(service);
        }

        message.append("! Reward: ");

        if (keys > 0) {
            message.append(keys)
                    .append(" Vote Key")
                    .append(keys == 1 ? "" : "s");
        }

        if (keys > 0 && money > 0L) {
            message.append(" + ");
        }

        if (money > 0L) {
            message.append(EconomyService.format(money));
        }

        player.sendMessage(
                Text.literal(message.toString()),
                false
        );

        CobbleClubServer.LOGGER.info(
                "Rewarded {} for a vote from {}",
                player.getGameProfile().getName(),
                service
        );
    }

    private static void loadPending() {
        synchronized (PENDING) {
            PENDING.clear();

            try {
                if (!Files.exists(PENDING_PATH)) {
                    return;
                }

                Map<String, Integer> loaded = GSON.fromJson(
                        Files.readString(PENDING_PATH, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, Integer>>() {}.getType()
                );

                if (loaded != null) {
                    loaded.forEach((name, count) -> {
                        if (name != null && count != null && count > 0) {
                            PENDING.put(
                                    VoteRewardService.normalize(name),
                                    count
                            );
                        }
                    });
                }
            } catch (Exception error) {
                CobbleClubServer.LOGGER.warn(
                        "Could not load pending CobbleClub vote rewards",
                        error
                );
            }
        }
    }

    private static void savePending() {
        try {
            Files.createDirectories(PENDING_PATH.getParent());

            Files.writeString(
                    PENDING_PATH,
                    GSON.toJson(PENDING),
                    StandardCharsets.UTF_8
            );
        } catch (Exception error) {
            CobbleClubServer.LOGGER.warn(
                    "Could not save pending CobbleClub vote rewards",
                    error
            );
        }
    }

    private static String normalize(String username) {
        return username == null
                ? ""
                : username.trim().toLowerCase(Locale.ROOT);
    }

    private static int safeAdd(int a, int b) {
        long result = (long) a + (long) b;

        return result > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) Math.max(0L, result);
    }
}
