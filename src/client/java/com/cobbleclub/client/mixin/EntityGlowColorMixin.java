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

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityGlowColorMixin {
    @Inject(method = "method_22861()I", at = @At("HEAD"), cancellable = true, remap = false)
    private void cobbleclub$exactGlowColor(CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (self instanceof AbstractClientPlayerEntity player) {
            int color = CosmeticRenderState.get(player.getUuid()).glowColor();
            if (color >= 0) cir.setReturnValue(color & 0xFFFFFF);
        }
    }
}
