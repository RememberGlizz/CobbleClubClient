package com.cobbleclub.client.mixin;

import com.cobbleclub.client.render.renderTypes.IrisIgnoreShader;
import com.cobbleclub.client.render.shader.RegisterShaderEvent;
import com.cobbleclub.client.render.shader.ShaderRegister;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(
        method = "method_34538(Lnet/minecraft/class_5912;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/class_757;method_57797(Lnet/minecraft/class_5912;)V", shift = At.Shift.AFTER, remap = false),
        remap = false
    )
    private void cobbleclub$onShaderLoad(ResourceFactory provider, CallbackInfo info, @Local(index = 3) List<Pair<ShaderProgram, Consumer<ShaderProgram>>> programs) throws IOException {
        ShaderRegister register = new ShaderRegister() {
            @Override
            public ShaderProgram create(Identifier location, VertexFormat format, boolean irisIgnore) throws IOException {
                if (irisIgnore) return new IrisIgnoreShader(provider, location, format);
                return new FabricShaderProgram(provider, location, format);
            }
            @Override
            public void register(ShaderProgram shaderInstance, Consumer<ShaderProgram> loadCallback) {
                programs.add(Pair.of(shaderInstance, loadCallback));
            }
        };
        ((RegisterShaderEvent) RegisterShaderEvent.EVENT.invoker()).create(register);
    }
}
