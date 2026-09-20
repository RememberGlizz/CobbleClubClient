/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1927
 *  net.minecraft.class_1937
 *  net.minecraft.class_3218
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow
    @Final
    private World world;

    @Inject(
            method = "collectBlocksAndDamageEntities",
            at = @At("RETURN")
    )
    private void cobbleclub$protectClaimBlocksFromExplosions(CallbackInfo ci) {
        World entityWorld = this.world;

        if (!(entityWorld instanceof ServerWorld)) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) entityWorld;
        Explosion self = (Explosion) (Object) this;

        self.getAffectedBlocks().removeIf(
                pos -> ClaimsService.isClaimed(serverWorld, pos)
        );
    }
}

