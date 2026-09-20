/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2186
 *  net.minecraft.class_2231
 *  net.minecraft.class_2338
 *  net.minecraft.class_239
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_2596
 *  net.minecraft.class_2743
 *  net.minecraft.class_3222
 *  net.minecraft.class_3532
 *  net.minecraft.class_3965
 *  net.minecraft.class_5218
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.service.PermissionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

public final class LaunchService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PAD_RETRIGGER_TICKS = 10;
    private static final Map<PadKey, LaunchPad> PADS = new HashMap<PadKey, LaunchPad>();
    private static final Map<UUID, PadKey> ACTIVE_PADS = new HashMap<UUID, PadKey>();
    private static final Map<UUID, Integer> NEXT_ALLOWED_TICK = new HashMap<UUID, Integer>();
    private static Path path;

    private LaunchService() {
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"launch").requires(source -> PermissionService.admin(source, "cobbleclub.admin.launch", 2))).then(CommandManager.argument((String)"targets", (ArgumentType)EntityArgumentType.players()).then(CommandManager.argument((String)"forward", (ArgumentType)FloatArgumentType.floatArg((float)-3.9f, (float)3.9f)).then(((RequiredArgumentBuilder)CommandManager.argument((String)"up", (ArgumentType)FloatArgumentType.floatArg((float)-3.9f, (float)3.9f)).executes(context -> LaunchService.launch((ServerCommandSource)context.getSource(), EntityArgumentType.getPlayers((CommandContext)context, (String)"targets"), FloatArgumentType.getFloat((CommandContext)context, (String)"forward"), FloatArgumentType.getFloat((CommandContext)context, (String)"up"), null))).then(CommandManager.argument((String)"yaw", (ArgumentType)FloatArgumentType.floatArg((float)-3600.0f, (float)3600.0f)).executes(context -> LaunchService.launch((ServerCommandSource)context.getSource(), EntityArgumentType.getPlayers((CommandContext)context, (String)"targets"), FloatArgumentType.getFloat((CommandContext)context, (String)"forward"), FloatArgumentType.getFloat((CommandContext)context, (String)"up"), Float.valueOf(FloatArgumentType.getFloat((CommandContext)context, (String)"yaw")))))))));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"launchpad").requires(source -> PermissionService.admin(source, "cobbleclub.admin.launch", 2))).then(CommandManager.literal((String)"set").then(CommandManager.argument((String)"forward", (ArgumentType)FloatArgumentType.floatArg((float)-3.9f, (float)3.9f)).then(((RequiredArgumentBuilder)CommandManager.argument((String)"up", (ArgumentType)FloatArgumentType.floatArg((float)-3.9f, (float)3.9f)).executes(context -> LaunchService.setPad((ServerCommandSource)context.getSource(), FloatArgumentType.getFloat((CommandContext)context, (String)"forward"), FloatArgumentType.getFloat((CommandContext)context, (String)"up"), null))).then(CommandManager.argument((String)"yaw", (ArgumentType)FloatArgumentType.floatArg((float)-3600.0f, (float)3600.0f)).executes(context -> LaunchService.setPad((ServerCommandSource)context.getSource(), FloatArgumentType.getFloat((CommandContext)context, (String)"forward"), FloatArgumentType.getFloat((CommandContext)context, (String)"up"), Float.valueOf(FloatArgumentType.getFloat((CommandContext)context, (String)"yaw"))))))))).then(CommandManager.literal((String)"remove").executes(context -> LaunchService.removePad((ServerCommandSource)context.getSource())))).then(CommandManager.literal((String)"info").executes(context -> LaunchService.padInfo((ServerCommandSource)context.getSource())))).then(CommandManager.literal((String)"list").executes(context -> LaunchService.listPads((ServerCommandSource)context.getSource()))));
    }

    public static void initialize(MinecraftServer server) {
        path = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("launch-pads.json");
        PADS.clear();
        ACTIVE_PADS.clear();
        NEXT_ALLOWED_TICK.clear();
        try {
            LaunchService.loadFrom(path);
        }
        catch (Exception primaryFailure) {
            CobbleClubServer.LOGGER.warn("Could not read launch-pads.json; trying the backup", (Throwable)primaryFailure);
            try {
                LaunchService.loadFrom(path.resolveSibling("launch-pads.json.bak"));
                CobbleClubServer.LOGGER.warn("Recovered pressure-plate launch pads from launch-pads.json.bak");
            }
            catch (Exception backupFailure) {
                CobbleClubServer.LOGGER.error("Could not read launch-pads.json or its backup", (Throwable)backupFailure);
                PADS.clear();
            }
        }
        CobbleClubServer.LOGGER.info("Loaded {} pressure-plate launch pads", (Object)PADS.size());
    }

    public static void shutdown() {
        LaunchService.save();
        PADS.clear();
        ACTIVE_PADS.clear();
        NEXT_ALLOWED_TICK.clear();
        path = null;
    }

    public static void tick(MinecraftServer server) {
        if (PADS.isEmpty()) {
            return;
        }
        int tick = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PadMatch match = LaunchService.padUnder(player);
            UUID playerId = player.getUuid();
            if (match == null) {
                ACTIVE_PADS.remove(playerId);
                continue;
            }
            PadKey previous = ACTIVE_PADS.get(playerId);
            if (match.key.equals(previous) || tick < NEXT_ALLOWED_TICK.getOrDefault(playerId, 0)) continue;
            ACTIVE_PADS.put(playerId, match.key);
            NEXT_ALLOWED_TICK.put(playerId, tick + 10);
            LaunchService.applyLaunch(player, match.pad.forward, match.pad.up, match.pad.yaw);
        }
    }

    public static void forget(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        ACTIVE_PADS.remove(playerId);
        NEXT_ALLOWED_TICK.remove(playerId);
    }

    private static int launch(ServerCommandSource source, Collection<ServerPlayerEntity> targets, float forward, float up, Float fixedYaw) {
        if (targets == null || targets.isEmpty()) {
            source.sendError((Text)Text.literal((String)"No players matched that selector."));
            return 0;
        }
        for (ServerPlayerEntity player : targets) {
            LaunchService.applyLaunch(player, forward, up, fixedYaw != null ? fixedYaw.floatValue() : player.getYaw());
        }
        if (targets.size() == 1) {
            ServerPlayerEntity player = targets.iterator().next();
            source.sendFeedback(() -> Text.literal((String)("Launched " + player.getGameProfile().getName() + " (forward " + forward + ", up " + up + (String)(fixedYaw == null ? ", using player facing" : ", yaw " + fixedYaw) + ").")), false);
        } else {
            source.sendFeedback(() -> Text.literal((String)("Launched " + targets.size() + " players.")), false);
        }
        return targets.size();
    }

    private static void applyLaunch(ServerPlayerEntity player, float forward, float up, float yaw) {
        double radians = Math.toRadians(yaw);
        double x = -Math.sin(radians) * (double)forward;
        double z = Math.cos(radians) * (double)forward;
        Vec3d velocity = new Vec3d(x, (double)up, z);
        player.setVelocity(velocity);
        player.networkHandler.sendPacket((Packet)new EntityVelocityUpdateS2CPacket(player.getId(), velocity));
        player.fallDistance = 0.0f;
    }

    private static int setPad(ServerCommandSource source, float forward, float up, Float requestedYaw) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        }
        catch (Exception error) {
            source.sendError((Text)Text.literal((String)"A player must stand on or look at the pressure plate to assign it."));
            return 0;
        }
        BlockPos pos = LaunchService.selectedPressurePlate(player);
        if (pos == null) {
            source.sendError((Text)Text.literal((String)"Stand on or look directly at a pressure plate within 6 blocks."));
            return 0;
        }
        float yaw = requestedYaw != null ? requestedYaw.floatValue() : MathHelper.wrapDegrees((float)player.getYaw());
        String dimension = LaunchService.dimension(player);
        PadKey key = new PadKey(dimension, pos.asLong());
        PADS.put(key, new LaunchPad(forward, up, yaw));
        ACTIVE_PADS.values().removeIf(key::equals);
        LaunchService.save();
        source.sendFeedback(() -> Text.literal((String)("Launch pad assigned at " + LaunchService.coordinates(pos) + ": forward " + LaunchService.number(forward) + ", up " + LaunchService.number(up) + ", yaw " + LaunchService.number(yaw) + ".")), false);
        return 1;
    }

    private static int removePad(ServerCommandSource source) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        }
        catch (Exception error) {
            source.sendError((Text)Text.literal((String)"A player must stand on or look at the launch pad to remove it."));
            return 0;
        }
        PadKey key = LaunchService.selectedAssignedPad(player);
        if (key == null || PADS.remove(key) == null) {
            source.sendError((Text)Text.literal((String)"Stand on or look directly at an assigned launch pad within 6 blocks."));
            return 0;
        }
        ACTIVE_PADS.values().removeIf(key::equals);
        LaunchService.save();
        BlockPos pos = BlockPos.fromLong((long)key.pos);
        source.sendFeedback(() -> Text.literal((String)("Removed launch pad at " + LaunchService.coordinates(pos) + ".")), false);
        return 1;
    }

    private static int padInfo(ServerCommandSource source) {
        LaunchPad pad;
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        }
        catch (Exception error) {
            source.sendError((Text)Text.literal((String)"A player must stand on or look at the launch pad."));
            return 0;
        }
        PadKey key = LaunchService.selectedAssignedPad(player);
        LaunchPad launchPad = pad = key == null ? null : PADS.get(key);
        if (pad == null) {
            source.sendError((Text)Text.literal((String)"Stand on or look directly at an assigned launch pad within 6 blocks."));
            return 0;
        }
        BlockPos pos = BlockPos.fromLong((long)key.pos);
        source.sendFeedback(() -> Text.literal((String)("Launch pad at " + LaunchService.coordinates(pos) + ": forward " + LaunchService.number(pad.forward) + ", up " + LaunchService.number(pad.up) + ", yaw " + LaunchService.number(pad.yaw) + ".")), false);
        return 1;
    }

    private static int listPads(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal((String)("Assigned pressure-plate launch pads: " + PADS.size() + ".")), false);
        return PADS.size();
    }

    private static PadMatch padUnder(ServerPlayerEntity player) {
        BlockPos feet;
        String dimension = LaunchService.dimension(player);
        PadMatch atFeet = LaunchService.match(player, dimension, feet = player.getBlockPos());
        return atFeet != null ? atFeet : LaunchService.match(player, dimension, feet.down());
    }

    private static PadMatch match(ServerPlayerEntity player, String dimension, BlockPos pos) {
        PadKey key = new PadKey(dimension, pos.asLong());
        LaunchPad pad = PADS.get(key);
        if (pad == null || !(player.getWorld().getBlockState(pos).getBlock() instanceof PressurePlateBlock)) {
            return null;
        }
        return new PadMatch(key, pad);
    }

    private static BlockPos selectedPressurePlate(ServerPlayerEntity player) {
        BlockHitResult blockHit;
        BlockPos feet = player.getBlockPos();
        if (LaunchService.isPressurePlate(player, feet)) {
            return feet.toImmutable();
        }
        if (LaunchService.isPressurePlate(player, feet.down())) {
            return feet.down().toImmutable();
        }
        HitResult hit = player.raycast(6.0, 0.0f, false);
        if (hit instanceof BlockHitResult && LaunchService.isPressurePlate(player, (blockHit = (BlockHitResult)hit).getBlockPos())) {
            return blockHit.getBlockPos().toImmutable();
        }
        return null;
    }

    private static PadKey selectedAssignedPad(ServerPlayerEntity player) {
        BlockHitResult blockHit;
        BlockPos feet;
        String dimension = LaunchService.dimension(player);
        PadKey key = new PadKey(dimension, (feet = player.getBlockPos()).asLong());
        if (PADS.containsKey(key)) {
            return key;
        }
        key = new PadKey(dimension, feet.down().asLong());
        if (PADS.containsKey(key)) {
            return key;
        }
        HitResult hit = player.raycast(6.0, 0.0f, false);
        if (hit instanceof BlockHitResult && PADS.containsKey(key = new PadKey(dimension, (blockHit = (BlockHitResult)hit).getBlockPos().asLong()))) {
            return key;
        }
        return null;
    }

    private static boolean isPressurePlate(ServerPlayerEntity player, BlockPos pos) {
        return player.getWorld().getBlockState(pos).getBlock() instanceof PressurePlateBlock;
    }

    private static String dimension(ServerPlayerEntity player) {
        return player.getServerWorld().getRegistryKey().getValue().toString();
    }

    private static boolean valid(LaunchPadData row) {
        return row != null && row.dimension != null && !row.dimension.isBlank() && Float.isFinite(row.forward) && row.forward >= -3.9f && row.forward <= 3.9f && Float.isFinite(row.up) && row.up >= -3.9f && row.up <= 3.9f && Float.isFinite(row.yaw) && row.yaw >= -3600.0f && row.yaw <= 3600.0f;
    }

    private static void loadFrom(Path source) throws IOException {
        if (source == null || !Files.exists(source, new LinkOption[0])) {
            return;
        }
        LaunchPadFile file = (LaunchPadFile)GSON.fromJson(Files.readString(source, StandardCharsets.UTF_8), LaunchPadFile.class);
        if (file == null || file.pads == null) {
            throw new IOException("Launch pad file has no pad list");
        }
        PADS.clear();
        for (LaunchPadData row : file.pads) {
            if (!LaunchService.valid(row)) continue;
            PadKey key = new PadKey(row.dimension, BlockPos.asLong((int)row.x, (int)row.y, (int)row.z));
            PADS.put(key, new LaunchPad(row.forward, row.up, row.yaw));
        }
    }

    private static void save() {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            LaunchPadFile file = new LaunchPadFile();
            file.pads = new ArrayList<LaunchPadData>(PADS.size());
            for (Map.Entry<PadKey, LaunchPad> entry : PADS.entrySet()) {
                BlockPos pos = BlockPos.fromLong((long)entry.getKey().pos);
                LaunchPad pad = entry.getValue();
                file.pads.add(new LaunchPadData(entry.getKey().dimension, pos.getX(), pos.getY(), pos.getZ(), pad.forward, pad.up, pad.yaw));
            }
            file.pads.sort((left, right) -> {
                int byDimension = left.dimension.compareTo(right.dimension);
                if (byDimension != 0) {
                    return byDimension;
                }
                int byX = Integer.compare(left.x, right.x);
                if (byX != 0) {
                    return byX;
                }
                int byY = Integer.compare(left.y, right.y);
                return byY != 0 ? byY : Integer.compare(left.z, right.z);
            });
            Path temp = path.resolveSibling("launch-pads.json.tmp");
            Path backup = path.resolveSibling("launch-pads.json.bak");
            Files.writeString(temp, (CharSequence)GSON.toJson((Object)file), StandardCharsets.UTF_8, new OpenOption[0]);
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
        catch (IOException error) {
            CobbleClubServer.LOGGER.error("Could not save pressure-plate launch pads to {}", (Object)path, (Object)error);
        }
    }

    private static String coordinates(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.2f", Float.valueOf(value)).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private record PadMatch(PadKey key, LaunchPad pad) {
    }

    private record PadKey(String dimension, long pos) {
    }

    private record LaunchPad(float forward, float up, float yaw) {
    }

    private static final class LaunchPadData {
        String dimension;
        int x;
        int y;
        int z;
        float forward;
        float up;
        float yaw;

        LaunchPadData(String dimension, int x, int y, int z, float forward, float up, float yaw) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.forward = forward;
            this.up = up;
            this.yaw = yaw;
        }
    }

    private static final class LaunchPadFile {
        int version = 1;
        List<LaunchPadData> pads = new ArrayList<LaunchPadData>();

        private LaunchPadFile() {
        }
    }
}

