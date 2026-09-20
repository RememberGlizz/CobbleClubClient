/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.event.Event
 *  net.fabricmc.fabric.api.event.EventFactory
 */
package com.cobbleclub.client.render.shader;

import com.cobbleclub.client.render.shader.ShaderRegister;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@Environment(value=EnvType.CLIENT)
public interface RegisterShaderEvent {
    public static final Event<RegisterShaderEvent> EVENT = EventFactory.createArrayBacked(RegisterShaderEvent.class, listeners -> event -> {
        for (RegisterShaderEvent listener : listeners) {
            listener.create(event);
        }
    });

    public void create(ShaderRegister var1) throws IOException;
}

