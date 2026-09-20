/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_1937
 *  net.minecraft.class_2246
 *  net.minecraft.class_2960
 *  net.minecraft.class_308
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4608
 *  net.minecraft.class_7923
 *  net.minecraft.class_811
 *  net.minecraft.class_9280
 *  net.minecraft.class_9282
 *  net.minecraft.class_9334
 *  org.joml.Quaternionf
 */
package com.cobbleclub.client.furniturepreview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.registry.Registries;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.DataComponentTypes;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public final class FurniturePreviewRenderer {
    private static final float DEG_TO_RAD = (float)Math.PI / 180;
    private static final float ISO_TILT = 30.0f;

    private FurniturePreviewRenderer() {
    }

    public static ItemStack stackFor(String material, int customModelData, Integer dyeRgb) {
        Identifier id;
        Identifier parsedId = id = material != null ? Identifier.tryParse((String)material) : null;
        if (id != null && Registries.ITEM.containsId(id)) {
            ItemStack stack = new ItemStack((ItemConvertible)Registries.ITEM.get(id));
            if (stack.isEmpty()) {
                stack = new ItemStack((ItemConvertible)Blocks.BARRIER);
            }
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(customModelData));
            if (dyeRgb != null) {
                stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(dyeRgb & 0xFFFFFF, false));
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public static void renderIcon(DrawContext g, int x0, int y0, int x1, int y1, float zoom, ItemStack stack) {
        if (!stack.isEmpty()) {
            float paneMin = Math.min(x1 - x0, y1 - y0);
            float centerX = (float)(x0 + x1) / 2.0f;
            float centerY = (float)(y0 + y1) / 2.0f;
            float scale = paneMin / 24.0f * zoom;
            g.enableScissor(x0, y0, x1, y1);
            MatrixStack pose = g.getMatrices();
            pose.push();
            pose.translate((double)centerX, (double)centerY, 100.0);
            pose.scale(scale, scale, scale);
            g.drawItem(stack, -8, -8);
            pose.pop();
            g.disableScissor();
        }
    }

    public static void render3D(DrawContext g, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, ItemStack stack) {
        if (!stack.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            float paneMin = Math.min(x1 - x0, y1 - y0);
            float centerX = (float)(x0 + x1) / 2.0f;
            float centerY = (float)(y0 + y1) / 2.0f;
            float scale = paneMin * 0.62f * zoom;
            float t = 0.5235988f;
            g.enableScissor(x0, y0, x1, y1);
            MatrixStack pose = g.getMatrices();
            pose.push();
            pose.translate((double)centerX, (double)centerY, 150.0);
            pose.scale(scale, -scale, scale);
            pose.multiply(new Quaternionf().rotationX(pitch * ((float)Math.PI / 180)));
            pose.multiply(new Quaternionf().rotationAxis(yaw * ((float)Math.PI / 180), 0.0f, (float)Math.cos(t), (float)Math.sin(t)));
            DiffuseLighting.enableGuiDepthLighting();
            VertexConsumerProvider.Immediate buffer = g.getVertexConsumers();
            mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, 0xF000F0, OverlayTexture.DEFAULT_UV, pose, (VertexConsumerProvider)buffer, (World)mc.world, 0);
            g.draw();
            pose.pop();
            g.disableScissor();
            DiffuseLighting.enableGuiDepthLighting();
        }
    }
}

