package com.cobbleclub.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_5250;
import net.minecraft.class_7417;
import net.minecraft.class_8828;

@Environment(EnvType.CLIENT)
public final class ConfiguredText {
   private ConfiguredText() {
   }

   public static class_2561 fill(class_2561 text, String... placeholders) {
      return (class_2561)(text != null && placeholders.length >= 2 ? visit(text, placeholders) : text);
   }

   private static class_5250 visit(class_2561 source, String[] placeholders) {
      class_7417 var4 = source.method_10851();
      class_5250 var10000;
      if (var4 instanceof class_8828 plain) {
         var10000 = class_2561.method_43470(replace(plain.comp_737(), placeholders));
      } else {
         var10000 = source.method_27662();
      }

      class_5250 copy = var10000;
      copy.method_10862(source.method_10866());

      for(class_2561 sibling : source.method_10855()) {
         copy.method_10852(visit(sibling, placeholders));
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
