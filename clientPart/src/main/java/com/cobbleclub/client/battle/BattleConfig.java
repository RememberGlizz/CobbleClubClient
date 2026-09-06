package com.cobbleclub.client.battle;

import com.cobbleclub.client.ui.ClientJsonConfig;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BattleConfig {
   private static final Path FILE = ClientJsonConfig.file("cobbleclub-battle.json");
   private static BattleConfig instance = new BattleConfig();
   public boolean turnIndicator = true;
   public boolean moveTooltips = true;
   public boolean hoverPanel = true;
   public boolean switchTypes = true;
   public boolean enhancedLog = true;
   public boolean hideNativeLog = true;
   public double logAnchorX = (double)-1.0F;
   public double logAnchorY = (double)-1.0F;
   public int logWidth = 160;
   public int logHeight = 90;

   public static BattleConfig get() {
      return instance;
   }

   public static void load() {
      instance = (BattleConfig)ClientJsonConfig.load(FILE, BattleConfig.class, BattleConfig::new);
   }

   public static void save() {
      ClientJsonConfig.save(FILE, instance);
   }
}
