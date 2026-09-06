package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
    @Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
    private void cobbleclub$protectClaimFarmland(World world, BlockState state, BlockPos pos,
                                                  Entity entity, float fallDistance, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld) || !ClaimsService.isClaimed(serverWorld, pos)) return;
        if (entity instanceof ServerPlayerEntity player && ClaimsService.canBuild(player, pos)) return;
        // Prevent visitors, mobs and other entities from trampling claimed farmland.
        ci.cancel();
    }
}
