/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.network.SellPayloads;
import com.cobbleclub.server.service.ActivityEconomyService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class SellGuiInitializer
implements ModInitializer {
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(SellPayloads.Action.ID, SellPayloads.Action.CODEC);
        PayloadTypeRegistry.playS2C().register(SellPayloads.Open.ID, SellPayloads.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(SellPayloads.State.ID, SellPayloads.State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SellPayloads.Action.ID, (payload, context) -> context.server().execute(() -> ActivityEconomyService.handleSellAction(context.player(), payload.json())));
    }
}

