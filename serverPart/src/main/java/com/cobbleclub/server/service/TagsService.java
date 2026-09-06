package com.cobbleclub.server.service;

import com.cobbleclub.clubhouse.tags.protocol.TagsActionMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsActionType;
import com.cobbleclub.clubhouse.tags.protocol.TagsCategoryEntry;
import com.cobbleclub.clubhouse.tags.protocol.TagsEntry;
import com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg;
import com.cobbleclub.clubhouse.tags.protocol.TagsProtocol;
import com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg;
import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.network.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TagsService {
    private static final long SWITCH_COOLDOWN_MILLIS = 15_000L;
    private static int rankSyncTicks;

    private TagsService() {}

    public static void open(ServerPlayerEntity player) {
        syncRank(player, true);
        if (!ServerPlayNetworking.canSend(player, Payloads.TagsOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        List<TagsCategoryEntry> categories = config.tagCategories.stream()
                .map(value -> new TagsCategoryEntry(value.id(), value.displayName())).toList();
        TagsOpenMsg message = new TagsOpenMsg(
                1,
                "{\"text\":\"CobbleClub Tags\",\"color\":\"light_purple\",\"bold\":true}",
                categories,
                entries(player),
                false, // CobbleClub players always have a tag; no empty-tag state.
                "{\"text\":\"Use Rank Default\",\"color\":\"yellow\"}"
        );
        ServerPlayNetworking.send(player, new Payloads.TagsOpen(TagsProtocol.INSTANCE.encode(message)));
    }

    public static void handle(ServerPlayerEntity player, String json) {
        syncRank(player, false);
        TagsActionMsg message = TagsProtocol.INSTANCE.decode(json, TagsActionMsg.class);
        if (message == null || message.getProtocolVersion() != 1 || message.getAction() == null) return;
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();

        String requested;
        if (message.getAction() == TagsActionType.UNSET) {
            requested = RankAccessService.defaultTag(player);
        } else if (message.getAction() == TagsActionType.SET) {
            requested = message.getTagId();
        } else return;

        ServerConfig.TagDefinition definition = find(requested);
        if (definition == null || !selectable(player, data, definition)) {
            player.sendMessage(Text.literal("You do not have access to that CobbleClub tag."), false);
            sendState(player, data.revision);
            return;
        }
        if (requested.equals(data.activeTag)) return;

        long now = System.currentTimeMillis();
        long wait = SWITCH_COOLDOWN_MILLIS - (now - data.lastTagSwitchMillis);
        if (wait > 0L) {
            long seconds = Math.max(1L, (wait + 999L) / 1000L);
            player.sendMessage(Text.literal("Please wait " + seconds + "s before switching tags again."), false);
            sendState(player, data.revision);
            return;
        }

        data.activeTag = requested;
        data.lastTagSwitchMillis = now;
        data.revision++;
        PlayerDataStore.save();
        apply(player);
        sendState(player, data.revision);
    }

    /** First join + promotions/demotions automatically move the player to the new group's tag. */
    public static boolean syncRank(ServerPlayerEntity player, boolean save) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        String previousRank = data.lastTagRank;
        String rank = RankAccessService.group(player);
        String rankDefault = RankAccessService.defaultTagForGroup(rank);
        boolean rankChanged = previousRank == null || !rank.equals(previousRank);
        ServerConfig.TagDefinition active = find(data.activeTag);
        boolean invalidActive = active == null || !selectable(player, data, active);
        boolean changed = false;
        if (rankChanged || invalidActive) {
            data.activeTag = rankDefault;
            data.lastTagRank = rank;
            data.revision++;
            changed = true;
        }
        if (changed) {
            apply(player);
            if (save) PlayerDataStore.save();
            sendState(player, data.revision);
            if (rankChanged && isPremiumPromotion(previousRank, rank)) {
                announceRankUpgrade(player, rank);
            }
        }
        return changed;
    }

    private static boolean isPremiumPromotion(String previousRank, String newRank) {
        if (previousRank == null || newRank == null) return false; // Never announce first-time data initialization.
        int newLevel = premiumLevel(newRank);
        return newLevel > 0 && newLevel > premiumLevel(previousRank);
    }

    private static int premiumLevel(String rank) {
        return switch (rank == null ? "" : rank) {
            case "ace" -> 1;
            case "champion" -> 2;
            case "master" -> 3;
            case "legend" -> 4;
            default -> 0;
        };
    }

    private static void announceRankUpgrade(ServerPlayerEntity player, String rank) {
        if (player.getServer() == null) return;
        String displayRank = switch (rank) {
            case "ace" -> "ACE";
            case "champion" -> "CHAMPION";
            case "master" -> "MASTER";
            case "legend" -> "LEGEND";
            default -> rank.toUpperCase();
        };
        Formatting rankColor = switch (rank) {
            case "ace" -> Formatting.AQUA;
            case "champion" -> Formatting.LIGHT_PURPLE;
            case "master" -> Formatting.GOLD;
            case "legend" -> Formatting.YELLOW;
            default -> Formatting.WHITE;
        };

        MutableText line = Text.literal("✦ ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                .append(Text.literal(player.getGameProfile().getName()).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(" just upgraded to ").formatted(Formatting.GRAY))
                .append(Text.literal(displayRank).formatted(rankColor, Formatting.BOLD))
                .append(Text.literal("! ").formatted(Formatting.GRAY))
                .append(Text.literal("✦").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        player.getServer().getPlayerManager().broadcast(line, false);
        player.getServer().getPlayerManager().broadcast(
                Text.literal("Drop a GG in chat! ✦").formatted(Formatting.LIGHT_PURPLE), false
        );
    }

    /** Detect live LuckPerms promotions without requiring a relog. */
    public static void tick(MinecraftServer server) {
        if (++rankSyncTicks < 40) return; // every 2 seconds
        rankSyncTicks = 0;
        boolean changed = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            changed |= syncRank(player, false);
        }
        if (changed) PlayerDataStore.save();
    }

    public static String activeTagJson(ServerPlayerEntity player) {
        syncRank(player, false);
        ServerConfig.TagDefinition definition = find(PlayerDataStore.get(player.getUuid()).activeTag);
        return definition == null ? null : definition.tagJson();
    }

    /** Applies the active tag to the vanilla nameplate/tab-list scoreboard channel. */
    public static void apply(ServerPlayerEntity player) {
        try {
            if (!CobbleClubServer.config().scoreboardTagsEnabled || player.getServer() == null) return;
            Scoreboard scoreboard = player.getServer().getScoreboard();
            String holder = player.getGameProfile().getName();
            Team current = scoreboard.getScoreHolderTeam(holder);
            if (current != null && (current.getName().startsWith("cc_tag_") || current.getName().startsWith("cc_glow_"))) scoreboard.clearTeam(holder);

            PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
            ServerConfig.TagDefinition definition = find(data.activeTag);
            ServerConfig.GlowDefinition glow = findGlow(data.wardrobeHidden ? null : data.glow);
            if (definition == null && glow == null) return;
            String teamId = definition != null ? teamId(definition.id()) : glowTeamId(glow.id());
            Team team = scoreboard.getTeam(teamId);
            if (team == null) team = scoreboard.addTeam(teamId);
            MutableText prefix;
            if (definition != null) {
                try { prefix = Text.Serialization.fromJson(definition.tagJson(), player.getRegistryManager()); }
                catch (Exception ignored) { prefix = null; }
                if (prefix == null) prefix = Text.literal("[" + definition.id() + "]");
                MutableText safePrefix = Text.empty();
                safePrefix.append(prefix);
                safePrefix.append(Text.literal(" "));
                team.setPrefix(safePrefix);
            } else team.setPrefix(Text.empty());
            team.setColor(glow == null || glow.colors().isEmpty() ? Formatting.WHITE : nearestFormatting(glow.colors().get(0)));
            scoreboard.addScoreHolderToTeam(holder, team);
        } catch (Exception error) {
            CobbleClubServer.LOGGER.error("Could not apply tag for {}; login will continue", player.getGameProfile().getName(), error);
        }
    }

    public static boolean buy(ServerPlayerEntity player, String id) {
        ServerConfig.TagDefinition definition = find(id);
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        if (definition == null || definition.price() <= 0 || owned(data, definition)) return false;
        // Rank/staff tags are never purchasable even if a config is accidentally edited.
        if (isRestrictedRankTag(definition.id())) return false;
        if (!EconomyService.withdraw(player, definition.price())) return false;
        data.ownedTags.add(definition.id());
        data.revision++;
        PlayerDataStore.save();
        player.sendMessage(Text.literal("Unlocked tag " + definition.id() + ". Open /tags to equip it."), false);
        sendState(player, data.revision);
        return true;
    }

    public static boolean grant(ServerPlayerEntity player, String id) {
        ServerConfig.TagDefinition definition = find(id);
        if (definition == null || isRestrictedRankTag(id)) return false;
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        boolean changed = data.ownedTags.add(definition.id());
        if (changed) { data.revision++; PlayerDataStore.save(); }
        return changed;
    }

    private static void sendState(ServerPlayerEntity player, int revision) {
        if (!ServerPlayNetworking.canSend(player, Payloads.TagsState.ID)) return;
        TagsStateMsg state = new TagsStateMsg(1, Math.max(1, revision), entries(player));
        ServerPlayNetworking.send(player, new Payloads.TagsState(TagsProtocol.INSTANCE.encode(state)));
    }

    private static List<TagsEntry> entries(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData playerData = PlayerDataStore.get(player.getUuid());
        playerData.normalize();
        String active = playerData.activeTag;
        Map<String, Integer> owners = new HashMap<>();
        for (PlayerDataStore.PlayerData data : PlayerDataStore.all().values()) if (data.activeTag != null) owners.merge(data.activeTag, 1, Integer::sum);
        List<TagsEntry> result = new ArrayList<>();
        for (ServerConfig.TagDefinition definition : CobbleClubServer.config().tags) {
            if (definition == null || definition.id() == null || definition.tagJson() == null) continue;
            result.add(new TagsEntry(definition.id(), definition.tagJson(),
                    definition.description() == null ? List.of() : definition.description(), definition.category(),
                    selectable(player, playerData, definition), definition.id().equals(active), owners.getOrDefault(definition.id(), 0)));
        }
        return result;
    }

    private static ServerConfig.TagDefinition find(String id) {
        if (id == null) return null;
        for (ServerConfig.TagDefinition definition : CobbleClubServer.config().tags) if (definition != null && id.equals(definition.id())) return definition;
        return null;
    }

    private static ServerConfig.GlowDefinition findGlow(String id) {
        if (id == null) return null;
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) if (definition != null && id.equals(definition.id())) return definition;
        return null;
    }

    private static Formatting nearestFormatting(int rgb) {
        Formatting best = Formatting.WHITE;
        long bestDistance = Long.MAX_VALUE;
        int wanted = rgb & 0xFFFFFF;
        for (Formatting candidate : Formatting.values()) {
            Integer color = candidate.getColorValue();
            if (color == null) continue;
            int dr = ((wanted >> 16) & 255) - ((color >> 16) & 255);
            int dg = ((wanted >> 8) & 255) - ((color >> 8) & 255);
            int db = (wanted & 255) - (color & 255);
            long distance = (long)dr * dr + (long)dg * dg + (long)db * db;
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        return best;
    }

    private static boolean owned(PlayerDataStore.PlayerData data, ServerConfig.TagDefinition definition) {
        return definition.ownedByDefault() || data.ownedTags.contains(definition.id());
    }

    private static boolean selectable(ServerPlayerEntity player, PlayerDataStore.PlayerData data, ServerConfig.TagDefinition definition) {
        if (!RankAccessService.canUseRankTag(player, definition.id())) return false;
        if (isRestrictedRankTag(definition.id())) return true;
        return owned(data, definition);
    }

    private static boolean isRestrictedRankTag(String id) {
        return "newb".equals(id) || "ace".equals(id) || "champion".equals(id) || "master".equals(id)
                || "legend".equals(id) || "mod".equals(id) || "admin".equals(id);
    }

    private static String teamId(String id) {
        String safe = id == null ? "tag" : id.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        String result = "cc_tag_" + safe;
        return result.length() <= 16 ? result : result.substring(0, 16);
    }

    private static String glowTeamId(String id) {
        String safe = id == null ? "glow" : id.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        String result = "cc_glow_" + safe;
        return result.length() <= 16 ? result : result.substring(0, 16);
    }
}
