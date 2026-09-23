/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.battles.model.actor.BattleActor
 *  com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
 *  com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
 *  com.cobblemon.mod.common.battles.actor.PlayerBattleActor
 *  com.cobblemon.mod.common.battles.pokemon.BattlePokemon
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_5218
 *  net.minecraft.class_5321
 *  net.minecraft.class_7924
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.PermissionService;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;

public final class LeaderboardService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, Placement> BOARDS = new LinkedHashMap<String, Placement>();
    private static final Map<String, String> LAST_RENDER = new HashMap<String, String>();
    private static final int REFRESH_TICKS = 1200;
    private static final int STARTUP_DEDUPE_DELAY_TICKS = 100;
    private static final int STARTUP_DEDUPE_INTERVAL_TICKS = 20;
    private static final int STARTUP_DEDUPE_WINDOW_TICKS = 6000;
    private static final int LIMIT = 10;
    private static final String ROOT_TAG = "cobbleclub_leaderboard";
    private static final int BOARD_BACKGROUND = 0;
    private static final int EDGE_ANIMATION_INTERVAL_TICKS = 8;
    private static Path storePath;
    private static int tickCounter;
    private static int startupTicks;
    private static int edgeAnimationTicks;
    private static int edgePhase;

    private LeaderboardService() {
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"leaderboard").requires(source -> PermissionService.admin(source, "cobbleclub.admin.leaderboard", 2))).executes(context -> LeaderboardService.list((ServerCommandSource)context.getSource()))).then(CommandManager.literal((String)"list").executes(context -> LeaderboardService.list((ServerCommandSource)context.getSource())))).then(CommandManager.literal((String)"refresh").executes(context -> {
            LAST_RENDER.clear();
            LeaderboardService.refreshAll(((ServerCommandSource)context.getSource()).getServer());
            ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal((String)"CobbleClub leaderboards refreshed."), false);
            return 1;
        }))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"create").then(CommandManager.literal((String)"shinies").executes(context -> LeaderboardService.create(((ServerCommandSource)context.getSource()).getPlayer(), Type.SHINIES)))).then(CommandManager.literal((String)"catches").executes(context -> LeaderboardService.create(((ServerCommandSource)context.getSource()).getPlayer(), Type.SHINIES)))).then(CommandManager.literal((String)"kills").executes(context -> LeaderboardService.create(((ServerCommandSource)context.getSource()).getPlayer(), Type.KILLS)))).then(CommandManager.literal((String)"claims").executes(context -> LeaderboardService.create(((ServerCommandSource)context.getSource()).getPlayer(), Type.CLAIMS)))).then(CommandManager.literal((String)"money").executes(context -> LeaderboardService.create(((ServerCommandSource)context.getSource()).getPlayer(), Type.MONEY))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"remove").then(CommandManager.literal((String)"shinies").executes(context -> LeaderboardService.remove((ServerCommandSource)context.getSource(), Type.SHINIES)))).then(CommandManager.literal((String)"catches").executes(context -> LeaderboardService.remove((ServerCommandSource)context.getSource(), Type.SHINIES)))).then(CommandManager.literal((String)"kills").executes(context -> LeaderboardService.remove((ServerCommandSource)context.getSource(), Type.KILLS)))).then(CommandManager.literal((String)"claims").executes(context -> LeaderboardService.remove((ServerCommandSource)context.getSource(), Type.CLAIMS)))).then(CommandManager.literal((String)"money").executes(context -> LeaderboardService.remove((ServerCommandSource)context.getSource(), Type.MONEY)))));
    }

    public static void load(MinecraftServer server) {
        storePath = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("leaderboards.json");
        BOARDS.clear();
        LAST_RENDER.clear();
        tickCounter = 0;
        startupTicks = 0;
        edgeAnimationTicks = 0;
        edgePhase = 0;
        boolean migrated = false;
        try {
            StoreFile file;
            if (Files.exists(storePath, new LinkOption[0]) && (file = (StoreFile)GSON.fromJson(Files.readString(storePath, StandardCharsets.UTF_8), StoreFile.class)) != null && file.boards != null) {
                for (Map.Entry<String, Placement> entry : file.boards.entrySet()) {
                    Type type;
                    if (entry.getValue() == null || !LeaderboardService.validDimension(entry.getValue().dimension)) continue;
                    String id = entry.getKey();
                    if ("catches".equalsIgnoreCase(id)) {
                        LeaderboardService.killTag(server, entry.getValue().dimension, "cobbleclub_lb_catches");
                        id = Type.SHINIES.id;
                        migrated = true;
                    }
                    if ((type = Type.fromId(id)) == null) continue;
                    BOARDS.put(type.id, entry.getValue());
                }
            }
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not load CobbleClub leaderboard placements", (Throwable)error);
        }
        LeaderboardService.syncOnlineNames(server);
        if (migrated) {
            LeaderboardService.save();
        }
    }

    public static void save() {
        if (storePath == null) {
            return;
        }
        try {
            Files.createDirectories(storePath.getParent(), new FileAttribute[0]);
            StoreFile file = new StoreFile();
            file.boards = new LinkedHashMap<String, Placement>(BOARDS);
            String json = GSON.toJson((Object)file);
            Path temp = storePath.resolveSibling("leaderboards.json.tmp");
            Path backup = storePath.resolveSibling("leaderboards.json.bak");
            Files.writeString(temp, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
            if (Files.exists(storePath, new LinkOption[0])) {
                Files.copy(storePath, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException atomicUnsupported) {
                Files.move(temp, storePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException error) {
            CobbleClubServer.LOGGER.warn("Could not save CobbleClub leaderboard placements", (Throwable)error);
        }
    }

    public static void tick(MinecraftServer server) {
        if (BOARDS.isEmpty()) {
            return;
        }

        if (++edgeAnimationTicks >= EDGE_ANIMATION_INTERVAL_TICKS) {
            edgeAnimationTicks = 0;
            edgePhase = (edgePhase + 1) & 3;
            LeaderboardService.animateEdges(server);
        }

        if (++startupTicks >= 100 && startupTicks <= 6000 && startupTicks % 20 == 0) {
            LeaderboardService.syncOnlineNames(server);
            LeaderboardService.refreshAll(server);
        }
        if (++tickCounter < 1200) {
            return;
        }
        tickCounter = 0;
        LeaderboardService.syncOnlineNames(server);
        LeaderboardService.refreshAll(server);
    }

    public static void onPokemonCaptured(PokemonCapturedEvent event) {
        if (event == null || event.getPlayer() == null || event.getPokemon() == null) {
            return;
        }
        ServerPlayerEntity player = event.getPlayer();
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        data.lastKnownName = player.getGameProfile().getName();
        data.pokemonCatches = LeaderboardService.safeIncrement(data.pokemonCatches);
        if (event.getPokemon().getShiny()) {
            data.shinyPokemonCatches = LeaderboardService.safeIncrement(data.shinyPokemonCatches);
            LAST_RENDER.remove(Type.SHINIES.id);
        }
        ++data.revision;
    }

    public static void onBattleFainted(BattleFaintedEvent event) {
        if (event == null || event.getContext() == null || event.getKilled() == null) {
            return;
        }
        BattlePokemon origin = event.getContext().getOrigin();
        if (origin == null || origin.getActor() == null || event.getKilled().getActor() == null) {
            return;
        }
        if (origin.getActor() == event.getKilled().getActor()) {
            return;
        }
        BattleActor battleActor = origin.getActor();
        if (!(battleActor instanceof PlayerBattleActor)) {
            return;
        }
        PlayerBattleActor playerActor = (PlayerBattleActor)battleActor;
        ServerPlayerEntity player = playerActor.getEntity();
        if (player == null) {
            return;
        }
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        data.lastKnownName = player.getGameProfile().getName();
        data.pokemonDefeats = LeaderboardService.safeIncrement(data.pokemonDefeats);
        ++data.revision;
        LAST_RENDER.remove(Type.KILLS.id);
    }

    public static void refreshAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (Type type : Type.values()) {
            Placement placement = BOARDS.get(type.id);
            if (placement == null) continue;
            LeaderboardService.refreshBoard(server, type, placement);
        }
    }

    private static int create(ServerPlayerEntity player, Type type) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        Placement old = BOARDS.get(type.id);
        if (old != null) {
            LeaderboardService.killBoardEntities(server, type, old.dimension);
        }
        if (type == Type.SHINIES) {
            LeaderboardService.killTag(server, player.getServerWorld().getRegistryKey().getValue().toString(), "cobbleclub_lb_catches");
        }
        Placement placement = new Placement();
        placement.dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        placement.x = player.getX();
        placement.y = player.getY();
        placement.z = player.getZ();
        placement.yaw = LeaderboardService.normalizeYaw(player.getYaw());
        BOARDS.put(type.id, placement);
        LAST_RENDER.remove(type.id);
        LeaderboardService.save();
        LeaderboardService.syncOnlineNames(server);
        LeaderboardService.refreshBoard(server, type, placement);
        player.sendMessage((Text)Text.literal((String)("Created " + type.title + " leaderboard at your position. It faces the direction you were facing.")), false);
        player.sendMessage((Text)Text.literal((String)("Base: " + LeaderboardService.fmt(placement.x) + " " + LeaderboardService.fmt(placement.y) + " " + LeaderboardService.fmt(placement.z) + " in " + placement.dimension + ". Re-run the same create command to move/rotate it.")), false);
        return 1;
    }

    private static int remove(ServerCommandSource source, Type type) {
        Placement placement = BOARDS.remove(type.id);
        if (placement == null) {
            source.sendError((Text)Text.literal((String)("No " + type.id + " leaderboard is currently placed.")));
            return 0;
        }
        LeaderboardService.killBoardEntities(source.getServer(), type, placement.dimension);
        if (type == Type.SHINIES) {
            LeaderboardService.killTag(source.getServer(), placement.dimension, "cobbleclub_lb_catches");
        }
        LAST_RENDER.remove(type.id);
        LeaderboardService.save();
        source.sendFeedback(() -> Text.literal((String)("Removed the " + type.title + " leaderboard.")), false);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal((String)"CobbleClub hologram leaderboards:"), false);
        int placed = 0;
        for (Type type : Type.values()) {
            Placement placement = BOARDS.get(type.id);
            if (placement == null) {
                source.sendFeedback(() -> Text.literal((String)("- " + type.id + ": not placed")), false);
                continue;
            }
            ++placed;
            String line = "- " + type.id + ": " + placement.dimension + " @ " + LeaderboardService.fmt(placement.x) + ", " + LeaderboardService.fmt(placement.y) + ", " + LeaderboardService.fmt(placement.z) + " yaw " + String.format(Locale.ROOT, "%.1f", Float.valueOf(placement.yaw));
            source.sendFeedback(() -> Text.literal((String)line), false);
        }
        return placed;
    }

    private static void refreshBoard(MinecraftServer server, Type type, Placement placement) {
        if (!LeaderboardService.validDimension(placement.dimension)) {
            return;
        }
        if (!LeaderboardService.placementChunkLoaded(server, placement)) {
            return;
        }
        try {
            List<Entry> entries = LeaderboardService.entries(type, 10);
            String signatureText = LeaderboardService.boardTextJson(type, entries, 0);
            String textJson = LeaderboardService.boardTextJson(type, entries, edgePhase);
            String signature = placement.dimension + "|" + placement.x + "|" + placement.y + "|" + placement.z + "|" + placement.yaw + "|" + signatureText;
            if (signature.equals(LAST_RENDER.get(type.id))) {
                return;
            }
            LeaderboardService.killBoardEntities(server, type, placement.dimension);
            LeaderboardService.summonBoard(server, type, placement, textJson);
            LAST_RENDER.put(type.id, signature);
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.warn("Could not refresh {} leaderboard", (Object)type.id, (Object)error);
        }
    }

    private static boolean placementChunkLoaded(MinecraftServer server, Placement placement) {
        if (server == null || placement == null || !LeaderboardService.validDimension(placement.dimension)) {
            return false;
        }
        Identifier id = Identifier.tryParse((String)placement.dimension);
        if (id == null) {
            return false;
        }
        RegistryKey key = RegistryKey.of((RegistryKey)RegistryKeys.WORLD, (Identifier)id);
        ServerWorld world = server.getWorld(key);
        if (world == null) {
            return false;
        }
        int chunkX = (int)Math.floor(placement.x) >> 4;
        int chunkZ = (int)Math.floor(placement.z) >> 4;
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    private static void summonBoard(MinecraftServer server, Type type, Placement placement, String textJson) {
        String nbt = "{Tags:[\"cobbleclub_leaderboard\",\"" + LeaderboardService.boardTag(type) + "\"],billboard:\"fixed\",brightness:{block:15,sky:15},view_range:2.0f,shadow:1b,see_through:0b,background:" + BOARD_BACKGROUND + ",default_background:0b,alignment:\"center\",line_width:460,width:0.0f,height:0.0f,Invulnerable:1b,NoGravity:1b,Rotation:[" + String.format(Locale.ROOT, "%.2ff", Float.valueOf(placement.yaw)) + ",0.0f],transformation:[1.35f,0.0f,0.0f,0.0f,0.0f,1.35f,0.0f,0.0f,0.0f,0.0f,1.35f,0.0f,0.0f,0.0f,0.0f,1.0f],text:'" + LeaderboardService.snbtSingleQuoted(textJson) + "'}";
        String command = "execute in " + placement.dimension + " run summon minecraft:text_display " + LeaderboardService.fmt(placement.x) + " " + LeaderboardService.fmt(placement.y + 4.1) + " " + LeaderboardService.fmt(placement.z) + " " + nbt;
        LeaderboardService.executeSilent(server, command);
    }

    private static void animateEdges(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (Type type : Type.values()) {
            Placement placement = BOARDS.get(type.id);
            if (placement == null || !LeaderboardService.placementChunkLoaded(server, placement)) {
                continue;
            }
            try {
                String textJson = LeaderboardService.boardTextJson(type, LeaderboardService.entries(type, 10), edgePhase);
                String command = "execute in " + placement.dimension
                        + " run data merge entity @e[type=minecraft:text_display,tag="
                        + LeaderboardService.boardTag(type)
                        + ",limit=1] {text:'"
                        + LeaderboardService.snbtSingleQuoted(textJson)
                        + "'}";
                LeaderboardService.executeSilent(server, command);
            }
            catch (Exception error) {
                CobbleClubServer.LOGGER.debug("Could not animate {} leaderboard edge", type.id, error);
            }
        }
    }

    private static String boardTextJson(Type type, List<Entry> entries, int phase) {
        StringBuilder json = new StringBuilder(3072);
        json.append("{\"text\":\"\",\"extra\":[");
        boolean first = true;

        first = LeaderboardService.appendEdgeLeft(json, first, phase, 0);
        first = LeaderboardService.appendComponent(json, first, "\u2726  " + type.title + "  \u2726", type.titleColor, true);
        first = LeaderboardService.appendEdgeRight(json, first, phase, 0, true);

        first = LeaderboardService.appendEdgeLeft(json, first, phase, 1);
        first = LeaderboardService.appendComponent(json, first, type.subtitle, "#A9A9B8", false);
        first = LeaderboardService.appendEdgeRight(json, first, phase, 1, true);

        first = LeaderboardService.appendComponent(json, first, "\u00b7  \u25ab \u25aa  ", "#34343D", false);
        first = LeaderboardService.appendComponent(json, first, "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501", "#73737F", false);
        first = LeaderboardService.appendComponent(json, first, "  \u25aa \u25ab  \u00b7\n\n", "#34343D", false);

        for (int i = 0; i < 10; ++i) {
            String row;
            boolean bold;
            int rank = i + 1;
            String color = rank == 1 ? "#FFD700" : (rank == 2 ? "#C0C0C0" : (rank == 3 ? "#CD7F32" : "#F5F5F5"));
            bold = rank <= 3;
            if (i < entries.size()) {
                Entry entry = entries.get(i);
                row = LeaderboardService.rankPrefix(rank) + "  " + LeaderboardService.trimName(entry.name) + "   \u2022   " + LeaderboardService.formatValue(type, entry.value);
            } else {
                row = LeaderboardService.rankPrefix(rank) + "  \u2014";
                color = "#666672";
                bold = false;
            }
            first = LeaderboardService.appendEdgeLeft(json, first, phase, i + 2);
            first = LeaderboardService.appendComponent(json, first, row, color, bold);
            first = LeaderboardService.appendEdgeRight(json, first, phase, i + 2, true);
        }

        first = LeaderboardService.appendEdgeLeft(json, first, phase, 12);
        first = LeaderboardService.appendComponent(json, first, "LIVE  \u2022  AUTO-UPDATES", "#7F7F8D", false);
        first = LeaderboardService.appendEdgeRight(json, first, phase, 12, false);
        json.append("]}");
        return json.toString();
    }

    private static boolean appendEdgeLeft(StringBuilder json, boolean first, int phase, int row) {
        int p = (phase + row) & 3;
        String far = switch (p) {
            case 0 -> "\u00b7 ";
            case 1 -> "  ";
            case 2 -> "\u00b7  ";
            default -> " \u00b7";
        };
        String mid = (p == 1 || p == 3) ? "\u25ab " : " \u25ab ";
        String near = (p == 2) ? "\u25aa  " : "\u25aa ";
        first = LeaderboardService.appendComponent(json, first, far, "#2E2E36", false);
        first = LeaderboardService.appendComponent(json, first, mid, "#4A4A55", false);
        return LeaderboardService.appendComponent(json, first, near, "#747481", false);
    }

    private static boolean appendEdgeRight(StringBuilder json, boolean first, int phase, int row, boolean newline) {
        int p = (phase + row) & 3;
        String near = (p == 0) ? "  \u25aa" : " \u25aa";
        String mid = (p == 1 || p == 3) ? " \u25ab" : "  \u25ab";
        String far = switch (p) {
            case 0 -> " \u00b7";
            case 1 -> "  ";
            case 2 -> "  \u00b7";
            default -> "\u00b7 ";
        };
        first = LeaderboardService.appendComponent(json, first, near, "#747481", false);
        first = LeaderboardService.appendComponent(json, first, mid, "#4A4A55", false);
        return LeaderboardService.appendComponent(json, first, far + (newline ? "\n" : ""), "#2E2E36", false);
    }

    private static boolean appendComponent(StringBuilder json, boolean first, String text, String color, boolean bold) {
        if (!first) {
            json.append(',');
        }
        json.append("{\"text\":\"").append(LeaderboardService.jsonEscape(text)).append("\",\"color\":\"").append(color).append("\",\"bold\":").append(bold).append('}');
        return false;
    }

    private static String rankPrefix(int rank) {
        return switch (rank) {
            case 1 -> "#1";
            case 2 -> "#2";
            case 3 -> "#3";
            default -> "#" + rank;
        };
    }

    private static String trimName(String name) {
        if (name == null || name.isBlank()) {
            return "Unknown Player";
        }
        return name.length() <= 16 ? name : name.substring(0, 16);
    }

    private static void killBoardEntities(MinecraftServer server, Type type, String dimension) {
        if (server == null || !LeaderboardService.validDimension(dimension)) {
            return;
        }
        LeaderboardService.killTag(server, dimension, LeaderboardService.boardTag(type));
    }

    private static void killTag(MinecraftServer server, String dimension, String tag) {
        if (server == null || !LeaderboardService.validDimension(dimension) || tag == null || tag.isBlank()) {
            return;
        }
        LeaderboardService.executeSilent(server, "execute in " + dimension + " run kill @e[type=minecraft:text_display,tag=" + tag + "]");
    }

    private static void executeSilent(MinecraftServer server, String command) {
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), command);
    }

    private static List<Entry> entries(Type type, int limit) {
        return switch (type.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> LeaderboardService.playerStatEntries(true, limit);
            case 1 -> LeaderboardService.playerStatEntries(false, limit);
            case 2 -> LeaderboardService.claimEntries(limit);
            case 3 -> LeaderboardService.moneyEntries(limit);
        };
    }

    private static List<Entry> playerStatEntries(boolean shinies, int limit) {
        ArrayList<Entry> entries = new ArrayList<Entry>();
        for (Map.Entry<String, PlayerDataStore.PlayerData> row : PlayerDataStore.all().entrySet()) {
            PlayerDataStore.PlayerData data = row.getValue();
            if (data == null) continue;
            data.normalize();
            long value = shinies ? data.shinyPokemonCatches : data.pokemonDefeats;
            entries.add(new Entry(LeaderboardService.displayName(row.getKey(), data), Math.max(0L, value)));
        }
        return LeaderboardService.sorted(entries, limit);
    }

    private static List<Entry> moneyEntries(int limit) {
        ArrayList<Entry> entries = new ArrayList<Entry>();
        for (EconomyService.BalanceEntry row : EconomyService.topBalances(limit)) {
            entries.add(new Entry(row.name(), Math.max(0L, row.balance())));
        }
        return List.copyOf(entries);
    }

    private static List<Entry> claimEntries(int limit) {
        HashMap<String, Long> usedByOwner = new HashMap<String, Long>();
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (claim == null || claim.ownerUuid == null) continue;
            usedByOwner.merge(claim.ownerUuid, Long.valueOf(Math.max(0, claim.area())), LeaderboardService::safeAdd);
        }
        ArrayList<Entry> entries = new ArrayList<Entry>();
        long initial = Math.max(0, CobbleClubServer.config().initialClaimBlocks);
        for (Map.Entry<String, PlayerDataStore.PlayerData> row : PlayerDataStore.all().entrySet()) {
            PlayerDataStore.PlayerData data = row.getValue();
            if (data == null) continue;
            data.normalize();
            long total = LeaderboardService.safeAdd(initial, Math.max(0, data.bonusClaimBlocks));
            long used = Math.max(0L, usedByOwner.getOrDefault(row.getKey(), 0L));
            long remaining = Math.max(0L, total - used);
            entries.add(new Entry(LeaderboardService.displayName(row.getKey(), data), remaining));
        }
        return LeaderboardService.sorted(entries, limit);
    }

    private static List<Entry> sorted(List<Entry> entries, int limit) {
        entries.sort(Comparator.comparingLong(Entry::value).reversed().thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        int size = Math.min(entries.size(), Math.max(1, limit));
        return List.copyOf(entries.subList(0, size));
    }

    private static void syncOnlineNames(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
            data.normalize();
            String name = player.getGameProfile().getName();
            if (name == null || name.isBlank() || name.equals(data.lastKnownName)) continue;
            data.lastKnownName = name;
            ++data.revision;
        }
    }

    private static String displayName(String uuidText, PlayerDataStore.PlayerData data) {
        if (data.lastKnownName != null && !data.lastKnownName.isBlank()) {
            return data.lastKnownName;
        }
        try {
            String compact = UUID.fromString(uuidText).toString().replace("-", "");
            return "Player-" + compact.substring(0, 6);
        }
        catch (Exception ignored) {
            return "Unknown Player";
        }
    }

    private static String formatValue(Type type, long value) {
        if (type == Type.MONEY) {
            String symbol = CobbleClubServer.config().currencySymbol == null ? "" : CobbleClubServer.config().currencySymbol;
            return symbol + String.format(Locale.US, "%,d", Math.max(0L, value));
        }
        return String.format(Locale.US, "%,d", Math.max(0L, value));
    }

    private static long safeIncrement(long value) {
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, value) + 1L;
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, left + right);
    }

    private static float normalizeYaw(float yaw) {
        float result = yaw % 360.0f;
        if (result > 180.0f) {
            result -= 360.0f;
        }
        if (result <= -180.0f) {
            result += 360.0f;
        }
        return result;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String boardTag(Type type) {
        return "cobbleclub_lb_" + type.id;
    }

    private static boolean validDimension(String dimension) {
        return dimension != null && Identifier.tryParse((String)dimension) != null;
    }

    private static String snbtSingleQuoted(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        block7: for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': {
                    out.append("\\\\");
                    continue block7;
                }
                case '\"': {
                    out.append("\\\"");
                    continue block7;
                }
                case '\n': {
                    out.append("\\n");
                    continue block7;
                }
                case '\r': {
                    out.append("\\r");
                    continue block7;
                }
                case '\t': {
                    out.append("\\t");
                    continue block7;
                }
                default: {
                    if (c < ' ') {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int)c));
                        continue block7;
                    }
                    out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static final class StoreFile {
        Map<String, Placement> boards = new LinkedHashMap<String, Placement>();

        private StoreFile() {
        }
    }

    private static final class Placement {
        String dimension;
        double x;
        double y;
        double z;
        float yaw;

        private Placement() {
        }
    }

    private static enum Type {
        SHINIES("shinies", "SHINY HUNTERS", "TOP 10 \u2022 LIFETIME SHINY CAPTURES", "#E879F9"),
        KILLS("kills", "BATTLE LEGENDS", "TOP 10 \u2022 POK\u00c9MON DEFEATED", "#FB7185"),
        CLAIMS("claims", "LAND BARONS", "TOP 10 \u2022 AVAILABLE CLAIM BLOCKS", "#86EFAC"),
        MONEY("money", "COBBLECLUB ELITE", "TOP 10 \u2022 RICHEST TRAINERS", "#FCD34D");

        private final String id;
        private final String title;
        private final String subtitle;
        private final String titleColor;

        private Type(String id, String title, String subtitle, String titleColor) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.titleColor = titleColor;
        }

        private static Type fromId(String id) {
            if (id == null) {
                return null;
            }
            for (Type type : Type.values()) {
                if (!type.id.equalsIgnoreCase(id)) continue;
                return type;
            }
            return null;
        }
    }

    private record Entry(String name, long value) {
    }
}

