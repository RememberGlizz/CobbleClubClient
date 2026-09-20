package com.cobbleclub.client.kits;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KitsState {
    private final Map<String, KitEntry> kits =
            new LinkedHashMap<>();

    public final String balanceText;
    public final String reductionPriceText;
    public final long reductionStepSeconds;
    public final String notice;
    public final boolean error;

    private KitsState(JsonObject json) {
        JsonArray array =
                json.has("kits")
                                && json.get("kits").isJsonArray()
                        ? json.getAsJsonArray("kits")
                        : new JsonArray();

        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject kit =
                    element.getAsJsonObject();

            String id =
                    KitsState.string(
                            kit,
                            "id",
                            ""
                    );

            if (id.isEmpty()) {
                continue;
            }

            this.kits.put(
                    id,
                    new KitEntry(
                            id,
                            KitsState.string(
                                    kit,
                                    "displayName",
                                    id
                            ),
                            KitsState.string(
                                    kit,
                                    "permission",
                                    ""
                            ),
                            KitsState.bool(
                                    kit,
                                    "unlocked"
                            ),
                            KitsState.bool(
                                    kit,
                                    "available"
                            ),
                            KitsState.number(
                                    kit,
                                    "cooldownRemainingSeconds"
                            ),
                            KitsState.number(
                                    kit,
                                    "cooldownTotalSeconds"
                            ),
                            KitsState.number(
                                    kit,
                                    "reductionPurchasedSeconds"
                            ),
                            KitsState.number(
                                    kit,
                                    "reductionMaxSeconds"
                            )
                    )
            );
        }

        this.balanceText =
                KitsState.string(
                        json,
                        "balanceText",
                        "0 PokéDollars"
                );

        this.reductionPriceText =
                KitsState.string(
                        json,
                        "reductionPriceText",
                        "500 PokéDollars"
                );

        this.reductionStepSeconds =
                KitsState.number(
                        json,
                        "reductionStepSeconds"
                );

        this.notice =
                KitsState.string(
                        json,
                        "notice",
                        ""
                );

        this.error =
                KitsState.bool(
                        json,
                        "error"
                );
    }

    public static KitsState parse(String raw) {
        try {
            JsonElement element =
                    JsonParser.parseString(
                            raw == null
                                    ? "{}"
                                    : raw
                    );

            return new KitsState(
                    element.isJsonObject()
                            ? element.getAsJsonObject()
                            : new JsonObject()
            );
        } catch (Exception ignored) {
            return new KitsState(
                    new JsonObject()
            );
        }
    }

    public KitEntry kit(String id) {
        KitEntry entry =
                this.kits.get(id);

        return entry == null
                ? new KitEntry(
                        id,
                        id,
                        "",
                        false,
                        false,
                        0L,
                        0L,
                        0L,
                        0L
                )
                : entry;
    }

    private static boolean bool(
            JsonObject json,
            String key
    ) {
        try {
            return json.has(key)
                    && json.get(key)
                            .getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static long number(
            JsonObject json,
            String key
    ) {
        try {
            return json.has(key)
                    ? json.get(key)
                            .getAsLong()
                    : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String string(
            JsonObject json,
            String key,
            String fallback
    ) {
        try {
            return json.has(key)
                    ? json.get(key)
                            .getAsString()
                    : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Environment(EnvType.CLIENT)
    public static final class KitEntry {
        public final String id;
        public final String displayName;
        public final String permission;
        public final boolean unlocked;
        public final boolean available;
        public final long cooldownRemainingSeconds;
        public final long cooldownTotalSeconds;
        public final long reductionPurchasedSeconds;
        public final long reductionMaxSeconds;

        private final long receivedAtMillis =
                System.currentTimeMillis();

        private KitEntry(
                String id,
                String displayName,
                String permission,
                boolean unlocked,
                boolean available,
                long cooldownRemainingSeconds,
                long cooldownTotalSeconds,
                long reductionPurchasedSeconds,
                long reductionMaxSeconds
        ) {
            this.id = id;
            this.displayName = displayName;
            this.permission = permission;
            this.unlocked = unlocked;
            this.available = available;
            this.cooldownRemainingSeconds =
                    cooldownRemainingSeconds;
            this.cooldownTotalSeconds =
                    cooldownTotalSeconds;
            this.reductionPurchasedSeconds =
                    reductionPurchasedSeconds;
            this.reductionMaxSeconds =
                    reductionMaxSeconds;
        }

        public long secondsRemaining() {
            long elapsed =
                    Math.max(
                            0L,
                            (
                                    System.currentTimeMillis()
                                            - this.receivedAtMillis
                            ) / 1000L
                    );

            return Math.max(
                    0L,
                    this.cooldownRemainingSeconds
                            - elapsed
            );
        }
    }
}
