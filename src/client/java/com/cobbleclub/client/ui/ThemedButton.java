package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ThemedButton extends ButtonWidget {
   private Variant variant;

   public ThemedButton(int x, int y, int w, int h, Text label, ButtonWidget.PressAction onPress) {
      super(x, y, w, h, label, onPress, DEFAULT_NARRATION_SUPPLIER);
      this.variant = ThemedButton.Variant.DEFAULT;
   }

   public ThemedButton(int x, int y, int w, int h, Text label, Variant variant, ButtonWidget.PressAction onPress) {
      this(x, y, w, h, label, onPress);
      this.variant = variant;
   }

   public void setVariant(Variant variant) {
      this.variant = variant;
   }

   protected void renderWidget(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
      boolean hover = this.isHovered() && this.active;
      int x0 = this.getX();
      int y0 = this.getY();
      int x1 = x0 + this.getWidth();
      int y1 = y0 + this.getHeight();
      int fill = hover ? this.variant.fillHover : this.variant.fill;
      int outline = hover ? this.variant.outlineHover : this.variant.outline;
      guiGraphics.fill(x0, y0, x1, y1, this.active ? fill : dim(fill));
      guiGraphics.fill(x0, y0, x1, y0 + 1, this.variant.bevelTop);
      guiGraphics.fill(x0, y1 - 1, x1, y1, this.variant.bevelBottom);
      guiGraphics.drawBorder(x0, y0, this.getWidth(), this.getHeight(), this.active ? outline : dim(outline));
      int textColor = this.active ? (!hover && this.variant == ThemedButton.Variant.DEFAULT ? -2962968 : -1) : -9805184;
      guiGraphics.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(), (x0 + x1) / 2, y0 + (this.getHeight() - 8) / 2, textColor);
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
