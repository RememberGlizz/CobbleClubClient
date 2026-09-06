package com.cobbleclub.client.furniturepreview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2246;
import net.minecraft.class_2960;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_7923;
import net.minecraft.class_811;
import net.minecraft.class_9280;
import net.minecraft.class_9282;
import net.minecraft.class_9334;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class FurniturePreviewRenderer {
   private static final float DEG_TO_RAD = ((float)Math.PI / 180F);
   private static final float ISO_TILT = 30.0F;

   private FurniturePreviewRenderer() {
   }

   public static class_1799 stackFor(String material, int customModelData, Integer dyeRgb) {
      class_2960 id = material != null ? class_2960.method_12829(material) : null;
      if (id != null && class_7923.field_41178.method_10250(id)) {
         class_1799 stack = new class_1799((class_1935)class_7923.field_41178.method_10223(id));
         if (stack.method_7960()) {
            stack = new class_1799(class_2246.field_10499);
         }

         stack.method_57379(class_9334.field_49637, new class_9280(customModelData));
         if (dyeRgb != null) {
            stack.method_57379(class_9334.field_49644, new class_9282(dyeRgb & 16777215, false));
         }

         return stack;
      } else {
         return class_1799.field_8037;
      }
   }

   public static void renderIcon(class_332 g, int x0, int y0, int x1, int y1, float zoom, class_1799 stack) {
      if (!stack.method_7960()) {
         float paneMin = (float)Math.min(x1 - x0, y1 - y0);
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         float scale = paneMin / 24.0F * zoom;
         g.method_44379(x0, y0, x1, y1);
         class_4587 pose = g.method_51448();
         pose.method_22903();
         pose.method_22904((double)centerX, (double)centerY, (double)100.0F);
         pose.method_22905(scale, scale, scale);
         g.method_51427(stack, -8, -8);
         pose.method_22909();
         g.method_44380();
      }
   }

   public static void render3D(class_332 g, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, class_1799 stack) {
      if (!stack.method_7960()) {
         class_310 mc = class_310.method_1551();
         float paneMin = (float)Math.min(x1 - x0, y1 - y0);
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         float scale = paneMin * 0.62F * zoom;
         float t = ((float)Math.PI / 6F);
         g.method_44379(x0, y0, x1, y1);
         class_4587 pose = g.method_51448();
         pose.method_22903();
         pose.method_22904((double)centerX, (double)centerY, (double)150.0F);
         pose.method_22905(scale, -scale, scale);
         pose.method_22907((new Quaternionf()).rotationX(pitch * ((float)Math.PI / 180F)));
         pose.method_22907((new Quaternionf()).rotationAxis(yaw * ((float)Math.PI / 180F), 0.0F, (float)Math.cos((double)t), (float)Math.sin((double)t)));
         class_308.method_24211();
         class_4597.class_4598 buffer = g.method_51450();
         mc.method_1480().method_23178(stack, class_811.field_4317, 15728880, class_4608.field_21444, pose, buffer, mc.field_1687, 0);
         g.method_51452();
         pose.method_22909();
         g.method_44380();
         class_308.method_24211();
      }
   }
}
