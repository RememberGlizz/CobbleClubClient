package com.cobbleclub.client.wardrobe;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class WardrobeButton extends ButtonWidget {
   public static final int HEIGHT = 20;
   private static final int PAD_X = 5;
   private static final int ICON_SIZE = 16;
   private static final int ICON_TEXT_GAP = 3;
   private static final int LABEL_COLOR = -2570241;
   private static final int TOOLTIP_TITLE_COLOR = 13215999;
   private static final int TOOLTIP_DESC_COLOR = 11053240;
   private static final ItemStack ICON;
   private static final Text LABEL;
   private static final ButtonTextures BUTTON_SPRITES;

   public WardrobeButton(ButtonWidget.PressAction onPress) {
      super(0, 0, width(MinecraftClient.getInstance().textRenderer), 20, LABEL, onPress, DEFAULT_NARRATION_SUPPLIER);
      this.setTooltip(Tooltip.of(Text.translatable("cobbleclub.wardrobe.button").withColor(13215999).append(Text.literal("\n")).append(Text.translatable("cobbleclub.wardrobe.tooltip").withColor(11053240))));
   }

   private static int width(TextRenderer font) {
      return 24 + font.getWidth(LABEL) + 5;
   }

   protected void renderWidget(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      graphics.drawGuiTexture(BUTTON_SPRITES.get(this.active, this.isSelected()), this.getX(), this.getY(), this.getWidth(), 20);
      graphics.drawItem(ICON, this.getX() + 5, this.getY() + 2);
      Text var10002 = LABEL;
      int var10003 = this.getX() + 5 + 16 + 3;
      int var10004 = this.getY();
      Objects.requireNonNull(font);
      graphics.drawText(font, var10002, var10003, var10004 + (20 - 9) / 2 + 1, -2570241, true);
   }

   static {
      ICON = new ItemStack(Items.ARMOR_STAND);
      LABEL = Text.translatable("cobbleclub.wardrobe.button");
      BUTTON_SPRITES = new ButtonTextures(Identifier.ofVanilla("widget/button"), Identifier.ofVanilla("widget/button_disabled"), Identifier.ofVanilla("widget/button_highlighted"));
   }
}
