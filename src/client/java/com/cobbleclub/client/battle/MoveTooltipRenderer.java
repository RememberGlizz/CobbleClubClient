package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.gui.MoveCategoryIcon;
import com.cobblemon.mod.common.pokemon.FormData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class MoveTooltipRenderer {
   private static final int PADDING = 4;
   private static final int ICON_SIZE = 9;
   private static final int MAX_DESC_WIDTH = 150;
   private static final int SECTION_GAP = 3;
   private static final int CATEGORY_ICON_W = 12;
   private static final int LABEL_COLOR = -4671288;
   private static final int NAME_COLOR = -1;
   private static final int DESC_COLOR = -2238008;
   private static final int SUPER_COLOR = -9776022;
   private static final int WEAK_COLOR = -30116;
   private static final int IMMUNE_COLOR = -7303008;
   private static final int PHYSICAL_COLOR = -34228;
   private static final int SPECIAL_COLOR = -9787649;
   private static final int STATUS_COLOR = -4670264;

   private MoveTooltipRenderer() {
   }

   public static void render(DrawContext graphics, MoveTemplate template, ElementalType moveType, int curPp, int maxPp, ClientBattlePokemon target, int mouseX, int mouseY) {
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      List<Line> body = new ArrayList();
      DamageCategory category = template.getDamageCategory() != null ? template.getDamageCategory() : DamageCategories.INSTANCE.getSTATUS();
      boolean isStatus = "status".equalsIgnoreCase(category.getName());
      Text attackType = Text.literal("Attack Type: ").withColor(-4671288).append(Text.literal(titleCase(category.getName())).withColor(categoryColor(category)));
      body.add(new Line(attackType.asOrderedText(), -1, 0, category));
      if (!isStatus) {
         String power = template.getPower() > (double)0.0F ? String.valueOf((int)template.getPower()) : "—";
         body.add(labeled(font, "Power", power, 3));
      }

      String accuracy = template.getAccuracy() > (double)0.0F ? (int)template.getAccuracy() + "%" : "—";
      body.add(labeled(font, "Accuracy", accuracy, isStatus ? 3 : 0));
      body.add(labeled(font, "PP", curPp + "/" + maxPp, 0));
      if (template.getPriority() != 0) {
         String var10000 = template.getPriority() > 0 ? "+" : "";
         String priority = var10000 + template.getPriority();
         body.add(labeled(font, "Priority", priority, 0));
      }

      if (!isStatus && target != null) {
         FormData targetForm = BattleUtil.form(target);
         float mult = TypeChart.multiplier(moveType, targetForm.getPrimaryType(), targetForm.getSecondaryType());
         Text eff = effectivenessLine(mult, target);
         if (eff != null) {
            body.add(new Line(eff.asOrderedText(), effectivenessColor(mult), 3, (DamageCategory)null));
         }
      }

      Text desc = template.getDescription();
      if (desc != null && !desc.getString().isBlank()) {
         boolean firstDescLine = true;

         for(OrderedText seq : font.wrapLines(desc, 150)) {
            body.add(new Line(seq, -2238008, firstDescLine ? 3 : 0, (DamageCategory)null));
            firstDescLine = false;
         }
      }

      OrderedText nameSeq = template.getDisplayName().asOrderedText();
      int headerWidth = 12 + font.getWidth(nameSeq);
      int contentWidth = headerWidth;

      for(Line l : body) {
         int lineWidth = font.getWidth(l.seq());
         if (l.category() != null) {
            lineWidth += 15;
         }

         contentWidth = Math.max(contentWidth, lineWidth);
      }

      Objects.requireNonNull(font);
      int lineHeight = 9 + 2;
      int boxWidth = contentWidth + 8;
      Objects.requireNonNull(font);
      int headerHeight = Math.max(9, 9);
      int boxHeight = 8 + headerHeight + 3;

      for(Line l : body) {
         boxHeight += lineHeight + l.gapAbove();
      }

      int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
      int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
      int x = mouseX + 12;
      int y = mouseY - 12;
      if (x + boxWidth > screenW) {
         x = Math.max(2, mouseX - boxWidth - 12);
      }

      if (y + boxHeight > screenH) {
         y = Math.max(2, screenH - boxHeight - 2);
      }

      graphics.getMatrices().push();
      graphics.getMatrices().translate(0.0F, 0.0F, 400.0F);
      BattlePanel.draw(graphics, x, y, boxWidth, boxHeight);
      int tx = x + 4;
      int ty = y + 4;
      TypeIconRenderer.drawType(graphics, moveType, tx, ty, 9);
      int var10003 = tx + 9 + 3;
      Objects.requireNonNull(font);
      graphics.drawTextWithShadow(font, nameSeq, var10003, ty + (headerHeight - 9) / 2 + 1, -1);
      int rowY = ty + headerHeight + 3;

      for(Line l : body) {
         rowY += l.gapAbove();
         graphics.drawTextWithShadow(font, l.seq(), tx, rowY, l.color());
         if (l.category() != null) {
            (new MoveCategoryIcon(tx + font.getWidth(l.seq()) + 3, rowY, l.category(), 1.0F)).render(graphics);
         }

         rowY += lineHeight;
      }

      graphics.getMatrices().pop();
   }

   private static Line labeled(TextRenderer font, String label, String value, int gapAbove) {
      Text c = Text.literal(label + ": ").withColor(-4671288).append(Text.literal(value).withColor(-1));
      return new Line(c.asOrderedText(), -1, gapAbove, (DamageCategory)null);
   }

   private static String titleCase(String s) {
      if (s != null && !s.isEmpty()) {
         char var10000 = Character.toUpperCase(s.charAt(0));
         return var10000 + s.substring(1).toLowerCase(Locale.ROOT);
      } else {
         return s;
      }
   }

   private static int categoryColor(DamageCategory category) {
      int var10000;
      switch (category.getName().toLowerCase(Locale.ROOT)) {
         case "physical" -> var10000 = -34228;
         case "special" -> var10000 = -9787649;
         default -> var10000 = -4670264;
      }

      return var10000;
   }

   private static Text effectivenessLine(float mult, ClientBattlePokemon target) {
      if (mult == 0.0F) {
         return Text.literal("No effect on " + target.getSpecies().getName());
      } else if (mult > 1.0F) {
         return Text.literal("Super effective (" + trim(mult) + "x)");
      } else {
         return mult < 1.0F ? Text.literal("Not very effective (" + trim(mult) + "x)") : null;
      }
   }

   private static int effectivenessColor(float mult) {
      if (mult == 0.0F) {
         return -7303008;
      } else {
         return mult > 1.0F ? -9776022 : -30116;
      }
   }

   private static String trim(float value) {
      return value == (float)((int)value) ? String.valueOf((int)value) : String.valueOf(value).replaceAll("0+$", "").replaceAll("\\.$", "");
   }

   @Environment(EnvType.CLIENT)
   private static record Line(OrderedText seq, int color, int gapAbove, DamageCategory category) {
   }
}
