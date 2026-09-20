/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.client.CobblemonClient
 *  com.cobblemon.mod.common.client.battle.ClientBattle
 *  com.cobblemon.mod.common.client.gui.battle.BattleGUI
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
 *  net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
 *  net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
 *  net.minecraft.class_437
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.BattleLogStore;
import com.cobbleclub.client.battle.BattleState;
import com.cobbleclub.client.battle.EnhancedBattleLogWidget;
import com.cobbleclub.client.battle.TurnIndicatorRenderer;
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
import net.minecraft.client.gui.screen.Screen;

@Environment(value=EnvType.CLIENT)
public final class BattleClientInit {
    private static UUID lastBattleId = null;

    private BattleClientInit() {
    }

    public static void init() {
        BattleConfig.load();
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            try {
                TurnIndicatorRenderer.render(graphics);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        });
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof BattleGUI) {
                ScreenMouseEvents.allowMouseScroll((Screen)screen).register((s, mouseX, mouseY, horizontal, vertical) -> !EnhancedBattleLogWidget.mouseScrolled(mouseX, mouseY, vertical));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            UUID currentId;
            ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
            UUID uUID = currentId = battle == null ? null : battle.getBattleId();
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

