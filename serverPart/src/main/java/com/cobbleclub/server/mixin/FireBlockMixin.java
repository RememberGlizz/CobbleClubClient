package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(method = "trySpreadingFire", at = @At("HEAD"), cancellable = true)
    private void cobbleclub$stopFireGriefInClaims(World world, BlockPos targetPos, int spreadFactor,
                                                   Random random, int currentAge, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld && ClaimsService.isClaimed(serverWorld, targetPos)) {
            ci.cancel();
        }
    }
}
