package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.PondService;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps pond drops locked until ten seconds after landing;
 * special prizes pay on pickup.
 */
@Mixin(ItemEntity.class)
public abstract class PondItemPickupMixin {

    @Inject(
            method = "onPlayerCollision",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbleclub$pondPickup(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer
                && PondService.interceptPickup(
                serverPlayer,
                (ItemEntity) (Object) this
        )) {
            ci.cancel();
        }
    }
}
