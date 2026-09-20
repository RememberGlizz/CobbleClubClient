/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot
 *  com.mojang.authlib.GameProfile
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1664
 *  net.minecraft.class_1799
 *  net.minecraft.class_1921
 *  net.minecraft.class_1921$class_4688
 *  net.minecraft.class_290
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_308
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_4668
 *  net.minecraft.class_4668$class_4683
 *  net.minecraft.class_4668$class_5939
 *  net.minecraft.class_638
 *  net.minecraft.class_745
 *  net.minecraft.class_898
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package com.cobbleclub.client.wardrobe;

import com.cobbleclub.client.wardrobe.CosmeticRenderState;
import com.cobbleclub.client.wardrobe.WardrobeState;
import com.cobbleclub.clubhouse.wardrobe.protocol.WardrobeSlot;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

@Environment(value=EnvType.CLIENT)
public final class WardrobePreviewRenderer {
    private static final UUID PREVIEW_UUID = UUID.nameUUIDFromBytes("cobbleclub:wardrobe-preview".getBytes(StandardCharsets.UTF_8));
    private static final float DEG_TO_RAD = (float)Math.PI / 180;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Map<Identifier, RenderLayer> GLOW_OUTLINE_TYPES = new HashMap<Identifier, RenderLayer>();
    private static final RenderLayer STRING_TYPE = RenderLayer.of((String)"cobbleclub_gui_balloon_string", (VertexFormat)VertexFormats.POSITION_COLOR, (VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS, (int)256, (RenderLayer.MultiPhaseParameters)RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.COLOR_PROGRAM).cull(RenderPhase.DISABLE_CULLING).depthTest(RenderPhase.ALWAYS_DEPTH_TEST).writeMaskState(RenderPhase.COLOR_MASK).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).target(RenderPhase.MAIN_TARGET).build(false));
    private static final double BACKPACK_STAND_Y_OFFSET = -1.0;
    private static final float PLAYER_SCENE_TOP = 2.0f;
    private static final float SCENE_MARGIN = 0.4f;
    private static final float BALLOON_VISUAL_TOP = 2.5f;
    private static final float BALLOON_PREVIEW_MAX_Y = 1.3f;
    private static final float STRING_PLAYER_Y = 1.05f;
    private static final float STRING_BALLOON_Y = 1.35f;
    private static final float HALO_ALPHA = 0.22f;
    private static final float[][] GLOW_HALO = new float[][]{{2.8f, 0.0f}, {-2.8f, 0.0f}, {0.0f, 2.8f}, {0.0f, -2.8f}, {2.0f, 2.0f}, {2.0f, -2.0f}, {-2.0f, 2.0f}, {-2.0f, -2.0f}};
    private static final float[][] GLOW_CORE = new float[][]{{1.3f, 0.0f}, {-1.3f, 0.0f}, {0.0f, 1.3f}, {0.0f, -1.3f}, {0.95f, 0.95f}, {0.95f, -0.95f}, {-0.95f, 0.95f}, {-0.95f, -0.95f}};
    private ClientWorld level;
    private OtherClientPlayerEntity dummy;
    private ItemStack previewBalloon = ItemStack.EMPTY;
    private double[] balloonOffset = WardrobeState.DEFAULT_BALLOON_OFFSET;
    private boolean glowPass;

    private void ensureEntities() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.world != null && minecraft.player != null) {
            if (this.dummy == null || this.level != minecraft.world) {
                this.level = minecraft.world;
                this.dummy = new OtherClientPlayerEntity(minecraft.world, new GameProfile(PREVIEW_UUID, "CobbleClubPreview")){

                    public boolean isPartVisible(PlayerModelPart part) {
                        return WardrobePreviewRenderer.this.glowPass && part == PlayerModelPart.CAPE ? false : MinecraftClient.getInstance().options.isPlayerModelPartEnabled(part);
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

    public void updateEquipment(WardrobeState state) {
        this.updateEquipment(state.previewStack(WardrobeSlot.HELMET), state.previewStack(WardrobeSlot.BACKPACK), state.previewStack(WardrobeSlot.BALLOON), WardrobeState.get().getBalloonOffset());
    }

    public void updateEquipment(ItemStack helmet, ItemStack backpack, ItemStack balloon, double[] balloonOffset) {
        this.ensureEntities();
        if (this.dummy != null) {
            this.balloonOffset = balloonOffset;
            this.previewBalloon = balloon == null ? ItemStack.EMPTY : balloon;
            WardrobePreviewRenderer.setHeadItem((LivingEntity)this.dummy, ItemStack.EMPTY);
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
            double[] balloonOffset = new double[]{this.balloonOffset[0], Math.min(this.balloonOffset[1], (double)1.3f), this.balloonOffset[2]};
            boolean balloonVisible = !this.previewBalloon.isEmpty();
            float sceneTop = balloonVisible ? Math.max(2.0f, (float)balloonOffset[1] + 2.5f) : 2.0f;
            float scale = (float)(y1 - y0) / (sceneTop + 0.4f) * zoom;
            float centerX = (float)(x0 + x1) / 2.0f;
            float centerY = (float)(y0 + y1) / 2.0f;
            guiGraphics.enableScissor(x0, y0, x1, y1);
            guiGraphics.draw();
            RenderSystem.clear((int)256, (boolean)MinecraftClient.IS_SYSTEM_MAC);
            this.renderScene(guiGraphics, centerX, centerY, scale, sceneTop / 2.0f, 180.0f + yaw, pitch, balloonOffset, glowColor);
            guiGraphics.disableScissor();
        }
    }

    private void renderScene(DrawContext guiGraphics, float centerX, float centerY, float scale, float yOffset, float entityYaw, float cameraPitch, double[] balloonOffset, int glowColor) {
        WardrobePreviewRenderer.applyRotation((LivingEntity)this.dummy, entityYaw);
        Quaternionf pitchQuat = new Quaternionf().rotationX(cameraPitch * ((float)Math.PI / 180));
        Quaternionf poseQuat = new Quaternionf().rotationZ((float)Math.PI).mul((Quaternionfc)pitchQuat);
        DiffuseLighting.disableGuiDepthLighting();
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRotation(pitchQuat.conjugate(new Quaternionf()).rotateY((float)Math.PI));
        dispatcher.setRenderShadows(false);
        RenderSystem.runAsFancy(() -> {
            if (glowColor >= 0) {
                RenderLayer glowType = WardrobePreviewRenderer.flatOutlineType(this.dummy.getSkinTextures().texture());
                int argb = 0xFF000000 | glowColor & 0xFFFFFF;
                ItemStack savedHead = this.dummy.getEquippedStack(EquipmentSlot.HEAD);
                this.dummy.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
                this.glowPass = true;
                this.drawGlowTier(guiGraphics, dispatcher, glowType, argb, GLOW_HALO, 0.22f, centerX, centerY, scale, yOffset, poseQuat);
                this.drawGlowTier(guiGraphics, dispatcher, glowType, argb, GLOW_CORE, 1.0f, centerX, centerY, scale, yOffset, poseQuat);
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                this.glowPass = false;
                this.dummy.equipStack(EquipmentSlot.HEAD, savedHead);
            }
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

    private void drawGlowTier(DrawContext guiGraphics, EntityRenderDispatcher dispatcher, RenderLayer glowType, int argb, float[][] offsets, float alpha, float centerX, float centerY, float scale, float yOffset, Quaternionf poseQuat) {
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)alpha);
        for (float[] off : offsets) {
            VertexConsumerProvider glowSource = renderType -> new FlatColorConsumer(guiGraphics.getVertexConsumers().getBuffer(glowType), argb);
            guiGraphics.getMatrices().push();
            guiGraphics.getMatrices().translate((double)(centerX + off[0]), (double)(centerY + off[1]), 49.0);
            guiGraphics.getMatrices().scale(scale, scale, -scale);
            guiGraphics.getMatrices().translate(0.0f, yOffset, 0.0f);
            guiGraphics.getMatrices().multiply(poseQuat);
            dispatcher.render((Entity)this.dummy, 0.0, 0.0, 0.0, 0.0f, 1.0f, guiGraphics.getMatrices(), glowSource, 0xF000F0);
            guiGraphics.getMatrices().pop();
        }
        guiGraphics.getVertexConsumers().draw(glowType);
    }

    private void drawBalloonString(DrawContext guiGraphics, float centerX, float centerY, float scale, float yOffset, Quaternionf poseQuat, double[] balloonOffset) {
        Vector3f a = poseQuat.transform(new Vector3f(0.0f, 1.05f, 0.0f));
        Vector3f b = poseQuat.transform(new Vector3f((float)balloonOffset[0], (float)balloonOffset[1] + 1.35f, (float)balloonOffset[2]));
        float bx = centerX + scale * b.x;
        float ax = centerX + scale * a.x;
        float dx = bx - ax;
        float by = centerY + scale * (b.y + yOffset);
        float ay = centerY + scale * (a.y + yOffset);
        float dy = by - ay;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (!(len < 0.001f)) {
            float nx = -dy / len;
            float ny = dx / len;
            Matrix4f pose = guiGraphics.getMatrices().peek().getPositionMatrix();
            VertexConsumer buffer = guiGraphics.getVertexConsumers().getBuffer(STRING_TYPE);
            WardrobePreviewRenderer.stringQuad(buffer, pose, ax, ay, bx, by, nx, ny, 1.8f, -13753576);
            WardrobePreviewRenderer.stringQuad(buffer, pose, ax, ay, bx, by, nx, ny, 0.9f, -7705782);
            guiGraphics.getVertexConsumers().draw(STRING_TYPE);
        }
    }

    private static void stringQuad(VertexConsumer buffer, Matrix4f pose, float ax, float ay, float bx, float by, float nx, float ny, float width, int color) {
        float half = width / 2.0f;
        buffer.vertex(pose, ax - nx * half, ay - ny * half, 0.0f).color(color);
        buffer.vertex(pose, ax + nx * half, ay + ny * half, 0.0f).color(color);
        buffer.vertex(pose, bx + nx * half, by + ny * half, 0.0f).color(color);
        buffer.vertex(pose, bx - nx * half, by - ny * half, 0.0f).color(color);
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
        CosmeticRenderState.remove(PREVIEW_UUID);
        this.level = null;
        this.dummy = null;
        this.previewBalloon = ItemStack.EMPTY;
    }

    private static RenderLayer flatOutlineType(Identifier texture) {
        return GLOW_OUTLINE_TYPES.computeIfAbsent(texture, tex -> RenderLayer.of((String)"cobbleclub_gui_glow", (VertexFormat)VertexFormats.POSITION_TEXTURE_COLOR, (VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS, (int)1536, (RenderLayer.MultiPhaseParameters)RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.OUTLINE_PROGRAM).texture((RenderPhase.TextureBase)new RenderPhase.Texture(tex, false, false)).cull(RenderPhase.DISABLE_CULLING).depthTest(RenderPhase.ALWAYS_DEPTH_TEST).writeMaskState(RenderPhase.COLOR_MASK).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).target(RenderPhase.MAIN_TARGET).build(false)));
    }

    @Environment(value=EnvType.CLIENT)
    private record FlatColorConsumer(VertexConsumer delegate, int color) implements VertexConsumer
    {
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

