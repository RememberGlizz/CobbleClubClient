package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Neutral Minecraft-inspired CobbleClub palette.
 *
 * Layout geometry intentionally lives in the screens; this class only owns color.
 */
@Environment(EnvType.CLIENT)
public final class ClubPalette {
    public static final int PANEL_TOP = 0xF4484848;
    public static final int PANEL_BOTTOM = 0xF4323232;
    public static final int PANEL_BORDER_OUT = 0xFF111111;
    public static final int PANEL_BORDER_IN = 0xFF8E8E8E;
    public static final int HEADER_TOP = 0xFF5A5A5A;
    public static final int HEADER_BOTTOM = 0xFF3F3F3F;
    public static final int ACCENT = 0xFFBDBDBD;
    public static final int TITLE_COLOR = 0xFFF4F4F4;
    public static final int MUTED_TEXT = 0xFFB0B0B0;
    public static final int POSITIVE = 0xFF41A85F;
    public static final int WARNING = 0xFFE0A83F;
    public static final int NEGATIVE = 0xFFD64550;
    public static final int CONTENT_BG = 0xFF252525;
    public static final int ROW_BASE = 0xFF333333;
    public static final int ROW_HOVER = 0xFF474747;
    public static final int SCROLL_TRACK = 0xFF202020;
    public static final int SCROLL_THUMB = 0xFF777777;
    public static final int TEXT = 0xFFE5E5E5;
    public static final int SHADOW = 0x55000000;

    private ClubPalette() {
    }
}
