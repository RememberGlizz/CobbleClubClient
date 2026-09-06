package com.cobbleclub.client.render;

import com.cobbleclub.client.wardrobe.CosmeticRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_3883;
import net.minecraft.class_3887;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_591;
import net.minecraft.class_563;
import net.minecraft.class_630;
import net.minecraft.class_742;
import net.minecraft.class_2960;
import net.minecraft.class_759;
import net.minecraft.class_7833;
import net.minecraft.class_811;
import net.minecraft.class_9280;
import net.minecraft.class_9334;
import net.minecraft.class_976;

/** Direct player-model cosmetics: no armor stands, passengers, leashes, or equipment spoofing. */
@Environment(EnvType.CLIENT)
public class CosmeticWingsLayer extends class_3887<class_742, class_591<class_742>> {
   private final class_759 itemRenderer;
   private final class_563<class_742> clubWingModel;

   private static final class_2960 CHARIZARD_WING_TEX = class_2960.method_60655("cobbleclub", "textures/entity/wings/charizard_flame_wings.png");
   private static final class_2960 SYLVEON_WING_TEX = class_2960.method_60655("cobbleclub", "textures/entity/wings/sylveon_ribbon_wings.png");
   private static final class_2960 LUCARIO_WING_TEX = class_2960.method_60655("cobbleclub", "textures/entity/wings/lucario_aura_wings.png");
   private static final class_2960 BULBASAUR_WING_TEX = class_2960.method_60655("cobbleclub", "textures/entity/wings/bulbasaur_sprout_wings.png");

   public CosmeticWingsLayer(class_3883<class_742, class_591<class_742>> parent, class_759 itemRenderer, class_630 elytraRoot) {
      super(parent);
      this.itemRenderer = itemRenderer;
      this.clubWingModel = new class_563<>(elytraRoot);
   }

   @Override
   public void method_4199(class_4587 pose, class_4597 buffer, int light, class_742 player,
                           float limbSwing, float limbSwingAmount, float partialTick,
                           float age, float netHeadYaw, float headPitch) {
      CosmeticRenderState.State state = CosmeticRenderState.get(player.method_5667());
      if (state.hidden()) return;
      renderHead(pose, buffer, light, player, state.head());
      renderBack(pose, buffer, light, player, state.back(), limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
      renderBalloon(pose, buffer, light, player, state.balloon());
   }

   private void renderHead(class_4587 pose, class_4597 buffer, int light, class_742 player, class_1799 stack) {
      if (stack.method_7960()) return;

      class_9280 cmd = (class_9280)stack.method_57824(class_9334.field_49637);
      int model = cmd != null ? cmd.comp_2382() : 0;

      pose.method_22903();
      ((class_591)this.method_17165()).field_3398.method_22703(pose);

      if (model == 22004) {
         // Lucario orientation is now correct. Its source model sits much lower than the
         // helmet-style hats, so lift the entire crest substantially without changing
         // rotation, scale, or facing.
         pose.method_46416(0.0F, -0.88F, 0.0F);
         pose.method_22907(class_7833.field_40718.rotationDegrees(180.0F));
         pose.method_22905(0.82F, 0.82F, 0.82F);
      } else if (model == 22001 || model == 22002 || model == 22003 || model == 22005 || (model >= 22006 && model <= 22011)) {
         // Normal hats stay hard-anchored to the live player head.
         // Pika (22001) and Eevee (22002) were almost right but sat a little low,
         // so only those two are raised slightly. Gengar (22003) is intentionally untouched.
         float topOffset = (model == 22002 || (model >= 22009 && model <= 22011)) ? -0.62F
               : (model == 22001 ? -0.56F : -0.50F);
         pose.method_46416(0.0F, topOffset, 0.0F);
         pose.method_22907(class_7833.field_40714.rotationDegrees(180.0F));

         // Correct only the hats whose source model faces backward; rotate them in place
         // without changing their head anchor. Gengar/Lucario keep their proven orientation.
         if (model == 22001 || model == 22002 || model == 22005 || (model >= 22009 && model <= 22011)) {
            // Pika/Froggy-style hats and the Club Crown need the in-place Y turn to face forward.
            pose.method_22907(class_7833.field_40716.rotationDegrees(180.0F));
         }
         pose.method_22905(0.82F, 0.82F, 0.82F);
      } else {
         pose.method_22905(0.82F, 0.82F, 0.82F);
      }

      this.itemRenderer.method_3233(player, stack, class_811.field_4316, false, pose, buffer, light);
      pose.method_22909();
   }

   private void renderBack(class_4587 pose, class_4597 buffer, int light, class_742 player, class_1799 stack,
                           float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
      if (stack.method_7960()) return;

      // EVERY wing uses the exact same item-renderer path as Club Wings.
      // The only difference between CMD 0 and the Pokemon variants is the item texture
      // selected by assets/minecraft/models/item/elytra.json.
      pose.method_22903();
      ((class_591)this.method_17165()).field_3391.method_22703(pose);
      pose.method_46416(0.0F, 0.35F, 0.22F);
      pose.method_22905(1.55F, 1.55F, 1.55F);
      pose.method_22907(class_7833.field_40716.rotationDegrees(180.0F));

      // Club Wings (CMD 0) stay EXACTLY as-is. The Pokemon variants already sit in
      // the correct location and face the correct direction; their artwork is simply
      // upside-down. Rotate only those variants 180 degrees around local Z — like
      // turning a fixed wheel — so position/depth/facing cannot change.
      class_9280 cmd = (class_9280)stack.method_57824(class_9334.field_49637);
      int model = cmd != null ? cmd.comp_2382() : 0;
      if (model >= 22101 && model <= 22104) {
         pose.method_22907(class_7833.field_40718.rotationDegrees(180.0F));
      }

      this.itemRenderer.method_3233(player, stack, class_811.field_4317, false, pose, buffer, light);
      pose.method_22909();
   }

   private void renderBalloon(class_4587 pose, class_4597 buffer, int light, class_742 player, class_1799 stack) {
      if (stack.method_7960()) return;
      class_9280 cmd = (class_9280)stack.method_57824(class_9334.field_49637);
      int model = cmd != null ? cmd.comp_2382() : 0;
      boolean floaty = model >= 22301 && model <= 22306;
      pose.method_22903();
      if (floaty) {
         ((class_591)this.method_17165()).field_3391.method_22703(pose);
         // Stable body-local waist anchor. Raise slightly while keeping the proven upright X flip.
         pose.method_46416(0.0F, 0.56F, 0.02F);
         pose.method_22907(class_7833.field_40714.rotationDegrees(180.0F));
         // The geometry is otherwise perfect but faces backward, so turn it around on Y.
         pose.method_22907(class_7833.field_40716.rotationDegrees(180.0F));
         pose.method_22905(1.00F, 1.00F, 1.00F);
      } else {
         // Balloon strings are part of each balloon item model now. This avoids
         // custom BufferBuilder vertices in wardrobe previews (which caused UV0 crashes).
         // Feature-render coordinates use negative Y above the player's feet.
         pose.method_46416(0.72F, -2.25F, 0.08F);
         pose.method_22905(1.35F, 1.35F, 1.35F);
         class_976.method_32798(pose, false);
      }
      this.itemRenderer.method_3233(player, stack, floaty ? class_811.field_4319 : class_811.field_4317, false, pose, buffer, light);
      pose.method_22909();
   }
}
