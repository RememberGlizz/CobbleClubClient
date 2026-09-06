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
import net.minecraft.class_1921;
import net.minecraft.class_238;
import net.minecraft.class_290;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4668;
import net.minecraft.class_761;
import net.minecraft.class_1921.class_4688;
import net.minecraft.class_293.class_5596;

@Environment(EnvType.CLIENT)
public final class ClaimWorldRenderer {
   private static final class_1921 BORDER_BOX;
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
         if (context.world().method_27983().method_29177().toString().equals(msg.getDimension())) {
            long elapsedTicks = (System.currentTimeMillis() - snapshotAtMillis) / 50L;
            class_4587 poseStack = context.matrixStack();
            class_4597 consumers = context.consumers();
            poseStack.method_22903();
            // WorldRenderEvents gives us the world render matrix, but world-space claim coordinates
            // still need one camera translation. Apply it once to the matrix stack (never once per box).
            // This keeps the perimeter anchored to the claim while the camera/player moves.
            var cameraPos = context.camera().method_19326();
            poseStack.method_22904(-cameraPos.field_1352, -cameraPos.field_1351, -cameraPos.field_1350);
            class_4588 lines = consumers.getBuffer(class_1921.method_23594());
            class_4588 quads = consumers.getBuffer(BORDER_BOX);

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
                  class_238 aabb = new class_238(
                        (double)box.getMinX() - eps,
                        (double)box.getMinY() - eps,
                        (double)box.getMinZ() - eps,
                        (double)(box.getMaxX() + 1) + eps,
                        (double)(box.getMaxY() + 1) + eps,
                        (double)(box.getMaxZ() + 1) + eps);
                  float[] rgb = colorFor(type);
                  float pulse = type == WorldBoxType.DENIAL ? 0.75F + 0.25F * (float)Math.sin((double)System.currentTimeMillis() / (double)120.0F) : 1.0F;
                  class_761.method_3258(poseStack, quads, aabb.field_1323, aabb.field_1322, aabb.field_1321, aabb.field_1320, aabb.field_1325, aabb.field_1324, rgb[0], rgb[1], rgb[2], 0.055F * alphaScale * pulse);
                  class_761.method_22982(poseStack, lines, aabb, rgb[0], rgb[1], rgb[2], 1.0F * alphaScale * pulse);
               }
            }

            poseStack.method_22909();
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
      BORDER_BOX = class_1921.method_24048("cobbleclub_claim_border", class_290.field_1576, class_5596.field_27380, 1536, class_4688.method_23598().method_34578(class_4668.field_29442).method_23603(class_4668.field_21345).method_23616(class_4668.field_21350).method_23615(class_4668.field_21370).method_23617(false));
      MAIN_RGB = new float[]{1.0F, 0.78F, 0.24F};
      OTHER_RGB = new float[]{0.14F, 0.54F, 0.78F};
      SUB_RGB = new float[]{0.78F, 0.8F, 0.83F};
      EDIT_RGB = new float[]{0.24F, 0.4F, 0.86F};
      CORNER_RGB = new float[]{0.55F, 0.86F, 0.95F};
      DRAG_RGB = new float[]{0.98F, 0.9F, 0.31F};
      DENIAL_RGB = new float[]{1.0F, 0.35F, 0.25F};
   }
}
