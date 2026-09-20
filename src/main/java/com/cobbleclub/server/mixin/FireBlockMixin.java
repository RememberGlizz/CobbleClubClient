/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1937
 *  net.minecraft.class_2338
 *  net.minecraft.class_2358
 *  net.minecraft.class_3218
 *  net.minecraft.class_5819
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={FireBlock.class})
public abstract class FireBlockMixin {
    @Inject(method={"trySpreadingFire"}, at={@At(value="HEAD")}, cancellable=true)
    private void cobbleclub$stopFireGriefInClaims(World world, BlockPos targetPos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
        ServerWorld serverWorld;
        if (world instanceof ServerWorld && ClaimsService.isClaimed(serverWorld = (ServerWorld)world, targetPos)) {
            ci.cancel();
        }
    }
}

