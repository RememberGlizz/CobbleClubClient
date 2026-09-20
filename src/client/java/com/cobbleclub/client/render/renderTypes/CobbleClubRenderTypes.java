/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1921
 *  net.minecraft.class_1921$class_4688
 *  net.minecraft.class_290
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_4668$class_4683
 *  net.minecraft.class_4668$class_5939
 *  net.minecraft.class_4668$class_5942
 *  net.minecraft.class_5944
 */
package com.cobbleclub.client.render.renderTypes;

import com.cobbleclub.client.render.TeraMapLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.gl.ShaderProgram;

@Environment(value=EnvType.CLIENT)
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
        return RenderLayer.of((String)("tera_crystal_" + teraAspect), (VertexFormat)VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, (VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS, (int)256, (boolean)true, (boolean)false, (RenderLayer.MultiPhaseParameters)RenderLayer.MultiPhaseParameters.builder().program(new RenderPhase.ShaderProgram(() -> TeraMapLoader.getColorShaderMap(TeraMapLoader.REGISTRY.get(teraAspect)))).texture((RenderPhase.TextureBase)new RenderPhase.Texture(texture, false, false)).transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(RenderLayer.ENABLE_LIGHTMAP).overlay(RenderLayer.ENABLE_OVERLAY_COLOR).build(true));
    }

    public static String getTeraAnimationFromAspect(String aspect) {
        if (aspect == null) {
            return "cobblemon:tera_normal";
        }
        return aspect.startsWith("msd:") ? aspect.replaceFirst("msd:", "cobblemon:") : "cobblemon:tera_normal";
    }
}

