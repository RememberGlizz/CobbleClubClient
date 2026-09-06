package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
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

import java.util.List;

@Mixin(PistonHandler.class)
public abstract class PistonHandlerMixin {
    @Shadow @Final private World world;
    @Shadow @Final private BlockPos posFrom;
    @Shadow @Final private Direction motionDirection;
    @Shadow @Final private List<BlockPos> movedBlocks;
    @Shadow @Final private List<BlockPos> brokenBlocks;

    @Inject(method = "calculatePush", at = @At("RETURN"), cancellable = true)
    private void cobbleclub$preventCrossClaimPistonMovement(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !(world instanceof ServerWorld serverWorld)) return;

        for (BlockPos moved : movedBlocks) {
            BlockPos destination = moved.offset(motionDirection);
            if (!ClaimsService.sameProtectionZone(serverWorld, posFrom, moved)
                    || !ClaimsService.sameProtectionZone(serverWorld, posFrom, destination)) {
                cir.setReturnValue(false);
                return;
            }
        }
        for (BlockPos broken : brokenBlocks) {
            if (!ClaimsService.sameProtectionZone(serverWorld, posFrom, broken)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
