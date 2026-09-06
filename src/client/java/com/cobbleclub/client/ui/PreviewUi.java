package com.cobbleclub.client.ui;

import com.cobblemon.mod.common.CobblemonSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.Text.Serialization;

@Environment(EnvType.CLIENT)
public final class PreviewUi {
   private PreviewUi() {
   }

   public static RegistryWrapper.WrapperLookup registry() {
      return MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getRegistryManager() : null;
   }

   public static Text deserialize(String json, String fallback) {
      RegistryWrapper.WrapperLookup reg = registry();
      if (json != null && reg != null) {
         try {
            Text parsed = Serialization.fromJson(json, reg);
            if (parsed != null) {
               return parsed;
            }
         } catch (Exception var4) {
         }
      }

      return Text.literal(fallback != null ? fallback : "");
   }

   public static void playClick() {
      MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   public static void playOpen() {
      MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(CobblemonSounds.PC_ON, 0.8F, 0.3F));
   }

   public static void playClose() {
      MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(CobblemonSounds.PC_OFF, 1.0F, 0.3F));
   }

   public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
      return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
   }

   public static void renderScaledItem(DrawContext g, ItemStack stack, int itemX, int itemY, float scale) {
      float cx = (float)(itemX + 8);
      float cy = (float)(itemY + 8);
      g.getMatrices().push();
      g.getMatrices().translate((double)cx, (double)cy, (double)0.0F);
      g.getMatrices().scale(scale, scale, 1.0F);
      g.getMatrices().translate((double)(-cx), (double)(-cy), (double)0.0F);
      g.drawItem(stack, itemX, itemY);
      g.getMatrices().pop();
   }
}
