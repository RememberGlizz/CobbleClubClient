package com.cobbleclub.client.sell;

import com.cobbleclub.server.network.SellPayloads;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class SellNetworking implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SellPayloads.Open.ID, (payload, context) ->
                context.client().setScreen(new SellScreen(SellState.parse(payload.json()))));

        ClientPlayNetworking.registerGlobalReceiver(SellPayloads.State.ID, (payload, context) -> {
            Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof SellScreen screen) screen.applyState(SellState.parse(payload.json()));
        });
    }

    public static void sell(String itemId, int quantity) {
        if (itemId == null || quantity <= 0 || !ClientPlayNetworking.canSend(SellPayloads.Action.ID)) return;
        JsonObject json = new JsonObject();
        json.addProperty("action", "sell");
        json.addProperty("item", itemId);
        json.addProperty("quantity", quantity);
        ClientPlayNetworking.send(new SellPayloads.Action(json.toString()));
    }

    public static void refresh() {
        if (!ClientPlayNetworking.canSend(SellPayloads.Action.ID)) return;
        JsonObject json = new JsonObject();
        json.addProperty("action", "refresh");
        ClientPlayNetworking.send(new SellPayloads.Action(json.toString()));
    }
}
