/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.claims.protocol.BoxInfo
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsWorldMsg
 *  com.cobbleclub.clubhouse.claims.protocol.WorldBoxEntry
 *  com.cobbleclub.clubhouse.claims.protocol.WorldBoxType
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
 *  net.minecraft.class_1921
 *  net.minecraft.class_1921$class_4688
 *  net.minecraft.class_238
 *  net.minecraft.class_290
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4587
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_4668
 *  net.minecraft.class_761
 */
package com.cobbleclub.client.claims.world;

import com.cobbleclub.clubhouse.claims.protocol.BoxInfo;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsWorldMsg;
import com.cobbleclub.clubhouse.claims.protocol.WorldBoxEntry;
import com.cobbleclub.clubhouse.claims.protocol.WorldBoxType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Box;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.WorldRenderer;

@Environment(value=EnvType.CLIENT)
public final class ClaimWorldRenderer {
    private static final RenderLayer BORDER_BOX = RenderLayer.of((String)"cobbleclub_claim_border", (VertexFormat)VertexFormats.POSITION_COLOR, (VertexFormat.DrawMode)VertexFormat.DrawMode.TRIANGLE_STRIP, (int)1536, (RenderLayer.MultiPhaseParameters)RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.COLOR_PROGRAM).cull(RenderPhase.DISABLE_CULLING).writeMaskState(RenderPhase.COLOR_MASK).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).build(false));
    private static volatile ClaimsWorldMsg snapshot;
    private static volatile long snapshotAtMillis;
    private static final float[] MAIN_RGB;
    private static final float[] OTHER_RGB;
    private static final float[] SUB_RGB;
    private static final float[] EDIT_RGB;
    private static final float[] CORNER_RGB;
    private static final float[] DRAG_RGB;
    private static final float[] DENIAL_RGB;

    private ClaimWorldRenderer() {
    }

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(ClaimWorldRenderer::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            snapshot = null;
        });
    }

    public static void accept(ClaimsWorldMsg msg) {
        snapshot = msg.getGroups().isEmpty() ? null : msg;
        snapshotAtMillis = System.currentTimeMillis();
    }

    private static void render(WorldRenderContext context) {
        ClaimsWorldMsg msg = snapshot;
        if (msg != null && context.world() != null && context.matrixStack() != null && context.consumers() != null && context.world().getRegistryKey().getValue().toString().equals(msg.getDimension())) {
            long elapsedTicks = (System.currentTimeMillis() - snapshotAtMillis) / 50L;
            MatrixStack poseStack = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            poseStack.push();
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            VertexConsumer quads = consumers.getBuffer(BORDER_BOX);
            for (WorldBoxEntry entry : msg.getGroups()) {
                if (entry == null || entry.getBox() == null) continue;
                WorldBoxType type = entry.getType() != null ? entry.getType() : WorldBoxType.MAIN;
                float alphaScale = 1.0f;
                if (entry.getExpiresInTicks() != null) {
                    long remaining = (long)entry.getExpiresInTicks().intValue() - elapsedTicks;
                    if (remaining <= 0L) continue;
                    alphaScale = Math.min(1.0f, (float)remaining / 40.0f);
                }
                BoxInfo box = entry.getBox();
                double eps = 0.01;
                Box aabb = new Box((double)box.getMinX() - eps, (double)box.getMinY() - eps, (double)box.getMinZ() - eps, (double)(box.getMaxX() + 1) + eps, (double)(box.getMaxY() + 1) + eps, (double)(box.getMaxZ() + 1) + eps);
                float[] rgb = ClaimWorldRenderer.colorFor(type);
                float pulse = type == WorldBoxType.DENIAL ? 0.75f + 0.25f * (float)Math.sin((double)System.currentTimeMillis() / 120.0) : 1.0f;
                WorldRenderer.renderFilledBox((MatrixStack)poseStack, (VertexConsumer)quads, (double)aabb.minX, (double)aabb.minY, (double)aabb.minZ, (double)aabb.maxX, (double)aabb.maxY, (double)aabb.maxZ, (float)rgb[0], (float)rgb[1], (float)rgb[2], (float)(0.055f * alphaScale * pulse));
                WorldRenderer.drawBox((MatrixStack)poseStack, (VertexConsumer)lines, (Box)aabb, (float)rgb[0], (float)rgb[1], (float)rgb[2], (float)(1.0f * alphaScale * pulse));
            }
            poseStack.pop();
        }
    }

    private static float[] colorFor(WorldBoxType type) {
        return switch (type) {
            case MAIN -> MAIN_RGB;
            case OTHER -> OTHER_RGB;
            case SUB -> SUB_RGB;
            case EDIT -> EDIT_RGB;
            case CORNER -> CORNER_RGB;
            case DRAG -> DRAG_RGB;
            case DENIAL -> DENIAL_RGB;
        };
    }

    static {
        MAIN_RGB = new float[]{1.0f, 0.78f, 0.24f};
        OTHER_RGB = new float[]{0.14f, 0.54f, 0.78f};
        SUB_RGB = new float[]{0.78f, 0.8f, 0.83f};
        EDIT_RGB = new float[]{0.24f, 0.4f, 0.86f};
        CORNER_RGB = new float[]{0.55f, 0.86f, 0.95f};
        DRAG_RGB = new float[]{0.98f, 0.9f, 0.31f};
        DENIAL_RGB = new float[]{1.0f, 0.35f, 0.25f};
    }
}

