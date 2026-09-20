/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_437
 *  net.minecraft.class_8710
 */
package com.cobbleclub.client.crate;

import com.cobbleclub.client.crate.CrateCatalog;
import com.cobbleclub.client.crate.CratePreviewScreen;
import com.cobbleclub.client.pokemonpreview.PokemonFormCatalog;
import com.cobbleclub.server.network.Payloads;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class CratePreviewNetworking {
    private static final Gson GSON = new Gson();
    private static CrateCatalog pending;
    private static List<CrateCatalog.Prize> pendingPrizes;

    private CratePreviewNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.CratePreview.ID, (payload, context) -> {
            try {
                CrateCatalog catalog = (CrateCatalog)GSON.fromJson(payload.json(), CrateCatalog.class);
                if (catalog == null) {
                    return;
                }
                PokemonFormCatalog.configure(catalog.formAspects, catalog.formButtons, catalog.scopedForms);
                int totalParts = Math.max(1, catalog.totalParts);
                if (totalParts == 1) {
                    CratePreviewNetworking.reset();
                    context.client().setScreen((Screen)new CratePreviewScreen(catalog));
                    return;
                }
                if (catalog.part == 0) {
                    pending = catalog;
                    pendingPrizes = new ArrayList<CrateCatalog.Prize>();
                } else if (pending == null || !Objects.equals(CratePreviewNetworking.pending.id, catalog.id)) {
                    CratePreviewNetworking.reset();
                    return;
                }
                if (catalog.prizes != null) {
                    pendingPrizes.addAll(catalog.prizes);
                }
                if (catalog.part == totalParts - 1) {
                    CratePreviewNetworking.pending.prizes = pendingPrizes;
                    CrateCatalog assembled = pending;
                    CratePreviewNetworking.reset();
                    context.client().setScreen((Screen)new CratePreviewScreen(assembled));
                }
            }
            catch (Exception ignored) {
                CratePreviewNetworking.reset();
            }
        });
    }

    private static void reset() {
        pending = null;
        pendingPrizes = null;
    }

    static void requestTestReward(String crateId, int prizeIndex, boolean shiny) {
        if (crateId != null && ClientPlayNetworking.canSend(Payloads.CrateTestReward.ID)) {
            ClientPlayNetworking.send((CustomPayload)new Payloads.CrateTestReward(crateId, prizeIndex, shiny));
        }
    }
}

