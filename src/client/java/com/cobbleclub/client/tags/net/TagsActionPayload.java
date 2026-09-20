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
package com.cobbleclub.client.tags.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;

@Environment(value=EnvType.CLIENT)
public record TagsActionPayload(String json) implements CustomPayload
{
    public static final CustomPayload.Id<TagsActionPayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"tags_action/v1"));
    public static final PacketCodec<PacketByteBuf, TagsActionPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeString(payload.json, 4096), buf -> new TagsActionPayload(buf.readString(4096)));

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}

