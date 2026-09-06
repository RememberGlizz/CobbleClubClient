package com.cobbleclub.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClaimsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<ClaimData> CLAIMS = new ArrayList<>();
    private static final Map<String, Map<Long, ClaimData>> CHUNK_INDEX = new LinkedHashMap<>();
    private static final Map<String, ClaimData> ID_INDEX = new LinkedHashMap<>();
    private static Path path;

    private ClaimsStore() {}

    public static void load(MinecraftServer server) {
        path = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("claims.json");
        CLAIMS.clear();
        try {
            loadFrom(path);
        } catch (Exception primaryFailure) {
            try {
                loadFrom(path.resolveSibling("claims.json.bak"));
            } catch (Exception ignored) {
                CLAIMS.clear();
            }
        }
        reindex();
    }

    public static List<ClaimData> all() {
        return CLAIMS;
    }

    public static void save() {
        reindex();
        if (path == null) return;
        try {
            Files.createDirectories(path.getParent());
            StoreFile file = new StoreFile();
            file.claims = new ArrayList<>(CLAIMS);
            String json = GSON.toJson(file);
            Path temp = path.resolveSibling("claims.json.tmp");
            Path backup = path.resolveSibling("claims.json.bak");
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            if (Files.exists(path)) Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }



    private static void loadFrom(Path source) throws IOException {
        if (source == null || !Files.exists(source)) return;
        StoreFile file = GSON.fromJson(Files.readString(source, StandardCharsets.UTF_8), StoreFile.class);
        if (file == null || file.claims == null) return;
        CLAIMS.clear();
        for (ClaimData claim : file.claims) {
            if (claim == null) continue;
            claim.normalize();
            CLAIMS.add(claim);
        }
    }
    public static ClaimData findAt(String dimension, int blockX, int blockZ) {
        if (dimension == null) return null;
        Map<Long, ClaimData> dimensionIndex = CHUNK_INDEX.get(dimension);
        return dimensionIndex == null ? null : dimensionIndex.get(chunkKey(blockX >> 4, blockZ >> 4));
    }

    public static ClaimData findById(String id) {
        return id == null ? null : ID_INDEX.get(id);
    }

    public static void reindex() {
        CHUNK_INDEX.clear();
        ID_INDEX.clear();
        for (ClaimData claim : CLAIMS) {
            if (claim == null || claim.dimension == null) continue;
            if (claim.id != null) ID_INDEX.put(claim.id, claim);
            Map<Long, ClaimData> dimensionIndex = CHUNK_INDEX.computeIfAbsent(claim.dimension, ignored -> new LinkedHashMap<>());
            for (int cx = claim.minCx; cx <= claim.maxCx; cx++) {
                for (int cz = claim.minCz; cz <= claim.maxCz; cz++) {
                    dimensionIndex.put(chunkKey(cx, cz), claim);
                }
            }
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
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
        public Map<String, MemberData> trusted = new LinkedHashMap<>();
        public Map<String, MemberData> banned = new LinkedHashMap<>();
        public Map<String, String> permissions = new LinkedHashMap<>();
        public List<SubClaimData> subClaims = new ArrayList<>();
        public String enterTitle;
        public String enterSubtitle;
        public String leaveTitle;
        public String leaveSubtitle;

        public void normalize() {
            if (trusted == null) trusted = new LinkedHashMap<>();
            if (banned == null) banned = new LinkedHashMap<>();
            if (permissions == null) permissions = new LinkedHashMap<>();
            if (subClaims == null) subClaims = new ArrayList<>();
            for (SubClaimData subClaim : subClaims) subClaim.normalize();
        }

        public int area() {
            return (maxCx - minCx + 1) * 16 * (maxCz - minCz + 1) * 16;
        }

        public boolean containsBlock(int x, int z) {
            return x >= (minCx << 4) && x <= (maxCx << 4) + 15 && z >= (minCz << 4) && z <= (maxCz << 4) + 15;
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
        public Map<String, String> permissionOverrides = new LinkedHashMap<>();

        public void normalize() {
            if (permissionOverrides == null) permissionOverrides = new LinkedHashMap<>();
            if (minX > maxX) { int value = minX; minX = maxX; maxX = value; }
            if (minY > maxY) { int value = minY; minY = maxY; maxY = value; }
            if (minZ > maxZ) { int value = minZ; minZ = maxZ; maxZ = value; }
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }

    public static final class MemberData {
        public String uuid;
        public String name;
        public long joinedAt;

        public MemberData() {}

        public MemberData(String uuid, String name, long joinedAt) {
            this.uuid = uuid;
            this.name = name;
            this.joinedAt = joinedAt;
        }
    }

    private static final class StoreFile {
        List<ClaimData> claims = new ArrayList<>();
    }
}
