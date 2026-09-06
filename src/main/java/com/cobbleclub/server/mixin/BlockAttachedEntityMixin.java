package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void cobbleclub$protectFramesPaintingsAndKnots(DamageSource source, float amount,
                                                            CallbackInfoReturnable<Boolean> cir) {
        BlockAttachedEntity self = (BlockAttachedEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld world) || !ClaimsService.isClaimed(world, self.getBlockPos())) return;
        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity player) {
            if (!ClaimsService.canAttackEntity(player, self)) cir.setReturnValue(false);
        } else {
            // Environmental/projectile damage without an authorized player must not pop protected decorations.
            cir.setReturnValue(false);
        }
    }
}
