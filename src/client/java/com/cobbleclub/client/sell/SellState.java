/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2960
 *  net.minecraft.class_7923
 */
package com.cobbleclub.client.sell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

@Environment(value=EnvType.CLIENT)
public final class SellState {
    private static final DecimalFormat COUNT = new DecimalFormat("#,###");
    public final String balanceText;
    public final String currencySymbol;
    public final String currencyName;
    public final String notice;
    public final boolean error;
    public final List<ItemEntry> items;
    private final Set<String> namespaces;

    private SellState(JsonObject json) {
        this.balanceText = SellState.string(json, "balanceText", "");
        this.currencySymbol = SellState.string(json, "currencySymbol", "\u20bd");
        this.currencyName = SellState.string(json, "currencyName", "Pok\u00e9Dollars");
        this.notice = SellState.string(json, "notice", "");
        this.error = SellState.bool(json, "error");
        ArrayList<ItemEntry> parsed = new ArrayList<ItemEntry>();
        HashSet<String> foundNamespaces = new HashSet<String>();
        JsonArray array = json.has("items") && json.get("items").isJsonArray() ? json.getAsJsonArray("items") : new JsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            String id = SellState.string(item, "id", "");
            long price = SellState.number(item, "price", 0L);
            int count = (int)Math.max(0L, Math.min(Integer.MAX_VALUE, SellState.number(item, "count", 0L)));
            if (id.isBlank() || price <= 0L) continue;
            ItemEntry entry = new ItemEntry(id, price, count);
            parsed.add(entry);
            foundNamespaces.add(entry.namespace());
        }
        this.items = List.copyOf(parsed);
        this.namespaces = Set.copyOf(foundNamespaces);
    }

    public static SellState parse(String raw) {
        try {
            JsonElement element = JsonParser.parseString((String)(raw == null ? "{}" : raw));
            return new SellState(element.isJsonObject() ? element.getAsJsonObject() : new JsonObject());
        }
        catch (Exception ignored) {
            return new SellState(new JsonObject());
        }
    }

    public ItemEntry find(String id) {
        if (id == null) {
            return null;
        }
        for (ItemEntry item : this.items) {
            if (!id.equals(item.id)) continue;
            return item;
        }
        return null;
    }

    public boolean hasNamespace(String namespace) {
        return namespace != null && !namespace.isBlank() && this.namespaces.contains(namespace);
    }

    public String money(long amount) {
        return this.currencySymbol + COUNT.format(Math.max(0L, amount)) + " " + this.currencyName;
    }

    public String shortMoney(long amount) {
        return this.currencySymbol + COUNT.format(Math.max(0L, amount));
    }

    private static String string(JsonObject json, String key, String fallback) {
        try {
            return json.has(key) ? json.get(key).getAsString() : fallback;
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static long number(JsonObject json, String key, long fallback) {
        try {
            return json.has(key) ? json.get(key).getAsLong() : fallback;
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).getAsBoolean();
        }
        catch (Exception ignored) {
            return false;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static final class ItemEntry {
        public final String id;
        public final long price;
        public final int count;
        private ItemStack stack;
        private String namespace;
        private String displayName;
        private String searchText;

        ItemEntry(String id, long price, int count) {
            this.id = id;
            this.price = price;
            this.count = count;
        }

        public String namespace() {
            if (this.namespace != null) {
                return this.namespace;
            }
            Identifier identifier = Identifier.tryParse((String)this.id);
            this.namespace = identifier == null ? "" : identifier.getNamespace();
            return this.namespace;
        }

        public String displayName() {
            if (this.displayName != null) {
                return this.displayName;
            }
            ItemStack stack = this.stack();
            this.displayName = stack.isEmpty() ? this.id : stack.getName().getString();
            return this.displayName;
        }

        public String searchText() {
            if (this.searchText == null) {
                this.searchText = (this.id + " " + this.displayName()).toLowerCase(Locale.ROOT).replace('_', ' ');
            }
            return this.searchText;
        }

        public ItemStack stack() {
            if (this.stack != null) {
                return this.stack;
            }
            Identifier identifier = Identifier.tryParse((String)this.id);
            if (identifier == null || !Registries.ITEM.containsId(identifier)) {
                this.stack = ItemStack.EMPTY;
                return this.stack;
            }
            Item item = (Item)Registries.ITEM.get(identifier);
            this.stack = item == null ? ItemStack.EMPTY : new ItemStack((ItemConvertible)item);
            return this.stack;
        }
    }
}

