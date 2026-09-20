/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1799
 */
package com.cobbleclub.client.wardrobe;

import com.cobbleclub.client.CobbleClubClient;
import com.cobbleclub.client.furniturepreview.FurniturePreviewRenderer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public final class CosmeticRenderState {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<UUID, State>();

    private CosmeticRenderState() {
    }

    public static State get(UUID player) {
        return STATES.getOrDefault(player, State.EMPTY);
    }

    public static void putPreview(UUID player, ItemStack head, ItemStack back, ItemStack balloon) {
        STATES.put(player, new State(CosmeticRenderState.copy(head), CosmeticRenderState.copy(back), CosmeticRenderState.copy(balloon), false, -1));
    }

    public static void remove(UUID player) {
        STATES.remove(player);
    }

    public static void clear() {
        STATES.clear();
    }

    public static void apply(String raw) {
        try {
            JsonObject root = JsonParser.parseString((String)raw).getAsJsonObject();
            UUID player = UUID.fromString(root.get("player").getAsString());
            if (CosmeticRenderState.bool(root, "removed")) {
                STATES.remove(player);
                return;
            }
            boolean hidden = CosmeticRenderState.bool(root, "hidden");
            int glowColor = root.has("glowColor") ? root.get("glowColor").getAsInt() & 0xFFFFFF : -1;
            STATES.put(player, new State(CosmeticRenderState.stack(root, "head"), CosmeticRenderState.stack(root, "back"), CosmeticRenderState.stack(root, "balloon"), hidden, glowColor));
        }
        catch (Exception error) {
            CobbleClubClient.LOGGER.warn("Dropped malformed cosmetic render state", (Throwable)error);
        }
    }

    private static boolean bool(JsonObject root, String key) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsBoolean();
    }

    private static ItemStack stack(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonObject()) {
            return ItemStack.EMPTY;
        }
        JsonObject value = element.getAsJsonObject();
        String material = value.has("material") ? value.get("material").getAsString() : null;
        int model = value.has("model") ? value.get("model").getAsInt() : 0;
        Integer color = value.has("color") ? Integer.valueOf(value.get("color").getAsInt()) : null;
        return FurniturePreviewRenderer.stackFor(material, model, color);
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Environment(value=EnvType.CLIENT)
    public record State(ItemStack head, ItemStack back, ItemStack balloon, boolean hidden, int glowColor) {
        static final State EMPTY = new State(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, false, -1);
    }
}

