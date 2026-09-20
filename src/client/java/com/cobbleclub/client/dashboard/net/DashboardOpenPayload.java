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
package com.cobbleclub.client.dashboard.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;

@Environment(value=EnvType.CLIENT)
public record DashboardOpenPayload(String json) implements CustomPayload
{
    public static final CustomPayload.Id<DashboardOpenPayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"dashboard_open/v1"));
    public static final PacketCodec<PacketByteBuf, DashboardOpenPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> buf.writeString(payload.json, 32768), buf -> new DashboardOpenPayload(buf.readString(32768)));

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}

