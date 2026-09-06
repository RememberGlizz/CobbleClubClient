package com.cobbleclub.client.dashboard;

import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class DashboardNetworking {
   private DashboardNetworking() {}

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(Payloads.DashboardOpen.ID, (payload, context) ->
              context.client().setScreen(new DashboardScreen(DashboardState.parse(payload.json()))));
      ClientPlayNetworking.registerGlobalReceiver(Payloads.DashboardState.ID, (payload, context) -> {
         Screen current = MinecraftClient.getInstance().currentScreen;
         if (current instanceof DashboardScreen screen) screen.applyState(DashboardState.parse(payload.json()));
      });
   }

   public static void send(String action) {
      if (action != null && ClientPlayNetworking.canSend(Payloads.DashboardAction.ID)) {
         ClientPlayNetworking.send(new Payloads.DashboardAction(action));
      }
   }
}
