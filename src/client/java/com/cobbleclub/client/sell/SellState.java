package com.cobbleclub.client.sell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SellState {
    private static final DecimalFormat COUNT = new DecimalFormat("#,###");

    public final String balanceText;
    public final String currencySymbol;
    public final String currencyName;
    public final String notice;
    public final boolean error;
    public final List<ItemEntry> items;

    private SellState(JsonObject json) {
        this.balanceText = string(json, "balanceText", "");
        this.currencySymbol = string(json, "currencySymbol", "₽");
        this.currencyName = string(json, "currencyName", "PokéDollars");
        this.notice = string(json, "notice", "");
        this.error = bool(json, "error");

        List<ItemEntry> parsed = new ArrayList<>();
        JsonArray array = json.has("items") && json.get("items").isJsonArray() ? json.getAsJsonArray("items") : new JsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            String id = string(item, "id", "");
            long price = number(item, "price", 0L);
            int count = (int)Math.max(0L, Math.min(Integer.MAX_VALUE, number(item, "count", 0L)));
            if (!id.isBlank() && price > 0L) parsed.add(new ItemEntry(id, price, count));
        }
        this.items = List.copyOf(parsed);
    }

    public static SellState parse(String raw) {
        try {
            JsonElement element = JsonParser.parseString(raw == null ? "{}" : raw);
            return new SellState(element.isJsonObject() ? element.getAsJsonObject() : new JsonObject());
        } catch (Exception ignored) {
            return new SellState(new JsonObject());
        }
    }

    public ItemEntry find(String id) {
        if (id == null) return null;
        for (ItemEntry item : this.items) {
            if (id.equals(item.id)) return item;
        }
        return null;
    }

    public boolean hasNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) return false;
        for (ItemEntry item : this.items) {
            if (namespace.equals(item.namespace())) return true;
        }
        return false;
    }

    public String money(long amount) {
        return this.currencySymbol + COUNT.format(Math.max(0L, amount)) + " " + this.currencyName;
    }

    public String shortMoney(long amount) {
        return this.currencySymbol + COUNT.format(Math.max(0L, amount));
    }

    private static String string(JsonObject json, String key, String fallback) {
        try { return json.has(key) ? json.get(key).getAsString() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static long number(JsonObject json, String key, long fallback) {
        try { return json.has(key) ? json.get(key).getAsLong() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static boolean bool(JsonObject json, String key) {
        try { return json.has(key) && json.get(key).getAsBoolean(); }
        catch (Exception ignored) { return false; }
    }

    public static final class ItemEntry {
        public final String id;
        public final long price;
        public final int count;

        private ItemStack stack;
        private String displayName;
        private String searchText;

        ItemEntry(String id, long price, int count) {
            this.id = id;
            this.price = price;
            this.count = count;
        }

        public String namespace() {
            Identifier identifier = Identifier.tryParse(this.id);
            return identifier == null ? "" : identifier.getNamespace();
        }

        public String displayName() {
            if (this.displayName != null) return this.displayName;
            ItemStack stack = stack();
            this.displayName = stack.isEmpty() ? this.id : stack.getName().getString();
            return this.displayName;
        }

        public String searchText() {
            if (this.searchText == null) {
                this.searchText = (this.id + " " + displayName()).toLowerCase(Locale.ROOT).replace('_', ' ');
            }
            return this.searchText;
        }

        public ItemStack stack() {
            if (this.stack != null) return this.stack;
            Identifier identifier = Identifier.tryParse(this.id);
            if (identifier == null || !Registries.ITEM.containsId(identifier)) {
                this.stack = ItemStack.EMPTY;
                return this.stack;
            }
            Item item = Registries.ITEM.get(identifier);
            this.stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
            return this.stack;
        }
    }
}
