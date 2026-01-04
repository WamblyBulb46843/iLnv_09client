package dev.iLnv_09.mod.modules.impl.render.skyBox;

import  dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import  dev.iLnv_09.mod.modules.settings.impl.ColorSetting;
import  dev.iLnv_09.mod.modules.Module;
import net.fabricmc.fabric.impl.client.rendering.DimensionRenderingRegistryImpl;
import net.minecraft.world.World;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Map;

public class SkyBox extends Module {
    public static SkyBox INSTANCE;
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(0.77F, 0.31F, 0.73F)));
    public final ColorSetting color2 = this.add(new ColorSetting("Color2", new Color(0.77F, 0.31F, 0.73F)));
    public final ColorSetting color3 = this.add(new ColorSetting("Color3", new Color(0.77F, 0.31F, 0.73F)));
    public final ColorSetting color4 = this.add(new ColorSetting("Color4", new Color(0.77F, 0.31F, 0.73F)));
    public final ColorSetting color5 = this.add(new ColorSetting("Color5", new Color(255, 255, 255)));
    final BooleanSetting stars = this.add(new BooleanSetting("Stars", true));
    public static final CustomSkyRenderer skyRenderer = new CustomSkyRenderer();

    public SkyBox() {
        super("SkyBox", "Custom skybox", Module.Category.Render);
        setChinese("天空盒");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        try {
            Field field = DimensionRenderingRegistryImpl.class.getDeclaredField("SKY_RENDERERS");
            field.setAccessible(true);
            Map SKY_RENDERERS = (Map)field.get(null);
            SKY_RENDERERS.putIfAbsent(World.OVERWORLD, skyRenderer);
            SKY_RENDERERS.putIfAbsent(World.NETHER, skyRenderer);
            SKY_RENDERERS.putIfAbsent(World.END, skyRenderer);
        } catch (Exception var3) {
            Exception e = var3;
            e.printStackTrace();
        }

    }

    @Override
    public void onDisable() {
        try {
            Field field = DimensionRenderingRegistryImpl.class.getDeclaredField("SKY_RENDERERS");
            field.setAccessible(true);
            Map SKY_RENDERERS = (Map)field.get(null);
            SKY_RENDERERS.remove(World.OVERWORLD, skyRenderer);
            SKY_RENDERERS.remove(World.NETHER, skyRenderer);
            SKY_RENDERERS.remove(World.END, skyRenderer);
        } catch (Exception var3) {
            Exception e = var3;
            e.printStackTrace();
        }

    }
}
