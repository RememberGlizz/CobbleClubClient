/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2338
 *  net.minecraft.class_3218
 *  net.minecraft.class_5285
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.cobbleclub.server.mixin;

import com.cobbleclub.server.world.ManagedWorldService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class ManagedServerWorldDataMixin {

    @Inject(
            method = "getSeed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbleclub$managedSeed(CallbackInfoReturnable<Long> cir) {
        Long seed = ManagedWorldService.seedFor(
                (ServerWorld) (Object) this
        );

        if (seed != null) {
            cir.setReturnValue(seed);
        }
    }

    @Inject(
            method = "setSpawnPos",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbleclub$independentSpawn(
            BlockPos pos,
            float angle,
            CallbackInfo ci
    ) {
        if (ManagedWorldService.captureManagedSpawn(
                (ServerWorld) (Object) this,
                pos,
                angle
        )) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/GeneratorOptions;getSeed()J"
            )
    )
    private long cobbleclub$managedConstructorSeed(GeneratorOptions options) {
        Long seed = ManagedWorldService.activeConstructionSeed();

        return seed == null
                ? options.getSeed()
                : seed.longValue();
    }
}