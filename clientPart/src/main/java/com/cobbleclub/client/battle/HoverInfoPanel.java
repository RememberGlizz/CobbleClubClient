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
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

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

   public static void renderHovered(class_332 graphics, int mouseX, int mouseY) {
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

   private static void render(class_332 graphics, ClientBattlePokemon pokemon, BattleState.TileRect rect) {
      class_327 font = class_310.method_1551().field_1772;
      Pokemon own = BattleUtil.localPartyPokemon(pokemon.getUuid());
      RevealedBattleInfo revealed = BattleState.revealedOrNull(pokemon.getUuid());
      FormData form = BattleUtil.form(pokemon);
      ElementalType type1 = own != null ? own.getPrimaryType() : form.getPrimaryType();
      ElementalType type2 = own != null ? own.getSecondaryType() : form.getSecondaryType();
      Gender gender = pokemon.getGender();
      class_2561 header = class_2561.method_43470(pokemon.getDisplayName().getString()).method_10852(class_2561.method_43470("  Lv." + pokemon.getLevel()).method_54663(-5197632));
      List<class_2561> lines = new ArrayList();
      SpeedRangeResolver.SpeedInfo speed = SpeedRangeResolver.resolve(pokemon);
      if (speed != null) {
         lines.add(labeled("Speed", (String)speed.display(), -1));
      }

      addAbilityLines(lines, pokemon, own, revealed);
      class_2561 item = itemLine(own, revealed);
      if (item != null) {
         lines.add(item);
      }

      String formName = pokemon.getProperties() != null ? pokemon.getProperties().getForm() : null;
      if (formName != null && !formName.isBlank() && !formName.equalsIgnoreCase("normal")) {
         lines.add(labeled("Form", (String)formName, -1));
      }

      class_2561 statChanges = statChangeLine(pokemon);
      if (statChanges != null) {
         lines.add(statChanges);
      }

      List<MoveRow> moves = moveRows(own, revealed);
      int contentWidth = font.method_27525(header);
      contentWidth = Math.max(contentWidth, typeRowWidth(font, type1, type2, gender));

      for(class_2561 line : lines) {
         contentWidth = Math.max(contentWidth, font.method_27525(line));
      }

      for(MoveRow move : moves) {
         contentWidth = Math.max(contentWidth, 11 + font.method_27525(move.name()) + 8 + font.method_1727(move.pp()));
      }

      Objects.requireNonNull(font);
      int lineH = 9 + 1;
      int rows = lines.size() + (moves.isEmpty() ? 0 : 1 + moves.size());
      int boxW = contentWidth + 8;
      Objects.requireNonNull(font);
      int boxH = 8 + 9 + 1 + 9 + 2 + rows * lineH;
      int screenW = class_310.method_1551().method_22683().method_4486();
      int screenH = class_310.method_1551().method_22683().method_4502();
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

      graphics.method_51448().method_22903();
      graphics.method_51448().method_46416(0.0F, 0.0F, 400.0F);
      BattlePanel.draw(graphics, x, y, boxW, boxH);
      int cx = x + 4;
      int cy = y + 4;
      graphics.method_27535(font, header, cx, cy, -1);
      Objects.requireNonNull(font);
      cy += 9 + 1;
      int typeWidth = TypeIconRenderer.drawTypes(graphics, type1, type2, cx, cy, 9, 1);
      if (gender == Gender.MALE) {
         graphics.method_27535(font, class_2561.method_43470("♂"), cx + typeWidth + 3, cy + 1, -11167233);
      } else if (gender == Gender.FEMALE) {
         graphics.method_27535(font, class_2561.method_43470("♀"), cx + typeWidth + 3, cy + 1, -34902);
      }

      cy += 11;

      for(class_2561 line : lines) {
         graphics.method_27535(font, line, cx, cy, -1);
         cy += lineH;
      }

      if (!moves.isEmpty()) {
         graphics.method_27535(font, class_2561.method_43470("Moves:").method_54663(-5197632), cx, cy, -5197632);
         cy += lineH;

         for(MoveRow move : moves) {
            TypeIconRenderer.drawType(graphics, move.type(), cx, cy, 9);
            graphics.method_27535(font, move.name(), cx + 9 + 2, cy, -1);
            int ppX = x + boxW - 4 - font.method_1727(move.pp());
            graphics.method_27535(font, class_2561.method_43470(move.pp()).method_54663(-5197632), ppX, cy, -5197632);
            cy += lineH;
         }
      }

      graphics.method_51448().method_22909();
   }

   private static void addAbilityLines(List<class_2561> lines, ClientBattlePokemon pokemon, Pokemon own, RevealedBattleInfo revealed) {
      if (own != null && own.getAbility() != null) {
         lines.add(labeled("Ability", (class_2561)class_2561.method_43471(own.getAbility().getDisplayName()), -1));
      } else if (revealed != null && revealed.abilityName != null) {
         lines.add(labeled("Ability", (class_2561)class_2561.method_43470(revealed.abilityName), -1));
      } else {
         List<class_2561> possible = possibleAbilities(pokemon);
         if (possible.isEmpty()) {
            lines.add(labeled("Ability", (class_2561)class_2561.method_43470("???"), -8355696));
         } else if (possible.size() == 1) {
            lines.add(labeled("Ability", (class_2561)((class_2561)possible.get(0)), -1));
         } else {
            lines.add(class_2561.method_43470("Possible Abilities:").method_54663(-5197632));

            for(class_2561 ability : possible) {
               lines.add(class_2561.method_43470("  ").method_10852(ability.method_27661().method_54663(-1)));
            }
         }

      }
   }

   private static List<class_2561> possibleAbilities(ClientBattlePokemon pokemon) {
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

      List<class_2561> result = new ArrayList();

      for(AbilityTemplate template : commons.values()) {
         result.add(class_2561.method_43471(template.getDisplayName()));
      }

      for(Map.Entry<String, AbilityTemplate> entry : hiddens.entrySet()) {
         if (!commons.containsKey(entry.getKey())) {
            result.add(class_2561.method_43471(((AbilityTemplate)entry.getValue()).getDisplayName()).method_10852(class_2561.method_43470(" (HA)").method_54663(-5197632)));
         }
      }

      return result;
   }

   private static class_2561 itemLine(Pokemon own, RevealedBattleInfo revealed) {
      if (own != null) {
         class_1799 held = own.getHeldItem$common();
         return held != null && !held.method_7960() ? labeled("Item", (String)held.method_7964().getString(), -1) : null;
      } else if (revealed != null && revealed.heldItemName != null) {
         String suffix = revealed.heldItemConsumed ? " (used)" : "";
         return labeled("Item", (String)(revealed.heldItemName + suffix), -1);
      } else {
         return labeled("Item", (class_2561)class_2561.method_43470("???"), -8355696);
      }
   }

   private static class_2561 statChangeLine(ClientBattlePokemon pokemon) {
      Map<Stat, Integer> changes = pokemon.getStatChanges();
      if (changes != null && !changes.isEmpty()) {
         class_2561 result = class_2561.method_43473();
         boolean any = false;

         for(Map.Entry<Stat, Integer> entry : changes.entrySet()) {
            int stage = (Integer)entry.getValue();
            if (stage != 0) {
               if (any) {
                  result = result.method_27661().method_10852(class_2561.method_43470("  "));
               }

               String var10000 = shortStat((Stat)entry.getKey());
               String text = var10000 + " " + (stage > 0 ? "+" : "") + stage;
               result = result.method_27661().method_10852(class_2561.method_43470(text).method_54663(stage > 0 ? -9776022 : -38294));
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
            rows.add(new MoveRow(class_2561.method_43470(move.displayName), pp, type));
         }
      }

      return rows;
   }

   private static class_2561 labeled(String label, String value, int valueColor) {
      return labeled(label, (class_2561)class_2561.method_43470(value), valueColor);
   }

   private static class_2561 labeled(String label, class_2561 value, int valueColor) {
      return class_2561.method_43470(label + ": ").method_54663(-5197632).method_10852(value.method_27661().method_54663(valueColor));
   }

   private static int typeRowWidth(class_327 font, ElementalType t1, ElementalType t2, Gender gender) {
      int types = (t1 != null ? 9 : 0) + (t2 != null ? 10 : 0);
      int genderWidth = gender != Gender.MALE && gender != Gender.FEMALE ? 0 : 3 + font.method_1727("♂");
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
   private static record MoveRow(class_2561 name, String pp, ElementalType type) {
   }
}
