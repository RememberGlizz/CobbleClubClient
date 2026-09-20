package com.cobbleclub.server;

import com.cobbleclub.clubhouse.claims.protocol.BudgetInfo;
import com.cobbleclub.server.chestshop.ChestShopService;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.ServerNetworking;
import com.cobbleclub.server.service.CatalogService;
import com.cobbleclub.server.service.ClaimPresenceService;
import com.cobbleclub.server.service.ClaimsService;
import com.cobbleclub.server.service.ClubItems;
import com.cobbleclub.server.service.CosmeticVisualService;
import com.cobbleclub.server.service.CrateService;
import com.cobbleclub.server.service.DashboardService;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.FeatherboardService;
import com.cobbleclub.server.service.KitsService;
import com.cobbleclub.server.service.KantoRctService;
import com.cobbleclub.server.service.LaunchService;
import com.cobbleclub.server.service.LeaderboardService;
import com.cobbleclub.server.service.PermissionService;
import com.cobbleclub.server.service.PondService;
import com.cobbleclub.server.service.RtpService;
import com.cobbleclub.server.service.StoreBridgeService;
import com.cobbleclub.server.service.TagsService;
import com.cobbleclub.server.service.VoteRewardService;
import com.cobbleclub.server.service.WardrobeService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.text.MutableText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobbleClubServer
        implements ModInitializer {
    public static final String MOD_ID = "cobbleclub";
    public static final Logger LOGGER = LoggerFactory.getLogger("CobbleClub Server");
    private static final Map<UUID, String> CLIENT_VERSIONS = new ConcurrentHashMap<>();
    private static final String UI_PROTOCOL = "wild5-tags24-wool9-noblur1-managedborder1-featherboard1";
    private static ServerConfig config;

    public void onInitialize() {
        config = ServerConfig.load();
        ServerNetworking.register();
        CobbleClubServer.registerCommands();
        CobbleClubServer.registerProtection();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataStore.load(server);
            ClaimsStore.load(server);
            EconomyService.initialize(server);
            VoteRewardService.initialize(server);
            LeaderboardService.load(server);
            PondService.initialize(server);
            LaunchService.initialize(server);
            LOGGER.info("CobbleClub Server {} started with {} persistent claims", CobbleClubServer.serverModVersion(), ClaimsStore.all().size());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerDataStore.save();
            ClaimsStore.save();
            CLIENT_VERSIONS.clear();
            ClaimPresenceService.clear();
            LeaderboardService.save();
            PondService.shutdown();
            LaunchService.shutdown();
            RtpService.shutdown(server);
            VoteRewardService.shutdown();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            EconomyService.tick(server);
            LeaderboardService.tick(server);
            ClaimPresenceService.tick(server);
            ClaimsService.syncWorldSnapshots(server);
            RtpService.tick(server);
            LaunchService.tick(server);
            PondService.tick(server);
            TagsService.tick(server);
            CosmeticVisualService.tick(server);
            StoreBridgeService.tick(server);
            FeatherboardService.tick(server);
            KantoRctService.tick(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CobbleClubServer.safeJoinStep(handler.player, "economy", () -> EconomyService.data(handler.player));
            CobbleClubServer.safeJoinStep(handler.player, "tag rank", () -> TagsService.syncRank(handler.player, false));
            CobbleClubServer.safeJoinStep(handler.player, "tags", () -> TagsService.apply(handler.player));
            CobbleClubServer.safeJoinStep(handler.player, "cosmetics", () -> CosmeticVisualService.apply(handler.player));
            CobbleClubServer.safeJoinStep(handler.player, "cosmetic visibility", () -> CosmeticVisualService.onJoin(handler.player));
            CobbleClubServer.safeJoinStep(handler.player, "Newb Kit", () -> KitsService.onJoin(handler.player));
            CobbleClubServer.safeJoinStep(handler.player, "pending vote rewards", () -> VoteRewardService.onJoin(handler.player));
            CobbleClubServer.safeJoinStep(handler.player, "claim borders", () -> ClaimsService.syncWorldSnapshots(server));
            CobbleClubServer.safeJoinStep(handler.player, "featherboard online", () -> FeatherboardService.playerJoined(server));
            PlayerDataStore.save();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FeatherboardService.playerDisconnected(server, handler.player.getUuid());
            CosmeticVisualService.remove(handler.player);
            ClaimsService.forgetWorldSnapshot(handler.player);
            RtpService.forget(handler.player);
            LaunchService.forget(handler.player);
            CLIENT_VERSIONS.remove(handler.player.getUuid());
            PlayerDataStore.save();
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                EconomyService.handleDeath(player);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> PondService.onEntityLoaded(entity));
    }

    public static ServerConfig config() {
        if (config == null) {
            config = ServerConfig.load();
        }
        return config;
    }

    public static void acceptHandshake(ServerPlayerEntity player, String version) {
        String clientBuild = version == null ? "unknown" : version;
        String serverBuild = CobbleClubServer.serverModVersion() + "|" + UI_PROTOCOL;
        CLIENT_VERSIONS.put(player.getUuid(), clientBuild);
        LOGGER.info("CobbleClub client {} connected with build {} (server {})", new Object[]{player.getGameProfile().getName(), clientBuild, serverBuild});
        if (!serverBuild.equals(clientBuild)) {
            LOGGER.warn("Rejecting CobbleClub UI build mismatch for {}: client={}, server={}", new Object[]{player.getGameProfile().getName(), clientBuild, serverBuild});
            player.networkHandler.disconnect(Text.literal("CobbleClub client/server mismatch. Client: " + clientBuild + " | Server: " + serverBuild + ". Install the exact matching CobbleClub jar on your client.").formatted(Formatting.RED));
        }
    }

    private static String serverModVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    public static void requiresClient(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("The matching CobbleClub client mod " + CobbleClubServer.serverModVersion() + " is required to open this menu."), false);
    }

    private static void safeJoinStep(ServerPlayerEntity player, String step, Runnable action) {
        try {
            action.run();
        }
        catch (Exception error) {
            LOGGER.error("Could not initialize {} for {}; login will continue", new Object[]{step, player.getGameProfile().getName(), error});
        }
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LeaderboardService.registerCommands(dispatcher);
            PondService.registerCommands(dispatcher);
            LaunchService.registerCommands(dispatcher);
            dispatcher.register(CommandManager.literal("cobbleclubrctinternal")
                    .then(CommandManager.literal("kanto")
                            .then(CommandManager.argument("milestone", StringArgumentType.word())
                                    .executes(context -> KantoRctService.handleReward(
                                            ((ServerCommandSource)context.getSource()).getPlayer(),
                                            StringArgumentType.getString(context, "milestone")
                                    )))));
            dispatcher.register((CommandManager.literal("club").requires(source -> PermissionService.has(source, "cobbleclub.command.club", true))).executes(context -> {
                DashboardService.open(((ServerCommandSource)context.getSource()).getPlayer());
                return 1;
            }));
            dispatcher.register((CommandManager.literal(MOD_ID).requires(source -> PermissionService.has(source, "cobbleclub.command.club", true))).executes(context -> {
                DashboardService.open(((ServerCommandSource)context.getSource()).getPlayer());
                return 1;
            }));
            dispatcher.register((CommandManager.literal("kits").requires(source -> PermissionService.has(source, "cobbleclub.command.kits", true))).executes(context -> {
                KitsService.open(((ServerCommandSource)context.getSource()).getPlayer());
                return 1;
            }));
            dispatcher.register((CommandManager.literal("kit").requires(source -> PermissionService.has(source, "cobbleclub.command.kits", true))).executes(context -> {
                KitsService.open(((ServerCommandSource)context.getSource()).getPlayer());
                return 1;
            }));
            dispatcher.register((((((CommandManager.literal("claim").requires(source -> PermissionService.has(source, "cobbleclub.command.claim", true))).executes(context -> CobbleClubServer.openClaims(((ServerCommandSource)context.getSource()).getPlayer()))).then(CommandManager.literal("menu").executes(context -> CobbleClubServer.openClaims(((ServerCommandSource)context.getSource()).getPlayer())))).then((CommandManager.literal("tool").requires(source -> PermissionService.has(source, "cobbleclub.command.claim.tool", true))).executes(context -> {
                ServerPlayerEntity player = ((ServerCommandSource)context.getSource()).getPlayer();
                if (KitsService.hasClaimingTool(player)) {
                    player.sendMessage(Text.literal("You already have a CobbleClub Claiming Tool."), false);
                    return 0;
                }
                boolean given = KitsService.recoverClaimingTool(player);
                player.sendMessage(Text.literal(given ? "A replacement CobbleClub Claiming Tool was added to your inventory." : "The claiming tool could not be created; please contact an administrator."), false);
                return given ? 1 : 0;
            }))).then((CommandManager.literal("blocks").requires(source -> PermissionService.has(source, "cobbleclub.command.claim.blocks", true))).executes(context -> {
                ServerPlayerEntity player = ((ServerCommandSource)context.getSource()).getPlayer();
                BudgetInfo budget = ClaimsService.budget(player);
                player.sendMessage(Text.literal("Claim blocks: " + budget.getRemaining() + " available / " + budget.getTotal() + " total."), false);
                return 1;
            }))).then(CommandManager.literal("subclaim").then((CommandManager.literal("create").requires(source -> PermissionService.has(source, "cobbleclub.command.claim.subclaim.create", true))).then(CommandManager.argument("name", StringArgumentType.word()).then(CommandManager.argument("x1", IntegerArgumentType.integer()).then(CommandManager.argument("y1", IntegerArgumentType.integer()).then(CommandManager.argument("z1", IntegerArgumentType.integer()).then(CommandManager.argument("x2", IntegerArgumentType.integer()).then(CommandManager.argument("y2", IntegerArgumentType.integer()).then(CommandManager.argument("z2", IntegerArgumentType.integer()).executes(context -> {
                ServerPlayerEntity player;
                boolean made = ClaimsService.createSubclaim(player = ((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "name"), new BlockPos(IntegerArgumentType.getInteger(context, "x1"), IntegerArgumentType.getInteger(context, "y1"), IntegerArgumentType.getInteger(context, "z1")), new BlockPos(IntegerArgumentType.getInteger(context, "x2"), IntegerArgumentType.getInteger(context, "y2"), IntegerArgumentType.getInteger(context, "z2")));
                player.sendMessage(Text.literal(made ? "Subclaim created. Open /claim to edit it." : "Could not create that subclaim. Both corners must be inside one of your claims and may not overlap another subclaim."), false);
                return made ? 1 : 0;
            })))))))))));
            dispatcher.register((CommandManager.literal("claims").requires(source -> PermissionService.has(source, "cobbleclub.command.claim", true))).executes(context -> CobbleClubServer.openClaims(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register(((CommandManager.literal("tag").requires(source -> PermissionService.has(source, "cobbleclub.command.tags", true))).executes(context -> CobbleClubServer.openTags(((ServerCommandSource)context.getSource()).getPlayer()))).then((CommandManager.literal("buy").requires(source -> PermissionService.has(source, "cobbleclub.command.tags.buy", true))).then(CommandManager.argument("tag", StringArgumentType.word()).executes(context -> {
                boolean bought = TagsService.buy(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "tag"));
                if (!bought) {
                    ((ServerCommandSource)context.getSource()).sendError(Text.literal("That tag is unavailable, already owned, or you cannot afford it."));
                }
                return bought ? 1 : 0;
            }))));
            dispatcher.register((CommandManager.literal("tags").requires(source -> PermissionService.has(source, "cobbleclub.command.tags", true))).executes(context -> CobbleClubServer.openTags(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register(((CommandManager.literal("wardrobe").requires(source -> PermissionService.has(source, "cobbleclub.command.wardrobe", true))).executes(context -> CobbleClubServer.openWardrobe(((ServerCommandSource)context.getSource()).getPlayer()))).then(((CommandManager.literal("buy").requires(source -> PermissionService.has(source, "cobbleclub.command.wardrobe.buy", true))).then(CommandManager.literal("cosmetic").then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> CobbleClubServer.buyWardrobe(((ServerCommandSource)context.getSource()).getPlayer(), "cosmetic", StringArgumentType.getString(context, "id")))))).then(CommandManager.literal("glow").then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> CobbleClubServer.buyWardrobe(((ServerCommandSource)context.getSource()).getPlayer(), "glow", StringArgumentType.getString(context, "id")))))));
            dispatcher.register(((((CommandManager.literal("balance").requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))).executes(context -> CobbleClubServer.showBalance(((ServerCommandSource)context.getSource()).getPlayer(), null))).then(CommandManager.literal("leaderboard").executes(context -> CobbleClubServer.sendBalanceLeaderboard(((ServerCommandSource)context.getSource()).getPlayer())))).then(CommandManager.literal("top").executes(context -> CobbleClubServer.sendBalanceLeaderboard(((ServerCommandSource)context.getSource()).getPlayer())))).then(CommandManager.argument("player", StringArgumentType.word()).executes(context -> CobbleClubServer.showBalance(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player")))));
            dispatcher.register(((((CommandManager.literal("bal").requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))).executes(context -> CobbleClubServer.showBalance(((ServerCommandSource)context.getSource()).getPlayer(), null))).then(CommandManager.literal("leaderboard").executes(context -> CobbleClubServer.sendBalanceLeaderboard(((ServerCommandSource)context.getSource()).getPlayer())))).then(CommandManager.literal("top").executes(context -> CobbleClubServer.sendBalanceLeaderboard(((ServerCommandSource)context.getSource()).getPlayer())))).then(CommandManager.argument("player", StringArgumentType.word()).executes(context -> CobbleClubServer.showBalance(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player")))));
            dispatcher.register((CommandManager.literal("baltop").requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))).executes(context -> CobbleClubServer.sendBalanceLeaderboard(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("gems").requires(source -> PermissionService.has(source, "cobbleclub.command.gems", true))).executes(context -> {
                ServerPlayerEntity player = ((ServerCommandSource)context.getSource()).getPlayer();
                player.sendMessage(Text.literal("CobbleClub gems: " + EconomyService.formatGems(EconomyService.gems(player))), false);
                return 1;
            }));
            dispatcher.register((CommandManager.literal("pay").requires(source -> PermissionService.has(source, "cobbleclub.command.pay", true))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(1L)).executes(context -> CobbleClubServer.payCommand(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))));
            dispatcher.register((((((((CommandManager.literal("economy").requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))).executes(context -> CobbleClubServer.showBalance(((ServerCommandSource)context.getSource()).getPlayer(), null))).then(CommandManager.literal("balance").executes(context -> CobbleClubServer.showBalance(((ServerCommandSource)context.getSource()).getPlayer(), null)))).then(CommandManager.literal("top").executes(context -> CobbleClubServer.sendBalanceLeaderboard(((ServerCommandSource)context.getSource()).getPlayer())))).then((CommandManager.literal("pay").requires(source -> PermissionService.has(source, "cobbleclub.command.pay", true))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(1L)).executes(context -> CobbleClubServer.payCommand(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))))).then((CommandManager.literal("give").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.give", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(1L)).executes(context -> CobbleClubServer.adminGiveMoney(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))))).then((CommandManager.literal("take").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.take", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(1L)).executes(context -> CobbleClubServer.adminTakeMoney(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))))).then((CommandManager.literal("set").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.set", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(0L)).executes(context -> CobbleClubServer.adminSetMoney(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount")))))));
            dispatcher.register((CommandManager.literal("daily").requires(source -> PermissionService.has(source, "cobbleclub.command.daily", true))).executes(context -> {
                boolean claimed = EconomyService.claimDaily(((ServerCommandSource)context.getSource()).getPlayer());
                if (!claimed) {
                    ((ServerCommandSource)context.getSource()).sendError(Text.literal("Your next CobbleClub daily reward is not ready yet."));
                }
                return claimed ? 1 : 0;
            }));
            dispatcher.register((((((((((((CommandManager.literal("rtp").requires(source -> PermissionService.has(source, "cobbleclub.command.rtp", true))).executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), null))).then(CommandManager.literal("purple").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "purple")))).then(CommandManager.literal("red").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "red")))).then(CommandManager.literal("orange").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "orange")))).then(CommandManager.literal("yellow").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "yellow")))).then(CommandManager.literal("resource").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "resource")))).then(CommandManager.literal("resources").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "resource")))).then(CommandManager.literal("blue").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "blue")))).then(CommandManager.literal("pink").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "pink")))).then(CommandManager.literal("green").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "green")))).then(CommandManager.literal("cyan").executes(context -> RtpService.start(((ServerCommandSource)context.getSource()).getPlayer(), "cyan"))));
            dispatcher.register((CommandManager.literal("vote").requires(source -> PermissionService.has(source, "cobbleclub.command.vote", true))).executes(context -> CobbleClubServer.sendVoteLinks(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("votes").requires(source -> PermissionService.has(source, "cobbleclub.command.vote", true))).executes(context -> CobbleClubServer.sendVoteLinks(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("shop").requires(source -> PermissionService.has(source, "cobbleclub.command.store", true))).executes(context -> CobbleClubServer.sendStore(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("store").requires(source -> PermissionService.has(source, "cobbleclub.command.store", true))).executes(context -> CobbleClubServer.sendStore(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("wiki").requires(source -> PermissionService.has(source, "cobbleclub.command.wiki", true))).executes(context -> CobbleClubServer.sendWiki(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("features").requires(source -> PermissionService.has(source, "cobbleclub.command.features", true))).executes(context -> CobbleClubServer.sendFeatures(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((((CommandManager.literal("crate").requires(source -> PermissionService.has(source, "cobbleclub.command.crates", true))).executes(context -> CobbleClubServer.showKeys(((ServerCommandSource)context.getSource()).getPlayer()))).then(CommandManager.literal("preview").then(CommandManager.argument("crate", StringArgumentType.word()).executes(context -> {
                CatalogService.openCrate(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "crate"));
                return 1;
            })))).then(CommandManager.literal("open").then(CommandManager.argument("crate", StringArgumentType.word()).executes(context -> CrateService.open(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "crate")) ? 1 : 0))));
            dispatcher.register((CommandManager.literal("crates").requires(source -> PermissionService.has(source, "cobbleclub.command.crates", true))).executes(context -> CobbleClubServer.showKeys(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("keys").requires(source -> PermissionService.has(source, "cobbleclub.command.crates", true))).executes(context -> CobbleClubServer.showKeys(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((CommandManager.literal("pokemonpreview").requires(source -> PermissionService.has(source, "cobbleclub.command.preview.pokemon", true))).executes(context -> {
                CatalogService.openPokemonSkins(((ServerCommandSource)context.getSource()).getPlayer());
                return 1;
            }));
            dispatcher.register((CommandManager.literal("gearpreview").requires(source -> PermissionService.has(source, "cobbleclub.command.preview.gear", true))).executes(context -> {
                CatalogService.openGear(((ServerCommandSource)context.getSource()).getPlayer());
                return 1;
            }));
            dispatcher.register((CommandManager.literal("cratepreview").requires(source -> PermissionService.has(source, "cobbleclub.command.preview.crate", true))).then(CommandManager.argument("crate", StringArgumentType.word()).executes(context -> {
                CatalogService.openCrate(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "crate"));
                return 1;
            })));
            dispatcher.register((CommandManager.literal("clubitem").then((CommandManager.literal("list").requires(source -> PermissionService.admin(source, "cobbleclub.admin.clubitem.list", 2))).executes(context -> {
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("CobbleClub items: " + String.join(", ", ClubItems.ids())), false);
                return ClubItems.ids().size();
            }))).then((CommandManager.literal("give").requires(source -> PermissionService.admin(source, "cobbleclub.admin.clubitem.give", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                return target != null && ClubItems.give(target, StringArgumentType.getString(context, "id"), 1) ? 1 : 0;
            })))));
            dispatcher.register(((((((((((CommandManager.literal("cobbleclubserver").then((CommandManager.literal("status").requires(source -> PermissionService.admin(source, "cobbleclub.admin.status", 2))).executes(context -> {
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("CobbleClub Server 3.8.9 is running. Claims: " + ClaimsStore.all().size() + "; matching clients: " + CLIENT_VERSIONS.size()), false);
                return 1;
            }))).then((CommandManager.literal("reload").requires(source -> PermissionService.admin(source, "cobbleclub.admin.reload", 2))).executes(context -> {
                config = ServerConfig.load();
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("CobbleClub server configuration reloaded."), true);
                return 1;
            }))).then((CommandManager.literal("claimtool").requires(source -> PermissionService.admin(source, "cobbleclub.admin.claimtool.give", 2))).then(CommandManager.literal("give").then(CommandManager.argument("player", StringArgumentType.word()).executes(context -> {
                boolean given;
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                given = target != null && ClubItems.give(target, "claiming_tool", 1);
                if (given) {
                    ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("Gave a claiming tool to " + target.getGameProfile().getName() + "."), true);
                }
                return given ? 1 : 0;
            }))))).then(((CommandManager.literal("armor").requires(source -> PermissionService.admin(source, "cobbleclub.admin.armor", 2))).then(CommandManager.literal("list").executes(context -> {
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("Armor sets: spark (Gold), spectral (Iron), aura (Diamond), fairy (Netherite), adventure (Diamond)"), false);
                return ClubItems.armorSetIds().size();
            }))).then((CommandManager.literal("give").requires(source -> PermissionService.admin(source, "cobbleclub.admin.armor.give", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("set", StringArgumentType.word()).executes(context -> {
                boolean given;
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                String set = StringArgumentType.getString(context, "set");
                given = target != null && ClubItems.giveArmorSet(target, set);
                if (given) {
                    ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("Gave " + set + " armor (" + ClubItems.armorTier(set) + " tier) to " + target.getGameProfile().getName() + "."), true);
                }
                return given ? 1 : 0;
            })))))).then(((CommandManager.literal("vote").requires(source -> PermissionService.admin(source, "cobbleclub.admin.vote", 2))).then(CommandManager.literal("status").executes(context -> {
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal(VoteRewardService.status()), false);
                return 1;
            }))).then((CommandManager.literal("test").requires(source -> PermissionService.admin(source, "cobbleclub.admin.vote.test", 2))).then(CommandManager.argument("player", StringArgumentType.word()).executes(context -> {
                boolean rewarded;
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                rewarded = target != null && VoteRewardService.testReward(target);
                if (rewarded) {
                    ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("Issued a test vote reward to " + target.getGameProfile().getName() + "."), true);
                }
                return rewarded ? 1 : 0;
            }))))).then((CommandManager.literal("claim").requires(source -> PermissionService.admin(source, "cobbleclub.admin.claim", 2))).then((CommandManager.literal("deletehere").requires(source -> PermissionService.admin(source, "cobbleclub.admin.claim.deletehere", 2))).executes(context -> {
                String result = ClaimsService.adminDeleteClaimHere(((ServerCommandSource)context.getSource()).getPlayer());
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal(result), true);
                return result.startsWith("Deleted ") ? 1 : 0;
            })))).then((CommandManager.literal("key").requires(source -> PermissionService.admin(source, "cobbleclub.admin.key", 2))).then((CommandManager.literal("give").requires(source -> PermissionService.admin(source, "cobbleclub.admin.key.give", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("crate", StringArgumentType.word()).then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 100000)).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                if (target == null) {
                    return 0;
                }
                CrateService.giveKeys(target, StringArgumentType.getString(context, "crate"), IntegerArgumentType.getInteger(context, "amount"));
                return 1;
            }))))))).then((CommandManager.literal("tag").requires(source -> PermissionService.admin(source, "cobbleclub.admin.tag", 2))).then((CommandManager.literal("grant").requires(source -> PermissionService.admin(source, "cobbleclub.admin.tag.grant", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("tag", StringArgumentType.word()).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                return target != null && TagsService.grant(target, StringArgumentType.getString(context, "tag")) ? 1 : 0;
            })))))).then((((CommandManager.literal("money").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money", 2))).then((CommandManager.literal("give").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.give", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(1L, Long.MAX_VALUE)).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                if (target == null) {
                    return 0;
                }
                long amount = LongArgumentType.getLong(context, "amount");
                EconomyService.deposit(target, amount);
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("Gave " + EconomyService.format(amount) + " to " + target.getGameProfile().getName() + ". New balance: " + EconomyService.format(EconomyService.balance(target))), true);
                return 1;
            }))))).then((CommandManager.literal("take").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.take", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(1L, Long.MAX_VALUE)).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                if (target == null) {
                    return 0;
                }
                long amount = LongArgumentType.getLong(context, "amount");
                long balance = EconomyService.takeUpTo(target, amount);
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal(target.getGameProfile().getName() + " now has " + EconomyService.format(balance) + "."), true);
                return 1;
            }))))).then((CommandManager.literal("set").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.set", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(0L, Long.MAX_VALUE)).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                if (target == null) {
                    return 0;
                }
                long amount = LongArgumentType.getLong(context, "amount");
                EconomyService.setBalance(target, amount);
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal("Set " + target.getGameProfile().getName() + " balance to " + EconomyService.format(amount) + "."), true);
                return 1;
            })))))).then((CommandManager.literal("gems").requires(source -> PermissionService.admin(source, "cobbleclub.admin.gems", 2))).then((CommandManager.literal("give").requires(source -> PermissionService.admin(source, "cobbleclub.admin.gems.give", 2))).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("amount", LongArgumentType.longArg(1L, 1000000L)).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                if (target == null) {
                    return 0;
                }
                EconomyService.depositGems(target, LongArgumentType.getLong(context, "amount"));
                target.sendMessage(Text.literal("Received " + LongArgumentType.getLong(context, "amount") + " CobbleClub gems."), false);
                return 1;
            })))))).then((CommandManager.literal("unlock").requires(source -> PermissionService.admin(source, "cobbleclub.admin.unlock", 2))).then(CommandManager.argument("type", StringArgumentType.word()).then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> {
                ServerPlayerEntity target = CobbleClubServer.online(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString(context, "player"));
                return target != null && WardrobeService.grant(target, StringArgumentType.getString(context, "type"), StringArgumentType.getString(context, "id")) ? 1 : 0;
            }))))));
        });
    }

    private static void registerProtection() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            ServerWorld serverWorld;
            if (!(player instanceof ServerPlayerEntity)) {
                return true;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            if (world instanceof ServerWorld && ChestShopService.canManageShopAt(serverPlayer, serverWorld = (ServerWorld) world, pos)) {
                return true;
            }
            boolean allowed = ClaimsService.canBuild(serverPlayer, pos);
            if (!allowed) {
                CobbleClubServer.deny(serverPlayer);
            }
            return allowed;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            ServerWorld serverWorld;
            block6: {
                block5: {
                    if (!(world instanceof ServerWorld)) break block5;
                    serverWorld = (ServerWorld) world;
                    if (player instanceof ServerPlayerEntity) break block6;
                }
                return ActionResult.PASS;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            if (ChestShopService.canManageShopAt(serverPlayer, serverWorld, pos)) {
                return ActionResult.PASS;
            }
            if (ClaimsService.canBuild(serverPlayer, pos)) {
                return ActionResult.PASS;
            }
            CobbleClubServer.deny(serverPlayer);
            return ActionResult.FAIL;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos target;
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            if (KitsService.isClaimingTool(player.getStackInHand(hand))) {
                ClaimsService.open(serverPlayer);
                return ActionResult.SUCCESS;
            }
            if (CrateService.handleCrateBlock(serverPlayer, hitResult.getBlockPos())) {
                return ActionResult.SUCCESS;
            }
            if (ChestShopService.isShopBlock((ServerWorld) world, hitResult.getBlockPos())) {
                return ActionResult.PASS;
            }
            Item held = player.getStackInHand(hand).getItem();
            BlockPos clicked = hitResult.getBlockPos();
            if (held instanceof BlockItem && !ClaimsService.canPlace(serverPlayer, clicked, target = clicked.offset(hitResult.getSide()))) {
                CobbleClubServer.deny(serverPlayer);
                return ActionResult.FAIL;
            }
            if (held instanceof BucketItem && !ClaimsService.canUseItem(serverPlayer, clicked)) {
                CobbleClubServer.deny(serverPlayer);
                return ActionResult.FAIL;
            }
            if (ClaimsService.canInteract(serverPlayer, hitResult.getBlockPos())) {
                return ActionResult.PASS;
            }
            CobbleClubServer.deny(serverPlayer);
            return ActionResult.FAIL;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world instanceof ServerWorld && player instanceof ServerPlayerEntity) {
                BlockHitResult blockHit;
                HitResult ray;
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                if (KitsService.isClaimingTool(player.getStackInHand(hand))) {
                    ClaimsService.open(serverPlayer);
                    return TypedActionResult.success(player.getStackInHand(hand), false);
                }
                if (player.getStackInHand(hand).getItem() instanceof BucketItem && (ray = player.raycast(6.0, 0.0f, false)) instanceof BlockHitResult && !ClaimsService.canUseItem(serverPlayer, (blockHit = (BlockHitResult)ray).getBlockPos())) {
                    CobbleClubServer.deny(serverPlayer);
                    return TypedActionResult.fail(player.getStackInHand(hand));
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            if (ClaimsService.canAttackEntity(serverPlayer, entity)) {
                return ActionResult.PASS;
            }
            CobbleClubServer.deny(serverPlayer);
            return ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            if (ClaimsService.canUseEntity(serverPlayer, entity)) {
                return ActionResult.PASS;
            }
            CobbleClubServer.deny(serverPlayer);
            return ActionResult.FAIL;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity.getWorld() instanceof ServerWorld)) {
                return true;
            }
            Entity patt0$temp = source.getAttacker();
            if (patt0$temp instanceof ServerPlayerEntity) {
                ServerPlayerEntity attacker = (ServerPlayerEntity) patt0$temp;
                return ClaimsService.canAttackEntity(attacker, entity);
            }
            return true;
        });
    }

    private static int openClaims(ServerPlayerEntity player) {
        ClaimsService.open(player);
        return 1;
    }

    private static int openTags(ServerPlayerEntity player) {
        TagsService.open(player);
        return 1;
    }

    private static int openWardrobe(ServerPlayerEntity player) {
        WardrobeService.open(player);
        return 1;
    }

    private static int buyWardrobe(ServerPlayerEntity player, String type, String id) {
        boolean bought = WardrobeService.buy(player, type, id);
        if (!bought) {
            player.sendMessage(Text.literal("That unlock is unavailable, already owned, or you cannot afford it."), false);
        }
        return bought ? 1 : 0;
    }

    private static int showKeys(ServerPlayerEntity player) {
        player.sendMessage(Text.literal(CrateService.keySummary(player)), false);
        return 1;
    }

    private static ServerPlayerEntity online(ServerPlayerEntity requester, String name) {
        if (requester.getServer() == null || name == null) {
            return null;
        }
        for (ServerPlayerEntity player : requester.getServer().getPlayerManager().getPlayerList()) {
            if (!name.equalsIgnoreCase(player.getGameProfile().getName())) continue;
            return player;
        }
        return null;
    }

    private static int showBalance(ServerPlayerEntity requester, String targetName) {
        ServerPlayerEntity target = targetName == null ? requester : CobbleClubServer.online(requester, targetName);
        if (target == null) {
            requester.sendMessage(Text.literal("That player is not online."), false);
            return 0;
        }
        String label = target == requester ? "Your balance" : target.getGameProfile().getName() + "'s balance";
        requester.sendMessage(Text.literal("\u2726 " + label + ": ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD).append(Text.literal(EconomyService.format(EconomyService.balance(target))).formatted(Formatting.GOLD)), false);
        return 1;
    }

    private static int payCommand(ServerPlayerEntity sender, String targetName, long amount) {
        boolean paid;
        ServerPlayerEntity recipient = CobbleClubServer.online(sender, targetName);
        paid = recipient != null && EconomyService.pay(sender, recipient, amount);
        if (!paid) {
            sender.sendMessage(Text.literal("Payment failed. Check the player name and your balance.").formatted(Formatting.RED), false);
        }
        return paid ? 1 : 0;
    }

    private static int sendBalanceLeaderboard(ServerPlayerEntity player) {
        List<EconomyService.BalanceEntry> top = EconomyService.topBalances(10);
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 \u2726 COBBLECLUB RICHEST \u2726 \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        if (top.isEmpty()) {
            player.sendMessage(Text.literal("No balances have been recorded yet.").formatted(Formatting.GRAY), false);
            return 1;
        }
        for (int i = 0; i < top.size(); ++i) {
            EconomyService.BalanceEntry entry = top.get(i);
            Formatting rankColor = i == 0 ? Formatting.GOLD
                    : i == 1 ? Formatting.WHITE
                    : i == 2 ? Formatting.YELLOW
                    : Formatting.GRAY;
            String crown = i == 0 ? "\u265b " : (i == 1 ? "\u25c6 " : (i == 2 ? "\u25c7 " : ""));
            MutableText line = Text.literal(String.format("#%d %s%s", i + 1, crown, entry.name())).formatted(rankColor);
            if (i < 3) {
                line.formatted(Formatting.BOLD);
            }
            line.append(Text.literal("  \u2022  " + EconomyService.format(entry.balance())).formatted(Formatting.AQUA));
            player.sendMessage(line, false);
        }
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int adminGiveMoney(ServerPlayerEntity admin, String targetName, long amount) {
        ServerPlayerEntity target = CobbleClubServer.online(admin, targetName);
        if (target == null) {
            return 0;
        }
        EconomyService.deposit(target, amount);
        admin.sendMessage(Text.literal("Gave " + EconomyService.format(amount) + " to " + target.getGameProfile().getName() + "."), false);
        return 1;
    }

    private static int adminTakeMoney(ServerPlayerEntity admin, String targetName, long amount) {
        ServerPlayerEntity target = CobbleClubServer.online(admin, targetName);
        if (target == null) {
            return 0;
        }
        long now = EconomyService.takeUpTo(target, amount);
        admin.sendMessage(Text.literal(target.getGameProfile().getName() + " now has " + EconomyService.format(now) + "."), false);
        return 1;
    }

    private static int adminSetMoney(ServerPlayerEntity admin, String targetName, long amount) {
        ServerPlayerEntity target = CobbleClubServer.online(admin, targetName);
        if (target == null) {
            return 0;
        }
        EconomyService.setBalance(target, amount);
        admin.sendMessage(Text.literal("Set " + target.getGameProfile().getName() + " to " + EconomyService.format(amount) + "."), false);
        return 1;
    }

    private static int sendVoteLinks(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 \u2726 VOTE FOR COBBLECLUB \u2726 \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        if (config.voteRewardsEnabled) {
            String reward = Math.max(0, config.voteKeysPerVote) + " Vote Key" + (config.voteKeysPerVote == 1 ? "" : "s") + " + " + EconomyService.format(Math.max(0L, config.voteMoneyReward));
            player.sendMessage(Text.literal("Vote on each listed site and receive: ").formatted(Formatting.GRAY).append(Text.literal(reward).formatted(Formatting.GOLD, Formatting.BOLD)), false);
        } else {
            player.sendMessage(Text.literal("Voting links are available, but automatic vote rewards are currently disabled.").formatted(Formatting.YELLOW), false);
        }
        if (config.voteSites == null || config.voteSites.isEmpty()) {
            player.sendMessage(Text.literal("No vote sites are configured yet.").formatted(Formatting.RED), false);
        } else {
            for (ServerConfig.VoteSite site : config.voteSites) {
                if (site == null || site.url == null || site.url.isBlank()) continue;
                String name = site.name == null || site.name.isBlank() ? "Vote Site" : site.name;
                String url = site.url.trim();
                MutableText line = Text.literal("  \u2605 " + name + "  ").formatted(Formatting.AQUA, Formatting.BOLD);
                line.append(Text.literal("[VOTE]").styled(style -> style.withColor(Formatting.GREEN).withBold(true).withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
                line.append(Text.literal("  [COPY]").styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, url))));
                player.sendMessage(line, false);
            }
        }
        player.sendMessage(Text.literal("Your in-game username must match the name submitted to the vote site.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int sendWiki(ServerPlayerEntity player) {
        String url = "https://wiki.cobble-club.com";
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 \u2726 COBBLECLUB WIKI \u2726 \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Everything you need to learn CobbleClub is documented here:").formatted(Formatting.GRAY), false);
        MutableText link = Text.literal("  \u00bb https://wiki.cobble-club.com").styled(style -> style.withColor(Formatting.AQUA).withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://wiki.cobble-club.com")));
        link.append(Text.literal("   [COPY LINK]").styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "https://wiki.cobble-club.com"))));
        player.sendMessage(link, false);
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int sendStore(ServerPlayerEntity player) {
        String url = "https://store.cobble-club.com";
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 \u2726 COBBLECLUB STORE \u2726 \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Support the server and explore ranks, keys, cosmetics, and more.").formatted(Formatting.GRAY), false);
        MutableText link = Text.literal("  \u00bb OPEN THE COBBLECLUB STORE \u00ab  ").styled(style -> style.withColor(Formatting.AQUA).withBold(true).withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://store.cobble-club.com")));
        link.append(Text.literal("  [COPY LINK]").styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "https://store.cobble-club.com"))));
        player.sendMessage(link, false);
        player.sendMessage(Text.literal("Click OPEN above, then confirm Yes in Minecraft's link prompt.").formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int sendFeatures(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 \u2726 COBBLECLUB FEATURES \u2726 \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.literal("CobbleClub has custom claims, kits, cosmetics, crates, progression, economies, Pok\u00e9mon systems and more.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Use our Wiki to see every feature, learn how each system works, and get started without guessing.").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("Open the Wiki below or copy the link to share it:").formatted(Formatting.GRAY), false);
        CobbleClubServer.sendWikiLinkOnly(player);
        player.sendMessage(Text.literal("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static void sendWikiLinkOnly(ServerPlayerEntity player) {
        String url = "https://wiki.cobble-club.com";
        MutableText link = Text.literal("  \u00bb OPEN COBBLECLUB WIKI").styled(style -> style.withColor(Formatting.AQUA).withBold(true).withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://wiki.cobble-club.com")));
        link.append(Text.literal("   [COPY LINK]").styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "https://wiki.cobble-club.com"))));
        player.sendMessage(link, false);
    }

    private static void deny(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("This area is protected by CobbleClub Claims."), true);
    }
}
