/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.ui.ClientJsonConfig;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class BattleConfig {
    private static final Path FILE = ClientJsonConfig.file("cobbleclub-battle.json");
    private static BattleConfig instance = new BattleConfig();
    public boolean turnIndicator = true;
    public boolean moveTooltips = true;
    public boolean hoverPanel = true;
    public boolean switchTypes = true;
    public boolean enhancedLog = true;
    public boolean hideNativeLog = true;
    public double logAnchorX = -1.0;
    public double logAnchorY = -1.0;
    public int logWidth = 160;
    public int logHeight = 90;

    public static BattleConfig get() {
        return instance;
    }

    public static void load() {
        instance = ClientJsonConfig.load(FILE, BattleConfig.class, BattleConfig::new);
    }

    public static void save() {
        ClientJsonConfig.save(FILE, instance);
    }
}

