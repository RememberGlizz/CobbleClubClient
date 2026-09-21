package com.cobbleclub.client.hud;

import com.cobbleclub.server.network.Payloads;
import java.text.NumberFormat;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public final class FeatherboardHud {
    private static volatile Payloads.FeatherboardState state;
    private static volatile int liveOnline = -1;
    private static volatile boolean enabled = true;
    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);
    private static final float BOARD_SCALE = 0.5F;

    private FeatherboardHud() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.FeatherboardState.ID, (payload, context) ->
                context.client().execute(() -> {
                    state = payload;
                    liveOnline = payload.online();
                }));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.FeatherboardOnline.ID, (payload, context) ->
                context.client().execute(() -> liveOnline = payload.online()));
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("board").executes(context -> {
                    enabled = !enabled;
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("CobbleClub board " + (enabled ? "enabled." : "disabled."))
                                        .formatted(enabled ? Formatting.GREEN : Formatting.GRAY),
                                false
                        );
                    }
                    return 1;
                })));
    }

    private static void render(DrawContext g) {
        MinecraftClient client = MinecraftClient.getInstance();
        Payloads.FeatherboardState s = state;
        if (!enabled || s == null || client.player == null || client.options.hudHidden || client.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        TextRenderer font = client.textRenderer;
        int width = 110;
        int lineH = 10;
        int rows = 10;
        int height = 17 + rows * lineH + 7;

        // Render the exact same board design at half scale so all spacing, text,
        // borders and proportions stay intact while taking up half the screen space.
        int visualWidth = Math.round(width * BOARD_SCALE);
        int visualHeight = Math.round(height * BOARD_SCALE);
        int screenX = client.getWindow().getScaledWidth() - visualWidth - 8;
        int screenY = Math.max(28, (client.getWindow().getScaledHeight() - visualHeight) / 2);

        g.getMatrices().push();
        g.getMatrices().translate((float)screenX, (float)screenY, 0.0F);
        g.getMatrices().scale(BOARD_SCALE, BOARD_SCALE, 1.0F);

        int x = 0;
        int y = 0;
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x22000000);
        g.fillGradient(x, y, x + width, y + height, 0x66181024, 0x660B101A);
        g.drawBorder(x, y, width, height, 0xCC8B5CF6);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, 0xCCB784FF);

        drawGradientTitle(g, font, "✦ COBBLECLUB ✦", x + width / 2, y + 5);

        int yy = y + 17;
        row(g, font, x, yy, "Player", s.playerName(), 0xFFFFFFFF); yy += lineH;
        row(g, font, x, yy, "Rank", rankName(s.rank()), rankColor(s.rank())); yy += lineH;
        row(g, font, x, yy, "Poké$", NUMBERS.format(s.balance()), 0xFFFFD76A); yy += lineH;
        row(g, font, x, yy, "Gems", NUMBERS.format(s.gems()), 0xFF72E6FF); yy += lineH;
        row(g, font, x, yy, "Claims", NUMBERS.format(s.claimBlocks()), 0xFF92F7B3); yy += lineH;
        row(g, font, x, yy, "Caught", NUMBERS.format(s.catches()), 0xFFFF9F64); yy += lineH;
        row(g, font, x, yy, "Shiny", NUMBERS.format(s.shinies()), 0xFFFF70D0); yy += lineH;
        row(g, font, x, yy, "World", friendlyWorld(s.world()), 0xFFB9C6D8); yy += lineH;
        row(g, font, x, yy, "Online", Integer.toString(liveOnline >= 0 ? liveOnline : s.online()), 0xFFFFFFFF); yy += lineH;

        String health = String.format(Locale.ROOT, "%.1f TPS  ·  %.1f ms", s.tps(), s.mspt());
        int healthColor = s.tps() >= 19.0f ? 0xFF75F59A : (s.tps() >= 17.0f ? 0xFFFFD166 : 0xFFFF6B6B);
        g.fill(x + 5, yy - 1, x + width - 5, yy, 0x338B5CF6);
        g.drawCenteredTextWithShadow(font, Text.literal(health), x + width / 2, yy + 2, healthColor);

        g.getMatrices().pop();
    }

    private static void drawGradientTitle(DrawContext g, TextRenderer font, String text, int centerX, int y) {
        Text[] glyphs = text.codePoints()
                .mapToObj(cp -> Text.literal(new String(Character.toChars(cp))).formatted(Formatting.BOLD))
                .toArray(Text[]::new);

        int totalWidth = 0;
        for (Text glyph : glyphs) {
            totalWidth += font.getWidth(glyph);
        }

        int drawX = centerX - totalWidth / 2;
        for (int i = 0; i < glyphs.length; i++) {
            float t = glyphs.length <= 1 ? 0.0F : (float)i / (float)(glyphs.length - 1);
            int color;
            if (t < 0.5F) {
                color = lerpColor(0xFF72E6FF, 0xFFB784FF, t * 2.0F);
            } else {
                color = lerpColor(0xFFB784FF, 0xFFFFD76A, (t - 0.5F) * 2.0F);
            }
            Text glyph = glyphs[i];
            g.drawTextWithShadow(font, glyph, drawX, y, color);
            drawX += font.getWidth(glyph);
        }
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int a = (int)(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int)(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = (int)(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = (int)((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void row(DrawContext g, TextRenderer font, int x, int y, String label, String value, int valueColor) {
        String labelText = label + ":";
        int left = x + 5;
        int right = x + 105;
        g.drawTextWithShadow(font, Text.literal(labelText).formatted(Formatting.GRAY), left, y, 0xFFB7BBC5);
        int maxValueWidth = Math.max(18, right - left - font.getWidth(labelText) - 4);
        String fitted = fit(font, value, maxValueWidth);
        int valueWidth = font.getWidth(fitted);
        g.drawTextWithShadow(font, Text.literal(fitted), right - valueWidth, y, valueColor);
    }

    private static String fit(TextRenderer font, String value, int maxWidth) {
        if (value == null) return "";
        if (font.getWidth(value) <= maxWidth) return value;
        String ellipsis = "…";
        int target = Math.max(0, maxWidth - font.getWidth(ellipsis));
        String result = value;
        while (!result.isEmpty() && font.getWidth(result) > target) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }

    private static String rankName(String rank) {
        if (rank == null || rank.isBlank()) return "Newb";
        return Character.toUpperCase(rank.charAt(0)) + rank.substring(1).toLowerCase(Locale.ROOT);
    }

    private static int rankColor(String rank) {
        return switch (rank == null ? "" : rank.toLowerCase(Locale.ROOT)) {
            case "ace" -> 0xFF55FFFF;
            case "champion" -> 0xFFFF78FF;
            case "master" -> 0xFFFFAA33;
            case "legend" -> 0xFFFFFF55;
            case "mod" -> 0xFF55FF55;
            case "admin" -> 0xFFFF5555;
            default -> 0xFFFFFFFF;
        };
    }

    private static String friendlyWorld(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";
        String id = raw;
        int colon = id.indexOf(':');
        if (colon >= 0 && colon + 1 < id.length()) id = id.substring(colon + 1);
        id = id.replace('-', '_');
        if ("overworld".equalsIgnoreCase(id)) return "Overworld";
        if ("the_nether".equalsIgnoreCase(id)) return "Nether";
        if ("the_end".equalsIgnoreCase(id)) return "The End";
        if (id.toLowerCase(Locale.ROOT).endsWith("world") && id.length() > 5) {
            id = id.substring(0, id.length() - 5) + "_world";
        }
        StringBuilder out = new StringBuilder();
        for (String part : id.split("_+")) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }
}
