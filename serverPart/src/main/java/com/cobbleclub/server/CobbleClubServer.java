package com.cobbleclub.server;

import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.ServerNetworking;
import com.cobbleclub.server.service.CatalogService;
import com.cobbleclub.server.service.ClaimPresenceService;
import com.cobbleclub.server.service.ClaimsService;
import com.cobbleclub.server.service.ClubItems;
import com.cobbleclub.server.service.CrateService;
import com.cobbleclub.server.service.DashboardService;
import com.cobbleclub.server.service.CosmeticVisualService;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.KitsService;
import com.cobbleclub.server.service.PermissionService;
import com.cobbleclub.server.service.RtpService;
import com.cobbleclub.server.service.TagsService;
import com.cobbleclub.server.service.WardrobeService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CobbleClubServer implements ModInitializer {
    public static final String MOD_ID = "cobbleclub";
    public static final Logger LOGGER = LoggerFactory.getLogger("CobbleClub Server");
    private static final Map<UUID, String> CLIENT_VERSIONS = new ConcurrentHashMap<>();
    private static ServerConfig config;

    @Override
    public void onInitialize() {
        config = ServerConfig.load();
        ServerNetworking.register();
        registerCommands();
        registerProtection();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataStore.load(server);
            ClaimsStore.load(server);
            EconomyService.initialize(server);
            LOGGER.info("CobbleClub Server 2.6 started with {} persistent claims", ClaimsStore.all().size());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerDataStore.save();
            ClaimsStore.save();
            CLIENT_VERSIONS.clear();
            ClaimPresenceService.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            EconomyService.tick(server);
            ClaimPresenceService.tick(server);
            ClaimsService.syncWorldSnapshots(server);
            RtpService.tick(server);
            TagsService.tick(server);
            CosmeticVisualService.tick(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            safeJoinStep(handler.player, "economy", () -> EconomyService.data(handler.player));
            safeJoinStep(handler.player, "tag rank", () -> TagsService.syncRank(handler.player, false));
            safeJoinStep(handler.player, "tags", () -> TagsService.apply(handler.player));
            safeJoinStep(handler.player, "cosmetics", () -> CosmeticVisualService.apply(handler.player));
            safeJoinStep(handler.player, "cosmetic visibility", () -> CosmeticVisualService.onJoin(handler.player));
            safeJoinStep(handler.player, "Newb Kit", () -> KitsService.onJoin(handler.player));
            safeJoinStep(handler.player, "claim borders", () -> ClaimsService.syncWorldSnapshots(server));
            PlayerDataStore.save();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CosmeticVisualService.remove(handler.player);
            ClaimsService.forgetWorldSnapshot(handler.player);
            RtpService.forget(handler.player);
            CLIENT_VERSIONS.remove(handler.player.getUuid());
            PlayerDataStore.save();
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) EconomyService.handleDeath(player);
        });
    }

    public static ServerConfig config() {
        if (config == null) config = ServerConfig.load();
        return config;
    }

    public static void acceptHandshake(ServerPlayerEntity player, String version) {
        CLIENT_VERSIONS.put(player.getUuid(), version == null ? "unknown" : version);
        LOGGER.info("CobbleClub client {} connected with version {}", player.getGameProfile().getName(), version);
    }

    public static void requiresClient(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("The matching CobbleClub client mod 2.6 is required to open this menu."), false);
    }

    private static void safeJoinStep(ServerPlayerEntity player, String step, Runnable action) {
        try {
            action.run();
        } catch (Exception error) {
            LOGGER.error("Could not initialize {} for {}; login will continue", step, player.getGameProfile().getName(), error);
        }
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("club").requires(source -> PermissionService.has(source, "cobbleclub.command.club", true)).executes(context -> {
                DashboardService.open(context.getSource().getPlayer());
                return 1;
            }));
            dispatcher.register(CommandManager.literal("cobbleclub").requires(source -> PermissionService.has(source, "cobbleclub.command.club", true)).executes(context -> {
                DashboardService.open(context.getSource().getPlayer());
                return 1;
            }));
            dispatcher.register(CommandManager.literal("kits").requires(source -> PermissionService.has(source, "cobbleclub.command.kits", true)).executes(context -> {
                KitsService.open(context.getSource().getPlayer());
                return 1;
            }));
            dispatcher.register(CommandManager.literal("kit").requires(source -> PermissionService.has(source, "cobbleclub.command.kits", true)).executes(context -> {
                KitsService.open(context.getSource().getPlayer());
                return 1;
            }));
            dispatcher.register(CommandManager.literal("claim")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.claim", true))
                    .executes(context -> openClaims(context.getSource().getPlayer()))
                    .then(CommandManager.literal("menu").executes(context -> openClaims(context.getSource().getPlayer())))
                    .then(CommandManager.literal("tool").requires(source -> PermissionService.has(source, "cobbleclub.command.claim.tool", true)).executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (KitsService.hasClaimingTool(player)) {
                            player.sendMessage(Text.literal("You already have a CobbleClub Claiming Tool."), false);
                            return 0;
                        }
                        boolean given = KitsService.recoverClaimingTool(player);
                        player.sendMessage(Text.literal(given
                                ? "A replacement CobbleClub Claiming Tool was added to your inventory."
                                : "The claiming tool could not be created; please contact an administrator."), false);
                        return given ? 1 : 0;
                    }))
                    .then(CommandManager.literal("blocks").requires(source -> PermissionService.has(source, "cobbleclub.command.claim.blocks", true)).executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        var budget = ClaimsService.budget(player);
                        player.sendMessage(Text.literal("Claim blocks: " + budget.getRemaining() + " available / " + budget.getTotal() + " total."), false);
                        return 1;
                    }))
                    .then(CommandManager.literal("subclaim")
                            .then(CommandManager.literal("create")
                                    .requires(source -> PermissionService.has(source, "cobbleclub.command.claim.subclaim.create", true))
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .then(CommandManager.argument("x1", IntegerArgumentType.integer())
                                                    .then(CommandManager.argument("y1", IntegerArgumentType.integer())
                                                            .then(CommandManager.argument("z1", IntegerArgumentType.integer())
                                                                    .then(CommandManager.argument("x2", IntegerArgumentType.integer())
                                                                            .then(CommandManager.argument("y2", IntegerArgumentType.integer())
                                                                                    .then(CommandManager.argument("z2", IntegerArgumentType.integer())
                                                                                            .executes(context -> {
                                                                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                                                                boolean made = ClaimsService.createSubclaim(player,
                                                                                                        StringArgumentType.getString(context, "name"),
                                                                                                        new BlockPos(IntegerArgumentType.getInteger(context, "x1"), IntegerArgumentType.getInteger(context, "y1"), IntegerArgumentType.getInteger(context, "z1")),
                                                                                                        new BlockPos(IntegerArgumentType.getInteger(context, "x2"), IntegerArgumentType.getInteger(context, "y2"), IntegerArgumentType.getInteger(context, "z2")));
                                                                                                player.sendMessage(Text.literal(made ? "Subclaim created. Open /claim to edit it." : "Could not create that subclaim. Both corners must be inside one of your claims and may not overlap another subclaim."), false);
                                                                                                return made ? 1 : 0;
                                                                                            })))))))))));
            dispatcher.register(CommandManager.literal("claims").requires(source -> PermissionService.has(source, "cobbleclub.command.claim", true)).executes(context -> openClaims(context.getSource().getPlayer())));

            dispatcher.register(CommandManager.literal("tag")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.tags", true))
                    .executes(context -> openTags(context.getSource().getPlayer()))
                    .then(CommandManager.literal("buy")
                            .requires(source -> PermissionService.has(source, "cobbleclub.command.tags.buy", true))
                            .then(CommandManager.argument("tag", StringArgumentType.word()).executes(context -> {
                                boolean bought = TagsService.buy(context.getSource().getPlayer(), StringArgumentType.getString(context, "tag"));
                                if (!bought) context.getSource().sendError(Text.literal("That tag is unavailable, already owned, or you cannot afford it."));
                                return bought ? 1 : 0;
                            }))));
            dispatcher.register(CommandManager.literal("tags").requires(source -> PermissionService.has(source, "cobbleclub.command.tags", true)).executes(context -> openTags(context.getSource().getPlayer())));

            dispatcher.register(CommandManager.literal("wardrobe")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.wardrobe", true))
                    .executes(context -> openWardrobe(context.getSource().getPlayer()))
                    .then(CommandManager.literal("buy")
                            .requires(source -> PermissionService.has(source, "cobbleclub.command.wardrobe.buy", true))
                            .then(CommandManager.literal("cosmetic")
                                    .then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> buyWardrobe(context.getSource().getPlayer(), "cosmetic", StringArgumentType.getString(context, "id")))))
                            .then(CommandManager.literal("glow")
                                    .then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> buyWardrobe(context.getSource().getPlayer(), "glow", StringArgumentType.getString(context, "id")))))));

            dispatcher.register(CommandManager.literal("balance")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))
                    .executes(context -> showBalance(context.getSource().getPlayer(), null))
                    .then(CommandManager.literal("leaderboard").executes(context -> sendBalanceLeaderboard(context.getSource().getPlayer())))
                    .then(CommandManager.literal("top").executes(context -> sendBalanceLeaderboard(context.getSource().getPlayer())))
                    .then(CommandManager.argument("player", StringArgumentType.word()).executes(context ->
                            showBalance(context.getSource().getPlayer(), StringArgumentType.getString(context, "player")))));
            dispatcher.register(CommandManager.literal("bal")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))
                    .executes(context -> showBalance(context.getSource().getPlayer(), null))
                    .then(CommandManager.literal("leaderboard").executes(context -> sendBalanceLeaderboard(context.getSource().getPlayer())))
                    .then(CommandManager.literal("top").executes(context -> sendBalanceLeaderboard(context.getSource().getPlayer())))
                    .then(CommandManager.argument("player", StringArgumentType.word()).executes(context ->
                            showBalance(context.getSource().getPlayer(), StringArgumentType.getString(context, "player")))));
            dispatcher.register(CommandManager.literal("baltop")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))
                    .executes(context -> sendBalanceLeaderboard(context.getSource().getPlayer())));
            dispatcher.register(CommandManager.literal("gems").requires(source -> PermissionService.has(source, "cobbleclub.command.gems", true)).executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                player.sendMessage(Text.literal("CobbleClub gems: " + EconomyService.formatGems(EconomyService.gems(player))), false);
                return 1;
            }));
            dispatcher.register(CommandManager.literal("pay")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.pay", true))
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("amount", LongArgumentType.longArg(1)).executes(context ->
                                    payCommand(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))));

            dispatcher.register(CommandManager.literal("economy")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.balance", true))
                    .executes(context -> showBalance(context.getSource().getPlayer(), null))
                    .then(CommandManager.literal("balance").executes(context -> showBalance(context.getSource().getPlayer(), null)))
                    .then(CommandManager.literal("top").executes(context -> sendBalanceLeaderboard(context.getSource().getPlayer())))
                    .then(CommandManager.literal("pay")
                            .requires(source -> PermissionService.has(source, "cobbleclub.command.pay", true))
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1)).executes(context ->
                                            payCommand(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))))
                    .then(CommandManager.literal("give")
                            .requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.give", 2))
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1)).executes(context ->
                                            adminGiveMoney(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))))
                    .then(CommandManager.literal("take")
                            .requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.take", 2))
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1)).executes(context ->
                                            adminTakeMoney(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount"))))))
                    .then(CommandManager.literal("set")
                            .requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.set", 2))
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", LongArgumentType.longArg(0)).executes(context ->
                                            adminSetMoney(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"), LongArgumentType.getLong(context, "amount")))))));
            dispatcher.register(CommandManager.literal("daily").requires(source -> PermissionService.has(source, "cobbleclub.command.daily", true)).executes(context -> {
                boolean claimed = EconomyService.claimDaily(context.getSource().getPlayer());
                if (!claimed) context.getSource().sendError(Text.literal("Your next CobbleClub daily reward is not ready yet."));
                return claimed ? 1 : 0;
            }));

            dispatcher.register(CommandManager.literal("rtp")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.rtp", true))
                    .executes(context -> RtpService.start(context.getSource().getPlayer(), null))
                    .then(CommandManager.literal("red").executes(context -> RtpService.start(context.getSource().getPlayer(), "red")))
                    .then(CommandManager.literal("yellow").executes(context -> RtpService.start(context.getSource().getPlayer(), "yellow")))
                    .then(CommandManager.literal("blue").executes(context -> RtpService.start(context.getSource().getPlayer(), "blue")))
                    .then(CommandManager.literal("green").executes(context -> RtpService.start(context.getSource().getPlayer(), "green")))
                    .then(CommandManager.literal("resource").executes(context -> RtpService.start(context.getSource().getPlayer(), "resource")))
                    .then(CommandManager.literal("resources").executes(context -> RtpService.start(context.getSource().getPlayer(), "resource"))));

            dispatcher.register(CommandManager.literal("wiki")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.wiki", true))
                    .executes(context -> sendWiki(context.getSource().getPlayer())));
            dispatcher.register(CommandManager.literal("features")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.features", true))
                    .executes(context -> sendFeatures(context.getSource().getPlayer())));

            dispatcher.register(CommandManager.literal("crate")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.crates", true))
                    .executes(context -> showKeys(context.getSource().getPlayer()))
                    .then(CommandManager.literal("preview")
                            .then(CommandManager.argument("crate", StringArgumentType.word()).executes(context -> {
                                CatalogService.openCrate(context.getSource().getPlayer(), StringArgumentType.getString(context, "crate"));
                                return 1;
                            })))
                    .then(CommandManager.literal("open")
                            .then(CommandManager.argument("crate", StringArgumentType.word()).executes(context ->
                                    CrateService.open(context.getSource().getPlayer(), StringArgumentType.getString(context, "crate")) ? 1 : 0))));
            dispatcher.register(CommandManager.literal("crates").requires(source -> PermissionService.has(source, "cobbleclub.command.crates", true)).executes(context -> showKeys(context.getSource().getPlayer())));
            dispatcher.register(CommandManager.literal("keys").requires(source -> PermissionService.has(source, "cobbleclub.command.crates", true)).executes(context -> showKeys(context.getSource().getPlayer())));

            dispatcher.register(CommandManager.literal("pokemonpreview").requires(source -> PermissionService.has(source, "cobbleclub.command.preview.pokemon", true)).executes(context -> {
                CatalogService.openPokemonSkins(context.getSource().getPlayer());
                return 1;
            }));
            dispatcher.register(CommandManager.literal("gearpreview").requires(source -> PermissionService.has(source, "cobbleclub.command.preview.gear", true)).executes(context -> {
                CatalogService.openGear(context.getSource().getPlayer());
                return 1;
            }));
            dispatcher.register(CommandManager.literal("cratepreview")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.preview.crate", true))
                    .then(CommandManager.argument("crate", StringArgumentType.word()).executes(context -> {
                        CatalogService.openCrate(context.getSource().getPlayer(), StringArgumentType.getString(context, "crate"));
                        return 1;
                    })));

            dispatcher.register(CommandManager.literal("clubitem")
                    .then(CommandManager.literal("list").requires(source -> PermissionService.admin(source, "cobbleclub.admin.clubitem.list", 2)).executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("CobbleClub items: " + String.join(", ", ClubItems.ids())), false);
                        return ClubItems.ids().size();
                    }))
                    .then(CommandManager.literal("give").requires(source -> PermissionService.admin(source, "cobbleclub.admin.clubitem.give", 2))
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                    .then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> {
                                        ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                        return target != null && ClubItems.give(target, StringArgumentType.getString(context, "id"), 1) ? 1 : 0;
                                    })))));

            dispatcher.register(CommandManager.literal("cobbleclubserver")
                    .then(CommandManager.literal("status").requires(source -> PermissionService.admin(source, "cobbleclub.admin.status", 2)).executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("CobbleClub Server 2.6 is running. Claims: " + ClaimsStore.all().size()
                                + "; matching clients: " + CLIENT_VERSIONS.size()), false);
                        return 1;
                    }))
                    .then(CommandManager.literal("reload")
                            .requires(source -> PermissionService.admin(source, "cobbleclub.admin.reload", 2))
                            .executes(context -> {
                                config = ServerConfig.load();
                                context.getSource().sendFeedback(() -> Text.literal("CobbleClub server configuration reloaded."), true);
                                return 1;
                            }))
                    .then(CommandManager.literal("claimtool")
                            .requires(source -> PermissionService.admin(source, "cobbleclub.admin.claimtool.give", 2))
                            .then(CommandManager.literal("give")
                                    .then(CommandManager.argument("player", StringArgumentType.word()).executes(context -> {
                                        ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                        boolean given = target != null && ClubItems.give(target, "claiming_tool", 1);
                                        if (given) context.getSource().sendFeedback(() -> Text.literal("Gave a claiming tool to " + target.getGameProfile().getName() + "."), true);
                                        return given ? 1 : 0;
                                    }))))
                    .then(CommandManager.literal("claim")
                            .requires(source -> PermissionService.admin(source, "cobbleclub.admin.claim", 2))
                            .then(CommandManager.literal("deletehere").requires(source -> PermissionService.admin(source, "cobbleclub.admin.claim.deletehere", 2)).executes(context -> {
                                String result = ClaimsService.adminDeleteClaimHere(context.getSource().getPlayer());
                                context.getSource().sendFeedback(() -> Text.literal(result), true);
                                return result.startsWith("Deleted ") ? 1 : 0;
                            })))
                    .then(CommandManager.literal("key").requires(source -> PermissionService.admin(source, "cobbleclub.admin.key", 2))
                            .then(CommandManager.literal("give")
                                    .requires(source -> PermissionService.admin(source, "cobbleclub.admin.key.give", 2))
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .then(CommandManager.argument("crate", StringArgumentType.word())
                                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 100000)).executes(context -> {
                                                        ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                                        if (target == null) return 0;
                                                        CrateService.giveKeys(target, StringArgumentType.getString(context, "crate"), IntegerArgumentType.getInteger(context, "amount"));
                                                        return 1;
                                                    }))))))
                    .then(CommandManager.literal("tag").requires(source -> PermissionService.admin(source, "cobbleclub.admin.tag", 2))
                            .then(CommandManager.literal("grant")
                                    .requires(source -> PermissionService.admin(source, "cobbleclub.admin.tag.grant", 2))
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .then(CommandManager.argument("tag", StringArgumentType.word()).executes(context -> {
                                                ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                                return target != null && TagsService.grant(target, StringArgumentType.getString(context, "tag")) ? 1 : 0;
                                                    })))))
                    .then(CommandManager.literal("money").requires(source -> PermissionService.admin(source, "cobbleclub.admin.money", 2))
                            .then(CommandManager.literal("give")
                                    .requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.give", 2))
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .then(CommandManager.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE)).executes(context -> {
                                                ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                                if (target == null) return 0;
                                                long amount = LongArgumentType.getLong(context, "amount");
                                                EconomyService.deposit(target, amount);
                                                context.getSource().sendFeedback(() -> Text.literal("Gave " + EconomyService.format(amount) + " to " + target.getGameProfile().getName() + ". New balance: " + EconomyService.format(EconomyService.balance(target))), true);
                                                return 1;
                                            }))))
                            .then(CommandManager.literal("take")
                                    .requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.take", 2))
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .then(CommandManager.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE)).executes(context -> {
                                                ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                                if (target == null) return 0;
                                                long amount = LongArgumentType.getLong(context, "amount");
                                                long balance = EconomyService.takeUpTo(target, amount);
                                                context.getSource().sendFeedback(() -> Text.literal(target.getGameProfile().getName() + " now has " + EconomyService.format(balance) + "."), true);
                                                return 1;
                                            }))))
                            .then(CommandManager.literal("set")
                                    .requires(source -> PermissionService.admin(source, "cobbleclub.admin.money.set", 2))
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .then(CommandManager.argument("amount", LongArgumentType.longArg(0, Long.MAX_VALUE)).executes(context -> {
                                                ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                                if (target == null) return 0;
                                                long amount = LongArgumentType.getLong(context, "amount");
                                                EconomyService.setBalance(target, amount);
                                                context.getSource().sendFeedback(() -> Text.literal("Set " + target.getGameProfile().getName() + " balance to " + EconomyService.format(amount) + "."), true);
                                                return 1;
                                            })))))
                    .then(CommandManager.literal("gems").requires(source -> PermissionService.admin(source, "cobbleclub.admin.gems", 2))
                            .then(CommandManager.literal("give")
                                    .requires(source -> PermissionService.admin(source, "cobbleclub.admin.gems.give", 2))
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .then(CommandManager.argument("amount", LongArgumentType.longArg(1, 1000000)).executes(context -> {
                                                ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                                if (target == null) return 0;
                                                EconomyService.depositGems(target, LongArgumentType.getLong(context, "amount"));
                                                target.sendMessage(Text.literal("Received " + LongArgumentType.getLong(context, "amount") + " CobbleClub gems."), false);
                                                return 1;
                                            })))))
                    .then(CommandManager.literal("unlock").requires(source -> PermissionService.admin(source, "cobbleclub.admin.unlock", 2))
                            .then(CommandManager.argument("type", StringArgumentType.word())
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .then(CommandManager.argument("id", StringArgumentType.word()).executes(context -> {
                                                ServerPlayerEntity target = online(context.getSource().getPlayer(), StringArgumentType.getString(context, "player"));
                                                return target != null && WardrobeService.grant(target, StringArgumentType.getString(context, "type"), StringArgumentType.getString(context, "id")) ? 1 : 0;
                                            }))))));
        });
    }

    private static void registerProtection() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
            boolean allowed = ClaimsService.canBuild(serverPlayer, pos);
            if (!allowed) deny(serverPlayer);
            return allowed;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (ClaimsService.canBuild(serverPlayer, pos)) return ActionResult.PASS;
            deny(serverPlayer);
            return ActionResult.FAIL;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (KitsService.isClaimingTool(player.getStackInHand(hand))) {
                ClaimsService.open(serverPlayer);
                return ActionResult.SUCCESS;
            }
            if (CrateService.handleCrateBlock(serverPlayer, hitResult.getBlockPos())) return ActionResult.SUCCESS;
            var held = player.getStackInHand(hand).getItem();
            BlockPos clicked = hitResult.getBlockPos();
            if (held instanceof BlockItem) {
                BlockPos target = clicked.offset(hitResult.getSide());
                if (!ClaimsService.canPlace(serverPlayer, clicked, target)) { deny(serverPlayer); return ActionResult.FAIL; }
            }
            if (held instanceof BucketItem && !ClaimsService.canUseItem(serverPlayer, clicked)) {
                deny(serverPlayer);
                return ActionResult.FAIL;
            }
            if (ClaimsService.canInteract(serverPlayer, hitResult.getBlockPos())) return ActionResult.PASS;
            deny(serverPlayer);
            return ActionResult.FAIL;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world instanceof ServerWorld && player instanceof ServerPlayerEntity serverPlayer) {
                if (KitsService.isClaimingTool(player.getStackInHand(hand))) {
                    ClaimsService.open(serverPlayer);
                    return TypedActionResult.success(player.getStackInHand(hand), false);
                }
                if (player.getStackInHand(hand).getItem() instanceof BucketItem) {
                    HitResult ray = player.raycast(6.0D, 0.0F, false);
                    if (ray instanceof BlockHitResult blockHit && !ClaimsService.canUseItem(serverPlayer, blockHit.getBlockPos())) {
                        deny(serverPlayer);
                        return TypedActionResult.fail(player.getStackInHand(hand));
                    }
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (ClaimsService.canAttackEntity(serverPlayer, entity)) return ActionResult.PASS;
            deny(serverPlayer);
            return ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (ClaimsService.canUseEntity(serverPlayer, entity)) return ActionResult.PASS;
            deny(serverPlayer);
            return ActionResult.FAIL;
        });
        // Covers arrows/tridents and other indirect player damage that does not fire AttackEntityCallback.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity.getWorld() instanceof ServerWorld)) return true;
            if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
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
        if (!bought) player.sendMessage(Text.literal("That unlock is unavailable, already owned, or you cannot afford it."), false);
        return bought ? 1 : 0;
    }

    private static int showKeys(ServerPlayerEntity player) {
        player.sendMessage(Text.literal(CrateService.keySummary(player)), false);
        return 1;
    }

    private static ServerPlayerEntity online(ServerPlayerEntity requester, String name) {
        if (requester.getServer() == null || name == null) return null;
        for (ServerPlayerEntity player : requester.getServer().getPlayerManager().getPlayerList()) {
            if (name.equalsIgnoreCase(player.getGameProfile().getName())) return player;
        }
        return null;
    }

    private static int showBalance(ServerPlayerEntity requester, String targetName) {
        ServerPlayerEntity target = targetName == null ? requester : online(requester, targetName);
        if (target == null) {
            requester.sendMessage(Text.literal("That player is not online."), false);
            return 0;
        }
        String label = target == requester ? "Your balance" : target.getGameProfile().getName() + "'s balance";
        requester.sendMessage(Text.literal("✦ " + label + ": ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal(EconomyService.format(EconomyService.balance(target))).formatted(Formatting.GOLD)), false);
        return 1;
    }

    private static int payCommand(ServerPlayerEntity sender, String targetName, long amount) {
        ServerPlayerEntity recipient = online(sender, targetName);
        boolean paid = recipient != null && EconomyService.pay(sender, recipient, amount);
        if (!paid) sender.sendMessage(Text.literal("Payment failed. Check the player name and your balance.").formatted(Formatting.RED), false);
        return paid ? 1 : 0;
    }

    private static int sendBalanceLeaderboard(ServerPlayerEntity player) {
        var top = EconomyService.topBalances(10);
        player.sendMessage(Text.literal("━━━━━━━━━━ ✦ COBBLECLUB RICHEST ✦ ━━━━━━━━━━").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        if (top.isEmpty()) {
            player.sendMessage(Text.literal("No balances have been recorded yet.").formatted(Formatting.GRAY), false);
            return 1;
        }
        for (int i = 0; i < top.size(); i++) {
            var entry = top.get(i);
            Formatting rankColor = i == 0 ? Formatting.GOLD : i == 1 ? Formatting.WHITE : i == 2 ? Formatting.YELLOW : Formatting.GRAY;
            String crown = i == 0 ? "♛ " : i == 1 ? "◆ " : i == 2 ? "◇ " : "";
            MutableText line = Text.literal(String.format("#%d %s%s", i + 1, crown, entry.name())).formatted(rankColor);
            if (i < 3) line.formatted(Formatting.BOLD);
            line.append(Text.literal("  •  " + EconomyService.format(entry.balance())).formatted(Formatting.AQUA));
            player.sendMessage(line, false);
        }
        player.sendMessage(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int adminGiveMoney(ServerPlayerEntity admin, String targetName, long amount) {
        ServerPlayerEntity target = online(admin, targetName);
        if (target == null) return 0;
        EconomyService.deposit(target, amount);
        admin.sendMessage(Text.literal("Gave " + EconomyService.format(amount) + " to " + target.getGameProfile().getName() + "."), false);
        return 1;
    }

    private static int adminTakeMoney(ServerPlayerEntity admin, String targetName, long amount) {
        ServerPlayerEntity target = online(admin, targetName);
        if (target == null) return 0;
        long now = EconomyService.takeUpTo(target, amount);
        admin.sendMessage(Text.literal(target.getGameProfile().getName() + " now has " + EconomyService.format(now) + "."), false);
        return 1;
    }

    private static int adminSetMoney(ServerPlayerEntity admin, String targetName, long amount) {
        ServerPlayerEntity target = online(admin, targetName);
        if (target == null) return 0;
        EconomyService.setBalance(target, amount);
        admin.sendMessage(Text.literal("Set " + target.getGameProfile().getName() + " to " + EconomyService.format(amount) + "."), false);
        return 1;
    }

    private static int sendWiki(ServerPlayerEntity player) {
        final String url = "https://wiki.cobble-club.com";
        player.sendMessage(Text.literal("━━━━━━━━━━ ✦ COBBLECLUB WIKI ✦ ━━━━━━━━━━").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Everything you need to learn CobbleClub is documented here:").formatted(Formatting.GRAY), false);
        MutableText link = Text.literal("  » " + url).styled(style -> style.withColor(Formatting.AQUA).withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        link.append(Text.literal("   [COPY LINK]").styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, url))));
        player.sendMessage(link, false);
        player.sendMessage(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int sendFeatures(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("━━━━━━━━ ✦ COBBLECLUB FEATURES ✦ ━━━━━━━━").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.literal("CobbleClub has custom claims, kits, cosmetics, crates, progression, economies, Pokémon systems and more.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("Use our Wiki to see every feature, learn how each system works, and get started without guessing.").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("Open the Wiki below or copy the link to share it:").formatted(Formatting.GRAY), false);
        sendWikiLinkOnly(player);
        player.sendMessage(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static void sendWikiLinkOnly(ServerPlayerEntity player) {
        final String url = "https://wiki.cobble-club.com";
        MutableText link = Text.literal("  » OPEN COBBLECLUB WIKI").styled(style -> style.withColor(Formatting.AQUA).withBold(true).withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        link.append(Text.literal("   [COPY LINK]").styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, url))));
        player.sendMessage(link, false);
    }

    private static void deny(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("This area is protected by CobbleClub Claims."), true);
    }
}
