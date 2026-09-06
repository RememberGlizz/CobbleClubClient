package com.cobbleclub.client.mixin.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.BattleState;
import com.cobbleclub.client.battle.BattleUtil;
import com.cobbleclub.client.battle.EnhancedBattleLogWidget;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {BattleOverlay.class},
   remap = false
)
public class BattleOverlayMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      remap = true
   )
   private void cobbleclub$resetTileVisibility(class_332 context, class_9779 tickCounter, CallbackInfo ci) {
      BattleState.battleTilesVisible = false;

      try {
         EnhancedBattleLogWidget.renderUnderBattle(context);
      } catch (Throwable var5) {
      }

   }

   @Inject(
      method = {"drawTile"},
      at = {@At("TAIL")}
   )
   private void cobbleclub$recordTileRect(class_332 context, float tickDelta, ActiveClientBattlePokemon active, boolean left, int rank, PokedexEntryProgress dexState, boolean hasCommand, boolean isHovered, boolean isCompact, CallbackInfo ci) {
      ClientBattlePokemon pokemon = active.getBattlePokemon();
      if (pokemon != null) {
         float x = active.getXDisplacement();
         int width = isCompact ? 128 : 140;
         int screenW = class_310.method_1551().method_22683().method_4486();
         if (x + (float)width > 0.0F && x < (float)screenW) {
            BattleState.battleTilesVisible = true;
         }

         if (BattleConfig.get().hoverPanel) {
            int height = isCompact ? 28 : 40;
            int y = 10 + rank * (isCompact ? 30 : 40);

            try {
               ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
               int digit = Character.digit(active.getActorShowdownId().charAt(1), 10);
               int playerNumberOffset = (digit - 1) / 2 * 10;
               int actorsPerSide = battle.getBattleFormat().getBattleType().getActorsPerSide();
               y += left ? playerNumberOffset : (actorsPerSide - 1) * 10 - playerNumberOffset;
            } catch (Throwable var21) {
            }

            boolean opponent = !BattleUtil.isPlayerSide(active.getActor().getSide());
            BattleState.putTileRect(pokemon.getUuid(), new BattleState.TileRect(x, (float)y, (float)width, (float)height, opponent));
         }
      }
   }
}
