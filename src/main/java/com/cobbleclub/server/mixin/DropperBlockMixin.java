/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2315
 *  net.minecraft.class_2325
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2680
 *  net.minecraft.class_2769
 *  net.minecraft.class_3218
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DropperBlock.class})
public abstract class DropperBlockMixin {
    @Inject(method={"dispense"}, at={@At(value="HEAD")}, cancellable=true)
    private void cobbleclub$stopCrossClaimDropping(ServerWorld world, BlockState state, BlockPos pos, CallbackInfo ci) {
        Direction facing = (Direction)state.get((Property)DispenserBlock.FACING);
        if (!ClaimsService.sameProtectionZone(world, pos, pos.offset(facing))) {
            ci.cancel();
        }
    }
}

