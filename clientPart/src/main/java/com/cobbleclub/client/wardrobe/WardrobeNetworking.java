package com.cobbleclub.client.wardrobe;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.wardrobe.net.WardrobeActionPayload;
import com.cobbleclub.client.wardrobe.net.WardrobeOpenPayload;
import com.cobbleclub.client.wardrobe.net.WardrobeStatePayload;
import com.cobbleclub.client.wardrobe.net.CosmeticStatePayload;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionType;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeOpenMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeStateMsg;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.class_310;
import net.minecraft.class_437;

@Environment(EnvType.CLIENT)
public final class WardrobeNetworking {
   private WardrobeNetworking() {
   }

   public static void init() {
      PayloadTypeRegistry.playS2C().register(WardrobeOpenPayload.TYPE, WardrobeOpenPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(WardrobeStatePayload.TYPE, WardrobeStatePayload.STREAM_CODEC);
      PayloadTypeRegistry.playC2S().register(WardrobeActionPayload.TYPE, WardrobeActionPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(CosmeticStatePayload.TYPE, CosmeticStatePayload.STREAM_CODEC);
      ClientPlayNetworking.registerGlobalReceiver(CosmeticStatePayload.TYPE, (payload, context) ->
            CosmeticRenderState.apply(payload.json()));
      ClientPlayNetworking.registerGlobalReceiver(WardrobeOpenPayload.TYPE, (payload, context) -> {
         WardrobeOpenMsg msg = (WardrobeOpenMsg)WardrobeProtocol.INSTANCE.decode(payload.json(), WardrobeOpenMsg.class);
         if (msg == null) {
            CobbleClubClient.LOGGER.warn("Dropped malformed wardrobe open payload");
         } else {
            WardrobeState.get().applyOpen(msg);
            context.client().method_1507(new WardrobeScreen(msg.getInitialSlot()));
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(WardrobeStatePayload.TYPE, (payload, context) -> {
         WardrobeStateMsg msg = (WardrobeStateMsg)WardrobeProtocol.INSTANCE.decode(payload.json(), WardrobeStateMsg.class);
         if (msg == null) {
            CobbleClubClient.LOGGER.warn("Dropped malformed wardrobe state payload");
         } else if (WardrobeState.get().applyState(msg)) {
            class_437 patt0$temp = class_310.method_1551().field_1755;
            if (patt0$temp instanceof WardrobeScreen) {
               WardrobeScreen screen = (WardrobeScreen)patt0$temp;
               screen.onStateRefreshed();
            }

         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayConnectionEvents.Disconnect)(handler, client) -> {
         WardrobeState.get().reset();
         CosmeticRenderState.clear();
      });
   }

   public static void sendEquip(String cosmeticId) {
      send(new WardrobeActionMsg(2, WardrobeActionType.EQUIP, cosmeticId, (WardrobeSlot)null, (Integer)null, (Boolean)null, (String)null, (Integer)null));
   }

   public static void sendUnequip(WardrobeSlot slot) {
      send(new WardrobeActionMsg(2, WardrobeActionType.UNEQUIP, (String)null, slot, (Integer)null, (Boolean)null, (String)null, (Integer)null));
   }

   public static void sendUnequipAll() {
      send(new WardrobeActionMsg(2, WardrobeActionType.UNEQUIP_ALL, (String)null, (WardrobeSlot)null, (Integer)null, (Boolean)null, (String)null, (Integer)null));
   }

   public static void sendSetColor(String cosmeticId, int rgb) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SET_COLOR, cosmeticId, (WardrobeSlot)null, rgb & 16777215, (Boolean)null, (String)null, (Integer)null));
   }

   public static void sendSetHidden(boolean hidden) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SET_HIDDEN, (String)null, (WardrobeSlot)null, (Integer)null, hidden, (String)null, (Integer)null));
   }

   public static void sendSetGlow(String glowId) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SET_GLOW, (String)null, (WardrobeSlot)null, (Integer)null, (Boolean)null, glowId, (Integer)null));
   }

   public static void sendLoadPreset(int index) {
      send(new WardrobeActionMsg(2, WardrobeActionType.LOAD_PRESET, (String)null, (WardrobeSlot)null, (Integer)null, (Boolean)null, (String)null, index));
   }

   public static void sendSavePreset(int index) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SAVE_PRESET, (String)null, (WardrobeSlot)null, (Integer)null, (Boolean)null, (String)null, index));
   }

   public static void sendDeletePreset(int index) {
      send(new WardrobeActionMsg(2, WardrobeActionType.DELETE_PRESET, (String)null, (WardrobeSlot)null, (Integer)null, (Boolean)null, (String)null, index));
   }

   private static void send(WardrobeActionMsg msg) {
      if (ClientPlayNetworking.canSend(WardrobeActionPayload.TYPE)) {
         ClientPlayNetworking.send(new WardrobeActionPayload(WardrobeProtocol.INSTANCE.encode(msg)));
      }
   }
}
