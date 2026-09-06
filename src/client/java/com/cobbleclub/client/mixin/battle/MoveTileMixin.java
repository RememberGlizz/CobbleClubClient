package com.cobbleclub.client.mixin.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.InfoBadge;
import com.cobbleclub.client.battle.MoveBadgeRects;
import com.cobbleclub.client.battle.MoveTooltipState;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.battles.InBattleMove;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {BattleMoveSelection.MoveTile.class},
   remap = false
)
public class MoveTileMixin {
   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void cobbleclub$queueTooltip(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (BattleConfig.get().moveTooltips) {
         BattleMoveSelection.MoveTile self = (BattleMoveSelection.MoveTile)(Object)this;
         int bx = (int)self.getX() + 92 - 8 - 2;
         int by = (int)self.getY() + 2;
         MoveBadgeRects.record(bx, by);
         boolean badgeHovered = InfoBadge.isOver((double)mouseX, (double)mouseY, bx, by);
         float opacity = self.getMoveSelection().getOpacity();
         InfoBadge.draw(context, bx, by, badgeHovered, opacity);
         if (badgeHovered) {
            MoveTemplate template = self.getMoveTemplate();
            if (template != null) {
               InBattleMove move = self.getMove();
               int curPp = move != null ? move.getPp() : template.getMaxPp();
               int maxPp = move != null ? move.getMaxpp() : template.getMaxPp();
               MoveTooltipState.queue(template, self.getElementalType(), curPp, maxPp, mouseX, mouseY);
            }
         }
      }
   }
}
