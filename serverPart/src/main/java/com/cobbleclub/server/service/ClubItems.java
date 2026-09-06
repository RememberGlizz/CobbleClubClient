package com.cobbleclub.server.service;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Custom-textured vanilla equipment, avoiding new registry-sync requirements. */
public final class ClubItems {
    private static final Map<String, Definition> DEFINITIONS = definitions();

    private ClubItems() {}

    public static ItemStack stack(String rawId, int amount) {
        Definition definition = DEFINITIONS.get(normalize(rawId));
        if (definition == null) return ItemStack.EMPTY;
        Identifier materialId = Identifier.tryParse(definition.material());
        if (materialId == null || !Registries.ITEM.containsId(materialId)) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(materialId);
        ItemStack stack = new ItemStack(item, Math.max(1, amount));
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(definition.customModelData()));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(definition.displayName()));
        return stack;
    }

    public static boolean give(ServerPlayerEntity player, String id, int amount) {
        ItemStack stack = stack(id, amount);
        if (stack.isEmpty()) return false;
        if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
        player.sendMessage(Text.literal("Received CobbleClub item: " + stack.getName().getString()), false);
        return true;
    }

    public static boolean matches(ItemStack stack, String rawId) {
        Definition definition = DEFINITIONS.get(normalize(rawId));
        if (definition == null || stack == null || stack.isEmpty()) return false;
        Identifier materialId = Identifier.tryParse(definition.material());
        if (materialId == null || !Registries.ITEM.containsId(materialId) || !stack.isOf(Registries.ITEM.get(materialId))) return false;
        CustomModelDataComponent component = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return component != null && component.value() == definition.customModelData();
    }

    public static List<String> ids() { return List.copyOf(DEFINITIONS.keySet()); }

    private static String normalize(String rawId) {
        if (rawId == null) return "";
        String normalized = rawId.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("cobbleclub:") ? normalized.substring("cobbleclub:".length()) : normalized;
    }

    private static Map<String, Definition> definitions() {
        Map<String, Definition> result = new LinkedHashMap<>();
        add(result, "claiming_tool", "CobbleClub Claiming Tool", "minecraft:golden_shovel", 22601);
        add(result, "lightning_saber", "Lightning Saber", "minecraft:diamond_sword", 22201);
        add(result, "shadow_scythe", "Spectral Shadow Scythe", "minecraft:diamond_sword", 22202);
        add(result, "aura_pickaxe", "Aura Crystal Pickaxe", "minecraft:diamond_pickaxe", 22301);
        add(result, "ember_axe", "Ember Wing Axe", "minecraft:diamond_axe", 22401);
        armor(result, "spark", "Spark Vanguard", 22501);
        armor(result, "spectral", "Spectral Phantom", 22502);
        armor(result, "aura", "Aura Guardian", 22503);
        armor(result, "fairy", "Fairy Bloom", 22504);
        return Map.copyOf(result);
    }

    private static void armor(Map<String, Definition> map, String id, String name, int customModelData) {
        add(map, id + "_helmet", name + " Helmet", "minecraft:diamond_helmet", customModelData);
        add(map, id + "_chestplate", name + " Chestplate", "minecraft:diamond_chestplate", customModelData);
        add(map, id + "_leggings", name + " Leggings", "minecraft:diamond_leggings", customModelData);
        add(map, id + "_boots", name + " Boots", "minecraft:diamond_boots", customModelData);
    }

    private static void add(Map<String, Definition> map, String id, String displayName, String material, int customModelData) {
        map.put(id, new Definition(displayName, material, customModelData));
    }

    private record Definition(String displayName, String material, int customModelData) {}
}
