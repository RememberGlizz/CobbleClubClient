package com.cobbleclub.client.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Compact presentation only. Message text, signed-chat content, scoreboard/tag
 * prefixes and all server-side chat behavior remain untouched.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudCompactMixin {
    private static final double COBBLECLUB_CHAT_SCALE = 0.88D;

    @Inject(method = "getChatScale", at = @At("RETURN"), cancellable = true)
    private void cobbleclub$compactChatScale(CallbackInfoReturnable<Double> cir) {
        double vanilla = cir.getReturnValue();
        cir.setReturnValue(Math.max(0.45D, vanilla * COBBLECLUB_CHAT_SCALE));
    }

    @Inject(method = "getLineHeight", at = @At("RETURN"), cancellable = true)
    private void cobbleclub$compactLineHeight(CallbackInfoReturnable<Integer> cir) {
        int vanilla = cir.getReturnValue();
        cir.setReturnValue(Math.max(8, vanilla - 1));
    }
}
