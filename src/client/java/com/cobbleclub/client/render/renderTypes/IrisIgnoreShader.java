/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_293
 *  net.minecraft.class_2960
 *  net.minecraft.class_5912
 *  net.minecraft.class_5944
 */
package com.cobbleclub.client.render.renderTypes;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.client.gl.ShaderProgram;

@Environment(value=EnvType.CLIENT)
public class IrisIgnoreShader
extends ShaderProgram {
    public IrisIgnoreShader(ResourceFactory provider, Identifier location, VertexFormat format) throws IOException {
        super(provider, location.toString(), format);
    }

    public boolean iris$shouldSkipThis() {
        return false;
    }
}

