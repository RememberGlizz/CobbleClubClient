/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.class_2338
 *  net.minecraft.class_5218
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

public final class ClaimsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<ClaimData> CLAIMS = new ArrayList<ClaimData>();
    private static final Map<String, Map<Long, ClaimData>> CHUNK_INDEX = new LinkedHashMap<String, Map<Long, ClaimData>>();
    private static final Map<String, ClaimData> ID_INDEX = new LinkedHashMap<String, ClaimData>();
    private static Path path;

    private ClaimsStore() {
    }

    public static void load(MinecraftServer server) {
        path = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("claims.json");
        CLAIMS.clear();
        try {
            ClaimsStore.loadFrom(path);
        }
        catch (Exception primaryFailure) {
            try {
                ClaimsStore.loadFrom(path.resolveSibling("claims.json.bak"));
            }
            catch (Exception ignored) {
                CLAIMS.clear();
            }
        }
        ClaimsStore.reindex();
    }

    public static List<ClaimData> all() {
        return CLAIMS;
    }

    public static void save() {
        ClaimsStore.reindex();
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            StoreFile file = new StoreFile();
            file.claims = new ArrayList<ClaimData>(CLAIMS);
            String json = GSON.toJson((Object)file);
            Path temp = path.resolveSibling("claims.json.tmp");
            Path backup = path.resolveSibling("claims.json.bak");
            Files.writeString(temp, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
            if (Files.exists(path, new LinkOption[0])) {
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException atomicUnsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static void loadFrom(Path source) throws IOException {
        if (source == null || !Files.exists(source, new LinkOption[0])) {
            return;
        }
        StoreFile file = (StoreFile)GSON.fromJson(Files.readString(source, StandardCharsets.UTF_8), StoreFile.class);
        if (file == null || file.claims == null) {
            return;
        }
        CLAIMS.clear();
        for (ClaimData claim : file.claims) {
            if (claim == null) continue;
            claim.normalize();
            CLAIMS.add(claim);
        }
    }

    public static ClaimData findAt(String dimension, int blockX, int blockZ) {
        if (dimension == null) {
            return null;
        }
        Map<Long, ClaimData> dimensionIndex = CHUNK_INDEX.get(dimension);
        return dimensionIndex == null ? null : dimensionIndex.get(ClaimsStore.chunkKey(blockX >> 4, blockZ >> 4));
    }

    public static ClaimData findById(String id) {
        return id == null ? null : ID_INDEX.get(id);
    }

    public static int removeDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return 0;
        }
        int before = CLAIMS.size();
        CLAIMS.removeIf(claim -> claim != null && dimension.equals(claim.dimension));
        int removed = before - CLAIMS.size();
        if (removed > 0) {
            ClaimsStore.reindex();
            ClaimsStore.save();
        }
        return removed;
    }

    public static void reindex() {
        CHUNK_INDEX.clear();
        ID_INDEX.clear();
        for (ClaimData claim : CLAIMS) {
            if (claim == null || claim.dimension == null) continue;
            if (claim.id != null) {
                ID_INDEX.put(claim.id, claim);
            }
            Map dimensionIndex = CHUNK_INDEX.computeIfAbsent(claim.dimension, ignored -> new LinkedHashMap());
            for (int cx = claim.minCx; cx <= claim.maxCx; ++cx) {
                for (int cz = claim.minCz; cz <= claim.maxCz; ++cz) {
                    dimensionIndex.put(ClaimsStore.chunkKey(cx, cz), claim);
                }
            }
        }
    }

    private static long chunkKey(int cx, int cz) {
        return (long)cx << 32 ^ (long)cz & 0xFFFFFFFFL;
    }

    private static final class StoreFile {
        List<ClaimData> claims = new ArrayList<ClaimData>();

        private StoreFile() {
        }
    }

    public static final class ClaimData {
        public String id;
        public String ownerUuid;
        public String ownerName;
        public String name;
        public String dimension;
        public int minCx;
        public int minCz;
        public int maxCx;
        public int maxCz;
        public long createdAt;
        public Map<String, MemberData> trusted = new LinkedHashMap<String, MemberData>();
        public Map<String, MemberData> banned = new LinkedHashMap<String, MemberData>();
        public Map<String, String> permissions = new LinkedHashMap<String, String>();
        public List<SubClaimData> subClaims = new ArrayList<SubClaimData>();
        public String enterTitle;
        public String enterSubtitle;
        public String leaveTitle;
        public String leaveSubtitle;
        public boolean publicWarp;
        public String warpName;

        public void normalize() {
            if (this.trusted == null) {
                this.trusted = new LinkedHashMap<String, MemberData>();
            }
            if (this.banned == null) {
                this.banned = new LinkedHashMap<String, MemberData>();
            }
            if (this.permissions == null) {
                this.permissions = new LinkedHashMap<String, String>();
            }
            if (this.subClaims == null) {
                this.subClaims = new ArrayList<SubClaimData>();
            }
            if (this.warpName != null) {
                this.warpName = this.warpName.trim();
                if (this.warpName.isEmpty()) this.warpName = null;
            }
            for (SubClaimData subClaim : this.subClaims) {
                subClaim.normalize();
            }
        }

        public int area() {
            return (this.maxCx - this.minCx + 1) * 16 * (this.maxCz - this.minCz + 1) * 16;
        }

        public boolean containsBlock(int x, int z) {
            return x >= this.minCx << 4 && x <= (this.maxCx << 4) + 15 && z >= this.minCz << 4 && z <= (this.maxCz << 4) + 15;
        }
    }

    public static final class MemberData {
        public String uuid;
        public String name;
        public long joinedAt;

        public MemberData() {
        }

        public MemberData(String uuid, String name, long joinedAt) {
            this.uuid = uuid;
            this.name = name;
            this.joinedAt = joinedAt;
        }
    }

    public static final class SubClaimData {
        public String id;
        public String name;
        public int minX;
        public int minY;
        public int minZ;
        public int maxX;
        public int maxY;
        public int maxZ;
        public Map<String, String> permissionOverrides = new LinkedHashMap<String, String>();

        public void normalize() {
            int value;
            if (this.permissionOverrides == null) {
                this.permissionOverrides = new LinkedHashMap<String, String>();
            }
            if (this.minX > this.maxX) {
                value = this.minX;
                this.minX = this.maxX;
                this.maxX = value;
            }
            if (this.minY > this.maxY) {
                value = this.minY;
                this.minY = this.maxY;
                this.maxY = value;
            }
            if (this.minZ > this.maxZ) {
                value = this.minZ;
                this.minZ = this.maxZ;
                this.maxZ = value;
            }
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= this.minX && pos.getX() <= this.maxX && pos.getY() >= this.minY && pos.getY() <= this.maxY && pos.getZ() >= this.minZ && pos.getZ() <= this.maxZ;
        }
    }
}

