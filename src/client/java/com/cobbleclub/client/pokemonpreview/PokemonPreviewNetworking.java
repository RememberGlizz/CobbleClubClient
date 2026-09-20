/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_437
 */
package com.cobbleclub.client.pokemonpreview;

import com.cobbleclub.client.pokemonpreview.PokemonPreviewScreen;
import com.cobbleclub.client.pokemonpreview.PokemonSkinCatalog;
import com.cobbleclub.server.network.Payloads;
import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;

@Environment(value=EnvType.CLIENT)
public final class PokemonPreviewNetworking {
    private static final Gson GSON = new Gson();

    private PokemonPreviewNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.PokemonSkins.ID, (payload, context) -> {
            try {
                PokemonSkinCatalog catalog = (PokemonSkinCatalog)GSON.fromJson(payload.json(), PokemonSkinCatalog.class);
                if (catalog != null) {
                    context.client().setScreen((Screen)new PokemonPreviewScreen(catalog));
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }
}

