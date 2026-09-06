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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.MapColor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimsService {
    private static final Map<UUID, WorldSnapshotState> WORLD_SNAPSHOT_STATE = new ConcurrentHashMap<>();

    private static int revision;

    private ClaimsService() {}

    public static void open(ServerPlayerEntity player) {
        if (!PermissionService.has(player, "cobbleclub.claims.use", true)) {
            player.sendMessage(net.minecraft.text.Text.literal("You do not have permission to use claims."), false);
            return;
        }
        if (!ServerPlayNetworking.canSend(player, Payloads.ClaimsOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        ClaimsOpenMsg message = new ClaimsOpenMsg(
                1,
                textJson("CobbleClub Claims", "light_purple"),
                budget(player),
                player.getBlockX(),
                player.getBlockZ(),
                dimension(player.getServerWorld()),
                config.mapRadiusChunks,
                3,
                32,
                "^[A-Za-z0-9 _-]+$",
                64,
                config.maxClaimChunksPerSide,
                permissionCatalog(),
                List.of("VISITOR", "TRUSTED", "OWNER"),
                visibleDetails(player),
                mapClaims(player),
                null,
                "MAP",
                config.economyEnabled && config.claimBlockPurchaseAmount > 0 && config.claimBlockPurchasePrice > 0,
                null,
                config.maxClaimDistanceChunks,
                false,
                true,
                isBypass(player),
                List.of(
                        textJson("Shift-drag on the map to claim land.", "gray"),
                        textJson("Trusted players can build unless you change a permission.", "gray"),
                        textJson("Earn " + config.gemsPerPlaytimeReward + " gems every "
                                + Math.max(1, config.claimBlockRewardIntervalSeconds / 60) + " minutes online.", "aqua"),
                        textJson("Use /daily for " + config.dailyGems + " gems each UTC day.", "green")
                ),
                Map.of(
                        "claim_earn_playtime", textJson("Playtime: +" + config.gemsPerPlaytimeReward + " gems every "
                                + Math.max(1, config.claimBlockRewardIntervalSeconds / 60) + " minutes online", "aqua"),
                        "claim_earn_daily", textJson("Daily: +" + config.dailyGems + " gems with /daily", "green"),
                        "claim_purchase", textJson("Purchase: +" + config.claimBlockPurchaseAmount + " for "
                                + EconomyService.formatGems(config.claimBlockPurchasePrice), "yellow")
                )
        );
        ServerPlayNetworking.send(player, new Payloads.ClaimsOpen(ClaimsScreenProtocol.INSTANCE.encode(message)));
        sendWorld(player);
    }

    public static void handleAction(ServerPlayerEntity player, String json) {
        if (!PermissionService.has(player, "cobbleclub.claims.use", true)) return;
        ClaimsActionMsg message = ClaimsScreenProtocol.INSTANCE.decode(json, ClaimsActionMsg.class);
        if (message == null || message.getProtocolVersion() != 1 || message.getAction() == null) return;
        if (message.getAction() == ClaimsActionType.SCREEN_CLOSED) return;

        Result result;
        try {
            result = apply(player, message);
        } catch (Exception exception) {
            result = Result.error("The claim action could not be completed.");
        }
        if (result.changed) ClaimsStore.save();
        revision++;
        sendState(player, message.getNonce(), result);
        sendWorld(player);
    }

    public static void handleMapRequest(ServerPlayerEntity player, String json) {
        ClaimsMapRequestMsg request = ClaimsScreenProtocol.INSTANCE.decode(json, ClaimsMapRequestMsg.class);
        if (request == null || request.getProtocolVersion() != 1 || request.getDimension() == null || request.getChunks() == null) return;
        ServerWorld world = world(player.getServer(), request.getDimension());
        if (world == null) return;

        List<MapTileEntry> tiles = new ArrayList<>();
        List<List<Integer>> missing = new ArrayList<>();
        int count = 0;
        for (List<Integer> pair : request.getChunks()) {
            if (count++ >= 128 || pair == null || pair.size() != 2 || pair.get(0) == null || pair.get(1) == null) break;
            int cx = pair.get(0);
            int cz = pair.get(1);
            if (!world.isChunkLoaded(cx, cz)) {
                missing.add(List.of(cx, cz));
                continue;
            }
            tiles.add(new MapTileEntry(cx, cz, encodeTile(world, cx, cz)));
        }

        ClaimsMapTilesMsg response = new ClaimsMapTilesMsg(1, request.getDimension(), tiles, List.of(), missing);
        if (ServerPlayNetworking.canSend(player, Payloads.ClaimsMapTiles.ID)) {
            ServerPlayNetworking.send(player, new Payloads.ClaimsMapTiles(ClaimsScreenProtocol.INSTANCE.encode(response)));
        }
    }

    public static boolean canBuild(ServerPlayerEntity player, BlockPos pos) {
        return allowed(player, player.getServerWorld(), pos, "build");
    }

    public static boolean canInteract(ServerPlayerEntity player, BlockPos pos) {
        String permission = player.getServerWorld().getBlockEntity(pos) instanceof Inventory ? "containers" : "interact";
        return allowed(player, player.getServerWorld(), pos, permission);
    }

    public static boolean canPlace(ServerPlayerEntity player, BlockPos clickedPos, BlockPos targetPos) {
        return allowed(player, player.getServerWorld(), clickedPos, "build")
                && allowed(player, player.getServerWorld(), targetPos, "build");
    }

    public static boolean canUseItem(ServerPlayerEntity player, BlockPos pos) {
        return allowed(player, player.getServerWorld(), pos, "item_use");
    }

    public static boolean canAttackEntity(ServerPlayerEntity player, net.minecraft.entity.Entity target) {
        String permission = target instanceof ServerPlayerEntity ? "pvp" : "entities";
        return allowed(player, player.getServerWorld(), target.getBlockPos(), permission);
    }

    public static boolean canUseEntity(ServerPlayerEntity player, net.minecraft.entity.Entity target) {
        return allowed(player, player.getServerWorld(), target.getBlockPos(), "entities");
    }

    public static boolean isBanned(ServerPlayerEntity player, ClaimsStore.ClaimData claim) {
        return claim != null && claim.banned.containsKey(player.getUuidAsString()) && !isBypass(player);
    }

    public static ClaimsStore.ClaimData claimAt(ServerWorld world, BlockPos pos) {
        return ClaimsStore.findAt(dimension(world), pos.getX(), pos.getZ());
    }

    /** Fast, allocation-light claim lookup for environmental protection mixins. */
    public static boolean isClaimed(ServerWorld world, BlockPos pos) {
        return world != null && pos != null && claimAt(world, pos) != null;
    }

    /**
     * Returns true when an environmental transfer may cross from one block position to another.
     * Transfers are allowed inside the same claim or entirely outside claims, but never across a
     * claim boundary or directly between two different claims.
     */
    public static boolean sameProtectionZone(ServerWorld world, BlockPos from, BlockPos to) {
        if (world == null || from == null || to == null) return true;
        ClaimsStore.ClaimData left = claimAt(world, from);
        ClaimsStore.ClaimData right = claimAt(world, to);
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.id != null && left.id.equals(right.id);
    }

    private static boolean allowed(ServerPlayerEntity player, ServerWorld world, BlockPos pos, String permission) {
        ClaimsStore.ClaimData claim = claimAt(world, pos);
        if (claim == null || isBypass(player)) return true;
        if (claim.banned.containsKey(player.getUuidAsString())) return false;
        int role = roleRank(claim, player);
        String requiredRole = claim.permissions.getOrDefault(permission, defaultRole(permission));
        for (ClaimsStore.SubClaimData subClaim : claim.subClaims) {
            if (subClaim.contains(pos) && subClaim.permissionOverrides.containsKey(permission)) {
                requiredRole = subClaim.permissionOverrides.get(permission);
                break;
            }
        }
        int required = roleRank(requiredRole);
        return role >= required;
    }

    private static Result apply(ServerPlayerEntity player, ClaimsActionMsg message) {
        return switch (message.getAction()) {
            case CREATE_FROM_CHUNKS -> require(player, "cobbleclub.claims.create", () -> create(player, message.getRect()));
            case RESIZE_TO_CHUNKS -> require(player, "cobbleclub.claims.resize", () -> resize(player, message.getClaimId(), message.getRect()));
            case DELETE -> require(player, "cobbleclub.claims.delete", () -> delete(player, message.getClaimId()));
            case RENAME -> require(player, "cobbleclub.claims.rename", () -> rename(player, message.getClaimId(), message.getName()));
            case SET_PERMISSION_ROLE -> require(player, "cobbleclub.claims.permissions", () -> permission(player, message));
            case TRUST -> require(player, "cobbleclub.claims.trust", () -> trust(player, message.getClaimId(), message.getName()));
            case UNTRUST -> require(player, "cobbleclub.claims.trust", () -> untrust(player, message.getClaimId(), message.getTargetUuid()));
            case BAN -> require(player, "cobbleclub.claims.ban", () -> ban(player, message.getClaimId(), message.getName()));
            case UNBAN -> require(player, "cobbleclub.claims.ban", () -> unban(player, message.getClaimId(), message.getTargetUuid()));
            case TRANSFER -> require(player, "cobbleclub.claims.transfer", () -> transfer(player, message.getClaimId(), message.getName()));
            case SET_MESSAGES -> require(player, "cobbleclub.claims.messages", () -> messages(player, message.getClaimId(), message.getMessages()));
            case TELEPORT -> require(player, "cobbleclub.claims.teleport", () -> teleport(player, message.getClaimId()));
            case RENAME_SUB -> require(player, "cobbleclub.claims.subclaim", () -> renameSub(player, message.getClaimId(), message.getSubId(), message.getName()));
            case DELETE_SUB -> require(player, "cobbleclub.claims.subclaim", () -> deleteSub(player, message.getClaimId(), message.getSubId()));
            case BUY_BLOCKS -> require(player, "cobbleclub.claims.buyblocks", () -> buyBlocks(player));
            case SCREEN_CLOSED -> Result.ok(false, "");
        };
    }

    private static Result require(ServerPlayerEntity player, String permission, java.util.function.Supplier<Result> action) {
        if (!PermissionService.has(player, permission, true)) {
            return Result.error("You do not have permission: " + permission);
        }
        return action.get();
    }

    private static Result create(ServerPlayerEntity player, ChunkRect rect) {
        if (!CobbleClubServer.config().claimsEnabled) return Result.error("Claims are disabled on this server.");
        if (!valid(rect)) return Result.error("Select a valid chunk area.");
        int width = rect.getMaxCx() - rect.getMinCx() + 1;
        int depth = rect.getMaxCz() - rect.getMinCz() + 1;
        if (width > CobbleClubServer.config().maxClaimChunksPerSide || depth > CobbleClubServer.config().maxClaimChunksPerSide) {
            return Result.error("That claim is too large.");
        }
        if (!nearPlayer(player, rect)) return Result.error("That area is too far away from you.");
        String dimension = dimension(player.getServerWorld());
        if (overlaps(dimension, rect, null)) return Result.error("That area overlaps another claim.");
        int area = width * depth * 256;
        if (budget(player).getRemaining() < area) return Result.error("You do not have enough claim blocks.");

        ClaimsStore.ClaimData claim = new ClaimsStore.ClaimData();
        claim.id = UUID.randomUUID().toString();
        claim.ownerUuid = player.getUuidAsString();
        claim.ownerName = player.getGameProfile().getName();
        claim.name = "Claim " + (owned(player).size() + 1);
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
        ClaimsStore.ClaimData claim = manageable(player, id);
        if (claim == null || !valid(rect)) return Result.error("That claim cannot be resized.");
        int width = rect.getMaxCx() - rect.getMinCx() + 1;
        int depth = rect.getMaxCz() - rect.getMinCz() + 1;
        if (width > CobbleClubServer.config().maxClaimChunksPerSide || depth > CobbleClubServer.config().maxClaimChunksPerSide) return Result.error("That claim is too large.");
        if (!nearPlayer(player, rect)) return Result.error("That area is too far away from you.");
        if (overlaps(claim.dimension, rect, claim.id)) return Result.error("That area overlaps another claim.");
        int extra = width * depth * 256 - claim.area();
        if (extra > budget(player).getRemaining()) return Result.error("You do not have enough claim blocks.");
        claim.minCx = rect.getMinCx();
        claim.minCz = rect.getMinCz();
        claim.maxCx = rect.getMaxCx();
        claim.maxCz = rect.getMaxCz();
        return Result.ok(true, "Claim resized.");
    }

    private static Result delete(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        if (claim == null) return Result.error("You cannot delete that claim.");
        ClaimsStore.all().remove(claim);
        return Result.ok(true, "Claim deleted.");
    }

    private static Result rename(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        if (claim == null || name == null || !name.matches("[A-Za-z0-9 _-]{3,32}")) return Result.error("Use 3-32 letters, numbers, spaces, _ or -.");
        claim.name = name;
        return Result.ok(true, "Claim renamed.");
    }

    private static Result permission(ServerPlayerEntity player, ClaimsActionMsg message) {
        ClaimsStore.ClaimData claim = manageable(player, message.getClaimId());
        if (claim == null || !claimPermissionIds().contains(message.getPermission())) return Result.error("That permission cannot be changed.");
        Map<String, String> permissions = claim.permissions;
        if (message.getSubId() != null) {
            ClaimsStore.SubClaimData subClaim = findSub(claim, message.getSubId());
            if (subClaim == null) return Result.error("That subclaim does not exist.");
            permissions = subClaim.permissionOverrides;
        }
        String role = message.getRole();
        if (role == null) permissions.remove(message.getPermission());
        else if (List.of("VISITOR", "TRUSTED", "OWNER").contains(role.toUpperCase(Locale.ROOT))) permissions.put(message.getPermission(), role.toUpperCase(Locale.ROOT));
        else return Result.error("Invalid permission role.");
        return Result.ok(true, "Permission updated.");
    }

    private static Result trust(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        ServerPlayerEntity target = online(player.getServer(), name);
        if (claim == null || target == null) return Result.error("That player must be online.");
        if (target.getUuidAsString().equals(claim.ownerUuid)) return Result.error("The owner is already trusted.");
        claim.banned.remove(target.getUuidAsString());
        claim.trusted.put(target.getUuidAsString(), new ClaimsStore.MemberData(target.getUuidAsString(), target.getGameProfile().getName(), System.currentTimeMillis()));
        return Result.ok(true, "Player trusted.");
    }

    private static Result untrust(ServerPlayerEntity player, String id, String uuid) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        if (claim == null || uuid == null || claim.trusted.remove(uuid) == null) return Result.error("That player is not trusted.");
        return Result.ok(true, "Trust removed.");
    }

    private static Result ban(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        ServerPlayerEntity target = online(player.getServer(), name);
        if (claim == null || target == null || target.getUuidAsString().equals(claim.ownerUuid)) return Result.error("That player cannot be banned.");
        claim.trusted.remove(target.getUuidAsString());
        claim.banned.put(target.getUuidAsString(), new ClaimsStore.MemberData(target.getUuidAsString(), target.getGameProfile().getName(), System.currentTimeMillis()));
        return Result.ok(true, "Player banned from the claim.");
    }

    private static Result unban(ServerPlayerEntity player, String id, String uuid) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        if (claim == null || uuid == null || claim.banned.remove(uuid) == null) return Result.error("That player is not banned.");
        return Result.ok(true, "Ban removed.");
    }

    private static Result transfer(ServerPlayerEntity player, String id, String name) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        ServerPlayerEntity target = online(player.getServer(), name);
        if (claim == null || target == null || target.getUuid().equals(player.getUuid())) return Result.error("That claim cannot be transferred.");
        claim.ownerUuid = target.getUuidAsString();
        claim.ownerName = target.getGameProfile().getName();
        claim.trusted.remove(target.getUuidAsString());
        claim.trusted.put(player.getUuidAsString(), new ClaimsStore.MemberData(player.getUuidAsString(), player.getGameProfile().getName(), System.currentTimeMillis()));
        return Result.ok(true, "Claim transferred.");
    }

    private static Result messages(ServerPlayerEntity player, String id, ClaimMessagesEdit edit) {
        ClaimsStore.ClaimData claim = manageable(player, id);
        if (claim == null || edit == null) return Result.error("Claim messages could not be updated.");
        claim.enterTitle = limit(edit.getEnterTitle(), 64);
        claim.enterSubtitle = limit(edit.getEnterSubtitle(), 64);
        claim.leaveTitle = limit(edit.getLeaveTitle(), 64);
        claim.leaveSubtitle = limit(edit.getLeaveSubtitle(), 64);
        return Result.ok(true, "Claim messages updated.");
    }

    private static Result teleport(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = visible(player, id);
        if (claim == null) return Result.error("You cannot teleport to that claim.");
        ServerWorld world = world(player.getServer(), claim.dimension);
        if (world == null) return Result.error("That world is unavailable.");

        BlockPos safe = findSafeClaimTeleport(world, claim);
        if (safe == null) {
            return Result.error("No safe surface location was found inside that claim.");
        }

        player.teleport(world, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, player.getYaw(), player.getPitch());
        return Result.ok(false, "Teleported safely to the claim surface.");
    }

    /**
     * Finds a safe surface destination inside the claim. This is intentionally
     * claim-teleport-only; /wild and /rtp use their own already-approved logic.
     *
     * We search from the claim centre outward and always use the world's highest
     * motion-blocking non-leaf surface for each column. A destination is accepted
     * only when the block beneath the player's feet is solid/dry/non-hazardous and
     * there are two blocks of clear headroom.
     */
    private static BlockPos findSafeClaimTeleport(ServerWorld world, ClaimsStore.ClaimData claim) {
        int minX = claim.minCx << 4;
        int maxX = (claim.maxCx << 4) + 15;
        int minZ = claim.minCz << 4;
        int maxZ = (claim.maxCz << 4) + 15;
        int centerX = minX + ((maxX - minX) / 2);
        int centerZ = minZ + ((maxZ - minZ) / 2);

        // Prefer the centre of the claim, then expand in square rings. This keeps
        // teleports predictable while still recovering from roofs, pools, cactus,
        // fire, magma, powder snow, campfires, berry bushes, etc.
        int maxRadius = Math.max(maxX - minX, maxZ - minZ);
        for (int radius = 0; radius <= maxRadius; radius++) {
            int left = Math.max(minX, centerX - radius);
            int right = Math.min(maxX, centerX + radius);
            int top = Math.max(minZ, centerZ - radius);
            int bottom = Math.min(maxZ, centerZ + radius);

            if (radius == 0) {
                BlockPos safe = safeSurfaceAt(world, centerX, centerZ);
                if (safe != null) return safe;
                continue;
            }

            for (int x = left; x <= right; x++) {
                BlockPos safe = safeSurfaceAt(world, x, top);
                if (safe != null) return safe;
                if (bottom != top) {
                    safe = safeSurfaceAt(world, x, bottom);
                    if (safe != null) return safe;
                }
            }
            for (int z = top + 1; z < bottom; z++) {
                BlockPos safe = safeSurfaceAt(world, left, z);
                if (safe != null) return safe;
                if (right != left) {
                    safe = safeSurfaceAt(world, right, z);
                    if (safe != null) return safe;
                }
            }
        }
        return null;
    }

    private static BlockPos safeSurfaceAt(ServerWorld world, int x, int z) {
        // Loading the target chunk first avoids stale/empty height data when a
        // claim is in an area that has not been visited recently.
        world.getChunk(x >> 4, z >> 4);
        int feetY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, feetY, z);
        BlockPos head = feet.up();
        BlockPos ground = feet.down();

        BlockState groundState = world.getBlockState(ground);
        BlockState feetState = world.getBlockState(feet);
        BlockState headState = world.getBlockState(head);

        if (groundState.isAir() || !groundState.getFluidState().isEmpty() || claimTeleportHazard(groundState)) return null;
        if (!feetState.isAir() || !headState.isAir()) return null;
        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()) return null;
        if (claimTeleportHazard(feetState) || claimTeleportHazard(headState)) return null;
        return feet;
    }

    private static boolean claimTeleportHazard(BlockState state) {
        return state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.WATER)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.CAMPFIRE)
                || state.isOf(Blocks.SOUL_CAMPFIRE)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.WITHER_ROSE)
                || state.isOf(Blocks.POWDER_SNOW)
                || state.isOf(Blocks.POINTED_DRIPSTONE);
    }

    private static Result renameSub(ServerPlayerEntity player, String claimId, String subId, String name) {
        ClaimsStore.ClaimData claim = manageable(player, claimId);
        ClaimsStore.SubClaimData subClaim = claim == null ? null : findSub(claim, subId);
        if (subClaim == null || name == null || !name.matches("[A-Za-z0-9 _-]{3,32}")) {
            return Result.error("Use 3-32 letters, numbers, spaces, _ or -.");
        }
        subClaim.name = name;
        return Result.ok(true, "Subclaim renamed.");
    }

    private static Result deleteSub(ServerPlayerEntity player, String claimId, String subId) {
        ClaimsStore.ClaimData claim = manageable(player, claimId);
        ClaimsStore.SubClaimData subClaim = claim == null ? null : findSub(claim, subId);
        if (subClaim == null) return Result.error("That subclaim does not exist.");
        claim.subClaims.remove(subClaim);
        return Result.ok(true, "Subclaim deleted.");
    }

    private static Result buyBlocks(ServerPlayerEntity player) {
        ServerConfig config = CobbleClubServer.config();
        if (!config.economyEnabled || config.claimBlockPurchaseAmount <= 0 || config.claimBlockPurchasePrice <= 0) {
            return Result.error("Buying claim blocks is disabled.");
        }
        if (!EconomyService.withdrawGems(player, config.claimBlockPurchasePrice)) {
            return Result.error("You need " + EconomyService.formatGems(config.claimBlockPurchasePrice) + ".");
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.bonusClaimBlocks = safeAdd(data.bonusClaimBlocks, config.claimBlockPurchaseAmount);
        data.revision++;
        PlayerDataStore.save();
        return Result.ok(true, "Purchased " + config.claimBlockPurchaseAmount + " claim blocks.");
    }

    public static String purchaseBlocks(ServerPlayerEntity player) {
        Result result = buyBlocks(player);
        if (result.ok) {
            revision++;
            sendWorld(player);
        }
        return result.message;
    }

    /** OP-only command target: deletes the complete persisted claim under the operator. */
    public static String adminDeleteClaimHere(ServerPlayerEntity operator) {
        ClaimsStore.ClaimData claim = claimAt(operator.getServerWorld(), operator.getBlockPos());
        if (claim == null) return "There is no CobbleClub claim at your position.";
        String description = claim.name + " (owner: " + claim.ownerName + ", id: " + claim.id + ")";
        ClaimsStore.all().remove(claim);
        ClaimsStore.save();
        revision++;
        if (operator.getServer() != null) {
            for (ServerPlayerEntity player : operator.getServer().getPlayerManager().getPlayerList()) sendWorld(player);
        }
        return "Deleted " + description + " including its subclaims, trust list, bans and permissions.";
    }

    public static boolean createSubclaim(ServerPlayerEntity player, String name, BlockPos first, BlockPos second) {
        if (!PermissionService.has(player, "cobbleclub.claims.subclaim", true)) return false;
        if (name == null || !name.matches("[A-Za-z0-9_-]{3,24}") || first == null || second == null) return false;
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());
        ClaimsStore.ClaimData claim = claimAt(player.getServerWorld(), first);
        if (claim == null || manageable(player, claim.id) == null || claimAt(player.getServerWorld(), second) != claim) return false;
        for (ClaimsStore.SubClaimData existing : claim.subClaims) {
            if (minX <= existing.maxX && maxX >= existing.minX && minY <= existing.maxY && maxY >= existing.minY
                    && minZ <= existing.maxZ && maxZ >= existing.minZ) return false;
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
        if (!ServerPlayNetworking.canSend(player, Payloads.ClaimsState.ID)) return;
        ClaimsStateMsg state = new ClaimsStateMsg(
                1,
                Math.max(1, revision),
                budget(player),
                visibleDetails(player),
                mapClaims(player),
                new ActionFeedback(nonce, result.ok, textJson(result.message, result.ok ? "green" : "red"))
        );
        ServerPlayNetworking.send(player, new Payloads.ClaimsState(ClaimsScreenProtocol.INSTANCE.encode(state)));
    }

    /** Keep claim borders available without requiring the claims GUI to be open. */
    public static void syncWorldSnapshots(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String dim = dimension(player.getServerWorld());
            WorldSnapshotState state = WORLD_SNAPSHOT_STATE.get(player.getUuid());
            if (state == null || state.revision != revision || !state.dimension.equals(dim)) {
                sendWorld(player);
            }
        }
    }

    public static void forgetWorldSnapshot(ServerPlayerEntity player) {
        if (player != null) WORLD_SNAPSHOT_STATE.remove(player.getUuid());
    }

    private record WorldSnapshotState(String dimension, int revision) {}

    private static void sendWorld(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, Payloads.ClaimsWorld.ID)) return;
        String dimension = dimension(player.getServerWorld());
        List<WorldBoxEntry> boxes = ClaimsStore.all().stream()
                .filter(claim -> dimension.equals(claim.dimension))
                .map(claim -> new WorldBoxEntry(
                        claim.ownerUuid.equals(player.getUuidAsString()) ? WorldBoxType.MAIN : WorldBoxType.OTHER,
                        box(claim),
                        null
                ))
                .toList();
        ClaimsWorldMsg message = new ClaimsWorldMsg(1, dimension, boxes);
        ServerPlayNetworking.send(player, new Payloads.ClaimsWorld(ClaimsScreenProtocol.INSTANCE.encode(message)));
        WORLD_SNAPSHOT_STATE.put(player.getUuid(), new WorldSnapshotState(dimension, revision));
    }

    private static List<ClaimDetailEntry> visibleDetails(ServerPlayerEntity player) {
        List<ClaimDetailEntry> result = new ArrayList<>();
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            boolean owner = claim.ownerUuid.equals(player.getUuidAsString());
            if (!owner && !claim.trusted.containsKey(player.getUuidAsString()) && !isBypass(player)) continue;
            result.add(detail(player, claim, owner));
        }
        result.sort(Comparator.comparing(ClaimDetailEntry::getCreatedAt));
        return result;
    }

    private static ClaimDetailEntry detail(ServerPlayerEntity player, ClaimsStore.ClaimData claim, boolean owner) {
        List<MemberEntry> members = new ArrayList<>();
        members.add(new MemberEntry(claim.ownerUuid, claim.ownerName, online(player.getServer(), claim.ownerUuid) != null, claim.createdAt, true));
        for (ClaimsStore.MemberData member : claim.trusted.values()) {
            members.add(new MemberEntry(member.uuid, member.name, online(player.getServer(), member.uuid) != null, member.joinedAt, false));
        }
        List<MemberEntry> banned = claim.banned.values().stream()
                .map(member -> new MemberEntry(member.uuid, member.name, online(player.getServer(), member.uuid) != null, member.joinedAt, false))
                .toList();
        List<PermissionStateEntry> permissions = claim.permissions.entrySet().stream()
                .map(entry -> new PermissionStateEntry(entry.getKey(), entry.getValue()))
                .toList();
        List<SubClaimEntry> subClaims = claim.subClaims.stream().map(subClaim -> new SubClaimEntry(
                subClaim.id,
                subClaim.name,
                new BoxInfo(subClaim.minX, subClaim.minY, subClaim.minZ, subClaim.maxX, subClaim.maxY, subClaim.maxZ),
                subClaim.permissionOverrides.entrySet().stream()
                        .map(entry -> new PermissionStateEntry(entry.getKey(), entry.getValue()))
                        .toList()
        )).toList();
        return new ClaimDetailEntry(
                claim.id, claim.name, claim.ownerName, claim.dimension, claim.dimension,
                claim.dimension.equals(dimension(player.getServerWorld())), box(claim), false, claim.area(), claim.createdAt,
                members, banned, subClaims, permissions,
                claim.enterTitle, claim.enterSubtitle, claim.leaveTitle, claim.leaveSubtitle,
                owner, owner || isBypass(player), owner || isBypass(player), owner || isBypass(player)
        );
    }

    private static List<MapClaimEntry> mapClaims(ServerPlayerEntity player) {
        String dimension = dimension(player.getServerWorld());
        List<MapClaimEntry> result = new ArrayList<>();
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (!dimension.equals(claim.dimension)) continue;
            String relation = claim.ownerUuid.equals(player.getUuidAsString()) ? "OWN" : claim.trusted.containsKey(player.getUuidAsString()) ? "TRUSTED" : "OTHER";
            result.add(new MapClaimEntry(claim.id, claim.name, claim.ownerName, claim.minCx << 4, claim.minCz << 4, (claim.maxCx << 4) + 15, (claim.maxCz << 4) + 15, relation));
        }
        return result;
    }

    private static List<PermissionCatalogEntry> permissionCatalog() {
        List<String> roles = List.of("VISITOR", "TRUSTED", "OWNER");
        return List.of(
                new PermissionCatalogEntry("build", textJson("Build & Break", "white"), textJson("Place and break blocks", "gray"), "TRUSTED", roles, "minecraft:diamond_pickaxe"),
                new PermissionCatalogEntry("interact", textJson("Use Blocks", "white"), textJson("Doors, buttons, beds, redstone and other blocks", "gray"), "TRUSTED", roles, "minecraft:oak_door"),
                new PermissionCatalogEntry("containers", textJson("Containers", "white"), textJson("Chests, barrels, hoppers and storage", "gray"), "TRUSTED", roles, "minecraft:chest"),
                new PermissionCatalogEntry("item_use", textJson("Use Items", "white"), textJson("Buckets, placeable items and right-click item use", "gray"), "TRUSTED", roles, "minecraft:water_bucket"),
                new PermissionCatalogEntry("entities", textJson("Entities", "white"), textJson("Interact with or attack mobs, armor stands and vehicles", "gray"), "TRUSTED", roles, "minecraft:saddle"),
                new PermissionCatalogEntry("pvp", textJson("PvP", "white"), textJson("Attack players inside this claim", "gray"), "OWNER", roles, "minecraft:diamond_sword")
        );
    }


    private static List<String> claimPermissionIds() {
        return List.of("build", "interact", "containers", "item_use", "entities", "pvp");
    }
    public static BudgetInfo budget(ServerPlayerEntity player) {
        int used = owned(player).stream().mapToInt(ClaimsStore.ClaimData::area).sum();
        int total = safeAdd(Math.max(0, CobbleClubServer.config().initialClaimBlocks), EconomyService.data(player).bonusClaimBlocks);
        return new BudgetInfo(total, Math.max(0, total - used));
    }

    private static List<ClaimsStore.ClaimData> owned(ServerPlayerEntity player) {
        return ClaimsStore.all().stream().filter(claim -> claim.ownerUuid.equals(player.getUuidAsString())).toList();
    }

    private static ClaimsStore.ClaimData manageable(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = find(id);
        return claim != null && (claim.ownerUuid.equals(player.getUuidAsString()) || isBypass(player)) ? claim : null;
    }

    private static ClaimsStore.ClaimData visible(ServerPlayerEntity player, String id) {
        ClaimsStore.ClaimData claim = find(id);
        return claim != null && (claim.ownerUuid.equals(player.getUuidAsString()) || claim.trusted.containsKey(player.getUuidAsString()) || isBypass(player)) ? claim : null;
    }

    private static ClaimsStore.ClaimData find(String id) {
        return ClaimsStore.findById(id);
    }

    private static boolean overlaps(String dimension, ChunkRect rect, String exceptId) {
        for (ClaimsStore.ClaimData claim : ClaimsStore.all()) {
            if (!dimension.equals(claim.dimension) || claim.id.equals(exceptId)) continue;
            if (rect.getMinCx() <= claim.maxCx && rect.getMaxCx() >= claim.minCx && rect.getMinCz() <= claim.maxCz && rect.getMaxCz() >= claim.minCz) return true;
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
        if (claim == null || id == null) return null;
        for (ClaimsStore.SubClaimData subClaim : claim.subClaims) if (id.equals(subClaim.id)) return subClaim;
        return null;
    }

    private static BoxInfo box(ClaimsStore.ClaimData claim) {
        return new BoxInfo(claim.minCx << 4, -64, claim.minCz << 4, (claim.maxCx << 4) + 15, 320, (claim.maxCz << 4) + 15);
    }

    private static int roleRank(ClaimsStore.ClaimData claim, ServerPlayerEntity player) {
        if (claim.ownerUuid.equals(player.getUuidAsString())) return 2;
        if (claim.trusted.containsKey(player.getUuidAsString())) return 1;
        return 0;
    }

    private static String defaultRole(String permission) {
        return "pvp".equals(permission) ? "OWNER" : "TRUSTED";
    }

    private static int roleRank(String role) {
        if (role == null) return 1;
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
        if (server == null || dimension == null) return null;
        for (ServerWorld world : server.getWorlds()) if (dimension.equals(dimension(world))) return world;
        return null;
    }

    private static ServerPlayerEntity online(MinecraftServer server, String nameOrUuid) {
        if (server == null || nameOrUuid == null) return null;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (nameOrUuid.equalsIgnoreCase(player.getGameProfile().getName()) || nameOrUuid.equals(player.getUuidAsString())) return player;
        }
        return null;
    }

    private static String encodeTile(ServerWorld world, int cx, int cz) {
        byte[] bytes = new byte[256];
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int blockX = (cx << 4) + x;
                int blockZ = (cz << 4) + z;
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, blockX, blockZ) - 1;
                pos.set(blockX, y, blockZ);
                MapColor color = world.getBlockState(pos).getMapColor(world, pos);
                bytes[x + z * 16] = color == MapColor.CLEAR ? 0 : color.getRenderColorByte(MapColor.Brightness.NORMAL);
            }
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String textJson(String text, String color) {
        String escaped = (text == null ? "" : text).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return "{\"text\":\"" + escaped + "\",\"color\":\"" + color + "\"}";
    }

    private static int safeAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) return Integer.MAX_VALUE;
        return Math.max(0, left + right);
    }

    private record Result(boolean ok, boolean changed, String message) {
        static Result ok(boolean changed, String message) { return new Result(true, changed, message); }
        static Result error(String message) { return new Result(false, false, message); }
    }
}
