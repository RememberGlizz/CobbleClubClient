/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1937
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2674
 *  net.minecraft.class_3218
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import java.util.List;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHandler.class)
public abstract class PistonHandlerMixin {

    @Shadow
    @Final
    private World world;

    @Shadow
    @Final
    private BlockPos posFrom;

    @Shadow
    @Final
    private Direction motionDirection;

    @Shadow
    @Final
    private List<BlockPos> movedBlocks;

    @Shadow
    @Final
    private List<BlockPos> brokenBlocks;

    @Inject(
            method = "calculatePush",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cobbleclub$preventCrossClaimPistonMovement(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue() || !(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        for (BlockPos moved : this.movedBlocks) {
            BlockPos destination = moved.offset(this.motionDirection);

            if (ClaimsService.sameProtectionZone(serverWorld, this.posFrom, moved)
                    && ClaimsService.sameProtectionZone(serverWorld, this.posFrom, destination)) {
                continue;
            }

            cir.setReturnValue(false);
            return;
        }

        for (BlockPos broken : this.brokenBlocks) {
            if (ClaimsService.sameProtectionZone(serverWorld, this.posFrom, broken)) {
                continue;
            }

            cir.setReturnValue(false);
            return;
        }
    }
}

