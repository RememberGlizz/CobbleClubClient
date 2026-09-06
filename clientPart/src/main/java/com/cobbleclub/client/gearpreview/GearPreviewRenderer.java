package com.cobbleclub.client.gearpreview;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1664;
import net.minecraft.class_1799;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4608;
import net.minecraft.class_638;
import net.minecraft.class_745;
import net.minecraft.class_811;
import net.minecraft.class_898;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class GearPreviewRenderer {
   private static final float DEG_TO_RAD = ((float)Math.PI / 180F);
   private static final int FULL_BRIGHT = 15728880;
   private static final float SCENE_TOP = 2.0F;
   private static final float SCENE_MARGIN = 0.4F;
   private static final class_1304[] PREVIEW_SLOTS;
   private class_638 level;
   private class_745 dummy;

   private void ensureDummy() {
      class_310 minecraft = class_310.method_1551();
      if (minecraft.field_1687 != null && minecraft.field_1724 != null) {
         if (this.dummy == null || this.level != minecraft.field_1687) {
            this.level = minecraft.field_1687;
            this.dummy = new class_745(minecraft.field_1687, minecraft.field_1724.method_7334()) {
               public boolean method_7348(class_1664 part) {
                  return class_310.method_1551().field_1690.method_32594(part);
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

   private void applyEquipment(Map<class_1304, class_1799> equipment) {
      for(class_1304 slot : PREVIEW_SLOTS) {
         class_1799 stack = (class_1799)equipment.getOrDefault(slot, class_1799.field_8037);
         if (this.dummy.method_6118(slot) != stack) {
            this.dummy.method_5673(slot, stack);
         }
      }

   }

   public void render(class_332 guiGraphics, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, Map<class_1304, class_1799> equipment) {
      this.ensureDummy();
      if (this.dummy != null) {
         this.applyEquipment(equipment);
         float scale = (float)(y1 - y0) / 2.4F * zoom;
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         guiGraphics.method_44379(x0, y0, x1, y1);
         this.renderScene(guiGraphics, centerX, centerY, scale, 1.0F, 180.0F + yaw, pitch);
         guiGraphics.method_44380();
      }
   }

   private void renderScene(class_332 guiGraphics, float centerX, float centerY, float scale, float yOffset, float entityYaw, float cameraPitch) {
      applyRotation(this.dummy, entityYaw);
      Quaternionf pitchQuat = (new Quaternionf()).rotationX(cameraPitch * ((float)Math.PI / 180F));
      Quaternionf poseQuat = (new Quaternionf()).rotationZ((float)Math.PI).mul(pitchQuat);
      class_308.method_34742();
      class_898 dispatcher = class_310.method_1551().method_1561();
      dispatcher.method_24196(pitchQuat.conjugate(new Quaternionf()).rotateY((float)Math.PI));
      dispatcher.method_3948(false);
      RenderSystem.runAsFancy(() -> {
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

   public static void renderItem3D(class_332 guiGraphics, int x0, int y0, int x1, int y1, Quaternionf orientation, float zoom, class_1799 stack) {
      if (!stack.method_7960()) {
         class_310 mc = class_310.method_1551();
         float paneMin = (float)Math.min(x1 - x0, y1 - y0);
         float scale = paneMin * 0.62F * zoom;
         guiGraphics.method_44379(x0, y0, x1, y1);
         guiGraphics.method_51448().method_22903();
         guiGraphics.method_51448().method_22904((double)((float)(x0 + x1) / 2.0F), (double)((float)(y0 + y1) / 2.0F), (double)150.0F);
         guiGraphics.method_51448().method_22905(scale, -scale, scale);
         guiGraphics.method_51448().method_22907(orientation);
         class_308.method_24211();
         mc.method_1480().method_23178(stack, class_811.field_4317, 15728880, class_4608.field_21444, guiGraphics.method_51448(), guiGraphics.method_51450(), mc.field_1687, 0);
         guiGraphics.method_51452();
         guiGraphics.method_51448().method_22909();
         guiGraphics.method_44380();
         class_308.method_24211();
      }
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
      this.level = null;
      this.dummy = null;
   }

   static {
      PREVIEW_SLOTS = new class_1304[]{class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166, class_1304.field_6173, class_1304.field_6171};
   }
}
