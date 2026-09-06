package com.cobbleclub.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class Tooltips {
   private static final int MAX_WIDTH = 200;
   private static final int MIN_WIDTH = 120;

   private Tooltips() {
   }

   public static void render(DrawContext g, TextRenderer font, List<Text> lines, int mouseX, int mouseY) {
      g.drawOrderedTooltip(font, wrap(font, lines, g.getScaledWindowWidth()), mouseX, mouseY);
   }

   public static void render(DrawContext g, TextRenderer font, Text line, int mouseX, int mouseY) {
      render(g, font, List.of(line), mouseX, mouseY);
   }

   public static void render(DrawContext g, TextRenderer font, ItemStack stack, int mouseX, int mouseY) {
      render(g, font, Screen.getTooltipFromItem(MinecraftClient.getInstance(), stack), mouseX, mouseY);
   }

   private static List<OrderedText> wrap(TextRenderer font, List<Text> lines, int guiWidth) {
      int width = Math.max(120, Math.min(200, guiWidth / 3));
      List<OrderedText> wrapped = new ArrayList(lines.size());

      for(Text line : lines) {
         List<OrderedText> split = font.wrapLines(line, width);
         if (split.isEmpty()) {
            wrapped.add(OrderedText.EMPTY);
         } else {
            wrapped.addAll(split);
         }
      }

      return wrapped;
   }
}
