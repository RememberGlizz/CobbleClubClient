/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_8710
 */
package com.cobbleclub.client.net;

import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class ClientModHandshake {
    private static final String UI_PROTOCOL = "wild5-tags24-wool9-noblur1-managedborder1-featherboard1";

    private ClientModHandshake() {
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(Payloads.Handshake.ID)) {
                ClientPlayNetworking.send((CustomPayload)new Payloads.Handshake(ClientModHandshake.modVersion() + "|" + UI_PROTOCOL));
            }
        });
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer("cobbleclub").map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }
}

