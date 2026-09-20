/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2540
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9139
 */
package com.cobbleclub.client.wardrobe.net;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;

@Environment(value=EnvType.CLIENT)
public record WardrobeOpenPayload(String json) implements CustomPayload
{
    public static final CustomPayload.Id<WardrobeOpenPayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"wardrobe_open/v2"));
    public static final PacketCodec<PacketByteBuf, WardrobeOpenPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeByteArray(WardrobeProtocol.compress((String)payload.json)), buf -> new WardrobeOpenPayload(WardrobeProtocol.decompress((byte[])buf.readByteArray(262144))));

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}

