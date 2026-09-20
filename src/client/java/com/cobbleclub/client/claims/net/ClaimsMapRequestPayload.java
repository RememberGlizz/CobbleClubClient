/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2540
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9139
 */
package com.cobbleclub.client.claims.net;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;

@Environment(value=EnvType.CLIENT)
public record ClaimsMapRequestPayload(String json) implements CustomPayload
{
    public static final CustomPayload.Id<ClaimsMapRequestPayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"claims_map_request/v1"));
    public static final PacketCodec<PacketByteBuf, ClaimsMapRequestPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeByteArray(ClaimsScreenProtocol.compress((String)payload.json)), buf -> new ClaimsMapRequestPayload(ClaimsScreenProtocol.decompress((byte[])buf.readByteArray(8192))));

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}

