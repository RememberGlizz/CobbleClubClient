package com.cobbleclub.client.contracts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ContractsState {
    public final String balanceText;
    public final String refreshText;
    public final String notice;
    public final boolean error;
    public final List<Entry> refreshing;
    public final List<Entry> milestones;
    public final List<Entry> journey;

    private ContractsState(JsonObject json) {
        this.balanceText = string(json, "balanceText", "");
        this.refreshText = string(json, "refreshText", "");
        this.notice = string(json, "notice", "");
        this.error = bool(json, "error");
        this.refreshing = parseEntries(json, "refreshing");
        this.milestones = parseEntries(json, "milestones");
        this.journey = parseEntries(json, "journey");
    }

    public static ContractsState parse(String raw) {
        try {
            JsonElement element = JsonParser.parseString(raw == null ? "{}" : raw);
            return new ContractsState(element.isJsonObject() ? element.getAsJsonObject() : new JsonObject());
        } catch (Exception ignored) {
            return new ContractsState(new JsonObject());
        }
    }

    public List<Entry> entries(String tab) {
        if ("milestones".equals(tab)) return milestones;
        if ("journey".equals(tab)) return journey;
        return refreshing;
    }

    public Entry find(String id) {
        if (id == null) return null;
        for (Entry entry : refreshing) if (id.equals(entry.id)) return entry;
        for (Entry entry : milestones) if (id.equals(entry.id)) return entry;
        for (Entry entry : journey) if (id.equals(entry.id)) return entry;
        return null;
    }

    private static List<Entry> parseEntries(JsonObject root, String key) {
        ArrayList<Entry> out = new ArrayList<>();
        JsonArray array = root.has(key) && root.get(key).isJsonArray() ? root.getAsJsonArray(key) : new JsonArray();

        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject json = element.getAsJsonObject();
            String id = string(json, "id", "");
            if (id.isBlank()) continue;

            ArrayList<Objective> objectives = new ArrayList<>();
            JsonArray objectiveArray = json.has("objectives") && json.get("objectives").isJsonArray()
                    ? json.getAsJsonArray("objectives")
                    : new JsonArray();
            for (JsonElement objectiveElement : objectiveArray) {
                if (!objectiveElement.isJsonObject()) continue;
                JsonObject objective = objectiveElement.getAsJsonObject();
                objectives.add(new Objective(
                        string(objective, "label", "Objective"),
                        number(objective, "current", 0L),
                        Math.max(1L, number(objective, "target", 1L)),
                        bool(objective, "complete")
                ));
            }

            out.add(new Entry(
                    id,
                    string(json, "title", "Contract"),
                    string(json, "subtitle", ""),
                    string(json, "description", ""),
                    string(json, "reward", ""),
                    string(json, "status", ""),
                    number(json, "progress", 0L),
                    Math.max(1L, number(json, "target", 1L)),
                    bool(json, "claimable"),
                    bool(json, "claimed"),
                    bool(json, "locked"),
                    bool(json, "autoReward"),
                    string(json, "region", ""),
                    (int)Math.max(0L, Math.min(Integer.MAX_VALUE, number(json, "badgeNumber", 0L))),
                    List.copyOf(objectives)
            ));
        }

        return List.copyOf(out);
    }

    private static String string(JsonObject json, String key, String fallback) {
        try {
            return json.has(key) ? json.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long number(JsonObject json, String key, long fallback) {
        try {
            return json.has(key) ? json.get(key).getAsLong() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Environment(EnvType.CLIENT)
    public record Entry(
            String id,
            String title,
            String subtitle,
            String description,
            String reward,
            String status,
            long progress,
            long target,
            boolean claimable,
            boolean claimed,
            boolean locked,
            boolean autoReward,
            String region,
            int badgeNumber,
            List<Objective> objectives
    ) {
        public double ratio() {
            if (target <= 0L) return 0.0D;
            return Math.max(0.0D, Math.min(1.0D, (double)progress / (double)target));
        }
    }

    @Environment(EnvType.CLIENT)
    public record Objective(String label, long current, long target, boolean complete) {
        public double ratio() {
            return target <= 0L ? 0.0D : Math.max(0.0D, Math.min(1.0D, (double)current / (double)target));
        }
    }
}
