package com.cobbleclub.client.mixin;

import com.cobbleclub.client.KotlinHelperFabric;
import com.cobbleclub.client.render.renderTypes.CobbleClubRenderTypes;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {PokemonClientDelegate.class},
   remap = false
)
public class PokemonClientDelegateMixin {
   @Shadow
   public PokemonEntity currentEntity;
   @Unique
   private long cobbleclub$lastTeraParticle;
   @Unique
   private final float cobbleclub$teraParticleCooldown = 2.0F;

   @Unique
   private float cobbleclub$secondsSinceLastTeraParticle() {
      return (float)(System.currentTimeMillis() - this.cobbleclub$lastTeraParticle) / 1000.0F;
   }

   @Inject(
      method = {"tick(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V"},
      at = {@At("TAIL")}
   )
   private void tick(PokemonEntity entity, CallbackInfo ci) {
      this.cobbleclub$playTera();
   }

   @Unique
   private void cobbleclub$playTera() {
      PokemonClientDelegate self = (PokemonClientDelegate)(Object)this;
      class_310 mc = class_310.method_1551();
      class_1657 player = mc.field_1724;
      if (player != null) {
         PokemonEntity entity = this.currentEntity;
         Optional<String> aspect = entity.getAspects().stream().filter((a) -> a.startsWith("msd:tera_")).findFirst();
         if (!aspect.isEmpty()) {
            double distance = player.method_19538().method_1022(entity.method_19538());
            if (!(distance > (double)Cobblemon.config.getShinyNoticeParticlesDistance())) {
               if (this.cobbleclub$secondsSinceLastTeraParticle() > 2.0F) {
                  KotlinHelperFabric.INSTANCE.playParticleEffect(CobbleClubRenderTypes.getTeraAnimationFromAspect((String)aspect.get()), "root", self.getRuntime());
                  this.cobbleclub$lastTeraParticle = System.currentTimeMillis();
               }

            }
         }
      }
   }
}
