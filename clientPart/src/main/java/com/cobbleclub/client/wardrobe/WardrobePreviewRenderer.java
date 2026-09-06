package com.cobbleclub.client.wardrobe;

import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1664;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4668;
import net.minecraft.class_638;
import net.minecraft.class_745;
import net.minecraft.class_898;
import net.minecraft.class_1921.class_4688;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class WardrobePreviewRenderer {
   private static final UUID PREVIEW_UUID = UUID.nameUUIDFromBytes("cobbleclub:wardrobe-preview".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   private static final float DEG_TO_RAD = ((float)Math.PI / 180F);
   private static final int FULL_BRIGHT = 15728880;
   private static final Map<class_2960, class_1921> GLOW_OUTLINE_TYPES = new HashMap();
   private static final class_1921 STRING_TYPE;
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
   private class_638 level;
   private class_745 dummy;
   private class_1799 previewBalloon = class_1799.field_8037;
   private double[] balloonOffset;
   private boolean glowPass;

   public WardrobePreviewRenderer() {
      this.balloonOffset = WardrobeState.DEFAULT_BALLOON_OFFSET;
   }

   private void ensureEntities() {
      class_310 minecraft = class_310.method_1551();
      if (minecraft.field_1687 != null && minecraft.field_1724 != null) {
         if (this.dummy == null || this.level != minecraft.field_1687) {
            this.level = minecraft.field_1687;
            this.dummy = new class_745(minecraft.field_1687, new GameProfile(PREVIEW_UUID, "CobbleClubPreview")) {
               public boolean method_7348(class_1664 part) {
                  return WardrobePreviewRenderer.this.glowPass && part == class_1664.field_7559 ? false : class_310.method_1551().field_1690.method_32594(part);
               }
            };
            double px = minecraft.field_1724.method_23317();
            double py = minecraft.field_1724.method_23318() - (double)1000.0F;
            double pz = minecraft.field_1724.method_23321();
            this.dummy.method_5814(px, py, pz);
            this.dummy.method_22862();
            this.dummy.field_7500 = this.dummy.field_7524 = px;
            this.dummy.field_7521 = this.dummy.field_7502 = py;
            this.dummy.field_7499 = this.dummy.field_7522 = pz;
            this.dummy.method_5803(true);
         }
      } else {
         this.close();
      }
   }

   public void updateEquipment(WardrobeState state) {
      this.updateEquipment(state.previewStack(WardrobeSlot.HELMET), state.previewStack(WardrobeSlot.BACKPACK), state.previewStack(WardrobeSlot.BALLOON), WardrobeState.get().getBalloonOffset());
   }

   public void updateEquipment(class_1799 helmet, class_1799 backpack, class_1799 balloon, double[] balloonOffset) {
      this.ensureEntities();
      if (this.dummy != null) {
         this.balloonOffset = balloonOffset;
         this.previewBalloon = balloon == null ? class_1799.field_8037 : balloon;
         setHeadItem(this.dummy, class_1799.field_8037);
         CosmeticRenderState.putPreview(PREVIEW_UUID, helmet, backpack, balloon);
      }
   }

   public void updateEquipment(class_1799 helmet, class_1799 backpack, class_1799 balloon) {
      this.updateEquipment(helmet, backpack, balloon, WardrobeState.DEFAULT_BALLOON_OFFSET);
   }

   private static void setHeadItem(class_1309 entity, class_1799 stack) {
      if (entity.method_6118(class_1304.field_6169) != stack) {
         entity.method_5673(class_1304.field_6169, stack);
      }

   }

   public void render(class_332 guiGraphics, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, int glowColor, float partialTick) {
      this.ensureEntities();
      if (this.dummy != null) {
         double[] balloonOffset = new double[]{this.balloonOffset[0], Math.min(this.balloonOffset[1], (double)1.3F), this.balloonOffset[2]};
         boolean balloonVisible = !this.previewBalloon.method_7960();
         float sceneTop = balloonVisible ? Math.max(2.0F, (float)balloonOffset[1] + 2.5F) : 2.0F;
         float scale = (float)(y1 - y0) / (sceneTop + 0.4F) * zoom;
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         guiGraphics.method_44379(x0, y0, x1, y1);
         guiGraphics.method_51452();
         RenderSystem.clear(256, class_310.field_1703);
         this.renderScene(guiGraphics, centerX, centerY, scale, sceneTop / 2.0F, 180.0F + yaw, pitch, balloonOffset, glowColor);
         guiGraphics.method_44380();
      }
   }

   private void renderScene(class_332 guiGraphics, float centerX, float centerY, float scale, float yOffset, float entityYaw, float cameraPitch, double[] balloonOffset, int glowColor) {
      applyRotation(this.dummy, entityYaw);
      Quaternionf pitchQuat = (new Quaternionf()).rotationX(cameraPitch * ((float)Math.PI / 180F));
      Quaternionf poseQuat = (new Quaternionf()).rotationZ((float)Math.PI).mul(pitchQuat);
      class_308.method_34742();
      class_898 dispatcher = class_310.method_1551().method_1561();
      dispatcher.method_24196(pitchQuat.conjugate(new Quaternionf()).rotateY((float)Math.PI));
      dispatcher.method_3948(false);
      RenderSystem.runAsFancy(() -> {
         if (glowColor >= 0) {
            class_1921 glowType = flatOutlineType(this.dummy.method_52814().comp_1626());
            int argb = -16777216 | glowColor & 16777215;
            class_1799 savedHead = this.dummy.method_6118(class_1304.field_6169);
            this.dummy.method_5673(class_1304.field_6169, class_1799.field_8037);
            this.glowPass = true;
            this.drawGlowTier(guiGraphics, dispatcher, glowType, argb, GLOW_HALO, 0.22F, centerX, centerY, scale, yOffset, poseQuat);
            this.drawGlowTier(guiGraphics, dispatcher, glowType, argb, GLOW_CORE, 1.0F, centerX, centerY, scale, yOffset, poseQuat);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.glowPass = false;
            this.dummy.method_5673(class_1304.field_6169, savedHead);
         }

         guiGraphics.method_51448().method_22903();
         guiGraphics.method_51448().method_22904((double)centerX, (double)centerY, (double)50.0F);
         guiGraphics.method_51448().method_22905(scale, scale, -scale);
         guiGraphics.method_51448().method_46416(0.0F, yOffset, 0.0F);
         guiGraphics.method_51448().method_22907(poseQuat);
         dispatcher.method_3954(this.dummy, (double)0.0F, (double)0.0F, (double)0.0F, 0.0F, 1.0F, guiGraphics.method_51448(), guiGraphics.method_51450(), 15728880);
         guiGraphics.method_51448().method_22909();
      });
      guiGraphics.method_51452();
      dispatcher.method_3948(true);
      class_308.method_24211();
   }

   private void drawGlowTier(class_332 guiGraphics, class_898 dispatcher, class_1921 glowType, int argb, float[][] offsets, float alpha, float centerX, float centerY, float scale, float yOffset, Quaternionf poseQuat) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

      for(float[] off : offsets) {
         class_4597 glowSource = (renderType) -> new FlatColorConsumer(guiGraphics.method_51450().getBuffer(glowType), argb);
         guiGraphics.method_51448().method_22903();
         guiGraphics.method_51448().method_22904((double)(centerX + off[0]), (double)(centerY + off[1]), (double)49.0F);
         guiGraphics.method_51448().method_22905(scale, scale, -scale);
         guiGraphics.method_51448().method_46416(0.0F, yOffset, 0.0F);
         guiGraphics.method_51448().method_22907(poseQuat);
         dispatcher.method_3954(this.dummy, (double)0.0F, (double)0.0F, (double)0.0F, 0.0F, 1.0F, guiGraphics.method_51448(), glowSource, 15728880);
         guiGraphics.method_51448().method_22909();
      }

      guiGraphics.method_51450().method_22994(glowType);
   }

   private void drawBalloonString(class_332 guiGraphics, float centerX, float centerY, float scale, float yOffset, Quaternionf poseQuat, double[] balloonOffset) {
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
         Matrix4f pose = guiGraphics.method_51448().method_23760().method_23761();
         class_4588 buffer = guiGraphics.method_51450().getBuffer(STRING_TYPE);
         stringQuad(buffer, pose, ax, ay, bx, by, nx, ny, 1.8F, -13753576);
         stringQuad(buffer, pose, ax, ay, bx, by, nx, ny, 0.9F, -7705782);
         guiGraphics.method_51450().method_22994(STRING_TYPE);
      }
   }

   private static void stringQuad(class_4588 buffer, Matrix4f pose, float ax, float ay, float bx, float by, float nx, float ny, float width, int color) {
      float half = width / 2.0F;
      buffer.method_22918(pose, ax - nx * half, ay - ny * half, 0.0F).method_39415(color);
      buffer.method_22918(pose, ax + nx * half, ay + ny * half, 0.0F).method_39415(color);
      buffer.method_22918(pose, bx + nx * half, by + ny * half, 0.0F).method_39415(color);
      buffer.method_22918(pose, bx - nx * half, by - ny * half, 0.0F).method_39415(color);
   }

   private static void applyRotation(class_1309 entity, float yaw) {
      entity.field_6283 = yaw;
      entity.field_6220 = yaw;
      entity.field_6241 = yaw;
      entity.field_6259 = yaw;
      entity.method_36456(yaw);
      entity.method_36457(0.0F);
   }

   public void close() {
      CosmeticRenderState.remove(PREVIEW_UUID);
      this.level = null;
      this.dummy = null;
      this.previewBalloon = class_1799.field_8037;
   }

   private static class_1921 flatOutlineType(class_2960 texture) {
      return (class_1921)GLOW_OUTLINE_TYPES.computeIfAbsent(texture, (tex) -> class_1921.method_24048("cobbleclub_gui_glow", class_290.field_1575, class_5596.field_27382, 1536, class_4688.method_23598().method_34578(class_4668.field_29418).method_34577(new class_4668.class_4683(tex, false, false)).method_23603(class_4668.field_21345).method_23604(class_4668.field_21346).method_23616(class_4668.field_21350).method_23615(class_4668.field_21370).method_23610(class_4668.field_21358).method_23617(false)));
   }

   static {
      STRING_TYPE = class_1921.method_24048("cobbleclub_gui_balloon_string", class_290.field_1576, class_5596.field_27382, 256, class_4688.method_23598().method_34578(class_4668.field_29442).method_23603(class_4668.field_21345).method_23604(class_4668.field_21346).method_23616(class_4668.field_21350).method_23615(class_4668.field_21370).method_23610(class_4668.field_21358).method_23617(false));
      GLOW_HALO = new float[][]{{2.8F, 0.0F}, {-2.8F, 0.0F}, {0.0F, 2.8F}, {0.0F, -2.8F}, {2.0F, 2.0F}, {2.0F, -2.0F}, {-2.0F, 2.0F}, {-2.0F, -2.0F}};
      GLOW_CORE = new float[][]{{1.3F, 0.0F}, {-1.3F, 0.0F}, {0.0F, 1.3F}, {0.0F, -1.3F}, {0.95F, 0.95F}, {0.95F, -0.95F}, {-0.95F, 0.95F}, {-0.95F, -0.95F}};
   }

   @Environment(EnvType.CLIENT)
   private static record FlatColorConsumer(class_4588 delegate, int color) implements class_4588 {
      public class_4588 method_22912(float x, float y, float z) {
         this.delegate.method_22912(x, y, z).method_39415(this.color);
         return this;
      }

      public class_4588 method_1336(int r, int g, int b, int a) {
         return this;
      }

      public class_4588 method_22913(float u, float v) {
         this.delegate.method_22913(u, v);
         return this;
      }

      public class_4588 method_60796(int u, int v) {
         return this;
      }

      public class_4588 method_22921(int u, int v) {
         return this;
      }

      public class_4588 method_22914(float x, float y, float z) {
         return this;
      }
   }
}
