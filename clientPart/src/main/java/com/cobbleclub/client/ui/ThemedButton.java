package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;

@Environment(EnvType.CLIENT)
public final class ThemedButton extends class_4185 {
   private Variant variant;

   public ThemedButton(int x, int y, int w, int h, class_2561 label, class_4185.class_4241 onPress) {
      super(x, y, w, h, label, onPress, field_40754);
      this.variant = ThemedButton.Variant.DEFAULT;
   }

   public ThemedButton(int x, int y, int w, int h, class_2561 label, Variant variant, class_4185.class_4241 onPress) {
      this(x, y, w, h, label, onPress);
      this.variant = variant;
   }

   public void setVariant(Variant variant) {
      this.variant = variant;
   }

   protected void method_48579(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
      boolean hover = this.method_49606() && this.field_22763;
      int x0 = this.method_46426();
      int y0 = this.method_46427();
      int x1 = x0 + this.method_25368();
      int y1 = y0 + this.method_25364();
      int fill = hover ? this.variant.fillHover : this.variant.fill;
      int outline = hover ? this.variant.outlineHover : this.variant.outline;
      guiGraphics.method_25294(x0, y0, x1, y1, this.field_22763 ? fill : dim(fill));
      guiGraphics.method_25294(x0, y0, x1, y0 + 1, this.variant.bevelTop);
      guiGraphics.method_25294(x0, y1 - 1, x1, y1, this.variant.bevelBottom);
      guiGraphics.method_49601(x0, y0, this.method_25368(), this.method_25364(), this.field_22763 ? outline : dim(outline));
      int textColor = this.field_22763 ? (!hover && this.variant == ThemedButton.Variant.DEFAULT ? -2962968 : -1) : -9805184;
      guiGraphics.method_27534(class_310.method_1551().field_1772, this.method_25369(), (x0 + x1) / 2, y0 + (this.method_25364() - 8) / 2, textColor);
   }

   private static int dim(int argb) {
      return argb & -16777216 | argb >> 1 & 8355711;
   }

   @Environment(EnvType.CLIENT)
   public static enum Variant {
      DEFAULT(-15199710, -13950142, -13161134, -16119790, -16447985, -9815394),
      GREEN(-15452897, -14791378, -13997504, -16313591, -13726644, -11351944),
      BLUE(-15455926, -14795410, -14003057, -16314854, -12683326, -10049038),
      RED(-12970725, -11263960, -9557452, -15530231, -5227962, -2072211);

      final int fill;
      final int fillHover;
      final int bevelTop;
      final int bevelBottom;
      final int outline;
      final int outlineHover;

      private Variant(int fill, int fillHover, int bevelTop, int bevelBottom, int outline, int outlineHover) {
         this.fill = fill;
         this.fillHover = fillHover;
         this.bevelTop = bevelTop;
         this.bevelBottom = bevelBottom;
         this.outline = outline;
         this.outlineHover = outlineHover;
      }

      // $FF: synthetic method
      private static Variant[] $values() {
         return new Variant[]{DEFAULT, GREEN, BLUE, RED};
      }
   }
}
