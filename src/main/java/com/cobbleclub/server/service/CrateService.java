/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2338
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_7923
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.service.CatalogService;
import com.cobbleclub.server.service.ClubItems;
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.util.CrateKeyIds;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;

public final class CrateService {
    private CrateService() {
    }

    public static int keys(ServerPlayerEntity player, String crateId) {
        return EconomyService.data((ServerPlayerEntity)player).crateKeys.getOrDefault(CrateKeyIds.canonical(crateId), 0);
    }

    public static boolean giveKeys(ServerPlayerEntity player, String crateId, int amount) {
        ServerConfig.CrateDefinition crate = CatalogService.findCrate(crateId);
        if (crate == null || amount <= 0) {
            return false;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        String key = CrateKeyIds.canonical(crate.keyId == null ? crate.id : crate.keyId);
        data.crateKeys.merge(key, amount, CrateService::safeAdd);
        ++data.revision;
        PlayerDataStore.save();
        player.sendMessage((Text)Text.literal((String)("Received " + amount + " " + crate.keyDisplayName + "(s).")), false);
        return true;
    }

    public static boolean open(ServerPlayerEntity player, String crateId) {
        ServerConfig.CrateDefinition crate = CatalogService.findCrate(crateId);
        if (crate == null || crate.prizes == null || crate.prizes.isEmpty()) {
            player.sendMessage((Text)Text.literal((String)"That crate has no configured rewards."), false);
            return false;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        String key = CrateKeyIds.canonical(crate.keyId == null ? crate.id : crate.keyId);
        int held = data.crateKeys.getOrDefault(key, 0);
        if (held <= 0) {
            player.sendMessage((Text)Text.literal((String)("You need a " + crate.keyDisplayName + " to open this crate.")), false);
            return false;
        }
        ServerConfig.CratePrize prize = CrateService.roll(crate.prizes);
        if (prize == null) {
            player.sendMessage((Text)Text.literal((String)"This crate's reward weights are invalid."), false);
            return false;
        }
        data.crateKeys.put(key, held - 1);
        data.crateOpens.merge(crate.id, 1, CrateService::safeAdd);
        CrateService.deliver(player, prize);
        ++data.revision;
        PlayerDataStore.save();
        player.sendMessage((Text)Text.literal((String)("You opened the " + crate.id + " crate and won " + prize.displayName + "!")), false);
        if ((prize.broadcast || crate.broadcastWins) && CobbleClubServer.config().broadcastRareCrateWins) {
            player.getServer().getPlayerManager().broadcast((Text)Text.literal((String)("[CobbleClub] " + player.getGameProfile().getName() + " won " + prize.displayName + "!")), false);
        }
        return true;
    }

    public static boolean testReward(ServerPlayerEntity player, String crateId, int prizeIndex) {
        ServerConfig.CrateDefinition crate = CatalogService.findCrate(crateId);
        if (crate == null || !crate.canTest || !player.hasPermissionLevel(2) || prizeIndex < 0 || prizeIndex >= crate.prizes.size()) {
            return false;
        }
        ServerConfig.CratePrize prize = crate.prizes.get(prizeIndex);
        CrateService.deliver(player, prize);
        player.sendMessage((Text)Text.literal((String)("Issued test reward: " + prize.displayName)), false);
        return true;
    }

    public static boolean handleCrateBlock(ServerPlayerEntity player, BlockPos pos) {
        String dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        for (ServerConfig.CrateLocation location : CobbleClubServer.config().crateLocations) {
            if (location == null || !dimension.equals(location.dimension()) || pos.getX() != location.x() || pos.getY() != location.y() || pos.getZ() != location.z()) continue;
            if (player.isSneaking()) {
                CatalogService.openCrate(player, location.crateId());
            } else {
                CrateService.open(player, location.crateId());
            }
            return true;
        }
        return false;
    }

    public static String keySummary(ServerPlayerEntity player) {
        StringBuilder result = new StringBuilder("CobbleClub keys: ");
        boolean first = true;
        for (ServerConfig.CrateDefinition crate : CobbleClubServer.config().crates) {
            if (crate == null || crate.id == null) continue;
            if (!first) {
                result.append(", ");
            }
            first = false;
            result.append(crate.id).append('=').append(CrateService.keys(player, crate.keyId == null ? crate.id : crate.keyId));
        }
        return first ? "No crates are configured." : result.toString();
    }

    private static ServerConfig.CratePrize roll(List<ServerConfig.CratePrize> prizes) {
        double total = 0.0;
        for (ServerConfig.CratePrize prize : prizes) {
            if (prize == null || !(prize.chance > 0.0)) continue;
            total += prize.chance;
        }
        if (total <= 0.0) {
            return null;
        }
        double selected = ThreadLocalRandom.current().nextDouble(total);
        for (ServerConfig.CratePrize prize : prizes) {
            if (prize == null || prize.chance <= 0.0 || !((selected -= prize.chance) < 0.0)) continue;
            return prize;
        }
        return prizes.get(prizes.size() - 1);
    }

    private static void deliver(ServerPlayerEntity player, ServerConfig.CratePrize prize) {
        String type = prize.rewardType == null ? "ITEM" : prize.rewardType.toUpperCase(Locale.ROOT);
        String id = prize.rewardId == null ? prize.material : prize.rewardId;
        long amount = Math.max(1L, prize.rewardAmount);
        switch (type) {
            case "MONEY": {
                EconomyService.deposit(player, amount);
                break;
            }
            case "CLAIM_BLOCKS": {
                EconomyService.data((ServerPlayerEntity)player).bonusClaimBlocks = CrateService.safeAdd(EconomyService.data((ServerPlayerEntity)player).bonusClaimBlocks, (int)Math.min(Integer.MAX_VALUE, amount));
                break;
            }
            case "KEY": {
                CrateService.giveKeys(player, id, (int)Math.min(Integer.MAX_VALUE, amount));
                break;
            }
            case "TAG": {
                EconomyService.data((ServerPlayerEntity)player).ownedTags.add(id);
                break;
            }
            case "COSMETIC": {
                EconomyService.data((ServerPlayerEntity)player).ownedCosmetics.add(id);
                break;
            }
            case "GLOW": {
                EconomyService.data((ServerPlayerEntity)player).ownedGlows.add(id);
                break;
            }
            case "POKEMON": {
                CrateService.runPokemonReward(player, prize);
                break;
            }
            case "COMMAND": {
                CrateService.runCommands(player, prize.commands);
                break;
            }
            default: {
                CrateService.giveItem(player, id, (int)Math.min(Integer.MAX_VALUE, amount));
            }
        }
        if (!"COMMAND".equals(type) && prize.commands != null && !prize.commands.isEmpty()) {
            CrateService.runCommands(player, prize.commands);
        }
    }

    private static void giveItem(ServerPlayerEntity player, String itemId, int amount) {
        int count;
        ItemStack custom = ClubItems.stack(itemId, amount);
        if (!custom.isEmpty()) {
            if (!player.getInventory().insertStack(custom)) {
                player.dropItem(custom, false);
            }
            return;
        }
        Identifier identifier = Identifier.tryParse((String)itemId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            player.sendMessage((Text)Text.literal((String)("Reward item is not installed: " + itemId)), false);
            return;
        }
        Item item = (Item)Registries.ITEM.get(identifier);
        for (int remaining = Math.max(1, amount); remaining > 0; remaining -= count) {
            count = Math.min(item.getMaxCount(), remaining);
            ItemStack stack = new ItemStack((ItemConvertible)item, count);
            if (player.getInventory().insertStack(stack)) continue;
            player.dropItem(stack, false);
        }
    }

    private static void runPokemonReward(ServerPlayerEntity player, ServerConfig.CratePrize prize) {
        String template = CobbleClubServer.config().pokemonRewardCommand;
        if (template == null || template.isBlank()) {
            player.sendMessage((Text)Text.literal((String)"Pok\u00e9mon reward command is not configured."), false);
            return;
        }
        String species = prize.species == null ? prize.rewardId : prize.species;
        String aspects = prize.aspects == null || prize.aspects.isEmpty() ? "" : String.join((CharSequence)",", prize.aspects);
        CrateService.runCommand(player, template.replace("{player}", player.getGameProfile().getName()).replace("{species}", species == null ? "" : species).replace("{aspects}", aspects));
    }

    private static void runCommands(ServerPlayerEntity player, List<String> commands) {
        if (commands == null) {
            return;
        }
        for (String command : commands) {
            if (command == null || command.isBlank()) continue;
            CrateService.runCommand(player, command.replace("{player}", player.getGameProfile().getName()));
        }
    }

    private static void runCommand(ServerPlayerEntity player, String command) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), normalized);
    }

    private static int safeAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, left + right);
    }
}

