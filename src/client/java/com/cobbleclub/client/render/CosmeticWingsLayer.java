package com.cobbleclub.client.render;

import com.cobbleclub.client.wardrobe.CosmeticRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Direct player-model cosmetics: no armor stands, passengers, leashes, or equipment spoofing. */
@Environment(EnvType.CLIENT)
public class CosmeticWingsLayer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
   private final HeldItemRenderer itemRenderer;
   private final ElytraEntityModel<AbstractClientPlayerEntity> clubWingModel;

   private static final Identifier CHARIZARD_WING_TEX = Identifier.of("cobbleclub", "textures/entity/wings/charizard_flame_wings.png");
   private static final Identifier SYLVEON_WING_TEX = Identifier.of("cobbleclub", "textures/entity/wings/sylveon_ribbon_wings.png");
   private static final Identifier LUCARIO_WING_TEX = Identifier.of("cobbleclub", "textures/entity/wings/lucario_aura_wings.png");
   private static final Identifier BULBASAUR_WING_TEX = Identifier.of("cobbleclub", "textures/entity/wings/bulbasaur_sprout_wings.png");

   public CosmeticWingsLayer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> parent, HeldItemRenderer itemRenderer, ModelPart elytraRoot) {
      super(parent);
      this.itemRenderer = itemRenderer;
      this.clubWingModel = new ElytraEntityModel<>(elytraRoot);
   }

   @Override
   public void render(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player,
                           float limbSwing, float limbSwingAmount, float partialTick,
                           float age, float netHeadYaw, float headPitch) {
      CosmeticRenderState.State state = CosmeticRenderState.get(player.getUuid());
      if (state.hidden()) return;
      renderHead(pose, buffer, light, player, state.head());
      renderBack(pose, buffer, light, player, state.back(), limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
      renderBalloon(pose, buffer, light, player, state.balloon());
   }

   private void renderHead(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player, ItemStack stack) {
      if (stack.isEmpty()) return;

      CustomModelDataComponent cmd = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      int model = cmd != null ? cmd.value() : 0;

      pose.push();
      ((PlayerEntityModel)this.getContextModel()).head.rotate(pose);

      if (model == 22004) {
         // Lucario orientation is now correct. Its source model sits much lower than the
         // helmet-style hats, so lift the entire crest substantially without changing
         // rotation, scale, or facing.
         pose.translate(0.0F, -0.88F, 0.0F);
         pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
         pose.scale(0.82F, 0.82F, 0.82F);
      } else if (model == 22001 || model == 22002 || model == 22003 || model == 22005 || (model >= 22006 && model <= 22011)) {
         // Normal hats stay hard-anchored to the live player head.
         // Pika (22001) and Eevee (22002) were almost right but sat a little low,
         // so only those two are raised slightly. Gengar (22003) is intentionally untouched.
         float topOffset = (model == 22002 || (model >= 22009 && model <= 22011)) ? -0.62F
               : (model == 22001 ? -0.56F : -0.50F);
         pose.translate(0.0F, topOffset, 0.0F);
         pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));

         // Correct only the hats whose source model faces backward; rotate them in place
         // without changing their head anchor. Gengar/Lucario keep their proven orientation.
         if (model == 22001 || model == 22002 || model == 22005 || (model >= 22009 && model <= 22011)) {
            // Pika/Froggy-style hats and the Club Crown need the in-place Y turn to face forward.
            pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
         }
         pose.scale(0.82F, 0.82F, 0.82F);
      } else {
         pose.scale(0.82F, 0.82F, 0.82F);
      }

      this.itemRenderer.renderItem(player, stack, ModelTransformationMode.HEAD, false, pose, buffer, light);
      pose.pop();
   }

   private void renderBack(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player, ItemStack stack,
                           float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
      if (stack.isEmpty()) return;

      // EVERY wing uses the exact same item-renderer path as Club Wings.
      // The only difference between CMD 0 and the Pokemon variants is the item texture
      // selected by assets/minecraft/models/item/elytra.json.
      pose.push();
      ((PlayerEntityModel)this.getContextModel()).body.rotate(pose);
      pose.translate(0.0F, 0.35F, 0.22F);
      pose.scale(1.55F, 1.55F, 1.55F);
      pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

      // Club Wings (CMD 0) stay EXACTLY as-is. The Pokemon variants already sit in
      // the correct location and face the correct direction; their artwork is simply
      // upside-down. Rotate only those variants 180 degrees around local Z — like
      // turning a fixed wheel — so position/depth/facing cannot change.
      CustomModelDataComponent cmd = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      int model = cmd != null ? cmd.value() : 0;
      if (model >= 22101 && model <= 22104) {
         pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
      }

      this.itemRenderer.renderItem(player, stack, ModelTransformationMode.GUI, false, pose, buffer, light);
      pose.pop();
   }

   private void renderBalloon(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player, ItemStack stack) {
      if (stack.isEmpty()) return;
      CustomModelDataComponent cmd = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      int model = cmd != null ? cmd.value() : 0;
      boolean floaty = model >= 22301 && model <= 22306;
      pose.push();
      if (floaty) {
         ((PlayerEntityModel)this.getContextModel()).body.rotate(pose);
         // Stable body-local waist anchor. Raise slightly while keeping the proven upright X flip.
         pose.translate(0.0F, 0.56F, 0.02F);
         pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
         // The geometry is otherwise perfect but faces backward, so turn it around on Y.
         pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
         pose.scale(1.00F, 1.00F, 1.00F);
      } else {
         // Balloon strings are part of each balloon item model now. This avoids
         // custom BufferBuilder vertices in wardrobe previews (which caused UV0 crashes).
         // Feature-render coordinates use negative Y above the player's feet.
         pose.translate(0.72F, -2.25F, 0.08F);
         pose.scale(1.35F, 1.35F, 1.35F);
         HeadFeatureRenderer.translate(pose, false);
      }
      this.itemRenderer.renderItem(player, stack, floaty ? ModelTransformationMode.FIXED : ModelTransformationMode.GUI, false, pose, buffer, light);
      pose.pop();
   }
}
