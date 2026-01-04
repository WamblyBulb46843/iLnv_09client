package dev.iLnv_09.asm.mixins;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.events.impl.MouseUpdateEvent;
import dev.iLnv_09.mod.gui.clickgui.ClickGuiScreen;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.iLnv_09.api.utils.Wrapper.mc;
@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouse(long window, int button, int action, int mods, CallbackInfo ci) {
        int key = -(button + 2);
        if (mc.currentScreen instanceof ClickGuiScreen && action == 1 && iLnv_09.MODULE.setBind(key)) {
            return;
        }
        if (action == 1) {
            iLnv_09.MODULE.onKeyPressed(key);
        }
        if (action == 0) {
            iLnv_09.MODULE.onKeyReleased(key);
        }
    }

    @Inject(method = "updateMouse", at = @At("RETURN"))
    private void updateHook(CallbackInfo ci) {
        iLnv_09.EVENT_BUS.post(new MouseUpdateEvent());
    }
}
