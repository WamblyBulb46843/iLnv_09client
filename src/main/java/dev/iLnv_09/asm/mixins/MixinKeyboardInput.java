package dev.iLnv_09.asm.mixins;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.events.impl.KeyboardInputEvent;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z", shift = At.Shift.AFTER), cancellable = true)
    private void onSneak(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        KeyboardInputEvent event = new KeyboardInputEvent();
        iLnv_09.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }
}
