package com.cobbleclub.client.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class RevealedBattleInfo {
   public String abilityName;
   public String heldItemName;
   public boolean heldItemConsumed;
   public final Map<String, RevealedMove> moves = new LinkedHashMap();

   public void recordAbility(String name) {
      if (name != null && !name.isBlank()) {
         this.abilityName = name;
      }

   }

   public void recordHeldItem(String name) {
      if (name != null && !name.isBlank()) {
         this.heldItemName = name;
         this.heldItemConsumed = false;
      }

   }

   public void consumeHeldItem() {
      this.heldItemConsumed = true;
   }

   public void recordMoveUse(String moveId, String displayName, String typeId) {
      if (moveId != null && !moveId.isBlank()) {
         RevealedMove move = (RevealedMove)this.moves.computeIfAbsent(moveId, (id) -> new RevealedMove(displayName, typeId));
         ++move.uses;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class RevealedMove {
      public final String displayName;
      public final String typeId;
      public int uses;

      public RevealedMove(String displayName, String typeId) {
         this.displayName = displayName;
         this.typeId = typeId;
      }
   }
}
