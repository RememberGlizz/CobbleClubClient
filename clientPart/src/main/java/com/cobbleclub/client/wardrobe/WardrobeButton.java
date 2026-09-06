package com.cobbleclub.client.wardrobe;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_7919;
import net.minecraft.class_8666;

@Environment(EnvType.CLIENT)
public class WardrobeButton extends class_4185 {
   public static final int HEIGHT = 20;
   private static final int PAD_X = 5;
   private static final int ICON_SIZE = 16;
   private static final int ICON_TEXT_GAP = 3;
   private static final int LABEL_COLOR = -2570241;
   private static final int TOOLTIP_TITLE_COLOR = 13215999;
   private static final int TOOLTIP_DESC_COLOR = 11053240;
   private static final class_1799 ICON;
   private static final class_2561 LABEL;
   private static final class_8666 BUTTON_SPRITES;

   public WardrobeButton(class_4185.class_4241 onPress) {
      super(0, 0, width(class_310.method_1551().field_1772), 20, LABEL, onPress, field_40754);
      this.method_47400(class_7919.method_47407(class_2561.method_43471("cobbleclub.wardrobe.button").method_54663(13215999).method_10852(class_2561.method_43470("\n")).method_10852(class_2561.method_43471("cobbleclub.wardrobe.tooltip").method_54663(11053240))));
   }

   private static int width(class_327 font) {
      return 24 + font.method_27525(LABEL) + 5;
   }

   protected void method_48579(class_332 graphics, int mouseX, int mouseY, float partialTick) {
      class_327 font = class_310.method_1551().field_1772;
      graphics.method_52706(BUTTON_SPRITES.method_52729(this.field_22763, this.method_25367()), this.method_46426(), this.method_46427(), this.method_25368(), 20);
      graphics.method_51427(ICON, this.method_46426() + 5, this.method_46427() + 2);
      class_2561 var10002 = LABEL;
      int var10003 = this.method_46426() + 5 + 16 + 3;
      int var10004 = this.method_46427();
      Objects.requireNonNull(font);
      graphics.method_51439(font, var10002, var10003, var10004 + (20 - 9) / 2 + 1, -2570241, true);
   }

   static {
      ICON = new class_1799(class_1802.field_8694);
      LABEL = class_2561.method_43471("cobbleclub.wardrobe.button");
      BUTTON_SPRITES = new class_8666(class_2960.method_60656("widget/button"), class_2960.method_60656("widget/button_disabled"), class_2960.method_60656("widget/button_highlighted"));
   }
}
