/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9129
 *  net.minecraft.class_9135
 *  net.minecraft.class_9139
 */
package com.cobbleclub.client.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.codec.PacketCodec;

@Environment(value=EnvType.CLIENT)
public record ClientModHandshakePayload(String version) implements CustomPayload
{
    public static final CustomPayload.Id<ClientModHandshakePayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"handshake/v1"));
    public static final PacketCodec<RegistryByteBuf, ClientModHandshakePayload> STREAM_CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.STRING, ClientModHandshakePayload::version, ClientModHandshakePayload::new);

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}

