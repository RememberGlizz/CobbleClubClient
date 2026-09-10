package com.cobbleclub.server.service;

import com.cobbleclub.server.network.SellPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class SellGuiInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(SellPayloads.Action.ID, SellPayloads.Action.CODEC);
        PayloadTypeRegistry.playS2C().register(SellPayloads.Open.ID, SellPayloads.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(SellPayloads.State.ID, SellPayloads.State.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SellPayloads.Action.ID, (payload, context) ->
                context.server().execute(() -> ActivityEconomyService.handleSellAction(context.player(), payload.json())));
    }
}
