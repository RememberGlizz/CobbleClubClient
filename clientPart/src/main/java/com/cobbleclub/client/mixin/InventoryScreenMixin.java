package com.cobbleclub.client.mixin;

import com.cobbleclub.client.wardrobe.WardrobeButton;
import com.cobbleclub.client.wardrobe.net.WardrobeActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.class_1661;
import net.minecraft.class_1723;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_485;
import net.minecraft.class_490;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_490.class})
public abstract class InventoryScreenMixin extends class_485<class_1723> {
   @Unique
   private WardrobeButton cobbleclub$wardrobeButton;

   private InventoryScreenMixin(class_1723 menu, class_1661 inventory, class_2561 title) {
      super(menu, inventory, title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void cobbleclub$addWardrobeButton(CallbackInfo ci) {
      if (ClientPlayNetworking.canSend(WardrobeActionPayload.TYPE)) {
         this.cobbleclub$wardrobeButton = (WardrobeButton)this.method_37063(new WardrobeButton((button) -> {
            if (this.field_22787 != null && this.field_22787.field_1724 != null) {
               this.field_22787.field_1724.field_3944.method_45730("wardrobe");
            }

         }));
         this.cobbleclub$positionWardrobeButton();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void cobbleclub$trackWardrobeButton(class_332 graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      this.cobbleclub$positionWardrobeButton();
   }

   @Unique
   private void cobbleclub$positionWardrobeButton() {
      if (this.cobbleclub$wardrobeButton != null) {
         this.cobbleclub$wardrobeButton.method_48229(this.field_2776, this.field_2800 - 20 - 2);
      }

   }
}
