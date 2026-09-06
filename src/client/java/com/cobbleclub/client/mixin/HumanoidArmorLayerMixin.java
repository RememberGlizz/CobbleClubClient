package com.cobbleclub.client.mixin;

import com.cobbleclub.client.render.ArmorEquipmentLoader;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(ArmorFeatureRenderer.class)
public abstract class HumanoidArmorLayerMixin {
    @ModifyExpressionValue(
        method = "method_4169",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1741$class_9196;method_56693(Z)Lnet/minecraft/class_2960;", remap = false),
        remap = false
    )
    private Identifier cobbleclub$swapCustomArmorTexture(Identifier original, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) EquipmentSlot slot) {
        try {
            ItemStack stack = entity.getEquippedStack(slot);
            CustomModelDataComponent cmd = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (cmd == null) return original;
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            ArmorEquipmentLoader.Entry entry = ArmorEquipmentLoader.lookup(itemId, cmd.value());
            if (entry == null) return original;
            Identifier custom = slot == EquipmentSlot.LEGS ? entry.leggings() : entry.body();
            return custom != null ? custom : original;
        } catch (Throwable ignored) {
            return original;
        }
    }
}
