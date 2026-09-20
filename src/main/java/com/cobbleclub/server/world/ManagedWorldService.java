package com.cobbleclub.server.world;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.chestshop.ChestShopStore;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.mixin.WorldBorderAccessor;
import com.cobbleclub.server.mixin.WorldBorderSyncerAccessor;
import com.cobbleclub.server.world.ManagedBorderService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.event.DynamicDimensionLoadCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.Heightmap;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;

public final class ManagedWorldService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<WorldSpec> SPECS = List.of(new WorldSpec("redworld", false, List.of("redworld", "red")), new WorldSpec("yellowworld", false, List.of("yellowworld", "yellow")), new WorldSpec("blueworld", false, List.of("blueworld", "blue")), new WorldSpec("greenworld", false, List.of("greenworld", "green")), new WorldSpec("resource", false, List.of("resource", "resourceworld", "resources")), new WorldSpec("purpleworld", true, List.of("purpleworld", "purple")), new WorldSpec("orangeworld", true, List.of("orangeworld", "orange")), new WorldSpec("pinkworld", true, List.of("pinkworld", "pink")), new WorldSpec("cyanworld", true, List.of("cyanworld", "cyan")));
    private static final Map<String, ManagedEntry> ENTRIES = new LinkedHashMap<String, ManagedEntry>();
    private static final ThreadLocal<Long> ACTIVE_CONSTRUCTION_SEED = new ThreadLocal();
    private static final Set<String> REGENERATING = new LinkedHashSet<String>();
    private static Path statePath;
    private static long ticks;

    private ManagedWorldService() {
    }

    public static void loadPersistedDimensionsEarly(MinecraftServer server, DynamicDimensionLoadCallback.DynamicDimensionLoader loader) {
        if (server == null || loader == null) {
            return;
        }
        statePath = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("managed-worlds.json");
        ManagedWorldService.loadState();
        boolean changed = false;
        for (WorldSpec spec : SPECS) {
            Identifier id;
            ManagedEntry entry = ENTRIES.get(spec.logical);
            if (entry == null || entry.id == null || entry.id.isBlank() || (id = Identifier.tryParse((String)entry.id)) == null || !Files.isDirectory(ManagedWorldService.dimensionFolder(server, id), new LinkOption[0]) || ManagedWorldService.findLoaded(server, id) != null) continue;
            if (entry.seed == null) {
                ManagedWorldService.ensureLegacySeed(server, entry);
                changed = true;
            }
            try {
                ServerWorld world = ManagedWorldService.withConstructionSeed(entry.seed, () -> loader.loadDynamicDimension(id, server.getOverworld().getChunkManager().getChunkGenerator(), ManagedWorldService.freshOverworldDimensionType(server)));
                if (world == null) continue;
                ManagedWorldService.detachOverworldBorderMirror(server, world);
                if (entry.spawn == null) {
                    ManagedWorldService.ensureLegacySpawn(world, entry);
                    changed = true;
                }
                entry.initialized = true;
            }
            catch (Throwable error) {
                CobbleClubServer.LOGGER.error("Early restore failed for managed world {} ({})", new Object[]{spec.logical, id, error});
            }
        }
        if (changed) {
            ManagedWorldService.saveState();
        }
    }

    public static void initialize(MinecraftServer server) {
        ticks = 0L;
        REGENERATING.clear();
        ScheduledTasks.clear();
        statePath = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("managed-worlds.json");
        ManagedWorldService.loadState();
        for (WorldSpec spec : SPECS) {
            ManagedWorldService.initializeOne(server, spec);
        }
        ManagedWorldService.saveState();
    }

    public static void shutdown(MinecraftServer server) {
        ManagedWorldService.saveState();
        REGENERATING.clear();
        ACTIVE_CONSTRUCTION_SEED.remove();
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % 200L == 0L) {
            ManagedWorldService.finalizeFirstCreations(server);
        }
    }

    public static List<String> logicalNames() {
        return SPECS.stream().map(WorldSpec::logical).toList();
    }

    public static boolean isManagedName(String input) {
        return ManagedWorldService.normalize(input) != null;
    }

    public static ServerWorld getWorld(MinecraftServer server, String input) {
        String logical = ManagedWorldService.normalize(input);
        if (server == null || logical == null || REGENERATING.contains(logical)) {
            return null;
        }
        ManagedEntry entry = ENTRIES.get(logical);
        if (entry == null || entry.id == null || entry.id.isBlank()) {
            return ManagedWorldService.findLoadedByAliases(server, ManagedWorldService.spec(logical));
        }
        Identifier id = Identifier.tryParse((String)entry.id);
        return id == null ? null : ManagedWorldService.findLoaded(server, id);
    }

    public static String resolvedId(String input) {
        String logical = ManagedWorldService.normalize(input);
        ManagedEntry entry = logical == null ? null : ENTRIES.get(logical);
        return entry == null ? null : entry.id;
    }

    public static String status(MinecraftServer server, String input) {
        String id;
        String logical = ManagedWorldService.normalize(input);
        if (logical == null) {
            return "Unknown managed world.";
        }
        ManagedEntry entry = ENTRIES.get(logical);
        ServerWorld world = ManagedWorldService.getWorld(server, logical);
        String string = id = entry == null || entry.id == null ? "unresolved" : entry.id;
        if (REGENERATING.contains(logical)) {
            return ManagedWorldService.displayName(logical) + " is regenerating (" + id + ").";
        }
        if (world == null) {
            return ManagedWorldService.displayName(logical) + " is unavailable (saved id: " + id + "). No automatic regeneration will occur.";
        }
        String seed = entry != null && entry.seed != null ? Long.toString(entry.seed) : "legacy/shared";
        String spawn = entry != null && entry.spawn != null ? String.format(Locale.ROOT, "%d %d %d", entry.spawn.x, entry.spawn.y, entry.spawn.z) : "legacy/shared";
        return String.format(Locale.ROOT, "%s is online as %s | seed %s | spawn %s.", ManagedWorldService.displayName(logical), id, seed, spawn);
    }

    public static int regenerate(MinecraftServer server, String input, Consumer<Text> feedback) {
        String logical = ManagedWorldService.normalize(input);
        if (server == null || logical == null) {
            feedback.accept((Text)Text.literal((String)"Unknown managed world. Use redworld, yellowworld, blueworld, greenworld, resource, purpleworld, orangeworld, pinkworld or cyanworld.").formatted(Formatting.RED));
            return 0;
        }
        if (REGENERATING.contains(logical)) {
            feedback.accept((Text)Text.literal((String)(ManagedWorldService.displayName(logical) + " is already regenerating.")).formatted(Formatting.YELLOW));
            return 0;
        }
        ManagedEntry entry = ENTRIES.get(logical);
        if (entry == null || entry.id == null || entry.id.isBlank()) {
            feedback.accept((Text)Text.literal((String)(ManagedWorldService.displayName(logical) + " has no resolved dimension ID, so it was not touched.")).formatted(Formatting.RED));
            return 0;
        }
        Identifier id = Identifier.tryParse((String)entry.id);
        if (id == null) {
            return 0;
        }
        ManagedWorldService.ensureLegacySeed(server, entry);
        DynamicDimensionRegistry registry = DynamicDimensionRegistry.from((MinecraftServer)server);
        Path folder = ManagedWorldService.dimensionFolder(server, id);
        ServerWorld loaded = ManagedWorldService.findLoaded(server, id);
        if (loaded == null && !Files.isDirectory(folder, new LinkOption[0])) {
            REGENERATING.add(logical);
            int claimsRemoved = ClaimsStore.removeDimension(entry.id);
            int shopsRemoved = ChestShopStore.removeDimension(entry.id);
            long newSeed = ManagedWorldService.rotateSeed(server, entry);
            feedback.accept((Text)Text.literal((String)(ManagedWorldService.displayName(logical) + " has no world data on disk. Creating a fresh independent world with seed " + newSeed + ".")).formatted(Formatting.GOLD));
            ScheduledTasks.schedule(ticks + 1L, () -> ManagedWorldService.recreateAfterDelete(server, registry, entry, logical, id, claimsRemoved, shopsRemoved));
            return 1;
        }
        RegistryKey worldKey = RegistryKey.of((RegistryKey)RegistryKeys.WORLD, (Identifier)id);
        if (!registry.dynamicDimensionExists(worldKey) && loaded == null && Files.isDirectory(folder, new LinkOption[0])) {
            ManagedWorldService.loadDynamicDimension(server, registry, entry, id);
        }
        if (!registry.canDeleteDimension(worldKey)) {
            feedback.accept((Text)Text.literal((String)(ManagedWorldService.displayName(logical) + " is not a CobbleClub dynamic world in this session, so regeneration was refused instead of risking its files.")).formatted(Formatting.RED));
            return 0;
        }
        REGENERATING.add(logical);
        feedback.accept((Text)Text.literal((String)("Regenerating " + ManagedWorldService.displayName(logical) + "... players are being moved to spawn and the old dimension data will be deleted.")).formatted(Formatting.GOLD));
        boolean deleted = registry.deleteDynamicDimension(id, (srv, player) -> ManagedWorldService.moveToOverworld(srv, player));
        if (!deleted) {
            REGENERATING.remove(logical);
            feedback.accept((Text)Text.literal((String)("Could not delete " + ManagedWorldService.displayName(logical) + ". Nothing was regenerated.")).formatted(Formatting.RED));
            return 0;
        }
        int claimsRemoved = ClaimsStore.removeDimension(entry.id);
        int shopsRemoved = ChestShopStore.removeDimension(entry.id);
        long newSeed = ManagedWorldService.rotateSeed(server, entry);
        feedback.accept((Text)Text.literal((String)(ManagedWorldService.displayName(logical) + " will be recreated with independent seed " + newSeed + ".")).formatted(Formatting.GREEN));
        ScheduledTasks.schedule(ticks + 4L, () -> ManagedWorldService.recreateAfterDelete(server, registry, entry, logical, id, claimsRemoved, shopsRemoved));
        return 1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void recreateAfterDelete(MinecraftServer server, DynamicDimensionRegistry registry, ManagedEntry entry, String logical, Identifier id, int claimsRemoved, int shopsRemoved) {
        try {
            ServerWorld created = ManagedWorldService.createDynamicDimension(server, registry, entry, id);
            if (created == null) {
                CobbleClubServer.LOGGER.error("Could not recreate managed world {} ({})", (Object)logical, (Object)id);
                return;
            }
            entry.initialized = true;
            ManagedWorldService.ensureFreshSpawn(created, entry);
            if (entry.spawn != null) {
                ManagedBorderService.onWorldRegenerated(server, logical, entry.spawn.pos());
            }
            ManagedWorldService.saveState();
            CobbleClubServer.LOGGER.info("Regenerated {} as {} with independent seed {} and spawn {}. Removed {} claims and {} chest shops.", new Object[]{logical, id, entry.seed, entry.spawn == null ? "unresolved" : entry.spawn.describe(), claimsRemoved, shopsRemoved});
        }
        finally {
            REGENERATING.remove(logical);
        }
    }

    public static void runScheduled() {
        ScheduledTasks.run(ticks);
    }

    private static void initializeOne(MinecraftServer server, WorldSpec spec) {
        ManagedEntry existingState = ENTRIES.get(spec.logical);
        if (existingState != null && existingState.id != null && !existingState.id.isBlank()) {
            Identifier id = Identifier.tryParse((String)existingState.id);
            if (id == null) {
                return;
            }
            ServerWorld loaded = ManagedWorldService.findLoaded(server, id);
            if (loaded != null) {
                existingState.initialized = true;
                ManagedWorldService.ensureLegacySeed(server, existingState);
                ManagedWorldService.ensureLegacySpawn(loaded, existingState);
                ManagedWorldService.detachOverworldBorderMirror(server, loaded);
                return;
            }
            Path folder = ManagedWorldService.dimensionFolder(server, id);
            if (Files.isDirectory(folder, new LinkOption[0])) {
                ManagedWorldService.loadExisting(server, spec, existingState, id);
                return;
            }
            if (existingState.initialized) {
                CobbleClubServer.LOGGER.error("Managed world {} ({}) is missing from disk. It will stay unavailable until an admin restores it or explicitly regenerates it.", (Object)spec.logical, (Object)id);
                return;
            }
            if (spec.createOnFirstRun) {
                ManagedWorldService.createFirstTime(server, spec, existingState, id);
            }
            return;
        }
        ServerWorld loaded = ManagedWorldService.findLoadedByAliases(server, spec);
        if (loaded != null) {
            String id = loaded.getRegistryKey().getValue().toString();
            ManagedEntry entry = new ManagedEntry(id, true);
            ManagedWorldService.ensureLegacySeed(server, entry);
            ManagedWorldService.ensureLegacySpawn(loaded, entry);
            ENTRIES.put(spec.logical, entry);
            ManagedWorldService.detachOverworldBorderMirror(server, loaded);
            return;
        }
        Identifier discovered = ManagedWorldService.discoverExistingId(server, spec);
        if (discovered != null) {
            ManagedEntry entry = new ManagedEntry(discovered.toString(), true);
            ManagedWorldService.ensureLegacySeed(server, entry);
            ENTRIES.put(spec.logical, entry);
            ManagedWorldService.loadExisting(server, spec, entry, discovered);
            return;
        }
        if (spec.createOnFirstRun) {
            Identifier id = Identifier.of((String)"cobbleclub", (String)spec.logical);
            ManagedEntry entry = new ManagedEntry(id.toString(), false);
            entry.seed = ManagedWorldService.nextUniqueSeed(server);
            ENTRIES.put(spec.logical, entry);
            ManagedWorldService.saveState();
            ManagedWorldService.createFirstTime(server, spec, entry, id);
        } else {
            CobbleClubServer.LOGGER.error("Existing managed world '{}' was not found. CobbleClub will NOT create or regenerate it automatically.", (Object)spec.logical);
        }
    }

    private static void loadExisting(MinecraftServer server, WorldSpec spec, ManagedEntry entry, Identifier id) {
        ManagedWorldService.ensureLegacySeed(server, entry);
        DynamicDimensionRegistry registry = DynamicDimensionRegistry.from((MinecraftServer)server);
        ServerWorld world = ManagedWorldService.loadDynamicDimension(server, registry, entry, id);
        if (world != null) {
            entry.initialized = true;
            ManagedWorldService.ensureLegacySpawn(world, entry);
            ManagedWorldService.saveState();
            CobbleClubServer.LOGGER.info("Adopted existing {} as {} without regenerating it.", (Object)spec.logical, (Object)id);
        } else {
            CobbleClubServer.LOGGER.error("Could not load existing managed world {} ({})", (Object)spec.logical, (Object)id);
        }
    }

    private static void createFirstTime(MinecraftServer server, WorldSpec spec, ManagedEntry entry, Identifier id) {
        DynamicDimensionRegistry registry;
        ServerWorld world;
        if (entry.seed == null) {
            entry.seed = ManagedWorldService.nextUniqueSeed(server);
        }
        if ((world = ManagedWorldService.createDynamicDimension(server, registry = DynamicDimensionRegistry.from((MinecraftServer)server), entry, id)) != null) {
            ManagedWorldService.ensureFreshSpawn(world, entry);
            entry.initialized = Files.isDirectory(ManagedWorldService.dimensionFolder(server, id), new LinkOption[0]);
            ManagedWorldService.saveState();
            CobbleClubServer.LOGGER.info("Created new managed world {} as {}.", (Object)spec.logical, (Object)id);
        } else {
            CobbleClubServer.LOGGER.error("First-time creation failed for managed world {} ({})", (Object)spec.logical, (Object)id);
        }
    }

    public static Long seedFor(ServerWorld world) {
        ManagedEntry entry = ManagedWorldService.entryFor(world);
        return entry == null ? null : entry.seed;
    }

    public static Long activeConstructionSeed() {
        return ACTIVE_CONSTRUCTION_SEED.get();
    }

    public static BlockPos spawnFor(ServerWorld world) {
        ManagedEntry entry = ManagedWorldService.entryFor(world);
        return entry == null || entry.spawn == null ? null : entry.spawn.pos();
    }

    public static Float spawnAngleFor(ServerWorld world) {
        ManagedEntry entry = ManagedWorldService.entryFor(world);
        return entry == null || entry.spawn == null ? null : Float.valueOf(entry.spawn.angle);
    }

    public static boolean captureManagedSpawn(ServerWorld world, BlockPos pos, float angle) {
        ManagedEntry entry = ManagedWorldService.entryFor(world);
        if (entry == null || pos == null) {
            return false;
        }
        entry.spawn = new SpawnAnchor(pos.getX(), pos.getY(), pos.getZ(), angle);
        ManagedWorldService.saveState();
        return true;
    }

    public static int setSpawnHere(MinecraftServer server, String input, ServerPlayerEntity player, Consumer<Text> feedback) {
        String logical = ManagedWorldService.normalize(input);
        if (server == null || logical == null) {
            feedback.accept((Text)Text.literal((String)"Unknown managed world.").formatted(Formatting.RED));
            return 0;
        }
        if (player == null) {
            feedback.accept((Text)Text.literal((String)"Run this command in-game while standing in the target world.").formatted(Formatting.RED));
            return 0;
        }
        ServerWorld target = ManagedWorldService.getWorld(server, logical);
        if (target == null) {
            feedback.accept((Text)Text.literal((String)(ManagedWorldService.displayName(logical) + " is unavailable.")).formatted(Formatting.RED));
            return 0;
        }
        if (player.getServerWorld() != target) {
            feedback.accept((Text)Text.literal((String)("Stand inside " + ManagedWorldService.displayName(logical) + " before using setspawn here.")).formatted(Formatting.RED));
            return 0;
        }
        BlockPos pos = player.getBlockPos();
        ManagedEntry entry = ENTRIES.get(logical);
        if (entry == null) {
            return 0;
        }
        entry.spawn = new SpawnAnchor(pos.getX(), pos.getY(), pos.getZ(), player.getYaw());
        ManagedWorldService.saveState();
        feedback.accept((Text)Text.literal((String)String.format(Locale.ROOT, "%s spawn set to %d %d %d. Border center is unchanged until you run border %s center spawn.", ManagedWorldService.displayName(logical), pos.getX(), pos.getY(), pos.getZ(), logical)).formatted(Formatting.GREEN));
        return 1;
    }

    private static ManagedEntry entryFor(ServerWorld world) {
        if (world == null) {
            return null;
        }
        String id = world.getRegistryKey().getValue().toString();
        for (ManagedEntry entry : ENTRIES.values()) {
            if (entry == null || entry.id == null || !entry.id.equals(id)) continue;
            return entry;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static <T> T withConstructionSeed(Long seed, Supplier<T> action) {
        Long previous = ACTIVE_CONSTRUCTION_SEED.get();
        if (seed == null) {
            ACTIVE_CONSTRUCTION_SEED.remove();
        } else {
            ACTIVE_CONSTRUCTION_SEED.set(seed);
        }
        try {
            T t = action.get();
            return t;
        }
        finally {
            if (previous == null) {
                ACTIVE_CONSTRUCTION_SEED.remove();
            } else {
                ACTIVE_CONSTRUCTION_SEED.set(previous);
            }
        }
    }

    private static ServerWorld createDynamicDimension(MinecraftServer server, DynamicDimensionRegistry registry, ManagedEntry entry, Identifier id) {
        ManagedWorldService.ensureLegacySeed(server, entry);
        ServerWorld world = ManagedWorldService.withConstructionSeed(entry.seed, () -> registry.createDynamicDimension(id, server.getOverworld().getChunkManager().getChunkGenerator(), ManagedWorldService.freshOverworldDimensionType(server)));
        if (world != null) {
            ManagedWorldService.detachOverworldBorderMirror(server, world);
        }
        return world;
    }

    private static ServerWorld loadDynamicDimension(MinecraftServer server, DynamicDimensionRegistry registry, ManagedEntry entry, Identifier id) {
        ManagedWorldService.ensureLegacySeed(server, entry);
        ServerWorld world = ManagedWorldService.withConstructionSeed(entry.seed, () -> registry.loadDynamicDimension(id, server.getOverworld().getChunkManager().getChunkGenerator(), ManagedWorldService.freshOverworldDimensionType(server)));
        if (world != null) {
            ManagedWorldService.detachOverworldBorderMirror(server, world);
        }
        return world;
    }

    private static void ensureLegacySeed(MinecraftServer server, ManagedEntry entry) {
        if (entry != null && entry.seed == null) {
            entry.seed = server.getOverworld().getSeed();
        }
    }

    private static long rotateSeed(MinecraftServer server, ManagedEntry entry) {
        long seed = ManagedWorldService.nextUniqueSeed(server);
        entry.seed = seed;
        entry.spawn = null;
        ManagedWorldService.saveState();
        return seed;
    }

    private static long nextUniqueSeed(MinecraftServer server) {
        long candidate;
        long overworldSeed = server.getOverworld().getSeed();
        while ((candidate = ThreadLocalRandom.current().nextLong()) == overworldSeed || candidate == 0L || ManagedWorldService.seedAlreadyUsed(candidate)) {
        }
        return candidate;
    }

    private static boolean seedAlreadyUsed(long candidate) {
        for (ManagedEntry entry : ENTRIES.values()) {
            if (entry == null || entry.seed == null || entry.seed != candidate) continue;
            return true;
        }
        return false;
    }

    private static void ensureLegacySpawn(ServerWorld world, ManagedEntry entry) {
        if (world == null || entry == null || entry.spawn != null) {
            return;
        }
        BlockPos spawn = world.getSpawnPos();
        entry.spawn = new SpawnAnchor(spawn.getX(), spawn.getY(), spawn.getZ(), world.getSpawnAngle());
    }

    private static void ensureFreshSpawn(ServerWorld world, ManagedEntry entry) {
        if (world == null || entry == null || entry.seed == null || entry.spawn != null) {
            return;
        }
        Random random = new Random((long)(entry.seed ^ 0x434F42424C45434CL));
        BlockPos fallback = null;
        for (int attempt = 0; attempt < 32; ++attempt) {
            BlockPos overworldSpawn;
            int z;
            int x = random.nextInt(8001) - 4000;
            if (ManagedWorldService.spawnCoordinatesAlreadyUsed(entry, x, z = random.nextInt(8001) - 4000) || (overworldSpawn = world.getServer().getOverworld().getSpawnPos()).getX() == x && overworldSpawn.getZ() == z) continue;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (fallback == null) {
                fallback = pos;
            }
            if (!ManagedWorldService.isSafeSpawn(world, pos)) continue;
            entry.spawn = new SpawnAnchor(x, y, z, 0.0f);
            return;
        }
        if (fallback == null) {
            fallback = new BlockPos(0, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 0, 0), 0);
        }
        entry.spawn = new SpawnAnchor(fallback.getX(), fallback.getY(), fallback.getZ(), 0.0f);
    }

    private static boolean spawnCoordinatesAlreadyUsed(ManagedEntry self, int x, int z) {
        for (ManagedEntry entry : ENTRIES.values()) {
            if (entry == null || entry == self || entry.spawn == null || entry.spawn.x != x || entry.spawn.z != z) continue;
            return true;
        }
        return false;
    }

    private static boolean isSafeSpawn(ServerWorld world, BlockPos pos) {
        BlockPos below = pos.down();
        return world.getBlockState(below).isSolidBlock((BlockView)world, below) && world.getFluidState(pos).isEmpty() && world.getBlockState(pos).getCollisionShape((BlockView)world, pos).isEmpty() && world.getBlockState(pos.up()).getCollisionShape((BlockView)world, pos.up()).isEmpty();
    }

    private static void detachOverworldBorderMirror(MinecraftServer server, ServerWorld managedWorld) {
        if (server == null || managedWorld == null || server.getOverworld() == null) {
            return;
        }
        WorldBorder overworldBorder = server.getOverworld().getWorldBorder();
        WorldBorder managedBorder = managedWorld.getWorldBorder();
        ArrayList<WorldBorderListener> listeners = new ArrayList<WorldBorderListener>(((WorldBorderAccessor)overworldBorder).cobbleclub$getListeners());
        int removed = 0;
        for (WorldBorderListener listener : listeners) {
            WorldBorder target;
            if (!(listener instanceof WorldBorderListener.WorldBorderSyncer) || (target = ((WorldBorderSyncerAccessor)listener).cobbleclub$getBorder()) != managedBorder) continue;
            overworldBorder.removeListener(listener);
            ++removed;
        }
        if (removed > 0) {
            CobbleClubServer.LOGGER.info("Detached {} DynamicDimensions Overworld border mirror(s) from {}.", (Object)removed, (Object)managedWorld.getRegistryKey().getValue());
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static Identifier discoverExistingId(MinecraftServer server, WorldSpec spec) {
        Path dimensions = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions");
        if (!Files.isDirectory(dimensions, new LinkOption[0])) {
            return null;
        }
        List<String> preferredNamespaces = List.of("multiworld", "minecraft", "resourceworld", "cobbleclub");
        for (String namespace : preferredNamespaces) {
            for (String alias : spec.aliases) {
                Path candidate = dimensions.resolve(namespace).resolve(alias);
                if (!Files.isDirectory(candidate, new LinkOption[0])) continue;
                return Identifier.of((String)namespace, (String)alias);
            }
        }
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(dimensions);){
            for (Path namespace : namespaces) {
                if (!Files.isDirectory(namespace, new LinkOption[0])) continue;
                String ns = namespace.getFileName().toString();
                for (String alias : spec.aliases) {
                    if (!Files.isDirectory(namespace.resolve(alias), new LinkOption[0])) continue;
                    Identifier parsedId = Identifier.of((String)ns, (String)alias);
                    return parsedId;
                }
            }
        }
        catch (IOException error) {
            CobbleClubServer.LOGGER.warn("Could not scan legacy managed-world folders", (Throwable)error);
        }
        return null;
    }

    private static ServerWorld findLoadedByAliases(MinecraftServer server, WorldSpec spec) {
        if (server == null || spec == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            String path = world.getRegistryKey().getValue().getPath().toLowerCase(Locale.ROOT);
            if (!spec.aliases.contains(path)) continue;
            return world;
        }
        return null;
    }

    private static ServerWorld findLoaded(MinecraftServer server, Identifier id) {
        if (server == null || id == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (!world.getRegistryKey().getValue().equals((Object)id)) continue;
            return world;
        }
        return null;
    }

    private static Path dimensionFolder(MinecraftServer server, Identifier id) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath());
    }

    private static void finalizeFirstCreations(MinecraftServer server) {
        boolean changed = false;
        for (Map.Entry<String, ManagedEntry> value : ENTRIES.entrySet()) {
            Identifier id;
            ManagedEntry entry = value.getValue();
            if (entry == null || entry.initialized || entry.id == null || (id = Identifier.tryParse((String)entry.id)) == null || !Files.isDirectory(ManagedWorldService.dimensionFolder(server, id), new LinkOption[0])) continue;
            entry.initialized = true;
            changed = true;
        }
        if (changed) {
            ManagedWorldService.saveState();
        }
    }

    private static DimensionType freshOverworldDimensionType(MinecraftServer server) {
        DimensionType source = server.getOverworld().getDimension();
        return new DimensionType(source.fixedTime(), source.hasSkyLight(), source.hasCeiling(), source.ultrawarm(), source.natural(), source.coordinateScale(), source.bedWorks(), source.respawnAnchorWorks(), source.minY(), source.height(), source.logicalHeight(), source.infiniburn(), source.effects(), source.ambientLight(), source.monsterSettings());
    }

    private static void moveToOverworld(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld overworld = server.getOverworld();
        BlockPos spawn = overworld.getSpawnPos();
        player.teleport(overworld, (double)spawn.getX() + 0.5, (double)spawn.getY() + 1.0, (double)spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.sendMessage((Text)Text.literal((String)"The world you were in is being regenerated. You were moved safely to spawn.").formatted(Formatting.YELLOW), false);
    }

    public static String normalize(String input) {
        String value;
        if (input == null || input.isBlank()) {
            return null;
        }
        return switch (value = input.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "")) {
            case "red", "redworld" -> "redworld";
            case "yellow", "yellowworld" -> "yellowworld";
            case "blue", "blueworld" -> "blueworld";
            case "green", "greenworld" -> "greenworld";
            case "resource", "resources", "resourceworld" -> "resource";
            case "purple", "purpleworld" -> "purpleworld";
            case "orange", "orangeworld" -> "orangeworld";
            case "pink", "pinkworld" -> "pinkworld";
            case "cyan", "cyanworld" -> "cyanworld";
            default -> null;
        };
    }

    public static String displayName(String logical) {
        String normalized = ManagedWorldService.normalize(logical);
        if (normalized == null) {
            return logical == null ? "World" : logical;
        }
        return switch (normalized) {
            case "redworld" -> "Red World";
            case "yellowworld" -> "Yellow World";
            case "blueworld" -> "Blue World";
            case "greenworld" -> "Green World";
            case "resource" -> "Resource World";
            case "purpleworld" -> "Purple World";
            case "orangeworld" -> "Orange World";
            case "pinkworld" -> "Pink World";
            case "cyanworld" -> "Cyan World";
            default -> normalized;
        };
    }

    private static WorldSpec spec(String logical) {
        for (WorldSpec spec : SPECS) {
            if (!spec.logical.equals(logical)) continue;
            return spec;
        }
        return null;
    }

    private static void loadState() {
        ENTRIES.clear();
        if (statePath == null || !Files.exists(statePath, new LinkOption[0])) {
            return;
        }
        try {
            StateFile file = (StateFile)GSON.fromJson(Files.readString(statePath, StandardCharsets.UTF_8), StateFile.class);
            if (file != null && file.worlds != null) {
                ENTRIES.putAll(file.worlds);
            }
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.error("Could not read managed-world state. Existing dimensions will be discovered again without overwriting them.", (Throwable)error);
        }
    }

    private static void saveState() {
        if (statePath == null) {
            return;
        }
        try {
            Files.createDirectories(statePath.getParent(), new FileAttribute[0]);
            StateFile file = new StateFile();
            file.version = 2;
            file.worlds.putAll(ENTRIES);
            Files.writeString(statePath, (CharSequence)GSON.toJson((Object)file), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException error) {
            CobbleClubServer.LOGGER.error("Could not save managed-world state", (Throwable)error);
        }
    }

    private record WorldSpec(String logical, boolean createOnFirstRun, List<String> aliases) {
    }

    private static final class ManagedEntry {
        String id;
        boolean initialized;
        Long seed;
        SpawnAnchor spawn;

        ManagedEntry() {
        }

        ManagedEntry(String id, boolean initialized) {
            this.id = id;
            this.initialized = initialized;
        }
    }

    private static final class SpawnAnchor {
        int x;
        int y;
        int z;
        float angle;

        SpawnAnchor() {
        }

        SpawnAnchor(int x, int y, int z, float angle) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.angle = angle;
        }

        BlockPos pos() {
            return new BlockPos(this.x, this.y, this.z);
        }

        String describe() {
            return this.x + " " + this.y + " " + this.z;
        }
    }

    private static final class ScheduledTasks {
        private static final List<Task> TASKS = new ArrayList<Task>();

        private ScheduledTasks() {
        }

        static void clear() {
            TASKS.clear();
        }

        static void schedule(long at, Runnable task) {
            TASKS.add(new Task(at, task));
        }

        static void run(long now) {
            for (Task task : List.copyOf(TASKS)) {
                if (task.at > now) continue;
                TASKS.remove(task);
                try {
                    task.action.run();
                }
                catch (Throwable error) {
                    CobbleClubServer.LOGGER.error("Managed-world task failed", error);
                }
            }
        }

        private record Task(long at, Runnable action) {
        }
    }

    private static final class StateFile {
        int version = 2;
        Map<String, ManagedEntry> worlds = new LinkedHashMap<String, ManagedEntry>();

        private StateFile() {
        }
    }
}
