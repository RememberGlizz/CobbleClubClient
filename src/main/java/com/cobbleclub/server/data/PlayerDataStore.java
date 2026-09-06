package com.cobbleclub.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, PlayerData> PLAYERS = new HashMap<>();
    private static Path path;

    private PlayerDataStore() {}

    public static void load(MinecraftServer server) {
        path = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("player-data.json");
        PLAYERS.clear();
        try {
            if (Files.exists(path)) {
                StoreFile store = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), StoreFile.class);
                if (store != null && store.players != null) {
                    PLAYERS.putAll(store.players);
                    PLAYERS.values().forEach(PlayerData::normalize);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static PlayerData get(UUID uuid) {
        return PLAYERS.computeIfAbsent(uuid.toString(), ignored -> new PlayerData());
    }

    public static Map<String, PlayerData> all() {
        return PLAYERS;
    }

    public static void save() {
        if (path == null) return;
        try {
            Files.createDirectories(path.getParent());
            StoreFile file = new StoreFile();
            file.players = new LinkedHashMap<>(PLAYERS);
            Files.writeString(path, GSON.toJson(file), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static final class PlayerData {
        public String activeTag;
        public String lastTagRank;
        public long lastTagSwitchMillis;
        public Set<String> ownedTags = new HashSet<>();
        public Map<String, String> equipped = new LinkedHashMap<>();
        public Map<String, Integer> colors = new LinkedHashMap<>();
        public Set<String> ownedCosmetics = new HashSet<>();
        public boolean wardrobeHidden;
        public String glow;
        public Set<String> ownedGlows = new HashSet<>();
        public Map<String, PresetData> presets = new LinkedHashMap<>();
        public Map<String, Integer> crateKeys = new LinkedHashMap<>();
        public Map<String, Integer> crateOpens = new LinkedHashMap<>();
        public String lastKnownName;
        public long balance;
        public long gems;
        public boolean economyInitialized;
        public int bonusClaimBlocks;
        public long rewardedPlayTicks;
        public long moneyRewardedPlayTicks;
        public long lastDailyClaimMillis;
        public long lastDailyClaimEpochDay = Long.MIN_VALUE;
        public boolean newbKitReceived;
        public long lastNewbKitClaimMillis;
        public Map<String, Long> kitClaimTimes = new LinkedHashMap<>();
        public Map<String, Long> kitCooldownReductions = new LinkedHashMap<>();
        public int revision;

        public void normalize() {
            if (ownedTags == null) ownedTags = new HashSet<>();
            if (equipped == null) equipped = new LinkedHashMap<>();
            if (colors == null) colors = new LinkedHashMap<>();
            if (ownedCosmetics == null) ownedCosmetics = new HashSet<>();
            if (ownedGlows == null) ownedGlows = new HashSet<>();
            if (presets == null) presets = new LinkedHashMap<>();
            if (crateKeys == null) crateKeys = new LinkedHashMap<>();
            if (crateOpens == null) crateOpens = new LinkedHashMap<>();
            if (kitClaimTimes == null) kitClaimTimes = new LinkedHashMap<>();
            if (kitCooldownReductions == null) kitCooldownReductions = new LinkedHashMap<>();

            // 3.3 cosmetic id migration: preserve existing purchases/equipment after the
            // user-facing Gengar id changed from gengar_grin_mask to gengar-hat.
            if (ownedCosmetics.remove("gengar_grin_mask")) ownedCosmetics.add("gengar-hat");
            equipped.replaceAll((slot, id) -> "gengar_grin_mask".equals(id) ? "gengar-hat" : id);
            if (colors.containsKey("gengar_grin_mask") && !colors.containsKey("gengar-hat")) {
                colors.put("gengar-hat", colors.remove("gengar_grin_mask"));
            } else {
                colors.remove("gengar_grin_mask");
            }
            for (PresetData preset : presets.values()) {
                if (preset == null) continue;
                if (preset.equipped != null) preset.equipped.replaceAll((slot, id) -> "gengar_grin_mask".equals(id) ? "gengar-hat" : id);
                if (preset.colors != null && preset.colors.containsKey("gengar_grin_mask") && !preset.colors.containsKey("gengar-hat")) {
                    preset.colors.put("gengar-hat", preset.colors.remove("gengar_grin_mask"));
                }
            }
        }
    }

    public static final class PresetData {
        public Map<String, String> equipped = new LinkedHashMap<>();
        public Map<String, Integer> colors = new LinkedHashMap<>();
        public String glow;
    }

    private static final class StoreFile {
        Map<String, PlayerData> players = new LinkedHashMap<>();
    }
}
