/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.claims.protocol.ChunkRect
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimMessagesEdit
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsActionMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsActionType
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsMapRequestMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsMapTilesMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsOpenMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsStateMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsWorldMsg
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_8710
 */
package com.cobbleclub.client.claims;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.claims.ClaimsScreen;
import com.cobbleclub.client.claims.ClaimsState;
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
import com.cobbleclub.server.network.Payloads;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class ClaimsNetworking {
    private static final AtomicInteger NONCE = new AtomicInteger();
    private static volatile List<PublicWarp> PUBLIC_WARPS = List.of();

    private ClaimsNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ClaimsOpen.ID, (payload, context) -> {
            ClaimsOpenMsg msg = (ClaimsOpenMsg)ClaimsScreenProtocol.INSTANCE.decode(payload.json(), ClaimsOpenMsg.class);
            if (msg != null && msg.getMyClaims() != null && msg.getPermissionCatalog() != null) {
                context.client().setScreen((Screen)new ClaimsScreen(new ClaimsState(msg)));
            } else {
                CobbleClubClient.LOGGER.warn("Dropped malformed claims open payload");
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ClaimsState.ID, (payload, context) -> {
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
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ClaimsMapTiles.ID, (payload, context) -> {
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
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ClaimsWorld.ID, (payload, context) -> {
            ClaimsWorldMsg msg = (ClaimsWorldMsg)ClaimsScreenProtocol.INSTANCE.decode(payload.json(), ClaimsWorldMsg.class);
            if (msg != null && msg.getGroups() != null) {
                ClaimWorldRenderer.accept(msg);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ClaimsWarpState.ID, (payload, context) -> {
            try {
                JsonObject root = JsonParser.parseString(payload.json()).getAsJsonObject();
                JsonArray array = root.has("warps") && root.get("warps").isJsonArray() ? root.getAsJsonArray("warps") : new JsonArray();
                ArrayList<PublicWarp> warps = new ArrayList<>();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject o = element.getAsJsonObject();
                    warps.add(new PublicWarp(
                            string(o, "claimId"),
                            string(o, "name"),
                            string(o, "claimName"),
                            string(o, "owner"),
                            string(o, "world"),
                            o.has("owned") && o.get("owned").getAsBoolean(),
                            o.has("public") && o.get("public").getAsBoolean()
                    ));
                }
                PUBLIC_WARPS = List.copyOf(warps);
            } catch (Exception exception) {
                CobbleClubClient.LOGGER.warn("Dropped malformed claims warp state payload", exception);
            }
        });
        ClaimWorldRenderer.init();
    }

    public static void sendExtra(String action, String claimId, String value) {
        if (ClientPlayNetworking.canSend(Payloads.ClaimsExtraAction.ID)) {
            ClientPlayNetworking.send(new Payloads.ClaimsExtraAction(
                    action == null ? "" : action,
                    claimId == null ? "" : claimId,
                    value == null ? "" : value
            ));
        }
    }

    public static List<PublicWarp> publicWarps() {
        return PUBLIC_WARPS;
    }

    public static PublicWarp warpForClaim(String claimId) {
        if (claimId == null) return null;
        for (PublicWarp warp : PUBLIC_WARPS) {
            if (claimId.equals(warp.claimId())) return warp;
        }
        return null;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    public static void sendMapRequest(String dimension, List<List<Integer>> chunks) {
        if (ClientPlayNetworking.canSend(Payloads.ClaimsMapRequest.ID)) {
            ClaimsMapRequestMsg msg = new ClaimsMapRequestMsg(1, dimension, chunks);
            ClientPlayNetworking.send((CustomPayload)new Payloads.ClaimsMapRequest(ClaimsScreenProtocol.INSTANCE.encode((Object)msg)));
        }
    }

    public static int sendCreate(ChunkRect rect) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.CREATE_FROM_CHUNKS).rect(rect));
    }

    public static int sendResize(String claimId, ChunkRect rect) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.RESIZE_TO_CHUNKS).claimId(claimId).rect(rect));
    }

    public static int sendDelete(String claimId) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.DELETE).claimId(claimId));
    }

    public static int sendRename(String claimId, String name) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.RENAME).claimId(claimId).name(name));
    }

    public static int sendSetPermission(String claimId, String subId, String permission, String role) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.SET_PERMISSION_ROLE).claimId(claimId).subId(subId).permission(permission).role(role));
    }

    public static int sendTrust(String claimId, String playerName) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.TRUST).claimId(claimId).name(playerName));
    }

    public static int sendUntrust(String claimId, String targetUuid, String knownName) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.UNTRUST).claimId(claimId).targetUuid(targetUuid).name(knownName));
    }

    public static int sendBan(String claimId, String playerName) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.BAN).claimId(claimId).name(playerName));
    }

    public static int sendUnban(String claimId, String targetUuid, String knownName) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.UNBAN).claimId(claimId).targetUuid(targetUuid).name(knownName));
    }

    public static int sendTransfer(String claimId, String playerName) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.TRANSFER).claimId(claimId).name(playerName));
    }

    public static int sendSetMessages(String claimId, ClaimMessagesEdit edit) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.SET_MESSAGES).claimId(claimId).messages(edit));
    }

    public static int sendRenameSub(String claimId, String subId, String name) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.RENAME_SUB).claimId(claimId).subId(subId).name(name));
    }

    public static int sendDeleteSub(String claimId, String subId) {
        return ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.DELETE_SUB).claimId(claimId).subId(subId));
    }

    public static void sendBuyBlocks(String tab) {
        ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.BUY_BLOCKS).tab(tab));
    }

    public static void sendTeleport(String claimId) {
        ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.TELEPORT).claimId(claimId));
    }

    public static void sendScreenClosed() {
        ClaimsNetworking.send(ClaimsNetworking.action(ClaimsActionType.SCREEN_CLOSED));
    }

    private static ActionBuilder action(ClaimsActionType type) {
        return new ActionBuilder(type);
    }

    private static int send(ActionBuilder builder) {
        if (!ClientPlayNetworking.canSend(Payloads.ClaimsAction.ID)) {
            return -1;
        }
        int nonce = NONCE.incrementAndGet();
        ClaimsActionMsg msg = new ClaimsActionMsg(1, builder.type, nonce, builder.claimId, builder.rect, builder.name, builder.targetUuid, builder.permission, builder.role, builder.subId, builder.messages, builder.tab);
        ClientPlayNetworking.send((CustomPayload)new Payloads.ClaimsAction(ClaimsScreenProtocol.INSTANCE.encode((Object)msg)));
        return nonce;
    }

    @Environment(value=EnvType.CLIENT)
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
    public record PublicWarp(String claimId, String name, String claimName, String owner, String world, boolean owned, boolean publicEnabled) {
    }
}

