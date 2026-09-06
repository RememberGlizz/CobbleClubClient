package com.cobbleclub.server.network;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.service.CatalogService;
import com.cobbleclub.server.service.ClaimsService;
import com.cobbleclub.server.service.DashboardService;
import com.cobbleclub.server.service.KitsService;
import com.cobbleclub.server.service.TagsService;
import com.cobbleclub.server.service.WardrobeService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ServerNetworking {
    private ServerNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(Payloads.Handshake.ID, Payloads.Handshake.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.DashboardAction.ID, Payloads.DashboardAction.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.KitsAction.ID, Payloads.KitsAction.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.ClaimsAction.ID, Payloads.ClaimsAction.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.ClaimsMapRequest.ID, Payloads.ClaimsMapRequest.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.TagsAction.ID, Payloads.TagsAction.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.WardrobeAction.ID, Payloads.WardrobeAction.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.CrateTestReward.ID, Payloads.CrateTestReward.CODEC);

        PayloadTypeRegistry.playS2C().register(Payloads.ClaimsOpen.ID, Payloads.ClaimsOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.DashboardOpen.ID, Payloads.DashboardOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.DashboardState.ID, Payloads.DashboardState.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.KitsOpen.ID, Payloads.KitsOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.KitsState.ID, Payloads.KitsState.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.ClaimsState.ID, Payloads.ClaimsState.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.ClaimsMapTiles.ID, Payloads.ClaimsMapTiles.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.ClaimsWorld.ID, Payloads.ClaimsWorld.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.TagsOpen.ID, Payloads.TagsOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.TagsState.ID, Payloads.TagsState.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.WardrobeOpen.ID, Payloads.WardrobeOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.WardrobeState.ID, Payloads.WardrobeState.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.CosmeticState.ID, Payloads.CosmeticState.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.PokemonSkins.ID, Payloads.PokemonSkins.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.CratePreview.ID, Payloads.CratePreview.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.GearCatalog.ID, Payloads.GearCatalog.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Handshake.ID, (payload, context) ->
                context.server().execute(() -> CobbleClubServer.acceptHandshake(context.player(), payload.version())));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.DashboardAction.ID, (payload, context) ->
                context.server().execute(() -> DashboardService.handle(context.player(), payload.action())));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.KitsAction.ID, (payload, context) ->
                context.server().execute(() -> KitsService.handle(context.player(), payload.action())));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ClaimsAction.ID, (payload, context) ->
                context.server().execute(() -> ClaimsService.handleAction(context.player(), payload.json())));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ClaimsMapRequest.ID, (payload, context) ->
                context.server().execute(() -> ClaimsService.handleMapRequest(context.player(), payload.json())));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.TagsAction.ID, (payload, context) ->
                context.server().execute(() -> TagsService.handle(context.player(), payload.json())));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.WardrobeAction.ID, (payload, context) ->
                context.server().execute(() -> WardrobeService.handle(context.player(), payload.json())));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.CrateTestReward.ID, (payload, context) ->
                context.server().execute(() -> CatalogService.handleTestReward(context.player(), payload)));
    }
}
