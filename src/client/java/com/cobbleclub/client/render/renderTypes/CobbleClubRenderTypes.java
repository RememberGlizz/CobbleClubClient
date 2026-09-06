package com.cobbleclub.client.render.renderTypes;

import com.cobbleclub.client.render.TeraMapLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CobbleClubRenderTypes {
   public static ShaderProgram teraFire;
   public static ShaderProgram teraWater;
   public static ShaderProgram teraGrass;
   public static ShaderProgram teraElectric;
   public static ShaderProgram teraIce;
   public static ShaderProgram teraFighting;
   public static ShaderProgram teraPoison;
   public static ShaderProgram teraGround;
   public static ShaderProgram teraFlying;
   public static ShaderProgram teraPsychic;
   public static ShaderProgram teraBug;
   public static ShaderProgram teraRock;
   public static ShaderProgram teraGhost;
   public static ShaderProgram teraDragon;
   public static ShaderProgram teraDark;
   public static ShaderProgram teraSteel;
   public static ShaderProgram teraFairy;
   public static ShaderProgram teraNormal;
   public static ShaderProgram teraStellar;

   public static RenderLayer pokemonShader(Identifier texture, String teraAspect) {
      return RenderLayer.of("tera_crystal_" + teraAspect, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 256, true, false, MultiPhaseParameters.builder().program(new RenderPhase.ShaderProgram(() -> TeraMapLoader.getColorShaderMap((String)TeraMapLoader.REGISTRY.get(teraAspect)))).texture(new RenderPhase.Texture(texture, false, false)).transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(RenderLayer.ENABLE_LIGHTMAP).overlay(RenderLayer.ENABLE_OVERLAY_COLOR).build(true));
   }

   public static String getTeraAnimationFromAspect(String aspect) {
      if (aspect == null) {
         return "cobblemon:tera_normal";
      } else {
         return aspect.startsWith("msd:") ? aspect.replaceFirst("msd:", "cobblemon:") : "cobblemon:tera_normal";
      }
   }
}
