package com.cobbleclub.client.mixin.battle;

import com.cobbleclub.client.battle.EnhancedBattleLogWidget;
import com.cobbleclub.client.battle.HoverInfoPanel;
import com.cobbleclub.client.battle.MoveBadgeRects;
import com.cobbleclub.client.battle.MoveTooltipState;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleGeneralActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleTargetSelection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({BattleGUI.class})
public class BattleGuiMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void cobbleclub$beginBadgeFrame(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      MoveBadgeRects.beginFrame();
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void cobbleclub$renderOverlays(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      HoverInfoPanel.renderHovered(graphics, mouseX, mouseY);
      MoveTooltipState.flush(graphics);
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cobbleclub$logMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == 0 && MoveBadgeRects.contains(mouseX, mouseY)) {
         cir.setReturnValue(true);
      } else {
         if (button == 0) {
            BattleGUI self = (BattleGUI)(Object)this;

            for(Element child : self.children()) {
               if (child instanceof BattleActionSelection) {
                  BattleActionSelection selection = (BattleActionSelection)child;
                  boolean consumed = selection.mouseClicked(mouseX, mouseY, button);
                  if (consumed || cobbleclub$actsWithoutConsuming(selection, mouseX, mouseY)) {
                     cir.setReturnValue(true);
                     return;
                  }
                  break;
               }
            }
         }

         if (EnhancedBattleLogWidget.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
         }

      }
   }

   @Unique
   private static boolean cobbleclub$actsWithoutConsuming(BattleActionSelection selection, double mouseX, double mouseY) {
      if (!(selection instanceof BattleMoveSelection move)) {
         if (selection instanceof BattleGeneralActionSelection general) {
            return general.getBackButton().isHovered(mouseX, mouseY);
         } else if (selection instanceof BattleTargetSelection target) {
            return target.getBackButton().isHovered(mouseX, mouseY);
         } else {
            return false;
         }
      } else {
         return move.getBackButton().isHovered(mouseX, mouseY) || move.getShiftButton().isHovered(mouseX, mouseY) || move.getGimmickButtons().stream().anyMatch((gimmick) -> gimmick.isHovered(mouseX, mouseY));
      }
   }

   @Inject(
      method = {"mouseDragged"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cobbleclub$logMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
      if (EnhancedBattleLogWidget.mouseDragged(mouseX, mouseY, button)) {
         cir.setReturnValue(true);
      }

   }
}
