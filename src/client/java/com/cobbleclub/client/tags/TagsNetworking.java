/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.tags.protocol.TagsActionMsg
 *  com.cobbleclub.clubhouse.tags.protocol.TagsActionType
 *  com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg
 *  com.cobbleclub.clubhouse.tags.protocol.TagsProtocol
 *  com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_8710
 */
package com.cobbleclub.client.tags;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.tags.TagsScreen;
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
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class TagsNetworking {
    private TagsNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.TagsOpen.ID, (payload, context) -> {
            TagsOpenMsg msg = (TagsOpenMsg)TagsProtocol.INSTANCE.decode(payload.json(), TagsOpenMsg.class);
            if (msg != null && msg.getEntries() != null) {
                context.client().setScreen((Screen)new TagsScreen(msg));
            } else {
                CobbleClubClient.LOGGER.warn("Dropped malformed tags open payload");
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Payloads.TagsState.ID, (payload, context) -> {
            TagsStateMsg msg = (TagsStateMsg)TagsProtocol.INSTANCE.decode(payload.json(), TagsStateMsg.class);
            if (msg != null && msg.getEntries() != null) {
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TagsScreen) {
                    TagsScreen screen = (TagsScreen)current;
                    screen.applyState(msg);
                }
            } else {
                CobbleClubClient.LOGGER.warn("Dropped malformed tags state payload");
            }
        });
    }

    public static void sendSet(String tagId) {
        TagsNetworking.send(new TagsActionMsg(1, TagsActionType.SET, tagId));
    }

    public static void sendUnset() {
        TagsNetworking.send(new TagsActionMsg(1, TagsActionType.UNSET, null));
    }

    private static void send(TagsActionMsg msg) {
        if (ClientPlayNetworking.canSend(Payloads.TagsAction.ID)) {
            ClientPlayNetworking.send((CustomPayload)new Payloads.TagsAction(TagsProtocol.INSTANCE.encode((Object)msg)));
        }
    }
}

