package com.cobbleclub.cobblethread.mixin;

import com.cobbleclub.cobblethread.runtime.CrossWorldTransfers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTransferMixin {
    @Inject(method = "changeDimension", at = @At("HEAD"), cancellable = true)
    private void cobblethread$queueCrossWorldTransfer(DimensionTransition transition,
                                                       CallbackInfoReturnable<Entity> callback) {
        Entity self = (Entity) (Object) this;
        if (CrossWorldTransfers.deferIfNeeded(self, transition)) {
            // Vanilla declares changeDimension nullable. The actual transfer is
            // completed on the compatibility/server lane after the world barrier.
            callback.setReturnValue(null);
        }
    }
}
