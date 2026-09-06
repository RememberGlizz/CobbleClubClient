package com.cobbleclub.client.mixin;

import com.cobbleclub.client.render.ArmorEquipmentLoader;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;
import net.minecraft.class_9280;
import net.minecraft.class_9334;
import net.minecraft.class_970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({class_970.class})
public class HumanoidArmorLayerMixin {
   @ModifyExpressionValue(
      method = {"renderArmorPiece"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/item/ArmorMaterial$Layer;texture(Z)Lnet/minecraft/resources/ResourceLocation;"
)}
   )
   private class_2960 cobbleclub$swapCustomArmorTexture(class_2960 original, @Local(argsOnly = true) class_1309 entity, @Local(argsOnly = true) class_1304 slot) {
      try {
         class_1799 stack = entity.method_6118(slot);
         class_9280 cmd = (class_9280)stack.method_57824(class_9334.field_49637);
         if (cmd == null) {
            return original;
         } else {
            class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
            ArmorEquipmentLoader.Entry entry = ArmorEquipmentLoader.lookup(itemId, cmd.comp_2382());
            if (entry == null) {
               return original;
            } else {
               class_2960 custom = slot == class_1304.field_6172 ? entry.leggings() : entry.body();
               return custom != null ? custom : original;
            }
         }
      } catch (Throwable var9) {
         return original;
      }
   }
}
