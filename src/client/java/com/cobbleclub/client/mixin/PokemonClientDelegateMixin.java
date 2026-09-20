package com.cobbleclub.client.mixin;

import com.cobbleclub.client.KotlinHelperFabric;
import com.cobbleclub.client.render.renderTypes.CobbleClubRenderTypes;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = PokemonClientDelegate.class, remap = false)
public class PokemonClientDelegateMixin {
    @Shadow
    public PokemonEntity currentEntity;

    @Unique
    private long cobbleclub$lastTeraParticle;

    @Unique
    private final float cobbleclub$teraParticleCooldown = 2.0f;

    @Unique
    private float cobbleclub$secondsSinceLastTeraParticle() {
        return (float) (System.currentTimeMillis() - this.cobbleclub$lastTeraParticle)
                / 1000.0f;
    }

    @Inject(
            method = "tick(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V",
            at = @At("TAIL")
    )
    private void tick(PokemonEntity entity, CallbackInfo ci) {
        this.cobbleclub$playTera();
    }

    @Unique
    private void cobbleclub$playTera() {
        PokemonClientDelegate self =
                (PokemonClientDelegate) (Object) this;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;

        if (player == null) {
            return;
        }

        PokemonEntity entity = this.currentEntity;
        Optional<String> aspect = entity.getAspects()
                .stream()
                .filter(a -> a.startsWith("msd:tera_"))
                .findFirst();

        if (aspect.isEmpty()) {
            return;
        }

        double distance = player.getPos().distanceTo(entity.getPos());
        if (distance > Cobblemon.config.getShinyNoticeParticlesDistance()) {
            return;
        }

        if (this.cobbleclub$secondsSinceLastTeraParticle()
                <= this.cobbleclub$teraParticleCooldown) {
            return;
        }

        KotlinHelperFabric.INSTANCE.playParticleEffect(
                CobbleClubRenderTypes.getTeraAnimationFromAspect(aspect.get()),
                "root",
                self.getRuntime()
        );

        this.cobbleclub$lastTeraParticle = System.currentTimeMillis();
    }
}
