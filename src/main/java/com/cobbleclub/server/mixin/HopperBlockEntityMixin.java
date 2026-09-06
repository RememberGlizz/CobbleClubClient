package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void cobbleclub$stopCrossClaimHopperTransfer(World world, BlockPos pos, BlockState state,
                                                                 HopperBlockEntity blockEntity, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        Direction output = state.get(HopperBlock.FACING);
        BlockPos input = pos.up();
        BlockPos destination = pos.offset(output);
        if (!ClaimsService.sameProtectionZone(serverWorld, pos, input)
                || !ClaimsService.sameProtectionZone(serverWorld, pos, destination)) {
            ci.cancel();
        }
    }
}
