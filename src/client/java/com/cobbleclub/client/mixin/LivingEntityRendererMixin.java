package com.cobbleclub.client.mixin;

import com.cobbleclub.client.render.renderTypes.CobbleClubRenderTypes;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntityRenderer.class})
public class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
   @WrapOperation(
      method = {"render*"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"
)}
   )
   private VertexConsumer cobbleclub$applyPokemonShader(VertexConsumerProvider buffer, RenderLayer original, Operation<VertexConsumer> originalCall, T entity) {
      try {
         if (entity instanceof PokemonEntity pokemon) {
            if (pokemon.getPokemon().getSpecies().getName().equals("Terapagos")) {
               return (VertexConsumer)originalCall.call(new Object[]{buffer, original});
            } else {
               Optional<String> aspect = pokemon.getAspects().stream().filter((a) -> a.startsWith("msd:tera_")).findFirst();
               if (aspect.isEmpty()) {
                  return (VertexConsumer)originalCall.call(new Object[]{buffer, original});
               } else {
                  PokemonClientDelegate delegate = (PokemonClientDelegate)pokemon.getDelegate();
                  Identifier texture = VaryingModelRepository.INSTANCE.getTexture(pokemon.getPokemon().getSpecies().getResourceIdentifier(), delegate);
                  return buffer.getBuffer(CobbleClubRenderTypes.pokemonShader(texture, (String)aspect.get()));
               }
            }
         } else {
            return (VertexConsumer)originalCall.call(new Object[]{buffer, original});
         }
      } catch (Exception var9) {
         return (VertexConsumer)originalCall.call(new Object[]{buffer, original});
      }
   }
}
