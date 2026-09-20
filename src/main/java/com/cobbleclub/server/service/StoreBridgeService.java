/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.service.CosmeticVisualService;
import com.cobbleclub.server.service.CrateService;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.TagsService;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public final class StoreBridgeService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private static final AtomicBoolean REQUEST_RUNNING = new AtomicBoolean();
    private static long nextPollTick;

    private StoreBridgeService() {
    }

    public static void tick(MinecraftServer server) {
        ServerConfig cfg = CobbleClubServer.config();
        if (!cfg.storeEnabled || cfg.storeBridgeToken == null || cfg.storeBridgeToken.isBlank() || cfg.storeApiUrl == null || cfg.storeApiUrl.isBlank()) {
            return;
        }
        long ticks = server.getTicks();
        if (ticks < nextPollTick || !REQUEST_RUNNING.compareAndSet(false, true)) {
            return;
        }
        nextPollTick = ticks + (long)Math.max(5, cfg.storePollIntervalSeconds) * 20L;
        String base = cfg.storeApiUrl.replaceAll("/+$", "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/minecraft/orders")).timeout(Duration.ofSeconds(8L)).header("Authorization", "Bearer " + cfg.storeBridgeToken).GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((response, error) -> {
            REQUEST_RUNNING.set(false);
            if (error != null || response == null || response.statusCode() != 200) {
                return;
            }
            try {
                OrderEnvelope envelope = (OrderEnvelope)GSON.fromJson((String)response.body(), OrderEnvelope.class);
                if (envelope == null || envelope.orders == null) {
                    return;
                }
                server.execute(() -> {
                    for (StoreOrder order : envelope.orders) {
                        StoreBridgeService.process(server, base, cfg.storeBridgeToken, order);
                    }
                });
            }
            catch (Exception e) {
                CobbleClubServer.LOGGER.warn("Could not parse CobbleClub Store order response", (Throwable)e);
            }
        });
    }

    private static void process(MinecraftServer server, String base, String token, StoreOrder order) {
        if (order == null || order.orderId <= 0L || order.minecraftUsername == null || !order.minecraftUsername.matches("[A-Za-z0-9_]{3,16}") || order.fulfillment == null) {
            return;
        }
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(order.minecraftUsername);
        if (player == null) {
            return;
        }
        String marker = Long.toString(order.orderId);
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        if (data.fulfilledStoreOrders.contains(marker)) {
            StoreBridgeService.acknowledge(base, token, order.orderId);
            return;
        }
        if (!StoreBridgeService.grant(server, player, order.fulfillment)) {
            return;
        }
        data.fulfilledStoreOrders.add(marker);
        ++data.revision;
        PlayerDataStore.save();
        player.sendMessage((Text)Text.literal((String)"\u00a7d\u00a7lCobbleClub Store \u00a7r\u00a77\u00bb \u00a7fYour purchase has been delivered!"), false);
        StoreBridgeService.acknowledge(base, token, order.orderId);
    }

    private static boolean grant(MinecraftServer server, ServerPlayerEntity player, Fulfillment f) {
        if (f.type == null || f.value == null || f.quantity <= 0L) {
            return false;
        }
        String type = f.type.toLowerCase(Locale.ROOT);
        String value = f.value.toLowerCase(Locale.ROOT);
        switch (type) {
            case "gems": {
                EconomyService.depositGems(player, f.quantity);
                return true;
            }
            case "key": {
                return Set.of("vote", "shiny", "legendary").contains(value) && CrateService.giveKeys(player, value, Math.toIntExact(Math.min(f.quantity, 1000L)));
            }
            case "cosmetic": {
                if (!value.matches("[a-z0-9_-]{1,64}")) {
                    return false;
                }
                PlayerDataStore.get((UUID)player.getUuid()).ownedCosmetics.add(value);
                CosmeticVisualService.apply(player);
                return true;
            }
            case "rank": {
                if (!Set.of("ace", "champion", "master", "legend").contains(value)) {
                    return false;
                }
                String safeName = player.getGameProfile().getName();
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "lp user " + safeName + " parent set " + value);
                TagsService.syncRank(player, true);
                return true;
            }
        }
        return false;
    }

    private static void acknowledge(String base, String token, long id) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/minecraft/orders/" + id + "/fulfilled")).timeout(Duration.ofSeconds(8L)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"note\":\"Delivered by CobbleClub V3.8\"}")).build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private static final class StoreOrder {
        long orderId;
        String minecraftUsername;
        Fulfillment fulfillment;

        private StoreOrder() {
        }
    }

    private static final class Fulfillment {
        String type;
        String value;
        long quantity;

        private Fulfillment() {
        }
    }

    private static final class OrderEnvelope {
        StoreOrder[] orders;

        private OrderEnvelope() {
        }
    }
}

