package com.cobbleclub.client.net;

import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class ClientModHandshake {
   private ClientModHandshake() {}

   public static void init() {
      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
         if (ClientPlayNetworking.canSend(Payloads.Handshake.ID)) {
            ClientPlayNetworking.send(new Payloads.Handshake(modVersion()));
         }
      });
   }

   private static String modVersion() {
      return FabricLoader.getInstance().getModContainer("cobbleclub")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
   }
}
