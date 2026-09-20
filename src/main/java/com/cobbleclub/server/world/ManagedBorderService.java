/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_124
 *  net.minecraft.class_2338
 *  net.minecraft.class_2561
 *  net.minecraft.class_2784
 *  net.minecraft.class_2960
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_5218
 *  net.minecraft.class_8710
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.world;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.network.Payloads;
import com.cobbleclub.server.world.ManagedWorldService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

public final class ManagedBorderService {
    public static final double DEFAULT_SIZE = 40000.0;
    private static final double MIN_SIZE = 2.0;
    private static final double MAX_SIZE = 5.9999968E7;
    private static final double EDGE_EPSILON = 0.05;
    private static final String OVERWORLD = "minecraft:overworld";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, BorderEntry> BORDERS = new LinkedHashMap<String, BorderEntry>();
    private static Path statePath;
    private static long ticks;

    private ManagedBorderService() {
    }

    public static void initialize(MinecraftServer server) {
        ticks = 0L;
        statePath = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("managed-borders.json");
        ManagedBorderService.load();
        ManagedBorderService.ensureDefaults(server);
        if (BORDERS.containsKey(OVERWORLD)) {
            ManagedBorderService.neutralizeVanillaSharedBorder(server);
        }
        ManagedBorderService.save();
        CobbleClubServer.LOGGER.info("Managed border service initialized for {} configured worlds.", (Object)BORDERS.size());
    }

    public static void shutdown() {
        ManagedBorderService.save();
        BORDERS.clear();
        statePath = null;
        ticks = 0L;
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (++ticks % 200L == 0L) {
            ManagedBorderService.ensureDefaults(server);
        }
        if (BORDERS.containsKey(OVERWORLD) && ticks % 100L == 0L) {
            ManagedBorderService.neutralizeVanillaSharedBorder(server);
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ManagedBorderService.enforce(player);
        }
    }

    public static void sync(ServerPlayerEntity player) {
        BorderEntry border;
        if (player == null || !ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.ManagedBorderState.ID)) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        String worldId = world.getRegistryKey().getValue().toString();
        String target = ManagedBorderService.targetFor(world);
        BorderEntry borderEntry = border = target == null ? null : BORDERS.get(target);
        if (target == null || border == null) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.ManagedBorderState(worldId, false, false, 0.0, 0.0, 0.0));
            return;
        }
        if (!border.enabled) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.ManagedBorderState(worldId, true, false, 0.0, 0.0, 5.9999968E7));
            return;
        }
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.ManagedBorderState(worldId, true, true, border.centerX, border.centerZ, border.size));
    }

    public static int get(MinecraftServer server, String input, Consumer<Text> feedback) {
        String target = ManagedBorderService.normalizeTarget(input);
        if (target == null) {
            return ManagedBorderService.unknown(feedback);
        }
        if (OVERWORLD.equals(target) && !BORDERS.containsKey(OVERWORLD)) {
            WorldBorder vanilla = server.getOverworld().getWorldBorder();
            feedback.accept((Text)Text.literal((String)String.format(Locale.ROOT, "Minecraft Overworld is still using the vanilla border | size %.0f x %.0f | center %.1f, %.1f. Use /cobbleclubserver border minecraft:overworld set <size>, center ..., or reset to move it into the CobbleClub border system.", vanilla.getSize(), vanilla.getSize(), vanilla.getCenterX(), vanilla.getCenterZ())).formatted(Formatting.AQUA));
            return 1;
        }
        BorderEntry border = ManagedBorderService.ensureFor(server, target);
        if (border == null) {
            return ManagedBorderService.unavailable(target, feedback);
        }
        feedback.accept((Text)Text.literal((String)String.format(Locale.ROOT, "%s border: %s | size %.0f x %.0f | center %.1f, %.1f | dimension %s", ManagedBorderService.displayName(target), border.enabled ? "ON" : "OFF", border.size, border.size, border.centerX, border.centerZ, ManagedBorderService.resolvedId(target))).formatted(Formatting.AQUA));
        return 1;
    }

    public static int setSize(MinecraftServer server, String input, double size, Consumer<Text> feedback) {
        String target = ManagedBorderService.normalizeTarget(input);
        if (target == null) {
            return ManagedBorderService.unknown(feedback);
        }
        BorderEntry border = ManagedBorderService.ensureFor(server, target);
        if (border == null) {
            return ManagedBorderService.unavailable(target, feedback);
        }
        border.size = ManagedBorderService.clampSize(size);
        border.enabled = true;
        ManagedBorderService.finishChange(server, target);
        feedback.accept((Text)Text.literal((String)String.format(Locale.ROOT, "%s border size set to %.0f blocks.", ManagedBorderService.displayName(target), border.size)).formatted(Formatting.GREEN));
        return 1;
    }

    public static int setCenter(MinecraftServer server, String input, double x, double z, Consumer<Text> feedback) {
        String target = ManagedBorderService.normalizeTarget(input);
        if (target == null) {
            return ManagedBorderService.unknown(feedback);
        }
        BorderEntry border = ManagedBorderService.ensureFor(server, target);
        if (border == null) {
            return ManagedBorderService.unavailable(target, feedback);
        }
        border.centerX = x;
        border.centerZ = z;
        border.enabled = true;
        ManagedBorderService.finishChange(server, target);
        feedback.accept((Text)Text.literal((String)String.format(Locale.ROOT, "%s border centered at %.1f, %.1f.", ManagedBorderService.displayName(target), x, z)).formatted(Formatting.GREEN));
        return 1;
    }

    public static int centerOnSpawn(MinecraftServer server, String input, Consumer<Text> feedback) {
        String target = ManagedBorderService.normalizeTarget(input);
        if (target == null) {
            return ManagedBorderService.unknown(feedback);
        }
        ServerWorld world = ManagedBorderService.getTargetWorld(server, target);
        if (world == null) {
            return ManagedBorderService.unavailable(target, feedback);
        }
        BorderEntry border = ManagedBorderService.ensureFor(server, target);
        if (border == null) {
            return ManagedBorderService.unavailable(target, feedback);
        }
        BlockPos spawn = world.getSpawnPos();
        border.centerX = (double)spawn.getX() + 0.5;
        border.centerZ = (double)spawn.getZ() + 0.5;
        border.enabled = true;
        ManagedBorderService.finishChange(server, target);
        feedback.accept((Text)Text.literal((String)String.format(Locale.ROOT, "%s border centered on world spawn at %.1f, %.1f.", ManagedBorderService.displayName(target), border.centerX, border.centerZ)).formatted(Formatting.GREEN));
        return 1;
    }

    public static int setEnabled(MinecraftServer server, String input, boolean enabled, Consumer<Text> feedback) {
        String target = ManagedBorderService.normalizeTarget(input);
        if (target == null) {
            return ManagedBorderService.unknown(feedback);
        }
        BorderEntry border = ManagedBorderService.ensureFor(server, target);
        if (border == null) {
            return ManagedBorderService.unavailable(target, feedback);
        }
        border.enabled = enabled;
        ManagedBorderService.finishChange(server, target);
        feedback.accept((Text)Text.literal((String)(ManagedBorderService.displayName(target) + " managed border " + (enabled ? "enabled." : "disabled."))).formatted(enabled ? Formatting.GREEN : Formatting.YELLOW));
        return 1;
    }

    public static int reset(MinecraftServer server, String input, Consumer<Text> feedback) {
        String target = ManagedBorderService.normalizeTarget(input);
        if (target == null) {
            return ManagedBorderService.unknown(feedback);
        }
        ServerWorld world = ManagedBorderService.getTargetWorld(server, target);
        if (world == null) {
            return ManagedBorderService.unavailable(target, feedback);
        }
        BlockPos spawn = world.getSpawnPos();
        BorderEntry border = new BorderEntry(true, (double)spawn.getX() + 0.5, (double)spawn.getZ() + 0.5, 40000.0);
        BORDERS.put(target, border);
        ManagedBorderService.finishChange(server, target);
        feedback.accept((Text)Text.literal((String)String.format(Locale.ROOT, "%s border reset to %.0f x %.0f centered on spawn (%.1f, %.1f).", ManagedBorderService.displayName(target), 40000.0, 40000.0, border.centerX, border.centerZ)).formatted(Formatting.GREEN));
        return 1;
    }

    public static void onWorldRegenerated(MinecraftServer server, String input, BlockPos spawn) {
        String target = ManagedWorldService.normalize(input);
        if (server == null || target == null || spawn == null) {
            return;
        }
        BorderEntry border = BORDERS.get(target);
        if (border == null) {
            border = new BorderEntry(true, (double)spawn.getX() + 0.5, (double)spawn.getZ() + 0.5, 40000.0);
            BORDERS.put(target, border);
        } else {
            border.centerX = (double)spawn.getX() + 0.5;
            border.centerZ = (double)spawn.getZ() + 0.5;
        }
        ManagedBorderService.save();
        ManagedBorderService.syncTarget(server, target);
    }

    public static BorderBounds safeBounds(ServerWorld world, double requestedMargin) {
        BorderEntry border;
        if (world == null) {
            return null;
        }
        String target = ManagedBorderService.targetFor(world);
        BorderEntry borderEntry = border = target == null ? null : BORDERS.get(target);
        if (border == null || !border.enabled || border.size < 2.0) {
            return null;
        }
        double half = border.size / 2.0;
        double maxInset = Math.max(0.0, half - 0.51);
        double inset = Math.max(0.0, Math.min(Math.max(0.0, requestedMargin), maxInset));
        double edge = 0.05 + inset;
        return new BorderBounds(border.centerX, border.centerZ, border.centerX - half + edge, border.centerX + half - edge, border.centerZ - half + edge, border.centerZ + half - edge);
    }

    public static boolean contains(ServerWorld world, double x, double z, double requestedMargin) {
        BorderBounds bounds = ManagedBorderService.safeBounds(world, requestedMargin);
        return bounds == null || bounds.contains(x, z);
    }

    private static void finishChange(MinecraftServer server, String target) {
        if (OVERWORLD.equals(target)) {
            ManagedBorderService.neutralizeVanillaSharedBorder(server);
        }
        ManagedBorderService.save();
        ManagedBorderService.syncTarget(server, target);
    }

    private static void enforce(ServerPlayerEntity player) {
        BorderEntry border;
        if (player == null || player.isSpectator()) {
            return;
        }
        String target = ManagedBorderService.targetFor(player.getServerWorld());
        BorderEntry borderEntry = border = target == null ? null : BORDERS.get(target);
        if (border == null || !border.enabled || border.size < 2.0) {
            return;
        }
        double half = border.size / 2.0;
        double minX = border.centerX - half + 0.05;
        double maxX = border.centerX + half - 0.05;
        double minZ = border.centerZ - half + 0.05;
        double maxZ = border.centerZ + half - 0.05;
        double x = player.getX();
        double z = player.getZ();
        double clippedX = Math.max(minX, Math.min(maxX, x));
        double clippedZ = Math.max(minZ, Math.min(maxZ, z));
        if (clippedX == x && clippedZ == z) {
            return;
        }
        player.teleport(player.getServerWorld(), clippedX, player.getY(), clippedZ, player.getYaw(), player.getPitch());
    }

    private static void ensureDefaults(MinecraftServer server) {
        boolean changed = false;
        for (String logical : ManagedWorldService.logicalNames()) {
            ServerWorld world;
            if (BORDERS.containsKey(logical) || (world = ManagedWorldService.getWorld(server, logical)) == null) continue;
            BlockPos spawn = world.getSpawnPos();
            BORDERS.put(logical, new BorderEntry(true, (double)spawn.getX() + 0.5, (double)spawn.getZ() + 0.5, 40000.0));
            changed = true;
        }
        if (changed) {
            ManagedBorderService.save();
        }
    }

    private static BorderEntry ensureFor(MinecraftServer server, String target) {
        BorderEntry created;
        BorderEntry existing = BORDERS.get(target);
        if (existing != null) {
            return existing;
        }
        ServerWorld world = ManagedBorderService.getTargetWorld(server, target);
        if (world == null) {
            return null;
        }
        if (OVERWORLD.equals(target)) {
            WorldBorder vanilla = world.getWorldBorder();
            created = new BorderEntry(true, vanilla.getCenterX(), vanilla.getCenterZ(), vanilla.getSize());
        } else {
            BlockPos spawn = world.getSpawnPos();
            created = new BorderEntry(true, (double)spawn.getX() + 0.5, (double)spawn.getZ() + 0.5, 40000.0);
        }
        BORDERS.put(target, created);
        return created;
    }

    private static String targetFor(ServerWorld world) {
        if (world == null) {
            return null;
        }
        Identifier id = world.getRegistryKey().getValue();
        String full = id.toString();
        if (OVERWORLD.equals(full)) {
            return OVERWORLD;
        }
        for (String logical : ManagedWorldService.logicalNames()) {
            String resolved = ManagedWorldService.resolvedId(logical);
            if (resolved == null || !resolved.equals(full)) continue;
            return logical;
        }
        return ManagedWorldService.normalize(id.getPath());
    }

    private static String normalizeTarget(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String raw = input.trim().toLowerCase(Locale.ROOT);
        String compact = raw.replace("_", "").replace("-", "");
        if (raw.equals(OVERWORLD) || compact.equals("overworld") || compact.equals(OVERWORLD)) {
            return OVERWORLD;
        }
        return ManagedWorldService.normalize(input);
    }

    private static ServerWorld getTargetWorld(MinecraftServer server, String target) {
        if (server == null || target == null) {
            return null;
        }
        if (OVERWORLD.equals(target)) {
            return server.getOverworld();
        }
        return ManagedWorldService.getWorld(server, target);
    }

    private static String displayName(String target) {
        return OVERWORLD.equals(target) ? "Minecraft Overworld" : ManagedWorldService.displayName(target);
    }

    private static String resolvedId(String target) {
        if (OVERWORLD.equals(target)) {
            return OVERWORLD;
        }
        String id = ManagedWorldService.resolvedId(target);
        return id == null ? "unresolved" : id;
    }

    private static void syncTarget(MinecraftServer server, String target) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!target.equals(ManagedBorderService.targetFor(player.getServerWorld()))) continue;
            ManagedBorderService.sync(player);
        }
    }

    private static void neutralizeVanillaSharedBorder(MinecraftServer server) {
        if (server == null || server.getOverworld() == null) {
            return;
        }
        WorldBorder border = server.getOverworld().getWorldBorder();
        if (Math.abs(border.getCenterX()) > 1.0E-4 || Math.abs(border.getCenterZ()) > 1.0E-4) {
            border.setCenter(0.0, 0.0);
        }
        if (Math.abs(border.getSize() - 5.9999968E7) > 0.5) {
            border.setSize(5.9999968E7);
        }
    }

    private static double clampSize(double size) {
        if (Double.isNaN(size) || Double.isInfinite(size)) {
            return 40000.0;
        }
        return Math.max(2.0, Math.min(5.9999968E7, size));
    }

    private static int unknown(Consumer<Text> feedback) {
        feedback.accept((Text)Text.literal((String)"Unknown border world. Use minecraft:overworld (or overworld), redworld, yellowworld, blueworld, greenworld, resource, purpleworld, orangeworld, pinkworld or cyanworld.").formatted(Formatting.RED));
        return 0;
    }

    private static int unavailable(String target, Consumer<Text> feedback) {
        feedback.accept((Text)Text.literal((String)(ManagedBorderService.displayName(target) + " is currently unavailable.")).formatted(Formatting.RED));
        return 0;
    }

    private static void load() {
        BORDERS.clear();
        if (statePath == null || !Files.exists(statePath, new LinkOption[0])) {
            return;
        }
        try {
            BorderFile file = (BorderFile)GSON.fromJson(Files.readString(statePath, StandardCharsets.UTF_8), BorderFile.class);
            if (file != null && file.worlds != null) {
                BORDERS.putAll(file.worlds);
            }
            for (BorderEntry entry : BORDERS.values()) {
                if (entry == null) continue;
                entry.size = ManagedBorderService.clampSize(entry.size);
            }
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.error("Could not read managed border state; defaults will be recreated only for missing entries.", (Throwable)error);
        }
    }

    private static void save() {
        if (statePath == null) {
            return;
        }
        try {
            Files.createDirectories(statePath.getParent(), new FileAttribute[0]);
            BorderFile file = new BorderFile();
            file.worlds.putAll(BORDERS);
            Files.writeString(statePath, (CharSequence)GSON.toJson((Object)file), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException error) {
            CobbleClubServer.LOGGER.error("Could not save managed border state", (Throwable)error);
        }
    }

    private static final class BorderEntry {
        boolean enabled = true;
        double centerX;
        double centerZ;
        double size = 40000.0;

        BorderEntry() {
        }

        BorderEntry(boolean enabled, double centerX, double centerZ, double size) {
            this.enabled = enabled;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.size = ManagedBorderService.clampSize(size);
        }
    }

    public record BorderBounds(double centerX, double centerZ, double minX, double maxX, double minZ, double maxZ) {
        public boolean contains(double x, double z) {
            return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
        }
    }

    private static final class BorderFile {
        int version = 2;
        Map<String, BorderEntry> worlds = new LinkedHashMap<String, BorderEntry>();

        private BorderFile() {
        }
    }
}

