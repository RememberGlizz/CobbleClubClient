package com.cobbleclub.server.mixin;

import com.cobbleclub.server.world.ManagedWorldService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class ManagedWorldSpawnMixin {

    @Inject(
            method = "getSpawnPos",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbleclub$managedSpawnPos(
            CallbackInfoReturnable<BlockPos> cir
    ) {
        World self = (World) (Object) this;

        if (self instanceof ServerWorld serverWorld) {
            BlockPos spawn = ManagedWorldService.spawnFor(serverWorld);

            if (spawn != null) {
                cir.setReturnValue(spawn);
            }
        }
    }

    @Inject(
            method = "getSpawnAngle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbleclub$managedSpawnAngle(
            CallbackInfoReturnable<Float> cir
    ) {
        World self = (World) (Object) this;

        if (self instanceof ServerWorld serverWorld) {
            Float angle = ManagedWorldService.spawnAngleFor(serverWorld);

            if (angle != null) {
                cir.setReturnValue(angle);
            }
        }
    }

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/source/BiomeAccess;<init>(Lnet/minecraft/world/biome/source/BiomeAccess$Storage;J)V"
            ),
            index = 1
    )
    private long cobbleclub$managedBiomeSeed(long originalSeed) {
        Long seed = ManagedWorldService.activeConstructionSeed();

        return seed == null
                ? originalSeed
                : BiomeAccess.hashSeed(seed);
    }
}