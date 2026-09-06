package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbility;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class HoverInfoPanel {
   private static final int PAD = 4;
   private static final int TYPE_ICON = 9;
   private static final int MOVE_ICON = 9;
   private static final int GAP = 2;
   private static final int LABEL = -5197632;
   private static final int VALUE = -1;
   private static final int UNKNOWN = -8355696;
   private static final int UP = -9776022;
   private static final int DOWN = -38294;
   private static final int MALE = -11167233;
   private static final int FEMALE = -34902;

   private HoverInfoPanel() {
   }

   public static void renderHovered(DrawContext graphics, int mouseX, int mouseY) {
      if (BattleConfig.get().hoverPanel) {
         Iterator<Map.Entry<UUID, BattleState.TileRect>> it = BattleState.tileRects().entrySet().iterator();

         while(it.hasNext()) {
            Map.Entry<UUID, BattleState.TileRect> entry = (Map.Entry)it.next();
            BattleState.TileRect rect = (BattleState.TileRect)entry.getValue();
            if (!((float)mouseX < rect.x) && !((float)mouseX > rect.x + rect.width) && !((float)mouseY < rect.y) && !((float)mouseY > rect.y + rect.height)) {
               ClientBattlePokemon pokemon = BattleUtil.activeByUuid((UUID)entry.getKey());
               if (pokemon != null) {
                  render(graphics, pokemon, rect);
                  return;
               }

               it.remove();
            }
         }

      }
   }

   private static void render(DrawContext graphics, ClientBattlePokemon pokemon, BattleState.TileRect rect) {
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      Pokemon own = BattleUtil.localPartyPokemon(pokemon.getUuid());
      RevealedBattleInfo revealed = BattleState.revealedOrNull(pokemon.getUuid());
      FormData form = BattleUtil.form(pokemon);
      ElementalType type1 = own != null ? own.getPrimaryType() : form.getPrimaryType();
      ElementalType type2 = own != null ? own.getSecondaryType() : form.getSecondaryType();
      Gender gender = pokemon.getGender();
      Text header = Text.literal(pokemon.getDisplayName().getString()).append(Text.literal("  Lv." + pokemon.getLevel()).withColor(-5197632));
      List<Text> lines = new ArrayList();
      SpeedRangeResolver.SpeedInfo speed = SpeedRangeResolver.resolve(pokemon);
      if (speed != null) {
         lines.add(labeled("Speed", (String)speed.display(), -1));
      }

      addAbilityLines(lines, pokemon, own, revealed);
      Text item = itemLine(own, revealed);
      if (item != null) {
         lines.add(item);
      }

      String formName = pokemon.getProperties() != null ? pokemon.getProperties().getForm() : null;
      if (formName != null && !formName.isBlank() && !formName.equalsIgnoreCase("normal")) {
         lines.add(labeled("Form", (String)formName, -1));
      }

      Text statChanges = statChangeLine(pokemon);
      if (statChanges != null) {
         lines.add(statChanges);
      }

      List<MoveRow> moves = moveRows(own, revealed);
      int contentWidth = font.getWidth(header);
      contentWidth = Math.max(contentWidth, typeRowWidth(font, type1, type2, gender));

      for(Text line : lines) {
         contentWidth = Math.max(contentWidth, font.getWidth(line));
      }

      for(MoveRow move : moves) {
         contentWidth = Math.max(contentWidth, 11 + font.getWidth(move.name()) + 8 + font.getWidth(move.pp()));
      }

      Objects.requireNonNull(font);
      int lineH = 9 + 1;
      int rows = lines.size() + (moves.isEmpty() ? 0 : 1 + moves.size());
      int boxW = contentWidth + 8;
      Objects.requireNonNull(font);
      int boxH = 8 + 9 + 1 + 9 + 2 + rows * lineH;
      int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
      int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
      int x = rect.opponent ? (int)rect.x - boxW - 2 : (int)(rect.x + rect.width) + 2;
      if (x < 2 || x + boxW > screenW) {
         x = rect.opponent ? (int)(rect.x + rect.width) + 2 : (int)rect.x - boxW - 2;
      }

      x = Math.max(2, Math.min(x, screenW - boxW - 2));
      int y = Math.max(2, Math.min((int)rect.y, screenH - boxH - 2));
      TurnIndicatorRenderer.Rect pill = TurnIndicatorRenderer.currentRect();
      if (pill != null && x < pill.x() + pill.width() && x + boxW > pill.x() && y < pill.y() + pill.height() && y + boxH > pill.y()) {
         y = Math.min(pill.y() + pill.height() + 2, screenH - boxH - 2);
      }

      graphics.getMatrices().push();
      graphics.getMatrices().translate(0.0F, 0.0F, 400.0F);
      BattlePanel.draw(graphics, x, y, boxW, boxH);
      int cx = x + 4;
      int cy = y + 4;
      graphics.drawTextWithShadow(font, header, cx, cy, -1);
      Objects.requireNonNull(font);
      cy += 9 + 1;
      int typeWidth = TypeIconRenderer.drawTypes(graphics, type1, type2, cx, cy, 9, 1);
      if (gender == Gender.MALE) {
         graphics.drawTextWithShadow(font, Text.literal("♂"), cx + typeWidth + 3, cy + 1, -11167233);
      } else if (gender == Gender.FEMALE) {
         graphics.drawTextWithShadow(font, Text.literal("♀"), cx + typeWidth + 3, cy + 1, -34902);
      }

      cy += 11;

      for(Text line : lines) {
         graphics.drawTextWithShadow(font, line, cx, cy, -1);
         cy += lineH;
      }

      if (!moves.isEmpty()) {
         graphics.drawTextWithShadow(font, Text.literal("Moves:").withColor(-5197632), cx, cy, -5197632);
         cy += lineH;

         for(MoveRow move : moves) {
            TypeIconRenderer.drawType(graphics, move.type(), cx, cy, 9);
            graphics.drawTextWithShadow(font, move.name(), cx + 9 + 2, cy, -1);
            int ppX = x + boxW - 4 - font.getWidth(move.pp());
            graphics.drawTextWithShadow(font, Text.literal(move.pp()).withColor(-5197632), ppX, cy, -5197632);
            cy += lineH;
         }
      }

      graphics.getMatrices().pop();
   }

   private static void addAbilityLines(List<Text> lines, ClientBattlePokemon pokemon, Pokemon own, RevealedBattleInfo revealed) {
      if (own != null && own.getAbility() != null) {
         lines.add(labeled("Ability", (Text)Text.translatable(own.getAbility().getDisplayName()), -1));
      } else if (revealed != null && revealed.abilityName != null) {
         lines.add(labeled("Ability", (Text)Text.literal(revealed.abilityName), -1));
      } else {
         List<Text> possible = possibleAbilities(pokemon);
         if (possible.isEmpty()) {
            lines.add(labeled("Ability", (Text)Text.literal("???"), -8355696));
         } else if (possible.size() == 1) {
            lines.add(labeled("Ability", (Text)((Text)possible.get(0)), -1));
         } else {
            lines.add(Text.literal("Possible Abilities:").withColor(-5197632));

            for(Text ability : possible) {
               lines.add(Text.literal("  ").append(ability.copy().withColor(-1)));
            }
         }

      }
   }

   private static List<Text> possibleAbilities(ClientBattlePokemon pokemon) {
      boolean wild = BattleUtil.isWildActor(pokemon);
      Set<String> aspects = pokemon.getState().getCurrentAspects();
      Map<String, AbilityTemplate> commons = new LinkedHashMap();
      Map<String, AbilityTemplate> hiddens = new LinkedHashMap();

      for(PotentialAbility potential : BattleUtil.form(pokemon).getAbilities()) {
         if (potential instanceof HiddenAbility) {
            if (!wild) {
               hiddens.putIfAbsent(potential.getTemplate().getName(), potential.getTemplate());
            }
         } else if (potential.isSatisfiedBy(aspects)) {
            commons.putIfAbsent(potential.getTemplate().getName(), potential.getTemplate());
         }
      }

      List<Text> result = new ArrayList();

      for(AbilityTemplate template : commons.values()) {
         result.add(Text.translatable(template.getDisplayName()));
      }

      for(Map.Entry<String, AbilityTemplate> entry : hiddens.entrySet()) {
         if (!commons.containsKey(entry.getKey())) {
            result.add(Text.translatable(((AbilityTemplate)entry.getValue()).getDisplayName()).append(Text.literal(" (HA)").withColor(-5197632)));
         }
      }

      return result;
   }

   private static Text itemLine(Pokemon own, RevealedBattleInfo revealed) {
      if (own != null) {
         ItemStack held = own.getHeldItem$common();
         return held != null && !held.isEmpty() ? labeled("Item", (String)held.getName().getString(), -1) : null;
      } else if (revealed != null && revealed.heldItemName != null) {
         String suffix = revealed.heldItemConsumed ? " (used)" : "";
         return labeled("Item", (String)(revealed.heldItemName + suffix), -1);
      } else {
         return labeled("Item", (Text)Text.literal("???"), -8355696);
      }
   }

   private static Text statChangeLine(ClientBattlePokemon pokemon) {
      Map<Stat, Integer> changes = pokemon.getStatChanges();
      if (changes != null && !changes.isEmpty()) {
         Text result = Text.empty();
         boolean any = false;

         for(Map.Entry<Stat, Integer> entry : changes.entrySet()) {
            int stage = (Integer)entry.getValue();
            if (stage != 0) {
               if (any) {
                  result = result.copy().append(Text.literal("  "));
               }

               String var10000 = shortStat((Stat)entry.getKey());
               String text = var10000 + " " + (stage > 0 ? "+" : "") + stage;
               result = result.copy().append(Text.literal(text).withColor(stage > 0 ? -9776022 : -38294));
               any = true;
            }
         }

         return any ? result : null;
      } else {
         return null;
      }
   }

   private static List<MoveRow> moveRows(Pokemon own, RevealedBattleInfo revealed) {
      List<MoveRow> rows = new ArrayList();
      if (own != null && own.getMoveSet() != null) {
         for(Move move : own.getMoveSet().getMoves()) {
            if (move != null) {
               rows.add(new MoveRow(move.getDisplayName(), move.getCurrentPp() + "/" + move.getMaxPp(), move.getType()));
            }
         }
      } else if (revealed != null) {
         for(Map.Entry<String, RevealedBattleInfo.RevealedMove> entry : revealed.moves.entrySet()) {
            RevealedBattleInfo.RevealedMove move = (RevealedBattleInfo.RevealedMove)entry.getValue();
            MoveTemplate template = Moves.getByName((String)entry.getKey());
            int maxPp = template != null ? template.getMaxPp() : 0;
            ElementalType type = template != null ? template.getElementalType() : null;
            String pp = maxPp > 0 ? "~" + Math.max(0, maxPp - move.uses) + "/" + maxPp : "";
            rows.add(new MoveRow(Text.literal(move.displayName), pp, type));
         }
      }

      return rows;
   }

   private static Text labeled(String label, String value, int valueColor) {
      return labeled(label, (Text)Text.literal(value), valueColor);
   }

   private static Text labeled(String label, Text value, int valueColor) {
      return Text.literal(label + ": ").withColor(-5197632).append(value.copy().withColor(valueColor));
   }

   private static int typeRowWidth(TextRenderer font, ElementalType t1, ElementalType t2, Gender gender) {
      int types = (t1 != null ? 9 : 0) + (t2 != null ? 10 : 0);
      int genderWidth = gender != Gender.MALE && gender != Gender.FEMALE ? 0 : 3 + font.getWidth("♂");
      return types + genderWidth;
   }

   private static String shortStat(Stat stat) {
      String var10000;
      switch (stat.getShowdownId()) {
         case "atk" -> var10000 = "Atk";
         case "def" -> var10000 = "Def";
         case "spa" -> var10000 = "SpA";
         case "spd" -> var10000 = "SpD";
         case "spe" -> var10000 = "Spe";
         case "accuracy" -> var10000 = "Acc";
         case "evasion" -> var10000 = "Eva";
         default -> var10000 = stat.getShowdownId();
      }

      return var10000;
   }

   @Environment(EnvType.CLIENT)
   private static record MoveRow(Text name, String pp, ElementalType type) {
   }
}
