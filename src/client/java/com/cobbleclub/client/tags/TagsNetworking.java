package com.cobbleclub.client.tags;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.tags.net.TagsActionPayload;
import com.cobbleclub.client.tags.net.TagsOpenPayload;
import com.cobbleclub.client.tags.net.TagsStatePayload;
import com.cobbleclub.clubhouse.tags.protocol.TagsActionMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsActionType;
import com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsProtocol;
import com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class TagsNetworking {
   private TagsNetworking() {
   }

   public static void init() {
      PayloadTypeRegistry.playS2C().register(TagsOpenPayload.TYPE, TagsOpenPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(TagsStatePayload.TYPE, TagsStatePayload.STREAM_CODEC);
      PayloadTypeRegistry.playC2S().register(TagsActionPayload.TYPE, TagsActionPayload.STREAM_CODEC);
      ClientPlayNetworking.registerGlobalReceiver(TagsOpenPayload.TYPE, (payload, context) -> {
         TagsOpenMsg msg = (TagsOpenMsg)TagsProtocol.INSTANCE.decode(payload.json(), TagsOpenMsg.class);
         if (msg != null && msg.getEntries() != null) {
            context.client().setScreen(new TagsScreen(msg));
         } else {
            CobbleClubClient.LOGGER.warn("Dropped malformed tags open payload");
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(TagsStatePayload.TYPE, (payload, context) -> {
         TagsStateMsg msg = (TagsStateMsg)TagsProtocol.INSTANCE.decode(payload.json(), TagsStateMsg.class);
         if (msg != null && msg.getEntries() != null) {
            Screen patt0$temp = MinecraftClient.getInstance().currentScreen;
            if (patt0$temp instanceof TagsScreen) {
               TagsScreen screen = (TagsScreen)patt0$temp;
               screen.applyState(msg);
            }

         } else {
            CobbleClubClient.LOGGER.warn("Dropped malformed tags state payload");
         }
      });
   }

   public static void sendSet(String tagId) {
      send(new TagsActionMsg(1, TagsActionType.SET, tagId));
   }

   public static void sendUnset() {
      send(new TagsActionMsg(1, TagsActionType.UNSET, (String)null));
   }

   private static void send(TagsActionMsg msg) {
      if (ClientPlayNetworking.canSend(TagsActionPayload.TYPE)) {
         ClientPlayNetworking.send(new TagsActionPayload(TagsProtocol.INSTANCE.encode(msg)));
      }
   }
}
