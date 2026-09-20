/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1664
 *  net.minecraft.class_1799
 *  net.minecraft.class_1937
 *  net.minecraft.class_308
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_4597
 *  net.minecraft.class_4608
 *  net.minecraft.class_638
 *  net.minecraft.class_745
 *  net.minecraft.class_811
 *  net.minecraft.class_898
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package com.cobbleclub.client.gearpreview;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public final class GearPreviewRenderer {
    private static final float DEG_TO_RAD = (float)Math.PI / 180;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float SCENE_TOP = 2.0f;
    private static final float SCENE_MARGIN = 0.4f;
    private static final EquipmentSlot[] PREVIEW_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private ClientWorld level;
    private OtherClientPlayerEntity dummy;

    private void ensureDummy() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.world != null && minecraft.player != null) {
            if (this.dummy == null || this.level != minecraft.world) {
                this.level = minecraft.world;
                this.dummy = new OtherClientPlayerEntity(minecraft.world, minecraft.player.getGameProfile()){

                    public boolean isPartVisible(PlayerModelPart part) {
                        return MinecraftClient.getInstance().options.isPlayerModelPartEnabled(part);
                    }
                };
                double px = minecraft.player.getX();
                double py = minecraft.player.getY() - 1000.0;
                double pz = minecraft.player.getZ();
                this.dummy.setPosition(px, py, pz);
                this.dummy.resetPosition();
                this.dummy.capeX = this.dummy.prevCapeX = px;
                this.dummy.capeY = this.dummy.prevCapeY = py;
                this.dummy.capeZ = this.dummy.prevCapeZ = pz;
                this.dummy.setSilent(true);
            }
        } else {
            this.close();
        }
    }

    private void applyEquipment(Map<EquipmentSlot, ItemStack> equipment) {
        for (EquipmentSlot slot : PREVIEW_SLOTS) {
            ItemStack stack = equipment.getOrDefault(slot, ItemStack.EMPTY);
            if (this.dummy.getEquippedStack(slot) == stack) continue;
            this.dummy.equipStack(slot, stack);
        }
    }

    public void render(DrawContext guiGraphics, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, Map<EquipmentSlot, ItemStack> equipment) {
        this.ensureDummy();
        if (this.dummy != null) {
            this.applyEquipment(equipment);
            float scale = (float)(y1 - y0) / 2.4f * zoom;
            float centerX = (float)(x0 + x1) / 2.0f;
            float centerY = (float)(y0 + y1) / 2.0f;
            guiGraphics.enableScissor(x0, y0, x1, y1);
            this.renderScene(guiGraphics, centerX, centerY, scale, 1.0f, 180.0f + yaw, pitch);
            guiGraphics.disableScissor();
        }
    }

    private void renderScene(DrawContext guiGraphics, float centerX, float centerY, float scale, float yOffset, float entityYaw, float cameraPitch) {
        GearPreviewRenderer.applyRotation((LivingEntity)this.dummy, entityYaw);
        Quaternionf pitchQuat = new Quaternionf().rotationX(cameraPitch * ((float)Math.PI / 180));
        Quaternionf poseQuat = new Quaternionf().rotationZ((float)Math.PI).mul((Quaternionfc)pitchQuat);
        DiffuseLighting.disableGuiDepthLighting();
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRotation(pitchQuat.conjugate(new Quaternionf()).rotateY((float)Math.PI));
        dispatcher.setRenderShadows(false);
        RenderSystem.runAsFancy(() -> {
            guiGraphics.getMatrices().push();
            guiGraphics.getMatrices().translate((double)centerX, (double)centerY, 50.0);
            guiGraphics.getMatrices().scale(scale, scale, -scale);
            guiGraphics.getMatrices().translate(0.0f, yOffset, 0.0f);
            guiGraphics.getMatrices().multiply(poseQuat);
            dispatcher.render((Entity)this.dummy, 0.0, 0.0, 0.0, 0.0f, 1.0f, guiGraphics.getMatrices(), (VertexConsumerProvider)guiGraphics.getVertexConsumers(), 0xF000F0);
            guiGraphics.getMatrices().pop();
        });
        guiGraphics.draw();
        dispatcher.setRenderShadows(true);
        DiffuseLighting.enableGuiDepthLighting();
    }

    public static void renderItem3D(DrawContext guiGraphics, int x0, int y0, int x1, int y1, Quaternionf orientation, float zoom, ItemStack stack) {
        if (!stack.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            float paneMin = Math.min(x1 - x0, y1 - y0);
            float scale = paneMin * 0.62f * zoom;
            guiGraphics.enableScissor(x0, y0, x1, y1);
            guiGraphics.getMatrices().push();
            guiGraphics.getMatrices().translate((double)((float)(x0 + x1) / 2.0f), (double)((float)(y0 + y1) / 2.0f), 150.0);
            guiGraphics.getMatrices().scale(scale, -scale, scale);
            guiGraphics.getMatrices().multiply(orientation);
            DiffuseLighting.enableGuiDepthLighting();
            mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, 0xF000F0, OverlayTexture.DEFAULT_UV, guiGraphics.getMatrices(), (VertexConsumerProvider)guiGraphics.getVertexConsumers(), (World)mc.world, 0);
            guiGraphics.draw();
            guiGraphics.getMatrices().pop();
            guiGraphics.disableScissor();
            DiffuseLighting.enableGuiDepthLighting();
        }
    }

    private static void applyRotation(LivingEntity entity, float yaw) {
        entity.bodyYaw = yaw;
        entity.prevBodyYaw = yaw;
        entity.headYaw = yaw;
        entity.prevHeadYaw = yaw;
        entity.setYaw(yaw);
        entity.setPitch(0.0f);
    }

    public void close() {
        this.level = null;
        this.dummy = null;
    }
}

