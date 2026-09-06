package com.cobbleclub.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class GuiDepth {
   private GuiDepth() {
   }

   public static void clearForOverlay(DrawContext guiGraphics) {
      guiGraphics.draw();
      RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
   }
}
