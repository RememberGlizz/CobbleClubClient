/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2540
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9139
 */
package com.cobbleclub.client.wardrobe.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;

@Environment(value=EnvType.CLIENT)
public record CosmeticStatePayload(String json) implements CustomPayload
{
    public static final CustomPayload.Id<CosmeticStatePayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"cosmetic_state/v1"));
    public static final PacketCodec<PacketByteBuf, CosmeticStatePayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeString(payload.json(), 16384), buf -> new CosmeticStatePayload(buf.readString(16384)));

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}

