package com.cobbleclub.client.claims;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.claims.net.ClaimsActionPayload;
import com.cobbleclub.client.claims.net.ClaimsMapRequestPayload;
import com.cobbleclub.client.claims.net.ClaimsMapTilesPayload;
import com.cobbleclub.client.claims.net.ClaimsOpenPayload;
import com.cobbleclub.client.claims.net.ClaimsStatePayload;
import com.cobbleclub.client.claims.net.ClaimsWorldPayload;
import com.cobbleclub.client.claims.world.ClaimWorldRenderer;
import com.cobbleclub.clubhouse.claims.protocol.ChunkRect;
import com.cobbleclub.clubhouse.claims.protocol.ClaimMessagesEdit;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsActionMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsActionType;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsMapRequestMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsMapTilesMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsOpenMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsStateMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsWorldMsg;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class ClaimsNetworking {
   private static final AtomicInteger NONCE = new AtomicInteger();

   private ClaimsNetworking() {
   }

   public static void init() {
      PayloadTypeRegistry.playS2C().register(ClaimsOpenPayload.TYPE, ClaimsOpenPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(ClaimsStatePayload.TYPE, ClaimsStatePayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(ClaimsMapTilesPayload.TYPE, ClaimsMapTilesPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(ClaimsWorldPayload.TYPE, ClaimsWorldPayload.STREAM_CODEC);
      PayloadTypeRegistry.playC2S().register(ClaimsActionPayload.TYPE, ClaimsActionPayload.STREAM_CODEC);
      PayloadTypeRegistry.playC2S().register(ClaimsMapRequestPayload.TYPE, ClaimsMapRequestPayload.STREAM_CODEC);
      ClientPlayNetworking.registerGlobalReceiver(ClaimsOpenPayload.TYPE, (payload, context) -> {
         ClaimsOpenMsg msg = (ClaimsOpenMsg)ClaimsScreenProtocol.INSTANCE.decode(payload.json(), ClaimsOpenMsg.class);
         if (msg != null && msg.getMyClaims() != null && msg.getPermissionCatalog() != null) {
            context.client().setScreen(new ClaimsScreen(new ClaimsState(msg)));
         } else {
            CobbleClubClient.LOGGER.warn("Dropped malformed claims open payload");
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(ClaimsStatePayload.TYPE, (payload, context) -> {
         ClaimsStateMsg msg = (ClaimsStateMsg)ClaimsScreenProtocol.INSTANCE.decode(payload.json(), ClaimsStateMsg.class);
         if (msg != null && msg.getMyClaims() != null) {
            Screen patt0$temp = MinecraftClient.getInstance().currentScreen;
            if (patt0$temp instanceof ClaimsScreen) {
               ClaimsScreen screen = (ClaimsScreen)patt0$temp;
               screen.applyState(msg);
            }

         } else {
            CobbleClubClient.LOGGER.warn("Dropped malformed claims state payload");
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(ClaimsMapTilesPayload.TYPE, (payload, context) -> {
         ClaimsMapTilesMsg msg = (ClaimsMapTilesMsg)ClaimsScreenProtocol.INSTANCE.decode(payload.json(), ClaimsMapTilesMsg.class);
         if (msg != null && msg.getTiles() != null) {
            Screen patt0$temp = MinecraftClient.getInstance().currentScreen;
            if (patt0$temp instanceof ClaimsScreen) {
               ClaimsScreen screen = (ClaimsScreen)patt0$temp;
               screen.acceptTiles(msg);
            }

         } else {
            CobbleClubClient.LOGGER.warn("Dropped malformed claims map tiles payload");
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(ClaimsWorldPayload.TYPE, (payload, context) -> {
         ClaimsWorldMsg msg = (ClaimsWorldMsg)ClaimsScreenProtocol.INSTANCE.decode(payload.json(), ClaimsWorldMsg.class);
         if (msg != null && msg.getGroups() != null) {
            ClaimWorldRenderer.accept(msg);
         }
      });
      ClaimWorldRenderer.init();
   }

   public static void sendMapRequest(String dimension, List<List<Integer>> chunks) {
      if (ClientPlayNetworking.canSend(ClaimsMapRequestPayload.TYPE)) {
         ClaimsMapRequestMsg msg = new ClaimsMapRequestMsg(1, dimension, chunks);
         ClientPlayNetworking.send(new ClaimsMapRequestPayload(ClaimsScreenProtocol.INSTANCE.encode(msg)));
      }
   }

   public static int sendCreate(ChunkRect rect) {
      return send(action(ClaimsActionType.CREATE_FROM_CHUNKS).rect(rect));
   }

   public static int sendResize(String claimId, ChunkRect rect) {
      return send(action(ClaimsActionType.RESIZE_TO_CHUNKS).claimId(claimId).rect(rect));
   }

   public static int sendDelete(String claimId) {
      return send(action(ClaimsActionType.DELETE).claimId(claimId));
   }

   public static int sendRename(String claimId, String name) {
      return send(action(ClaimsActionType.RENAME).claimId(claimId).name(name));
   }

   public static int sendSetPermission(String claimId, String subId, String permission, String role) {
      return send(action(ClaimsActionType.SET_PERMISSION_ROLE).claimId(claimId).subId(subId).permission(permission).role(role));
   }

   public static int sendTrust(String claimId, String playerName) {
      return send(action(ClaimsActionType.TRUST).claimId(claimId).name(playerName));
   }

   public static int sendUntrust(String claimId, String targetUuid, String knownName) {
      return send(action(ClaimsActionType.UNTRUST).claimId(claimId).targetUuid(targetUuid).name(knownName));
   }

   public static int sendBan(String claimId, String playerName) {
      return send(action(ClaimsActionType.BAN).claimId(claimId).name(playerName));
   }

   public static int sendUnban(String claimId, String targetUuid, String knownName) {
      return send(action(ClaimsActionType.UNBAN).claimId(claimId).targetUuid(targetUuid).name(knownName));
   }

   public static int sendTransfer(String claimId, String playerName) {
      return send(action(ClaimsActionType.TRANSFER).claimId(claimId).name(playerName));
   }

   public static int sendSetMessages(String claimId, ClaimMessagesEdit edit) {
      return send(action(ClaimsActionType.SET_MESSAGES).claimId(claimId).messages(edit));
   }

   public static int sendRenameSub(String claimId, String subId, String name) {
      return send(action(ClaimsActionType.RENAME_SUB).claimId(claimId).subId(subId).name(name));
   }

   public static int sendDeleteSub(String claimId, String subId) {
      return send(action(ClaimsActionType.DELETE_SUB).claimId(claimId).subId(subId));
   }

   public static void sendBuyBlocks(String tab) {
      send(action(ClaimsActionType.BUY_BLOCKS).tab(tab));
   }

   public static void sendTeleport(String claimId) {
      send(action(ClaimsActionType.TELEPORT).claimId(claimId));
   }

   public static void sendScreenClosed() {
      send(action(ClaimsActionType.SCREEN_CLOSED));
   }

   private static ActionBuilder action(ClaimsActionType type) {
      return new ActionBuilder(type);
   }

   private static int send(ActionBuilder builder) {
      if (!ClientPlayNetworking.canSend(ClaimsActionPayload.TYPE)) {
         return -1;
      } else {
         int nonce = NONCE.incrementAndGet();
         ClaimsActionMsg msg = new ClaimsActionMsg(1, builder.type, nonce, builder.claimId, builder.rect, builder.name, builder.targetUuid, builder.permission, builder.role, builder.subId, builder.messages, builder.tab);
         ClientPlayNetworking.send(new ClaimsActionPayload(ClaimsScreenProtocol.INSTANCE.encode(msg)));
         return nonce;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class ActionBuilder {
      final ClaimsActionType type;
      String claimId;
      ChunkRect rect;
      String name;
      String targetUuid;
      String permission;
      String role;
      String subId;
      ClaimMessagesEdit messages;
      String tab;

      ActionBuilder(ClaimsActionType type) {
         this.type = type;
      }

      ActionBuilder claimId(String v) {
         this.claimId = v;
         return this;
      }

      ActionBuilder rect(ChunkRect v) {
         this.rect = v;
         return this;
      }

      ActionBuilder name(String v) {
         this.name = v;
         return this;
      }

      ActionBuilder targetUuid(String v) {
         this.targetUuid = v;
         return this;
      }

      ActionBuilder permission(String v) {
         this.permission = v;
         return this;
      }

      ActionBuilder role(String v) {
         this.role = v;
         return this;
      }

      ActionBuilder subId(String v) {
         this.subId = v;
         return this;
      }

      ActionBuilder messages(ClaimMessagesEdit v) {
         this.messages = v;
         return this;
      }

      ActionBuilder tab(String v) {
         this.tab = v;
         return this;
      }
   }
}
