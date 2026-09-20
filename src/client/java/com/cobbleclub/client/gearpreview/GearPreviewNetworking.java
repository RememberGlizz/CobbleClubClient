/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_437
 */
package com.cobbleclub.client.gearpreview;

import com.cobbleclub.client.gearpreview.GearCatalogPayload;
import com.cobbleclub.client.gearpreview.GearPreviewScreen;
import com.cobbleclub.server.network.Payloads;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;

@Environment(value=EnvType.CLIENT)
public final class GearPreviewNetworking {
    private GearPreviewNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.GearCatalog.ID, (payload, context) -> {
            List<GearCatalogPayload.GearCatalogSet> sets = payload.sets().stream().map(set -> new GearCatalogPayload.GearCatalogSet(set.id(), set.displayName(), set.items())).toList();
            context.client().setScreen((Screen)new GearPreviewScreen(sets));
        });
    }
}

