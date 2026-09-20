package com.cobbleclub.server.chestshop;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public final class ChestShopPayloads {
    private ChestShopPayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.of("cobbleclub", path);
    }

    public record CancelSetup() implements CustomPayload {
        public static final CustomPayload.Id<CancelSetup> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_cancel_setup/v1"));
        public static final PacketCodec<RegistryByteBuf, CancelSetup> CODEC =
                PacketCodec.unit(new CancelSetup());

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Close(String message, boolean success) implements CustomPayload {
        public static final CustomPayload.Id<Close> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_close/v1"));
        public static final PacketCodec<RegistryByteBuf, Close> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeString(value.message == null ? "" : value.message, 512);
            buf.writeBoolean(value.success);
        }, buf -> new Close(buf.readString(512), buf.readBoolean()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ShopAction(String shopId, String action, int quantity) implements CustomPayload {
        public static final CustomPayload.Id<ShopAction> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_action/v1"));
        public static final PacketCodec<RegistryByteBuf, ShopAction> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeString(value.shopId == null ? "" : value.shopId, 128);
            buf.writeString(value.action == null ? "" : value.action, 64);
            buf.writeVarInt(Math.max(0, value.quantity));
        }, buf -> new ShopAction(buf.readString(128), buf.readString(64), buf.readVarInt()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ShopOpen(
            String shopId,
            ItemStack item,
            long unitPrice,
            int stock,
            String ownerName,
            String currencySymbol,
            boolean manager,
            String notice,
            boolean error
    ) implements CustomPayload {
        public static final CustomPayload.Id<ShopOpen> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_shop_open/v1"));
        public static final PacketCodec<RegistryByteBuf, ShopOpen> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeString(value.shopId == null ? "" : value.shopId, 128);
            ItemStack.PACKET_CODEC.encode(buf, value.item);
            buf.writeLong(value.unitPrice);
            buf.writeVarInt(Math.max(0, value.stock));
            buf.writeString(value.ownerName == null ? "Unknown" : value.ownerName, 64);
            buf.writeString(value.currencySymbol == null ? "" : value.currencySymbol, 16);
            buf.writeBoolean(value.manager);
            buf.writeString(value.notice == null ? "" : value.notice, 512);
            buf.writeBoolean(value.error);
        }, buf -> new ShopOpen(
                buf.readString(128),
                ItemStack.PACKET_CODEC.decode(buf),
                buf.readLong(),
                buf.readVarInt(),
                buf.readString(64),
                buf.readString(16),
                buf.readBoolean(),
                buf.readString(512),
                buf.readBoolean()
        ));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetPrice(long price) implements CustomPayload {
        public static final CustomPayload.Id<SetPrice> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_set_price/v1"));
        public static final PacketCodec<RegistryByteBuf, SetPrice> CODEC =
                PacketCodec.of((value, buf) -> buf.writeLong(value.price),
                        buf -> new SetPrice(buf.readLong()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PriceOpen(
            ItemStack item,
            long minimumPrice,
            long recommendedPrice,
            String currencySymbol,
            String title
    ) implements CustomPayload {
        public static final CustomPayload.Id<PriceOpen> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_price_open/v1"));
        public static final PacketCodec<RegistryByteBuf, PriceOpen> CODEC = PacketCodec.of((value, buf) -> {
            ItemStack.PACKET_CODEC.encode(buf, value.item);
            buf.writeLong(value.minimumPrice);
            buf.writeLong(value.recommendedPrice);
            buf.writeString(value.currencySymbol == null ? "" : value.currencySymbol, 16);
            buf.writeString(value.title == null ? "Set Shop Price" : value.title, 256);
        }, buf -> new PriceOpen(
                ItemStack.PACKET_CODEC.decode(buf),
                buf.readLong(),
                buf.readLong(),
                buf.readString(16),
                buf.readString(256)
        ));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SelectItem(int slot) implements CustomPayload {
        public static final CustomPayload.Id<SelectItem> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_select_item/v1"));
        public static final PacketCodec<RegistryByteBuf, SelectItem> CODEC =
                PacketCodec.of((value, buf) -> buf.writeVarInt(value.slot),
                        buf -> new SelectItem(buf.readVarInt()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SelectOpen(String title) implements CustomPayload {
        public static final CustomPayload.Id<SelectOpen> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_select_open/v1"));
        public static final PacketCodec<RegistryByteBuf, SelectOpen> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeString(
                                value.title == null ? "Select Shop Item" : value.title, 256),
                        buf -> new SelectOpen(buf.readString(256))
                );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Placement(boolean active, String message) implements CustomPayload {
        public static final CustomPayload.Id<Placement> ID =
                new CustomPayload.Id<>(ChestShopPayloads.id("chestshop_placement/v1"));
        public static final PacketCodec<RegistryByteBuf, Placement> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeBoolean(value.active);
            buf.writeString(value.message == null ? "" : value.message, 512);
        }, buf -> new Placement(buf.readBoolean(), buf.readString(512)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}


