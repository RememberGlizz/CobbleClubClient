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
import net.minecraft.class_1799;

@Environment(EnvType.CLIENT)
public final class CosmeticRenderState {
   private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

   private CosmeticRenderState() {}

   public static State get(UUID player) {
      return STATES.getOrDefault(player, State.EMPTY);
   }

   public static void putPreview(UUID player, class_1799 head, class_1799 back, class_1799 balloon) {
      STATES.put(player, new State(copy(head), copy(back), copy(balloon), false, -1));
   }

   public static void remove(UUID player) {
      STATES.remove(player);
   }

   public static void clear() {
      STATES.clear();
   }

   public static void apply(String raw) {
      try {
         JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
         UUID player = UUID.fromString(root.get("player").getAsString());
         if (bool(root, "removed")) {
            STATES.remove(player);
            return;
         }
         boolean hidden = bool(root, "hidden");
         int glowColor = root.has("glowColor") ? (root.get("glowColor").getAsInt() & 0xFFFFFF) : -1;
         STATES.put(player, new State(stack(root, "head"), stack(root, "back"), stack(root, "balloon"), hidden, glowColor));
      } catch (Exception error) {
         CobbleClubClient.LOGGER.warn("Dropped malformed cosmetic render state", error);
      }
   }

   private static boolean bool(JsonObject root, String key) {
      JsonElement value = root.get(key);
      return value != null && value.isJsonPrimitive() && value.getAsBoolean();
   }

   private static class_1799 stack(JsonObject root, String key) {
      JsonElement element = root.get(key);
      if (element == null || !element.isJsonObject()) return class_1799.field_8037;
      JsonObject value = element.getAsJsonObject();
      String material = value.has("material") ? value.get("material").getAsString() : null;
      int model = value.has("model") ? value.get("model").getAsInt() : 0;
      Integer color = value.has("color") ? value.get("color").getAsInt() : null;
      return FurniturePreviewRenderer.stackFor(material, model, color);
   }

   private static class_1799 copy(class_1799 stack) {
      return stack == null || stack.method_7960() ? class_1799.field_8037 : stack.method_7972();
   }

   public record State(class_1799 head, class_1799 back, class_1799 balloon, boolean hidden, int glowColor) {
      static final State EMPTY = new State(class_1799.field_8037, class_1799.field_8037, class_1799.field_8037, false, -1);
   }
}
