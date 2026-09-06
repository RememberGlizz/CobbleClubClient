package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

@Environment(EnvType.CLIENT)
public final class BattleClientInit {
   private static UUID lastBattleId = null;

   private BattleClientInit() {
   }

   public static void init() {
      BattleConfig.load();
      HudRenderCallback.EVENT.register((HudRenderCallback)(graphics, tickDelta) -> {
         try {
            TurnIndicatorRenderer.render(graphics);
         } catch (Throwable var3) {
         }

      });
      ScreenEvents.BEFORE_INIT.register((ScreenEvents.BeforeInit)(client, screen, scaledWidth, scaledHeight) -> {
         if (screen instanceof BattleGUI) {
            ScreenMouseEvents.allowMouseScroll(screen).register((ScreenMouseEvents.AllowMouseScroll)(s, mouseX, mouseY, horizontal, vertical) -> !EnhancedBattleLogWidget.mouseScrolled(mouseX, mouseY, vertical));
         }

      });
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> {
         ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
         UUID currentId = battle == null ? null : battle.getBattleId();
         if (!Objects.equals(currentId, lastBattleId)) {
            BattleState.reset();
            EnhancedBattleLogWidget.resetScroll();
            if (currentId != null) {
               BattleLogStore.clear();
            }

            lastBattleId = currentId;
         }

         EnhancedBattleLogWidget.tick();
      });
   }
}
