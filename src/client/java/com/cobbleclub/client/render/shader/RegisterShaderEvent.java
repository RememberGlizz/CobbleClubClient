package com.cobbleclub.client.render.shader;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@Environment(EnvType.CLIENT)
public interface RegisterShaderEvent {
   Event<RegisterShaderEvent> EVENT = EventFactory.createArrayBacked(RegisterShaderEvent.class, (listeners) -> (event) -> {
         for(RegisterShaderEvent listener : listeners) {
            listener.create(event);
         }

      });

   void create(ShaderRegister var1) throws IOException;
}
