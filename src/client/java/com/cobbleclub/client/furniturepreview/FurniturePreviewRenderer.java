package com.cobbleclub.client.furniturepreview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class FurniturePreviewRenderer {
   private static final float DEG_TO_RAD = ((float)Math.PI / 180F);
   private static final float ISO_TILT = 30.0F;

   private FurniturePreviewRenderer() {
   }

   public static ItemStack stackFor(String material, int customModelData, Integer dyeRgb) {
      Identifier id = material != null ? Identifier.tryParse(material) : null;
      if (id != null && Registries.ITEM.containsId(id)) {
         ItemStack stack = new ItemStack((ItemConvertible)Registries.ITEM.get(id));
         if (stack.isEmpty()) {
            stack = new ItemStack(Blocks.BARRIER);
         }

         stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(customModelData));
         if (dyeRgb != null) {
            stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(dyeRgb & 16777215, false));
         }

         return stack;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public static void renderIcon(DrawContext g, int x0, int y0, int x1, int y1, float zoom, ItemStack stack) {
      if (!stack.isEmpty()) {
         float paneMin = (float)Math.min(x1 - x0, y1 - y0);
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         float scale = paneMin / 24.0F * zoom;
         g.enableScissor(x0, y0, x1, y1);
         MatrixStack pose = g.getMatrices();
         pose.push();
         pose.translate((double)centerX, (double)centerY, (double)100.0F);
         pose.scale(scale, scale, scale);
         g.drawItem(stack, -8, -8);
         pose.pop();
         g.disableScissor();
      }
   }

   public static void render3D(DrawContext g, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, ItemStack stack) {
      if (!stack.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         float paneMin = (float)Math.min(x1 - x0, y1 - y0);
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         float scale = paneMin * 0.62F * zoom;
         float t = ((float)Math.PI / 6F);
         g.enableScissor(x0, y0, x1, y1);
         MatrixStack pose = g.getMatrices();
         pose.push();
         pose.translate((double)centerX, (double)centerY, (double)150.0F);
         pose.scale(scale, -scale, scale);
         pose.multiply((new Quaternionf()).rotationX(pitch * ((float)Math.PI / 180F)));
         pose.multiply((new Quaternionf()).rotationAxis(yaw * ((float)Math.PI / 180F), 0.0F, (float)Math.cos((double)t), (float)Math.sin((double)t)));
         DiffuseLighting.enableGuiDepthLighting();
         VertexConsumerProvider.Immediate buffer = g.getVertexConsumers();
         mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, 15728880, OverlayTexture.DEFAULT_UV, pose, buffer, mc.world, 0);
         g.draw();
         pose.pop();
         g.disableScissor();
         DiffuseLighting.enableGuiDepthLighting();
      }
   }
}
