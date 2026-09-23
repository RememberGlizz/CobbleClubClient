package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.PlayerDataStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * RCT reward bridge for the post-Kanto CobbleClub Journey.
 *
 * Kanto remains entirely owned by KantoRctService. This class mirrors that
 * already-proven reward flow for Johto, Hoenn and Sinnoh only.
 */
public final class RegionalRctService {
    private static final long DROP_PROTECTION_TICKS = 400L;
    private static final Map<UUID, ProtectedDrop> PROTECTED_DROPS = new LinkedHashMap<>();
    private static long ticks;

    private static final Map<String, Map<String, Milestone>> REGIONS = Map.of(
            "johto", Map.ofEntries(
                    Map.entry("falkner", new Milestone("Gym Leader Falkner", "Zephyr Badge", "zephyr", 6000L, 6L, 3)),
                    Map.entry("bugsy", new Milestone("Gym Leader Bugsy", "Hive Badge", "hive", 6800L, 7L, 3)),
                    Map.entry("whitney", new Milestone("Gym Leader Whitney", "Plain Badge", "plain", 7600L, 8L, 3)),
                    Map.entry("morty", new Milestone("Gym Leader Morty", "Fog Badge", "fog", 8400L, 8L, 4)),
                    Map.entry("chuck", new Milestone("Gym Leader Chuck", "Storm Badge", "storm", 9200L, 9L, 4)),
                    Map.entry("jasmine", new Milestone("Gym Leader Jasmine", "Mineral Badge", "mineral", 10000L, 10L, 4)),
                    Map.entry("pryce", new Milestone("Gym Leader Pryce", "Glacier Badge", "glacier", 11000L, 11L, 5)),
                    Map.entry("clair", new Milestone("Gym Leader Clair", "Rising Badge", "rising", 12500L, 12L, 5))
            ),
            "hoenn", Map.ofEntries(
                    Map.entry("roxanne", new Milestone("Gym Leader Roxanne", "Stone Badge", "stone", 14000L, 12L, 4)),
                    Map.entry("brawly", new Milestone("Gym Leader Brawly", "Knuckle Badge", "knuckle", 15500L, 13L, 4)),
                    Map.entry("wattson", new Milestone("Gym Leader Wattson", "Dynamo Badge", "dynamo", 17000L, 14L, 5)),
                    Map.entry("flannery", new Milestone("Gym Leader Flannery", "Heat Badge", "heat", 18500L, 15L, 5)),
                    Map.entry("norman", new Milestone("Gym Leader Norman", "Balance Badge", "balance", 20500L, 16L, 5)),
                    Map.entry("winona", new Milestone("Gym Leader Winona", "Feather Badge", "feather", 22500L, 17L, 6)),
                    Map.entry("tate_liza", new Milestone("Gym Leaders Tate & Liza", "Mind Badge", "mind", 24500L, 18L, 6)),
                    Map.entry("wallace", new Milestone("Gym Leader Wallace", "Rain Badge", "rain", 27000L, 20L, 7))
            ),
            "sinnoh", Map.ofEntries(
                    Map.entry("roark", new Milestone("Gym Leader Roark", "Coal Badge", "coal", 30000L, 20L, 6)),
                    Map.entry("gardenia", new Milestone("Gym Leader Gardenia", "Forest Badge", "forest", 32500L, 21L, 6)),
                    Map.entry("maylene", new Milestone("Gym Leader Maylene", "Cobble Badge", "cobble", 35000L, 22L, 7)),
                    Map.entry("wake", new Milestone("Gym Leader Crasher Wake", "Fen Badge", "fen", 38000L, 24L, 7)),
                    Map.entry("fantina", new Milestone("Gym Leader Fantina", "Relic Badge", "relic", 41000L, 26L, 8)),
                    Map.entry("byron", new Milestone("Gym Leader Byron", "Mine Badge", "mine", 44000L, 28L, 8)),
                    Map.entry("candice", new Milestone("Gym Leader Candice", "Icicle Badge", "icicle", 47000L, 30L, 9)),
                    Map.entry("volkner", new Milestone("Gym Leader Volkner", "Beacon Badge", "beacon", 50000L, 32L, 10))
            )
    );

    private RegionalRctService() {
    }

    public static int handleReward(ServerPlayerEntity player, String region, String key) {
        if (player == null || region == null || key == null) return 0;

        String normalizedRegion = region.toLowerCase(Locale.ROOT);
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        Map<String, Milestone> milestones = REGIONS.get(normalizedRegion);
        if (milestones == null) return 0;

        Milestone milestone = milestones.get(normalizedKey);
        if (milestone == null) return 0;

        String pendingTag = "cc_rct_" + normalizedRegion + "_" + normalizedKey + "_pending";
        if (!player.getCommandTags().contains(pendingTag)) {
            return 0;
        }
        player.removeCommandTag(pendingTag);

        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.normalize();
        String completionKey = normalizedRegion + ":" + normalizedKey;
        if (!data.rctMilestones.add(completionKey)) {
            return 0;
        }

        EconomyService.deposit(player, milestone.money());
        EconomyService.depositGems(player, milestone.gems());

        ItemStack badge = badgeStack(normalizedRegion, milestone.badgeKey(), milestone.badgeName());
        deliver(player, badge, milestone.badgeName());

        if (milestone.rareCandies() > 0) {
            deliver(
                    player,
                    new ItemStack(resolveItem("cobblemon:rare_candy", Items.EXPERIENCE_BOTTLE), milestone.rareCandies()),
                    "Rare Candy reward"
            );
        }

        ++data.revision;
        PlayerDataStore.save();

        announce(player, normalizedRegion, milestone);
        player.sendMessage(
                Text.literal(titleCase(normalizedRegion) + " reward: ").formatted(Formatting.GRAY)
                        .append(Text.literal(EconomyService.format(milestone.money())).formatted(Formatting.GOLD))
                        .append(Text.literal(" + " + milestone.gems() + " Gems").formatted(Formatting.AQUA)),
                false
        );
        return 1;
    }

    public static void tick(MinecraftServer server) {
        ++ticks;
        if (PROTECTED_DROPS.isEmpty()) return;

        var iterator = PROTECTED_DROPS.entrySet().iterator();
        while (iterator.hasNext()) {
            ProtectedDrop protectedDrop = iterator.next().getValue();
            ItemEntity item = protectedDrop.item();
            if (item == null || !item.isAlive()) {
                iterator.remove();
                continue;
            }
            if (ticks >= protectedDrop.expiresAt()) {
                item.setOwner(null);
                iterator.remove();
            }
        }
    }

    private static void announce(ServerPlayerEntity player, String region, Milestone milestone) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        MutableText message = Text.literal("✦ ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal(player.getGameProfile().getName()).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(" defeated ").formatted(Formatting.GRAY))
                .append(Text.literal(milestone.opponent()).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" and earned the ").formatted(Formatting.GRAY))
                .append(Text.literal(milestone.badgeName()).formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal(" · " + titleCase(region) + " Journey").formatted(Formatting.GRAY));

        server.getPlayerManager().broadcast(message, false);
    }

    private static ItemStack badgeStack(String region, String badgeKey, String displayName) {
        List<Identifier> preferredIds = new ArrayList<>();
        String regionalPath = region + "_" + badgeKey + "_badge";
        String genericPath = badgeKey + "_badge";

        preferredIds.add(Identifier.of("cobbleversebadges", regionalPath));
        preferredIds.add(Identifier.of("cobblemonpokemonbadges", regionalPath));
        preferredIds.add(Identifier.of("cobblemon_pokemon_badges", regionalPath));
        preferredIds.add(Identifier.of("cobblemonpokemonbadges", genericPath));
        preferredIds.add(Identifier.of("cobblemon_pokemon_badges", genericPath));

        for (Identifier id : preferredIds) {
            if (!Registries.ITEM.containsId(id)) continue;
            Item item = Registries.ITEM.get(id);
            if (item != null && item != Items.AIR) {
                CobbleClubServer.LOGGER.info("Resolved {} as {}", displayName, id);
                return new ItemStack(item);
            }
        }

        for (Identifier id : Registries.ITEM.getIds()) {
            String path = id.getPath();
            if (!path.equals(regionalPath) && !path.equals(genericPath)) continue;
            Item item = Registries.ITEM.get(id);
            if (item == null || item == Items.AIR) continue;
            CobbleClubServer.LOGGER.warn("Resolved {} from fallback badge item {}", displayName, id);
            return new ItemStack(item);
        }

        CobbleClubServer.LOGGER.error("No installed item was found for {}. Using named fallback item.", displayName);
        ItemStack fallback = new ItemStack(Items.NETHER_STAR);
        fallback.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).formatted(Formatting.GOLD, Formatting.BOLD));
        return fallback;
    }

    private static Item resolveItem(String rawId, Item fallback) {
        try {
            Identifier id = Identifier.of(rawId);
            Item item = Registries.ITEM.get(id);
            return item == null || item == Items.AIR ? fallback : item;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void deliver(ServerPlayerEntity player, ItemStack stack, String rewardName) {
        if (stack == null || stack.isEmpty()) return;

        ItemStack remaining = stack.copy();
        boolean inserted = player.getInventory().insertStack(remaining);
        if (inserted && remaining.isEmpty()) return;

        Vec3d look = player.getRotationVec(1.0f);
        double x = player.getX() + look.x * 1.25;
        double y = player.getY() + 0.6;
        double z = player.getZ() + look.z * 1.25;

        ItemEntity drop = new ItemEntity(player.getServerWorld(), x, y, z, remaining);
        drop.setOwner(player.getUuid());
        drop.setPickupDelay(0);
        player.getServerWorld().spawnEntity(drop);
        PROTECTED_DROPS.put(drop.getUuid(), new ProtectedDrop(drop, ticks + DROP_PROTECTION_TICKS));

        player.sendMessage(
                Text.literal("Your inventory was full, so " + rewardName + " was dropped in front of you.")
                        .formatted(Formatting.YELLOW),
                false
        );
        player.sendMessage(
                Text.literal("Only you can pick it up for the first 20 seconds.").formatted(Formatting.GRAY),
                false
        );
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        for (String part : value.split("[ _-]+")) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    private record ProtectedDrop(ItemEntity item, long expiresAt) {
    }

    private record Milestone(
            String opponent,
            String badgeName,
            String badgeKey,
            long money,
            long gems,
            int rareCandies
    ) {
    }
}
