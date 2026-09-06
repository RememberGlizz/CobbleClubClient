package com.cobbleclub.server.service;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.config.ServerConfig;
import com.cobbleclub.server.network.Payloads;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class CatalogService {
    private static final Gson GSON = new Gson();

    private CatalogService() {}

    public static void openPokemonSkins(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, Payloads.PokemonSkins.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerPlayNetworking.send(player, new Payloads.PokemonSkins(GSON.toJson(CobbleClubServer.config().pokemonCatalog)));
    }

    public static void openGear(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, Payloads.GearCatalog.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        List<Payloads.GearSet> sets = new ArrayList<>();
        for (ServerConfig.GearSetDefinition definition : CobbleClubServer.config().gearSets) {
            if (definition == null || definition.id() == null) continue;
            List<ItemStack> items = new ArrayList<>();
            if (definition.items() != null) {
                for (String itemId : definition.items()) {
                    ItemStack custom = ClubItems.stack(itemId, 1);
                    if (!custom.isEmpty()) {
                        items.add(custom);
                        continue;
                    }
                    Identifier id = Identifier.tryParse(itemId);
                    if (id == null || !Registries.ITEM.containsId(id)) continue;
                    Item item = Registries.ITEM.get(id);
                    items.add(new ItemStack(item));
                }
            }
            sets.add(new Payloads.GearSet(definition.id(), definition.displayNameJson(), List.copyOf(items)));
        }
        ServerPlayNetworking.send(player, new Payloads.GearCatalog(List.copyOf(sets)));
    }

    public static void openCrate(ServerPlayerEntity player, String crateId) {
        if (!ServerPlayNetworking.canSend(player, Payloads.CratePreview.ID)) {
            CobbleClubServer.requiresClient(player);
            return;
        }
        ServerConfig.CrateDefinition crate = findCrate(crateId);
        if (crate == null) return;
        JsonObject json = GSON.toJsonTree(crate).getAsJsonObject();
        json.addProperty("part", 0);
        json.addProperty("totalParts", 1);
        ServerPlayNetworking.send(player, new Payloads.CratePreview(GSON.toJson(json)));
    }

    public static boolean handleTestReward(ServerPlayerEntity player, Payloads.CrateTestReward request) {
        return CrateService.testReward(player, request.crateId(), request.prizeIndex());
    }

    public static ServerConfig.CrateDefinition findCrate(String id) {
        if (id == null) return null;
        for (ServerConfig.CrateDefinition crate : CobbleClubServer.config().crates) {
            if (crate != null && id.equals(crate.id)) return crate;
        }
        return null;
    }
}
