package com.cobbleclub.client.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class ClientModHandshake {
   private ClientModHandshake() {
   }

   public static void init() {
      PayloadTypeRegistry.playC2S().register(ClientModHandshakePayload.TYPE, ClientModHandshakePayload.STREAM_CODEC);
      ClientPlayConnectionEvents.JOIN.register((ClientPlayConnectionEvents.Join)(handler, sender, client) -> {
         if (ClientPlayNetworking.canSend(ClientModHandshakePayload.TYPE)) {
            ClientPlayNetworking.send(new ClientModHandshakePayload(modVersion()));
         }

      });
   }

   private static String modVersion() {
      return (String)FabricLoader.getInstance().getModContainer("cobbleclub").map((container) -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
   }
}
