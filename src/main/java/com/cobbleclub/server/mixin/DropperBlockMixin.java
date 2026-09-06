package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DropperBlock.class)
public abstract class DropperBlockMixin {
    @Inject(method = "dispense", at = @At("HEAD"), cancellable = true)
    private void cobbleclub$stopCrossClaimDropping(ServerWorld world, BlockState state, BlockPos pos, CallbackInfo ci) {
        Direction facing = state.get(DispenserBlock.FACING);
        if (!ClaimsService.sameProtectionZone(world, pos, pos.offset(facing))) ci.cancel();
    }
}
