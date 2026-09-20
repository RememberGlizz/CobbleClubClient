/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_7923
 *  net.minecraft.class_8710
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.network.Payloads;
import com.cobbleclub.server.service.ClubItems;
import com.cobbleclub.server.service.CrateService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.CustomPayload;

public final class CatalogService {
    private static final Gson GSON = new Gson();
    private static final List<String> DEFAULT_ADVENTURE = List.of("minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots");
    private static final List<String> TEXTURED_ADVENTURE = List.of("cobbleclub:adventure_helmet", "cobbleclub:adventure_chestplate", "cobbleclub:adventure_leggings", "cobbleclub:adventure_boots");

    private CatalogService() {
    }

    public static void openPokemonSkins(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.PokemonSkins.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.PokemonSkins(GSON.toJson((Object)CobbleClubServer.config().pokemonCatalog)));
    }

    public static void openGear(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.GearCatalog.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ArrayList<Payloads.GearSet> sets = new ArrayList<Payloads.GearSet>();
        for (ServerConfig.GearSetDefinition definition : CobbleClubServer.config().gearSets) {
            if (definition == null || definition.id() == null) continue;
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();
            List<String> configuredItems = definition.items();
            if ("adventure".equals(definition.id()) && DEFAULT_ADVENTURE.equals(configuredItems)) {
                configuredItems = TEXTURED_ADVENTURE;
            }
            if (configuredItems != null) {
                for (String itemId : configuredItems) {
                    ItemStack custom = ClubItems.stack(itemId, 1);
                    if (!custom.isEmpty()) {
                        items.add(custom);
                        continue;
                    }
                    Identifier id = Identifier.tryParse((String)itemId);
                    if (id == null || !Registries.ITEM.containsId(id)) continue;
                    Item item = (Item)Registries.ITEM.get(id);
                    items.add(new ItemStack((ItemConvertible)item));
                }
            }
            sets.add(new Payloads.GearSet(definition.id(), definition.displayNameJson(), List.copyOf(items)));
        }
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.GearCatalog(List.copyOf(sets)));
    }

    public static void openCrate(ServerPlayerEntity player, String crateId) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, Payloads.CratePreview.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerConfig.CrateDefinition crate = CatalogService.findCrate(crateId);
        if (crate == null) {
            return;
        }
        JsonObject json = GSON.toJsonTree((Object)crate).getAsJsonObject();
        json.addProperty("part", (Number)0);
        json.addProperty("totalParts", (Number)1);
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new Payloads.CratePreview(GSON.toJson((JsonElement)json)));
    }

    public static boolean handleTestReward(ServerPlayerEntity player, Payloads.CrateTestReward request) {
        return CrateService.testReward(player, request.crateId(), request.prizeIndex());
    }

    public static ServerConfig.CrateDefinition findCrate(String id) {
        if (id == null) {
            return null;
        }
        for (ServerConfig.CrateDefinition crate : CobbleClubServer.config().crates) {
            if (crate == null || !id.equals(crate.id)) continue;
            return crate;
        }
        return null;
    }
}

