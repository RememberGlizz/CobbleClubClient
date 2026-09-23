/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.class_124
 *  net.minecraft.class_1259$class_1260
 *  net.minecraft.class_1259$class_1261
 *  net.minecraft.class_1922
 *  net.minecraft.class_1923
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2394
 *  net.minecraft.class_2398
 *  net.minecraft.class_2561
 *  net.minecraft.class_2680
 *  net.minecraft.class_2902$class_2903
 *  net.minecraft.class_3213
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_3230
 *  net.minecraft.class_3417
 *  net.minecraft.class_3419
 *  net.minecraft.class_5218
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.world.ManagedBorderService;
import com.cobbleclub.server.world.ManagedWorldService;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Formatting;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.world.Heightmap;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

public final class RtpService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MIN_RADIUS = 500;
    private static final int MAX_RADIUS = 19000;
    private static final int MIN_Y = 63;
    private static final int MAX_Y = 256;
    private static final double BORDER_MARGIN = 32.0;
    private static final long WARMUP_TICKS = 0L;
    private static final long BASE_COOLDOWN_SECONDS = 300L;
    private static final long SEARCH_GRACE_TICKS = 240L;
    private static final int MAX_ATTEMPTS = 12;
    private static final int MAX_ACTIVE_CHUNK_REQUESTS = 3;
    private static final int SAFE_COLUMNS_PER_CHUNK = 24;
    private static final int MAX_CACHED_LANDINGS_PER_WORLD = 256;
    private static final int CANDIDATE_SAMPLE_ATTEMPTS = 96;
    private static final int NEIGHBORHOOD_RADIUS = 1;
    private static final int NEIGHBORHOOD_CHUNKS = 9;
    private static final double MIN_DESTINATION_SEPARATION = 1000.0;
    private static final ChunkTicketType<Integer> RTP_TICKET = ChunkTicketType.create("cobbleclub_rtp", Integer::compare);
    private static final List<String> RANDOM_WORLDS = List.of("purpleworld", "redworld", "orangeworld", "yellowworld", "blueworld", "pinkworld", "greenworld", "cyanworld");
    private static final Map<UUID, PendingTeleport> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<Integer, SearchWork> WORK = new HashMap<>();
    private static final List<HeldTicket> HELD_TICKETS = new ArrayList<>();
    private static final Map<String, List<BlockPos>> READY = new HashMap<>();
    private static final Map<String, List<BlockPos>> LANDING_CACHE = new HashMap<>();
    private static final Map<String, Integer> READY_CURSOR = new HashMap<>();
    private static Path cachePath;
    private static boolean cacheLoaded;
    private static boolean cacheDirty;
    private static long serverTick;
    private static int nextTicketId;

    private RtpService() {
    }

    public static int start(ServerPlayerEntity player, String requested) {
        if (player == null) {
            return 0;
        }
        String target = RtpService.normalizeTarget(requested);
        if (target == null) {
            player.sendMessage(Text.literal("Usage: /rtp [purple|red|orange|yellow|resource|blue|pink|green|cyan]").formatted(Formatting.RED), false);
            return 0;
        }
        long until = COOLDOWNS.getOrDefault(player.getUuid(), 0L);
        if (serverTick < until) {
            long seconds = Math.max(1L, (until - serverTick + 19L) / 20L);
            player.sendMessage(Text.literal("RTP is on cooldown for " + seconds + "s.").formatted(Formatting.RED), false);
            return 0;
        }
        if (PENDING.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("A random teleport is already warming up.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        MinecraftServer server = player.getServer();
        if (RtpService.findWorld(server, target) == null) {
            player.sendMessage(Text.literal("That CobbleClub world is currently unavailable.").formatted(Formatting.RED), false);
            return 0;
        }
        RtpService.ensureCacheLoaded(server);
        PendingTeleport pending = new PendingTeleport(target, player.getServerWorld().getRegistryKey().getValue().toString(), player.getX(), player.getY(), player.getZ(), serverTick + 0L, serverTick + 0L + 240L);
        pending.bar.addPlayer(player);
        PENDING.put(player.getUuid(), pending);
        RtpService.ensureWorkForWaiting(server);
        player.sendMessage(Text.literal("\u2726 RTP \u2022 ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD).append(Text.literal("Preparing " + RtpService.displayName(target) + ". You can keep moving.").formatted(RtpService.colorFor(target))), false);
        RtpService.showEffect(player, target);
        return 1;
    }

    public static void tick(MinecraftServer server) {
        ++serverTick;
        if (server == null) {
            return;
        }
        RtpService.tickWork(server);
        for (Map.Entry<UUID, PendingTeleport> entry : List.copyOf(PENDING.entrySet())) {
            boolean assignmentReady;
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                RtpService.removePending(entry.getKey());
                continue;
            }
            PendingTeleport pending = entry.getValue();
            String currentDim = player.getServerWorld().getRegistryKey().getValue().toString();
            if (!currentDim.equals(pending.startDimension)) {
                RtpService.removePending(player.getUuid());
                player.sendMessage(Text.literal("RTP cancelled because you changed dimensions.").formatted(Formatting.RED), false);
                continue;
            }
            ServerWorld targetWorld = RtpService.findWorld(server, pending.target);
            if (targetWorld == null) {
                RtpService.removePending(player.getUuid());
                player.sendMessage(Text.literal("That CobbleClub world is currently unavailable.").formatted(Formatting.RED), false);
                continue;
            }
            RtpService.updateProgressBar(pending, targetWorld);
            long remaining = pending.executeAtTick - serverTick;
            if (remaining > 0L) {
                if (remaining != 40L && remaining != 20L) continue;
                int seconds = (int)(remaining / 20L);
                player.sendMessage(Text.literal("Teleporting to " + RtpService.displayName(pending.target) + " in " + seconds + "...").formatted(RtpService.colorFor(pending.target)), true);
                RtpService.showEffect(player, pending.target);
                continue;
            }
            int desiredDestinations = Math.min(3, Math.max(1, RtpService.waitingPlayers(pending.target)));
            boolean bl = assignmentReady = RtpService.readyChunkCount(targetWorld, pending.target) >= desiredDestinations || serverTick >= pending.searchDeadline - 20L;
            if (pending.safeLocation == null && assignmentReady) {
                pending.safeLocation = RtpService.readyLocation(targetWorld, pending.target);
            }
            if (pending.safeLocation != null) {
                BlockPos safe = RtpService.revalidate(targetWorld, pending.safeLocation);
                if (safe == null) {
                    RtpService.discardLanding(pending.target, pending.safeLocation);
                    pending.safeLocation = null;
                    continue;
                }
                RtpService.removePending(player.getUuid());
                if (!RtpService.teleportPrepared(player, pending.target, targetWorld, safe)) continue;
                COOLDOWNS.put(player.getUuid(), serverTick + RtpService.cooldownSeconds(player) * 20L);
                continue;
            }
            if (serverTick >= pending.searchDeadline) {
                RtpService.removePending(player.getUuid());
                player.sendMessage(Text.literal("No safe RTP location was found. Please try again.").formatted(Formatting.RED), false);
                continue;
            }
            if (pending.waitingMessageSent) continue;
            pending.waitingMessageSent = true;
            player.sendMessage(Text.literal("Preparing a safe destination \u2014 you can keep moving.").formatted(Formatting.YELLOW), true);
        }
        RtpService.ensureWorkForWaiting(server);
        if (cacheDirty && serverTick % 600L == 0L) {
            RtpService.saveCache();
        }
    }

    public static void forget(ServerPlayerEntity player) {
        if (player != null) {
            RtpService.removePending(player.getUuid());
        }
    }

    public static void shutdown(MinecraftServer server) {
        RtpService.saveCache();
        if (server != null) {
            ServerWorld world;
            for (SearchWork work : WORK.values()) {
                world = RtpService.findWorld(server, work.target);
                if (world == null) continue;
                RtpService.removeTicket(world, work);
            }
            for (HeldTicket held : HELD_TICKETS) {
                world = RtpService.findWorld(server, held.target);
                if (world == null) continue;
                RtpService.removeNeighborhoodTickets(world, held.chunkX, held.chunkZ, held.ticketId);
            }
        }
        WORK.clear();
        HELD_TICKETS.clear();
        READY.clear();
        LANDING_CACHE.clear();
        READY_CURSOR.clear();
        for (PendingTeleport pending : PENDING.values()) {
            pending.bar.clearPlayers();
        }
        PENDING.clear();
        COOLDOWNS.clear();
        cachePath = null;
        cacheLoaded = false;
        cacheDirty = false;
        serverTick = 0L;
        nextTicketId = 1;
    }

    private static void ensureWorkForWaiting(MinecraftServer server) {
        if (server == null || WORK.size() >= 3) {
            return;
        }
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        for (PendingTeleport pending : PENDING.values()) {
            if (pending.safeLocation != null) continue;
            targets.add(pending.target);
        }
        for (String target : targets) {
            SearchWork work;
            if (WORK.size() >= 3) break;
            ServerWorld world = RtpService.findWorld(server, target);
            if (world == null) continue;
            int desiredChunks = Math.min(3, RtpService.waitingPlayers(target));
            int availableChunks = RtpService.readyChunkCount(world, target) + RtpService.activeWorkCount(target);
            if (WORK.size() >= 3 || availableChunks >= desiredChunks || !RtpService.selectCandidate(world, work = new SearchWork(target, nextTicketId++))) continue;
            WORK.put(work.ticketId, work);
            RtpService.addTicket(world, work);
        }
    }

    private static void tickWork(MinecraftServer server) {
        HELD_TICKETS.removeIf(held -> {
            if (serverTick < held.releaseAt) {
                return false;
            }
            ServerWorld world = RtpService.findWorld(server, held.target);
            if (world != null) {
                RtpService.removeNeighborhoodTickets(world, held.chunkX, held.chunkZ, held.ticketId);
            }
            return true;
        });
        for (SearchWork work : List.copyOf(WORK.values())) {
            ServerWorld world = RtpService.findWorld(server, work.target);
            if (world == null) {
                WORK.remove(work.ticketId);
                continue;
            }
            if (RtpService.loadedNeighborhoodChunks(world, work.chunkX, work.chunkZ) < 9) {
                if (serverTick - work.startedAt <= 240L) continue;
                RtpService.removeTicket(world, work);
                if (!RtpService.hasWaitingPlayer(work.target) || work.attempts >= 12 || !RtpService.selectCandidate(world, work)) {
                    WORK.remove(work.ticketId);
                    continue;
                }
                RtpService.addTicket(world, work);
                continue;
            }
            List<BlockPos> safe = RtpService.inspectSafeLocations(world, work.chunkX, work.chunkZ, work.sourceX, work.sourceZ);
            if (!safe.isEmpty()) {
                for (BlockPos position : safe) {
                    RtpService.cacheLanding(work.target, position);
                }
                List<BlockPos> ready = READY.computeIfAbsent(work.target, ignored -> new ArrayList<>());
                ready.removeIf(pos -> pos.getX() >> 4 == work.chunkX && pos.getZ() >> 4 == work.chunkZ);
                ready.addAll(safe);
                WORK.remove(work.ticketId);
                HELD_TICKETS.add(new HeldTicket(work.target, work.chunkX, work.chunkZ, work.ticketId, serverTick + 300L));
                continue;
            }
            RtpService.discardChunk(work.target, work.chunkX, work.chunkZ);
            RtpService.removeTicket(world, work);
            if (!RtpService.hasWaitingPlayer(work.target) || work.attempts >= 12 || !RtpService.selectCandidate(world, work)) {
                WORK.remove(work.ticketId);
                continue;
            }
            RtpService.addTicket(world, work);
        }
    }

    private static boolean selectCandidate(ServerWorld world, SearchWork work) {
        Candidate candidate;
        BlockPos cached = RtpService.cachedCandidate(world, work.target);
        Candidate candidate2 = candidate = cached == null ? RtpService.chooseUniqueCandidate(world, work.target) : new Candidate(cached.getX(), cached.getZ());
        if (candidate == null) {
            return false;
        }
        work.sourceX = candidate.x;
        work.sourceZ = candidate.z;
        work.chunkX = candidate.x >> 4;
        work.chunkZ = candidate.z >> 4;
        work.startedAt = serverTick;
        ++work.attempts;
        return true;
    }

    private static void addTicket(ServerWorld world, SearchWork work) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                world.getChunkManager().addTicket(RTP_TICKET, new ChunkPos(work.chunkX + dx, work.chunkZ + dz), 1, work.ticketId);
            }
        }
    }

    private static void removeTicket(ServerWorld world, SearchWork work) {
        RtpService.removeNeighborhoodTickets(world, work.chunkX, work.chunkZ, work.ticketId);
    }

    private static void removeNeighborhoodTickets(ServerWorld world, int chunkX, int chunkZ, int ticketId) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                world.getChunkManager().removeTicket(RTP_TICKET, new ChunkPos(chunkX + dx, chunkZ + dz), 1, ticketId);
            }
        }
    }

    private static int loadedNeighborhoodChunks(ServerWorld world, int chunkX, int chunkZ) {
        int loaded = 0;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                if (world.getChunkManager().getWorldChunk(chunkX + dx, chunkZ + dz) == null) continue;
                ++loaded;
            }
        }
        return loaded;
    }

    private static void updateProgressBar(PendingTeleport pending, ServerWorld world) {
        int desired = Math.min(3, Math.max(1, RtpService.waitingPlayers(pending.target)));
        int total = desired * 9;
        int loaded = Math.min(desired, RtpService.readyChunkCount(world, pending.target)) * 9;
        for (SearchWork work : WORK.values()) {
            if (!work.target.equals(pending.target)) continue;
            loaded += RtpService.loadedNeighborhoodChunks(world, work.chunkX, work.chunkZ);
        }
        loaded = Math.min(total, loaded);
        pending.bar.setPercent(Math.min(1.0f, (float)loaded / (float)total));
        pending.bar.setName(Text.literal(("RTP \u2022 Preparing " + RtpService.displayName(pending.target) + " \u2022 " + loaded + "/" + total + " chunks")));
    }

    private static void removePending(UUID playerId) {
        PendingTeleport removed = PENDING.remove(playerId);
        if (removed != null) {
            removed.bar.clearPlayers();
        }
    }

    private static boolean hasWaitingPlayer(String target) {
        for (PendingTeleport pending : PENDING.values()) {
            if (!pending.target.equals(target) || pending.safeLocation != null) continue;
            return true;
        }
        return false;
    }

    private static int waitingPlayers(String target) {
        int count = 0;
        for (PendingTeleport pending : PENDING.values()) {
            if (!pending.target.equals(target) || pending.safeLocation != null) continue;
            ++count;
        }
        return count;
    }

    private static int activeWorkCount(String target) {
        int count = 0;
        for (SearchWork work : WORK.values()) {
            if (!work.target.equals(target)) continue;
            ++count;
        }
        return count;
    }

    private static int readyChunkCount(ServerWorld world, String target) {
        List<BlockPos> locations = READY.get(target);
        if (locations == null) {
            return 0;
        }
        LinkedHashSet<Long> chunks = new LinkedHashSet<>();
        for (BlockPos pos : locations) {
            if (world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) == null || !ManagedBorderService.contains(world, (double)pos.getX() + 0.5, (double)pos.getZ() + 0.5, 1.0)) continue;
            chunks.add(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));
        }
        return chunks.size();
    }

    private static BlockPos readyLocation(ServerWorld world, String target) {
        List<BlockPos> locations = READY.get(target);
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        HashMap<Long, List<BlockPos>> byChunk = new HashMap<>();
        for (BlockPos pos : locations) {
            if (world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) == null || !ManagedBorderService.contains(world, (double)pos.getX() + 0.5, (double)pos.getZ() + 0.5, 1.0)) continue;
            byChunk.computeIfAbsent(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4), ignored -> new ArrayList<>()).add(pos);
        }
        if (byChunk.isEmpty()) {
            return null;
        }
        ArrayList<Long> chunks = new ArrayList<>(byChunk.keySet());
        chunks.sort(Long::compare);
        int chunkCursor = Math.floorMod(READY_CURSOR.getOrDefault(target, 0), chunks.size());
        long chunk = chunks.get(chunkCursor);
        READY_CURSOR.put(target, chunkCursor + 1);
        List<BlockPos> choices = byChunk.get(chunk);
        String positionKey = target + ":" + chunk;
        int positionCursor = Math.floorMod(READY_CURSOR.getOrDefault(positionKey, 0), choices.size());
        READY_CURSOR.put(positionKey, positionCursor + 1);
        return choices.get(positionCursor);
    }

    private static BlockPos cachedCandidate(ServerWorld world, String target) {
        List<BlockPos> locations = LANDING_CACHE.get(target);
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        int start = ThreadLocalRandom.current().nextInt(locations.size());
        for (int i = 0; i < locations.size(); ++i) {
            BlockPos pos = locations.get((start + i) % locations.size());
            if (!ManagedBorderService.contains(world, (double)pos.getX() + 0.5, (double)pos.getZ() + 0.5, 1.0) || RtpService.chunkInUse(target, pos.getX() >> 4, pos.getZ() >> 4) || !RtpService.farEnoughFromPrepared(target, pos.getX(), pos.getZ())) continue;
            return pos;
        }
        locations.clear();
        cacheDirty = true;
        return null;
    }

    private static Candidate chooseUniqueCandidate(ServerWorld world, String target) {
        for (int i = 0; i < 32; ++i) {
            Candidate candidate = RtpService.chooseCandidate(world);
            if (candidate == null || RtpService.chunkInUse(target, candidate.x >> 4, candidate.z >> 4) || !RtpService.farEnoughFromPrepared(target, candidate.x, candidate.z)) continue;
            return candidate;
        }
        return RtpService.chooseCandidate(world);
    }

    private static boolean farEnoughFromPrepared(String target, int x, int z) {
        double minimumSq = 1000000.0;
        for (SearchWork work : WORK.values()) {
            double dz;
            double dx;
            if (!work.target.equals(target) || !((dx = (double)(x - work.sourceX)) * dx + (dz = (double)(z - work.sourceZ)) * dz < minimumSq)) continue;
            return false;
        }
        List<BlockPos> ready = READY.get(target);
        if (ready != null) {
            for (BlockPos pos : ready) {
                double dz;
                double dx = x - pos.getX();
                if (!(dx * dx + (dz = (double)(z - pos.getZ())) * dz < minimumSq)) continue;
                return false;
            }
        }
        return true;
    }

    private static boolean chunkInUse(String target, int chunkX, int chunkZ) {
        for (SearchWork work : WORK.values()) {
            if (target != null && !work.target.equals(target) || work.chunkX != chunkX || work.chunkZ != chunkZ) continue;
            return true;
        }
        return false;
    }

    private static BlockPos revalidate(ServerWorld world, BlockPos pos) {
        if (world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return null;
        }
        if (!ManagedBorderService.contains(world, (double)pos.getX() + 0.5, (double)pos.getZ() + 0.5, 1.0)) {
            return null;
        }
        return RtpService.inspectSafeLocation(world, pos.getX(), pos.getZ());
    }

    private static List<BlockPos> inspectSafeLocations(ServerWorld world, int chunkX, int chunkZ, int firstX, int firstZ) {
        ArrayList<BlockPos> safe = new ArrayList<>();
        LinkedHashSet<Long> tested = new LinkedHashSet<>();
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        for (int i = 0; i < 24; ++i) {
            BlockPos location;
            int z;
            int x = i == 0 ? firstX : startX + Math.floorMod(i * 7 + 3, 16);
            int n = z = i == 0 ? firstZ : startZ + Math.floorMod(i * 11 + 5, 16);
            if (!tested.add(BlockPos.asLong(x, 0, z)) || !ManagedBorderService.contains(world, (double)x + 0.5, (double)z + 0.5, 1.0) || (location = RtpService.inspectSafeLocation(world, x, z)) == null) continue;
            safe.add(location.toImmutable());
        }
        return safe;
    }

    private static BlockPos inspectSafeLocation(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y < 63 || y > 256) {
            return null;
        }
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos head = feet.up();
        BlockPos ground = feet.down();
        BlockState groundState = world.getBlockState(ground);
        BlockState feetState = world.getBlockState(feet);
        BlockState headState = world.getBlockState(head);
        if (!feetState.getCollisionShape((BlockView)world, feet).isEmpty() || !headState.getCollisionShape((BlockView)world, head).isEmpty()) {
            return null;
        }
        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()) {
            return null;
        }
        if (groundState.isAir() || !groundState.getFluidState().isEmpty() || !groundState.isSolidBlock((BlockView)world, ground)) {
            return null;
        }
        return RtpService.unsafe(groundState) || RtpService.unsafe(feetState) || RtpService.unsafe(headState) ? null : feet;
    }

    private static Candidate chooseCandidate(ServerWorld world) {
        double maxZ;
        double minZ;
        double maxX;
        double minX;
        double originZ;
        double originX;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ManagedBorderService.BorderBounds bounds = ManagedBorderService.safeBounds(world, 32.0);
        if (bounds != null) {
            originX = bounds.centerX();
            originZ = bounds.centerZ();
            minX = Math.max(bounds.minX(), originX - 19000.0);
            maxX = Math.min(bounds.maxX(), originX + 19000.0);
            minZ = Math.max(bounds.minZ(), originZ - 19000.0);
            maxZ = Math.min(bounds.maxZ(), originZ + 19000.0);
        } else {
            BlockPos spawn = world.getSpawnPos();
            originX = (double)spawn.getX() + 0.5;
            originZ = (double)spawn.getZ() + 0.5;
            minX = originX - 19000.0;
            maxX = originX + 19000.0;
            minZ = originZ - 19000.0;
            maxZ = originZ + 19000.0;
        }
        int blockMinX = (int)Math.ceil(minX - 0.5);
        int blockMaxX = (int)Math.floor(maxX - 0.5);
        int blockMinZ = (int)Math.ceil(minZ - 0.5);
        int blockMaxZ = (int)Math.floor(maxZ - 0.5);
        if (blockMinX > blockMaxX || blockMinZ > blockMaxZ) {
            return null;
        }
        double farX = Math.max(Math.abs((double)blockMinX + 0.5 - originX), Math.abs((double)blockMaxX + 0.5 - originX));
        double farZ = Math.max(Math.abs((double)blockMinZ + 0.5 - originZ), Math.abs((double)blockMaxZ + 0.5 - originZ));
        double effectiveMax = Math.min(19000.0, Math.sqrt(farX * farX + farZ * farZ));
        double effectiveMin = Math.min(500.0, effectiveMax * 0.45);
        for (int i = 0; i < 96; ++i) {
            int z;
            double dz;
            int x = RtpService.nextIntInclusive(random, blockMinX, blockMaxX);
            double dx = (double)x + 0.5 - originX;
            double distance = dx * dx + (dz = (double)(z = RtpService.nextIntInclusive(random, blockMinZ, blockMaxZ)) + 0.5 - originZ) * dz;
            if (!(distance >= effectiveMin * effectiveMin) || !(distance <= effectiveMax * effectiveMax)) continue;
            return new Candidate(x, z);
        }
        return new Candidate(RtpService.nextIntInclusive(random, blockMinX, blockMaxX), RtpService.nextIntInclusive(random, blockMinZ, blockMaxZ));
    }

    private static int nextIntInclusive(ThreadLocalRandom random, int min, int max) {
        return min == max ? min : (int)random.nextLong(min, (long)max + 1L);
    }

    private static void cacheLanding(String target, BlockPos position) {
        BlockPos immutable;
        List<BlockPos> locations = LANDING_CACHE.computeIfAbsent(target, ignored -> new ArrayList<>());
        if (locations.contains(immutable = position.toImmutable())) {
            return;
        }
        if (locations.size() >= 256) {
            locations.remove(0);
        }
        locations.add(immutable);
        cacheDirty = true;
    }

    private static void discardLanding(String target, BlockPos position) {
        List<BlockPos> cached;
        List<BlockPos> ready = READY.get(target);
        if (ready != null) {
            ready.remove(position);
        }
        if ((cached = LANDING_CACHE.get(target)) != null && cached.remove(position)) {
            cacheDirty = true;
        }
    }

    private static void discardChunk(String target, int chunkX, int chunkZ) {
        List<BlockPos> cached = LANDING_CACHE.get(target);
        if (cached != null && cached.removeIf(pos -> pos.getX() >> 4 == chunkX && pos.getZ() >> 4 == chunkZ)) {
            cacheDirty = true;
        }
    }

    private static void ensureCacheLoaded(MinecraftServer server) {
        if (cacheLoaded || server == null) {
            return;
        }
        cacheLoaded = true;
        cachePath = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("rtp-landings.json");
        try {
            if (!RtpService.loadCache(cachePath)) {
                RtpService.loadCache(cachePath.resolveSibling("rtp-landings.json.bak"));
            }
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not read the RTP landing cache; starting empty", error);
            LANDING_CACHE.clear();
        }
    }

    private static boolean loadCache(Path source) throws IOException {
        if (source == null || !Files.exists(source)) {
            return false;
        }
        LandingCacheFile file = GSON.fromJson(Files.readString(source, StandardCharsets.UTF_8), LandingCacheFile.class);
        if (file == null || file.worlds == null) {
            return false;
        }
        for (Map.Entry<String, List<LandingData>> row : file.worlds.entrySet()) {
            String target = ManagedWorldService.normalize(row.getKey());
            if (target == null || row.getValue() == null) continue;
            ArrayList<BlockPos> locations = new ArrayList<>();
            for (LandingData landing : row.getValue()) {
                if (landing == null || locations.size() >= 256) break;
                locations.add(new BlockPos(landing.x, landing.y, landing.z));
            }
            if (locations.isEmpty()) continue;
            LANDING_CACHE.put(target, locations);
        }
        return true;
    }

    private static void saveCache() {
        if (cachePath == null || !cacheDirty) {
            return;
        }
        try {
            Files.createDirectories(cachePath.getParent());
            LandingCacheFile file = new LandingCacheFile();
            for (Map.Entry<String, List<BlockPos>> row : LANDING_CACHE.entrySet()) {
                ArrayList<LandingData> data = new ArrayList<>();
                for (BlockPos pos : row.getValue()) {
                    data.add(new LandingData(pos.getX(), pos.getY(), pos.getZ()));
                }
                file.worlds.put(row.getKey(), data);
            }
            Path temp = cachePath.resolveSibling("rtp-landings.json.tmp");
            Path backup = cachePath.resolveSibling("rtp-landings.json.bak");
            Files.writeString(temp, GSON.toJson(file), StandardCharsets.UTF_8);
            if (Files.exists(cachePath)) {
                Files.copy(cachePath, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, cachePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException ignored) {
                Files.move(temp, cachePath, StandardCopyOption.REPLACE_EXISTING);
            }
            cacheDirty = false;
        }
        catch (IOException error) {
            CobbleClubServer.LOGGER.error("Could not save the RTP landing cache", error);
        }
    }

    private static long cooldownSeconds(ServerPlayerEntity player) {
        long rankReduction = switch (RankAccessService.premiumRank(player)) {
            case "ace" -> 30L;
            case "champion" -> 60L;
            case "master" -> 90L;
            case "legend" -> 120L;
            default -> 0L;
        };
        long dexReduction = RtpService.pokedexCooldownReduction(player);
        return Math.max(1L, BASE_COOLDOWN_SECONDS - rankReduction - dexReduction);
    }

    private static long pokedexCooldownReduction(ServerPlayerEntity player) {
        try {
            PokedexManager pokedex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
            int total = PokemonSpecies.INSTANCE.getImplemented().size();
            if (total <= 0) {
                return 0L;
            }
            int caught = 0;
            for (var species : PokemonSpecies.INSTANCE.getImplemented()) {
                if (pokedex.getHighestKnowledgeForSpecies(species.getResourceIdentifier()) == PokedexEntryProgress.CAUGHT) {
                    ++caught;
                }
            }
            if (caught >= total) {
                return 20L;
            }
            return (long)caught * 2L >= (long)total ? 10L : 0L;
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not read Pokédex completion for RTP cooldown bonus for {}", player.getGameProfile().getName(), error);
            return 0L;
        }
    }

    private static boolean teleportPrepared(ServerPlayerEntity player, String target, ServerWorld world, BlockPos safe) {
        player.setVelocity(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        player.teleport(world, (double)safe.getX() + 0.5, (double)safe.getY(), (double)safe.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.setVelocity(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        world.playSound(null, safe, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.9f, 1.1f);
        RtpService.showEffect(player, target);
        player.sendMessage(Text.literal("\u2726 RTP \u2022 ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD).append(Text.literal("Welcome to " + RtpService.displayName(target) + ".").formatted(RtpService.colorFor(target), Formatting.BOLD)), false);
        return true;
    }

    private static boolean unsafe(BlockState state) {
        return state.isOf(Blocks.LAVA) || state.isOf(Blocks.WATER) || state.isOf(Blocks.MAGMA_BLOCK) || state.isOf(Blocks.CACTUS) || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE) || state.isOf(Blocks.POWDER_SNOW) || state.isOf(Blocks.SWEET_BERRY_BUSH) || state.isOf(Blocks.WITHER_ROSE) || state.isOf(Blocks.POINTED_DRIPSTONE) || state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE);
    }

    private static ServerWorld findWorld(MinecraftServer server, String target) {
        return ManagedWorldService.getWorld(server, target);
    }

    private static String normalizeTarget(String requested) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase("random")) {
            return RANDOM_WORLDS.get(ThreadLocalRandom.current().nextInt(RANDOM_WORLDS.size()));
        }
        return ManagedWorldService.normalize(requested);
    }

    private static String displayName(String target) {
        return ManagedWorldService.displayName(target);
    }

    private static Formatting colorFor(String target) {
        return switch (target) {
            case "redworld" -> Formatting.RED;
            case "yellowworld" -> Formatting.YELLOW;
            case "blueworld", "cyanworld" -> Formatting.AQUA;
            case "greenworld" -> Formatting.GREEN;
            case "purpleworld" -> Formatting.DARK_PURPLE;
            case "pinkworld" -> Formatting.LIGHT_PURPLE;
            case "orangeworld" -> Formatting.GOLD;
            default -> Formatting.WHITE;
        };
    }

    private static ParticleEffect particleFor(String target) {
        return switch (target) {
            case "redworld", "orangeworld" -> ParticleTypes.FLAME;
            case "yellowworld" -> ParticleTypes.WAX_ON;
            case "blueworld", "cyanworld" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "greenworld" -> ParticleTypes.HAPPY_VILLAGER;
            case "purpleworld", "pinkworld" -> ParticleTypes.REVERSE_PORTAL;
            default -> ParticleTypes.PORTAL;
        };
    }

    private static void showEffect(ServerPlayerEntity player, String target) {
        ServerWorld world = player.getServerWorld();
        world.spawnParticles(RtpService.particleFor(target), player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.45, 0.75, 0.45, 0.02);
    }

    static {
        nextTicketId = 1;
    }

    private static final class PendingTeleport {
        final String target;
        final String startDimension;
        final double startX;
        final double startY;
        final double startZ;
        final long executeAtTick;
        final long searchDeadline;
        final ServerBossBar bar;
        BlockPos safeLocation;
        boolean waitingMessageSent;

        PendingTeleport(String target, String dimension, double x, double y, double z, long execute, long deadline) {
            this.target = target;
            this.startDimension = dimension;
            this.startX = x;
            this.startY = y;
            this.startZ = z;
            this.executeAtTick = execute;
            this.searchDeadline = deadline;
            this.bar = new ServerBossBar(Text.literal(("RTP \u2022 Preparing " + RtpService.displayName(target) + " \u2022 0/9 chunks")), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
            this.bar.setPercent(0.0f);
        }
    }

    private static final class SearchWork {
        final String target;
        final int ticketId;
        int attempts;
        int sourceX;
        int sourceZ;
        int chunkX;
        int chunkZ;
        long startedAt;

        SearchWork(String target, int ticketId) {
            this.target = target;
            this.ticketId = ticketId;
        }
    }

    private record HeldTicket(String target, int chunkX, int chunkZ, int ticketId, long releaseAt) {
    }

    private record Candidate(int x, int z) {
    }

    private static final class LandingCacheFile {
        Map<String, List<LandingData>> worlds = new HashMap<>();

        private LandingCacheFile() {
        }
    }

    private static final class LandingData {
        int x;
        int y;
        int z;

        LandingData(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
