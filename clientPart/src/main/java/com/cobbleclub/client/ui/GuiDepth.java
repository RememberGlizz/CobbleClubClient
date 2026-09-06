package com.cobbleclub.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public final class GuiDepth {
   private GuiDepth() {
   }

   public static void clearForOverlay(class_332 guiGraphics) {
      guiGraphics.method_51452();
      RenderSystem.clear(256, class_310.field_1703);
   }
}
