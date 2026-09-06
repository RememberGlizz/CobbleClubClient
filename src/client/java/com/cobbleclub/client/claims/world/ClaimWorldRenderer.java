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
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public final class ClaimWorldRenderer {
   private static final RenderLayer BORDER_BOX;
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
      ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayConnectionEvents.Disconnect)(handler, client) -> snapshot = null);
   }

   public static void accept(ClaimsWorldMsg msg) {
      snapshot = msg.getGroups().isEmpty() ? null : msg;
      snapshotAtMillis = System.currentTimeMillis();
   }

   private static void render(WorldRenderContext context) {
      ClaimsWorldMsg msg = snapshot;
      if (msg != null && context.world() != null && context.matrixStack() != null && context.consumers() != null) {
         if (context.world().getRegistryKey().getValue().toString().equals(msg.getDimension())) {
            long elapsedTicks = (System.currentTimeMillis() - snapshotAtMillis) / 50L;
            MatrixStack poseStack = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            poseStack.push();
            // WorldRenderEvents gives us the world render matrix, but world-space claim coordinates
            // still need one camera translation. Apply it once to the matrix stack (never once per box).
            // This keeps the perimeter anchored to the claim while the camera/player moves.
            var cameraPos = context.camera().getPos();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            VertexConsumer quads = consumers.getBuffer(BORDER_BOX);

            for(WorldBoxEntry entry : msg.getGroups()) {
               if (entry != null && entry.getBox() != null) {
                  WorldBoxType type = entry.getType() != null ? entry.getType() : WorldBoxType.MAIN;
                  float alphaScale = 1.0F;
                  if (entry.getExpiresInTicks() != null) {
                     long remaining = (long)entry.getExpiresInTicks() - elapsedTicks;
                     if (remaining <= 0L) {
                        continue;
                     }

                     alphaScale = Math.min(1.0F, (float)remaining / 40.0F);
                  }

                  BoxInfo box = entry.getBox();
                  // Keep the AABB in absolute world coordinates; the camera translation above is applied once.
                  // A small outward expansion keeps the translucent wall/outline off block faces and prevents
                  // z-fighting shimmer without visibly changing the claimed area.
                  double eps = 0.01D;
                  Box aabb = new Box(
                        (double)box.getMinX() - eps,
                        (double)box.getMinY() - eps,
                        (double)box.getMinZ() - eps,
                        (double)(box.getMaxX() + 1) + eps,
                        (double)(box.getMaxY() + 1) + eps,
                        (double)(box.getMaxZ() + 1) + eps);
                  float[] rgb = colorFor(type);
                  float pulse = type == WorldBoxType.DENIAL ? 0.75F + 0.25F * (float)Math.sin((double)System.currentTimeMillis() / (double)120.0F) : 1.0F;
                  WorldRenderer.renderFilledBox(poseStack, quads, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, rgb[0], rgb[1], rgb[2], 0.055F * alphaScale * pulse);
                  WorldRenderer.drawBox(poseStack, lines, aabb, rgb[0], rgb[1], rgb[2], 1.0F * alphaScale * pulse);
               }
            }

            poseStack.pop();
         }
      }
   }

   private static float[] colorFor(WorldBoxType type) {
      float[] var10000;
      switch (type) {
         case MAIN -> var10000 = MAIN_RGB;
         case OTHER -> var10000 = OTHER_RGB;
         case SUB -> var10000 = SUB_RGB;
         case EDIT -> var10000 = EDIT_RGB;
         case CORNER -> var10000 = CORNER_RGB;
         case DRAG -> var10000 = DRAG_RGB;
         case DENIAL -> var10000 = DENIAL_RGB;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   static {
      BORDER_BOX = RenderLayer.of("cobbleclub_claim_border", VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_STRIP, 1536, MultiPhaseParameters.builder().program(RenderPhase.COLOR_PROGRAM).cull(RenderPhase.DISABLE_CULLING).writeMaskState(RenderPhase.COLOR_MASK).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).build(false));
      MAIN_RGB = new float[]{1.0F, 0.78F, 0.24F};
      OTHER_RGB = new float[]{0.14F, 0.54F, 0.78F};
      SUB_RGB = new float[]{0.78F, 0.8F, 0.83F};
      EDIT_RGB = new float[]{0.24F, 0.4F, 0.86F};
      CORNER_RGB = new float[]{0.55F, 0.86F, 0.95F};
      DRAG_RGB = new float[]{0.98F, 0.9F, 0.31F};
      DENIAL_RGB = new float[]{1.0F, 0.35F, 0.25F};
   }
}
