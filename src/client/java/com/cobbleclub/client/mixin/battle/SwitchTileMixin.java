package com.cobbleclub.client.mixin.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.TypeIconRenderer;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {BattleSwitchPokemonSelection.SwitchTile.class},
   remap = false
)
public class SwitchTileMixin {
   private static final int ICON_SIZE = 9;
   private static final int ICON_GAP = 2;

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void cobbleclub$renderTypeIcons(DrawContext context, double mouseX, double mouseY, float deltaTicks, CallbackInfo ci) {
      if (BattleConfig.get().switchTypes) {
         BattleSwitchPokemonSelection.SwitchTile self = (BattleSwitchPokemonSelection.SwitchTile)(Object)this;
         Pokemon pokemon = self.getPokemon();
         if (pokemon != null) {
            int x = (int)self.getX();
            int y = (int)self.getY();
            int width = pokemon.getSecondaryType() != null ? 20 : 9;
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 200.0F);
            TypeIconRenderer.drawTypes(context, pokemon.getPrimaryType(), pokemon.getSecondaryType(), x + 76 - width, y + 10, 9, 2);
            context.getMatrices().pop();
         }
      }
   }
}
