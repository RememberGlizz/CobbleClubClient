package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

@Environment(EnvType.CLIENT)
public final class ConfiguredText {
   private ConfiguredText() {
   }

   public static Text fill(Text text, String... placeholders) {
      return (Text)(text != null && placeholders.length >= 2 ? visit(text, placeholders) : text);
   }

   private static MutableText visit(Text source, String[] placeholders) {
      TextContent var4 = source.getContent();
      MutableText var10000;
      if (var4 instanceof PlainTextContent plain) {
         var10000 = Text.literal(replace(plain.string(), placeholders));
      } else {
         var10000 = source.copyContentOnly();
      }

      MutableText copy = var10000;
      copy.setStyle(source.getStyle());

      for(Text sibling : source.getSiblings()) {
         copy.append(visit(sibling, placeholders));
      }

      return copy;
   }

   private static String replace(String text, String[] placeholders) {
      String out = text;

      for(int i = 0; i + 1 < placeholders.length; i += 2) {
         out = out.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
      }

      return out;
   }
}
