package com.cobbleclub.client.tags;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.clubhouse.tags.protocol.TagsActionMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsActionType;
import com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsProtocol;
import com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg;
import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class TagsNetworking {
   private TagsNetworking() {}

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(Payloads.TagsOpen.ID, (payload, context) -> {
         TagsOpenMsg msg = TagsProtocol.INSTANCE.decode(payload.json(), TagsOpenMsg.class);
         if (msg != null && msg.getEntries() != null) {
            context.client().setScreen(new TagsScreen(msg));
         } else {
            CobbleClubClient.LOGGER.warn("Dropped malformed tags open payload");
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(Payloads.TagsState.ID, (payload, context) -> {
         TagsStateMsg msg = TagsProtocol.INSTANCE.decode(payload.json(), TagsStateMsg.class);
         if (msg != null && msg.getEntries() != null) {
            Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof TagsScreen screen) screen.applyState(msg);
         } else {
            CobbleClubClient.LOGGER.warn("Dropped malformed tags state payload");
         }
      });
   }

   public static void sendSet(String tagId) {
      send(new TagsActionMsg(1, TagsActionType.SET, tagId));
   }

   public static void sendUnset() {
      send(new TagsActionMsg(1, TagsActionType.UNSET, null));
   }

   private static void send(TagsActionMsg msg) {
      if (ClientPlayNetworking.canSend(Payloads.TagsAction.ID)) {
         ClientPlayNetworking.send(new Payloads.TagsAction(TagsProtocol.INSTANCE.encode(msg)));
      }
   }
}
