package com.cobbleclub.client.gearpreview;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class GearPreviewRenderer {
   private static final float DEG_TO_RAD = ((float)Math.PI / 180F);
   private static final int FULL_BRIGHT = 15728880;
   private static final float SCENE_TOP = 2.0F;
   private static final float SCENE_MARGIN = 0.4F;
   private static final EquipmentSlot[] PREVIEW_SLOTS;
   private ClientWorld level;
   private OtherClientPlayerEntity dummy;

   private void ensureDummy() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft.world != null && minecraft.player != null) {
         if (this.dummy == null || this.level != minecraft.world) {
            this.level = minecraft.world;
            this.dummy = new OtherClientPlayerEntity(minecraft.world, minecraft.player.getGameProfile()) {
               public boolean isPartVisible(PlayerModelPart part) {
                  return MinecraftClient.getInstance().options.isPlayerModelPartEnabled(part);
               }
            };
            double px = minecraft.player.getX();
            double py = minecraft.player.getY() - (double)1000.0F;
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
      for(EquipmentSlot slot : PREVIEW_SLOTS) {
         ItemStack stack = (ItemStack)equipment.getOrDefault(slot, ItemStack.EMPTY);
         if (this.dummy.getEquippedStack(slot) != stack) {
            this.dummy.equipStack(slot, stack);
         }
      }

   }

   public void render(DrawContext guiGraphics, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, Map<EquipmentSlot, ItemStack> equipment) {
      this.ensureDummy();
      if (this.dummy != null) {
         this.applyEquipment(equipment);
         float scale = (float)(y1 - y0) / 2.4F * zoom;
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         guiGraphics.enableScissor(x0, y0, x1, y1);
         this.renderScene(guiGraphics, centerX, centerY, scale, 1.0F, 180.0F + yaw, pitch);
         guiGraphics.disableScissor();
      }
   }

   private void renderScene(DrawContext guiGraphics, float centerX, float centerY, float scale, float yOffset, float entityYaw, float cameraPitch) {
      applyRotation(this.dummy, entityYaw);
      Quaternionf pitchQuat = (new Quaternionf()).rotationX(cameraPitch * ((float)Math.PI / 180F));
      Quaternionf poseQuat = (new Quaternionf()).rotationZ((float)Math.PI).mul(pitchQuat);
      DiffuseLighting.method_34742();
      EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
      dispatcher.setRotation(pitchQuat.conjugate(new Quaternionf()).rotateY((float)Math.PI));
      dispatcher.setRenderShadows(false);
      RenderSystem.runAsFancy(() -> {
         guiGraphics.getMatrices().push();
         guiGraphics.getMatrices().translate((double)centerX, (double)centerY, (double)50.0F);
         guiGraphics.getMatrices().scale(scale, scale, -scale);
         guiGraphics.getMatrices().translate(0.0F, yOffset, 0.0F);
         guiGraphics.getMatrices().multiply(poseQuat);
         dispatcher.render(this.dummy, (double)0.0F, (double)0.0F, (double)0.0F, 0.0F, 1.0F, guiGraphics.getMatrices(), guiGraphics.getVertexConsumers(), 15728880);
         guiGraphics.getMatrices().pop();
      });
      guiGraphics.draw();
      dispatcher.setRenderShadows(true);
      DiffuseLighting.enableGuiDepthLighting();
   }

   public static void renderItem3D(DrawContext guiGraphics, int x0, int y0, int x1, int y1, Quaternionf orientation, float zoom, ItemStack stack) {
      if (!stack.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         float paneMin = (float)Math.min(x1 - x0, y1 - y0);
         float scale = paneMin * 0.62F * zoom;
         guiGraphics.enableScissor(x0, y0, x1, y1);
         guiGraphics.getMatrices().push();
         guiGraphics.getMatrices().translate((double)((float)(x0 + x1) / 2.0F), (double)((float)(y0 + y1) / 2.0F), (double)150.0F);
         guiGraphics.getMatrices().scale(scale, -scale, scale);
         guiGraphics.getMatrices().multiply(orientation);
         DiffuseLighting.enableGuiDepthLighting();
         mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, 15728880, OverlayTexture.DEFAULT_UV, guiGraphics.getMatrices(), guiGraphics.getVertexConsumers(), mc.world, 0);
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
      entity.setPitch(0.0F);
   }

   public void close() {
      this.level = null;
      this.dummy = null;
   }

   static {
      PREVIEW_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
   }
}
