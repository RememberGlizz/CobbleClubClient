/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.class_124
 *  net.minecraft.class_1259$class_1260
 *  net.minecraft.class_1259$class_1261
 *  net.minecraft.class_1297
 *  net.minecraft.class_1536
 *  net.minecraft.class_1542
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1922
 *  net.minecraft.class_1935
 *  net.minecraft.class_1937
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_238
 *  net.minecraft.class_2561
 *  net.minecraft.class_2806
 *  net.minecraft.class_2960
 *  net.minecraft.class_3213
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_5218
 *  net.minecraft.class_5321
 *  net.minecraft.class_7923
 *  net.minecraft.class_7924
 *  net.minecraft.class_9279
 *  net.minecraft.class_9331
 *  net.minecraft.class_9334
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.service.ClubItems;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.PermissionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Formatting;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.util.Identifier;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.MinecraftServer;

public final class PondService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long INTERVAL_MS = 2700000L;
    private static final long WARNING_MS = 60000L;
    private static final long DURATION_MS = 240000L;
    private static final long DROP_MS = 2000L;
    private static final int MAX_ITEMS = 40;
    private static final String DEFAULT_ALPHA_COMMAND = "givepokemonother {player} {species} alpha=true shiny=true";
    private static final String DROP_TAG = "cobbleclub_pond_drop";
    private static final Map<UUID, CornerSelection> SELECTIONS = new HashMap<>();
    private static final Map<UUID, LiveDrop> DROPS = new HashMap<>();
    private static final Map<UUID, PendingTravel> TRAVELS = new LinkedHashMap<>();
    private static final ServerBossBar BAR = new ServerBossBar(Text.literal("Fishing Pond"), BossBar.Color.BLUE, BossBar.Style.PROGRESS);
    private static PondFile data = new PondFile();
    private static Path path;
    private static String activePond;
    private static long activeUntil;
    private static long nextDrop;
    private static int spawnCursor;
    private static int ticks;
    private static boolean warned;
    private static List<String> alphaSpecies;

    private PondService() {
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("pond")
                        .requires(source -> PermissionService.has(source, "cobbleclub.command.pond", true))
                        .executes(ctx -> PondService.teleport(ctx.getSource().getPlayer()))
                        .then(CommandManager.literal("status")
                                .executes(ctx -> PondService.status(ctx.getSource())))
        );

        dispatcher.register(
                CommandManager.literal("cobbleclubserver")
                        .then(
                                CommandManager.literal("pond")
                                        .requires(source -> PermissionService.admin(source, "cobbleclub.admin.pond", 2))
                                        .then(CommandManager.literal("pos1")
                                                .executes(ctx -> PondService.setCorner(ctx.getSource(), true)))
                                        .then(CommandManager.literal("pos2")
                                                .executes(ctx -> PondService.setCorner(ctx.getSource(), false)))
                                        .then(CommandManager.literal("create")
                                                .then(CommandManager.argument("name", StringArgumentType.word())
                                                        .executes(ctx -> PondService.create(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name")
                                                        ))))
                                        .then(CommandManager.literal("spawn")
                                                .then(CommandManager.argument("name", StringArgumentType.word())
                                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 10))
                                                                .executes(ctx -> PondService.setSpawn(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "name"),
                                                                        IntegerArgumentType.getInteger(ctx, "slot")
                                                                )))))
                                        .then(CommandManager.literal("start")
                                                .then(CommandManager.argument("name", StringArgumentType.word())
                                                        .executes(ctx -> PondService.start(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name")
                                                        ))))
                                        .then(CommandManager.literal("testalpha")
                                                .executes(ctx -> PondService.testAlpha(ctx.getSource())))
                                        .then(CommandManager.literal("stop")
                                                .executes(ctx -> PondService.stop(ctx.getSource())))
                                        .then(CommandManager.literal("list")
                                                .executes(ctx -> PondService.list(ctx.getSource())))
                        )
        );
    }

    public static void initialize(MinecraftServer server) {
        path = server.getSavePath(WorldSavePath.ROOT).resolve("cobbleclub").resolve("ponds.json");
        boolean firstSetup = !Files.exists(path);
        try {
            if (Files.exists(path)) {
                data = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), PondFile.class);
            }
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.error("Could not read fishing pond settings", error);
        }
        if (data == null) {
            data = new PondFile();
        }
        if (PondService.data.ponds == null) {
            PondService.data.ponds = new LinkedHashMap<>();
        }
        if (PondService.data.low == null) {
            PondService.data.low = new PondFile().low;
        }
        if (PondService.data.mid == null) {
            PondService.data.mid = new PondFile().mid;
        }
        if (PondService.data.rare == null) {
            PondService.data.rare = new PondFile().rare;
        }
        if (PondService.data.dolls == null) {
            PondService.data.dolls = new PondFile().dolls;
        }
        if (PondService.data.alphaCommand == null || PondService.data.alphaCommand.isBlank() || PondService.data.alphaCommand.equals("pokegive {player} {species} shiny alpha") || PondService.data.alphaCommand.equals("pokegive {player} {species} alpha=true shiny=true") || !PondService.data.alphaCommand.contains("{player}") || !PondService.data.alphaCommand.contains("{species}")) {
            PondService.data.alphaCommand = DEFAULT_ALPHA_COMMAND;
        }
        alphaSpecies = PondService.loadAlphaSpecies();
        PondService.discoverAddonItems();
        if (PondService.data.nextAt < System.currentTimeMillis()) {
            PondService.data.nextAt = System.currentTimeMillis() + (firstSetup ? 2700000L : 60000L);
        }
        activePond = null;
        activeUntil = 0L;
        warned = false;
        DROPS.clear();
        TRAVELS.clear();
        PondService.save();
    }

    public static void shutdown() {
        PondService.save();
        BAR.clearPlayers();
        SELECTIONS.clear();
        DROPS.clear();
        TRAVELS.clear();
        alphaSpecies = List.of();
        activePond = null;
        path = null;
    }

    public static void onEntityLoaded(Entity entity) {
        if (entity instanceof ItemEntity && entity.getCommandTags().contains(DROP_TAG) && !DROPS.containsKey(entity.getUuid())) {
            entity.discard();
        }
    }

    public static void tick(MinecraftServer server) {
        if (!TRAVELS.isEmpty()) {
            PondService.tickTravel(server);
        }
        if (++ticks % 2 == 0 && !DROPS.isEmpty()) {
            PondService.updateDrops(server);
        }
        if (ticks % 20 != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (activePond != null && now >= activeUntil) {
            PondService.end(server);
        }
        if (activePond == null && now >= PondService.data.nextAt - 60000L) {
            Pond next = PondService.nextReady(server);
            if (next != null) {
                if (!warned) {
                    warned = true;
                    server.getPlayerManager().broadcast(Text.literal("\u2726 Fishing Pond opens in 60 seconds! Type /pond to join.").formatted(Formatting.AQUA), false);
                }
                PondService.showBar(server, next.name, Math.min(1.0f, Math.max(0.0f, 1.0f - (float)(PondService.data.nextAt - now) / 60000.0f)), "opens in " + Math.max(0L, (PondService.data.nextAt - now + 999L) / 1000L) + "s");
                if (now >= PondService.data.nextAt) {
                    PondService.begin(server, next);
                }
            } else if (now >= PondService.data.nextAt) {
                PondService.data.nextAt = now + 2700000L;
                warned = false;
                BAR.clearPlayers();
                PondService.save();
            }
        } else if (activePond != null) {
            PondService.showBar(server, activePond, 1.0f, "LIVE \u2022 /pond \u2022 " + Math.max(0L, (activeUntil - now + 999L) / 1000L) + "s");
            if (now >= nextDrop) {
                nextDrop = now + 2000L;
                Pond pond = PondService.data.ponds.get(activePond);
                if (pond != null && DROPS.size() < 40) {
                    PondService.drop(server, pond, null);
                }
            }
        }
    }

    public static boolean interceptPickup(ServerPlayerEntity player, ItemEntity item) {
        LiveDrop live = DROPS.get(item.getUuid());
        if (live == null) {
            return false;
        }
        if (live.landedAt == 0L || System.currentTimeMillis() - live.landedAt < 10000L) {
            return true;
        }
        if (live.prize != Prize.ITEM) {
            PondService.claim(player, item, live);
            return true;
        }
        return false;
    }

    private static void updateDrops(MinecraftServer server) {
        long now = System.currentTimeMillis();
        ArrayList<UUID> remove = new ArrayList<>();
        HashMap<Pond, List<FishingBobberEntity>> hooksByPond = new HashMap<>();
        block0: for (Map.Entry<UUID, LiveDrop> row : new ArrayList<>(DROPS.entrySet())) {
            World entityWorld;
            LiveDrop live = row.getValue();
            ItemEntity item = live.item;
            if (item.isRemoved()) {
                remove.add(row.getKey());
                continue;
            }
            if (live.landedAt == 0L && (item.isOnGround() || item.isTouchingWater())) {
                live.landedAt = now;
            }
            if (live.landedAt == 0L || now - live.landedAt < 10000L) {
                item.setPickupDelayInfinite();
            } else {
                item.setPickupDelay(0);
            }
            if (live.landedAt != 0L && now - live.landedAt > 40000L) {
                item.discard();
                remove.add(row.getKey());
                continue;
            }
            if (live.landedAt != 0L) continue;
            entityWorld = item.getWorld();
            if (!(entityWorld instanceof ServerWorld)) continue;
            ServerWorld world = (ServerWorld) entityWorld;
            List<FishingBobberEntity> hooks = hooksByPond.computeIfAbsent(live.pond, pond -> {
                Box scan = new Box((double)(pond.minX - 3), (double)(pond.y - 5), (double)(pond.minZ - 3), (double)(pond.maxX + 4), (double)(pond.y + 17), (double)(pond.maxZ + 4));
                return world.getEntitiesByClass(FishingBobberEntity.class, scan, hook -> hook.getOwner() instanceof ServerPlayerEntity);
            });
            for (FishingBobberEntity hook : hooks) {
                double reach;
                ServerPlayerEntity fisher;
                Entity ownerEntity = hook.getOwner();
                if (!(ownerEntity instanceof ServerPlayerEntity)) continue;
                fisher = (ServerPlayerEntity) ownerEntity;
                if (fisher.getServerWorld() != world) continue;
                double d = reach = PondService.isPremiumRod(fisher) ? 2.4 : 1.1;
                if (hook.squaredDistanceTo(item) > reach * reach) continue;
                PondService.claim(fisher, item, live);
                remove.add(row.getKey());
                continue block0;
            }
        }
        remove.forEach(DROPS::remove);
    }

    private static void claim(ServerPlayerEntity player, ItemEntity item, LiveDrop live) {
        DROPS.remove(item.getUuid());
        ItemStack claimedStack = item.getStack().copy();
        item.discard();
        switch (live.prize.ordinal()) {
            case 1: {
                EconomyService.deposit(player, 50000L);
                player.getServer().getPlayerManager().broadcast(Text.literal(("\u2726 " + player.getName().getString() + " caught the 50,000 Pok\u00e9Dollar pond prize!")).formatted(Formatting.GOLD), false);
                break;
            }
            case 2: {
                String cmd = PondService.data.alphaCommand == null ? "" : PondService.data.alphaCommand.trim();
                String species = live.species;
                if (species == null || cmd.isEmpty() || !PondService.executeAlpha(player, species)) {
                    NbtComponent.set(DataComponentTypes.CUSTOM_DATA, claimedStack, nbt -> {
                        nbt.putString("pond_reward", "alpha");
                        if (species != null) {
                            nbt.putString("pond_species", species);
                        }
                    });
                    PondService.deliver(player, claimedStack);
                    player.sendMessage(Text.literal("Your Shiny Alpha claim item was saved. Ask an administrator to verify the Alpha reward command."), false);
                    CobbleClubServer.LOGGER.error("Pond Shiny Alpha reward for {} ({}) needs manual fulfillment; check alphaCommand in ponds.json", player.getGameProfile().getName(), species);
                    break;
                }
                player.getServer().getPlayerManager().broadcast(Text.literal(("\u2726 " + player.getName().getString() + " caught a Shiny Alpha pond prize!")).formatted(Formatting.LIGHT_PURPLE), false);
                player.sendMessage(Text.literal(("Your pond prize was a Shiny Alpha " + species + "! Check your party or PC.")).formatted(Formatting.LIGHT_PURPLE), false);
                break;
            }
            case 0: {
                PondService.deliver(player, claimedStack);
            }
        }
    }

    private static void deliver(ServerPlayerEntity player, ItemStack stack) {
        if (!player.getInventory().insertStack(stack) && !stack.isEmpty()) {
            player.dropItem(stack, false);
        }
    }

    private static boolean isPremiumRod(ServerPlayerEntity player) {
        return ClubItems.matches(player.getMainHandStack(), "pond_rod") || ClubItems.matches(player.getOffHandStack(), "pond_rod");
    }

    private static List<String> loadAlphaSpecies() {
        try {
            Class<?> registryClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies");
            Object registry = registryClass.getField("INSTANCE").get(null);
            Object entries = registryClass.getMethod("getSpeciesInNamespace", String.class).invoke(registry, "cobblemon");
            if (!(entries instanceof Map)) {
                throw new IllegalStateException("Cobblemon species registry did not return a map");
            }
            Map speciesMap = (Map)entries;
            ArrayList<String> available = new ArrayList<>();
            int skipped = 0;
            Exception firstError = null;
            for (Object species : speciesMap.values()) {
                try {
                    String full;
                    String path;
                    Object identifier;
                    Object implemented;
                    try {
                        implemented = species.getClass().getMethod("getImplemented").invoke(species);
                    }
                    catch (NoSuchMethodException ignored) {
                        implemented = species.getClass().getMethod("isImplemented").invoke(species);
                    }
                    if (!Boolean.TRUE.equals(implemented)) continue;
                    try {
                        identifier = species.getClass().getMethod("getResourceIdentifier").invoke(species);
                    }
                    catch (NoSuchMethodException ignored) {
                        identifier = species.getClass().getField("resourceIdentifier").get(species);
                    }
                    if (!(path = (full = identifier.toString()).substring(full.indexOf(58) + 1)).matches("[a-z0-9_]{1,64}")) continue;
                    available.add(path);
                }
                catch (ReflectiveOperationException | RuntimeException error) {
                    ++skipped;
                    if (firstError != null) continue;
                    firstError = error;
                }
            }
            if (skipped > 0) {
                CobbleClubServer.LOGGER.warn("Skipped {} Cobblemon species while loading pond Alpha prizes", skipped, firstError);
            }
            available.sort(Comparator.naturalOrder());
            CobbleClubServer.LOGGER.info("Fishing pond loaded {} implemented Cobblemon species for Shiny Alpha prizes", available.size());
            return List.copyOf(available);
        }
        catch (ReflectiveOperationException | RuntimeException error) {
            CobbleClubServer.LOGGER.error("Could not read the installed Cobblemon species registry for pond Alpha prizes", error);
            return List.of();
        }
    }

    private static boolean executeAlpha(ServerPlayerEntity player, String species) {
        String command = PondService.data.alphaCommand.replace("{player}", player.getGameProfile().getName()).replace("{species}", species);
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        try {
            CommandManager manager = player.getServer().getCommandManager();
            boolean[] result = new boolean[]{false, false};
            ServerCommandSource rewardSource = player.getServer().getCommandSource().withWorld(player.getServerWorld()).withPosition(player.getPos()).withReturnValueConsumer((successful, returnValue) -> {
                result[0] = true;
                result[1] = successful;
            });
            ParseResults parsed = manager.getDispatcher().parse(command, rewardSource);
            if (parsed.getReader().canRead() || !parsed.getExceptions().isEmpty()) {
                return false;
            }
            manager.executeWithPrefix(rewardSource, command);
            if (!result[0] || !result[1]) {
                CobbleClubServer.LOGGER.error("Pond Alpha reward command reported failure: {}", command);
            }
            return result[0] && result[1];
        }
        catch (RuntimeException error) {
            CobbleClubServer.LOGGER.error("Pond Alpha reward command failed", error);
            return false;
        }
    }

    private static boolean drop(MinecraftServer server, Pond pond, Prize forcedPrize) {
        ServerWorld world = PondService.getWorld(server, pond.dimension);
        if (world == null || pond.water == null || pond.water.isEmpty()) {
            return false;
        }

        boolean nearby = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() == world
                    && player.squaredDistanceTo(pond.centerX(), pond.y, pond.centerZ()) < 9216.0) {
                nearby = true;
                break;
            }
        }

        if (!nearby) {
            return false;
        }

        int start = ThreadLocalRandom.current().nextInt(pond.water.size());
        for (int tries = 0; tries < Math.min(8, pond.water.size()); ++tries) {
            Surface surface = pond.water.get((start + tries) % pond.water.size());
            if (!world.isChunkLoaded(surface.x >> 4, surface.z >> 4)) {
                continue;
            }

            int roll = ThreadLocalRandom.current().nextInt(6000);
            Prize prize = forcedPrize != null
                    ? forcedPrize
                    : (roll == 0 ? Prize.ALPHA : (roll < 5 ? Prize.MONEY : Prize.ITEM));

            List<String> choices = prize != Prize.ITEM
                    ? List.of()
                    : (roll < 20
                    ? (PondService.data.dolls.isEmpty() ? PondService.data.rare : PondService.data.dolls)
                    : (roll < 95
                    ? PondService.data.rare
                    : (roll < 845 ? PondService.data.mid : PondService.data.low)));

            ItemStack stack = prize == Prize.MONEY
                    ? new ItemStack(Items.EMERALD)
                    : (prize == Prize.ALPHA
                    ? new ItemStack(Items.NETHER_STAR)
                    : PondService.pickItem(choices));

            if (stack.isEmpty()) {
                stack = PondService.pickItem(PondService.data.low);
            }
            if (stack.isEmpty()) {
                return false;
            }

            if (prize == Prize.MONEY) {
                stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("✦ 50,000 PokéDollars"));
            }
            if (prize == Prize.ALPHA) {
                stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("✦ Shiny Alpha Pokémon"));
            }

            String species = prize == Prize.ALPHA && !alphaSpecies.isEmpty()
                    ? alphaSpecies.get(ThreadLocalRandom.current().nextInt(alphaSpecies.size()))
                    : null;

            if (prize == Prize.ALPHA && species == null) {
                CobbleClubServer.LOGGER.error(
                        "Pond Alpha species pool is empty; winner will receive a claim item"
                );
            }

            if (prize != Prize.ITEM) {
                NbtComponent.set(
                        DataComponentTypes.CUSTOM_DATA,
                        stack,
                        nbt -> nbt.putString("pond_drop", UUID.randomUUID().toString())
                );
            }

            ItemEntity item = new ItemEntity(
                    world,
                    surface.x + 0.5,
                    surface.y + 12.0,
                    surface.z + 0.5,
                    stack
            );
            item.setPickupDelayInfinite();
            item.addCommandTag(DROP_TAG);
            DROPS.put(item.getUuid(), new LiveDrop(item, prize, species, pond));

            if (!world.spawnEntity(item)) {
                DROPS.remove(item.getUuid());
                return false;
            }

            return true;
        }

        return false;
    }

    private static ItemStack pickItem(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int start = ThreadLocalRandom.current().nextInt(ids.size());
        for (int i = 0; i < ids.size(); ++i) {
            String raw = ids.get((start + i) % ids.size());
            Identifier id = raw == null ? null : Identifier.tryParse(raw);

            if (id == null || !Registries.ITEM.containsId(id)) {
                continue;
            }

            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) {
                continue;
            }

            return new ItemStack(item, 1);
        }

        return ItemStack.EMPTY;
    }

    private static void discoverAddonItems() {
        for (Identifier id : Registries.ITEM.getIds()) {
            String ns = id.getNamespace();
            if (ns.equals("minecraft") || ns.equals("cobblemon") || ns.equals("cobbleclub")) continue;
            String path = id.getPath();
            String full = id.toString();
            if ((path.contains("doll") || path.contains("plush")) && PondService.data.dolls.size() < 80 && !PondService.data.dolls.contains(full)) {
                PondService.data.dolls.add(full);
                continue;
            }
            if ((path.endsWith("_berry") || path.endsWith("_apricorn")) && PondService.data.low.size() < 80 && !PondService.data.low.contains(full)) {
                PondService.data.low.add(full);
                continue;
            }
            if (!path.contains("candy") && !path.endsWith("_stone") || PondService.data.mid.size() >= 80 || PondService.data.mid.contains(full)) continue;
            PondService.data.mid.add(full);
        }
    }

    private static Pond nextReady(MinecraftServer server) {
        Pond forced;
        if (PondService.data.forcedName != null && (forced = PondService.data.ponds.get(PondService.data.forcedName)) != null && forced.ready() && PondService.getWorld(server, forced.dimension) != null) {
            return forced;
        }
        ArrayList<Pond> ready = new ArrayList<>();
        for (Pond pond : PondService.data.ponds.values()) {
            if (pond == null || !pond.ready() || PondService.getWorld(server, pond.dimension) == null) continue;
            ready.add(pond);
        }
        return ready.isEmpty() ? null : ready.get(Math.floorMod(PondService.data.rotation, ready.size()));
    }

    private static void begin(MinecraftServer server, Pond pond) {
        activePond = pond.name;
        long now = System.currentTimeMillis();
        activeUntil = now + 240000L;
        PondService.data.nextAt = now + 2700000L;
        nextDrop = System.currentTimeMillis();
        ++PondService.data.rotation;
        PondService.data.forcedName = null;
        PondService.save();
        server.getPlayerManager().broadcast(Text.literal(("\u2726 " + pond.name + " Fishing Pond is LIVE! Type /pond.")).formatted(Formatting.AQUA), false);
    }

    private static void end(MinecraftServer server) {
        activePond = null;
        activeUntil = 0L;
        warned = false;
        if (PondService.data.nextAt < System.currentTimeMillis() + 60000L) {
            PondService.data.nextAt = System.currentTimeMillis() + 2700000L;
        }
        PondService.save();
        BAR.clearPlayers();
        server.getPlayerManager().broadcast(Text.literal("\u2726 Fishing Pond has ended. The next one opens on the 45-minute cycle."), false);
    }

    private static void showBar(MinecraftServer server, String name, float progress, String suffix) {
        BAR.setName(Text.literal(("\u2726 Fishing Pond " + suffix)));
        BAR.setPercent(progress);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BAR.addPlayer(player);
        }
    }

    private static void tickTravel(MinecraftServer server) {
        long now = System.currentTimeMillis();
        boolean generating = false;
        for (PendingTravel pendingTravel : TRAVELS.values()) {
            if (pendingTravel.future == null || pendingTravel.future.isDone()) continue;
            generating = true;
            break;
        }
        for (Map.Entry<UUID, PendingTravel> entry : new ArrayList<>(TRAVELS.entrySet())) {
            PendingTravel travel = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            ServerWorld world = PondService.getWorld(server, travel.dimension);
            if (player == null || world == null || now - travel.createdAt > 15000L) {
                TRAVELS.remove(entry.getKey());
                if (player == null) continue;
                player.sendMessage(Text.literal("Pond travel timed out; please try /pond again."), true);
                continue;
            }
            int cx = travel.spot.x >> 4;
            int cz = travel.spot.z >> 4;
            if (world.isChunkLoaded(cx, cz)) {
                player.teleport(world, (double)travel.spot.x + 0.5, (double)travel.spot.y, (double)travel.spot.z + 0.5, travel.spot.yaw, 0.0f);
                TRAVELS.remove(entry.getKey());
                continue;
            }
            if (travel.future != null) {
                if (!travel.future.isDone()) continue;
                TRAVELS.remove(entry.getKey());
                player.sendMessage(Text.literal("Pond spawn could not load; please try another /pond spot."), true);
                continue;
            }
            if (generating) continue;
            travel.future = world.getChunkManager().getChunkFutureSyncOnMainThread(cx, cz, ChunkStatus.FULL, false);
            generating = true;
        }
    }

    private static int teleport(ServerPlayerEntity player) {
        Spot spot;
        Pond pond;
        if (player == null) {
            return 0;
        }
        Pond pond2 = pond = activePond == null ? PondService.nextReady(player.getServer()) : PondService.data.ponds.get(activePond);
        if (pond == null) {
            player.sendMessage(Text.literal("No fishing pond has its 10 safe spawn spots set yet."), false);
            return 0;
        }
        ServerWorld world = PondService.getWorld(player.getServer(), pond.dimension);
        if (world == null) {
            return 0;
        }
        if ((spot = pond.spawns.get(Math.floorMod(spawnCursor++, 10))) == null) {
            return 0;
        }
        if (world.isChunkLoaded(spot.x >> 4, spot.z >> 4)) {
            player.teleport(world, (double)spot.x + 0.5, (double)spot.y, (double)spot.z + 0.5, spot.yaw, 0.0f);
            player.sendMessage(Text.literal((activePond == null ? "Fishing Pond opens soon; wait for the countdown." : "Good luck fishing!")), true);
        } else {
            TRAVELS.put(player.getUuid(), new PendingTravel(pond.dimension, spot));
            player.sendMessage(Text.literal("Preparing your pond spawn without freezing the server..."), true);
        }
        return 1;
    }

    private static int setCorner(ServerCommandSource source, boolean first) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        CornerSelection selection = SELECTIONS.computeIfAbsent(player.getUuid(), ignored -> new CornerSelection());
        String dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        if (selection.dimension != null && !selection.dimension.equals(dimension)) {
            selection = new CornerSelection();
        }
        selection.dimension = dimension;
        if (first) {
            selection.a = player.getBlockPos();
        } else {
            selection.b = player.getBlockPos();
        }
        SELECTIONS.put(player.getUuid(), selection);
        int number = first ? 1 : 2;
        BlockPos pos = player.getBlockPos();
        source.sendFeedback(() -> Text.literal(("Pond corner " + number + " set at " + pos.toShortString() + ".")), false);
        return 1;
    }

    private static int create(ServerCommandSource source, String rawName) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        String name = rawName.toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]{1,24}") || PondService.data.ponds.containsKey(name)) {
            return PondService.fail(source, "Use a new pond name of 1\u201324 letters, numbers, _ or -.");
        }
        CornerSelection selection = SELECTIONS.get(player.getUuid());
        if (selection == null || selection.a == null || selection.b == null) {
            return PondService.fail(source, "Set both corners while standing at the pond: pond pos1 and pos2.");
        }
        ServerWorld world = player.getServerWorld();
        if (!selection.dimension.equals(world.getRegistryKey().getValue().toString())) {
            return PondService.fail(source, "Create the pond in the selected world.");
        }
        int minX = Math.min(selection.a.getX(), selection.b.getX());
        int maxX = Math.max(selection.a.getX(), selection.b.getX());
        int minZ = Math.min(selection.a.getZ(), selection.b.getZ());
        int maxZ = Math.max(selection.a.getZ(), selection.b.getZ());
        int minY = Math.min(selection.a.getY(), selection.b.getY()) - 3;
        int maxY = Math.max(selection.a.getY(), selection.b.getY()) + 2;
        if (maxX - minX > 63 || maxZ - minZ > 63 || maxY - minY > 12) {
            return PondService.fail(source, "Pond area must fit within 64\u00d764 blocks and 12 blocks of height.");
        }
        ArrayList<Surface> water = new ArrayList<>();
        for (int x = minX; x <= maxX; ++x) {
            block1: for (int z = minZ; z <= maxZ; ++z) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    return PondService.fail(source, "Visit or load every selected pond chunk before creating the pond.");
                }
                for (int y = maxY; y >= minY; --y) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.getBlockState(pos).isOf(Blocks.WATER) || !world.getBlockState(pos.up()).isAir()) continue;
                    water.add(new Surface(x, y + 1, z));
                    continue block1;
                }
            }
        }
        if (water.size() < 16) {
            return PondService.fail(source, "Select at least 16 open water surface blocks in the pond.");
        }
        Pond pond = new Pond();
        pond.name = name;
        pond.dimension = selection.dimension;
        pond.y = (water.get((int)0)).y;
        pond.minX = minX;
        pond.maxX = maxX;
        pond.minZ = minZ;
        pond.maxZ = maxZ;
        pond.water = water;
        PondService.data.ponds.put(name, pond);
        SELECTIONS.remove(player.getUuid());
        PondService.save();
        source.sendFeedback(() -> Text.literal(("Pond " + name + " saved with " + water.size() + " water spots. Set 10 shore spawns with /cobbleclubserver pond spawn " + name + " <1-10>.")), true);
        return 1;
    }

    private static int setSpawn(ServerCommandSource source, String rawName, int slot) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        Pond pond = PondService.data.ponds.get(rawName.toLowerCase(Locale.ROOT));
        if (pond == null || !pond.dimension.equals(player.getServerWorld().getRegistryKey().getValue().toString())) {
            return PondService.fail(source, "Stand beside this pond in its world.");
        }
        BlockPos pos = player.getBlockPos();
        ServerWorld world = player.getServerWorld();
        if (!(world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty() && world.getBlockState(pos).getFluidState().isEmpty())) {
            return PondService.fail(source, "Stand on solid ground with two clear blocks above it.");
        }
        if (Math.hypot((double)pos.getX() - pond.centerX(), (double)pos.getZ() - pond.centerZ()) > 80.0) {
            return PondService.fail(source, "Stand within 80 blocks of the pond center.");
        }
        for (int i = 0; i < pond.spawns.size(); ++i) {
            Spot other = pond.spawns.get(i);
            if (i == slot - 1 || other == null || !(Math.hypot(pos.getX() - other.x, pos.getZ() - other.z) < 5.0)) continue;
            return PondService.fail(source, "Set each spawn at least five blocks from the other nine to spread players around the pond.");
        }
        while (pond.spawns.size() < 10) {
            pond.spawns.add(null);
        }
        pond.spawns.set(slot - 1, new Spot(pos.getX(), pos.getY(), pos.getZ(), player.getYaw()));
        PondService.save();
        source.sendFeedback(() -> Text.literal(("Pond " + pond.name + " spawn " + slot + " set. " + pond.countSpawns() + "/10 ready.")), true);
        return 1;
    }

    private static int start(ServerCommandSource source, String name) {
        Pond pond = PondService.data.ponds.get(name.toLowerCase(Locale.ROOT));
        if (pond == null || !pond.ready()) {
            return PondService.fail(source, "Set all 10 spawns before starting this pond.");
        }
        if (PondService.getWorld(source.getServer(), pond.dimension) == null) {
            return PondService.fail(source, "The pond world is unavailable.");
        }
        if (activePond != null) {
            return PondService.fail(source, "A fishing pond is already active.");
        }
        PondService.data.nextAt = System.currentTimeMillis() + 60000L;
        PondService.data.forcedName = pond.name;
        warned = false;
        PondService.save();
        source.sendFeedback(() -> Text.literal(("Pond " + pond.name + " will open after the 60-second countdown.")), true);
        return 1;
    }

    private static int stop(ServerCommandSource source) {
        if (activePond != null) {
            PondService.end(source.getServer());
        } else {
            PondService.data.nextAt = System.currentTimeMillis() + 2700000L;
            PondService.data.forcedName = null;
            BAR.clearPlayers();
            warned = false;
            PondService.save();
        }
        source.sendFeedback(() -> Text.literal("Pond event stopped; the 45-minute cycle has been reset."), true);
        return 1;
    }

    private static int testAlpha(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return PondService.fail(source, "Stand beside a pond to test its Alpha drop.");
        }
        if (alphaSpecies.isEmpty()) {
            return PondService.fail(source, "No implemented Cobblemon species were loaded for the Alpha prize; check the server log.");
        }
        if (DROPS.size() >= 40) {
            return PondService.fail(source, "Wait for pond drops to clear before testing again.");
        }
        Pond closest = null;
        double distance = 9216.0;
        for (Pond pond : PondService.data.ponds.values()) {
            double candidate;
            if (pond.water == null || pond.water.isEmpty() || !pond.dimension.equals(player.getServerWorld().getRegistryKey().getValue().toString()) || !((candidate = player.squaredDistanceTo(pond.centerX(), (double)pond.y, pond.centerZ())) < distance)) continue;
            distance = candidate;
            closest = pond;
        }
        if (closest == null) {
            return PondService.fail(source, "Stand within 96 blocks of a saved pond in its world.");
        }
        if (!PondService.drop(source.getServer(), closest, Prize.ALPHA)) {
            return PondService.fail(source, "Could not drop the Alpha prize. Load the pond chunks and stand nearby.");
        }
        source.sendFeedback(() -> Text.literal("A Shiny Alpha prize is falling over the pond. Hook the item with your rod to claim one random Shiny Alpha; ground pickup unlocks ten seconds after landing."), false);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        for (Pond pond : PondService.data.ponds.values()) {
            source.sendFeedback(() -> Text.literal((pond.name + " \u2022 " + pond.dimension + " \u2022 " + pond.countSpawns() + "/10 spawn points \u2022 " + pond.water.size() + " water spots")), false);
        }
        return PondService.data.ponds.size();
    }

    private static int status(ServerCommandSource source) {
        long ms = activePond == null ? PondService.data.nextAt - System.currentTimeMillis() : activeUntil - System.currentTimeMillis();
        source.sendFeedback(() -> Text.literal((activePond == null ? "Next fishing pond in " + Math.max(0L, ms / 1000L) + "s." : activePond + " Fishing Pond is active for " + Math.max(0L, ms / 1000L) + "s.")), false);
        return 1;
    }

    private static ServerWorld getWorld(MinecraftServer server, String dimension) {
        Identifier id = dimension == null ? null : Identifier.tryParse(dimension);
        return server == null || id == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
    }

    private static int fail(ServerCommandSource source, String message) {
        source.sendError(Text.literal(message));
        return 0;
    }

    private static void save() {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8);
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.error("Could not save fishing ponds", error);
        }
    }

    static {
        alphaSpecies = List.of();
    }

    private static final class PondFile {
        long nextAt;
        int rotation;
        String forcedName;
        Map<String, Pond> ponds = new LinkedHashMap<>();
        List<String> low = new ArrayList<String>(List.of("cobblemon:poke_ball", "cobblemon:great_ball", "cobblemon:oran_berry", "cobblemon:pecha_berry", "cobblemon:cheri_berry", "cobblemon:chesto_berry", "cobblemon:rawst_berry", "cobblemon:sitrus_berry", "cobblemon:red_apricorn", "cobblemon:blue_apricorn", "cobblemon:yellow_apricorn", "cobblemon:green_apricorn", "cobblemon:iron", "cobblemon:protein", "minecraft:experience_bottle", "minecraft:gold_nugget", "minecraft:prismarine_shard", "minecraft:nautilus_shell"));
        List<String> mid = new ArrayList<String>(List.of("cobblemon:ultra_ball", "cobblemon:quick_ball", "cobblemon:dusk_ball", "cobblemon:timer_ball", "cobblemon:rare_candy", "cobblemon:exp_candy_s", "cobblemon:exp_candy_m", "cobblemon:water_stone", "cobblemon:thunder_stone", "cobblemon:fire_stone", "cobblemon:leaf_stone", "minecraft:diamond", "minecraft:heart_of_the_sea"));
        List<String> rare = new ArrayList<String>(List.of("cobblemon:exp_candy_l", "cobblemon:exp_candy_xl", "cobblemon:beast_ball", "cobblemon:ability_capsule", "cobblemon:gold_bottle_cap", "minecraft:enchanted_golden_apple"));
        List<String> dolls = new ArrayList<>();
        String alphaCommand = "givepokemonother {player} {species} alpha=true shiny=true";

        private PondFile() {
        }
    }

    private static final class Pond {
        String name;
        String dimension;
        int minX;
        int maxX;
        int minZ;
        int maxZ;
        int y;
        List<Surface> water = new ArrayList<>();
        List<Spot> spawns = new ArrayList<>();

        private Pond() {
        }

        int countSpawns() {
            int n = 0;
            if (this.spawns != null) {
                for (Spot spot : this.spawns) {
                    if (spot == null) continue;
                    ++n;
                }
            }
            return n;
        }

        boolean ready() {
            return this.water != null && !this.water.isEmpty() && this.spawns != null && this.spawns.size() == 10 && this.countSpawns() == 10;
        }

        double centerX() {
            return (double)(this.minX + this.maxX) / 2.0;
        }

        double centerZ() {
            return (double)(this.minZ + this.maxZ) / 2.0;
        }
    }

    private enum Prize {
        ITEM,
        MONEY,
        ALPHA;

    }

    private static final class LiveDrop {
        final ItemEntity item;
        final Prize prize;
        final String species;
        final Pond pond;
        long landedAt;

        LiveDrop(ItemEntity item, Prize prize, String species, Pond pond) {
            this.item = item;
            this.prize = prize;
            this.species = species;
            this.pond = pond;
        }
    }

    private static final class Surface {
        int x;
        int y;
        int z;

        Surface(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class PendingTravel {
        final String dimension;
        final Spot spot;
        final long createdAt = System.currentTimeMillis();
        CompletableFuture<?> future;

        PendingTravel(String dimension, Spot spot) {
            this.dimension = dimension;
            this.spot = spot;
        }
    }

    private static final class Spot {
        int x;
        int y;
        int z;
        float yaw;

        Spot(int x, int y, int z, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }

    private static final class CornerSelection {
        String dimension;
        BlockPos a;
        BlockPos b;

        private CornerSelection() {
        }
    }
}
