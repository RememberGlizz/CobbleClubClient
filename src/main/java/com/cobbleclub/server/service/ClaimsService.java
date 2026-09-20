/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.claims.protocol.ActionFeedback
 *  com.cobbleclub.clubhouse.claims.protocol.BoxInfo
 *  com.cobbleclub.clubhouse.claims.protocol.BudgetInfo
 *  com.cobbleclub.clubhouse.claims.protocol.ChunkRect
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimMessagesEdit
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsActionMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsActionType
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsMapRequestMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsMapTilesMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsOpenMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsStateMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsWorldMsg
 *  com.cobbleclub.clubhouse.claims.protocol.MapClaimEntry
 *  com.cobbleclub.clubhouse.claims.protocol.MapTileEntry
 *  com.cobbleclub.clubhouse.claims.protocol.MemberEntry
 *  com.cobbleclub.clubhouse.claims.protocol.PermissionCatalogEntry
 *  com.cobbleclub.clubhouse.claims.protocol.PermissionStateEntry
 *  com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry
 *  com.cobbleclub.clubhouse.claims.protocol.WorldBoxEntry
 *  com.cobbleclub.clubhouse.claims.protocol.WorldBoxType
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_1263
 *  net.minecraft.class_1297
 *  net.minecraft.class_1922
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2338$class_2339
 *  net.minecraft.class_2561
 *  net.minecraft.class_2680
 *  net.minecraft.class_2902$class_2903
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_3620
 *  net.minecraft.class_3620$class_6594
 *  net.minecraft.class_8710
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.clubhouse.claims.protocol.ActionFeedback;
import com.cobbleclub.clubhouse.claims.protocol.BoxInfo;
import com.cobbleclub.clubhouse.claims.protocol.BudgetInfo;
import com.cobbleclub.clubhouse.claims.protocol.ChunkRect;
import com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry;
import com.cobbleclub.clubhouse.claims.protocol.ClaimMessagesEdit;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsActionMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsActionType;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsMapRequestMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsMapTilesMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsOpenMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsScreenProtocol;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsStateMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsWorldMsg;
import com.cobbleclub.clubhouse.claims.protocol.MapClaimEntry;
import com.cobbleclub.clubhouse.claims.protocol.MapTileEntry;
import com.cobbleclub.clubhouse.claims.protocol.MemberEntry;
import com.cobbleclub.clubhouse.claims.protocol.PermissionCatalogEntry;
import com.cobbleclub.clubhouse.claims.protocol.PermissionStateEntry;
import com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry;
import com.cobbleclub.clubhouse.claims.protocol.WorldBoxEntry;
import com.cobbleclub.clubhouse.claims.protocol.WorldBoxType;
import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.ClaimsStore;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.PermissionService;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.world.Heightmap;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.MapColor;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

public final class ClaimsService {
    private static final Map<UUID, WorldSnapshotState> WORLD_SNAPSHOT_STATE = new ConcurrentHashMap<UUID, WorldSnapshotState>();
    private static int revision;

    private ClaimsService() {
    }

    public static void open(ServerPlayerEntity player) {
        if (!PermissionService.has(player, "cobbleclub.claims.use", true)) {
            player.sendMessage((Text)Text.literal((String)"You do not have permission to use claims."), false);
            return;
        }
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.ClaimsOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        ClaimsOpenMsg message = new ClaimsOpenMsg(1, ClaimsService.textJson("CobbleClub Claims", "light_purple"), ClaimsService.budget(player), player.getBlockX(), player.getBlockZ(), ClaimsService.dimension(player.getServerWorld()), config.mapRadiusChunks, 3, 32, "^[A-Za-z0-9 _-]+$", 64, config.maxClaimChunksPerSide, ClaimsService.permissionCatalog(), List.of("VISITOR", "TRUSTED", "OWNER"), ClaimsService.visibleDetails(player), ClaimsService.mapClaims(player), null, "MAP", Boolean.valueOf(config.economyEnabled && config.claimBlockPurchaseAmount > 0 && ClaimsService.claimBlockPurchasePrice(player) > 0L), null, Integer.valueOf(config.maxClaimDistanceChunks), Boolean.valueOf(false), Boolean.valueOf(true), Boolean.valueOf(ClaimsService.isBypass(player)), List.of(ClaimsService.textJson("Shift-drag on the map to claim land.", "gray"), ClaimsService.textJson("Trusted players can build unless you change a permission.", "gray"), ClaimsService.textJson("Earn " + config.gemsPerPlaytimeReward + " gems every " + Math.max(1, config.claimBlockRewardIntervalSeconds / 60) + " minutes online.", "aqua"), ClaimsService.textJson("Use /daily for " + config.dailyGems + " gems each UTC day.", "green")), Map.of("claim_earn_playtime", ClaimsService.textJson("Playtime: +" + config.gemsPerPlaytimeReward + " gems every " + Math.max(1, config.claimBlockRewardIntervalSeconds / 60) + " minutes online", "aqua"), "claim_earn_daily", ClaimsService.textJson("Daily: +" + config.dailyGems + " gems with /daily", "green"), "claim_purchase", ClaimsService.textJson("Next purchase: +" + config.claimBlockPurchaseAmount + " for " + EconomyService.formatGems(ClaimsService.claimBlockPurchasePrice(player)), "yellow")));
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.ClaimsOpen(ClaimsScreenProtocol.INSTANCE.encode((Object)message)));
        ClaimsService.sendWorld(player);
        ClaimsService.sendWarpState(player);
    }

    public static void handleExtraAction(ServerPlayerEntity player, String action, String claimId, String value) {
        if (player == null || action == null || !PermissionService.has(player, "cobbleclub.claims.use", true)) {
            return;
        }
        String normalized = action.trim().toLowerCase(Locale.ROOT);
        Result result;
        switch (normalized) {
            case "refresh" -> {
                ClaimsService.sendWarpState(player);
                return;
            }
            case "leave" -> {
                ClaimsStore.ClaimData claim = ClaimsService.find(claimId);
                if (claim == null || claim.ownerUuid.equals(player.getUuidAsString())) {
                    result = Result.error("You cannot leave that claim.");
                } else if (claim.trusted.remove(player.getUuidAsString()) == null) {
                    result = Result.error("You are not a trusted member of that claim.");
                } else {
                    result = Result.ok(true, "You left " + claim.name + ".");
                }
            }
            case "set_public" -> {
                ClaimsStore.ClaimData claim = ClaimsService.manageable(player, claimId);
                if (claim == null) {
                    result = Result.error("You cannot change that claim warp.");
                } else {
                    claim.publicWarp = Boolean.parseBoolean(value);
                    if (claim.publicWarp && (claim.warpName == null || claim.warpName.isBlank())) {
                        claim.warpName = claim.name;
                    }
                    result = Result.ok(true, claim.publicWarp ? "Public warp enabled." : "Public warp disabled.");
                }
            }
            case "set_warp_name" -> {
                ClaimsStore.ClaimData claim = ClaimsService.manageable(player, claimId);
                String name = value == null ? "" : value.trim();
                if (claim == null) {
                    result = Result.error("You cannot rename that warp.");
                } else if (!ClaimsService.validWarpName(name)) {
                    result = Result.error("Use a clean 3-32 character warp name.");
                } else {
                    claim.warpName = name;
                    result = Result.ok(true, "Warp name updated.");
                }
            }
            case "warp" -> {
                ClaimsStore.ClaimData claim = ClaimsService.find(claimId);
                if (claim == null || ClaimsService.isBanned(player, claim)) {
                    result = Result.error("That warp is unavailable.");
                } else {
                    boolean member = claim.ownerUuid.equals(player.getUuidAsString()) || claim.trusted.containsKey(player.getUuidAsString()) || ClaimsService.isBypass(player);
                    if (!claim.publicWarp && !member) {
                        result = Result.error("That claim is private.");
                    } else {
                        result = ClaimsService.teleportToClaim(player, claim);
                    }
                }
            }
            default -> {
                return;
            }
        }
        if (result.changed) {
            ClaimsStore.save();
            ++revision;
        }
        ClaimsService.sendState(player, 0, result);
        ClaimsService.sendWorld(player);
        ClaimsService.sendWarpState(player);
    }

    public static void handleAction(ServerPlayerEntity player, String json) {
        Result result;
        if (!PermissionService.has(player, "cobbleclub.claims.use", true)) {
            return;
        }
        ClaimsActionMsg message = (ClaimsActionMsg)ClaimsScreenProtocol.INSTANCE.decode(json, ClaimsActionMsg.class);
        if (message == null || message.getProtocolVersion() != 1 || message.getAction() == null) {
            return;
        }
        if (message.getAction() == ClaimsActionType.SCREEN_CLOSED) {
            return;
        }
        try {
            result = ClaimsService.apply(player, message);
        }
        catch (Exception exception) {
            result = Result.error("The claim action could not be completed.");
        }
        if (result.changed) {
            ClaimsStore.save();
        }
        ++revision;
        ClaimsService.sendState(player, message.getNonce(), result);
        ClaimsService.sendWorld(player);
        ClaimsService.sendWarpState(player);
    }

    public static void handleMapRequest(ServerPlayerEntity player, String json) {
        ClaimsMapRequestMsg request = (ClaimsMapRequestMsg)ClaimsScreenProtocol.INSTANCE.decode(json, ClaimsMapRequestMsg.class);
        if (request == null || request.getProtocolVersion() != 1 || request.getDimension() == null || request.getChunks() == null) {
            return;
        }
        ServerWorld world = ClaimsService.world(player.getServer(), request.getDimension());
        if (world == null) {
            return;
        }
        ArrayList<MapTileEntry> tiles = new ArrayList<MapTileEntry>();
        ArrayList<List<Integer>> missing = new ArrayList<List<Integer>>();
        int count = 0;
        for (List pair : request.getChunks()) {
            int cz;
            if (count++ >= 128 || pair == null || pair.size() != 2 || pair.get(0) == null || pair.get(1) == null) break;
            int cx = (Integer)pair.get(0);
            if (!world.isChunkLoaded(cx, cz = ((Integer)pair.get(1)).intValue())) {
                missing.add(List.of(Integer.valueOf(cx), Integer.valueOf(cz)));
                continue;
            }
            tiles.add(new MapTileEntry(cx, cz, ClaimsService.encodeTile(world, cx, cz)));
        }
        ClaimsMapTilesMsg response = new ClaimsMapTilesMsg(1, request.getDimension(), tiles, List.of(), missing);
        if (ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.ClaimsMapTiles.ID)) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.ClaimsMapTiles(ClaimsScreenProtocol.INSTANCE.encode((Object)response)));
        }
    }

    public static boolean canBuild(ServerPlayerEntity player, BlockPos pos) {
        return ClaimsService.allowed(player, player.getServerWorld(), pos, "build");
    }

    public static boolean canInteract(ServerPlayerEntity player, BlockPos pos) {
        String permission = player.getServerWorld().getBlockEntity(pos) instanceof Inventory ? "containers" : "interact";
        return ClaimsService.allowed(player, player.getServerWorld(), pos, permission);
    }

    public static boolean canManageChestShop(ServerPlayerEntity player, BlockPos pos) {
        return ClaimsService.claimAt(player.getServerWorld(), pos) != null && ClaimsService.allowed(player, player.getServerWorld(), pos, "chest_shops");
    }

    public static boolean canBuyChestShop(ServerPlayerEntity player, BlockPos pos) {
        if (player == null || pos == null) {
            return false;
        }
        ClaimsStore.ClaimData claim = ClaimsService.claimAt(player.getServerWorld(), pos);
        if (claim == null) {
            return false;
        }
        return !ClaimsService.isBanned(player, claim);
    }

    public static boolean canPlace(ServerPlayerEntity player, BlockPos clickedPos, BlockPos targetPos) {
        return ClaimsService.allowed(player, player.getServerWorld(), clickedPos, "build") && ClaimsService.allowed(player, player.getServerWorld(), targetPos, "build");
    }

    public static boolean canUseItem(ServerPlayerEntity player, BlockPos pos) {
        return ClaimsService.allowed(player, player.getServerWorld(), pos, "item_use");
    }

    public static boolean canAttackEntity(ServerPlayerEntity player, Entity target) {
        String permission = target instanceof ServerPlayerEntity ? "pvp" : "entities";
        return ClaimsService.allowed(player, player.getServerWorld(), target.getBlockPos(), permission);
    }

    public static boolean canUseEntity(ServerPlayerEntity player, Entity target) {
        return ClaimsService.allowed(player, player.getServerWorld(), target.getBlockPos(), "entities");
    }

    public static boolean isBanned(ServerPlayerEntity player, ClaimsStore.ClaimData claim) {
        return claim != null && claim.banned.containsKey(player.getUuidAsString()) && !ClaimsService.isBypass(player);
    }

    public static ClaimsStore.ClaimData claimAt(ServerWorld world, BlockPos pos) {
        return ClaimsStore.findAt(ClaimsService.dimension(world), pos.getX(), pos.getZ());
    }

    public static boolean isClaimed(ServerWorld world, BlockPos pos) {
        return world != null && pos != null && ClaimsService.claimAt(world, pos) != null;
    }

    public static boolean sameProtectionZone(ServerWorld world, BlockPos from, BlockPos to) {
        ClaimsStore.ClaimData right;
        if (world == null || from == null || to == null) {
            return true;
        }
        ClaimsStore.ClaimData left = ClaimsService.claimAt(world, from);
        if (left == (right = ClaimsService.claimAt(world, to))) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.id != null && left.id.equals(right.id);
    }

    private static boolean allowed(ServerPlayerEntity player, ServerWorld world, BlockPos pos, String permission) {
        int required;
        ClaimsStore.ClaimData claim = ClaimsService.claimAt(world, pos);
        if (claim == null || ClaimsService.isBypass(player)) {
            return true;
        }
        if (claim.banned.containsKey(player.getUuidAsString())) {
            return false;
        }
        int role = ClaimsService.roleRank(claim, player);
        String requiredRole = claim.permissions.getOrDefault(permission, ClaimsService.defaultRole(permission));
        for (ClaimsStore.SubClaimData subClaim : claim.subClaims) {
            if (!subClaim.contains(pos) || !subClaim.permissionOverrides.containsKey(permission)) continue;
            requiredRole = subClaim.permissionOverrides.get(permission);
            break;
        }
        return role >= (required = ClaimsService.roleRank(requiredRole));
    }

    private static Result apply(ServerPlayerEntity player, ClaimsActionMsg message) {
        return switch (message.getAction()) {
            default -> throw new MatchException(null, null);
            case ClaimsActionType.CREATE_FROM_CHUNKS -> ClaimsService.require(player, "cobbleclub.claims.create", () -> ClaimsService.create(player, message.getRect()));
            case ClaimsActionType.RESIZE_TO_CHUNKS -> ClaimsService.require(player, "cobbleclub.claims.resize", () -> ClaimsService.resize(player, message.getClaimId(), message.getRect()));
            case ClaimsActionType.DELETE -> ClaimsService.require(player, "cobbleclub.claims.delete", () -> ClaimsService.delete(player, message.getClaimId()));
            case ClaimsActionType.RENAME -> ClaimsService.require(player, "cobbleclub.claims.rename", () -> ClaimsService.rename(player, message.getClaimId(), message.getName()));
            case ClaimsActionType.SET_PERMISSION_ROLE -> ClaimsService.require(player, "cobbleclub.claims.permissions", () -> ClaimsService.permission(player, message));
            case ClaimsActionType.TRUST -> ClaimsService.require(player, "cobbleclub.claims.trust", () -> ClaimsService.trust(player, message.getClaimId(), message.getName()));
            case ClaimsActionType.UNTRUST -> ClaimsService.require(player, "cobbleclub.claims.trust", () -> ClaimsService.untrust(player, message.getClaimId(), message.getTargetUuid()));
            case ClaimsActionType.BAN -> ClaimsService.require(player, "cobbleclub.claims.ban", () -> ClaimsService.ban(player, message.getClaimId(), message.getName()));
            case ClaimsActionType.UNBAN -> ClaimsService.require(player, "cobbleclub.claims.ban", () -> ClaimsService.unban(player, message.getClaimId(), message.getTargetUuid()));
            case ClaimsActionType.TRANSFER -> ClaimsService.require(player, "cobbleclub.claims.transfer", () -> ClaimsService.transfer(player, message.getClaimId(), message.getName()));
            case ClaimsActionType.SET_MESSAGES -> ClaimsService.require(player, "cobbleclub.claims.messages", () -> ClaimsService.messages(player, message.getClaimId(), message.getMessages()));
            case ClaimsActionType.TELEPORT -> ClaimsService.require(player, "cobbleclub.claims.teleport", () -> ClaimsService.teleport(player, message.getClaimId()));
            case ClaimsActionType.RENAME_SUB -> ClaimsService.require(player, "cobbleclub.claims.subclaim", () -> ClaimsService.renameSub(player, message.getClaimId(), message.getSubId(), message.getName()));
            case ClaimsActionType.DELETE_SUB -> ClaimsService.require(player, "cobbleclub.claims.subclaim", () -> ClaimsService.deleteSub(player, message.getClaimId(), message.getSubId()));
            case ClaimsActionType.BUY_BLOCKS -> ClaimsService.require(player, "cobbleclub.claims.buyblocks", () -> ClaimsService.buyBlocks(player));
            case ClaimsActionType.SCREEN_CLOSED -> Result.ok(false, "");
        };
    }

    private static Result require(ServerPlayerEntity player, String permission, Supplier<Result> action) {
        if (!PermissionService.has(player, permission, true)) {
            return Result.error("You do not have permission: " + permission);
        }
        return action.get();
    }

    private static Result create(ServerPlayerEntity player, ChunkRect rect) {
        if (!CobbleClubServer.config().claimsEnabled) {
            return Result.error("Claims are disabled on this server.");
        }
        if (!ClaimsService.valid(rect)) {
            return Result.error("Select a valid chunk area.");
        }
        int width = rect.getMaxCx() - rect.getMinCx() + 1;
        int depth = rect.getMaxCz() - rect.getMinCz() + 1;
        if (width > CobbleClubServer.config().maxClaimChunksPerSide || depth > CobbleClubServer.config().maxClaimChunksPerSide) {
            return Result.error("That claim is too large.");
        }
        if (!ClaimsService.nearPlayer(player, rect)) {
            return Result.error("That area is too far away from you.");
        }
        String dimension = ClaimsService.dimension(player.getServerWorld());
        if (ClaimsService.overlaps(dimension, rect, null)) {
            return Result.error("That area overlaps another claim.");
        }
        int area = width * depth * 256;
        if (ClaimsService.budget(player).getRemaining() < area) {
            return Result.error("You do not have enough claim blocks.");
        }
        ClaimsStore.ClaimData claim = new ClaimsStore.ClaimData();
        claim.id = UUID.randomUUID().toString();
        claim.ownerUuid = player.getUuidAsString();
        claim.ownerName = player.getGameProfile().getName();
        claim.name = "Claim " + (ClaimsService.owned(player).size() + 1);
        claim.dimension = dimension;
        claim.minCx = rect.getMinCx();
        claim.minCz = rect.getMinCz();
        claim.maxCx = rect.getMaxCx();
        claim.maxCz = rect.getMaxCz();
        claim.createdAt = System.currentTimeMillis();
        claim.normalize();
        ClaimsStore.all().add(claim);
        return Result.ok(true, "Claim created.");
    }

    private static Result resize(ServerPlayerEntity player, String id, ChunkRect rect) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        if (claim == null || !ClaimsService.valid(rect)) {
            return Result.error("That claim cannot be resized.");
        }
        int width = rect.getMaxCx() - rect.getMinCx() + 1;
        int depth = rect.getMaxCz() - rect.getMinCz() + 1;
        if (width > CobbleClubServer.config().maxClaimChunksPerSide || depth > CobbleClubServer.config().maxClaimChunksPerSide) {
            return Result.error("That claim is too large.");
        }
        if (!ClaimsService.nearPlayer(player, rect)) {
            return Result.error("That area is too far away from you.");
        }
        if (ClaimsService.overlaps(claim.dimension, rect, claim.id)) {
            return Result.error("That area overlaps another claim.");
        }
        int extra = width * depth * 256 - claim.area();
        if (extra > ClaimsService.budget(player).getRemaining()) {
            return Result.error("You do not have enough claim blocks.");
        }
        claim.minCx = rect.getMinCx();
        claim.minCz = rect.getMinCz();
        claim.maxCx = rect.getMaxCx();
        claim.maxCz = rect.getMaxCz();
        return Result.ok(true, "Claim resized.");
    }

    private static Result delete(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        if (claim == null) {
            return Result.error("You cannot delete that claim.");
        }
        ClaimsStore.all().remove(claim);
        return Result.ok(true, "Claim deleted.");
    }

    private static Result rename(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        if (claim == null || name == null || !name.matches("[A-Za-z0-9 _-]{3,32}")) {
            return Result.error("Use 3-32 letters, numbers, spaces, _ or -.");
        }
        claim.name = name;
        return Result.ok(true, "Claim renamed.");
    }

    private static Result permission(ServerPlayerEntity player, ClaimsActionMsg message) {
        String role;
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, message.getClaimId());
        if (claim == null || !ClaimsService.claimPermissionIds().contains(message.getPermission())) {
            return Result.error("That permission cannot be changed.");
        }
        Map<String, String> permissions = claim.permissions;
        if (message.getSubId() != null) {
            ClaimsStore.SubClaimData subClaim = ClaimsService.findSub(claim, message.getSubId());
            if (subClaim == null) {
                return Result.error("That subclaim does not exist.");
            }
            permissions = subClaim.permissionOverrides;
        }
        if ((role = message.getRole()) == null) {
            permissions.remove(message.getPermission());
        } else if (List.of("VISITOR", "TRUSTED", "OWNER").contains(role.toUpperCase(Locale.ROOT))) {
            permissions.put(message.getPermission(), role.toUpperCase(Locale.ROOT));
        } else {
            return Result.error("Invalid permission role.");
        }
        return Result.ok(true, "Permission updated.");
    }

    private static Result trust(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        ServerPlayerEntity target = ClaimsService.online(player.getServer(), name);
        if (claim == null || target == null) {
            return Result.error("That player must be online.");
        }
        if (target.getUuidAsString().equals(claim.ownerUuid)) {
            return Result.error("The owner is already trusted.");
        }
        claim.banned.remove(target.getUuidAsString());
        claim.trusted.put(target.getUuidAsString(), new ClaimsStore.MemberData(target.getUuidAsString(), target.getGameProfile().getName(), System.currentTimeMillis()));
        return Result.ok(true, "Player trusted.");
    }

    private static Result untrust(ServerPlayerEntity player, String id, String uuid) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        if (claim == null || uuid == null || claim.trusted.remove(uuid) == null) {
            return Result.error("That player is not trusted.");
        }
        return Result.ok(true, "Trust removed.");
    }

    private static Result ban(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        ServerPlayerEntity target = ClaimsService.online(player.getServer(), name);
        if (claim == null || target == null || target.getUuidAsString().equals(claim.ownerUuid)) {
            return Result.error("That player cannot be banned.");
        }
        claim.trusted.remove(target.getUuidAsString());
        claim.banned.put(target.getUuidAsString(), new ClaimsStore.MemberData(target.getUuidAsString(), target.getGameProfile().getName(), System.currentTimeMillis()));
        return Result.ok(true, "Player banned from the claim.");
    }

    private static Result unban(ServerPlayerEntity player, String id, String uuid) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        if (claim == null || uuid == null || claim.banned.remove(uuid) == null) {
            return Result.error("That player is not banned.");
        }
        return Result.ok(true, "Ban removed.");
    }

    private static Result transfer(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        ServerPlayerEntity target = ClaimsService.online(player.getServer(), name);
        if (claim == null || target == null || target.getUuid().equals(player.getUuid())) {
            return Result.error("That claim cannot be transferred.");
        }
        claim.ownerUuid = target.getUuidAsString();
        claim.ownerName = target.getGameProfile().getName();
        claim.trusted.remove(target.getUuidAsString());
        claim.trusted.put(player.getUuidAsString(), new ClaimsStore.MemberData(player.getUuidAsString(), player.getGameProfile().getName(), System.currentTimeMillis()));
        return Result.ok(true, "Claim transferred.");
    }

    private static Result messages(ServerPlayerEntity player, String id, ClaimMessagesEdit edit) {
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, id);
        if (claim == null || edit == null) {
            return Result.error("Claim messages could not be updated.");
        }
        claim.enterTitle = ClaimsService.limit(edit.getEnterTitle(), 64);
        claim.enterSubtitle = ClaimsService.limit(edit.getEnterSubtitle(), 64);
        claim.leaveTitle = ClaimsService.limit(edit.getLeaveTitle(), 64);
        claim.leaveSubtitle = ClaimsService.limit(edit.getLeaveSubtitle(), 64);
        return Result.ok(true, "Claim messages updated.");
    }

    private static Result teleport(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = ClaimsService.visible(player, id);
        if (claim == null) {
            return Result.error("You cannot teleport to that claim.");
        }
        return ClaimsService.teleportToClaim(player, claim);
    }

    private static Result teleportToClaim(ServerPlayerEntity player, ClaimsStore.ClaimData claim) {
        ServerWorld world = ClaimsService.world(player.getServer(), claim.dimension);
        if (world == null) {
            return Result.error("That world is unavailable.");
        }
        BlockPos safe = ClaimsService.findSafeClaimTeleport(world, claim);
        if (safe == null) {
            return Result.error("No safe surface location was found inside that claim.");
        }
        player.teleport(world, (double)safe.getX() + 0.5, (double)safe.getY(), (double)safe.getZ() + 0.5, player.getYaw(), player.getPitch());
        return Result.ok(false, "Teleported safely to the claim surface.");
    }

    private static BlockPos findSafeClaimTeleport(ServerWorld world, ClaimsStore.ClaimData claim) {
        int minX = claim.minCx << 4;
        int maxX = (claim.maxCx << 4) + 15;
        int minZ = claim.minCz << 4;
        int maxZ = (claim.maxCz << 4) + 15;
        int centerX = minX + (maxX - minX) / 2;
        int centerZ = minZ + (maxZ - minZ) / 2;
        int maxRadius = Math.max(maxX - minX, maxZ - minZ);
        for (int radius = 0; radius <= maxRadius; ++radius) {
            BlockPos safe;
            int left = Math.max(minX, centerX - radius);
            int right = Math.min(maxX, centerX + radius);
            int top = Math.max(minZ, centerZ - radius);
            int bottom = Math.min(maxZ, centerZ + radius);
            if (radius == 0) {
                BlockPos safe2 = ClaimsService.safeSurfaceAt(world, centerX, centerZ);
                if (safe2 == null) continue;
                return safe2;
            }
            for (int x = left; x <= right; ++x) {
                safe = ClaimsService.safeSurfaceAt(world, x, top);
                if (safe != null) {
                    return safe;
                }
                if (bottom == top || (safe = ClaimsService.safeSurfaceAt(world, x, bottom)) == null) continue;
                return safe;
            }
            for (int z = top + 1; z < bottom; ++z) {
                safe = ClaimsService.safeSurfaceAt(world, left, z);
                if (safe != null) {
                    return safe;
                }
                if (right == left || (safe = ClaimsService.safeSurfaceAt(world, right, z)) == null) continue;
                return safe;
            }
        }
        return null;
    }

    private static BlockPos safeSurfaceAt(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int feetY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, feetY, z);
        BlockPos head = feet.up();
        BlockPos ground = feet.down();
        BlockState groundState = world.getBlockState(ground);
        BlockState feetState = world.getBlockState(feet);
        BlockState headState = world.getBlockState(head);
        if (groundState.isAir() || !groundState.getFluidState().isEmpty() || ClaimsService.claimTeleportHazard(groundState)) {
            return null;
        }
        if (!feetState.isAir() || !headState.isAir()) {
            return null;
        }
        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()) {
            return null;
        }
        if (ClaimsService.claimTeleportHazard(feetState) || ClaimsService.claimTeleportHazard(headState)) {
            return null;
        }
        return feet;
    }

    private static boolean claimTeleportHazard(BlockState state) {
        return state.isOf(Blocks.LAVA) || state.isOf(Blocks.WATER) || state.isOf(Blocks.CACTUS) || state.isOf(Blocks.MAGMA_BLOCK) || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE) || state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE) || state.isOf(Blocks.SWEET_BERRY_BUSH) || state.isOf(Blocks.WITHER_ROSE) || state.isOf(Blocks.POWDER_SNOW) || state.isOf(Blocks.POINTED_DRIPSTONE);
    }

    private static Result renameSub(ServerPlayerEntity player, String claimId, String subId, String name) {
        ClaimsStore.SubClaimData subClaim;
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, claimId);
        ClaimsStore.SubClaimData subClaimData = subClaim = claim == null ? null : ClaimsService.findSub(claim, subId);
        if (subClaim == null || name == null || !name.matches("[A-Za-z0-9 _-]{3,32}")) {
            return Result.error("Use 3-32 letters, numbers, spaces, _ or -.");
        }
        subClaim.name = name;
        return Result.ok(true, "Subclaim renamed.");
    }

    private static Result deleteSub(ServerPlayerEntity player, String claimId, String subId) {
        ClaimsStore.SubClaimData subClaim;
        ClaimsStore.ClaimData claim = ClaimsService.manageable(player, claimId);
        ClaimsStore.SubClaimData subClaimData = subClaim = claim == null ? null : ClaimsService.findSub(claim, subId);
        if (subClaim == null) {
            return Result.error("That subclaim does not exist.");
        }
        claim.subClaims.remove(subClaim);
        return Result.ok(true, "Subclaim deleted.");
    }

    private static Result buyBlocks(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        long price = ClaimsService.claimBlockPurchasePrice(player);
        if (!config.economyEnabled || config.claimBlockPurchaseAmount <= 0 || price <= 0L) {
            return Result.error("Buying claim blocks is disabled.");
        }
        if (!EconomyService.withdrawGems(player, price)) {
            return Result.error("You need " + EconomyService.formatGems(price) + " for the next land expansion.");
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.bonusClaimBlocks = ClaimsService.safeAdd(data.bonusClaimBlocks, config.claimBlockPurchaseAmount);
        data.claimBlockPurchases = ClaimsService.safeAdd(data.claimBlockPurchases, 1);
        ++data.revision;
        PlayerDataStore.save();
        long next = ClaimsService.claimBlockPurchasePrice(player);
        return Result.ok(true, "Purchased " + config.claimBlockPurchaseAmount + " claim blocks for " + EconomyService.formatGems(price) + ". Next expansion: " + EconomyService.formatGems(next) + ".");
    }

    private static void sendWarpState(ServerPlayerEntity player) {
        if (player == null || !ServerPlayNetworking.canSend(player, Payloads.ClaimsWarpState.ID)) {
            return;
        }
        JsonObject root = new JsonObject();
        JsonArray warps = new JsonArray();
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (claim == null) continue;
            boolean owned = claim.ownerUuid.equals(player.getUuidAsString());
            if (!claim.publicWarp && !owned) continue;
            JsonObject entry = new JsonObject();
            entry.addProperty("claimId", claim.id);
            entry.addProperty("name", claim.warpName == null || claim.warpName.isBlank() ? claim.name : claim.warpName);
            entry.addProperty("claimName", claim.name);
            entry.addProperty("owner", claim.ownerName);
            entry.addProperty("world", claim.dimension);
            entry.addProperty("owned", owned);
            entry.addProperty("public", claim.publicWarp);
            warps.add(entry);
        }
        root.add("warps", warps);
        ServerPlayNetworking.send(player, new Payloads.ClaimsWarpState(root.toString()));
    }

    private static boolean validWarpName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9 _'\\-]{3,32}")) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return !(normalized.contains("fuck")
                || normalized.contains("shit")
                || normalized.contains("bitch")
                || normalized.contains("cunt")
                || normalized.contains("nigger")
                || normalized.contains("faggot")
                || normalized.contains("retard"));
    }

    public static long claimBlockPurchasePrice(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        long base = Math.max(1L, config.claimBlockPurchasePrice);
        long cap = Math.max(base, config.claimBlockPurchasePriceCap);
        int purchases = Math.max(0, EconomyService.data((ServerPlayerEntity)player).claimBlockPurchases);
        double multiplier = Math.max(1.0, config.claimBlockPurchasePriceMultiplier);
        double calculated = (double)base * Math.pow(multiplier, purchases);
        if (!Double.isFinite(calculated) || calculated >= (double)cap) {
            return cap;
        }
        return Math.max(base, Math.min(cap, Math.round(calculated)));
    }

    public static String purchaseBlocks(ServerPlayerEntity player) {
        Result result = ClaimsService.buyBlocks(player);
        if (result.ok) {
            ++revision;
            ClaimsService.sendWorld(player);
        }
        return result.message;
    }

    public static String adminDeleteClaimHere(ServerPlayerEntity operator) {
        ClaimsStore.ClaimData claim = ClaimsService.claimAt(operator.getServerWorld(), operator.getBlockPos());
        if (claim == null) {
            return "There is no CobbleClub claim at your position.";
        }
        String description = claim.name + " (owner: " + claim.ownerName + ", id: " + claim.id + ")";
        ClaimsStore.all().remove(claim);
        ClaimsStore.save();
        ++revision;
        if (operator.getServer() != null) {
            for (ServerPlayerEntity player : operator.getServer().getPlayerManager().getPlayerList()) {
                ClaimsService.sendWorld(player);
            }
        }
        return "Deleted " + description + " including its subclaims, trust list, bans and permissions.";
    }

    public static boolean createSubclaim(ServerPlayerEntity player, String name, BlockPos first, BlockPos second) {
        if (!PermissionService.has(player, "cobbleclub.claims.subclaim", true)) {
            return false;
        }
        if (name == null || !name.matches("[A-Za-z0-9_-]{3,24}") || first == null || second == null) {
            return false;
        }
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());
        ClaimsStore.ClaimData claim = ClaimsService.claimAt(player.getServerWorld(), first);
        if (claim == null || ClaimsService.manageable(player, claim.id) == null || ClaimsService.claimAt(player.getServerWorld(), second) != claim) {
            return false;
        }
        for (ClaimsStore.SubClaimData existing : claim.subClaims) {
            if (minX > existing.maxX || maxX < existing.minX || minY > existing.maxY || maxY < existing.minY || minZ > existing.maxZ || maxZ < existing.minZ) continue;
            return false;
        }
        ClaimsStore.SubClaimData subClaim = new ClaimsStore.SubClaimData();
        subClaim.id = UUID.randomUUID().toString();
        subClaim.name = name;
        subClaim.minX = minX;
        subClaim.minY = minY;
        subClaim.minZ = minZ;
        subClaim.maxX = maxX;
        subClaim.maxY = maxY;
        subClaim.maxZ = maxZ;
        subClaim.normalize();
        claim.subClaims.add(subClaim);
        ClaimsStore.save();
        return true;
    }

    private static void sendState(ServerPlayerEntity player, int nonce, Result result) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.ClaimsState.ID)) {
            return;
        }
        ClaimsStateMsg state = new ClaimsStateMsg(1, Math.max(1, revision), ClaimsService.budget(player), ClaimsService.visibleDetails(player), ClaimsService.mapClaims(player), new ActionFeedback(nonce, result.ok, ClaimsService.textJson(result.message, result.ok ? "green" : "red")));
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.ClaimsState(ClaimsScreenProtocol.INSTANCE.encode((Object)state)));
    }

    public static void syncWorldSnapshots(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String dim = ClaimsService.dimension(player.getServerWorld());
            WorldSnapshotState state = WORLD_SNAPSHOT_STATE.get(player.getUuid());
            if (state != null && state.revision == revision && state.dimension.equals(dim)) continue;
            ClaimsService.sendWorld(player);
        }
    }

    public static void forgetWorldSnapshot(ServerPlayerEntity player) {
        if (player != null) {
            WORLD_SNAPSHOT_STATE.remove(player.getUuid());
        }
    }

    private static void sendWorld(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.ClaimsWorld.ID)) {
            return;
        }
        String dimension = ClaimsService.dimension(player.getServerWorld());
        List<WorldBoxEntry> boxes = ClaimsStore.all().stream().filter(claim -> dimension.equals(claim.dimension)).map(claim -> new WorldBoxEntry(claim.ownerUuid.equals(player.getUuidAsString()) ? WorldBoxType.MAIN : WorldBoxType.OTHER, ClaimsService.box(claim), null)).toList();
        ClaimsWorldMsg message = new ClaimsWorldMsg(1, dimension, boxes);
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.ClaimsWorld(ClaimsScreenProtocol.INSTANCE.encode((Object)message)));
        WORLD_SNAPSHOT_STATE.put(player.getUuid(), new WorldSnapshotState(dimension, revision));
    }

    private static List<ClaimDetailEntry> visibleDetails(ServerPlayerEntity player) {
        ArrayList<ClaimDetailEntry> result = new ArrayList<ClaimDetailEntry>();
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            boolean owner = claim.ownerUuid.equals(player.getUuidAsString());
            if (!owner && !claim.trusted.containsKey(player.getUuidAsString()) && !ClaimsService.isBypass(player)) continue;
            result.add(ClaimsService.detail(player, claim, owner));
        }
        result.sort(Comparator.comparing(ClaimDetailEntry::getCreatedAt));
        return result;
    }

    private static ClaimDetailEntry detail(ServerPlayerEntity player, ClaimsStore.ClaimData claim, boolean owner) {
        ArrayList<MemberEntry> members = new ArrayList<MemberEntry>();
        members.add(new MemberEntry(claim.ownerUuid, claim.ownerName, ClaimsService.online(player.getServer(), claim.ownerUuid) != null, claim.createdAt, true));
        for (ClaimsStore.MemberData member2 : claim.trusted.values()) {
            members.add(new MemberEntry(member2.uuid, member2.name, ClaimsService.online(player.getServer(), member2.uuid) != null, member2.joinedAt, false));
        }
        List<MemberEntry> banned = claim.banned.values().stream().map(member -> new MemberEntry(member.uuid, member.name, ClaimsService.online(player.getServer(), member.uuid) != null, member.joinedAt, false)).toList();
        List<PermissionStateEntry> permissions = claim.permissions.entrySet().stream().map(entry -> new PermissionStateEntry((String)entry.getKey(), (String)entry.getValue())).toList();
        List<SubClaimEntry> subClaims = claim.subClaims.stream().map(subClaim -> new SubClaimEntry(subClaim.id, subClaim.name, new BoxInfo(subClaim.minX, subClaim.minY, subClaim.minZ, subClaim.maxX, subClaim.maxY, subClaim.maxZ), subClaim.permissionOverrides.entrySet().stream().map(entry -> new PermissionStateEntry((String)entry.getKey(), (String)entry.getValue())).toList())).toList();
        return new ClaimDetailEntry(claim.id, claim.name, claim.ownerName, claim.dimension, claim.dimension, claim.dimension.equals(ClaimsService.dimension(player.getServerWorld())), ClaimsService.box(claim), false, claim.area(), claim.createdAt, members, banned, subClaims, permissions, claim.enterTitle, claim.enterSubtitle, claim.leaveTitle, claim.leaveSubtitle, owner, owner || ClaimsService.isBypass(player), Boolean.valueOf(owner || ClaimsService.isBypass(player)), Boolean.valueOf(owner || ClaimsService.isBypass(player)));
    }

    private static List<MapClaimEntry> mapClaims(ServerPlayerEntity player) {
        String dimension = ClaimsService.dimension(player.getServerWorld());
        ArrayList<MapClaimEntry> result = new ArrayList<MapClaimEntry>();
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (!dimension.equals(claim.dimension)) continue;
            String relation = claim.ownerUuid.equals(player.getUuidAsString()) ? "OWN" : (claim.trusted.containsKey(player.getUuidAsString()) ? "TRUSTED" : "OTHER");
            result.add(new MapClaimEntry(claim.id, claim.name, claim.ownerName, claim.minCx << 4, claim.minCz << 4, (claim.maxCx << 4) + 15, (claim.maxCz << 4) + 15, relation));
        }
        return result;
    }

    private static List<PermissionCatalogEntry> permissionCatalog() {
        List<String> roles = List.of("VISITOR", "TRUSTED", "OWNER");
        return List.of(new PermissionCatalogEntry("build", ClaimsService.textJson("Build & Break", "white"), ClaimsService.textJson("Place and break blocks", "gray"), "TRUSTED", roles, "minecraft:diamond_pickaxe"), new PermissionCatalogEntry("interact", ClaimsService.textJson("Use Blocks", "white"), ClaimsService.textJson("Doors, buttons, beds, redstone and other blocks", "gray"), "TRUSTED", roles, "minecraft:oak_door"), new PermissionCatalogEntry("containers", ClaimsService.textJson("Containers", "white"), ClaimsService.textJson("Chests, barrels, hoppers and storage", "gray"), "TRUSTED", roles, "minecraft:chest"), new PermissionCatalogEntry("chest_shops", ClaimsService.textJson("Chest Shops", "gold"), ClaimsService.textJson("Place, edit, stock or break player chest shops", "gray"), "OWNER", roles, "minecraft:oak_sign"), new PermissionCatalogEntry("item_use", ClaimsService.textJson("Use Items", "white"), ClaimsService.textJson("Buckets, placeable items and right-click item use", "gray"), "TRUSTED", roles, "minecraft:water_bucket"), new PermissionCatalogEntry("entities", ClaimsService.textJson("Entities", "white"), ClaimsService.textJson("Interact with or attack mobs, armor stands and vehicles", "gray"), "TRUSTED", roles, "minecraft:saddle"), new PermissionCatalogEntry("pvp", ClaimsService.textJson("PvP", "white"), ClaimsService.textJson("Attack players inside this claim", "gray"), "OWNER", roles, "minecraft:diamond_sword"));
    }

    private static List<String> claimPermissionIds() {
        return List.of("build", "interact", "containers", "chest_shops", "item_use", "entities", "pvp");
    }

    public static BudgetInfo budget(ServerPlayerEntity player) {
        int used = ClaimsService.owned(player).stream().mapToInt(ClaimsStore.ClaimData::area).sum();
        int total = ClaimsService.safeAdd(Math.max(0, CobbleClubServer.config().initialClaimBlocks), EconomyService.data((ServerPlayerEntity)player).bonusClaimBlocks);
        return new BudgetInfo(total, Math.max(0, total - used));
    }

    private static List<ClaimsStore.ClaimData> owned(ServerPlayerEntity player) {
        return ClaimsStore.all().stream().filter(claim -> claim.ownerUuid.equals(player.getUuidAsString())).toList();
    }

    private static ClaimsStore.ClaimData manageable(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = ClaimsService.find(id);
        return claim != null && (claim.ownerUuid.equals(player.getUuidAsString()) || ClaimsService.isBypass(player)) ? claim : null;
    }

    private static ClaimsStore.ClaimData visible(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = ClaimsService.find(id);
        return claim != null && (claim.ownerUuid.equals(player.getUuidAsString()) || claim.trusted.containsKey(player.getUuidAsString()) || ClaimsService.isBypass(player)) ? claim : null;
    }

    private static ClaimsStore.ClaimData find(String id) {
        return ClaimsStore.findById(id);
    }

    private static boolean overlaps(String dimension, ChunkRect rect, String exceptId) {
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (!dimension.equals(claim.dimension) || claim.id.equals(exceptId) || rect.getMinCx() > claim.maxCx || rect.getMaxCx() < claim.minCx || rect.getMinCz() > claim.maxCz || rect.getMaxCz() < claim.minCz) continue;
            return true;
        }
        return false;
    }

    private static boolean valid(ChunkRect rect) {
        return rect != null && rect.getMinCx() <= rect.getMaxCx() && rect.getMinCz() <= rect.getMaxCz();
    }

    private static boolean nearPlayer(ServerPlayerEntity player, ChunkRect rect) {
        int limit = Math.max(0, CobbleClubServer.config().maxClaimDistanceChunks);
        int playerCx = player.getBlockX() >> 4;
        int playerCz = player.getBlockZ() >> 4;
        int nearestCx = Math.max(rect.getMinCx(), Math.min(playerCx, rect.getMaxCx()));
        int nearestCz = Math.max(rect.getMinCz(), Math.min(playerCz, rect.getMaxCz()));
        return Math.max(Math.abs(nearestCx - playerCx), Math.abs(nearestCz - playerCz)) <= limit;
    }

    private static ClaimsStore.SubClaimData findSub(ClaimsStore.ClaimData claim, String id) {
        if (claim == null || id == null) {
            return null;
        }
        for (ClaimsStore.SubClaimData subClaim : claim.subClaims) {
            if (!id.equals(subClaim.id)) continue;
            return subClaim;
        }
        return null;
    }

    private static BoxInfo box(ClaimsStore.ClaimData claim) {
        return new BoxInfo(claim.minCx << 4, -64, claim.minCz << 4, (claim.maxCx << 4) + 15, 320, (claim.maxCz << 4) + 15);
    }

    private static int roleRank(ClaimsStore.ClaimData claim, ServerPlayerEntity player) {
        if (claim.ownerUuid.equals(player.getUuidAsString())) {
            return 2;
        }
        if (claim.trusted.containsKey(player.getUuidAsString())) {
            return 1;
        }
        return 0;
    }

    private static String defaultRole(String permission) {
        return "pvp".equals(permission) || "chest_shops".equals(permission) ? "OWNER" : "TRUSTED";
    }

    private static int roleRank(String role) {
        if (role == null) {
            return 1;
        }
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "OWNER" -> 2;
            case "TRUSTED" -> 1;
            default -> 0;
        };
    }

    private static boolean isBypass(ServerPlayerEntity player) {
        return CobbleClubServer.config().operatorsBypassClaims && PermissionService.admin(player, "cobbleclub.claims.bypass", 2);
    }

    private static String dimension(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static ServerWorld world(MinecraftServer server, String dimension) {
        if (server == null || dimension == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (!dimension.equals(ClaimsService.dimension(world))) continue;
            return world;
        }
        return null;
    }

    private static ServerPlayerEntity online(MinecraftServer server, String nameOrUuid) {
        if (server == null || nameOrUuid == null) {
            return null;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!nameOrUuid.equalsIgnoreCase(player.getGameProfile().getName()) && !nameOrUuid.equals(player.getUuidAsString())) continue;
            return player;
        }
        return null;
    }

    private static String encodeTile(ServerWorld world, int cx, int cz) {
        byte[] bytes = new byte[256];
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int z = 0; z < 16; ++z) {
            for (int x = 0; x < 16; ++x) {
                int blockX = (cx << 4) + x;
                int blockZ = (cz << 4) + z;
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, blockX, blockZ) - 1;
                pos.set(blockX, y, blockZ);
                MapColor color = world.getBlockState((BlockPos)pos).getMapColor((BlockView)world, (BlockPos)pos);
                bytes[x + z * 16] = color == MapColor.CLEAR ? (byte)0 : color.getRenderColorByte(MapColor.Brightness.NORMAL);
            }
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String textJson(String text, String color) {
        String escaped = (text == null ? "" : text).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return "{\"text\":\"" + escaped + "\",\"color\":\"" + color + "\"}";
    }

    private static int safeAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, left + right);
    }

    private record Result(boolean ok, boolean changed, String message) {
        static Result ok(boolean changed, String message) {
            return new Result(true, changed, message);
        }

        static Result error(String message) {
            return new Result(false, false, message);
        }
    }

    private record WorldSnapshotState(String dimension, int revision) {
    }
}

