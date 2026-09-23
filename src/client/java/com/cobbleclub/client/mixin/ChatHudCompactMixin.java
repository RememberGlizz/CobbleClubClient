package com.cobbleclub.client.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Compact presentation only. Message text, signed-chat content, scoreboard/tag
 * prefixes and all server-side chat behavior remain untouched.
 *
 * Uses the stable 1.21.1 intermediary selectors directly because this mixin is
 * shipped from the split client source set and the named selectors were not
 * being remapped in production.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudCompactMixin {
    private static final double COBBLECLUB_CHAT_SCALE = 0.88D;

    // ChatHud#getChatScale()D = class_338.method_1814()D in 1.21.1.
    @Inject(
            method = "method_1814()D",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void cobbleclub$compactChatScale(CallbackInfoReturnable<Double> cir) {
        double vanilla = cir.getReturnValue();
        cir.setReturnValue(Math.max(0.45D, vanilla * COBBLECLUB_CHAT_SCALE));
    }

    // ChatHud#getLineHeight()I = class_338.method_44752()I in 1.21.1.
    @Inject(
            method = "method_44752()I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void cobbleclub$compactLineHeight(CallbackInfoReturnable<Integer> cir) {
        int vanilla = cir.getReturnValue();
        cir.setReturnValue(Math.max(8, vanilla - 1));
    }
}
