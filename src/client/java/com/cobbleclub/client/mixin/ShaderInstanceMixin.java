package com.cobbleclub.client.mixin;

import com.cobbleclub.client.render.renderTypes.IrisIgnoreShader;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(ShaderProgram.class)
public abstract class ShaderInstanceMixin {
    @Shadow(remap = false) @Final private String field_29494;

    @WrapOperation(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2960;method_60656(Ljava/lang/String;)Lnet/minecraft/class_2960;", remap = false),
        allow = 1,
        remap = false
    )
    private Identifier cobbleclub$modifyId(String id, Operation<Identifier> original) {
        if ((Object) this instanceof IrisIgnoreShader) return FabricShaderProgram.rewriteAsId(id, this.field_29494);
        return original.call(id);
    }
}
