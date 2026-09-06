package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.types.ElementalType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public final class TypeIconRenderer {
   private static final class_2960 TYPES = class_2960.method_60655("cobblemon", "textures/gui/types.png");
   private static final class_2960 TYPES_SMALL = class_2960.method_60655("cobblemon", "textures/gui/types_small.png");
   private static final int TYPE_CELL = 36;
   private static final int SMALL_CELL = 18;

   private TypeIconRenderer() {
   }

   public static void drawType(class_332 graphics, ElementalType type, int x, int y, int size) {
      if (type != null) {
         int physical = (int)Math.round((double)size * class_310.method_1551().method_22683().method_4495());
         int cell = physical <= 18 ? 18 : 36;
         graphics.method_25293(cell == 18 ? TYPES_SMALL : TYPES, x, y, size, size, (float)type.getTextureXMultiplier() * (float)cell + 0.1F, 0.0F, cell, cell, cell * 18, cell);
      }
   }

   public static int drawTypes(class_332 graphics, ElementalType primary, ElementalType secondary, int x, int y, int size, int gap) {
      int drawn = 0;
      if (primary != null) {
         drawType(graphics, primary, x, y, size);
         ++drawn;
      }

      if (secondary != null) {
         drawType(graphics, secondary, x + drawn * (size + gap), y, size);
         ++drawn;
      }

      return drawn == 0 ? 0 : drawn * size + (drawn - 1) * gap;
   }
}
