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
package com.cobbleclub.server.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class WildPayloads {
    private WildPayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.of("cobbleclub", path);
    }

    public record Select(String world) implements CustomPayload {
        public static final CustomPayload.Id<Select> ID =
                new CustomPayload.Id<>(WildPayloads.id("wild_select/v5"));

        public static final PacketCodec<RegistryByteBuf, Select> CODEC =
                PacketCodec.of(
                        (value, buf) ->
                                buf.writeString(
                                        value.world == null ? "" : value.world,
                                        64
                                ),
                        buf -> new Select(buf.readString(64))
                );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Open(String subtitle, List<String> worlds) implements CustomPayload {
        public static final CustomPayload.Id<Open> ID =
                new CustomPayload.Id<>(WildPayloads.id("wild_open/v5"));

        public static final PacketCodec<RegistryByteBuf, Open> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                            buf.writeString(
                                    value.subtitle == null ? "" : value.subtitle,
                                    256
                            );

                            List<String> names =
                                    value.worlds == null ? List.of() : value.worlds;

                            int count = Math.min(names.size(), 64);
                            buf.writeVarInt(count);

                            for (int i = 0; i < count; ++i) {
                                String name = names.get(i);
                                buf.writeString(name == null ? "" : name, 64);
                            }
                        },
                        buf -> {
                            String subtitle = buf.readString(256);
                            int count = Math.max(
                                    0,
                                    Math.min(buf.readVarInt(), 64)
                            );

                            ArrayList<String> names = new ArrayList<>(count);

                            for (int i = 0; i < count; ++i) {
                                names.add(buf.readString(64));
                            }

                            return new Open(
                                    subtitle,
                                    List.copyOf(names)
                            );
                        }
                );

        public Open {
            subtitle = subtitle == null ? "" : subtitle;
            worlds = worlds == null ? List.of() : List.copyOf(worlds);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
