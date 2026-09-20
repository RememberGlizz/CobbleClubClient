package com.cobbleclub.server.chestshop;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.chestshop.ChestShopPayloads;
import com.cobbleclub.server.chestshop.ChestShopStore;
import com.cobbleclub.server.service.ActivityEconomyService;
import com.cobbleclub.server.service.ClaimsService;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.PermissionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.WallSignBlock;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.ChestType;
import net.minecraft.state.property.Property;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.BlockView;
import net.minecraft.block.entity.SignText;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

public final class ChestShopService {
    private static final Map<UUID, PendingSetup> PENDING = new HashMap<>();
    private static final Map<UUID, Long> PLACEMENT = new HashMap<>();
    private static int refreshTicks;
    private static final long MAX_UNIT_PRICE = 1000000000L;

    private ChestShopService() {
    }

    public static void load(MinecraftServer server) {
        ChestShopStore.load(server);
        PENDING.clear();
        PLACEMENT.clear();
        ChestShopService.refreshAll(server);
    }

    public static void shutdown() {
        ChestShopStore.save();
        PENDING.clear();
        PLACEMENT.clear();
    }

    public static void arm(ServerPlayerEntity player) {
        ChestShopService.cancelSetup(player, true);
        PLACEMENT.put(player.getUuid(), System.currentTimeMillis());
        String msg = "Right click the side of a chest to place your shop sign";
        ChestShopService.send(player, new ChestShopPayloads.Placement(true, msg));
    }

    public static void cancel(ServerPlayerEntity player) {
        boolean wasActive = PLACEMENT.remove(player.getUuid()) != null;
        boolean hadPending = PENDING.containsKey(player.getUuid());
        ChestShopService.cancelSetup(player, true);
        if (wasActive || hadPending) {
            ChestShopService.send(player, new ChestShopPayloads.Placement(false, "Chest shop setup cancelled"));
            player.sendMessage(Text.literal("Chest shop setup cancelled."), false);
        } else {
            player.sendMessage(Text.literal("You do not have a chest shop setup in progress."), false);
        }
    }

    public static void disconnect(ServerPlayerEntity player) {
        PLACEMENT.remove(player.getUuid());
        ChestShopService.cancelSetup(player, true);
    }

    public static boolean isPlacementActive(ServerPlayerEntity player) {
        return PLACEMENT.containsKey(player.getUuid());
    }

    public static ActionResult handleUse(ServerPlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        ServerWorld world = player.getServerWorld();
        BlockPos pos = hit.getBlockPos();
        if (ChestShopService.isPlacementActive(player)) {
            return ChestShopService.placeProvisionalShop(player, world, hit);
        }
        ChestShopStore.ShopData shop = ChestShopService.findShopAt(world, pos);
        if (shop == null) {
            return ActionResult.PASS;
        }
        boolean sign = shop.signPos().equals(pos);
        boolean manager = ChestShopService.canManage(player, world, shop);
        if (!manager && !ClaimsService.canBuyChestShop(player, shop.chestPos())) {
            player.sendMessage(Text.literal("You cannot use chest shops in this claim."), true);
            return ActionResult.FAIL;
        }
        if (manager && !sign && !player.isSneaking()) {
            return ActionResult.PASS;
        }
        ChestShopService.openShop(player, shop, manager, "", false);
        return ActionResult.SUCCESS;
    }

    private static ActionResult placeProvisionalShop(ServerPlayerEntity player, ServerWorld world, BlockHitResult hit) {
        BlockPos chestPos = hit.getBlockPos();
        BlockState chestState = world.getBlockState(chestPos);
        if (!(chestState.getBlock() instanceof ChestBlock)) {
            player.sendMessage(Text.literal("Chest shops can only be placed on a chest."), true);
            return ActionResult.FAIL;
        }
        if (hit.getSide() == Direction.UP || hit.getSide() == Direction.DOWN) {
            player.sendMessage(Text.literal("Aim at a side of the chest so the shop sign has room."), true);
            return ActionResult.FAIL;
        }
        if (ClaimsService.claimAt(world, chestPos) == null) {
            player.sendMessage(Text.literal("Chest shops must be inside one of your CobbleClub claims."), true);
            return ActionResult.FAIL;
        }
        if (!ClaimsService.canManageChestShop(player, chestPos)) {
            player.sendMessage(Text.literal("Your claim role does not allow Chest Shops here."), true);
            return ActionResult.FAIL;
        }
        if (ChestShopService.findShopAt(world, chestPos) != null) {
            player.sendMessage(Text.literal("That chest is already part of a player shop."), true);
            return ActionResult.FAIL;
        }
        BlockPos signPos = chestPos.offset(hit.getSide());
        if (!world.getBlockState(signPos).isAir()) {
            player.sendMessage(Text.literal("There is not enough room for the shop sign on that side."), true);
            return ActionResult.FAIL;
        }
        BlockState signState = Blocks.DARK_OAK_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, hit.getSide());
        if (!signState.canPlaceAt(world, signPos)) {
            player.sendMessage(Text.literal("The shop sign cannot be safely placed there."), true);
            return ActionResult.FAIL;
        }
        world.setBlockState(signPos, signState, 3);
        ChestShopService.writeSetupSign(world, signPos);
        PLACEMENT.remove(player.getUuid());
        PendingSetup pending = new PendingSetup(chestPos.toImmutable(), signPos.toImmutable(), null, ItemStack.EMPTY);
        PENDING.put(player.getUuid(), pending);
        ChestShopService.send(player, new ChestShopPayloads.Placement(false, "Choose the item for this chest shop"));
        ChestShopService.send(player, new ChestShopPayloads.SelectOpen("Choose the item to sell"));
        return ActionResult.SUCCESS;
    }

    public static void selectInventoryItem(ServerPlayerEntity player, int slot) {
        ChestShopStore.ShopData shop;
        PendingSetup pending = PENDING.get(player.getUuid());
        if (pending == null) {
            return;
        }
        if (slot < 0 || slot >= player.getInventory().size()) {
            return;
        }
        ItemStack selected = player.getInventory().getStack(slot);
        if (selected == null || selected.isEmpty()) {
            player.sendMessage(Text.literal("Choose a non-empty inventory slot."), true);
            return;
        }
        pending.selected = selected = selected.copyWithCount(1);
        long minimum = ChestShopService.minimumPrice(selected);
        long current = 0L;
        if (pending.existingShopId != null && (shop = ChestShopStore.get(pending.existingShopId)) != null) {
            current = shop.unitPrice;
        }
        long recommended = Math.max(minimum, current > 0L ? current : ChestShopService.safeMultiply(minimum, 2L));
        recommended = Math.min(1000000000L, Math.max(10L, recommended));
        ChestShopService.send(player, new ChestShopPayloads.PriceOpen(selected, minimum, recommended, CobbleClubServer.config().currencySymbol, pending.existingShopId == null ? "Set price per item" : "Update shop price"));
    }

    public static void setPendingPrice(ServerPlayerEntity player, long requestedPrice) {
        ChestShopStore.ShopData shop;
        PendingSetup pending = PENDING.get(player.getUuid());
        if (pending == null || pending.selected == null || pending.selected.isEmpty()) {
            return;
        }
        long minimum = ChestShopService.minimumPrice(pending.selected);
        if (requestedPrice < minimum) {
            ChestShopService.send(player, new ChestShopPayloads.PriceOpen(pending.selected, minimum, Math.max(minimum, requestedPrice), CobbleClubServer.config().currencySymbol, "Price must be at least " + EconomyService.format(minimum)));
            return;
        }
        if (requestedPrice > 1000000000L) {
            player.sendMessage(Text.literal("Chest shop prices cannot exceed " + EconomyService.format(1000000000L) + "."), true);
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerWorld world;
        if (pending.existingShopId == null) {
            world = player.getServerWorld();
            if (!(world.getBlockState(pending.chestPos).getBlock() instanceof ChestBlock && world.getBlockState(pending.signPos).isOf(Blocks.DARK_OAK_WALL_SIGN) && ClaimsService.canManageChestShop(player, pending.chestPos))) {
                ChestShopService.cancelSetup(player, true);
                player.sendMessage(Text.literal("The chest shop setup is no longer valid."), false);
                return;
            }
            shop = ChestShopStore.create(player.getUuid(), player.getGameProfile().getName(), world, pending.chestPos, pending.signPos);
        } else {
            shop = ChestShopStore.get(pending.existingShopId);
            if (shop == null) {
                PENDING.remove(player.getUuid());
                return;
            }
            world = ChestShopStore.world(server, shop);
            if (world == null || !ChestShopService.canManage(player, world, shop)) {
                PENDING.remove(player.getUuid());
                player.sendMessage(Text.literal("You no longer have permission to manage that shop."), false);
                return;
            }
        }
        ChestShopStore.setTemplate(server, shop, pending.selected);
        shop.unitPrice = requestedPrice;
        ChestShopStore.put(shop);
        PENDING.remove(player.getUuid());
        ChestShopService.updateSign(server, shop);
        String message = "Chest shop ready: " + pending.selected.getName().getString() + " for " + EconomyService.format(requestedPrice) + " each.";
        ChestShopService.send(player, new ChestShopPayloads.Close(message, true));
        player.sendMessage(Text.literal("\u2713 " + message), false);
    }

    public static void handleShopAction(ServerPlayerEntity player, String shopId, String action, int quantity) {
        String normalized;
        ChestShopStore.ShopData shop = ChestShopStore.get(shopId);
        if (shop == null) {
            ChestShopService.send(player, new ChestShopPayloads.Close("That chest shop no longer exists.", false));
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerWorld world = ChestShopStore.world(server, shop);
        if (world == null) {
            return;
        }
        boolean manager = ChestShopService.canManage(player, world, shop);
        switch (normalized = action == null ? "" : action.trim().toLowerCase()) {
            case "buy": {
                ChestShopService.buy(player, shop, Math.max(1, quantity));
                break;
            }
            case "refresh": {
                ChestShopService.openShop(player, shop, manager, "", false);
                break;
            }
            case "edit_item": {
                if (!manager) {
                    return;
                }
                PENDING.put(player.getUuid(), new PendingSetup(shop.chestPos(), shop.signPos(), shop.id, ItemStack.EMPTY));
                ChestShopService.send(player, new ChestShopPayloads.SelectOpen("Choose the new shop item"));
                break;
            }
            case "edit_price": {
                if (!manager) {
                    return;
                }
                ItemStack template = ChestShopStore.template(server, shop);
                if (template.isEmpty()) {
                    return;
                }
                PENDING.put(player.getUuid(), new PendingSetup(shop.chestPos(), shop.signPos(), shop.id, template));
                long minimum = ChestShopService.minimumPrice(template);
                ChestShopService.send(player, new ChestShopPayloads.PriceOpen(template, minimum, Math.max(minimum, shop.unitPrice), CobbleClubServer.config().currencySymbol, "Update shop price"));
                break;
            }
            case "remove": {
                if (!manager) {
                    return;
                }
                ChestShopService.removeShop(server, shop, true);
                ChestShopService.send(player, new ChestShopPayloads.Close("Chest shop removed. The chest and its stock were left intact.", true));
                player.sendMessage(Text.literal("Chest shop removed. The chest and its stock were left intact."), false);
            }
        }
    }

    private static void buy(ServerPlayerEntity buyer, ChestShopStore.ShopData shop, int requestedQuantity) {
        MinecraftServer server = buyer.getServer();
        if (server == null) {
            return;
        }
        if (buyer.getUuidAsString().equals(shop.ownerUuid)) {
            ChestShopService.openShop(buyer, shop, ChestShopService.canManage(buyer, ChestShopStore.world(server, shop), shop), "You cannot buy from your own shop.", true);
            return;
        }
        ServerWorld world = ChestShopStore.world(server, shop);
        if (world == null) {
            return;
        }
        if (!ClaimsService.canBuyChestShop(buyer, shop.chestPos())) {
            ChestShopService.openShop(buyer, shop, false, "You cannot buy from shops in this claim.", true);
            return;
        }
        if (!ChestShopService.validShopLocation(world, shop)) {
            ChestShopService.removeShop(server, shop, true);
            ChestShopService.send(buyer, new ChestShopPayloads.Close("That chest shop is no longer valid.", false));
            return;
        }
        ItemStack template = ChestShopStore.template(server, shop);
        Inventory inventory = ChestShopService.inventory(world, shop.chestPos());
        if (template.isEmpty() || inventory == null) {
            ChestShopService.openShop(buyer, shop, false, "This shop is unavailable.", true);
            return;
        }
        long floor = ChestShopService.minimumPrice(template);
        if (shop.unitPrice < floor) {
            shop.unitPrice = floor;
            ChestShopStore.put(shop);
            ChestShopService.updateSign(server, shop);
        }
        int stock = ChestShopService.count(inventory, template);
        int quantity = Math.max(1, Math.min(requestedQuantity, stock));
        if (stock <= 0) {
            ChestShopService.openShop(buyer, shop, false, "This shop is sold out.", true);
            return;
        }
        long total = ChestShopService.safeMultiply(shop.unitPrice, quantity);
        if (total <= 0L || total == Long.MAX_VALUE) {
            ChestShopService.openShop(buyer, shop, false, "That purchase is too large.", true);
            return;
        }
        if (!EconomyService.withdraw(buyer, total)) {
            ChestShopService.openShop(buyer, shop, false, "You need " + EconomyService.format(total) + " for that purchase.", true);
            return;
        }
        int removed = ChestShopService.removeExact(inventory, template, quantity);
        if (removed != quantity) {
            if (removed > 0) {
                ChestShopService.restore(inventory, template, removed);
            }
            EconomyService.deposit(buyer, total);
            ChestShopService.openShop(buyer, shop, false, "Stock changed before the purchase finished. Your money was refunded.", true);
            return;
        }
        EconomyService.deposit(shop.ownerUuid(), shop.ownerName, total);
        ChestShopService.give(buyer, template, quantity);
        inventory.markDirty();
        ChestShopService.updateSign(server, shop);
        ChestShopService.openShop(buyer, shop, false, "Purchased x" + quantity + " for " + EconomyService.format(total) + ".", false);
        buyer.sendMessage(Text.literal("\u2713 Bought x" + quantity + " " + template.getName().getString() + " for " + EconomyService.format(total) + "."), false);
    }

    public static void openShop(ServerPlayerEntity player, ChestShopStore.ShopData shop, boolean manager, String notice, boolean error) {
        MinecraftServer server = player.getServer();
        if (server == null || shop == null) {
            return;
        }
        ServerWorld world = ChestShopStore.world(server, shop);
        if (world == null || !ChestShopService.validShopLocation(world, shop)) {
            ChestShopService.removeShop(server, shop, true);
            ChestShopService.send(player, new ChestShopPayloads.Close("That chest shop is no longer valid.", false));
            return;
        }
        ItemStack template = ChestShopStore.template(server, shop);
        Inventory inventory = ChestShopService.inventory(world, shop.chestPos());
        if (world == null || template.isEmpty() || inventory == null) {
            ChestShopService.send(player, new ChestShopPayloads.Close("That chest shop is unavailable.", false));
            return;
        }
        long floor = ChestShopService.minimumPrice(template);
        if (shop.unitPrice < floor) {
            shop.unitPrice = floor;
            ChestShopStore.put(shop);
            ChestShopService.updateSign(server, shop);
        }
        int stock = ChestShopService.count(inventory, template);
        ChestShopService.send(player, new ChestShopPayloads.ShopOpen(shop.id, template.copyWithCount(1), shop.unitPrice, stock, shop.ownerName, CobbleClubServer.config().currencySymbol, manager, notice, error));
    }

    public static void cancelSetup(ServerPlayerEntity player, boolean removeProvisional) {
        ServerWorld world;
        PendingSetup pending = PENDING.remove(player.getUuid());
        if (pending != null && removeProvisional && pending.existingShopId == null && (world = player.getServerWorld()).getBlockState(pending.signPos).isOf(Blocks.DARK_OAK_WALL_SIGN)) {
            world.removeBlock(pending.signPos, false);
        }
    }

    public static boolean canBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        ChestShopStore.ShopData shop = ChestShopService.findShopAt(world, pos);
        if (shop == null) {
            return true;
        }
        return ChestShopService.canManage(player, world, shop);
    }

    public static boolean isShopBlock(ServerWorld world, BlockPos pos) {
        return ChestShopService.findShopAt(world, pos) != null;
    }

    public static boolean canManageShopAt(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        ChestShopStore.ShopData shop = ChestShopService.findShopAt(world, pos);
        return shop != null && ChestShopService.canManage(player, world, shop);
    }

    public static void afterBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        ChestShopStore.ShopData shop = ChestShopService.findShopAt(world, pos);
        if (shop == null || !ChestShopService.canManage(player, world, shop)) {
            return;
        }
        boolean chestBroken = ChestShopService.isChestPosition(world, shop, pos);
        ChestShopStore.remove(shop.id);
        if (chestBroken && world.getBlockState(shop.signPos()).getBlock() instanceof WallSignBlock) {
            world.removeBlock(shop.signPos(), false);
        }
    }

    public static void tick(MinecraftServer server) {
        if (++refreshTicks < 40) {
            return;
        }
        refreshTicks = 0;
        ArrayList<ChestShopStore.ShopData> remove = new ArrayList<>();
        for (ChestShopStore.ShopData shop : new ArrayList<>(ChestShopStore.all())) {
            ServerWorld world = ChestShopStore.world(server, shop);
            if (world == null || !world.isChunkLoaded(shop.chestX >> 4, shop.chestZ >> 4) || !world.isChunkLoaded(shop.signX >> 4, shop.signZ >> 4)) continue;
            if (!ChestShopService.validShopLocation(world, shop) || ChestShopStore.template(server, shop).isEmpty()) {
                remove.add(shop);
                continue;
            }
            ItemStack template = ChestShopStore.template(server, shop);
            long floor = ChestShopService.minimumPrice(template);
            if (shop.unitPrice < floor) {
                shop.unitPrice = floor;
                ChestShopStore.put(shop);
            }
            ChestShopService.updateSign(server, shop);
        }
        for (ChestShopStore.ShopData shop : remove) {
            ChestShopService.removeShop(server, shop, true);
        }
    }

    private static boolean validShopLocation(ServerWorld world, ChestShopStore.ShopData shop) {
        return world != null && shop != null && ClaimsService.claimAt(world, shop.chestPos()) != null && world.getBlockState(shop.chestPos()).getBlock() instanceof ChestBlock && world.getBlockState(shop.signPos()).getBlock() instanceof WallSignBlock && ChestShopService.inventory(world, shop.chestPos()) != null;
    }

    private static void refreshAll(MinecraftServer server) {
        for (ChestShopStore.ShopData shop : new ArrayList<>(ChestShopStore.all())) {
            ChestShopService.updateSign(server, shop);
        }
    }

    private static void removeShop(MinecraftServer server, ChestShopStore.ShopData shop, boolean removeSign) {
        ChestShopStore.remove(shop.id);
        if (!removeSign) {
            return;
        }
        ServerWorld world = ChestShopStore.world(server, shop);
        if (world != null && world.getBlockState(shop.signPos()).getBlock() instanceof WallSignBlock) {
            world.removeBlock(shop.signPos(), false);
        }
    }

    private static boolean canManage(ServerPlayerEntity player, ServerWorld world, ChestShopStore.ShopData shop) {
        if (player == null || world == null || shop == null || player.getServerWorld() != world) {
            return false;
        }
        if (PermissionService.admin(player, "cobbleclub.chestshop.bypass", 2)) {
            return true;
        }
        return ClaimsService.canManageChestShop(player, shop.chestPos());
    }

    public static ChestShopStore.ShopData findShopAt(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        String dim = world.getRegistryKey().getValue().toString();
        for (ChestShopStore.ShopData shop : ChestShopStore.all()) {
            if (!dim.equals(shop.dimension) || !shop.signPos().equals(pos) && !ChestShopService.isChestPosition(world, shop, pos)) continue;
            return shop;
        }
        return null;
    }

    private static boolean isChestPosition(ServerWorld world, ChestShopStore.ShopData shop, BlockPos pos) {
        BlockPos main = shop.chestPos();
        if (main.equals(pos)) {
            return true;
        }
        BlockState state = world.getBlockState(main);
        if (!(state.getBlock() instanceof ChestBlock) || state.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
            return false;
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos candidatePos = main.offset(direction);
            BlockState candidate = world.getBlockState(candidatePos);
            if (!(candidate.getBlock() instanceof ChestBlock) || candidate.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE || candidate.get(ChestBlock.FACING) != state.get(ChestBlock.FACING) || candidate.get(ChestBlock.CHEST_TYPE) == state.get(ChestBlock.CHEST_TYPE) || !candidatePos.equals(pos)) continue;
            return true;
        }
        return false;
    }

    private static Inventory inventory(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof ChestBlock)) {
            return null;
        }
        ChestBlock chest = (ChestBlock) block;
        return ChestBlock.getInventory(chest, state, world, pos, true);
    }

    private static int count(Inventory inventory, ItemStack template) {
        long total = 0L;
        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack stack = inventory.getStack(i);
            if (!ItemStack.areItemsAndComponentsEqual(stack, template)) continue;
            total += (long)stack.getCount();
        }
        return (int)Math.min(Integer.MAX_VALUE, total);
    }

    private static int removeExact(Inventory inventory, ItemStack template, int amount) {
        int left = amount;
        for (int i = 0; i < inventory.size() && left > 0; ++i) {
            ItemStack stack = inventory.getStack(i);
            if (!ItemStack.areItemsAndComponentsEqual(stack, template)) continue;
            int take = Math.min(left, stack.getCount());
            stack.decrement(take);
            left -= take;
        }
        inventory.markDirty();
        return amount - left;
    }

    private static void restore(Inventory inventory, ItemStack template, int amount) {
        int i;
        int left = amount;
        int max = Math.max(1, template.getMaxCount());
        for (i = 0; i < inventory.size() && left > 0; ++i) {
            ItemStack existing = inventory.getStack(i);
            if (existing.isEmpty() || !ItemStack.areItemsAndComponentsEqual(existing, template) || existing.getCount() >= existing.getMaxCount()) continue;
            int add = Math.min(left, existing.getMaxCount() - existing.getCount());
            existing.increment(add);
            left -= add;
        }
        for (i = 0; i < inventory.size() && left > 0; ++i) {
            if (!inventory.getStack(i).isEmpty()) continue;
            int add = Math.min(left, max);
            inventory.setStack(i, template.copyWithCount(add));
            left -= add;
        }
        inventory.markDirty();
    }

    private static void give(ServerPlayerEntity player, ItemStack template, int amount) {
        int batch;
        int max = Math.max(1, template.getMaxCount());
        PlayerInventory inv = player.getInventory();
        for (int left = amount; left > 0; left -= batch) {
            batch = Math.min(left, max);
            ItemStack out = template.copyWithCount(batch);
            inv.insertStack(out);
            if (out.isEmpty()) continue;
            player.dropItem(out, false);
        }
    }

    private static long minimumPrice(ItemStack stack) {
        return Math.max(1L, ActivityEconomyService.sellPrice(stack));
    }

    private static long safeMultiply(long a, long b) {
        if (a <= 0L || b <= 0L) {
            return 0L;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    private static void writeSetupSign(ServerWorld world, BlockPos signPos) {
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        if (!(blockEntity instanceof SignBlockEntity)) {
            return;
        }
        SignBlockEntity sign = (SignBlockEntity)blockEntity;
        SignText text = new SignText().withColor(DyeColor.PURPLE).withGlowing(true).withMessage(0, Text.literal("\u2726 CobbleClub Shop \u2726")).withMessage(1, Text.literal("Setup in progress")).withMessage(2, Text.literal("Choose item")).withMessage(3, Text.literal("in the menu"));
        sign.setText(text, true);
        sign.setWaxed(true);
        sign.markDirty();
        world.updateListeners(signPos, world.getBlockState(signPos), world.getBlockState(signPos), 2);
    }

    private static void updateSign(MinecraftServer server, ChestShopStore.ShopData shop) {
        BlockEntity blockEntity;
        ServerWorld world = ChestShopStore.world(server, shop);
        if (world == null || !((blockEntity = world.getBlockEntity(shop.signPos())) instanceof SignBlockEntity)) {
            return;
        }
        SignBlockEntity sign = (SignBlockEntity)blockEntity;
        ItemStack template = ChestShopStore.template(server, shop);
        if (template.isEmpty()) {
            return;
        }
        Inventory inv = ChestShopService.inventory(world, shop.chestPos());
        if (inv == null) {
            return;
        }
        int stock = ChestShopService.count(inv, template);
        String item = ChestShopService.trim(template.getName().getString(), 18);
        String price = CobbleClubServer.config().currencySymbol + String.format("%,d", Math.max(1L, shop.unitPrice)) + " ea \u2022 x" + stock;
        SignText text = new SignText().withColor(DyeColor.PURPLE).withGlowing(true).withMessage(0, Text.literal("\u2726 CobbleClub Shop \u2726")).withMessage(1, Text.literal(item)).withMessage(2, Text.literal(ChestShopService.trim(price, 22))).withMessage(3, Text.literal("Right-click to buy"));
        SignText old = sign.getText(true);
        boolean changed = !sign.isWaxed();
        for (int line = 0; line < 4 && !changed; ++line) {
            changed = !old.getMessage(line, false).getString().equals(text.getMessage(line, false).getString());
        }
        if (changed) {
            sign.setText(text, true);
            sign.setWaxed(true);
            sign.markDirty();
            world.updateListeners(shop.signPos(), world.getBlockState(shop.signPos()), world.getBlockState(shop.signPos()), 2);
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(1, max - 1)) + "\u2026";
    }

    private static void send(ServerPlayerEntity player, CustomPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.getId())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static final class PendingSetup {
        final BlockPos chestPos;
        final BlockPos signPos;
        final String existingShopId;
        ItemStack selected;

        PendingSetup(BlockPos chestPos, BlockPos signPos, String existingShopId, ItemStack selected) {
            this.chestPos = chestPos;
            this.signPos = signPos;
            this.existingShopId = existingShopId;
            this.selected = selected == null ? ItemStack.EMPTY : selected;
        }
    }
}
