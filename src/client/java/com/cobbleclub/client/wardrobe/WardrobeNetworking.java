package com.cobbleclub.client.wardrobe;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeActionType;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeOpenMsg;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeStateMsg;
import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class WardrobeNetworking {
   private WardrobeNetworking() {}

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(Payloads.CosmeticState.ID, (payload, context) ->
            CosmeticRenderState.apply(payload.json()));
      ClientPlayNetworking.registerGlobalReceiver(Payloads.WardrobeOpen.ID, (payload, context) -> {
         WardrobeOpenMsg msg = WardrobeProtocol.INSTANCE.decode(payload.json(), WardrobeOpenMsg.class);
         if (msg == null) {
            CobbleClubClient.LOGGER.warn("Dropped malformed wardrobe open payload");
         } else {
            WardrobeState.get().applyOpen(msg);
            context.client().setScreen(new WardrobeScreen(msg.getInitialSlot()));
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(Payloads.WardrobeState.ID, (payload, context) -> {
         WardrobeStateMsg msg = WardrobeProtocol.INSTANCE.decode(payload.json(), WardrobeStateMsg.class);
         if (msg == null) {
            CobbleClubClient.LOGGER.warn("Dropped malformed wardrobe state payload");
         } else if (WardrobeState.get().applyState(msg)) {
            Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof WardrobeScreen screen) screen.onStateRefreshed();
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
         WardrobeState.get().reset();
         CosmeticRenderState.clear();
      });
   }

   public static void sendEquip(String cosmeticId) {
      send(new WardrobeActionMsg(2, WardrobeActionType.EQUIP, cosmeticId, null, null, null, null, null));
   }
   public static void sendUnequip(WardrobeSlot slot) {
      send(new WardrobeActionMsg(2, WardrobeActionType.UNEQUIP, null, slot, null, null, null, null));
   }
   public static void sendUnequipAll() {
      send(new WardrobeActionMsg(2, WardrobeActionType.UNEQUIP_ALL, null, null, null, null, null, null));
   }
   public static void sendSetColor(String cosmeticId, int rgb) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SET_COLOR, cosmeticId, null, rgb & 0xFFFFFF, null, null, null));
   }
   public static void sendSetHidden(boolean hidden) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SET_HIDDEN, null, null, null, hidden, null, null));
   }
   public static void sendSetGlow(String glowId) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SET_GLOW, null, null, null, null, glowId, null));
   }
   public static void sendLoadPreset(int index) {
      send(new WardrobeActionMsg(2, WardrobeActionType.LOAD_PRESET, null, null, null, null, null, index));
   }
   public static void sendSavePreset(int index) {
      send(new WardrobeActionMsg(2, WardrobeActionType.SAVE_PRESET, null, null, null, null, null, index));
   }
   public static void sendDeletePreset(int index) {
      send(new WardrobeActionMsg(2, WardrobeActionType.DELETE_PRESET, null, null, null, null, null, index));
   }

   private static void send(WardrobeActionMsg msg) {
      if (ClientPlayNetworking.canSend(Payloads.WardrobeAction.ID)) {
         ClientPlayNetworking.send(new Payloads.WardrobeAction(WardrobeProtocol.INSTANCE.encode(msg)));
      }
   }
}
