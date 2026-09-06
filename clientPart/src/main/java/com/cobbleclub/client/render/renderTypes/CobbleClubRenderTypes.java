package com.cobbleclub.client.render.renderTypes;

import com.cobbleclub.client.render.TeraMapLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1921;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_4668;
import net.minecraft.class_5944;
import net.minecraft.class_1921.class_4688;
import net.minecraft.class_293.class_5596;

@Environment(EnvType.CLIENT)
public class CobbleClubRenderTypes {
   public static class_5944 teraFire;
   public static class_5944 teraWater;
   public static class_5944 teraGrass;
   public static class_5944 teraElectric;
   public static class_5944 teraIce;
   public static class_5944 teraFighting;
   public static class_5944 teraPoison;
   public static class_5944 teraGround;
   public static class_5944 teraFlying;
   public static class_5944 teraPsychic;
   public static class_5944 teraBug;
   public static class_5944 teraRock;
   public static class_5944 teraGhost;
   public static class_5944 teraDragon;
   public static class_5944 teraDark;
   public static class_5944 teraSteel;
   public static class_5944 teraFairy;
   public static class_5944 teraNormal;
   public static class_5944 teraStellar;

   public static class_1921 pokemonShader(class_2960 texture, String teraAspect) {
      return class_1921.method_24049("tera_crystal_" + teraAspect, class_290.field_1580, class_5596.field_27382, 256, true, false, class_4688.method_23598().method_34578(new class_4668.class_5942(() -> TeraMapLoader.getColorShaderMap((String)TeraMapLoader.REGISTRY.get(teraAspect)))).method_34577(new class_4668.class_4683(texture, false, false)).method_23615(class_1921.field_21370).method_23608(class_1921.field_21383).method_23611(class_1921.field_21385).method_23617(true));
   }

   public static String getTeraAnimationFromAspect(String aspect) {
      if (aspect == null) {
         return "cobblemon:tera_normal";
      } else {
         return aspect.startsWith("msd:") ? aspect.replaceFirst("msd:", "cobblemon:") : "cobblemon:tera_normal";
      }
   }
}
