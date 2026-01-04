package dev.iLnv_09.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.iLnv_09.mod.modules.impl.misc.ExtraTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @ModifyConstant(constant = @Constant(longValue = 80L), method = "collectPlayerEntries")
    private long modifyCount(long count) {
        ExtraTab extraTab = ExtraTab.INSTANCE;

        return extraTab.isOn() ? extraTab.tabSize.getValueInt() : count;
    }

    @ModifyConstant(constant = @Constant(intValue = 20), method = "render")
    private int modifyHeight(int height) {
        ExtraTab extraTab = ExtraTab.INSTANCE;
        return extraTab.isOn() ? extraTab.tabHeight.getValueInt() : height;
    }

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(PlayerListEntry playerListEntry, CallbackInfoReturnable<Text> info) {
        ExtraTab extraTab = ExtraTab.INSTANCE;

        if (extraTab.isOn()) info.setReturnValue(extraTab.getPlayerName(playerListEntry));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    private int modifyWidth(int width) {
        ExtraTab extraTab = ExtraTab.INSTANCE;

        return extraTab.isOn() && extraTab.accurateLatency.getValue() ? width + 30 : width;
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        ExtraTab extraTab = ExtraTab.INSTANCE;

        if (extraTab.isOn() && extraTab.accurateLatency.getValue()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            TextRenderer textRenderer = mc.textRenderer;

            int latency = MathHelper.clamp(entry.getLatency(), 0, 9999);
            int color = latency < 150 ? 0x00E970 : latency < 300 ? 0xE7D020 : 0xD74238;
            String text = latency + "ms";
            context.drawTextWithShadow(textRenderer, text, x + width - textRenderer.getWidth(text), y, color);
            ci.cancel();
        }
    }
}
