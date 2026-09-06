package com.cobbleclub.client.kits;

import com.cobbleclub.client.ui.Starfield;
import com.cobbleclub.client.ui.ThemedButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;

import java.util.Map;

@Environment(EnvType.CLIENT)
public final class KitsScreen extends class_437 {
   private static final int PANEL_W = 416;
   private static final int PANEL_H = 224;
   private static final String[] IDS = {"newb", "ace", "champion", "master", "legend"};
   private static final Map<String, String[]> CONTENTS = Map.of(
           "newb", new String[]{"1× CobbleClub Claiming Tool", "1× Diamond Pickaxe", "1× Red Pokédex", "20× Poké Balls", "10× Great Balls", "6× Ultra Balls", "32× Steak", "1× PC", "1× Healing Machine"},
           "ace", new String[]{"2× Shiny Crate Keys", "6× Rare Candy", "6× Ultra Balls", "12× Quick Balls", "50× XP Bottles"},
           "champion", new String[]{"1× Legendary Crate Key", "1× Shiny Crate Key", "8× Rare Candy", "8× Ultra Balls", "16× Quick Balls", "64× XP Bottles"},
           "master", new String[]{"2× Beast Balls", "1× Legendary Crate Key", "1× Shiny Crate Key", "1× Vote Crate Key", "16× Rare Candy", "32× Quick Balls", "96× XP Bottles"},
           "legend", new String[]{"1× Master Ball", "1× Shiny Crate Key", "3× Vote Crate Keys", "46× Rare Candy", "46× Quick Balls", "178× XP Bottles"}
   );
   private KitsState state;
   private String selected = "newb";
   private ThemedButton claimButton;

   public KitsScreen(KitsState state) {
      super(class_2561.method_43470("CobbleClub Kits"));
      this.state = state;
   }

   public void applyState(KitsState state) { this.state = state; }
   private int left() { return (this.field_22789 - PANEL_W) / 2; }
   private int top() { return (this.field_22790 - PANEL_H) / 2; }

   protected void method_25426() {
      int x = this.left() + 8;
      int tabY = this.top() + 45;
      int gap = 4;
      int tabW = (PANEL_W - 16 - gap * 4) / 5;
      for (int i = 0; i < IDS.length; i++) {
         String id = IDS[i];
         String label = Character.toUpperCase(id.charAt(0)) + id.substring(1);
         this.method_37063(new ThemedButton(x + i * (tabW + gap), tabY, tabW, 20,
                 class_2561.method_43470(label), b -> this.selected = id));
      }
      int y = this.top() + 178;
      int buttonGap = 4;
      int width = (PANEL_W - 16 - buttonGap * 3) / 4;
      this.claimButton = new ThemedButton(x, y, width, 22, class_2561.method_43470("Claim Kit"), ThemedButton.Variant.GREEN, b -> KitsNetworking.send("claim:" + this.selected));
      this.method_37063(this.claimButton);
      this.method_37063(new ThemedButton(x + width + buttonGap, y, width, 22, class_2561.method_43470("−1h Cooldown"), ThemedButton.Variant.BLUE, b -> KitsNetworking.send("reduce:" + this.selected)));
      this.method_37063(new ThemedButton(x + (width + buttonGap) * 2, y, width, 22, class_2561.method_43470("Refresh Ranks"), b -> KitsNetworking.send("refresh")));
      this.method_37063(new ThemedButton(x + (width + buttonGap) * 3, y, width, 22, class_2561.method_43470("Close"), b -> this.method_25419()));
   }

   public void method_25394(class_332 g, int mouseX, int mouseY, float partialTick) {
      this.method_25420(g, mouseX, mouseY, partialTick);
      Starfield.draw(g, 0, 0, this.field_22789, this.field_22790, class_156.method_658(), 44, 8787L, 0.55F);
      int x = this.left();
      int y = this.top();
      g.method_25294(x - 3, y - 3, x + PANEL_W + 3, y + PANEL_H + 3, 1426063360);
      g.method_25296(x, y, x + PANEL_W, y + PANEL_H, -199614136, -200601562);
      g.method_49601(x - 1, y - 1, PANEL_W + 2, PANEL_H + 2, -16447985);
      g.method_49601(x, y, PANEL_W, PANEL_H, -13747610);
      g.method_25296(x, y, x + PANEL_W, y + 22, -14405538, -15459782);
      g.method_25294(x, y + 21, x + PANEL_W, y + 22, -6467875);
      g.method_27534(this.field_22793, class_2561.method_43470("CobbleClub · Kits"), x + PANEL_W / 2, y + 7, -2053377);

      KitsState.KitEntry kit = this.state.kit(this.selected);
      long remaining = kit.secondsRemaining();
      String status = !kit.unlocked ? "LOCKED" : remaining <= 0L ? "READY — FREE TO CLAIM" : "COOLDOWN — " + time(remaining);
      int statusColor = !kit.unlocked ? -2734768 : remaining <= 0L ? -12474273 : -2053377;
      g.method_25300(this.field_22793, kit.displayName + " Kit · " + status, x + PANEL_W / 2, y + 29, statusColor);
      String access = "newb".equals(this.selected)
              ? "Public · first join auto-delivery · 12h cooldown"
              : "Premium rank kit · 18h cooldown";
      float accessScale = 0.78F;
      g.method_51448().method_22903();
      g.method_51448().method_22905(accessScale, accessScale, 1.0F);
      g.method_25300(this.field_22793, access, (int)((x + PANEL_W / 2) / accessScale), (int)((y + 40) / accessScale), -7035976);
      g.method_51448().method_22909();
      if (this.claimButton != null) this.claimButton.field_22763 = kit.unlocked && remaining <= 0L;

      int boxY = y + 70;
      g.method_25296(x + 8, boxY, x + PANEL_W - 8, y + 173, -14998448, -16315880);
      g.method_49601(x + 8, boxY, PANEL_W - 16, 103, -13747610);
      g.method_25303(this.field_22793, (kit.displayName + " KIT CONTENTS").toUpperCase(), x + 19, boxY + 9, -6467875);
      g.method_25294(x + PANEL_W / 2, boxY + 24, x + PANEL_W / 2 + 1, y + 166, -13747610);
      String[] items = CONTENTS.getOrDefault(this.selected, new String[0]);
      int split = (items.length + 1) / 2;
      for (int i = 0; i < items.length; i++) {
         int column = i < split ? 0 : 1;
         int row = column == 0 ? i : i - split;
         g.method_25303(this.field_22793, "• " + items[i], x + 16 + column * 200, boxY + 28 + row * 15, -2962968);
      }

      for (class_364 child : this.method_25396()) {
         if (child instanceof class_4068 renderable) renderable.method_25394(g, mouseX, mouseY, partialTick);
      }
      if (!this.state.notice.isEmpty()) {
         g.method_25300(this.field_22793, this.state.notice, x + PANEL_W / 2, y + 211, this.state.error ? -2734768 : -12474273);
      } else {
         String paid = shortTime(kit.reductionPurchasedSeconds) + "/" + shortTime(kit.reductionMaxSeconds);
         float footerScale = 0.84F;
         String footer = "Cooldown buy: " + this.state.reductionPriceText + " per hour · used " + paid + " · balance " + this.state.balanceText;
         g.method_51448().method_22903();
         g.method_51448().method_22905(footerScale, footerScale, 1.0F);
         g.method_25300(this.field_22793, footer, (int)((x + PANEL_W / 2) / footerScale), (int)((y + 211) / footerScale), -7035976);
         g.method_51448().method_22909();
      }
   }

   private static String time(long seconds) {
      long hours = seconds / 3600L;
      long minutes = seconds % 3600L / 60L;
      long secs = seconds % 60L;
      return hours > 0L ? hours + "h " + minutes + "m" : minutes > 0L ? minutes + "m " + secs + "s" : secs + "s";
   }

   private static String shortTime(long seconds) {
      if (seconds % 3600L == 0L) return seconds / 3600L + "h";
      return time(seconds);
   }
}
