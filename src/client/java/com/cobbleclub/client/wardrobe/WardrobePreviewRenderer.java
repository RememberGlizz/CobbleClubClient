package com.cobbleclub.client.wardrobe;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class WardrobePreviewRenderer {
   private static final UUID PREVIEW_UUID = UUID.nameUUIDFromBytes("cobbleclub:wardrobe-preview".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   private static final float DEG_TO_RAD = ((float)Math.PI / 180F);
   private static final int FULL_BRIGHT = 15728880;
   private static final Map<Identifier, RenderLayer> GLOW_OUTLINE_TYPES = new HashMap();
   private static final RenderLayer STRING_TYPE;
   private static final double BACKPACK_STAND_Y_OFFSET = -1.0;
   private static final float PLAYER_SCENE_TOP = 2.0F;
   private static final float SCENE_MARGIN = 0.4F;
   private static final float BALLOON_VISUAL_TOP = 2.5F;
   private static final float BALLOON_PREVIEW_MAX_Y = 1.3F;
   private static final float STRING_PLAYER_Y = 1.05F;
   private static final float STRING_BALLOON_Y = 1.35F;
   private static final float HALO_ALPHA = 0.22F;
   private static final float[][] GLOW_HALO;
   private static final float[][] GLOW_CORE;
   private ClientWorld level;
   private OtherClientPlayerEntity dummy;
   private ItemStack previewBalloon = ItemStack.EMPTY;
   private double[] balloonOffset;
   private boolean glowPass;

   public WardrobePreviewRenderer() {
      this.balloonOffset = WardrobeState.DEFAULT_BALLOON_OFFSET;
   }

   private void ensureEntities() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft.world != null && minecraft.player != null) {
         if (this.dummy == null || this.level != minecraft.world) {
            this.level = minecraft.world;
            this.dummy = new OtherClientPlayerEntity(minecraft.world, new GameProfile(PREVIEW_UUID, "CobbleClubPreview")) {
               public boolean isPartVisible(PlayerModelPart part) {
                  return WardrobePreviewRenderer.this.glowPass && part == PlayerModelPart.CAPE ? false : MinecraftClient.getInstance().options.isPlayerModelPartEnabled(part);
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

   public void updateEquipment(WardrobeState state) {
      this.updateEquipment(state.previewStack(WardrobeSlot.HELMET), state.previewStack(WardrobeSlot.BACKPACK), state.previewStack(WardrobeSlot.BALLOON), WardrobeState.get().getBalloonOffset());
   }

   public void updateEquipment(ItemStack helmet, ItemStack backpack, ItemStack balloon, double[] balloonOffset) {
      this.ensureEntities();
      if (this.dummy != null) {
         this.balloonOffset = balloonOffset;
         this.previewBalloon = balloon == null ? ItemStack.EMPTY : balloon;
         setHeadItem(this.dummy, ItemStack.EMPTY);
         CosmeticRenderState.putPreview(PREVIEW_UUID, helmet, backpack, balloon);
      }
   }

   public void updateEquipment(ItemStack helmet, ItemStack backpack, ItemStack balloon) {
      this.updateEquipment(helmet, backpack, balloon, WardrobeState.DEFAULT_BALLOON_OFFSET);
   }

   private static void setHeadItem(LivingEntity entity, ItemStack stack) {
      if (entity.getEquippedStack(EquipmentSlot.HEAD) != stack) {
         entity.equipStack(EquipmentSlot.HEAD, stack);
      }

   }

   public void render(DrawContext guiGraphics, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, int glowColor, float partialTick) {
      this.ensureEntities();
      if (this.dummy != null) {
         double[] balloonOffset = new double[]{this.balloonOffset[0], Math.min(this.balloonOffset[1], (double)1.3F), this.balloonOffset[2]};
         boolean balloonVisible = !this.previewBalloon.isEmpty();
         float sceneTop = balloonVisible ? Math.max(2.0F, (float)balloonOffset[1] + 2.5F) : 2.0F;
         float scale = (float)(y1 - y0) / (sceneTop + 0.4F) * zoom;
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         guiGraphics.enableScissor(x0, y0, x1, y1);
         guiGraphics.draw();
         RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
         this.renderScene(guiGraphics, centerX, centerY, scale, sceneTop / 2.0F, 180.0F + yaw, pitch, balloonOffset, glowColor);
         guiGraphics.disableScissor();
      }
   }

   private void renderScene(DrawContext guiGraphics, float centerX, float centerY, float scale, float yOffset, float entityYaw, float cameraPitch, double[] balloonOffset, int glowColor) {
      applyRotation(this.dummy, entityYaw);
      Quaternionf pitchQuat = (new Quaternionf()).rotationX(cameraPitch * ((float)Math.PI / 180F));
      Quaternionf poseQuat = (new Quaternionf()).rotationZ((float)Math.PI).mul(pitchQuat);
      DiffuseLighting.method_34742();
      EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
      dispatcher.setRotation(pitchQuat.conjugate(new Quaternionf()).rotateY((float)Math.PI));
      dispatcher.setRenderShadows(false);
      RenderSystem.runAsFancy(() -> {
         if (glowColor >= 0) {
            RenderLayer glowType = flatOutlineType(this.dummy.getSkinTextures().texture());
            int argb = -16777216 | glowColor & 16777215;
            ItemStack savedHead = this.dummy.getEquippedStack(EquipmentSlot.HEAD);
            this.dummy.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            this.glowPass = true;
            this.drawGlowTier(guiGraphics, dispatcher, glowType, argb, GLOW_HALO, 0.22F, centerX, centerY, scale, yOffset, poseQuat);
            this.drawGlowTier(guiGraphics, dispatcher, glowType, argb, GLOW_CORE, 1.0F, centerX, centerY, scale, yOffset, poseQuat);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.glowPass = false;
            this.dummy.equipStack(EquipmentSlot.HEAD, savedHead);
         }

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

   private void drawGlowTier(DrawContext guiGraphics, EntityRenderDispatcher dispatcher, RenderLayer glowType, int argb, float[][] offsets, float alpha, float centerX, float centerY, float scale, float yOffset, Quaternionf poseQuat) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

      for(float[] off : offsets) {
         VertexConsumerProvider glowSource = (renderType) -> new FlatColorConsumer(guiGraphics.getVertexConsumers().getBuffer(glowType), argb);
         guiGraphics.getMatrices().push();
         guiGraphics.getMatrices().translate((double)(centerX + off[0]), (double)(centerY + off[1]), (double)49.0F);
         guiGraphics.getMatrices().scale(scale, scale, -scale);
         guiGraphics.getMatrices().translate(0.0F, yOffset, 0.0F);
         guiGraphics.getMatrices().multiply(poseQuat);
         dispatcher.render(this.dummy, (double)0.0F, (double)0.0F, (double)0.0F, 0.0F, 1.0F, guiGraphics.getMatrices(), glowSource, 15728880);
         guiGraphics.getMatrices().pop();
      }

      guiGraphics.getVertexConsumers().draw(glowType);
   }

   private void drawBalloonString(DrawContext guiGraphics, float centerX, float centerY, float scale, float yOffset, Quaternionf poseQuat, double[] balloonOffset) {
      Vector3f a = poseQuat.transform(new Vector3f(0.0F, 1.05F, 0.0F));
      Vector3f b = poseQuat.transform(new Vector3f((float)balloonOffset[0], (float)balloonOffset[1] + 1.35F, (float)balloonOffset[2]));
      float ax = centerX + scale * a.x;
      float ay = centerY + scale * (a.y + yOffset);
      float bx = centerX + scale * b.x;
      float by = centerY + scale * (b.y + yOffset);
      float dx = bx - ax;
      float dy = by - ay;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len < 0.001F)) {
         float nx = -dy / len;
         float ny = dx / len;
         Matrix4f pose = guiGraphics.getMatrices().peek().getPositionMatrix();
         VertexConsumer buffer = guiGraphics.getVertexConsumers().getBuffer(STRING_TYPE);
         stringQuad(buffer, pose, ax, ay, bx, by, nx, ny, 1.8F, -13753576);
         stringQuad(buffer, pose, ax, ay, bx, by, nx, ny, 0.9F, -7705782);
         guiGraphics.getVertexConsumers().draw(STRING_TYPE);
      }
   }

   private static void stringQuad(VertexConsumer buffer, Matrix4f pose, float ax, float ay, float bx, float by, float nx, float ny, float width, int color) {
      float half = width / 2.0F;
      buffer.vertex(pose, ax - nx * half, ay - ny * half, 0.0F).color(color);
      buffer.vertex(pose, ax + nx * half, ay + ny * half, 0.0F).color(color);
      buffer.vertex(pose, bx + nx * half, by + ny * half, 0.0F).color(color);
      buffer.vertex(pose, bx - nx * half, by - ny * half, 0.0F).color(color);
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
      CosmeticRenderState.remove(PREVIEW_UUID);
      this.level = null;
      this.dummy = null;
      this.previewBalloon = ItemStack.EMPTY;
   }

   private static RenderLayer flatOutlineType(Identifier texture) {
      return (RenderLayer)GLOW_OUTLINE_TYPES.computeIfAbsent(texture, (tex) -> RenderLayer.of("cobbleclub_gui_glow", VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS, 1536, MultiPhaseParameters.builder().program(RenderPhase.OUTLINE_PROGRAM).texture(new RenderPhase.Texture(tex, false, false)).cull(RenderPhase.DISABLE_CULLING).depthTest(RenderPhase.ALWAYS_DEPTH_TEST).writeMaskState(RenderPhase.COLOR_MASK).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).target(RenderPhase.MAIN_TARGET).build(false)));
   }

   static {
      STRING_TYPE = RenderLayer.of("cobbleclub_gui_balloon_string", VertexFormats.POSITION_COLOR, DrawMode.QUADS, 256, MultiPhaseParameters.builder().program(RenderPhase.COLOR_PROGRAM).cull(RenderPhase.DISABLE_CULLING).depthTest(RenderPhase.ALWAYS_DEPTH_TEST).writeMaskState(RenderPhase.COLOR_MASK).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).target(RenderPhase.MAIN_TARGET).build(false));
      GLOW_HALO = new float[][]{{2.8F, 0.0F}, {-2.8F, 0.0F}, {0.0F, 2.8F}, {0.0F, -2.8F}, {2.0F, 2.0F}, {2.0F, -2.0F}, {-2.0F, 2.0F}, {-2.0F, -2.0F}};
      GLOW_CORE = new float[][]{{1.3F, 0.0F}, {-1.3F, 0.0F}, {0.0F, 1.3F}, {0.0F, -1.3F}, {0.95F, 0.95F}, {0.95F, -0.95F}, {-0.95F, 0.95F}, {-0.95F, -0.95F}};
   }

   @Environment(EnvType.CLIENT)
   private static record FlatColorConsumer(VertexConsumer delegate, int color) implements VertexConsumer {
      public VertexConsumer vertex(float x, float y, float z) {
         this.delegate.vertex(x, y, z).color(this.color);
         return this;
      }

      public VertexConsumer color(int r, int g, int b, int a) {
         return this;
      }

      public VertexConsumer texture(float u, float v) {
         this.delegate.texture(u, v);
         return this;
      }

      public VertexConsumer overlay(int u, int v) {
         return this;
      }

      public VertexConsumer light(int u, int v) {
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         return this;
      }
   }
}
