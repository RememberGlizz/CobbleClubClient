/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.tags.protocol.TagsActionMsg
 *  com.cobbleclub.clubhouse.tags.protocol.TagsActionType
 *  com.cobbleclub.clubhouse.tags.protocol.TagsCategoryEntry
 *  com.cobbleclub.clubhouse.tags.protocol.TagsEntry
 *  com.cobbleclub.clubhouse.tags.protocol.TagsOpenMsg
 *  com.cobbleclub.clubhouse.tags.protocol.TagsProtocol
 *  com.cobbleclub.clubhouse.tags.protocol.TagsStateMsg
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_2561$class_2562
 *  net.minecraft.class_268
 *  net.minecraft.class_2995
 *  net.minecraft.class_3222
 *  net.minecraft.class_5250
 *  net.minecraft.class_7225$class_7874
 *  net.minecraft.class_8710
 *  net.minecraft.server.MinecraftServer
 */
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
import com.cobbleclub.server.service.EconomyService;
import com.cobbleclub.server.service.RankAccessService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

public final class TagsService {
    private static final long SWITCH_COOLDOWN_MILLIS = 15000L;
    private static int rankSyncTicks;

    private TagsService() {
    }

    public static void open(ServerPlayerEntity player) {
        TagsService.syncRank(player, true);
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.TagsOpen.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerConfig config = CobbleClubServer.config();
        List<TagsCategoryEntry> categories = config.tagCategories.stream().map(value -> new TagsCategoryEntry(value.id(), value.displayName())).toList();
        TagsOpenMsg message = new TagsOpenMsg(1, "{\"text\":\"CobbleClub Tags\",\"color\":\"light_purple\",\"bold\":true}", categories, TagsService.entries(player), false, "{\"text\":\"Use Rank Default\",\"color\":\"yellow\"}");
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.TagsOpen(TagsProtocol.INSTANCE.encode((Object)message)));
    }

    public static void handle(ServerPlayerEntity player, String json) {
        String requested;
        TagsService.syncRank(player, false);
        TagsActionMsg message = (TagsActionMsg)TagsProtocol.INSTANCE.decode(json, TagsActionMsg.class);
        if (message == null || message.getProtocolVersion() != 1 || message.getAction() == null) {
            return;
        }
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        if (message.getAction() == TagsActionType.UNSET) {
            requested = RankAccessService.defaultTag(player);
        } else if (message.getAction() == TagsActionType.SET) {
            requested = message.getTagId();
        } else {
            return;
        }
        ServerConfig.TagDefinition definition = TagsService.find(requested);
        if (definition == null || !TagsService.selectable(player, data, definition)) {
            player.sendMessage((Text)Text.literal((String)"You do not have access to that CobbleClub tag."), false);
            TagsService.sendState(player, data.revision);
            return;
        }
        if (requested.equals(data.activeTag)) {
            return;
        }
        long now = System.currentTimeMillis();
        long wait = 15000L - (now - data.lastTagSwitchMillis);
        if (wait > 0L) {
            long seconds = Math.max(1L, (wait + 999L) / 1000L);
            player.sendMessage((Text)Text.literal((String)("Please wait " + seconds + "s before switching tags again.")), false);
            TagsService.sendState(player, data.revision);
            return;
        }
        data.activeTag = requested;
        data.lastTagSwitchMillis = now;
        ++data.revision;
        PlayerDataStore.save();
        TagsService.apply(player);
        TagsService.sendState(player, data.revision);
    }

    public static boolean syncRank(ServerPlayerEntity player, boolean save) {
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        String previousRank = data.lastTagRank;
        String rank = RankAccessService.group(player);
        String rankDefault = RankAccessService.defaultTagForGroup(rank);
        boolean rankChanged = previousRank == null || !rank.equals(previousRank);
        ServerConfig.TagDefinition active = TagsService.find(data.activeTag);
        boolean invalidActive = active == null || !TagsService.selectable(player, data, active);
        boolean changed = false;
        if (rankChanged || invalidActive) {
            data.activeTag = rankDefault;
            data.lastTagRank = rank;
            ++data.revision;
            changed = true;
        }
        if (changed) {
            TagsService.apply(player);
            if (save) {
                PlayerDataStore.save();
            }
            TagsService.sendState(player, data.revision);
            if (rankChanged && TagsService.isPremiumPromotion(previousRank, rank)) {
                TagsService.announceRankUpgrade(player, rank);
            }
        }
        return changed;
    }

    private static boolean isPremiumPromotion(String previousRank, String newRank) {
        if (previousRank == null || newRank == null) {
            return false;
        }
        int newLevel = TagsService.premiumLevel(newRank);
        return newLevel > 0 && newLevel > TagsService.premiumLevel(previousRank);
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
        if (player.getServer() == null) {
            return;
        }
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
        MutableText line = Text.literal((String)"\u2726 ").formatted(new Formatting[]{Formatting.DARK_PURPLE, Formatting.BOLD}).append((Text)Text.literal((String)player.getGameProfile().getName()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD})).append((Text)Text.literal((String)" just upgraded to ").formatted(Formatting.GRAY)).append((Text)Text.literal((String)displayRank).formatted(new Formatting[]{rankColor, Formatting.BOLD})).append((Text)Text.literal((String)"! ").formatted(Formatting.GRAY)).append((Text)Text.literal((String)"\u2726").formatted(new Formatting[]{Formatting.DARK_PURPLE, Formatting.BOLD}));
        player.getServer().getPlayerManager().broadcast((Text)line, false);
        player.getServer().getPlayerManager().broadcast((Text)Text.literal((String)"Drop a GG in chat! \u2726").formatted(Formatting.LIGHT_PURPLE), false);
    }

    public static void tick(MinecraftServer server) {
        if (++rankSyncTicks < 40) {
            return;
        }
        rankSyncTicks = 0;
        boolean changed = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            changed |= TagsService.syncRank(player, false);
        }
        if (changed) {
            PlayerDataStore.save();
        }
    }

    public static String activeTagJson(ServerPlayerEntity player) {
        TagsService.syncRank(player, false);
        ServerConfig.TagDefinition definition = TagsService.find(PlayerDataStore.get((UUID)player.getUuid()).activeTag);
        return definition == null ? null : definition.tagJson();
    }

    public static void apply(ServerPlayerEntity player) {
        try {
            String holder;
            if (!CobbleClubServer.config().scoreboardTagsEnabled || player.getServer() == null) {
                return;
            }
            Scoreboard scoreboard = player.getServer().getScoreboard();
            Team current = scoreboard.getScoreHolderTeam(holder = player.getGameProfile().getName());
            if (current != null && (current.getName().startsWith("cc_tag_") || current.getName().startsWith("cc_glow_"))) {
                scoreboard.clearTeam(holder);
            }
            PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
            ServerConfig.TagDefinition definition = TagsService.find(data.activeTag);
            ServerConfig.GlowDefinition glow = TagsService.findGlow(data.wardrobeHidden ? null : data.glow);
            if (definition == null && glow == null) {
                return;
            }
            String teamId = definition != null ? TagsService.teamId(definition.id()) : TagsService.glowTeamId(glow.id());
            Team team = scoreboard.getTeam(teamId);
            if (team == null) {
                team = scoreboard.addTeam(teamId);
            }
            if (definition != null) {
                MutableText prefix;
                try {
                    prefix = Text.Serialization.fromJson((String)definition.tagJson(), (RegistryWrapper.WrapperLookup)player.getRegistryManager());
                }
                catch (Exception ignored) {
                    prefix = null;
                }
                if (prefix == null) {
                    prefix = Text.literal((String)("[" + definition.id() + "]"));
                }
                MutableText safePrefix = Text.empty();
                safePrefix.append((Text)prefix);
                safePrefix.append((Text)Text.literal((String)" "));
                team.setPrefix((Text)safePrefix);
            } else {
                team.setPrefix((Text)Text.empty());
            }
            team.setColor(glow == null || glow.colors().isEmpty() ? Formatting.WHITE : TagsService.nearestFormatting(glow.colors().get(0)));
            scoreboard.addScoreHolderToTeam(holder, team);
        }
        catch (Exception error) {
            CobbleClubServer.LOGGER.error("Could not apply tag for {}; login will continue", (Object)player.getGameProfile().getName(), (Object)error);
        }
    }

    public static boolean buy(ServerPlayerEntity player, String id) {
        ServerConfig.TagDefinition definition = TagsService.find(id);
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        if (definition == null || definition.price() <= 0L || TagsService.owned(data, definition)) {
            return false;
        }
        if (TagsService.isRestrictedRankTag(definition.id())) {
            return false;
        }
        if (!EconomyService.withdraw(player, definition.price())) {
            return false;
        }
        data.ownedTags.add(definition.id());
        ++data.revision;
        PlayerDataStore.save();
        player.sendMessage((Text)Text.literal((String)("Unlocked tag " + definition.id() + ". Open /tags to equip it.")), false);
        TagsService.sendState(player, data.revision);
        return true;
    }

    public static boolean grant(ServerPlayerEntity player, String id) {
        ServerConfig.TagDefinition definition = TagsService.find(id);
        if (definition == null || TagsService.isRestrictedRankTag(id)) {
            return false;
        }
        PlayerDataStore.PlayerData data = PlayerDataStore.get(player.getUuid());
        data.normalize();
        boolean changed = data.ownedTags.add(definition.id());
        if (changed) {
            ++data.revision;
            PlayerDataStore.save();
        }
        return changed;
    }

    private static void sendState(ServerPlayerEntity player, int revision) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.TagsState.ID)) {
            return;
        }
        TagsStateMsg state = new TagsStateMsg(1, Math.max(1, revision), TagsService.entries(player));
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.TagsState(TagsProtocol.INSTANCE.encode((Object)state)));
    }

    private static List<TagsEntry> entries(ServerPlayerEntity player) {
        PlayerDataStore.PlayerData playerData = PlayerDataStore.get(player.getUuid());
        playerData.normalize();
        String active = playerData.activeTag;
        HashMap<String, Integer> owners = new HashMap<String, Integer>();
        for (PlayerDataStore.PlayerData data : PlayerDataStore.all().values()) {
            if (data.activeTag == null) continue;
            owners.merge(data.activeTag, 1, Integer::sum);
        }
        ArrayList<TagsEntry> result = new ArrayList<TagsEntry>();
        for (ServerConfig.TagDefinition definition : CobbleClubServer.config().tags) {
            if (definition == null || definition.id() == null || definition.tagJson() == null) continue;
            result.add(new TagsEntry(definition.id(), definition.tagJson(), definition.description() == null ? List.of() : definition.description(), definition.category(), TagsService.selectable(player, playerData, definition), definition.id().equals(active), owners.getOrDefault(definition.id(), 0).intValue()));
        }
        return result;
    }

    private static ServerConfig.TagDefinition find(String id) {
        if (id == null) {
            return null;
        }
        for (ServerConfig.TagDefinition definition : CobbleClubServer.config().tags) {
            if (definition == null || !id.equals(definition.id())) continue;
            return definition;
        }
        return null;
    }

    private static ServerConfig.GlowDefinition findGlow(String id) {
        if (id == null) {
            return null;
        }
        for (ServerConfig.GlowDefinition definition : CobbleClubServer.config().glows) {
            if (definition == null || !id.equals(definition.id())) continue;
            return definition;
        }
        return null;
    }

    private static Formatting nearestFormatting(int rgb) {
        Formatting best = Formatting.WHITE;
        long bestDistance = Long.MAX_VALUE;
        int wanted = rgb & 0xFFFFFF;
        for (Formatting candidate : Formatting.values()) {
            int db;
            int dg;
            int dr;
            long distance;
            Integer color = candidate.getColorValue();
            if (color == null || (distance = (long)(dr = (wanted >> 16 & 0xFF) - (color >> 16 & 0xFF)) * (long)dr + (long)(dg = (wanted >> 8 & 0xFF) - (color >> 8 & 0xFF)) * (long)dg + (long)(db = (wanted & 0xFF) - (color & 0xFF)) * (long)db) >= bestDistance) continue;
            bestDistance = distance;
            best = candidate;
        }
        return best;
    }

    private static boolean owned(PlayerDataStore.PlayerData data, ServerConfig.TagDefinition definition) {
        return definition.ownedByDefault() || data.ownedTags.contains(definition.id());
    }

    private static boolean selectable(ServerPlayerEntity player, PlayerDataStore.PlayerData data, ServerConfig.TagDefinition definition) {
        if (!RankAccessService.canUseRankTag(player, definition.id())) {
            return false;
        }
        if (TagsService.isRestrictedRankTag(definition.id())) {
            return true;
        }
        return TagsService.owned(data, definition);
    }

    private static boolean isRestrictedRankTag(String id) {
        return "newb".equals(id) || "ace".equals(id) || "champion".equals(id) || "master".equals(id) || "legend".equals(id) || "mod".equals(id) || "admin".equals(id);
    }

    private static String teamId(String id) {
        String safe = id == null ? "tag" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        String result = "cc_tag_" + safe;
        return result.length() <= 16 ? result : result.substring(0, 16);
    }

    private static String glowTeamId(String id) {
        String safe = id == null ? "glow" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        String result = "cc_glow_" + safe;
        return result.length() <= 16 ? result : result.substring(0, 16);
    }
}

