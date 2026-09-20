/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9129
 *  net.minecraft.class_9139
 */
package com.cobbleclub.server.network;

import com.cobbleclub.server.network.SellProtocol;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public final class SellPayloads {
    private SellPayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.of((String)"cobbleclub", (String)path);
    }

    public record Action(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<Action> ID = new CustomPayload.Id(SellPayloads.id("sell_action/v1"));
        public static final PacketCodec<RegistryByteBuf, Action> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.json, 4096), buf -> new Action(buf.readString(4096)));

        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record State(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<State> ID = new CustomPayload.Id(SellPayloads.id("sell_state/v1"));
        public static final PacketCodec<RegistryByteBuf, State> CODEC = PacketCodec.of((value, buf) -> buf.writeByteArray(SellProtocol.compress(value.json)), buf -> new State(SellProtocol.decompress(buf.readByteArray(262144))));

        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Open(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<Open> ID = new CustomPayload.Id(SellPayloads.id("sell_open/v1"));
        public static final PacketCodec<RegistryByteBuf, Open> CODEC = PacketCodec.of((value, buf) -> buf.writeByteArray(SellProtocol.compress(value.json)), buf -> new Open(SellProtocol.decompress(buf.readByteArray(262144))));

        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}

