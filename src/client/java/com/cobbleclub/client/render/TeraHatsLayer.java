package com.cobbleclub.client.render;

import com.cobbleclub.client.render.renderTypes.CobbleClubRenderTypes;
import com.cobbleclub.client.rp.LayerDataLoader;
import com.cobbleclub.client.sizer.LayerCodec;
import com.cobbleclub.client.state.TeraHatState;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.MatrixWrapper;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.Unit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class TeraHatsLayer extends LayerEntity {
   private final Identifier poserId = Identifier.of("cobblemon", "tera_hat");

   public TeraHatsLayer() {
      super(new TeraHatState());
   }

   public void render(String aspect, RenderContext context, PokemonClientDelegate clientDelegate, PokemonEntity entity, Pokemon pokemon, float entityYaw, float partialTicks, MatrixStack poseStack, VertexConsumerProvider buffer, int packedLight) {
      if (!pokemon.getSpecies().getName().equals("Terapagos")) {
         super.render(context, clientDelegate, entity, pokemon, entityYaw, poseStack, buffer, packedLight);
         this.state.setCurrentAspects(Set.of(aspect));
         Map<String, MatrixWrapper> locatorStates = clientDelegate.getLocatorStates();
         MatrixWrapper headLocator = (MatrixWrapper)locatorStates.get("head");
         if (headLocator != null) {
            PosableModel model = VaryingModelRepository.INSTANCE.getPoser(this.poserId, this.state);
            Identifier texture = VaryingModelRepository.INSTANCE.getTexture(this.poserId, this.state);
            model.setContext(context);
            model.setBufferProvider(buffer);
            this.state.setCurrentModel(model);
            context.put(RenderContext.Companion.getASPECTS(), Set.of(aspect));
            context.put(RenderContext.Companion.getTEXTURE(), texture);
            context.put(RenderContext.Companion.getSPECIES(), this.poserId);
            context.put(RenderContext.Companion.getPOSABLE_STATE(), this.state);
            LayerCodec.Settings settings = LayerDataLoader.getSettings(pokemon, aspect);
            poseStack.push();
            poseStack.multiplyPositionMatrix(headLocator.getMatrix());
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            if (settings != null) {
               List<Float> translate = settings.translate();
               poseStack.translate((Float)translate.get(0), (Float)translate.get(1), (Float)translate.get(2));
            }

            if (settings != null) {
               List<Float> scale = settings.scale();
               poseStack.scale((Float)scale.get(0), (Float)scale.get(1), (Float)scale.get(2));
            }

            model.applyAnimations((Entity)null, this.state, 0.0F, 0.0F, this.ticks, 0.0F, 0.0F);
            VertexConsumer vertexConsumer = buffer.getBuffer(CobbleClubRenderTypes.pokemonShader(texture, aspect));
            model.render(context, poseStack, vertexConsumer, packedLight, OverlayTexture.DEFAULT_UV, -1);
            model.withLayerContext(buffer, this.state, VaryingModelRepository.INSTANCE.getLayers(this.poserId, this.state), () -> {
               model.render(context, poseStack, vertexConsumer, packedLight, OverlayTexture.DEFAULT_UV, -1);
               return Unit.INSTANCE;
            });
            model.setDefault();
            poseStack.pop();
         }
      }
   }
}
