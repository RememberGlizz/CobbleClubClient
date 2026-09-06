package com.cobbleclub.client.gearpreview;

import com.cobbleclub.server.network.Payloads;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public final class GearPreviewNetworking {
   private GearPreviewNetworking() {}

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(Payloads.GearCatalog.ID, (payload, context) -> {
         List<GearCatalogPayload.GearCatalogSet> sets = payload.sets().stream()
               .map(set -> new GearCatalogPayload.GearCatalogSet(set.id(), set.displayName(), set.items()))
               .toList();
         context.client().setScreen(new GearPreviewScreen(sets));
      });
   }
}
