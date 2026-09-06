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
import net.minecraft.class_1309;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_583;
import net.minecraft.class_922;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({class_922.class})
public class LivingEntityRendererMixin<T extends class_1309, M extends class_583<T>> {
   @WrapOperation(
      method = {"render*"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
)}
   )
   private class_4588 cobbleclub$applyPokemonShader(class_4597 buffer, class_1921 original, Operation<class_4588> originalCall, T entity) {
      try {
         if (entity instanceof PokemonEntity pokemon) {
            if (pokemon.getPokemon().getSpecies().getName().equals("Terapagos")) {
               return (class_4588)originalCall.call(new Object[]{buffer, original});
            } else {
               Optional<String> aspect = pokemon.getAspects().stream().filter((a) -> a.startsWith("msd:tera_")).findFirst();
               if (aspect.isEmpty()) {
                  return (class_4588)originalCall.call(new Object[]{buffer, original});
               } else {
                  PokemonClientDelegate delegate = (PokemonClientDelegate)pokemon.getDelegate();
                  class_2960 texture = VaryingModelRepository.INSTANCE.getTexture(pokemon.getPokemon().getSpecies().getResourceIdentifier(), delegate);
                  return buffer.getBuffer(CobbleClubRenderTypes.pokemonShader(texture, (String)aspect.get()));
               }
            }
         } else {
            return (class_4588)originalCall.call(new Object[]{buffer, original});
         }
      } catch (Exception var9) {
         return (class_4588)originalCall.call(new Object[]{buffer, original});
      }
   }
}
