package dev.iLnv_09.asm.mixins;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.gui.clickgui.ClickGuiScreen;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.mod.modules.settings.impl.StringSetting;
import dev.iLnv_09.api.utils.Wrapper;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Keyboard.class)
public class MixinKeyboard implements Wrapper {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
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

    @Shadow @Final private MinecraftClient client;
    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (window == this.client.getWindow().getHandle()) {
            Element element = this.client.currentScreen;
            if (element != null && this.client.getOverlay() == null) {
                if (Character.charCount(codePoint) == 1) {
                    if (!Module.nullCheck() && iLnv_09.GUI != null) {
                        if (iLnv_09.GUI.isClickGuiOpen()) {
                            iLnv_09.MODULE.modules.forEach(module -> module.getSettings().stream()
                                    .filter(setting -> setting instanceof StringSetting)
                                    .map(setting -> (StringSetting) setting)
                                    .filter(StringSetting::isListening)
                                    .forEach(setting -> setting.charType((char)codePoint)));
                            iLnv_09.MODULE.modules.forEach(module -> module.getSettings().stream()
                                    .filter(setting -> setting instanceof SliderSetting)
                                    .map(setting -> (SliderSetting) setting)
                                    .filter(SliderSetting::isListening)
                                    .forEach(setting -> setting.charType((char)codePoint)));
                        }
                    }
                    Screen.wrapScreenError(() -> element.charTyped((char)codePoint, modifiers), "charTyped event handler", element.getClass().getCanonicalName());
                } else {
                    char[] var6 = Character.toChars(codePoint);

                    for (char c : var6) {
                        if (!Module.nullCheck() && iLnv_09.GUI != null) {
                            if (iLnv_09.GUI.isClickGuiOpen()) {
                                iLnv_09.MODULE.modules.forEach(module -> module.getSettings().stream()
                                        .filter(setting -> setting instanceof StringSetting)
                                        .map(setting -> (StringSetting) setting)
                                        .filter(StringSetting::isListening)
                                        .forEach(setting -> setting.charType(c)));
                                iLnv_09.MODULE.modules.forEach(module -> module.getSettings().stream()
                                        .filter(setting -> setting instanceof SliderSetting)
                                        .map(setting -> (SliderSetting) setting)
                                        .filter(SliderSetting::isListening)
                                        .forEach(setting -> setting.charType((char)codePoint)));
                            }
                        }
                        Screen.wrapScreenError(() -> element.charTyped(c, modifiers), "charTyped event handler", element.getClass().getCanonicalName());
                    }
                }
            }
        }
        ci.cancel();
    }
}
