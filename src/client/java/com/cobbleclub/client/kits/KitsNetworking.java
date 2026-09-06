package com.cobbleclub.client.kits;

import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class KitsNetworking {
   private KitsNetworking() {}

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(Payloads.KitsOpen.ID, (payload, context) ->
              context.client().setScreen(new KitsScreen(KitsState.parse(payload.json()))));
      ClientPlayNetworking.registerGlobalReceiver(Payloads.KitsState.ID, (payload, context) -> {
         Screen current = MinecraftClient.getInstance().currentScreen;
         if (current instanceof KitsScreen screen) screen.applyState(KitsState.parse(payload.json()));
      });
   }

   public static void send(String action) {
      if (action != null && ClientPlayNetworking.canSend(Payloads.KitsAction.ID)) {
         ClientPlayNetworking.send(new Payloads.KitsAction(action));
      }
   }
}
