package com.cobbleclub.cobblethread.mixin;

import com.cobbleclub.cobblethread.runtime.CrossWorldTransfers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ServerPlayer overrides Entity.changeDimension, so it needs its own gate. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTransferMixin {
    @Inject(method = "changeDimension", at = @At("HEAD"), cancellable = true)
    private void cobblethread$queuePlayerCrossWorldTransfer(DimensionTransition transition,
                                                             CallbackInfoReturnable<Entity> callback) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (CrossWorldTransfers.deferIfNeeded(self, transition)) {
            callback.setReturnValue(null);
        }
    }
}
