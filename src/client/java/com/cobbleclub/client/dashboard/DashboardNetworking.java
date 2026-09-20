/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_8710
 */
package com.cobbleclub.client.dashboard;

import com.cobbleclub.client.dashboard.DashboardScreen;
import com.cobbleclub.client.dashboard.DashboardState;
import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class DashboardNetworking {
    private DashboardNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DashboardOpen.ID, (payload, context) -> context.client().setScreen((Screen)new DashboardScreen(DashboardState.parse(payload.json()))));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DashboardState.ID, (payload, context) -> {
            Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof DashboardScreen) {
                DashboardScreen screen = (DashboardScreen)current;
                screen.applyState(DashboardState.parse(payload.json()));
            }
        });
    }

    public static void send(String action) {
        if (action != null && ClientPlayNetworking.canSend(Payloads.DashboardAction.ID)) {
            ClientPlayNetworking.send((CustomPayload)new Payloads.DashboardAction(action));
        }
    }
}

