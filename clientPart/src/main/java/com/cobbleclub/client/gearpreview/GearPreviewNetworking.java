package com.cobbleclub.client.gearpreview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

@Environment(EnvType.CLIENT)
public final class GearPreviewNetworking {
   private GearPreviewNetworking() {
   }

   public static void init() {
      PayloadTypeRegistry.playS2C().register(GearCatalogPayload.TYPE, GearCatalogPayload.STREAM_CODEC);
      ClientPlayNetworking.registerGlobalReceiver(GearCatalogPayload.TYPE, (payload, context) -> context.client().method_1507(new GearPreviewScreen(payload.sets())));
   }
}
