package dev.iLnv_09.mod.modules.impl.render;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.Module;
import net.minecraft.block.Block;

public class XRay extends Module {
    public static XRay INSTANCE;
    public XRay() {
        super("XRay", Category.Render);
        setChinese("矿物透视");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        mc.chunkCullingEnabled = false;
        mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        mc.chunkCullingEnabled = true;
        mc.worldRenderer.reload();
    }

    public boolean isCheckableOre(Block block) {
        return iLnv_09.XRAY.inWhitelist(block.getTranslationKey());
    }
}
