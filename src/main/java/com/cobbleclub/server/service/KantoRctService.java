package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.data.PlayerDataStore;
import java.lang.reflect.Method;
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

public final class KantoRctService {
    private static final long DROP_PROTECTION_TICKS = 400L;
    private static final Map<UUID, ProtectedDrop> PROTECTED_DROPS = new LinkedHashMap<>();
    private static long ticks;

    private static final Map<String, Milestone> MILESTONES = Map.ofEntries(
            Map.entry("brock", new Milestone("Gym Leader Brock", "Boulder Badge", "boulder", 1200L, 2L, 2, null)),
            Map.entry("misty", new Milestone("Gym Leader Misty", "Cascade Badge", "cascade", 1600L, 2L, 2, null)),
            Map.entry("surge", new Milestone("Gym Leader Lt. Surge", "Thunder Badge", "thunder", 2000L, 3L, 2, null)),
            Map.entry("erika", new Milestone("Gym Leader Erika", "Rainbow Badge", "rainbow", 2400L, 3L, 2, null)),
            Map.entry("koga", new Milestone("Gym Leader Koga", "Soul Badge", "soul", 2800L, 4L, 2, null)),
            Map.entry("sabrina", new Milestone("Gym Leader Sabrina", "Marsh Badge", "marsh", 3400L, 4L, 3, null)),
            Map.entry("blaine", new Milestone("Gym Leader Blaine", "Volcano Badge", "volcano", 4000L, 5L, 3, null)),
            Map.entry("giovanni", new Milestone("Gym Leader Giovanni", "Earth Badge", "earth", 5000L, 6L, 3, null)),
            Map.entry("lorelei", new Milestone("Elite Four Lorelei", null, null, 3000L, 3L, 3, null)),
            Map.entry("bruno", new Milestone("Elite Four Bruno", null, null, 3500L, 3L, 3, null)),
            Map.entry("agatha", new Milestone("Elite Four Agatha", null, null, 4000L, 4L, 3, null)),
            Map.entry("lance", new Milestone("Elite Four Lance", null, null, 5000L, 5L, 4, null)),
            Map.entry("champion", new Milestone("Champion Blue", null, null, 12500L, 12L, 5, "cobblemon:master_ball"))
    );

    private KantoRctService() {
    }

    public static int handleReward(ServerPlayerEntity player, String key) {
        if (player == null || key == null) return 0;

        String normalized = key.toLowerCase(Locale.ROOT);
        Milestone milestone = MILESTONES.get(normalized);
        if (milestone == null) return 0;

        String pendingTag = "cc_rct_kanto_" + normalized + "_pending";
        if (!player.getCommandTags().contains(pendingTag)) {
            return 0;
        }
        player.removeCommandTag(pendingTag);

        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.normalize();
        String completionKey = "kanto:" + normalized;
        if (!data.rctMilestones.add(completionKey)) {
            return 0;
        }

        EconomyService.deposit(player, milestone.money());
        EconomyService.depositGems(player, milestone.gems());

        if (milestone.badgeKey() != null) {
            ItemStack badge = badgeStack(milestone.badgeKey(), milestone.badgeName());
            deliver(player, badge, milestone.badgeName());
        }

        if (milestone.rareCandies() > 0) {
            deliver(player, new ItemStack(resolveItem("cobblemon:rare_candy", Items.EXPERIENCE_BOTTLE), milestone.rareCandies()), "Rare Candy reward");
        }
        if (milestone.bonusItemId() != null) {
            Item bonus = resolveItem(milestone.bonusItemId(), Items.DIAMOND);
            String name = Registries.ITEM.getId(bonus).getPath().replace('_', ' ');
            deliver(player, new ItemStack(bonus), titleCase(name));
        }

        ++data.revision;
        PlayerDataStore.save();

        announce(player, milestone, normalized);
        player.sendMessage(Text.literal("Kanto reward: ")
                .formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(EconomyService.format(milestone.money())).formatted(Formatting.GOLD))
                .append(Text.literal(" + " + milestone.gems() + " Gems").formatted(Formatting.AQUA)), false);
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

    private static void announce(ServerPlayerEntity player, Milestone milestone, String key) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        String party = partySummary(player);
        boolean gym = milestone.badgeName() != null;

        MutableText message = Text.literal("✦ ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                .append(Text.literal(player.getGameProfile().getName()).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(gym ? " defeated " : " conquered ").formatted(Formatting.GRAY))
                .append(Text.literal(milestone.opponent()).formatted(gym ? Formatting.GOLD : Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal(" with ").formatted(Formatting.GRAY))
                .append(Text.literal(party).formatted(Formatting.AQUA))
                .append(Text.literal("!").formatted(Formatting.GRAY));

        server.getPlayerManager().broadcast(message, false);

        if (gym) {
            server.getPlayerManager().broadcast(
                    Text.literal("  ↳ " + milestone.badgeName() + " earned · Kanto Journey")
                            .formatted(Formatting.YELLOW), false);
        } else if ("champion".equals(key)) {
            server.getPlayerManager().broadcast(
                    Text.literal("  ↳ A new CobbleClub Kanto Champion has entered the Hall of Fame!")
                            .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        }
    }

    private static ItemStack badgeStack(String badgeKey, String displayName) {
        String wantedPath = badgeKey + "_badge";

        // Prefer the two badge mods actually shipped in the CobbleClub pack.
        // Namespace matching is intentionally tolerant because published mod ids can
        // differ slightly from their display names.
        for (String preferred : List.of("cobbleversebadges", "cobbleverse_badges", "cobblemonpokemonbadges", "cobblemon_pokemon_badges")) {
            for (Identifier id : Registries.ITEM.getIds()) {
                if (!wantedPath.equals(id.getPath())) continue;
                String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
                if (!namespace.equals(preferred) && !namespace.contains(preferred.replace("_", ""))) continue;
                Item candidate = Registries.ITEM.get(id);
                if (candidate != null && candidate != Items.AIR) {
                    CobbleClubServer.LOGGER.info("Resolved {} from installed badge mod item {}", displayName, id);
                    return new ItemStack(candidate);
                }
            }
        }

        // Safety net: accept any installed badge item with the canonical Kanto path.
        for (Identifier id : Registries.ITEM.getIds()) {
            if (!wantedPath.equals(id.getPath())) continue;
            Item candidate = Registries.ITEM.get(id);
            if (candidate == null || candidate == Items.AIR) continue;
            if (id.getNamespace().toLowerCase(Locale.ROOT).contains("badge")) {
                CobbleClubServer.LOGGER.warn("Resolved {} from fallback badge namespace {}", displayName, id);
                return new ItemStack(candidate);
            }
        }

        CobbleClubServer.LOGGER.error("No installed badge item with path '{}' was found. Using named fallback item.", wantedPath);
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
        if (inserted && remaining.isEmpty()) {
            return;
        }

        Vec3d look = player.getRotationVec(1.0f);
        double x = player.getX() + look.x * 1.25;
        double y = player.getY() + 0.6;
        double z = player.getZ() + look.z * 1.25;

        ItemEntity drop = new ItemEntity(player.getServerWorld(), x, y, z, remaining);
        drop.setOwner(player.getUuid());
        drop.setPickupDelay(0);
        player.getServerWorld().spawnEntity(drop);
        PROTECTED_DROPS.put(drop.getUuid(), new ProtectedDrop(drop, ticks + DROP_PROTECTION_TICKS));

        player.sendMessage(Text.literal("Your inventory was full, so " + rewardName + " was dropped in front of you.")
                .formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Only you can pick it up for the first 20 seconds.")
                .formatted(Formatting.GRAY), false);
    }

    private static String partySummary(ServerPlayerEntity player) {
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemon = cobblemonClass.getField("INSTANCE").get(null);
            Method getStorage = findNoArg(cobblemon.getClass(), "getStorage");
            if (getStorage == null) return "their team";
            Object storage = getStorage.invoke(cobblemon);
            Method getParty = null;
            for (Method method : storage.getClass().getMethods()) {
                if ("getParty".equals(method.getName()) && method.getParameterCount() == 1) {
                    getParty = method;
                    break;
                }
            }
            if (getParty == null) return "their team";
            Object party = getParty.invoke(storage, player);
            if (!(party instanceof Iterable<?> iterable)) return "their team";

            List<String> names = new ArrayList<>();
            for (Object pokemon : iterable) {
                if (pokemon == null) continue;
                String name = pokemonName(pokemon);
                if (!name.isBlank()) names.add(name);
                if (names.size() >= 6) break;
            }
            return names.isEmpty() ? "their team" : String.join(", ", names);
        } catch (Throwable ignored) {
            return "their team";
        }
    }

    private static String pokemonName(Object pokemon) {
        try {
            Method display = findNoArg(pokemon.getClass(), "getDisplayName");
            if (display != null) {
                Object text = display.invoke(pokemon);
                Method getString = text == null ? null : findNoArg(text.getClass(), "getString");
                if (getString != null) {
                    Object value = getString.invoke(text);
                    if (value != null && !value.toString().isBlank()) return value.toString();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Method speciesMethod = findNoArg(pokemon.getClass(), "getSpecies");
            Object species = speciesMethod == null ? null : speciesMethod.invoke(pokemon);
            if (species != null) {
                Method nameMethod = findNoArg(species.getClass(), "getName");
                Object name = nameMethod == null ? null : nameMethod.invoke(species);
                if (name != null && !name.toString().isBlank()) return name.toString();
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static Method findNoArg(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "Bonus reward";
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
            int rareCandies,
            String bonusItemId
    ) {
    }
}
