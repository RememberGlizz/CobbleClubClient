package com.cobbleclub.client.mixin;

import com.cobbleclub.client.wardrobe.WardrobeButton;
import com.cobbleclub.client.wardrobe.net.WardrobeActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({InventoryScreen.class})
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> {
   @Unique
   private WardrobeButton cobbleclub$wardrobeButton;

   private InventoryScreenMixin(PlayerScreenHandler menu, PlayerInventory inventory, Text title) {
      super(menu, inventory, title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void cobbleclub$addWardrobeButton(CallbackInfo ci) {
      if (ClientPlayNetworking.canSend(WardrobeActionPayload.TYPE)) {
         this.cobbleclub$wardrobeButton = (WardrobeButton)this.addDrawableChild(new WardrobeButton((button) -> {
            if (this.client != null && this.client.player != null) {
               this.client.player.networkHandler.sendChatCommand("wardrobe");
            }

         }));
         this.cobbleclub$positionWardrobeButton();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void cobbleclub$trackWardrobeButton(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      this.cobbleclub$positionWardrobeButton();
   }

   @Unique
   private void cobbleclub$positionWardrobeButton() {
      if (this.cobbleclub$wardrobeButton != null) {
         this.cobbleclub$wardrobeButton.setPosition(this.x, this.y - 20 - 2);
      }

   }
}
