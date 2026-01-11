package dev.iLnv_09.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public class CrystalRender
        extends Module {
    public static CrystalRender INSTANCE;
    public static HashMap<EndCrystalEntity, Double> spinMap;
    public static HashMap<Vec3d, Double> posSpinMap;
    public static HashMap<EndCrystalEntity, Double> floatMap;
    public static HashMap<Vec3d, Double> posFloatMap;
    public static Random random;
    public BooleanSetting cham = this.add(new BooleanSetting("CrystalChams", true)).setParent();
    private final ColorSetting crystalColor = this.add(new ColorSetting("CrystalColor", new Color(-1825711896, true), () -> this.cham.isOpen()));

    public CrystalRender() {
        super("CrystalRender", Module.Category.Render);
        setChinese("水晶渲染");
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        ArrayList<EndCrystalEntity> noSpinAge = new ArrayList<EndCrystalEntity>();
        ArrayList<EndCrystalEntity> noFloatAge = new ArrayList<EndCrystalEntity>();
        for (Entity entity : CrystalRender.mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            EndCrystalEntity crystal = (EndCrystalEntity)entity;
            if (spinMap.getOrDefault((Object)crystal, -1.0) != -1.0) {
                spinMap.put(crystal, spinMap.get((Object)crystal) + 1.0);
                posSpinMap.put(crystal.getPos(), spinMap.get((Object)crystal));
            } else {
                noSpinAge.add(crystal);
            }
            if (floatMap.getOrDefault((Object)crystal, -1.0) != -1.0) {
                floatMap.put(crystal, floatMap.get((Object)crystal) + 1.0);
                posFloatMap.put(crystal.getPos(), floatMap.get((Object)crystal));
                continue;
            }
            noFloatAge.add(crystal);
        }
        for (EndCrystalEntity crystal : noSpinAge) {
            if (spinMap.getOrDefault((Object)crystal, -1.0) != -1.0) continue;
            spinMap.put(crystal, posSpinMap.getOrDefault((Object)crystal.getPos(), Double.valueOf(random.nextInt(10000))));
        }
        for (EndCrystalEntity crystal : noFloatAge) {
            if (floatMap.getOrDefault((Object)crystal, -1.0) != -1.0) continue;
            floatMap.put(crystal, posFloatMap.getOrDefault((Object)crystal.getPos(), Double.valueOf(random.nextInt(10000))));
        }
    }

    public double getSpinAge(EndCrystalEntity crystal) {
        double age;
        if (spinMap.getOrDefault((Object)crystal, -1.0) == -1.0) {
            spinMap.put(crystal, posSpinMap.getOrDefault((Object)crystal.getPos(), Double.valueOf(random.nextInt(10000))));
        }
        if ((age = spinMap.getOrDefault((Object)crystal, posSpinMap.getOrDefault((Object)crystal.getPos(), -1.0)).doubleValue()) != -1.0) {
            return age;
        }
        age = random.nextInt(10000);
        posSpinMap.put(crystal.getPos(), age);
        return age;
    }

    public double getFloatAge(EndCrystalEntity crystal) {
        double age;
        if (floatMap.getOrDefault((Object)crystal, -1.0) == -1.0) {
            floatMap.put(crystal, posFloatMap.getOrDefault((Object)crystal.getPos(), Double.valueOf(random.nextInt(10000))));
        }
        if ((age = floatMap.getOrDefault((Object)crystal, posFloatMap.getOrDefault((Object)crystal.getPos(), -1.0)).doubleValue()) != -1.0) {
            return age;
        }
        age = random.nextInt(10000);
        posFloatMap.put(crystal.getPos(), age);
        return age;
    }

    public void renderCrystal(EndCrystalEntity endCrystalEntity, float f, float g, MatrixStack matrixStack, int i, ModelPart core, ModelPart frame) {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        matrixStack.push();
        float h = EndCrystalEntityRenderer.getYOffset((EndCrystalEntity)endCrystalEntity, (float)g);
        float j = ((float)endCrystalEntity.endCrystalAge + g) * 3.0f;
        matrixStack.push();
        RenderSystem.setShaderColor((float)((float)this.crystalColor.getValue().getRed() / 255.0f), (float)((float)this.crystalColor.getValue().getGreen() / 255.0f), (float)((float)this.crystalColor.getValue().getBlue() / 255.0f), (float)((float)this.crystalColor.getValue().getAlpha() / 255.0f));
        matrixStack.scale(2.0f, 2.0f, 2.0f);
        matrixStack.translate(0.0f, -0.5f, 0.0f);
        int k = OverlayTexture.DEFAULT_UV;
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        matrixStack.translate(0.0f, 1.5f + h / 2.0f, 0.0f);
        matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, (float)Math.sin(0.7853981633974483), (float)Math.sin(0.7853981633974483), (float)Math.sin(0.7853981633974483)));
        frame.render(matrixStack, (VertexConsumer)buffer, i, k);
        matrixStack.scale(0.875f, 0.875f, 0.875f);
        matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, (float)Math.sin(0.7853981633974483), 0.0f, (float)Math.sin(0.7853981633974483)));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        frame.render(matrixStack, (VertexConsumer)buffer, i, k);
        matrixStack.scale(0.875f, 0.875f, 0.875f);
        matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, (float)Math.sin(0.7853981633974483), 0.0f, (float)Math.sin(0.7853981633974483)));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        core.render(matrixStack, (VertexConsumer)buffer, i, k);
        matrixStack.pop();
        matrixStack.pop();
        tessellator.draw();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    static {
        spinMap = new HashMap();
        posSpinMap = new HashMap();
        floatMap = new HashMap();
        posFloatMap = new HashMap();
        random = new Random();
    }
}