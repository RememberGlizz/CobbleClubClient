package com.cobbleclub.client.hud;

import com.cobbleclub.server.network.Payloads;
import java.text.NumberFormat;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public final class FeatherboardHud {
    private static volatile Payloads.FeatherboardState state;
    private static volatile int liveOnline = -1;
    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

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
    }

    private static void render(DrawContext g) {
        MinecraftClient client = MinecraftClient.getInstance();
        Payloads.FeatherboardState s = state;
        if (s == null || client.player == null || client.options.hudHidden || client.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        TextRenderer font = client.textRenderer;
        int width = 156;
        int lineH = 12;
        int rows = 10;
        int height = 20 + rows * lineH + 8;
        int x = client.getWindow().getScaledWidth() - width - 8;
        int y = Math.max(28, (client.getWindow().getScaledHeight() - height) / 2);

        g.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x66000000);
        g.fillGradient(x, y, x + width, y + height, 0xD9181024, 0xD90B101A);
        g.drawBorder(x, y, width, height, 0xFF8B5CF6);
        g.fill(x + 1, y + 1, x + width - 1, y + 3, 0xFFB784FF);

        Text title = Text.literal("✦ COBBLECLUB ✦").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        g.drawCenteredTextWithShadow(font, title, x + width / 2, y + 7, 0xFFFFFF);

        int yy = y + 22;
        row(g, font, x, yy, "Player", s.playerName(), 0xFFFFFFFF); yy += lineH;
        row(g, font, x, yy, "Rank", rankName(s.rank()), rankColor(s.rank())); yy += lineH;
        row(g, font, x, yy, "PokéDollars", NUMBERS.format(s.balance()), 0xFFFFD76A); yy += lineH;
        row(g, font, x, yy, "Gems", NUMBERS.format(s.gems()), 0xFF72E6FF); yy += lineH;
        row(g, font, x, yy, "Claim Blocks", NUMBERS.format(s.claimBlocks()), 0xFF92F7B3); yy += lineH;
        row(g, font, x, yy, "Caught", NUMBERS.format(s.catches()), 0xFFFF9F64); yy += lineH;
        row(g, font, x, yy, "Shinies", NUMBERS.format(s.shinies()), 0xFFFF70D0); yy += lineH;
        row(g, font, x, yy, "World", friendlyWorld(s.world()), 0xFFB9C6D8); yy += lineH;
        row(g, font, x, yy, "Online", Integer.toString(liveOnline >= 0 ? liveOnline : s.online()), 0xFFFFFFFF); yy += lineH;

        String health = String.format(Locale.ROOT, "%.1f TPS  ·  %.1f ms", s.tps(), s.mspt());
        int healthColor = s.tps() >= 19.0f ? 0xFF75F59A : (s.tps() >= 17.0f ? 0xFFFFD166 : 0xFFFF6B6B);
        g.fill(x + 7, yy - 2, x + width - 7, yy - 1, 0x448B5CF6);
        g.drawCenteredTextWithShadow(font, Text.literal(health), x + width / 2, yy + 3, healthColor);
    }

    private static void row(DrawContext g, TextRenderer font, int x, int y, String label, String value, int valueColor) {
        g.drawTextWithShadow(font, Text.literal(label + ":").formatted(Formatting.GRAY), x + 8, y, 0xFFB7BBC5);
        int valueWidth = font.getWidth(value);
        g.drawTextWithShadow(font, Text.literal(value), x + 148 - valueWidth, y, valueColor);
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
