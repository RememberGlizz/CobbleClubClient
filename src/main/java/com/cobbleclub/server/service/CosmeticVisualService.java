package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Sends direct render state to clients and never spawns cosmetic entities. */
public final class CosmeticVisualService {
    private static int ticks;

    private CosmeticVisualService() {}

    public static void apply(ServerPlayerEntity owner) {
        PlayerDataStore.PlayerData data = data(owner);
        owner.setGlowing(!data.wardrobeHidden && data.glow != null && glow(data.glow) != null);
        TagsService.apply(owner);
        sync(owner);
    }

    public static void onJoin(ServerPlayerEntity observer) {
        MinecraftServer server = observer.getServer();
        if (server == null) return;
        for (ServerPlayerEntity owner : server.getPlayerManager().getPlayerList()) send(observer, owner, false);
    }

    public static void remove(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) return;
        for (ServerPlayerEntity observer : server.getPlayerManager().getPlayerList()) send(observer, owner, true);
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks % 20 == 0) {
            // Other scoreboard/nameplate systems can replace a player's team after wardrobe selection.
            // Re-apply CobbleClub's combined tag/glow team once per second so the chosen glow color
            // cannot silently fall back to vanilla white.
            for (ServerPlayerEntity owner : server.getPlayerManager().getPlayerList()) {
                PlayerDataStore.PlayerData data = data(owner);
                if (!data.wardrobeHidden && data.glow != null) TagsService.apply(owner);
            }
        }
        if (ticks % 200 == 0) {
            for (ServerPlayerEntity owner : server.getPlayerManager().getPlayerList()) sync(owner);
        }
    }

    private static void sync(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) return;
        for (ServerPlayerEntity observer : server.getPlayerManager().getPlayerList()) send(observer, owner, false);
    }

    private static void send(ServerPlayerEntity observer, ServerPlayerEntity owner, boolean removed) {
        if (!ServerPlayNetworking.canSend(observer, Payloads.CosmeticState.ID)) return;
        PlayerDataStore.PlayerData data = data(owner);
        JsonObject root = new JsonObject();
        root.addProperty("player", owner.getUuidAsString());
        root.addProperty("removed", removed);
        root.addProperty("hidden", data.wardrobeHidden);
        ServerConfig.GlowDefinition selectedGlow = !removed && !data.wardrobeHidden ? glow(data.glow) : null;
        if (selectedGlow != null && selectedGlow.colors() != null && !selectedGlow.colors().isEmpty()) {
            root.addProperty("glowColor", selectedGlow.colors().get(0) & 0xFFFFFF);
        }
        if (!removed && !data.wardrobeHidden) {
            add(root, "head", data, data.equipped.get("HELMET"));
            add(root, "back", data, data.equipped.get("BACKPACK"));
            add(root, "balloon", data, data.equipped.get("BALLOON"));
        }
        ServerPlayNetworking.send(observer, new Payloads.CosmeticState(root.toString()));
    }

    private static void add(JsonObject root, String key, PlayerDataStore.PlayerData data, String cosmeticId) {
        ServerConfig.CosmeticDefinition definition = cosmetic(cosmeticId);
        if (definition == null) return;
        JsonObject value = new JsonObject();
        value.addProperty("id", definition.id());
        value.addProperty("material", definition.material());
        value.addProperty("model", definition.customModelData());
        Integer color = data.colors.get(definition.id());
        if (definition.dyeable() && color != null) value.addProperty("color", color & 0xFFFFFF);
        root.add(key, value);
    }

    private static ServerConfig.CosmeticDefinition cosmetic(String id) {
        if (id == null) return null;
        for (ServerConfig.CosmeticDefinition definition : CobbleClubServer.config().cosmetics) {
            if (definition != null && id.equals(definition.id())) return definition;
        }
        return null;
    }

    private static ServerConfig.GlowDefinition glow(String id) {
        if (id == null) return null;
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) {
            if (definition != null && id.equals(definition.id())) return definition;
        }
        return null;
    }

    private static PlayerDataStore.PlayerData data(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        return data;
    }
}
