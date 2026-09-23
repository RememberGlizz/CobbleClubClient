package com.cobbleclub.server.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class ContractsPayloads {
    private ContractsPayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.of("cobbleclub", path);
    }

    public record Action(String json) implements CustomPayload {
        public static final Id<Action> ID = new Id<>(id("contracts_action/v1"));
        public static final PacketCodec<RegistryByteBuf, Action> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeString(value.json == null ? "{}" : value.json, 4096),
                        buf -> new Action(buf.readString(4096))
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record State(String json) implements CustomPayload {
        public static final Id<State> ID = new Id<>(id("contracts_state/v1"));
        public static final PacketCodec<RegistryByteBuf, State> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeString(value.json == null ? "{}" : value.json, 65535),
                        buf -> new State(buf.readString(65535))
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Open(String json) implements CustomPayload {
        public static final Id<Open> ID = new Id<>(id("contracts_open/v1"));
        public static final PacketCodec<RegistryByteBuf, Open> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeString(value.json == null ? "{}" : value.json, 65535),
                        buf -> new Open(buf.readString(65535))
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
