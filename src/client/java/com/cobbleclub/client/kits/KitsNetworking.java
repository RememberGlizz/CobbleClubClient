package com.cobbleclub.client.kits;

import com.cobbleclub.client.kits.net.KitsActionPayload;
import com.cobbleclub.client.kits.net.KitsOpenPayload;
import com.cobbleclub.client.kits.net.KitsStatePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class KitsNetworking {
   private KitsNetworking() {}

   public static void init() {
      PayloadTypeRegistry.playS2C().register(KitsOpenPayload.TYPE, KitsOpenPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(KitsStatePayload.TYPE, KitsStatePayload.STREAM_CODEC);
      PayloadTypeRegistry.playC2S().register(KitsActionPayload.TYPE, KitsActionPayload.STREAM_CODEC);
      ClientPlayNetworking.registerGlobalReceiver(KitsOpenPayload.TYPE, (payload, context) ->
              context.client().setScreen(new KitsScreen(KitsState.parse(payload.json()))));
      ClientPlayNetworking.registerGlobalReceiver(KitsStatePayload.TYPE, (payload, context) -> {
         Screen current = MinecraftClient.getInstance().currentScreen;
         if (current instanceof KitsScreen screen) screen.applyState(KitsState.parse(payload.json()));
      });
   }

   public static void send(String action) {
      if (action != null && ClientPlayNetworking.canSend(KitsActionPayload.TYPE)) {
         ClientPlayNetworking.send(new KitsActionPayload(action));
      }
   }
}
