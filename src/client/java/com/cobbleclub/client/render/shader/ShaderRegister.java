/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_293
 *  net.minecraft.class_2960
 *  net.minecraft.class_5944
 */
package com.cobbleclub.client.render.shader;

import com.cobbleclub.client.render.shader.LoaderInitializer;
import java.io.IOException;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.ShaderProgram;

@Environment(value=EnvType.CLIENT)
public interface ShaderRegister {
    public static final Factory INSTANCE = LoaderInitializer.getImplInstance(Factory.class, "com.cobbleclub.client.render.shader.ShaderRegisterFactory");

    default public ShaderProgram create(Identifier location, VertexFormat format) throws IOException {
        return this.create(location, format, true);
    }

    public ShaderProgram create(Identifier var1, VertexFormat var2, boolean var3) throws IOException;

    default public void register(Identifier location, VertexFormat format, Consumer<ShaderProgram> loadCallback) throws IOException {
        this.register(this.create(location, format), loadCallback);
    }

    public void register(ShaderProgram var1, Consumer<ShaderProgram> var2);

    @Environment(value=EnvType.CLIENT)
    public static interface Factory {
        public void register(String var1, Consumer<ShaderRegister> var2);
    }
}

