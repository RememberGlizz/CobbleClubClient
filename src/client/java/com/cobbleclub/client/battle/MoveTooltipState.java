/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.moves.MoveTemplate
 *  com.cobblemon.mod.common.api.types.ElementalType
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_332
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.BattleUtil;
import com.cobbleclub.client.battle.MoveTooltipRenderer;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.types.ElementalType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(value=EnvType.CLIENT)
public final class MoveTooltipState {
    private static MoveTemplate template;
    private static ElementalType moveType;
    private static int curPp;
    private static int maxPp;
    private static int mouseX;
    private static int mouseY;
    private static boolean pending;

    private MoveTooltipState() {
    }

    public static void queue(MoveTemplate template, ElementalType moveType, int curPp, int maxPp, int mouseX, int mouseY) {
        MoveTooltipState.template = template;
        MoveTooltipState.moveType = moveType;
        MoveTooltipState.curPp = curPp;
        MoveTooltipState.maxPp = maxPp;
        MoveTooltipState.mouseX = mouseX;
        MoveTooltipState.mouseY = mouseY;
        pending = true;
    }

    public static void flush(DrawContext graphics) {
        if (pending && template != null) {
            pending = false;
            MoveTooltipRenderer.render(graphics, template, moveType, curPp, maxPp, BattleUtil.firstOpponentActive(), mouseX, mouseY);
        } else {
            pending = false;
        }
    }
}

