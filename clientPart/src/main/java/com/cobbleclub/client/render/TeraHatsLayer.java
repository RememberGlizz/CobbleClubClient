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
import net.minecraft.class_1297;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_7833;

@Environment(EnvType.CLIENT)
public class TeraHatsLayer extends LayerEntity {
   private final class_2960 poserId = class_2960.method_60655("cobblemon", "tera_hat");

   public TeraHatsLayer() {
      super(new TeraHatState());
   }

   public void render(String aspect, RenderContext context, PokemonClientDelegate clientDelegate, PokemonEntity entity, Pokemon pokemon, float entityYaw, float partialTicks, class_4587 poseStack, class_4597 buffer, int packedLight) {
      if (!pokemon.getSpecies().getName().equals("Terapagos")) {
         super.render(context, clientDelegate, entity, pokemon, entityYaw, poseStack, buffer, packedLight);
         this.state.setCurrentAspects(Set.of(aspect));
         Map<String, MatrixWrapper> locatorStates = clientDelegate.getLocatorStates();
         MatrixWrapper headLocator = (MatrixWrapper)locatorStates.get("head");
         if (headLocator != null) {
            PosableModel model = VaryingModelRepository.INSTANCE.getPoser(this.poserId, this.state);
            class_2960 texture = VaryingModelRepository.INSTANCE.getTexture(this.poserId, this.state);
            model.setContext(context);
            model.setBufferProvider(buffer);
            this.state.setCurrentModel(model);
            context.put(RenderContext.Companion.getASPECTS(), Set.of(aspect));
            context.put(RenderContext.Companion.getTEXTURE(), texture);
            context.put(RenderContext.Companion.getSPECIES(), this.poserId);
            context.put(RenderContext.Companion.getPOSABLE_STATE(), this.state);
            LayerCodec.Settings settings = LayerDataLoader.getSettings(pokemon, aspect);
            poseStack.method_22903();
            poseStack.method_34425(headLocator.getMatrix());
            poseStack.method_22907(class_7833.field_40714.rotationDegrees(180.0F));
            poseStack.method_22907(class_7833.field_40716.rotationDegrees(180.0F));
            if (settings != null) {
               List<Float> translate = settings.translate();
               poseStack.method_46416((Float)translate.get(0), (Float)translate.get(1), (Float)translate.get(2));
            }

            if (settings != null) {
               List<Float> scale = settings.scale();
               poseStack.method_22905((Float)scale.get(0), (Float)scale.get(1), (Float)scale.get(2));
            }

            model.applyAnimations((class_1297)null, this.state, 0.0F, 0.0F, this.ticks, 0.0F, 0.0F);
            class_4588 vertexConsumer = buffer.getBuffer(CobbleClubRenderTypes.pokemonShader(texture, aspect));
            model.render(context, poseStack, vertexConsumer, packedLight, class_4608.field_21444, -1);
            model.withLayerContext(buffer, this.state, VaryingModelRepository.INSTANCE.getLayers(this.poserId, this.state), () -> {
               model.render(context, poseStack, vertexConsumer, packedLight, class_4608.field_21444, -1);
               return Unit.INSTANCE;
            });
            model.setDefault();
            poseStack.method_22909();
         }
      }
   }
}
