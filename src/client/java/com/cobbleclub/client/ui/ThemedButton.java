package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ThemedButton extends ButtonWidget {
    private Variant variant = Variant.DEFAULT;

    public ThemedButton(int x, int y, int w, int h, Text label, ButtonWidget.PressAction onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public ThemedButton(int x, int y, int w, int h, Text label, Variant variant, ButtonWidget.PressAction onPress) {
        this(x, y, w, h, label, onPress);
        this.variant = variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    @Override
    protected void renderWidget(DrawContext g, int mouseX, int mouseY, float partialTick) {
        boolean hover = this.isHovered() && this.active;

        int x0 = this.getX();
        int y0 = this.getY();
        int x1 = x0 + this.getWidth();
        int y1 = y0 + this.getHeight();

        int fill = hover ? this.variant.fillHover : this.variant.fill;
        int outline = hover ? this.variant.outlineHover : this.variant.outline;

        if (!this.active) {
            fill = dim(fill);
            outline = dim(outline);
        }

        // Vanilla-inspired 3D button: dark outer frame, raised light top/left,
        // recessed dark bottom/right. Geometry is unchanged.
        g.fill(x0, y0, x1, y1, outline);
        if (this.getWidth() > 2 && this.getHeight() > 2) {
            g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, this.active ? this.variant.bevelTop : dim(this.variant.bevelTop));
            g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, this.active ? this.variant.bevelTop : dim(this.variant.bevelTop));
            g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, this.active ? this.variant.bevelBottom : dim(this.variant.bevelBottom));
            g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, this.active ? this.variant.bevelBottom : dim(this.variant.bevelBottom));
        }

        int textColor = this.active ? (hover ? 0xFFFFFFFF : 0xFFE5E5E5) : 0xFF8A8A8A;
        int textY = y0 + (this.getHeight() - 8) / 2;

        g.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                (x0 + x1) / 2,
                textY,
                textColor
        );
    }

    private static int dim(int argb) {
        return argb & 0xFF000000 | argb >> 1 & 0x7F7F7F;
    }

    @Environment(EnvType.CLIENT)
    public enum Variant {
        DEFAULT(
                0xFF6A6A6A,
                0xFF7A7A7A,
                0xFFA8A8A8,
                0xFF343434,
                0xFF151515,
                0xFFE0E0E0
        ),
        GREEN(
                0xFF3F6D48,
                0xFF4E8259,
                0xFF79A880,
                0xFF24412A,
                0xFF17231A,
                0xFF9DD5A7
        ),
        BLUE(
                0xFF5D6165,
                0xFF73787D,
                0xFFADB1B5,
                0xFF303336,
                0xFF181A1C,
                0xFFE4E6E8
        ),
        RED(
                0xFF75484B,
                0xFF895457,
                0xFFAF7A7D,
                0xFF45292B,
                0xFF291718,
                0xFFD9A2A5
        );

        final int fill;
        final int fillHover;
        final int bevelTop;
        final int bevelBottom;
        final int outline;
        final int outlineHover;

        Variant(int fill, int fillHover, int bevelTop, int bevelBottom, int outline, int outlineHover) {
            this.fill = fill;
            this.fillHover = fillHover;
            this.bevelTop = bevelTop;
            this.bevelBottom = bevelBottom;
            this.outline = outline;
            this.outlineHover = outlineHover;
        }
    }
}
