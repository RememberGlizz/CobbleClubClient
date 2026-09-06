package com.cobbleclub.client.mixin;

import com.cobbleclub.client.duck.PokemonEntityDuck;
import com.cobbleclub.client.render.TeraHatsLayer;
import com.cobbleclub.client.rp.LayerDataLoader;
import com.cobbleclub.client.sizer.LayerCodec;
import com.cobbleclub.client.state.TeraCrystalState;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.MatrixWrapper;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockActiveAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext.RenderState;
import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kotlin.Unit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5617;
import net.minecraft.class_7833;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PokemonRenderer.class})
public class PokemonRendererMixin {
   @Unique
   private final RenderContext cobbleclub$context = new RenderContext();
   @Unique
   private final class_2960 cobbleclub$teraCrystalPoserId = class_2960.method_60655("cobblemon", "terastal_transformation");
   @Unique
   private final Set<String> cobbleclub$teraCrystalAspects = new HashSet();
   @Unique
   private final TeraHatsLayer cobbleclub$teraHatsLayer = new TeraHatsLayer();
   @Unique
   private boolean cobbleclub$teraCrystalAvailable = true;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void init(class_5617.class_5618 context, CallbackInfo ci) {
      this.cobbleclub$context.put(RenderContext.Companion.getRENDER_STATE(), RenderState.WORLD);
      this.cobbleclub$context.put(RenderContext.Companion.getDO_QUIRKS(), true);
   }

   @Inject(
      method = {"render*"},
      at = {@At("TAIL")},
      remap = false
   )
   public void render(PokemonEntity entity, float entityYaw, float partialTicks, class_4587 poseStack, class_4597 buffer, int packedLight, CallbackInfo ci) {
      try {
         PokemonClientDelegate clientDelegate = (PokemonClientDelegate)entity.getDelegate();
         Pokemon pokemon = entity.getPokemon();
         Set<String> aspects = pokemon.getAspects();
         boolean teraPlay = aspects.contains("play_tera");
         Optional<String> aspect = aspects.stream().filter((a) -> a.startsWith("msd:tera_")).findFirst();
         boolean cobbleclub$teraCrystalPlayed = ((PokemonEntityDuck)entity).cobbleclub$isTeraCrystalPlayed();
         boolean cobbleclub$teraCrystalPass = ((PokemonEntityDuck)entity).cobbleclub$isTeraCrystalPass();
         if (teraPlay && this.cobbleclub$teraCrystalAvailable && (!cobbleclub$teraCrystalPlayed || cobbleclub$teraCrystalPass)) {
            this.cobbleclub$renderTeraCrystals(entity, pokemon, clientDelegate, poseStack, buffer, packedLight);
            if (!cobbleclub$teraCrystalPass) {
               return;
            }
         }

         aspect.ifPresent((s) -> this.cobbleclub$teraHatsLayer.render(s, this.cobbleclub$context, clientDelegate, entity, pokemon, entityYaw, partialTicks, poseStack, buffer, packedLight));
      } catch (Exception var15) {
      }

   }

   @Unique
   private void cobbleclub$renderTeraCrystals(PokemonEntity entity, Pokemon pokemon, PokemonClientDelegate clientDelegate, class_4587 poseStack, class_4597 buffer, int packedLight) {
      PokemonEntityDuck duck = (PokemonEntityDuck)entity;
      long cobbleclub$lastCrystalTimeNs = duck.cobbleclub$getLastCrystalTimeNs();
      double cobbleclub$animCrystalSeconds = duck.cobbleclub$getAnimCrystalSeconds();
      if (!class_310.method_1551().method_1493()) {
         long now = System.nanoTime();
         if (cobbleclub$lastCrystalTimeNs != -1L) {
            double deltaSeconds = (double)(now - cobbleclub$lastCrystalTimeNs) / (double)1.0E9F;
            duck.cobbleclub$setAnimCrystalSeconds(cobbleclub$animCrystalSeconds + deltaSeconds);
         }

         duck.cobbleclub$setLastCrystalTimeNs(now);
      } else {
         duck.cobbleclub$setLastCrystalTimeNs(System.nanoTime());
      }

      float cobbleclub$teraCrystalDuration;
      try {
         cobbleclub$teraCrystalDuration = (new BedrockActiveAnimation(BedrockAnimationRepository.INSTANCE.getAnimation("terastal_transformation", "animation.terastal_transformation.transform"))).getDuration();
      } catch (Exception var27) {
         this.cobbleclub$teraCrystalAvailable = false;
         duck.cobbleclub$setTeraCrystalPlayed(true);
         duck.cobbleclub$setTeraCrystalPass(true);
         return;
      }

      TeraCrystalState cobbleclub$teraCrystalState = ((PokemonEntityDuck)entity).cobbleclub$getTeraCrystalState();
      if (cobbleclub$teraCrystalState.getAnimationSeconds() >= cobbleclub$teraCrystalDuration) {
         duck.cobbleclub$setTeraCrystalPlayed(true);
         duck.cobbleclub$setTeraCrystalPass(false);
         duck.cobbleclub$setAnimCrystalSeconds((double)0.0F);
         duck.cobbleclub$setLastCrystalTimeNs(-1L);
         cobbleclub$teraCrystalState.resetAnimation();
         entity.after(3.0F, () -> {
            duck.cobbleclub$setTeraCrystalPlayed(false);
            return Unit.INSTANCE;
         });
      } else {
         if ((double)cobbleclub$teraCrystalState.getAnimationSeconds() >= (double)cobbleclub$teraCrystalDuration - 0.3) {
            duck.cobbleclub$setTeraCrystalPass(true);
         }

         float ticks = (float)(cobbleclub$animCrystalSeconds * (double)20.0F);
         int age = (int)ticks;
         float pt = ticks - (float)age;
         cobbleclub$teraCrystalState.updateAge(age);
         cobbleclub$teraCrystalState.updatePartialTicks(pt);
         cobbleclub$teraCrystalState.setCurrentAspects(this.cobbleclub$teraCrystalAspects);
         Map<String, MatrixWrapper> locatorStates = clientDelegate.getLocatorStates();
         MatrixWrapper rootLocator = (MatrixWrapper)locatorStates.get("root");
         if (rootLocator != null) {
            PosableModel model = VaryingModelRepository.INSTANCE.getPoser(this.cobbleclub$teraCrystalPoserId, cobbleclub$teraCrystalState);
            model.setContext(this.cobbleclub$context);
            class_2960 texture = VaryingModelRepository.INSTANCE.getTexture(this.cobbleclub$teraCrystalPoserId, cobbleclub$teraCrystalState);
            class_4588 vertexConsumer = buffer.getBuffer(class_1921.method_23576(texture));
            model.setBufferProvider(buffer);
            cobbleclub$teraCrystalState.setCurrentModel(model);
            this.cobbleclub$context.put(RenderContext.Companion.getASPECTS(), this.cobbleclub$teraCrystalAspects);
            this.cobbleclub$context.put(RenderContext.Companion.getTEXTURE(), texture);
            this.cobbleclub$context.put(RenderContext.Companion.getSPECIES(), this.cobbleclub$teraCrystalPoserId);
            this.cobbleclub$context.put(RenderContext.Companion.getPOSABLE_STATE(), cobbleclub$teraCrystalState);
            LayerCodec.Settings settings = LayerDataLoader.getSettings(pokemon, "msd:tera_crystal");
            poseStack.method_22903();

            try {
               poseStack.method_34425(rootLocator.getMatrix());
               poseStack.method_22907(class_7833.field_40714.rotationDegrees(180.0F));
               poseStack.method_22907(class_7833.field_40716.rotationDegrees(180.0F));
               poseStack.method_22904(0.08, (double)0.0F, (double)0.0F);
               if (settings != null) {
                  List<Float> translate = settings.translate();
                  poseStack.method_46416((Float)translate.get(0), (Float)translate.get(1), (Float)translate.get(2));
               }

               poseStack.method_22905(1.5F, 1.5F, 1.5F);
               if (settings != null) {
                  List<Float> scale = settings.scale();
                  poseStack.method_22905((Float)scale.get(0), (Float)scale.get(1), (Float)scale.get(2));
               }

               model.applyAnimations((class_1297)null, cobbleclub$teraCrystalState, 0.0F, 0.0F, ticks, 0.0F, 0.0F);
               model.render(this.cobbleclub$context, poseStack, vertexConsumer, packedLight, class_4608.field_21444, -1);
               model.withLayerContext(buffer, cobbleclub$teraCrystalState, VaryingModelRepository.INSTANCE.getLayers(this.cobbleclub$teraCrystalPoserId, cobbleclub$teraCrystalState), () -> {
                  model.render(this.cobbleclub$context, poseStack, vertexConsumer, packedLight, class_4608.field_21444, -1);
                  return Unit.INSTANCE;
               });
               model.setDefault();
            } finally {
               poseStack.method_22909();
            }

         }
      }
   }
}
