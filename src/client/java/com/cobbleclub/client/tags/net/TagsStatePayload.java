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
public record TagsStatePayload(String json) implements CustomPayload
{
    public static final CustomPayload.Id<TagsStatePayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"tags_state/v1"));
    public static final PacketCodec<PacketByteBuf, TagsStatePayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeString(payload.json, 262144), buf -> new TagsStatePayload(buf.readString(262144)));

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}

