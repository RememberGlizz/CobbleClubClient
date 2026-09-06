package com.cobbleclub.client.mixin;

import com.cobbleclub.client.wardrobe.CosmeticRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forces CobbleClub's selected glow RGB on the client, so scoreboard/nameplate
 * mods cannot collapse every outline back to vanilla white. */
@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityGlowColorMixin {
   @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
   private void cobbleclub$exactGlowColor(CallbackInfoReturnable<Integer> cir) {
      Object self = this;
      if (self instanceof AbstractClientPlayerEntity player) {
         int color = CosmeticRenderState.get(player.getUuid()).glowColor();
         if (color >= 0) cir.setReturnValue(color & 0xFFFFFF);
      }
   }
}
