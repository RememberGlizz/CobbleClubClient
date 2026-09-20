/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.abilities.AbilityTemplate
 *  com.cobblemon.mod.common.api.abilities.PotentialAbility
 *  com.cobblemon.mod.common.api.moves.Move
 *  com.cobblemon.mod.common.api.moves.MoveTemplate
 *  com.cobblemon.mod.common.api.moves.Moves
 *  com.cobblemon.mod.common.api.pokemon.stats.Stat
 *  com.cobblemon.mod.common.api.types.ElementalType
 *  com.cobblemon.mod.common.client.battle.ClientBattlePokemon
 *  com.cobblemon.mod.common.pokemon.FormData
 *  com.cobblemon.mod.common.pokemon.Gender
 *  com.cobblemon.mod.common.pokemon.Pokemon
 *  com.cobblemon.mod.common.pokemon.abilities.HiddenAbility
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package com.cobbleclub.client.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.BattlePanel;
import com.cobbleclub.client.battle.BattleState;
import com.cobbleclub.client.battle.BattleUtil;
import com.cobbleclub.client.battle.RevealedBattleInfo;
import com.cobbleclub.client.battle.SpeedRangeResolver;
import com.cobbleclub.client.battle.TurnIndicatorRenderer;
import com.cobbleclub.client.battle.TypeIconRenderer;
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
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;

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
            while (it.hasNext()) {
                Map.Entry<UUID, BattleState.TileRect> entry = it.next();
                BattleState.TileRect rect = entry.getValue();
                if ((float)mouseX < rect.x || (float)mouseX > rect.x + rect.width || (float)mouseY < rect.y || (float)mouseY > rect.y + rect.height) continue;
                ClientBattlePokemon pokemon = BattleUtil.activeByUuid(entry.getKey());
                if (pokemon != null) {
                    HoverInfoPanel.render(graphics, pokemon, rect);
                    return;
                }
                it.remove();
            }
        }
    }

    private static void render(DrawContext graphics, ClientBattlePokemon pokemon, BattleState.TileRect rect) {
        int x;
        Text statChanges;
        String formName;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        Pokemon own = BattleUtil.localPartyPokemon(pokemon.getUuid());
        RevealedBattleInfo revealed = BattleState.revealedOrNull(pokemon.getUuid());
        FormData form = BattleUtil.form(pokemon);
        ElementalType type1 = own != null ? own.getPrimaryType() : form.getPrimaryType();
        ElementalType type2 = own != null ? own.getSecondaryType() : form.getSecondaryType();
        Gender gender = pokemon.getGender();
        MutableText header = Text.literal(pokemon.getDisplayName().getString())
                .append(Text.literal("  Lv." + pokemon.getLevel()).withColor(-5197632));
        ArrayList<Text> lines = new ArrayList<>();
        SpeedRangeResolver.SpeedInfo speed = SpeedRangeResolver.resolve(pokemon);
        if (speed != null) {
            lines.add(HoverInfoPanel.labeled("Speed", speed.display(), -1));
        }
        HoverInfoPanel.addAbilityLines(lines, pokemon, own, revealed);
        Text item = HoverInfoPanel.itemLine(own, revealed);
        if (item != null) {
            lines.add(item);
        }
        formName = pokemon.getProperties() != null ? pokemon.getProperties().getForm() : null;
        if (formName != null && !formName.isBlank() && !formName.equalsIgnoreCase("normal")) {
            lines.add(HoverInfoPanel.labeled("Form", formName, -1));
        }
        if ((statChanges = HoverInfoPanel.statChangeLine(pokemon)) != null) {
            lines.add(statChanges);
        }
        List<MoveRow> moves = HoverInfoPanel.moveRows(own, revealed);
        int contentWidth = font.getWidth(header);
        contentWidth = Math.max(contentWidth, HoverInfoPanel.typeRowWidth(font, type1, type2, gender));
        for (Text line : lines) {
            contentWidth = Math.max(contentWidth, font.getWidth(line));
        }
        for (MoveRow move : moves) {
            contentWidth = Math.max(contentWidth, 11 + font.getWidth(move.name()) + 8 + font.getWidth(move.pp()));
        }
        int lineH = 10;
        int rows = lines.size() + (moves.isEmpty() ? 0 : 1 + moves.size());
        int boxW = contentWidth + 8;
        int boxH = 29 + rows * lineH;
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        x = rect.opponent ? (int)rect.x - boxW - 2 : (int)(rect.x + rect.width) + 2;
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
        graphics.getMatrices().translate(0.0f, 0.0f, 400.0f);
        BattlePanel.draw(graphics, x, y, boxW, boxH);
        int cx = x + 4;
        int cy = y + 4;
        graphics.drawTextWithShadow(font, header, cx, cy, -1);
        int typeWidth = TypeIconRenderer.drawTypes(graphics, type1, type2, cx, cy += 10, 9, 1);
        if (gender == Gender.MALE) {
            graphics.drawTextWithShadow(font, Text.literal("\u2642"), cx + typeWidth + 3, cy + 1, -11167233);
        } else if (gender == Gender.FEMALE) {
            graphics.drawTextWithShadow(font, Text.literal("\u2640"), cx + typeWidth + 3, cy + 1, -34902);
        }
        cy += 11;
        for (Text line : lines) {
            graphics.drawTextWithShadow(font, line, cx, cy, -1);
            cy += lineH;
        }
        if (!moves.isEmpty()) {
            graphics.drawTextWithShadow(font, Text.literal("Moves:").withColor(-5197632), cx, cy, -5197632);
            cy += lineH;
            for (MoveRow move : moves) {
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
            lines.add(HoverInfoPanel.labeled("Ability", Text.translatable(own.getAbility().getDisplayName()), -1));
        } else if (revealed != null && revealed.abilityName != null) {
            lines.add(HoverInfoPanel.labeled("Ability", Text.literal(revealed.abilityName), -1));
        } else {
            List<Text> possible = HoverInfoPanel.possibleAbilities(pokemon);
            if (possible.isEmpty()) {
                lines.add(HoverInfoPanel.labeled("Ability", Text.literal("???"), -8355696));
            } else if (possible.size() == 1) {
                lines.add(HoverInfoPanel.labeled("Ability", possible.get(0), -1));
            } else {
                lines.add(Text.literal("Possible Abilities:").withColor(-5197632));
                for (Text ability : possible) {
                    lines.add(Text.literal("  ").append(ability.copy().withColor(-1)));
                }
            }
        }
    }

    private static List<Text> possibleAbilities(ClientBattlePokemon pokemon) {
        boolean wild = BattleUtil.isWildActor(pokemon);
        Set<String> aspects = pokemon.getState().getCurrentAspects();
        LinkedHashMap<String, AbilityTemplate> commons = new LinkedHashMap<>();
        LinkedHashMap<String, AbilityTemplate> hiddens = new LinkedHashMap<>();
        for (PotentialAbility potential : BattleUtil.form(pokemon).getAbilities()) {
            if (potential instanceof HiddenAbility) {
                if (wild) continue;
                hiddens.putIfAbsent(potential.getTemplate().getName(), potential.getTemplate());
                continue;
            }
            if (!potential.isSatisfiedBy(aspects)) continue;
            commons.putIfAbsent(potential.getTemplate().getName(), potential.getTemplate());
        }
        ArrayList<Text> result = new ArrayList<>();
        for (AbilityTemplate abilityTemplate : commons.values()) {
            result.add(Text.translatable(abilityTemplate.getDisplayName()));
        }
        for (Map.Entry<String, AbilityTemplate> entry : hiddens.entrySet()) {
            if (commons.containsKey(entry.getKey())) continue;
            result.add(Text.translatable((entry.getValue()).getDisplayName()).append(Text.literal(" (HA)").withColor(-5197632)));
        }
        return result;
    }

    private static Text itemLine(Pokemon own, RevealedBattleInfo revealed) {
        if (own != null) {
            ItemStack held = own.getHeldItem$common();
            return held != null && !held.isEmpty() ? HoverInfoPanel.labeled("Item", held.getName().getString(), -1) : null;
        }
        if (revealed != null && revealed.heldItemName != null) {
            String suffix = revealed.heldItemConsumed ? " (used)" : "";
            return HoverInfoPanel.labeled("Item", revealed.heldItemName + suffix, -1);
        }
        return HoverInfoPanel.labeled("Item", Text.literal("???"), -8355696);
    }

    private static Text statChangeLine(ClientBattlePokemon pokemon) {
        Map<Stat, Integer> changes = pokemon.getStatChanges();
        if (changes != null && !changes.isEmpty()) {
            MutableText result = Text.empty();
            boolean any = false;
            for (Map.Entry<Stat, Integer> entry : changes.entrySet()) {
                int stage = entry.getValue();
                if (stage == 0) continue;
                if (any) {
                    result = result.copy().append(Text.literal("  "));
                }
                String text = HoverInfoPanel.shortStat(entry.getKey()) + " " + (stage > 0 ? "+" : "") + stage;
                result = result.copy().append(Text.literal(text).withColor(stage > 0 ? -9776022 : -38294));
                any = true;
            }
            return any ? result : null;
        }
        return null;
    }

    private static List<MoveRow> moveRows(Pokemon own, RevealedBattleInfo revealed) {
        ArrayList<MoveRow> rows = new ArrayList<>();

        if (own != null && own.getMoveSet() != null) {
            for (Move move : own.getMoveSet().getMoves()) {
                if (move == null) {
                    continue;
                }

                rows.add(
                        new MoveRow(
                                move.getDisplayName(),
                                move.getCurrentPp() + "/" + move.getMaxPp(),
                                move.getType()
                        )
                );
            }

            return rows;
        }

        if (revealed != null) {
            for (Map.Entry<String, RevealedBattleInfo.RevealedMove> entry : revealed.moves.entrySet()) {
                RevealedBattleInfo.RevealedMove move = entry.getValue();
                MoveTemplate template = Moves.getByName(entry.getKey());
                int maxPp = template != null ? template.getMaxPp() : 0;
                ElementalType type = template != null ? template.getElementalType() : null;
                String pp = maxPp > 0
                        ? "~" + Math.max(0, maxPp - move.uses) + "/" + maxPp
                        : "";

                rows.add(
                        new MoveRow(
                                Text.literal(move.displayName),
                                pp,
                                type
                        )
                );
            }
        }

        return rows;
    }

    private static Text labeled(String label, String value, int valueColor) {
        return HoverInfoPanel.labeled(label, Text.literal(value), valueColor);
    }

    private static Text labeled(String label, Text value, int valueColor) {
        return Text.literal(label + ": ").withColor(-5197632).append(value.copy().withColor(valueColor));
    }

    private static int typeRowWidth(TextRenderer font, ElementalType t1, ElementalType t2, Gender gender) {
        int types = (t1 != null ? 9 : 0) + (t2 != null ? 10 : 0);
        int genderWidth = gender != Gender.MALE && gender != Gender.FEMALE ? 0 : 3 + font.getWidth("\u2642");
        return types + genderWidth;
    }

    private static String shortStat(Stat stat) {
        return switch (stat.getShowdownId()) {
            case "atk" -> "Atk";
            case "def" -> "Def";
            case "spa" -> "SpA";
            case "spd" -> "SpD";
            case "spe" -> "Spe";
            case "accuracy" -> "Acc";
            case "evasion" -> "Eva";
            default -> stat.getShowdownId();
        };
    }

    @Environment(EnvType.CLIENT)
    private record MoveRow(Text name, String pp, ElementalType type) {
    }
}
