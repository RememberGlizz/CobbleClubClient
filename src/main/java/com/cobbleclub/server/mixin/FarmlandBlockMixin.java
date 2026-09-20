/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1937
 *  net.minecraft.class_2338
 *  net.minecraft.class_2344
 *  net.minecraft.class_2680
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={FarmlandBlock.class})
public abstract class FarmlandBlockMixin {
    @Inject(method={"onLandedUpon"}, at={@At(value="HEAD")}, cancellable=true)
    private void cobbleclub$protectClaimFarmland(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        ServerPlayerEntity player;
        ServerWorld serverWorld;
        if (!(world instanceof ServerWorld) || !ClaimsService.isClaimed(serverWorld = (ServerWorld)world, pos)) {
            return;
        }
        if (entity instanceof ServerPlayerEntity && ClaimsService.canBuild(player = (ServerPlayerEntity)entity, pos)) {
            return;
        }
        ci.cancel();
    }
}

