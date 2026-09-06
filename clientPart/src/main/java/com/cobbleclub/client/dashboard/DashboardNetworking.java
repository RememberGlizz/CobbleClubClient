package com.cobbleclub.client.dashboard;

import com.cobbleclub.client.dashboard.net.DashboardActionPayload;
import com.cobbleclub.client.dashboard.net.DashboardOpenPayload;
import com.cobbleclub.client.dashboard.net.DashboardStatePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.class_310;
import net.minecraft.class_437;

@Environment(EnvType.CLIENT)
public final class DashboardNetworking {
   private DashboardNetworking() {}

   public static void init() {
      PayloadTypeRegistry.playS2C().register(DashboardOpenPayload.TYPE, DashboardOpenPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(DashboardStatePayload.TYPE, DashboardStatePayload.STREAM_CODEC);
      PayloadTypeRegistry.playC2S().register(DashboardActionPayload.TYPE, DashboardActionPayload.STREAM_CODEC);

      ClientPlayNetworking.registerGlobalReceiver(DashboardOpenPayload.TYPE, (payload, context) ->
              context.client().method_1507(new DashboardScreen(DashboardState.parse(payload.json()))));
      ClientPlayNetworking.registerGlobalReceiver(DashboardStatePayload.TYPE, (payload, context) -> {
         class_437 current = class_310.method_1551().field_1755;
         if (current instanceof DashboardScreen screen) screen.applyState(DashboardState.parse(payload.json()));
      });
   }

   public static void send(String action) {
      if (action != null && ClientPlayNetworking.canSend(DashboardActionPayload.TYPE)) {
         ClientPlayNetworking.send(new DashboardActionPayload(action));
      }
   }
}
