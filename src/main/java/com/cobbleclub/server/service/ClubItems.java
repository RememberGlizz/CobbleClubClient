/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1890
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_5455
 *  net.minecraft.class_5819
 *  net.minecraft.class_7923
 *  net.minecraft.class_9280
 *  net.minecraft.class_9334
 */
package com.cobbleclub.server.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public final class ClubItems {
    private static final Map<String, Definition> DEFINITIONS = ClubItems.definitions();
    private static final List<String> ARMOR_SETS = List.of("spark", "spectral", "aura", "fairy", "adventure");
    private static final List<String> ARMOR_PIECES = List.of("helmet", "chestplate", "leggings", "boots");

    private ClubItems() {
    }

    public static ItemStack stack(String rawId, int amount) {
        Definition definition = DEFINITIONS.get(ClubItems.normalize(rawId));
        if (definition == null) {
            return ItemStack.EMPTY;
        }

        Identifier materialId = Identifier.tryParse(definition.material());
        if (materialId == null || !Registries.ITEM.containsId(materialId)) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(materialId);
        ItemStack stack = new ItemStack(item, Math.max(1, amount));

        stack.set(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                new CustomModelDataComponent(definition.customModelData())
        );
        stack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.literal(definition.displayName())
        );

        if ("pond_rod".equals(ClubItems.normalize(rawId))) {
            stack.set(DataComponentTypes.MAX_DAMAGE, 2048);
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        return stack;
    }

    public static boolean give(ServerPlayerEntity player, String id, int amount) {
        ItemStack stack = ClubItems.stack(id, amount);
        if (stack.isEmpty()) {
            return false;
        }

        ClubItems.giveStack(player, stack);
        player.sendMessage(
                Text.literal("Received CobbleClub item: " + stack.getName().getString()),
                false
        );
        return true;
    }

    public static boolean giveArmorSet(ServerPlayerEntity player, String rawSet) {
        String set = ClubItems.normalize(rawSet);
        if (!ARMOR_SETS.contains(set)) {
            return false;
        }

        int power = ClubItems.enchantPower(set);
        boolean gaveAny = false;

        for (String piece : ARMOR_PIECES) {
            ItemStack stack = ClubItems.stack(set + "_" + piece, 1);
            if (stack.isEmpty()) {
                return false;
            }

            if (power > 0) {
                try {
                    stack = EnchantmentHelper.enchant(
                            Random.create(),
                            stack,
                            power,
                            player.getServerWorld().getServer().getRegistryManager(),
                            Optional.empty()
                    );
                } catch (Throwable throwable) {
                    // Preserve original behavior: if enchanting fails, give the base armor item.
                }
            }

            ClubItems.giveStack(player, stack);
            gaveAny = true;
        }

        if (gaveAny) {
            player.sendMessage(
                    Text.literal("Received CobbleClub armor set: " + ClubItems.armorDisplayName(set)),
                    false
            );
        }

        return gaveAny;
    }

    public static boolean matches(ItemStack stack, String rawId) {
        Definition definition = DEFINITIONS.get(ClubItems.normalize(rawId));
        if (definition == null || stack == null || stack.isEmpty()) {
            return false;
        }

        Identifier materialId = Identifier.tryParse(definition.material());
        if (materialId == null
                || !Registries.ITEM.containsId(materialId)
                || !stack.isOf(Registries.ITEM.get(materialId))) {
            return false;
        }

        CustomModelDataComponent component = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return component != null && component.value() == definition.customModelData();
    }

    public static List<String> ids() {
        return List.copyOf(DEFINITIONS.keySet());
    }

    public static List<String> armorSetIds() {
        return ARMOR_SETS;
    }

    public static String armorTier(String rawSet) {
        return switch (ClubItems.normalize(rawSet)) {
            case "spark" -> "Gold";
            case "spectral" -> "Iron";
            case "aura", "adventure" -> "Diamond";
            case "fairy" -> "Netherite";
            default -> "Unknown";
        };
    }

    private static int enchantPower(String set) {
        return switch (set) {
            case "spark" -> 15;
            case "spectral" -> 20;
            case "aura", "adventure" -> 25;
            case "fairy" -> 30;
            default -> 0;
        };
    }

    private static String armorDisplayName(String set) {
        return switch (set) {
            case "spark" -> "Spark Vanguard (Gold tier)";
            case "spectral" -> "Spectral Phantom (Iron tier)";
            case "aura" -> "Aura Guardian (Diamond tier)";
            case "fairy" -> "Fairy Bloom (Netherite tier)";
            case "adventure" -> "CobbleClub Adventure (Diamond tier)";
            default -> set;
        };
    }

    private static void giveStack(ServerPlayerEntity player, ItemStack stack) {
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    private static String normalize(String rawId) {
        if (rawId == null) {
            return "";
        }

        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("cobbleclub:")
                ? normalized.substring("cobbleclub:".length())
                : normalized;
    }

    private static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> result = new LinkedHashMap<>();

        ClubItems.add(result, "claiming_tool", "CobbleClub Claiming Tool", "minecraft:golden_shovel", 22601);
        ClubItems.add(result, "pond_rod", "✦ CobbleClub Angler Rod", "minecraft:fishing_rod", 22602);
        ClubItems.add(result, "lightning_saber", "Lightning Saber", "minecraft:diamond_sword", 22201);
        ClubItems.add(result, "shadow_scythe", "Spectral Shadow Scythe", "minecraft:diamond_sword", 22202);
        ClubItems.add(result, "aura_pickaxe", "Aura Crystal Pickaxe", "minecraft:diamond_pickaxe", 22301);
        ClubItems.add(result, "ember_axe", "Ember Wing Axe", "minecraft:diamond_axe", 22401);

        ClubItems.armor(result, "spark", "Spark Vanguard", 22501, "golden");
        ClubItems.armor(result, "spectral", "Spectral Phantom", 22502, "iron");
        ClubItems.armor(result, "aura", "Aura Guardian", 22503, "diamond");
        ClubItems.armor(result, "fairy", "Fairy Bloom", 22504, "netherite");
        ClubItems.armor(result, "adventure", "CobbleClub Adventure", 22505, "diamond");

        return Map.copyOf(result);
    }

    private static void armor(
            Map<String, Definition> map,
            String id,
            String name,
            int customModelData,
            String materialPrefix
    ) {
        ClubItems.add(map, id + "_helmet", name + " Helmet", "minecraft:" + materialPrefix + "_helmet", customModelData);
        ClubItems.add(map, id + "_chestplate", name + " Chestplate", "minecraft:" + materialPrefix + "_chestplate", customModelData);
        ClubItems.add(map, id + "_leggings", name + " Leggings", "minecraft:" + materialPrefix + "_leggings", customModelData);
        ClubItems.add(map, id + "_boots", name + " Boots", "minecraft:" + materialPrefix + "_boots", customModelData);
    }

    private static void add(
            Map<String, Definition> map,
            String id,
            String displayName,
            String material,
            int customModelData
    ) {
        map.put(id, new Definition(displayName, material, customModelData));
    }

    private record Definition(String displayName, String material, int customModelData) {
    }
}
