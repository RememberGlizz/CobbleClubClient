package com.cobbleclub.server.mixin;

import com.cobbleclub.server.service.ClaimsService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private World world;

    @Inject(method = "collectBlocksAndDamageEntities", at = @At("RETURN"))
    private void cobbleclub$protectClaimBlocksFromExplosions(CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        Explosion self = (Explosion) (Object) this;
        self.getAffectedBlocks().removeIf(pos -> ClaimsService.isClaimed(serverWorld, pos));
    }
}
