/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1309
 *  net.minecraft.class_1799
 *  net.minecraft.class_2960
 *  net.minecraft.class_3883
 *  net.minecraft.class_3887
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_563
 *  net.minecraft.class_591
 *  net.minecraft.class_630
 *  net.minecraft.class_742
 *  net.minecraft.class_759
 *  net.minecraft.class_7833
 *  net.minecraft.class_811
 *  net.minecraft.class_9280
 *  net.minecraft.class_9334
 *  net.minecraft.class_976
 */
package com.cobbleclub.client.render;

import com.cobbleclub.client.wardrobe.CosmeticRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;

@Environment(value=EnvType.CLIENT)
public class CosmeticWingsLayer
extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private final HeldItemRenderer itemRenderer;
    private final ElytraEntityModel<AbstractClientPlayerEntity> clubWingModel;
    private static final Identifier CHARIZARD_WING_TEX = Identifier.of((String)"cobbleclub", (String)"textures/entity/wings/charizard_flame_wings.png");
    private static final Identifier SYLVEON_WING_TEX = Identifier.of((String)"cobbleclub", (String)"textures/entity/wings/sylveon_ribbon_wings.png");
    private static final Identifier LUCARIO_WING_TEX = Identifier.of((String)"cobbleclub", (String)"textures/entity/wings/lucario_aura_wings.png");
    private static final Identifier BULBASAUR_WING_TEX = Identifier.of((String)"cobbleclub", (String)"textures/entity/wings/bulbasaur_sprout_wings.png");

    public CosmeticWingsLayer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> parent, HeldItemRenderer itemRenderer, ModelPart elytraRoot) {
        super(parent);
        this.itemRenderer = itemRenderer;
        this.clubWingModel = new ElytraEntityModel(elytraRoot);
    }

    public void render(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player, float limbSwing, float limbSwingAmount, float partialTick, float age, float netHeadYaw, float headPitch) {
        CosmeticRenderState.State state = CosmeticRenderState.get(player.getUuid());
        if (state.hidden()) {
            return;
        }
        this.renderHead(pose, buffer, light, player, state.head());
        this.renderBack(pose, buffer, light, player, state.back(), limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        this.renderBalloon(pose, buffer, light, player, state.balloon());
    }

    private void renderHead(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CustomModelDataComponent cmd = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        int model = cmd != null ? cmd.value() : 0;
        pose.push();
        ((PlayerEntityModel)this.getContextModel()).head.rotate(pose);
        if (model == 22004) {
            pose.translate(0.0f, -0.3f, 0.0f);
            pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
            pose.scale(0.82f, 0.82f, 0.82f);
        } else if (model == 22001 || model == 22002 || model == 22003 || model == 22005 || model >= 22006 && model <= 22011) {
            float topOffset = model == 22002 || model >= 22009 && model <= 22011 ? -0.62f : (model == 22001 ? -0.56f : -0.5f);
            pose.translate(0.0f, topOffset, 0.0f);
            pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
            if (model == 22001 || model == 22002 || model == 22005 || model >= 22009 && model <= 22011) {
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
            }
            pose.scale(0.82f, 0.82f, 0.82f);
        } else {
            pose.scale(0.82f, 0.82f, 0.82f);
        }
        this.itemRenderer.renderItem((LivingEntity)player, stack, ModelTransformationMode.HEAD, false, pose, buffer, light);
        pose.pop();
    }

    private void renderBack(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player, ItemStack stack, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        int model;
        if (stack.isEmpty()) {
            return;
        }
        pose.push();
        ((PlayerEntityModel)this.getContextModel()).body.rotate(pose);
        pose.translate(0.0f, 0.35f, 0.22f);
        pose.scale(1.55f, 1.55f, 1.55f);
        pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        CustomModelDataComponent cmd = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        int n = model = cmd != null ? cmd.value() : 0;
        if (model >= 22101 && model <= 22104) {
            pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        }
        this.itemRenderer.renderItem((LivingEntity)player, stack, ModelTransformationMode.GUI, false, pose, buffer, light);
        pose.pop();
    }

    private void renderBalloon(MatrixStack pose, VertexConsumerProvider buffer, int light, AbstractClientPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CustomModelDataComponent cmd = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        int model = cmd != null ? cmd.value() : 0;
        boolean floaty = model >= 22301 && model <= 22306;
        pose.push();
        if (floaty) {
            ((PlayerEntityModel)this.getContextModel()).body.rotate(pose);
            pose.translate(0.0f, 0.56f, 0.02f);
            pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
            pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
            pose.scale(1.0f, 1.0f, 1.0f);
        } else {
            pose.translate(0.72f, -2.25f, 0.08f);
            pose.scale(1.35f, 1.35f, 1.35f);
            HeadFeatureRenderer.translate((MatrixStack)pose, (boolean)false);
        }
        this.itemRenderer.renderItem((LivingEntity)player, stack, floaty ? ModelTransformationMode.FIXED : ModelTransformationMode.GUI, false, pose, buffer, light);
        pose.pop();
    }
}

