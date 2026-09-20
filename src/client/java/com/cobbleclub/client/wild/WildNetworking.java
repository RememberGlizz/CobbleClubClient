/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_437
 *  net.minecraft.class_8710
 */
package com.cobbleclub.client.wild;

import com.cobbleclub.client.wild.WildScreen;
import com.cobbleclub.server.world.WildPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class WildNetworking
implements ClientModInitializer {
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(WildPayloads.Open.ID, (payload, context) -> context.client().setScreen((Screen)new WildScreen(payload.subtitle(), payload.worlds())));
    }

    public static void select(String world) {
        if (world == null || !ClientPlayNetworking.canSend(WildPayloads.Select.ID)) {
            return;
        }
        ClientPlayNetworking.send((CustomPayload)new WildPayloads.Select(world));
    }
}

