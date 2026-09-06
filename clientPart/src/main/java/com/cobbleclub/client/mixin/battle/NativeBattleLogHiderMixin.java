package com.cobbleclub.client.mixin.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({BattleMessagePane.class})
public class NativeBattleLogHiderMixin {
   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cobbleclub$hideNativeLog(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (BattleConfig.get().enhancedLog && BattleConfig.get().hideNativeLog) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cobbleclub$muteNativeLog(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (BattleConfig.get().enhancedLog && BattleConfig.get().hideNativeLog) {
         cir.setReturnValue(false);
      }

   }
}
