package com.cobbleclub.client.contracts;

import com.cobbleclub.server.network.ContractsPayloads;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class ContractsNetworking {
    private ContractsNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(
                ContractsPayloads.Open.ID,
                (payload, context) ->
                        context.client().setScreen(new ContractsScreen(ContractsState.parse(payload.json())))
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ContractsPayloads.State.ID,
                (payload, context) -> {
                    Screen current = MinecraftClient.getInstance().currentScreen;
                    if (current instanceof ContractsScreen screen) {
                        screen.applyState(ContractsState.parse(payload.json()));
                    }
                }
        );
    }

    public static void refresh() {
        send("refresh", null);
    }

    public static void claim(String id) {
        send("claim", id);
    }

    private static void send(String action, String id) {
        if (!ClientPlayNetworking.canSend(ContractsPayloads.Action.ID)) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        if (id != null) {
            json.addProperty("id", id);
        }
        ClientPlayNetworking.send(new ContractsPayloads.Action(json.toString()));
    }
}
