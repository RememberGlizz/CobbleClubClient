package com.cobbleclub.client.battle;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.gui.MoveCategoryIcon;
import com.cobblemon.mod.common.pokemon.FormData;
import java.util.ArrayList;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
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

    public static void render(
            DrawContext graphics,
            MoveTemplate template,
            ElementalType moveType,
            int curPp,
            int maxPp,
            ClientBattlePokemon target,
            int mouseX,
            int mouseY
    ) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        ArrayList<Line> body = new ArrayList<>();

        DamageCategory category = template.getDamageCategory() != null
                ? template.getDamageCategory()
                : DamageCategories.INSTANCE.getSTATUS();

        boolean isStatus = "status".equalsIgnoreCase(category.getName());

        MutableText attackType = Text.literal("Attack Type: ")
                .withColor(LABEL_COLOR)
                .append(
                        Text.literal(MoveTooltipRenderer.titleCase(category.getName()))
                                .withColor(MoveTooltipRenderer.categoryColor(category))
                );

        body.add(new Line(attackType.asOrderedText(), NAME_COLOR, 0, category));

        if (!isStatus) {
            String power = template.getPower() > 0.0
                    ? String.valueOf((int) template.getPower())
                    : "—";

            body.add(MoveTooltipRenderer.labeled(font, "Power", power, SECTION_GAP));
        }

        String accuracy = template.getAccuracy() > 0.0
                ? (int) template.getAccuracy() + "%"
                : "—";

        body.add(
                MoveTooltipRenderer.labeled(
                        font,
                        "Accuracy",
                        accuracy,
                        isStatus ? SECTION_GAP : 0
                )
        );

        body.add(
                MoveTooltipRenderer.labeled(
                        font,
                        "PP",
                        curPp + "/" + maxPp,
                        0
                )
        );

        if (template.getPriority() != 0) {
            String priority =
                    (template.getPriority() > 0 ? "+" : "")
                            + template.getPriority();

            body.add(
                    MoveTooltipRenderer.labeled(
                            font,
                            "Priority",
                            priority,
                            0
                    )
            );
        }

        if (!isStatus && target != null) {
            FormData targetForm = BattleUtil.form(target);
            float mult = TypeChart.multiplier(
                    moveType,
                    targetForm.getPrimaryType(),
                    targetForm.getSecondaryType()
            );

            Text eff = MoveTooltipRenderer.effectivenessLine(mult, target);
            if (eff != null) {
                body.add(
                        new Line(
                                eff.asOrderedText(),
                                MoveTooltipRenderer.effectivenessColor(mult),
                                SECTION_GAP,
                                null
                        )
                );
            }
        }

        MutableText desc = template.getDescription();
        if (desc != null && !desc.getString().isBlank()) {
            boolean firstDescLine = true;

            for (OrderedText seq : font.wrapLines((StringVisitable) desc, MAX_DESC_WIDTH)) {
                body.add(
                        new Line(
                                seq,
                                DESC_COLOR,
                                firstDescLine ? SECTION_GAP : 0,
                                null
                        )
                );
                firstDescLine = false;
            }
        }

        OrderedText nameSeq = template.getDisplayName().asOrderedText();

        int headerWidth = CATEGORY_ICON_W + font.getWidth(nameSeq);
        int contentWidth = headerWidth;

        for (Line line : body) {
            int lineWidth = font.getWidth(line.seq());

            if (line.category() != null) {
                lineWidth += CATEGORY_ICON_W + SECTION_GAP;
            }

            contentWidth = Math.max(contentWidth, lineWidth);
        }

        int lineHeight = 11;
        int boxWidth = contentWidth + PADDING * 2;
        int headerHeight = ICON_SIZE;
        int boxHeight = PADDING * 2 + headerHeight + SECTION_GAP;

        for (Line line : body) {
            boxHeight += lineHeight + line.gapAbove();
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
        graphics.getMatrices().translate(0.0f, 0.0f, 400.0f);

        BattlePanel.draw(graphics, x, y, boxWidth, boxHeight);

        int tx = x + PADDING;
        int ty = y + PADDING;

        TypeIconRenderer.drawType(
                graphics,
                moveType,
                tx,
                ty,
                ICON_SIZE
        );

        int nameX = tx + ICON_SIZE + SECTION_GAP;

        graphics.drawTextWithShadow(
                font,
                nameSeq,
                nameX,
                ty + (headerHeight - ICON_SIZE) / 2 + 1,
                NAME_COLOR
        );

        int rowY = ty + headerHeight + SECTION_GAP;

        for (Line line : body) {
            rowY += line.gapAbove();

            graphics.drawTextWithShadow(
                    font,
                    line.seq(),
                    tx,
                    rowY,
                    line.color()
            );

            if (line.category() != null) {
                new MoveCategoryIcon(
                        tx + font.getWidth(line.seq()) + SECTION_GAP,
                        rowY,
                        line.category(),
                        1.0f
                ).render(graphics);
            }

            rowY += lineHeight;
        }

        graphics.getMatrices().pop();
    }

    private static Line labeled(
            TextRenderer font,
            String label,
            String value,
            int gapAbove
    ) {
        MutableText text = Text.literal(label + ": ")
                .withColor(LABEL_COLOR)
                .append(Text.literal(value).withColor(NAME_COLOR));

        return new Line(
                text.asOrderedText(),
                NAME_COLOR,
                gapAbove,
                null
        );
    }

    private static String titleCase(String s) {
        if (s != null && !s.isEmpty()) {
            char first = Character.toUpperCase(s.charAt(0));
            return first + s.substring(1).toLowerCase(Locale.ROOT);
        }

        return s;
    }

    private static int categoryColor(DamageCategory category) {
        return switch (category.getName().toLowerCase(Locale.ROOT)) {
            case "physical" -> PHYSICAL_COLOR;
            case "special" -> SPECIAL_COLOR;
            default -> STATUS_COLOR;
        };
    }

    private static Text effectivenessLine(
            float mult,
            ClientBattlePokemon target
    ) {
        if (mult == 0.0f) {
            return Text.literal(
                    "No effect on " + target.getSpecies().getName()
            );
        }

        if (mult > 1.0f) {
            return Text.literal(
                    "Super effective (" + MoveTooltipRenderer.trim(mult) + "x)"
            );
        }

        return mult < 1.0f
                ? Text.literal(
                "Not very effective ("
                        + MoveTooltipRenderer.trim(mult)
                        + "x)"
        )
                : null;
    }

    private static int effectivenessColor(float mult) {
        if (mult == 0.0f) {
            return IMMUNE_COLOR;
        }

        return mult > 1.0f
                ? SUPER_COLOR
                : WEAK_COLOR;
    }

    private static String trim(float value) {
        return value == (float) ((int) value)
                ? String.valueOf((int) value)
                : String.valueOf(value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    @Environment(EnvType.CLIENT)
    private record Line(
            OrderedText seq,
            int color,
            int gapAbove,
            DamageCategory category
    ) {
    }
}

