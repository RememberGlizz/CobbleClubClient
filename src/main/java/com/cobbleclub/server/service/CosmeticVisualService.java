/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_3222
 *  net.minecraft.class_8710
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import com.cobbleclub.server.service.TagsService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

public final class CosmeticVisualService {
    private static int ticks;

    private CosmeticVisualService() {
    }

    public static void apply(ServerPlayerEntity owner) {
        PlayerDataStore.PlayerData data = CosmeticVisualService.data(owner);
        owner.setGlowing(!data.wardrobeHidden && data.glow != null && CosmeticVisualService.glow(data.glow) != null);
        TagsService.apply(owner);
        CosmeticVisualService.sync(owner);
    }

    public static void onJoin(ServerPlayerEntity observer) {
        MinecraftServer server = observer.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity owner : server.getPlayerManager().getPlayerList()) {
            CosmeticVisualService.send(observer, owner, false);
        }
    }

    public static void remove(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity observer : server.getPlayerManager().getPlayerList()) {
            CosmeticVisualService.send(observer, owner, true);
        }
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % 20 == 0) {
            for (ServerPlayerEntity owner : server.getPlayerManager().getPlayerList()) {
                PlayerDataStore.PlayerData data = CosmeticVisualService.data(owner);
                if (data.wardrobeHidden || data.glow == null) continue;
                TagsService.apply(owner);
            }
        }
        if (ticks % 200 == 0) {
            for (ServerPlayerEntity owner : server.getPlayerManager().getPlayerList()) {
                CosmeticVisualService.sync(owner);
            }
        }
    }

    private static void sync(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity observer : server.getPlayerManager().getPlayerList()) {
            CosmeticVisualService.send(observer, owner, false);
        }
    }

    private static void send(ServerPlayerEntity observer, ServerPlayerEntity owner, boolean removed) {
        ServerConfig.GlowDefinition selectedGlow;
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)observer, Payloads.CosmeticState.ID)) {
            return;
        }
        PlayerDataStore.PlayerData data = CosmeticVisualService.data(owner);
        JsonObject root = new JsonObject();
        root.addProperty("player", owner.getUuidAsString());
        root.addProperty("removed", Boolean.valueOf(removed));
        root.addProperty("hidden", Boolean.valueOf(data.wardrobeHidden));
        ServerConfig.GlowDefinition glowDefinition = selectedGlow = !removed && !data.wardrobeHidden ? CosmeticVisualService.glow(data.glow) : null;
        if (selectedGlow != null && selectedGlow.colors() != null && !selectedGlow.colors().isEmpty()) {
            root.addProperty("glowColor", (Number)(selectedGlow.colors().get(0) & 0xFFFFFF));
        }
        if (!removed && !data.wardrobeHidden) {
            CosmeticVisualService.add(root, "head", data, data.equipped.get("HELMET"));
            CosmeticVisualService.add(root, "back", data, data.equipped.get("BACKPACK"));
            CosmeticVisualService.add(root, "balloon", data, data.equipped.get("BALLOON"));
        }
        ServerPlayNetworking.send((ServerPlayerEntity)observer, (CustomPayload)new Payloads.CosmeticState(root.toString()));
    }

    private static void add(JsonObject root, String key, PlayerDataStore.PlayerData data, String cosmeticId) {
        ServerConfig.CosmeticDefinition definition = CosmeticVisualService.cosmetic(cosmeticId);
        if (definition == null) {
            return;
        }
        JsonObject value = new JsonObject();
        value.addProperty("id", definition.id());
        value.addProperty("material", definition.material());
        value.addProperty("model", (Number)definition.customModelData());
        Integer color = data.colors.get(definition.id());
        if (definition.dyeable() && color != null) {
            value.addProperty("color", (Number)(color & 0xFFFFFF));
        }
        root.add(key, (JsonElement)value);
    }

    private static ServerConfig.CosmeticDefinition cosmetic(String id) {
        if (id == null) {
            return null;
        }
        for (ServerConfig.CosmeticDefinition definition : CobbleClubServer.config().cosmetics) {
            if (definition == null || !id.equals(definition.id())) continue;
            return definition;
        }
        return null;
    }

    private static ServerConfig.GlowDefinition glow(String id) {
        if (id == null) {
            return null;
        }
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) {
            if (definition == null || !id.equals(definition.id())) continue;
            return definition;
        }
        return null;
    }

    private static PlayerDataStore.PlayerData data(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        return data;
    }
}

