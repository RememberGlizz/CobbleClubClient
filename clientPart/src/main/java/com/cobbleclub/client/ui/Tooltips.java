package com.cobbleclub.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5481;

@Environment(EnvType.CLIENT)
public final class Tooltips {
   private static final int MAX_WIDTH = 200;
   private static final int MIN_WIDTH = 120;

   private Tooltips() {
   }

   public static void render(class_332 g, class_327 font, List<class_2561> lines, int mouseX, int mouseY) {
      g.method_51447(font, wrap(font, lines, g.method_51421()), mouseX, mouseY);
   }

   public static void render(class_332 g, class_327 font, class_2561 line, int mouseX, int mouseY) {
      render(g, font, List.of(line), mouseX, mouseY);
   }

   public static void render(class_332 g, class_327 font, class_1799 stack, int mouseX, int mouseY) {
      render(g, font, class_437.method_25408(class_310.method_1551(), stack), mouseX, mouseY);
   }

   private static List<class_5481> wrap(class_327 font, List<class_2561> lines, int guiWidth) {
      int width = Math.max(120, Math.min(200, guiWidth / 3));
      List<class_5481> wrapped = new ArrayList(lines.size());

      for(class_2561 line : lines) {
         List<class_5481> split = font.method_1728(line, width);
         if (split.isEmpty()) {
            wrapped.add(class_5481.field_26385);
         } else {
            wrapped.addAll(split);
         }
      }

      return wrapped;
   }
}
