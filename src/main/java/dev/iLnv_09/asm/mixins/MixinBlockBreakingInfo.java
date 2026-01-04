package dev.iLnv_09.asm.mixins;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.events.impl.WorldBreakEvent;
import net.minecraft.client.render.BlockBreakingInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBreakingInfo.class)
public class MixinBlockBreakingInfo {
    @Inject(method = "compareTo", at = @At("HEAD"))
    public void onCompareTo(BlockBreakingInfo blockBreakingInfo, CallbackInfoReturnable<Integer> cir) {
        iLnv_09.EVENT_BUS.post(new WorldBreakEvent(blockBreakingInfo));
    }
}
