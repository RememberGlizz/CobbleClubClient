/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.class_1799
 *  net.minecraft.class_2338
 *  net.minecraft.class_2487
 *  net.minecraft.class_2520
 *  net.minecraft.class_2522
 *  net.minecraft.class_3218
 *  net.minecraft.class_5218
 *  net.minecraft.class_7225$class_7874
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.chestshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;

public final class ChestShopStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, ShopData> SHOPS = new LinkedHashMap<>();
    private static Path path;

    private ChestShopStore() {
    }

    public static void load(MinecraftServer server) {
        path = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("chest-shops.json");
        SHOPS.clear();
        try {
            StoreFile file;
            if (Files.exists(path)
                    && (file = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), StoreFile.class)) != null
                    && file.shops != null) {
                for (ShopData shop : file.shops) {
                    if (shop == null) continue;
                    shop.normalize();
                    if (shop.id.isBlank()) continue;
                    SHOPS.put(shop.id, shop);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            StoreFile file = new StoreFile();
            file.shops = new ArrayList<>(SHOPS.values());
            Files.writeString(path, GSON.toJson(file), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static Collection<ShopData> all() {
        return SHOPS.values();
    }

    public static ShopData get(String id) {
        return id == null ? null : SHOPS.get(id);
    }

    public static void put(ShopData shop) {
        if (shop == null) {
            return;
        }
        shop.normalize();
        SHOPS.put(shop.id, shop);
        ChestShopStore.save();
    }

    public static ShopData remove(String id) {
        ShopData removed = id == null ? null : SHOPS.remove(id);
        if (removed != null) {
            ChestShopStore.save();
        }
        return removed;
    }

    public static int removeDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return 0;
        }
        int before = SHOPS.size();
        SHOPS.values().removeIf(shop -> shop != null && dimension.equals(shop.dimension));
        int removed = before - SHOPS.size();
        if (removed > 0) {
            ChestShopStore.save();
        }
        return removed;
    }

    public static ShopData create(UUID owner, String ownerName, ServerWorld world, BlockPos chest, BlockPos sign) {
        ShopData shop = new ShopData();
        shop.id = UUID.randomUUID().toString();
        shop.ownerUuid = owner.toString();
        shop.ownerName = ownerName == null ? "Unknown" : ownerName;
        shop.dimension = world.getRegistryKey().getValue().toString();
        shop.chestX = chest.getX();
        shop.chestY = chest.getY();
        shop.chestZ = chest.getZ();
        shop.signX = sign.getX();
        shop.signY = sign.getY();
        shop.signZ = sign.getZ();
        shop.createdAt = System.currentTimeMillis();
        return shop;
    }

    public static ServerWorld world(MinecraftServer server, ShopData shop) {
        if (server == null || shop == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (!world.getRegistryKey().getValue().toString().equals(shop.dimension)) continue;
            return world;
        }
        return null;
    }

    public static ItemStack template(MinecraftServer server, ShopData shop) {
        if (server == null || shop == null || shop.itemNbt == null || shop.itemNbt.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            NbtElement nbt = StringNbtReader.parse(shop.itemNbt);
            Optional<ItemStack> stack = ItemStack.fromNbt(server.getRegistryManager(), nbt);
            return stack.orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static void setTemplate(MinecraftServer server, ShopData shop, ItemStack source) {
        if (server == null || shop == null || source == null || source.isEmpty()) {
            return;
        }
        ItemStack one = source.copyWithCount(1);
        shop.itemNbt = one.encode(server.getRegistryManager()).toString();
    }

    private static final class StoreFile {
        ArrayList<ShopData> shops = new ArrayList<>();

        private StoreFile() {
        }
    }

    public static final class ShopData {
        public String id = "";
        public String ownerUuid = "";
        public String ownerName = "Unknown";
        public String dimension = "minecraft:overworld";
        public int chestX;
        public int chestY;
        public int chestZ;
        public int signX;
        public int signY;
        public int signZ;
        public String itemNbt = "";
        public long unitPrice;
        public long createdAt;

        public void normalize() {
            if (this.id == null) {
                this.id = "";
            }
            if (this.ownerUuid == null) {
                this.ownerUuid = "";
            }
            if (this.ownerName == null || this.ownerName.isBlank()) {
                this.ownerName = "Unknown";
            }
            if (this.dimension == null || this.dimension.isBlank()) {
                this.dimension = "minecraft:overworld";
            }
            if (this.itemNbt == null) {
                this.itemNbt = "";
            }
            this.unitPrice = Math.max(0L, this.unitPrice);
        }

        public UUID ownerUuid() {
            try {
                return UUID.fromString(this.ownerUuid);
            } catch (Exception ignored) {
                return new UUID(0L, 0L);
            }
        }

        public BlockPos chestPos() {
            return new BlockPos(this.chestX, this.chestY, this.chestZ);
        }

        public BlockPos signPos() {
            return new BlockPos(this.signX, this.signY, this.signZ);
        }
    }
}
