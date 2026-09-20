/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2561
 *  net.minecraft.class_5250
 *  net.minecraft.class_7417
 *  net.minecraft.class_8828
 */
package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextContent;
import net.minecraft.text.PlainTextContent;

@Environment(value=EnvType.CLIENT)
public final class ConfiguredText {
    private ConfiguredText() {
    }

    public static Text fill(Text text, String ... placeholders) {
        return text != null && placeholders.length >= 2 ? ConfiguredText.visit(text, placeholders) : text;
    }

    private static MutableText visit(Text source, String[] placeholders) {
        MutableText var10000;
        TextContent var4 = source.getContent();
        if (var4 instanceof PlainTextContent) {
            PlainTextContent plain = (PlainTextContent)var4;
            var10000 = Text.literal((String)ConfiguredText.replace(plain.string(), placeholders));
        } else {
            var10000 = source.copyContentOnly();
        }
        MutableText copy = var10000;
        copy.setStyle(source.getStyle());
        for (Text sibling : source.getSiblings()) {
            copy.append((Text)ConfiguredText.visit(sibling, placeholders));
        }
        return copy;
    }

    private static String replace(String text, String[] placeholders) {
        String out = text;
        int i = 0;
        while (i + 1 < placeholders.length) {
            out = out.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
            i += 2;
        }
        return out;
    }
}

