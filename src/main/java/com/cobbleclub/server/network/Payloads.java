/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol
 *  net.minecraft.class_1799
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9129
 *  net.minecraft.class_9139
 */
package com.cobbleclub.server.network;

import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeProtocol;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public final class Payloads {
    private Payloads() {
    }

    private static Identifier id(String path) {
        return Identifier.of("cobbleclub", path);
    }

    private static void writeClaimsJson(String json, RegistryByteBuf buf) {
        buf.writeByteArray(ClaimsScreenProtocol.compress(json));
    }

    private static String readClaimsJson(RegistryByteBuf buf, int maxBytes) {
        return ClaimsScreenProtocol.decompress(buf.readByteArray(maxBytes));
    }

    private static void writeJson(String json, int max, RegistryByteBuf buf) {
        buf.writeString(json, max);
    }

    private static void writeWardrobeJson(String json, RegistryByteBuf buf) {
        buf.writeByteArray(WardrobeProtocol.compress(json));
    }

    private static String readWardrobeJson(RegistryByteBuf buf, int maxBytes) {
        return WardrobeProtocol.decompress(buf.readByteArray(maxBytes));
    }

    public record GearCatalog(List<GearSet> sets) implements CustomPayload
    {
        public static final CustomPayload.Id<GearCatalog> ID = new CustomPayload.Id<>(Payloads.id("gear_catalog/v1"));
        public static final PacketCodec<RegistryByteBuf, GearCatalog> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeVarInt(value.sets.size());
            for (GearSet set : value.sets) {
                buf.writeString(set.id);
                buf.writeString(set.displayName);
                buf.writeVarInt(set.items.size());
                for (ItemStack item : set.items) {
                    ItemStack.PACKET_CODEC.encode(buf, item);
                }
            }
        }, buf -> {
            int count = Math.min(128, Math.max(0, buf.readVarInt()));
            ArrayList<GearSet> sets = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                String setId = buf.readString(Short.MAX_VALUE);
                String name = buf.readString(Short.MAX_VALUE);
                int itemCount = Math.min(1024, Math.max(0, buf.readVarInt()));
                ArrayList<ItemStack> items = new ArrayList<>(itemCount);
                for (int j = 0; j < itemCount; ++j) {
                    items.add(ItemStack.PACKET_CODEC.decode(buf));
                }
                sets.add(new GearSet(setId, name, List.copyOf(items)));
            }
            return new GearCatalog(List.copyOf(sets));
        });

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record GearSet(String id, String displayName, List<ItemStack> items) {
    }

    public record ManagedBorderState(String worldId, boolean managed, boolean enabled, double centerX, double centerZ, double size) implements CustomPayload
    {
        public static final CustomPayload.Id<ManagedBorderState> ID = new CustomPayload.Id<>(Payloads.id("managed_border_state/v1"));
        public static final PacketCodec<RegistryByteBuf, ManagedBorderState> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeString(value.worldId, 256);
            buf.writeBoolean(value.managed);
            buf.writeBoolean(value.enabled);
            buf.writeDouble(value.centerX);
            buf.writeDouble(value.centerZ);
            buf.writeDouble(value.size);
        }, buf -> new ManagedBorderState(buf.readString(256), buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record FeatherboardOnline(int online) implements CustomPayload
    {
        public static final CustomPayload.Id<FeatherboardOnline> ID = new CustomPayload.Id<>(Payloads.id("featherboard_online/v1"));
        public static final PacketCodec<RegistryByteBuf, FeatherboardOnline> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeVarInt(Math.max(0, value.online)),
                buf -> new FeatherboardOnline(Math.max(0, buf.readVarInt()))
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record FeatherboardState(
            String playerName,
            String rank,
            String world,
            long balance,
            long gems,
            int claimBlocks,
            long catches,
            long shinies,
            int online,
            float tps,
            float mspt
    ) implements CustomPayload
    {
        public static final CustomPayload.Id<FeatherboardState> ID = new CustomPayload.Id<>(Payloads.id("featherboard_state/v1"));
        public static final PacketCodec<RegistryByteBuf, FeatherboardState> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeString(value.playerName == null ? "" : value.playerName, 64);
            buf.writeString(value.rank == null ? "" : value.rank, 64);
            buf.writeString(value.world == null ? "" : value.world, 256);
            buf.writeLong(value.balance);
            buf.writeLong(value.gems);
            buf.writeInt(value.claimBlocks);
            buf.writeLong(value.catches);
            buf.writeLong(value.shinies);
            buf.writeInt(value.online);
            buf.writeFloat(value.tps);
            buf.writeFloat(value.mspt);
        }, buf -> new FeatherboardState(
                buf.readString(64),
                buf.readString(64),
                buf.readString(256),
                buf.readLong(),
                buf.readLong(),
                buf.readInt(),
                buf.readLong(),
                buf.readLong(),
                buf.readInt(),
                buf.readFloat(),
                buf.readFloat()
        ));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record CrateTestReward(String crateId, int prizeIndex, boolean shiny) implements CustomPayload
    {
        public static final CustomPayload.Id<CrateTestReward> ID = new CustomPayload.Id<>(Payloads.id("crate_test_reward/v1"));
        public static final PacketCodec<RegistryByteBuf, CrateTestReward> CODEC = PacketCodec.of((value, buf) -> {
            buf.writeString(value.crateId, 256);
            buf.writeInt(value.prizeIndex);
            buf.writeBoolean(value.shiny);
        }, buf -> new CrateTestReward(buf.readString(256), buf.readInt(), buf.readBoolean()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record CratePreview(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<CratePreview> ID = new CustomPayload.Id<>(Payloads.id("crate_preview/v1"));
        public static final PacketCodec<RegistryByteBuf, CratePreview> CODEC = PacketCodec.of((value, buf) -> Payloads.writeJson(value.json, 262144, buf), buf -> new CratePreview(buf.readString(262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PokemonSkins(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<PokemonSkins> ID = new CustomPayload.Id<>(Payloads.id("pokemon_skins/v1"));
        public static final PacketCodec<RegistryByteBuf, PokemonSkins> CODEC = PacketCodec.of((value, buf) -> Payloads.writeJson(value.json, 262144, buf), buf -> new PokemonSkins(buf.readString(262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record CosmeticState(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<CosmeticState> ID = new CustomPayload.Id<>(Payloads.id("cosmetic_state/v1"));
        public static final PacketCodec<RegistryByteBuf, CosmeticState> CODEC = PacketCodec.of((value, buf) -> Payloads.writeJson(value.json, 16384, buf), buf -> new CosmeticState(buf.readString(16384)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record WardrobeAction(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<WardrobeAction> ID = new CustomPayload.Id<>(Payloads.id("wardrobe_action/v2"));
        public static final PacketCodec<RegistryByteBuf, WardrobeAction> CODEC = PacketCodec.of((value, buf) -> Payloads.writeWardrobeJson(value.json, buf), buf -> new WardrobeAction(Payloads.readWardrobeJson(buf, 8192)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record WardrobeState(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<WardrobeState> ID = new CustomPayload.Id<>(Payloads.id("wardrobe_state/v2"));
        public static final PacketCodec<RegistryByteBuf, WardrobeState> CODEC = PacketCodec.of((value, buf) -> Payloads.writeWardrobeJson(value.json, buf), buf -> new WardrobeState(Payloads.readWardrobeJson(buf, 262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record WardrobeOpen(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<WardrobeOpen> ID = new CustomPayload.Id<>(Payloads.id("wardrobe_open/v2"));
        public static final PacketCodec<RegistryByteBuf, WardrobeOpen> CODEC = PacketCodec.of((value, buf) -> Payloads.writeWardrobeJson(value.json, buf), buf -> new WardrobeOpen(Payloads.readWardrobeJson(buf, 262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record TagsAction(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<TagsAction> ID = new CustomPayload.Id<>(Payloads.id("tags_action/v1"));
        public static final PacketCodec<RegistryByteBuf, TagsAction> CODEC = PacketCodec.of((value, buf) -> Payloads.writeJson(value.json, 4096, buf), buf -> new TagsAction(buf.readString(4096)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record TagsState(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<TagsState> ID = new CustomPayload.Id<>(Payloads.id("tags_state/v1"));
        public static final PacketCodec<RegistryByteBuf, TagsState> CODEC = PacketCodec.of((value, buf) -> Payloads.writeJson(value.json, 262144, buf), buf -> new TagsState(buf.readString(262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record TagsOpen(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<TagsOpen> ID = new CustomPayload.Id<>(Payloads.id("tags_open/v1"));
        public static final PacketCodec<RegistryByteBuf, TagsOpen> CODEC = PacketCodec.of((value, buf) -> Payloads.writeJson(value.json, 262144, buf), buf -> new TagsOpen(buf.readString(262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsMapRequest(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsMapRequest> ID = new CustomPayload.Id<>(Payloads.id("claims_map_request/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsMapRequest> CODEC = PacketCodec.of((value, buf) -> Payloads.writeClaimsJson(value.json, buf), buf -> new ClaimsMapRequest(Payloads.readClaimsJson(buf, 8192)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsAction(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsAction> ID = new CustomPayload.Id<>(Payloads.id("claims_action/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsAction> CODEC = PacketCodec.of((value, buf) -> Payloads.writeClaimsJson(value.json, buf), buf -> new ClaimsAction(Payloads.readClaimsJson(buf, 8192)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsWorld(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsWorld> ID = new CustomPayload.Id<>(Payloads.id("claims_world/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsWorld> CODEC = PacketCodec.of((value, buf) -> Payloads.writeClaimsJson(value.json, buf), buf -> new ClaimsWorld(Payloads.readClaimsJson(buf, 32768)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsMapTiles(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsMapTiles> ID = new CustomPayload.Id<>(Payloads.id("claims_map_tiles/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsMapTiles> CODEC = PacketCodec.of((value, buf) -> Payloads.writeClaimsJson(value.json, buf), buf -> new ClaimsMapTiles(Payloads.readClaimsJson(buf, 262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsState(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsState> ID = new CustomPayload.Id<>(Payloads.id("claims_state/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsState> CODEC = PacketCodec.of((value, buf) -> Payloads.writeClaimsJson(value.json, buf), buf -> new ClaimsState(Payloads.readClaimsJson(buf, 262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsOpen(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsOpen> ID = new CustomPayload.Id<>(Payloads.id("claims_open/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsOpen> CODEC = PacketCodec.of((value, buf) -> Payloads.writeClaimsJson(value.json, buf), buf -> new ClaimsOpen(Payloads.readClaimsJson(buf, 262144)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsExtraAction(String action, String claimId, String value) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsExtraAction> ID = new CustomPayload.Id<>(Payloads.id("claims_extra_action/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsExtraAction> CODEC = PacketCodec.of((entry, buf) -> {
            buf.writeString(entry.action == null ? "" : entry.action, 64);
            buf.writeString(entry.claimId == null ? "" : entry.claimId, 128);
            buf.writeString(entry.value == null ? "" : entry.value, 256);
        }, buf -> new ClaimsExtraAction(buf.readString(64), buf.readString(128), buf.readString(256)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClaimsWarpState(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<ClaimsWarpState> ID = new CustomPayload.Id<>(Payloads.id("claims_warp_state/v1"));
        public static final PacketCodec<RegistryByteBuf, ClaimsWarpState> CODEC = PacketCodec.of(
                (entry, buf) -> buf.writeString(entry.json == null ? "{}" : entry.json, 65536),
                buf -> new ClaimsWarpState(buf.readString(65536))
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record KitsAction(String action) implements CustomPayload
    {
        public static final CustomPayload.Id<KitsAction> ID = new CustomPayload.Id<>(Payloads.id("kits_action/v1"));
        public static final PacketCodec<RegistryByteBuf, KitsAction> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.action, 64), buf -> new KitsAction(buf.readString(64)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record KitsState(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<KitsState> ID = new CustomPayload.Id<>(Payloads.id("kits_state/v1"));
        public static final PacketCodec<RegistryByteBuf, KitsState> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.json, 32768), buf -> new KitsState(buf.readString(32768)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record KitsOpen(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<KitsOpen> ID = new CustomPayload.Id<>(Payloads.id("kits_open/v1"));
        public static final PacketCodec<RegistryByteBuf, KitsOpen> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.json, 32768), buf -> new KitsOpen(buf.readString(32768)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record DashboardAction(String action) implements CustomPayload
    {
        public static final CustomPayload.Id<DashboardAction> ID = new CustomPayload.Id<>(Payloads.id("dashboard_action/v1"));
        public static final PacketCodec<RegistryByteBuf, DashboardAction> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.action, 64), buf -> new DashboardAction(buf.readString(64)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record DashboardState(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<DashboardState> ID = new CustomPayload.Id<>(Payloads.id("dashboard_state/v1"));
        public static final PacketCodec<RegistryByteBuf, DashboardState> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.json, 32768), buf -> new DashboardState(buf.readString(32768)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record DashboardOpen(String json) implements CustomPayload
    {
        public static final CustomPayload.Id<DashboardOpen> ID = new CustomPayload.Id<>(Payloads.id("dashboard_open/v1"));
        public static final PacketCodec<RegistryByteBuf, DashboardOpen> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.json, 32768), buf -> new DashboardOpen(buf.readString(32768)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Handshake(String version) implements CustomPayload
    {
        public static final CustomPayload.Id<Handshake> ID = new CustomPayload.Id<>(Payloads.id("handshake/v1"));
        public static final PacketCodec<RegistryByteBuf, Handshake> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.version), buf -> new Handshake(buf.readString(Short.MAX_VALUE)));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
