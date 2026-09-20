/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_8710
 */
package com.cobbleclub.client.sell;

import com.cobbleclub.client.sell.SellScreen;
import com.cobbleclub.client.sell.SellState;
import com.cobbleclub.server.network.SellPayloads;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class SellNetworking
implements ClientModInitializer {
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SellPayloads.Open.ID, (payload, context) -> context.client().setScreen((Screen)new SellScreen(SellState.parse(payload.json()))));
        ClientPlayNetworking.registerGlobalReceiver(SellPayloads.State.ID, (payload, context) -> {
            Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof SellScreen) {
                SellScreen screen = (SellScreen)current;
                screen.applyState(SellState.parse(payload.json()));
            }
        });
    }

    public static void sell(String itemId, int quantity) {
        if (itemId == null || quantity <= 0 || !ClientPlayNetworking.canSend(SellPayloads.Action.ID)) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("action", "sell");
        json.addProperty("item", itemId);
        json.addProperty("quantity", (Number)quantity);
        ClientPlayNetworking.send((CustomPayload)new SellPayloads.Action(json.toString()));
    }

    public static void refresh() {
        if (!ClientPlayNetworking.canSend(SellPayloads.Action.ID)) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("action", "refresh");
        ClientPlayNetworking.send((CustomPayload)new SellPayloads.Action(json.toString()));
    }
}

