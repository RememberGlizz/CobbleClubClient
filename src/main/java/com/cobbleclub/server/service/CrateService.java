package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Weighted crate rolls and real reward delivery. */
public final class CrateService {
    private CrateService() {}

    public static int keys(ServerPlayerEntity player, String crateId) {
        return EconomyService.data(player).crateKeys.getOrDefault(crateId, 0);
    }

    public static boolean giveKeys(ServerPlayerEntity player, String crateId, int amount) {
        ServerConfig.CrateDefinition crate = CatalogService.findCrate(crateId);
        if (crate == null || amount <= 0) return false;
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.crateKeys.merge(crate.keyId, amount, CrateService::safeAdd);
        data.revision++;
        PlayerDataStore.save();
        player.sendMessage(Text.literal("Received " + amount + " " + crate.keyDisplayName + "(s)."), false);
        return true;
    }

    public static boolean open(ServerPlayerEntity player, String crateId) {
        ServerConfig.CrateDefinition crate = CatalogService.findCrate(crateId);
        if (crate == null || crate.prizes == null || crate.prizes.isEmpty()) {
            player.sendMessage(Text.literal("That crate has no configured rewards."), false);
            return false;
        }
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        String key = crate.keyId == null ? crate.id : crate.keyId;
        int held = data.crateKeys.getOrDefault(key, 0);
        if (held <= 0) {
            player.sendMessage(Text.literal("You need a " + crate.keyDisplayName + " to open this crate."), false);
            return false;
        }

        ServerConfig.CratePrize prize = roll(crate.prizes);
        if (prize == null) {
            player.sendMessage(Text.literal("This crate's reward weights are invalid."), false);
            return false;
        }
        data.crateKeys.put(key, held - 1);
        data.crateOpens.merge(crate.id, 1, CrateService::safeAdd);
        deliver(player, prize);
        data.revision++;
        PlayerDataStore.save();

        player.sendMessage(Text.literal("You opened the " + crate.id + " crate and won " + prize.displayName + "!"), false);
        if ((prize.broadcast || crate.broadcastWins) && CobbleClubServer.config().broadcastRareCrateWins) {
            player.getServer().getPlayerManager().broadcast(
                    Text.literal("[CobbleClub] " + player.getGameProfile().getName() + " won " + prize.displayName + "!"), false
            );
        }
        return true;
    }

    public static boolean testReward(ServerPlayerEntity player, String crateId, int prizeIndex) {
        ServerConfig.CrateDefinition crate = CatalogService.findCrate(crateId);
        if (crate == null || !crate.canTest || !player.hasPermissionLevel(2)
                || prizeIndex < 0 || prizeIndex >= crate.prizes.size()) return false;
        ServerConfig.CratePrize prize = crate.prizes.get(prizeIndex);
        deliver(player, prize);
        player.sendMessage(Text.literal("Issued test reward: " + prize.displayName), false);
        return true;
    }

    public static boolean handleCrateBlock(ServerPlayerEntity player, BlockPos pos) {
        String dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        for (ServerConfig.CrateLocation location : CobbleClubServer.config().crateLocations) {
            if (location != null && dimension.equals(location.dimension())
                    && pos.getX() == location.x() && pos.getY() == location.y() && pos.getZ() == location.z()) {
                if (player.isSneaking()) CatalogService.openCrate(player, location.crateId());
                else open(player, location.crateId());
                return true;
            }
        }
        return false;
    }

    public static String keySummary(ServerPlayerEntity player) {
        StringBuilder result = new StringBuilder("CobbleClub keys: ");
        boolean first = true;
        for (ServerConfig.CrateDefinition crate : CobbleClubServer.config().crates) {
            if (crate == null || crate.id == null) continue;
            if (!first) result.append(", ");
            first = false;
            result.append(crate.id).append('=').append(keys(player, crate.keyId == null ? crate.id : crate.keyId));
        }
        return first ? "No crates are configured." : result.toString();
    }

    private static ServerConfig.CratePrize roll(List<ServerConfig.CratePrize> prizes) {
        double total = 0;
        for (ServerConfig.CratePrize prize : prizes) if (prize != null && prize.chance > 0) total += prize.chance;
        if (total <= 0) return null;
        double selected = ThreadLocalRandom.current().nextDouble(total);
        for (ServerConfig.CratePrize prize : prizes) {
            if (prize == null || prize.chance <= 0) continue;
            selected -= prize.chance;
            if (selected < 0) return prize;
        }
        return prizes.get(prizes.size() - 1);
    }

    private static void deliver(ServerPlayerEntity player, ServerConfig.CratePrize prize) {
        String type = prize.rewardType == null ? "ITEM" : prize.rewardType.toUpperCase(Locale.ROOT);
        String id = prize.rewardId == null ? prize.material : prize.rewardId;
        long amount = Math.max(1, prize.rewardAmount);
        switch (type) {
            case "MONEY" -> EconomyService.deposit(player, amount);
            case "CLAIM_BLOCKS" -> EconomyService.data(player).bonusClaimBlocks = safeAdd(
                    EconomyService.data(player).bonusClaimBlocks, (int) Math.min(Integer.MAX_VALUE, amount));
            case "KEY" -> giveKeys(player, id, (int) Math.min(Integer.MAX_VALUE, amount));
            case "TAG" -> EconomyService.data(player).ownedTags.add(id);
            case "COSMETIC" -> EconomyService.data(player).ownedCosmetics.add(id);
            case "GLOW" -> EconomyService.data(player).ownedGlows.add(id);
            case "POKEMON" -> runPokemonReward(player, prize);
            case "COMMAND" -> runCommands(player, prize.commands);
            default -> giveItem(player, id, (int) Math.min(Integer.MAX_VALUE, amount));
        }
        if (!"COMMAND".equals(type) && prize.commands != null && !prize.commands.isEmpty()) runCommands(player, prize.commands);
    }

    private static void giveItem(ServerPlayerEntity player, String itemId, int amount) {
        ItemStack custom = ClubItems.stack(itemId, amount);
        if (!custom.isEmpty()) {
            if (!player.getInventory().insertStack(custom)) player.dropItem(custom, false);
            return;
        }
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            player.sendMessage(Text.literal("Reward item is not installed: " + itemId), false);
            return;
        }
        Item item = Registries.ITEM.get(identifier);
        int remaining = Math.max(1, amount);
        while (remaining > 0) {
            int count = Math.min(item.getMaxCount(), remaining);
            ItemStack stack = new ItemStack(item, count);
            if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
            remaining -= count;
        }
    }

    private static void runPokemonReward(ServerPlayerEntity player, ServerConfig.CratePrize prize) {
        String template = CobbleClubServer.config().pokemonRewardCommand;
        if (template == null || template.isBlank()) {
            player.sendMessage(Text.literal("Pokémon reward command is not configured."), false);
            return;
        }
        String species = prize.species == null ? prize.rewardId : prize.species;
        String aspects = prize.aspects == null || prize.aspects.isEmpty() ? "" : String.join(",", prize.aspects);
        runCommand(player, template
                .replace("{player}", player.getGameProfile().getName())
                .replace("{species}", species == null ? "" : species)
                .replace("{aspects}", aspects));
    }

    private static void runCommands(ServerPlayerEntity player, List<String> commands) {
        if (commands == null) return;
        for (String command : commands) {
            if (command == null || command.isBlank()) continue;
            runCommand(player, command.replace("{player}", player.getGameProfile().getName()));
        }
    }

    private static void runCommand(ServerPlayerEntity player, String command) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), normalized);
    }

    private static int safeAdd(int left, int right) {
        if (right > 0 && left > Integer.MAX_VALUE - right) return Integer.MAX_VALUE;
        return Math.max(0, left + right);
    }
}
