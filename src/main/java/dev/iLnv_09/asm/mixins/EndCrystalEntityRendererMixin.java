package dev.iLnv_09.asm.mixins;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import dev.iLnv_09.api.utils.render.CubeUtil;
import dev.iLnv_09.mod.modules.impl.render.CrystalRender;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EndCrystalEntityRenderer.class})
public abstract class EndCrystalEntityRendererMixin {
    private static final int ANIMATION_LENGTH = 400;
    private static final float CUBELET_SCALE = 0.4f;
    private int rotatingSide = 0;
    private long lastTime = 0L;
    private EndCrystalEntity endCrystalEntity;
    @Shadow
    @Final
    private ModelPart core;
    @Shadow
    @Final
    private ModelPart frame;

    @Inject(method={"render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at={@At(value="HEAD")}, cancellable=true)
    public void render(EndCrystalEntity endCrystalEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        this.endCrystalEntity = endCrystalEntity;
        if (CrystalRender.INSTANCE != null && CrystalRender.INSTANCE.isOn() && CrystalRender.INSTANCE.cham.getValue()) {
            ci.cancel();
            CrystalRender.INSTANCE.renderCrystal(endCrystalEntity, f, g, matrixStack, i, this.core, this.frame);
        }
    }

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="net/minecraft/client/util/math/MatrixStack.translate(FFF)V", ordinal=1), require=0)
    public void translate(MatrixStack class_45872, float f, float f2, float f3) {
        class_45872.translate(f, this.endCrystalEntity != null && this.endCrystalEntity.shouldShowBottom() ? 1.2f : 1.0f, f3);
    }

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="net/minecraft/client/model/ModelPart.render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal=3), require=0)
    public void renderCore(ModelPart class_6302, MatrixStack class_45872, VertexConsumer class_45882, int n, int n2) {
        this.render(class_6302, class_45872, class_45882, n, n2);
    }

    public void render(ModelPart class_6302, MatrixStack class_45872, VertexConsumer class_45882, int n, int n2) {
        if (CrystalRender.INSTANCE != null && CrystalRender.INSTANCE.isOn()) {
            long l = System.currentTimeMillis();
            if (l - 400L > this.lastTime) {
                int[] nArray = CubeUtil.cubeSides[this.rotatingSide];
                Quaternionf[] quaternionfArray = new Quaternionf[]{CubeUtil.cubeletStatus[nArray[0]], CubeUtil.cubeletStatus[nArray[1]], CubeUtil.cubeletStatus[nArray[2]], CubeUtil.cubeletStatus[nArray[3]], CubeUtil.cubeletStatus[nArray[4]], CubeUtil.cubeletStatus[nArray[5]], CubeUtil.cubeletStatus[nArray[6]], CubeUtil.cubeletStatus[nArray[7]], CubeUtil.cubeletStatus[nArray[8]]};
                CubeUtil.cubeletStatus[nArray[0]] = quaternionfArray[6];
                CubeUtil.cubeletStatus[nArray[1]] = quaternionfArray[3];
                CubeUtil.cubeletStatus[nArray[2]] = quaternionfArray[0];
                CubeUtil.cubeletStatus[nArray[3]] = quaternionfArray[7];
                CubeUtil.cubeletStatus[nArray[4]] = quaternionfArray[4];
                CubeUtil.cubeletStatus[nArray[5]] = quaternionfArray[1];
                CubeUtil.cubeletStatus[nArray[6]] = quaternionfArray[8];
                CubeUtil.cubeletStatus[nArray[7]] = quaternionfArray[5];
                CubeUtil.cubeletStatus[nArray[8]] = quaternionfArray[2];
                int[] nArray2 = CubeUtil.cubeSideTransforms[this.rotatingSide];
                for (int i = -1; i < 2; ++i) {
                    for (int j = -1; j < 2; ++j) {
                        for (int k = -1; k < 2; ++k) {
                            if (i == 0 && j == 0 && k == 0) continue;
                            this.applyCubeletRotation(i, j, k, nArray2[0], nArray2[1], nArray2[2]);
                        }
                    }
                }
                this.rotatingSide = ThreadLocalRandom.current().nextInt(0, 6);
                this.lastTime = l;
            }
            class_45872.scale(0.4f, 0.4f, 0.4f);
            for (int i = -1; i < 2; ++i) {
                for (int j = -1; j < 2; ++j) {
                    for (int k = -1; k < 2; ++k) {
                        if (i == 0 && j == 0 && k == 0) continue;
                        this.drawCubeletStatic(class_6302, class_45872, class_45882, n, n2, i, j, k);
                    }
                }
            }
            int[] nArray = CubeUtil.cubeSideTransforms[this.rotatingSide];
            class_45872.push();
            class_45872.translate((float)nArray[0] * 0.4f, (float)nArray[1] * 0.4f, (float)nArray[2] * 0.4f);
            float f = (float)Math.toRadians(CubeUtil.easeInOutCubic((float)(l - this.lastTime) / 400.0f) * 90.0);
            float f2 = (float)((double)nArray[0] * Math.sin(f / 2.0f));
            float f3 = (float)((double)nArray[1] * Math.sin(f / 2.0f));
            float f4 = (float)((double)nArray[2] * Math.sin(f / 2.0f));
            float f5 = (float)Math.cos(f / 2.0f);
            Quaternionf quaternionf = new Quaternionf(f2, f3, f4, f5);
            class_45872.multiply(quaternionf);
            for (int i = -1; i < 2; ++i) {
                for (int j = -1; j < 2; ++j) {
                    for (int k = -1; k < 2; ++k) {
                        if (i == 0 && j == 0 && k == 0) continue;
                        this.drawCubeletRotating(class_6302, class_45872, class_45882, n, n2, i, j, k);
                    }
                }
            }
            class_45872.pop();
        } else {
            class_45872.push();
            class_45872.scale(0.875f, 0.875f, 0.875f);
            class_6302.render(class_45872, class_45882, n, n2);
            class_45872.pop();
        }
    }

    private void drawCubeletStatic(ModelPart class_6302, MatrixStack class_45872, VertexConsumer class_45882, int n, int n3, int n4, int n5, int n6) {
        int n7 = CubeUtil.cubletLookup[n4 + 1][n5 + 1][n6 + 1];
        if (Arrays.stream(CubeUtil.cubeSides[this.rotatingSide]).anyMatch(n2 -> n2 == n7)) {
            return;
        }
        this.drawCubelet(class_6302, class_45872, class_45882, n, n3, n4, n5, n6, n7);
    }

    private void drawCubeletRotating(ModelPart class_6302, MatrixStack class_45872, VertexConsumer class_45882, int n, int n3, int n4, int n5, int n6) {
        int n7 = CubeUtil.cubletLookup[n4 + 1][n5 + 1][n6 + 1];
        if (Arrays.stream(CubeUtil.cubeSides[this.rotatingSide]).noneMatch(n2 -> n2 == n7)) {
            return;
        }
        int[] nArray = CubeUtil.cubeSideTransforms[this.rotatingSide];
        this.drawCubelet(class_6302, class_45872, class_45882, n, n3, n4 - nArray[0], n5 - nArray[1], n6 - nArray[2], n7);
    }

    private void applyCubeletRotation(int n, int n3, int n4, int n5, int n6, int n7) {
        int n8 = CubeUtil.cubletLookup[n + 1][n3 + 1][n4 + 1];
        if (Arrays.stream(CubeUtil.cubeSides[this.rotatingSide]).noneMatch(n2 -> n2 == n8)) {
            return;
        }
        float f = (float)Math.toRadians(90.0);
        float f2 = (float)((double)n5 * Math.sin(f / 2.0f));
        float f3 = (float)((double)n6 * Math.sin(f / 2.0f));
        float f4 = (float)((double)n7 * Math.sin(f / 2.0f));
        float f5 = (float)Math.cos(f / 2.0f);
        Quaternionf quaternionf = new Quaternionf(f2, f3, f4, f5);
        quaternionf.mul((Quaternionfc)CubeUtil.cubeletStatus[n8]);
        CubeUtil.cubeletStatus[n8] = quaternionf;
    }

    private void drawCubelet(ModelPart class_6302, MatrixStack class_45872, VertexConsumer class_45882, int n, int n2, int n3, int n4, int n5, int n6) {
        class_45872.push();
        class_45872.translate((float)n3 * 0.4f, (float)n4 * 0.4f, (float)n5 * 0.4f);
        class_45872.push();
        class_45872.multiply(CubeUtil.cubeletStatus[n6]);
        class_45872.scale(0.8f, 0.8f, 0.8f);
        class_6302.render(class_45872, class_45882, n, n2);
        class_45872.pop();
        class_45872.pop();
    }
}

