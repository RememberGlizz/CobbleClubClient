/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.class_5218
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.data;

import com.cobbleclub.server.util.CrateKeyIds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

public final class PlayerDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, PlayerData> PLAYERS = new HashMap<String, PlayerData>();
    private static Path path;

    private PlayerDataStore() {
    }

    public static void load(MinecraftServer server) {
        path = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("player-data.json");
        PLAYERS.clear();
        try {
            StoreFile store;
            if (Files.exists(path, new LinkOption[0]) && (store = (StoreFile)GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), StoreFile.class)) != null && store.players != null) {
                PLAYERS.putAll(store.players);
                PLAYERS.values().forEach(PlayerData::normalize);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static PlayerData get(UUID uuid) {
        return PLAYERS.computeIfAbsent(uuid.toString(), ignored -> new PlayerData());
    }

    public static Map<String, PlayerData> all() {
        return PLAYERS;
    }

    public static void save() {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            StoreFile file = new StoreFile();
            file.players = new LinkedHashMap<String, PlayerData>(PLAYERS);
            Files.writeString(path, (CharSequence)GSON.toJson((Object)file), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static final class StoreFile {
        Map<String, PlayerData> players = new LinkedHashMap<String, PlayerData>();

        private StoreFile() {
        }
    }

    public static final class PlayerData {
        public String activeTag;
        public String lastTagRank;
        public long lastTagSwitchMillis;
        public Set<String> ownedTags = new HashSet<String>();
        public Map<String, String> equipped = new LinkedHashMap<String, String>();
        public Map<String, Integer> colors = new LinkedHashMap<String, Integer>();
        public Set<String> ownedCosmetics = new HashSet<String>();
        public Set<String> fulfilledStoreOrders = new HashSet<String>();
        public boolean wardrobeHidden;
        public String glow;
        public Set<String> ownedGlows = new HashSet<String>();
        public Map<String, PresetData> presets = new LinkedHashMap<String, PresetData>();
        public Map<String, Integer> crateKeys = new CanonicalCrateKeyMap();
        public Map<String, Integer> crateOpens = new LinkedHashMap<String, Integer>();
        public String lastKnownName;
        public long balance;
        public long gems;
        public long pokemonCatches;
        public long shinyPokemonCatches;
        public long pokemonDefeats;
        public boolean economyInitialized;
        public int bonusClaimBlocks;
        public int claimBlockPurchases;
        public long rewardedPlayTicks;
        public long moneyRewardedPlayTicks;
        public long lastDailyClaimMillis;
        public long lastDailyClaimEpochDay = Long.MIN_VALUE;
        public boolean newbKitReceived;
        public long lastNewbKitClaimMillis;
        public Map<String, Long> kitClaimTimes = new LinkedHashMap<String, Long>();
        public Map<String, Long> kitCooldownReductions = new LinkedHashMap<String, Long>();
        public Set<String> rctMilestones = new HashSet<String>();
        public Set<String> claimedContractRewards = new HashSet<String>();

        // Temporary spawn sanctuary state. These values are the player's real
        // vitals from immediately before entering minecraft:overworld.
        public boolean spawnVitalsCaptured;
        public float spawnReturnHealth;
        public int spawnReturnFoodLevel;
        public float spawnReturnSaturation;
        public float spawnReturnExhaustion;

        public int revision;

        public void normalize() {
            if (this.ownedTags == null) {
                this.ownedTags = new HashSet<String>();
            }
            if (this.equipped == null) {
                this.equipped = new LinkedHashMap<String, String>();
            }
            if (this.colors == null) {
                this.colors = new LinkedHashMap<String, Integer>();
            }
            if (this.ownedCosmetics == null) {
                this.ownedCosmetics = new HashSet<String>();
            }
            if (this.fulfilledStoreOrders == null) {
                this.fulfilledStoreOrders = new HashSet<String>();
            }
            if (this.ownedGlows == null) {
                this.ownedGlows = new HashSet<String>();
            }
            if (this.presets == null) {
                this.presets = new LinkedHashMap<String, PresetData>();
            }
            if (this.crateKeys == null) {
                this.crateKeys = new CanonicalCrateKeyMap();
            } else if (!(this.crateKeys instanceof CanonicalCrateKeyMap)) {
                CanonicalCrateKeyMap canonical = new CanonicalCrateKeyMap();
                for (Map.Entry<String, Integer> entry : this.crateKeys.entrySet()) {
                    String key;
                    if (entry.getKey() == null || entry.getValue() == null || (key = CrateKeyIds.canonical(entry.getKey())).isEmpty()) continue;
                    canonical.merge(key, Math.max(0, entry.getValue()), Math::max);
                }
                this.crateKeys = canonical;
            }
            if (this.crateOpens == null) {
                this.crateOpens = new LinkedHashMap<String, Integer>();
            }
            if (this.kitClaimTimes == null) {
                this.kitClaimTimes = new LinkedHashMap<String, Long>();
            }
            if (this.kitCooldownReductions == null) {
                this.kitCooldownReductions = new LinkedHashMap<String, Long>();
            }
            if (this.rctMilestones == null) {
                this.rctMilestones = new HashSet<String>();
            }
            if (this.claimedContractRewards == null) {
                this.claimedContractRewards = new HashSet<String>();
            }
            this.pokemonCatches = Math.max(0L, this.pokemonCatches);
            this.shinyPokemonCatches = Math.max(0L, this.shinyPokemonCatches);
            this.pokemonDefeats = Math.max(0L, this.pokemonDefeats);
            this.claimBlockPurchases = Math.max(0, this.claimBlockPurchases);
            this.spawnReturnHealth = Math.max(0.0F, this.spawnReturnHealth);
            this.spawnReturnFoodLevel = Math.max(0, Math.min(20, this.spawnReturnFoodLevel));
            this.spawnReturnSaturation = Math.max(0.0F, Math.min(20.0F, this.spawnReturnSaturation));
            this.spawnReturnExhaustion = Math.max(0.0F, this.spawnReturnExhaustion);
            if (this.ownedCosmetics.remove("gengar_grin_mask")) {
                this.ownedCosmetics.add("gengar-hat");
            }
            if (this.ownedCosmetics.remove("pikachu_floaty")) {
                this.ownedCosmetics.add("squirtle_floaty");
            }
            this.equipped.replaceAll((slot, id) -> "pikachu_floaty".equals(id) ? "squirtle_floaty" : id);
            this.equipped.replaceAll((slot, id) -> "gengar_grin_mask".equals(id) ? "gengar-hat" : id);
            if (this.colors.containsKey("gengar_grin_mask") && !this.colors.containsKey("gengar-hat")) {
                this.colors.put("gengar-hat", this.colors.remove("gengar_grin_mask"));
            } else {
                this.colors.remove("gengar_grin_mask");
            }
            for (PresetData preset : this.presets.values()) {
                if (preset == null) continue;
                if (preset.equipped != null) {
                    preset.equipped.replaceAll((slot, id) -> "gengar_grin_mask".equals(id) ? "gengar-hat" : id);
                }
                if (preset.colors == null || !preset.colors.containsKey("gengar_grin_mask") || preset.colors.containsKey("gengar-hat")) continue;
                preset.colors.put("gengar-hat", preset.colors.remove("gengar_grin_mask"));
            }
        }
    }

    public static final class PresetData {
        public Map<String, String> equipped = new LinkedHashMap<String, String>();
        public Map<String, Integer> colors = new LinkedHashMap<String, Integer>();
        public String glow;
    }

    private static final class CanonicalCrateKeyMap
    extends LinkedHashMap<String, Integer> {
        private CanonicalCrateKeyMap() {
        }

        private static String key(Object key) {
            String string;
            if (key instanceof String) {
                String string2 = (String)key;
                string = CrateKeyIds.canonical(string2);
            } else {
                string = String.valueOf(key);
            }
            return string;
        }

        @Override
        public Integer get(Object key) {
            return (Integer)super.get(CanonicalCrateKeyMap.key(key));
        }

        @Override
        public Integer getOrDefault(Object key, Integer defaultValue) {
            return super.getOrDefault(CanonicalCrateKeyMap.key(key), defaultValue);
        }

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(CanonicalCrateKeyMap.key(key));
        }

        @Override
        public Integer put(String key, Integer value) {
            return super.put(CrateKeyIds.canonical(key), value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends Integer> map) {
            if (map == null) {
                return;
            }
            map.forEach(this::put);
        }

        @Override
        public Integer putIfAbsent(String key, Integer value) {
            return super.putIfAbsent(CrateKeyIds.canonical(key), value);
        }

        @Override
        public Integer remove(Object key) {
            return (Integer)super.remove(CanonicalCrateKeyMap.key(key));
        }

        @Override
        public boolean remove(Object key, Object value) {
            return super.remove(CanonicalCrateKeyMap.key(key), value);
        }

        @Override
        public Integer replace(String key, Integer value) {
            return super.replace(CrateKeyIds.canonical(key), value);
        }

        @Override
        public boolean replace(String key, Integer oldValue, Integer newValue) {
            return super.replace(CrateKeyIds.canonical(key), oldValue, newValue);
        }

        @Override
        public Integer merge(String key, Integer value, BiFunction<? super Integer, ? super Integer, ? extends Integer> remappingFunction) {
            return super.merge(CrateKeyIds.canonical(key), value, remappingFunction);
        }

        @Override
        public Integer compute(String key, BiFunction<? super String, ? super Integer, ? extends Integer> remappingFunction) {
            String canonical = CrateKeyIds.canonical(key);
            return super.compute(canonical, remappingFunction);
        }

        @Override
        public Integer computeIfAbsent(String key, Function<? super String, ? extends Integer> mappingFunction) {
            String canonical = CrateKeyIds.canonical(key);
            return super.computeIfAbsent(canonical, mappingFunction);
        }

        @Override
        public Integer computeIfPresent(String key, BiFunction<? super String, ? super Integer, ? extends Integer> remappingFunction) {
            String canonical = CrateKeyIds.canonical(key);
            return super.computeIfPresent(canonical, remappingFunction);
        }
    }
}

