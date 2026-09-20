/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.InfoBadge;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class MoveBadgeRects {
    private static final int MAX = 8;
    private static final int[] XS = new int[8];
    private static final int[] YS = new int[8];
    private static int count;

    private MoveBadgeRects() {
    }

    public static void beginFrame() {
        count = 0;
    }

    public static void record(int x, int y) {
        if (count < 8) {
            MoveBadgeRects.XS[MoveBadgeRects.count] = x;
            MoveBadgeRects.YS[MoveBadgeRects.count] = y;
            ++count;
        }
    }

    public static boolean contains(double mouseX, double mouseY) {
        for (int i = 0; i < count; ++i) {
            if (!InfoBadge.isOver(mouseX, mouseY, XS[i], YS[i])) continue;
            return true;
        }
        return false;
    }
}

