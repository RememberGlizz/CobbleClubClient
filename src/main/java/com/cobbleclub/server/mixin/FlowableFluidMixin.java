/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1936
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2680
 *  net.minecraft.class_3218
 *  net.minecraft.class_3609
 *  net.minecraft.class_3610
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.world.WorldAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={FlowableFluid.class})
public abstract class FlowableFluidMixin {
    @Inject(method={"flow"}, at={@At(value="HEAD")}, cancellable=true)
    private void cobbleclub$stopFluidCrossingClaimBoundary(WorldAccess world, BlockPos targetPos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        if (!(world instanceof ServerWorld)) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld)world;
        BlockPos sourcePos = targetPos.offset(direction.getOpposite());
        if (!ClaimsService.sameProtectionZone(serverWorld, sourcePos, targetPos)) {
            ci.cancel();
        }
    }
}

