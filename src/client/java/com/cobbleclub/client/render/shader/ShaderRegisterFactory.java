/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.render.shader;

import com.cobbleclub.client.render.shader.RegisterShaderEvent;
import com.cobbleclub.client.render.shader.ShaderRegister;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class ShaderRegisterFactory
implements ShaderRegister.Factory {
    @Override
    public void register(String modid, Consumer<ShaderRegister> consumer) {
        RegisterShaderEvent.EVENT.register(consumer::accept);
    }
}

