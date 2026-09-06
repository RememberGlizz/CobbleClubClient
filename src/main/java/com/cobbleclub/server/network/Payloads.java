package com.cobbleclub.server.network;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class Payloads {
    private Payloads() {}

    private static Identifier id(String path) {
        return Identifier.of("cobbleclub", path);
    }

    public record Handshake(String version) implements CustomPayload {
        public static final Id<Handshake> ID = new Id<>(id("handshake/v1"));
        public static final PacketCodec<RegistryByteBuf, Handshake> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.version),
                buf -> new Handshake(buf.readString(32767))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record DashboardOpen(String json) implements CustomPayload {
        public static final Id<DashboardOpen> ID = new Id<>(id("dashboard_open/v1"));
        public static final PacketCodec<RegistryByteBuf, DashboardOpen> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.json, 32768),
                buf -> new DashboardOpen(buf.readString(32768))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record DashboardState(String json) implements CustomPayload {
        public static final Id<DashboardState> ID = new Id<>(id("dashboard_state/v1"));
        public static final PacketCodec<RegistryByteBuf, DashboardState> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.json, 32768),
                buf -> new DashboardState(buf.readString(32768))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record DashboardAction(String action) implements CustomPayload {
        public static final Id<DashboardAction> ID = new Id<>(id("dashboard_action/v1"));
        public static final PacketCodec<RegistryByteBuf, DashboardAction> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.action, 64),
                buf -> new DashboardAction(buf.readString(64))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record KitsOpen(String json) implements CustomPayload {
        public static final Id<KitsOpen> ID = new Id<>(id("kits_open/v1"));
        public static final PacketCodec<RegistryByteBuf, KitsOpen> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.json, 32768),
                buf -> new KitsOpen(buf.readString(32768))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record KitsState(String json) implements CustomPayload {
        public static final Id<KitsState> ID = new Id<>(id("kits_state/v1"));
        public static final PacketCodec<RegistryByteBuf, KitsState> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.json, 32768),
                buf -> new KitsState(buf.readString(32768))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record KitsAction(String action) implements CustomPayload {
        public static final Id<KitsAction> ID = new Id<>(id("kits_action/v1"));
        public static final PacketCodec<RegistryByteBuf, KitsAction> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.action, 64),
                buf -> new KitsAction(buf.readString(64))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    private static void writeClaimsJson(String json, RegistryByteBuf buf) {
        buf.writeByteArray(ClaimsScreenProtocol.compress(json));
    }

    private static String readClaimsJson(RegistryByteBuf buf, int maxBytes) {
        return ClaimsScreenProtocol.decompress(buf.readByteArray(maxBytes));
    }

    public record ClaimsOpen(String json) implements CustomPayload {
        public static final Id<ClaimsOpen> ID = new Id<>(id("claims_open/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsOpen> CODEC = PacketCodec.of(
                (value, buf) -> writeClaimsJson(value.json, buf),
                buf -> new ClaimsOpen(readClaimsJson(buf, 262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ClaimsState(String json) implements CustomPayload {
        public static final Id<ClaimsState> ID = new Id<>(id("claims_state/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsState> CODEC = PacketCodec.of(
                (value, buf) -> writeClaimsJson(value.json, buf),
                buf -> new ClaimsState(readClaimsJson(buf, 262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ClaimsMapTiles(String json) implements CustomPayload {
        public static final Id<ClaimsMapTiles> ID = new Id<>(id("claims_map_tiles/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsMapTiles> CODEC = PacketCodec.of(
                (value, buf) -> writeClaimsJson(value.json, buf),
                buf -> new ClaimsMapTiles(readClaimsJson(buf, 262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ClaimsWorld(String json) implements CustomPayload {
        public static final Id<ClaimsWorld> ID = new Id<>(id("claims_world/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsWorld> CODEC = PacketCodec.of(
                (value, buf) -> writeClaimsJson(value.json, buf),
                buf -> new ClaimsWorld(readClaimsJson(buf, 32768))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ClaimsAction(String json) implements CustomPayload {
        public static final Id<ClaimsAction> ID = new Id<>(id("claims_action/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsAction> CODEC = PacketCodec.of(
                (value, buf) -> writeClaimsJson(value.json, buf),
                buf -> new ClaimsAction(readClaimsJson(buf, 8192))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ClaimsMapRequest(String json) implements CustomPayload {
        public static final Id<ClaimsMapRequest> ID = new Id<>(id("claims_map_request/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsMapRequest> CODEC = PacketCodec.of(
                (value, buf) -> writeClaimsJson(value.json, buf),
                buf -> new ClaimsMapRequest(readClaimsJson(buf, 8192))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    private static void writeJson(String json, int max, RegistryByteBuf buf) {
        buf.writeString(json, max);
    }

    public record TagsOpen(String json) implements CustomPayload {
        public static final Id<TagsOpen> ID = new Id<>(id("tags_open/v1"));
        public static final PacketCodec<RegistryByteBuf, TagsOpen> CODEC = PacketCodec.of(
                (value, buf) -> writeJson(value.json, 262144, buf),
                buf -> new TagsOpen(buf.readString(262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record TagsState(String json) implements CustomPayload {
        public static final Id<TagsState> ID = new Id<>(id("tags_state/v1"));
        public static final PacketCodec<RegistryByteBuf, TagsState> CODEC = PacketCodec.of(
                (value, buf) -> writeJson(value.json, 262144, buf),
                buf -> new TagsState(buf.readString(262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record TagsAction(String json) implements CustomPayload {
        public static final Id<TagsAction> ID = new Id<>(id("tags_action/v1"));
        public static final PacketCodec<RegistryByteBuf, TagsAction> CODEC = PacketCodec.of(
                (value, buf) -> writeJson(value.json, 4096, buf),
                buf -> new TagsAction(buf.readString(4096))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    private static void writeWardrobeJson(String json, RegistryByteBuf buf) {
        buf.writeByteArray(WardrobeProtocol.compress(json));
    }

    private static String readWardrobeJson(RegistryByteBuf buf, int maxBytes) {
        return WardrobeProtocol.decompress(buf.readByteArray(maxBytes));
    }

    public record WardrobeOpen(String json) implements CustomPayload {
        public static final Id<WardrobeOpen> ID = new Id<>(id("wardrobe_open/v2"));
        public static final PacketCodec<RegistryByteBuf, WardrobeOpen> CODEC = PacketCodec.of(
                (value, buf) -> writeWardrobeJson(value.json, buf),
                buf -> new WardrobeOpen(readWardrobeJson(buf, 262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record WardrobeState(String json) implements CustomPayload {
        public static final Id<WardrobeState> ID = new Id<>(id("wardrobe_state/v2"));
        public static final PacketCodec<RegistryByteBuf, WardrobeState> CODEC = PacketCodec.of(
                (value, buf) -> writeWardrobeJson(value.json, buf),
                buf -> new WardrobeState(readWardrobeJson(buf, 262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record WardrobeAction(String json) implements CustomPayload {
        public static final Id<WardrobeAction> ID = new Id<>(id("wardrobe_action/v2"));
        public static final PacketCodec<RegistryByteBuf, WardrobeAction> CODEC = PacketCodec.of(
                (value, buf) -> writeWardrobeJson(value.json, buf),
                buf -> new WardrobeAction(readWardrobeJson(buf, 8192))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CosmeticState(String json) implements CustomPayload {
        public static final Id<CosmeticState> ID = new Id<>(id("cosmetic_state/v1"));
        public static final PacketCodec<RegistryByteBuf, CosmeticState> CODEC = PacketCodec.of(
                (value, buf) -> writeJson(value.json, 16384, buf),
                buf -> new CosmeticState(buf.readString(16384))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PokemonSkins(String json) implements CustomPayload {
        public static final Id<PokemonSkins> ID = new Id<>(id("pokemon_skins/v1"));
        public static final PacketCodec<RegistryByteBuf, PokemonSkins> CODEC = PacketCodec.of(
                (value, buf) -> writeJson(value.json, 262144, buf),
                buf -> new PokemonSkins(buf.readString(262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CratePreview(String json) implements CustomPayload {
        public static final Id<CratePreview> ID = new Id<>(id("crate_preview/v1"));
        public static final PacketCodec<RegistryByteBuf, CratePreview> CODEC = PacketCodec.of(
                (value, buf) -> writeJson(value.json, 262144, buf),
                buf -> new CratePreview(buf.readString(262144))
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CrateTestReward(String crateId, int prizeIndex, boolean shiny) implements CustomPayload {
        public static final Id<CrateTestReward> ID = new Id<>(id("crate_test_reward/v1"));
        public static final PacketCodec<RegistryByteBuf, CrateTestReward> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeString(value.crateId, 256);
                    buf.writeInt(value.prizeIndex);
                    buf.writeBoolean(value.shiny);
                },
                buf -> new CrateTestReward(buf.readString(256), buf.readInt(), buf.readBoolean())
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record GearSet(String id, String displayName, List<ItemStack> items) {}

    public record GearCatalog(List<GearSet> sets) implements CustomPayload {
        public static final Id<GearCatalog> ID = new Id<>(id("gear_catalog/v1"));
        public static final PacketCodec<RegistryByteBuf, GearCatalog> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeVarInt(value.sets.size());
                    for (GearSet set : value.sets) {
                        buf.writeString(set.id);
                        buf.writeString(set.displayName);
                        buf.writeVarInt(set.items.size());
                        for (ItemStack item : set.items) ItemStack.PACKET_CODEC.encode(buf, item);
                    }
                },
                buf -> {
                    int count = Math.min(128, Math.max(0, buf.readVarInt()));
                    List<GearSet> sets = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        String setId = buf.readString(32767);
                        String name = buf.readString(32767);
                        int itemCount = Math.min(1024, Math.max(0, buf.readVarInt()));
                        List<ItemStack> items = new ArrayList<>(itemCount);
                        for (int j = 0; j < itemCount; j++) items.add(ItemStack.PACKET_CODEC.decode(buf));
                        sets.add(new GearSet(setId, name, List.copyOf(items)));
                    }
                    return new GearCatalog(List.copyOf(sets));
                }
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
