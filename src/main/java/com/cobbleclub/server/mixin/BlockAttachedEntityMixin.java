package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void cobbleclub$protectFramesPaintingsAndKnots(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerWorld world;

        BlockAttachedEntity self = (BlockAttachedEntity) (Object) this;
        World entityWorld = self.getWorld();

        if (!(entityWorld instanceof ServerWorld)
                || !ClaimsService.isClaimed(
                world = (ServerWorld) entityWorld,
                self.getBlockPos()
        )) {
            return;
        }

        Entity attacker = source.getAttacker();

        if (attacker instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) attacker;

            if (!ClaimsService.canAttackEntity(player, self)) {
                cir.setReturnValue(false);
            }
        } else {
            cir.setReturnValue(false);
        }
    }
}

