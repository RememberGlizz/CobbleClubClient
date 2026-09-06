package com.cobbleclub.server.service;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** CobbleClub instant /rtp command sharing the same world/radius/surface rules as /wild. */
public final class RtpService {
    private static final int MIN_RADIUS = 500;
    private static final int MAX_RADIUS = 5000;
    private static final int MIN_Y = 63;
    private static final int MAX_Y = 256;
    private static final long WARMUP_TICKS = 60L;
    private static final long COOLDOWN_TICKS = 6000L;
    private static final int MAX_ATTEMPTS = 48;

    private static final List<String> RANDOM_WORLDS = List.of("redworld", "yellowworld", "blueworld", "greenworld");
    private static final Map<UUID, PendingTeleport> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static long serverTick;

    private RtpService() {}

    public static int start(ServerPlayerEntity player, String requested) {
        if (player == null) return 0;
        String target = normalizeTarget(requested);
        if (target == null) {
            player.sendMessage(Text.literal("Usage: /rtp [red|yellow|blue|green|resource]").formatted(Formatting.RED), false);
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

        ServerWorld world = findWorld(player.getServer(), target);
        if (world == null) {
            player.sendMessage(Text.literal("That CobbleClub world is currently unavailable.").formatted(Formatting.RED), false);
            return 0;
        }

        PendingTeleport pending = new PendingTeleport(
                target,
                player.getServerWorld().getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(),
                serverTick + WARMUP_TICKS
        );
        PENDING.put(player.getUuid(), pending);
        player.sendMessage(Text.literal("✦ RTP • ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                .append(Text.literal("Teleporting to " + displayName(target) + " in 3 seconds — don't move.").formatted(colorFor(target))), false);
        showEffect(player, target);
        return 1;
    }

    public static void tick(MinecraftServer server) {
        serverTick++;
        if (server == null || PENDING.isEmpty()) return;

        for (Map.Entry<UUID, PendingTeleport> entry : List.copyOf(PENDING.entrySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                PENDING.remove(entry.getKey());
                continue;
            }
            PendingTeleport pending = entry.getValue();
            String currentDim = player.getServerWorld().getRegistryKey().getValue().toString();
            double dx = player.getX() - pending.x;
            double dy = player.getY() - pending.y;
            double dz = player.getZ() - pending.z;
            if (!currentDim.equals(pending.startDimension) || dx * dx + dy * dy + dz * dz > 0.04D) {
                PENDING.remove(player.getUuid());
                player.sendMessage(Text.literal("RTP cancelled because you moved.").formatted(Formatting.RED), false);
                continue;
            }

            long remaining = pending.executeAtTick - serverTick;
            if (remaining > 0L) {
                if (remaining == 40L || remaining == 20L) {
                    int seconds = (int)(remaining / 20L);
                    player.sendMessage(Text.literal("Teleporting to " + displayName(pending.target) + " in " + seconds + "...").formatted(colorFor(pending.target)), true);
                    showEffect(player, pending.target);
                }
                continue;
            }

            PENDING.remove(player.getUuid());
            if (teleport(player, pending.target)) {
                COOLDOWNS.put(player.getUuid(), serverTick + COOLDOWN_TICKS);
            }
        }
    }

    public static void forget(ServerPlayerEntity player) {
        if (player != null) PENDING.remove(player.getUuid());
    }

    private static boolean teleport(ServerPlayerEntity player, String target) {
        ServerWorld world = findWorld(player.getServer(), target);
        if (world == null) {
            player.sendMessage(Text.literal("That CobbleClub world is currently unavailable.").formatted(Formatting.RED), false);
            return false;
        }

        BlockPos safe = findSafeLocation(world);
        if (safe == null) {
            player.sendMessage(Text.literal("No safe RTP location was found. Please try again.").formatted(Formatting.RED), false);
            return false;
        }

        player.teleport(world, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, player.getYaw(), player.getPitch());
        world.playSound(null, safe, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.9F, 1.1F);
        showEffect(player, target);
        player.sendMessage(Text.literal("✦ RTP • ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                .append(Text.literal("Welcome to " + displayName(target) + ".").formatted(colorFor(target), Formatting.BOLD)), false);
        return true;
    }

    private static BlockPos findSafeLocation(ServerWorld world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = random.nextDouble(Math.PI * 2.0D);
            double distance = Math.sqrt(random.nextDouble((double)MIN_RADIUS * MIN_RADIUS, (double)MAX_RADIUS * MAX_RADIUS));
            int x = (int)Math.round(Math.cos(angle) * distance);
            int z = (int)Math.round(Math.sin(angle) * distance);

            // Force the target chunk to load before querying the surface height.
            world.getChunk(x >> 4, z >> 4);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < MIN_Y || y > MAX_Y) continue;

            BlockPos feet = new BlockPos(x, y, z);
            BlockPos head = feet.up();
            BlockPos ground = feet.down();
            BlockState groundState = world.getBlockState(ground);
            BlockState feetState = world.getBlockState(feet);
            BlockState headState = world.getBlockState(head);
            if (!feetState.isAir() || !headState.isAir()) continue;
            if (groundState.isAir() || !groundState.getFluidState().isEmpty()) continue;
            if (unsafe(groundState) || unsafe(feetState) || unsafe(headState)) continue;
            return feet;
        }
        return null;
    }

    private static boolean unsafe(BlockState state) {
        return state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.WATER)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE);
    }

    private static ServerWorld findWorld(MinecraftServer server, String target) {
        if (server == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            String path = world.getRegistryKey().getValue().getPath().toLowerCase(Locale.ROOT);
            if (path.equals(target)) return world;
            if (target.equals("resource") && (path.equals("resources") || path.equals("resourceworld"))) return world;
        }
        return null;
    }

    private static String normalizeTarget(String requested) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase("random")) {
            return RANDOM_WORLDS.get(ThreadLocalRandom.current().nextInt(RANDOM_WORLDS.size()));
        }
        String target = requested.toLowerCase(Locale.ROOT);
        return switch (target) {
            case "red", "redworld" -> "redworld";
            case "yellow", "yellowworld" -> "yellowworld";
            case "blue", "blueworld" -> "blueworld";
            case "green", "greenworld" -> "greenworld";
            case "resource", "resources", "resourceworld" -> "resource";
            default -> null;
        };
    }

    private static String displayName(String target) {
        return switch (target) {
            case "redworld" -> "Red World";
            case "yellowworld" -> "Yellow World";
            case "blueworld" -> "Blue World";
            case "greenworld" -> "Green World";
            case "resource" -> "Resource World";
            default -> target;
        };
    }

    private static Formatting colorFor(String target) {
        return switch (target) {
            case "redworld" -> Formatting.RED;
            case "yellowworld" -> Formatting.YELLOW;
            case "blueworld" -> Formatting.AQUA;
            case "greenworld" -> Formatting.GREEN;
            default -> Formatting.GOLD;
        };
    }

    private static ParticleEffect particleFor(String target) {
        return switch (target) {
            case "redworld" -> ParticleTypes.FLAME;
            case "yellowworld" -> ParticleTypes.WAX_ON;
            case "blueworld" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "greenworld" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.PORTAL;
        };
    }

    private static void showEffect(ServerPlayerEntity player, String target) {
        ServerWorld world = player.getServerWorld();
        world.spawnParticles(particleFor(target), player.getX(), player.getY() + 1.0D, player.getZ(), 14, 0.45D, 0.75D, 0.45D, 0.02D);
    }

    private record PendingTeleport(String target, String startDimension, double x, double y, double z, long executeAtTick) {}
}
