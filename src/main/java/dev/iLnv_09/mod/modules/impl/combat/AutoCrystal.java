package dev.iLnv_09.mod.modules.impl.combat;

import com.mojang.authlib.GameProfile;
import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.LookAtEvent;
import dev.iLnv_09.api.events.impl.PacketEvent;
import dev.iLnv_09.api.events.impl.Render3DEvent;
import dev.iLnv_09.api.events.impl.UpdateWalkingPlayerEvent;
import dev.iLnv_09.api.utils.combat.CombatUtil;
import dev.iLnv_09.api.utils.combat.ExplosionUtil;
import dev.iLnv_09.api.utils.combat.MeteorExplosionUtil;
import dev.iLnv_09.api.utils.combat.OyveyExplosionUtil;
import dev.iLnv_09.api.utils.combat.MioExplosionUtil;
import dev.iLnv_09.api.utils.combat.ThunderExplosionUtil;
import dev.iLnv_09.api.utils.entity.EntityUtil;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.math.*;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.render.ColorUtil;
import dev.iLnv_09.api.utils.render.JelloUtil;
import dev.iLnv_09.api.utils.render.Render3DUtil;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.asm.accessors.IEntity;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.client.AntiCheat;
import dev.iLnv_09.mod.modules.impl.client.ClientSetting;
import dev.iLnv_09.mod.modules.impl.exploit.Blink;
import dev.iLnv_09.mod.modules.impl.player.PacketMine;
import dev.iLnv_09.mod.modules.impl.render.ExplosionSpawn;
import dev.iLnv_09.mod.modules.settings.SwingSide;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.ColorSetting;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static dev.iLnv_09.api.utils.world.BlockUtil.getBlock;
import static dev.iLnv_09.api.utils.world.BlockUtil.hasCrystal;

public class AutoCrystal extends Module {
    public static AutoCrystal INSTANCE;
    public static BlockPos crystalPos;
    public final Timer lastBreakTimer = new Timer();
    private final Timer placeTimer = new Timer(), noPosTimer = new Timer(), switchTimer = new Timer(), calcDelay = new Timer();
    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    // 放置敲击算法模式选择
    private final EnumSetting<PlacementAlgorithm> placementAlgorithm = add(new EnumSetting<>("Algorithm", PlacementAlgorithm.iLnv_09, () -> page.getValue() == Page.General));

    // General settings
    private final BooleanSetting preferAnchor = add(new BooleanSetting("PreferAnchor", true, () -> page.getValue() == Page.General));
    private final BooleanSetting breakOnlyHasCrystal = add(new BooleanSetting("OnlyHold", true, () -> page.getValue() == Page.General));
    private final EnumSetting<SwingSide> swingMode = add(new EnumSetting<>("Swing", SwingSide.Server, () -> page.getValue() == Page.General));
    private final BooleanSetting eatingPause = add(new BooleanSetting("EatingPause", true, () -> page.getValue() == Page.General));
    private final SliderSetting switchCooldown = add(new SliderSetting("SwitchPause", 100, 0, 1000, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting targetRange = add(new SliderSetting("TargetRange", 12.0, 0.0, 20.0, () -> page.getValue() == Page.General).setSuffix("m"));
    private final SliderSetting updateDelay = add(new SliderSetting("UpdateDelay", 50, 0, 1000, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting wallRange = add(new SliderSetting("WallRange", 6.0, 0.0, 6.0, () -> page.getValue() == Page.General).setSuffix("m"));
    private final BooleanSetting webSync = add(new BooleanSetting("WebSync", true, () -> page.getValue() == Page.General));
    private final SliderSetting webSyncHealth = add(new SliderSetting("WebSyncHealth", 8.0, 0.0, 20.0, () -> page.getValue() == Page.General && webSync.getValue()).setSuffix("hp"));
    private final BooleanSetting webSyncBreak = add(new BooleanSetting("WebSyncBreak", true, () -> page.getValue() == Page.General && webSync.getValue()));
    private final SliderSetting webSyncDistance = add(new SliderSetting("WebSyncDistance", 3.0, 1.0, 6.0, () -> page.getValue() == Page.General && webSync.getValue()).setSuffix("m"));

    // Rotation settings
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotation).setParent());
    private final BooleanSetting onBreak = add(new BooleanSetting("OnBreak", false, () -> rotate.isOpen() && page.getValue() == Page.Rotation));
    private final SliderSetting yOffset = add(new SliderSetting("YOffset", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && onBreak.getValue() && page.getValue() == Page.Rotation));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, () -> rotate.isOpen() && page.getValue() == Page.Rotation));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotation));
    private final BooleanSetting checkFov = add(new BooleanSetting("OnlyLooking", true, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotation));
    private final SliderSetting fov = add(new SliderSetting("Fov", 30, 0, 50, () -> rotate.isOpen() && yawStep.getValue() && checkFov.getValue() && page.getValue() == Page.Rotation));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10, 0, 100, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotation));

    // Place settings - 所有模式共用iLnv_09的AutoSwap设置
    private final SliderSetting autoMinDamage = add(new SliderSetting("PistonMin", 5.0, 0.0, 36.0, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final SliderSetting minDamage = add(new SliderSetting("Min", 5.0, 0.0, 36.0, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final SliderSetting maxSelf = add(new SliderSetting("Self", 12.0, 0.0, 36.0, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final SliderSetting range = add(new SliderSetting("Range", 5.0, 0.0, 6, () -> page.getValue() == Page.Interact).setSuffix("m"));
    private final SliderSetting noSuicide = add(new SliderSetting("NoSuicide", 3.0, 0.0, 10.0, () -> page.getValue() == Page.Interact).setSuffix("hp"));
    private final BooleanSetting smart = add(new BooleanSetting("Smart", true, () -> page.getValue() == Page.Interact));
    private final BooleanSetting place = add(new BooleanSetting("Place", true,() -> page.getValue() == Page.Interact));
    private final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 300, 0, 1000, () -> page.getValue() == Page.Interact && place.isOpen())).setSuffix("ms");
    private final EnumSetting<SwapMode> autoSwap = add(new EnumSetting<>("AutoSwap", SwapMode.Off, () -> page.getValue() == Page.Interact && place.isOpen()));
    private final BooleanSetting afterBreak = add(new BooleanSetting("AfterBreak", true, () -> page.getValue() == Page.Interact && place.isOpen()));
    private final BooleanSetting breakSetting = add(new BooleanSetting("Break", true,() -> page.getValue() == Page.Interact));
    private final SliderSetting breakDelay = add(new SliderSetting("BreakDelay", 300, 0, 1000, () -> page.getValue() == Page.Interact && breakSetting.isOpen()).setSuffix("ms"));
    private final SliderSetting minAge = add(new SliderSetting("MinAge", 0, 0, 20, () -> page.getValue() == Page.Interact && breakSetting.isOpen()).setSuffix("tick"));
    private final BooleanSetting breakRemove = add(new BooleanSetting("Remove", false, () -> page.getValue() == Page.Interact && breakSetting.isOpen()));
    private final BooleanSetting onlyTick = add(new BooleanSetting("OnlyTick", true, () -> page.getValue() == Page.Interact));

    // Render settings
    private final EnumSetting<RenderMode> renderMode = add(new EnumSetting<>("RenderMode", RenderMode.iLnv_09, () -> page.getValue() == Page.Render));

    private final ColorSetting text = add(new ColorSetting("Text", new Color(-1), () -> page.getValue() == Page.Render && renderMode.getValue() != RenderMode.Moon).injectBoolean(true));
    private final BooleanSetting render = add(new BooleanSetting("Render", true, () -> page.getValue() == Page.Render));
    private final BooleanSetting sync = add(new BooleanSetting("Sync", true, () -> page.getValue() == Page.Render && render.getValue()));
    private final BooleanSetting shrink = add(new BooleanSetting("Shrinking", true, () -> page.getValue() == Page.Render && render.getValue()));
    private final ColorSetting box = add(new ColorSetting("Outline", new Color(255, 255, 255, 255), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.iLnv_09).injectBoolean(true));
    private final SliderSetting lineWidth = add(new SliderSetting("LineWidth", 1.5d, 0.01d, 3d, 0.01, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.iLnv_09));
    private final ColorSetting fill = add(new ColorSetting("Box", new Color(255, 255, 255, 100), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.iLnv_09).injectBoolean(true));
    private final SliderSetting sliderSpeed = add(new SliderSetting("SliderSpeed", 1, 0.01, 1, 0.01, () -> page.getValue() == Page.Render && render.getValue()));
    private final SliderSetting startFadeTime = add(new SliderSetting("StartFade", 0.3d, 0d, 2d, 0.01, () -> page.getValue() == Page.Render && render.getValue()).setSuffix("s"));
    private final SliderSetting fadeSpeed = add(new SliderSetting("FadeSpeed", 1d, 0.01d, 1d, 0.01, () -> page.getValue() == Page.Render && render.getValue()));
    private final EnumSetting<TargetESP> mode = add(new EnumSetting<>("TargetESP", TargetESP.None, () -> page.getValue() == Page.Render));
    private final ColorSetting color = add(new ColorSetting("TargetColor", new Color(255, 255, 255, 50), () -> page.getValue() == Page.Render));
    private final ColorSetting hitColor = add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> page.getValue() == Page.Render));
    public final SliderSetting animationTime = add(new SliderSetting("AnimationTime", 200, 0, 2000, 1, () -> page.getValue() == Page.Render && mode.is(TargetESP.Box)));
    public final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> page.getValue() == Page.Render && mode.is(TargetESP.Box)));
    private final BooleanSetting upBox = add(new BooleanSetting("UpBox", true, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon));
    private final SliderSetting upBoxHeight = add(new SliderSetting("UpBoxHeight", 0.1, 0.1, 2.0, 0.1, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue()));
    private final ColorSetting upBoxOutline = add(new ColorSetting("UpBoxOutline", new Color(255, 255, 255, 255), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue()).injectBoolean(true));
    private final ColorSetting upBoxFill = add(new ColorSetting("UpBoxFill", new Color(255, 255, 255, 50), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue()).injectBoolean(true));
    private final SliderSetting upBoxLineWidth = add(new SliderSetting("UpBoxLineWidth", 1.5d, 0.01d, 3d, 0.01, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue()));

    // UPTEXT render settings - 只在Moon模式下显示
    private final BooleanSetting upText = add(new BooleanSetting("UpText", true, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon));
    private final SliderSetting upTextHeight = add(new SliderSetting("UpTextHeight", 0.2, 0.5, 3.0, 0.1, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upText.getValue()));
    private final ColorSetting upTextColor = add(new ColorSetting("UpTextColor", new Color(255, 255, 255, 255), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upText.getValue()).injectBoolean(true));
    private final BooleanSetting damageColor = add(new BooleanSetting("DamageColor", true, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon));

    // Calculation settings - 添加CalcMode
    private final BooleanSetting thread = add(new BooleanSetting("Thread", true, () -> page.getValue() == Page.Calc));
    private final BooleanSetting doCrystal = add(new BooleanSetting("ThreadInteract", false, () -> page.getValue() == Page.Calc));
    private final BooleanSetting lite = add(new BooleanSetting("LessCPU", false, () -> page.getValue() == Page.Calc));
    private final EnumSetting<CalcMode> calcMode = add(new EnumSetting<>("CalcMode", CalcMode.OyVey, () -> page.getValue() == Page.Calc));
    private final SliderSetting predictTicks = add(new SliderSetting("Predict", 4, 0, 10,() -> page.getValue() == Page.Calc)).setSuffix("ticks");
    private final BooleanSetting terrainIgnore = add(new BooleanSetting("TerrainIgnore", true, () -> page.getValue() == Page.Calc));

    // Misc settings
    private final BooleanSetting ignoreMine = add(new BooleanSetting("IgnoreMine", true, () -> page.getValue() == Page.Misc).setParent());
    private final SliderSetting constantProgress = add(new SliderSetting("Progress", 90.0, 0.0, 100.0, () -> page.getValue() == Page.Misc && ignoreMine.isOpen()).setSuffix("%"));
    private final BooleanSetting antiSurround = add(new BooleanSetting("AntiSurround", false, () -> page.getValue() == Page.Misc).setParent());
    private final SliderSetting antiSurroundMax = add(new SliderSetting("WhenLower", 5.0, 0.0, 36.0, () -> page.getValue() == Page.Misc && antiSurround.isOpen()).setSuffix("dmg"));
    private final BooleanSetting slowPlace = add(new BooleanSetting("Timeout", true, () -> page.getValue() == Page.Misc).setParent());
    private final SliderSetting slowDelay = add(new SliderSetting("TimeoutDelay", 600, 0, 2000, () -> page.getValue() == Page.Misc && slowPlace.isOpen()).setSuffix("ms"));
    private final SliderSetting slowMinDamage = add(new SliderSetting("TimeoutMin", 1.5, 0.0, 36.0, () -> page.getValue() == Page.Misc && slowPlace.isOpen()).setSuffix("dmg"));
    private final BooleanSetting forcePlace = add(new BooleanSetting("ForcePlace", true, () -> page.getValue() == Page.Misc).setParent());
    private final SliderSetting forceMaxHealth = add(new SliderSetting("LowerThan", 7, 0, 36, () -> page.getValue() == Page.Misc && forcePlace.isOpen()).setSuffix("health"));
    private final SliderSetting forceMin = add(new SliderSetting("ForceMin", 1.5, 0.0, 36.0, () -> page.getValue() == Page.Misc && forcePlace.isOpen()).setSuffix("dmg"));
    private final BooleanSetting armorBreaker = add(new BooleanSetting("ArmorBreaker", true, () -> page.getValue() == Page.Misc).setParent());
    private final SliderSetting maxDurable = add(new SliderSetting("MaxDurable", 8, 0, 100, () -> page.getValue() == Page.Misc && armorBreaker.isOpen()).setSuffix("%"));
    private final SliderSetting armorBreakerDamage = add(new SliderSetting("BreakerMin", 3.0, 0.0, 36.0, () -> page.getValue() == Page.Misc && armorBreaker.isOpen()).setSuffix("dmg"));
    private final SliderSetting hurtTime = add(new SliderSetting("HurtTime", 10, 0, 10, 1, () -> page.getValue() == Page.Misc));
    private final SliderSetting waitHurt = add(new SliderSetting("WaitHurt", 10, 0, 10, 1, () -> page.getValue() == Page.Misc));
    private final SliderSetting syncTimeout = add(new SliderSetting("WaitTimeOut", 500, 0, 2000, 10, () -> page.getValue() == Page.Misc));
    private final BooleanSetting forceWeb = add(new BooleanSetting("ForceWeb", true, () -> page.getValue() == Page.Misc).setParent());
    public final BooleanSetting airPlace = add(new BooleanSetting("AirPlace", false, () -> page.getValue() == Page.Misc && forceWeb.isOpen()));
    public final BooleanSetting replace = add(new BooleanSetting("Replace", false, () -> page.getValue() == Page.Misc && forceWeb.isOpen()));

    public PlayerEntity displayTarget;
    private final Animation animation = new Animation();
    public float breakDamage, tempDamage, lastDamage;
    public Vec3d directionVec = null;
    double currentFade = 0;
    private BlockPos tempPos, breakPos, syncPos;
    private Vec3d placeVec3d, curVec3d;
    private final Timer syncTimer = new Timer();

    // 各模式专用计时器
    private final Timer ripplePlaceTimer = new Timer();
    private final Timer rippleBreakTimer = new Timer();

    // 多线程相关字段
    private final ExecutorService calculationExecutor = Executors.newSingleThreadExecutor();
    private final AtomicReference<CalculationResult> currentResult = new AtomicReference<>();
    private volatile boolean calculationInProgress = false;
    private final Object calculationLock = new Object();

    // 放置算法枚举
    public enum PlacementAlgorithm {
        iLnv_09,
        Ripple
    }

    // 计算模式枚举 - 从第一个文件整合过来
    public enum CalcMode {
        Meteor,
        Thunder,
        OyVey,
        Edit,
        Mio
    }

    private static class CalculationResult {
        public final BlockPos crystalPos;
        public final BlockPos breakPos;
        public final PlayerEntity displayTarget;
        public final float tempDamage;
        public final long timestamp;

        public CalculationResult(BlockPos crystalPos, BlockPos breakPos, PlayerEntity displayTarget, float tempDamage) {
            this.crystalPos = crystalPos;
            this.breakPos = breakPos;
            this.displayTarget = displayTarget;
            this.tempDamage = tempDamage;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public enum TargetESP {
        Box,
        Jello,
        None
    }

    public enum RenderMode {
        iLnv_09, Moon
    }

    public enum SwapMode {
        Off, Normal, Silent, Inventory
    }

    public AutoCrystal() {
        super("AutoCrystal", Category.Combat);
        setChinese("水晶光环");
        INSTANCE = this;
        iLnv_09.EVENT_BUS.subscribe(new CrystalRender());
    }

    public static boolean canSee(Vec3d from, Vec3d to) {
        if (mc.world == null || mc.player == null) return false;
        HitResult result = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    DecimalFormat df = new DecimalFormat("0.0");
    @Override
    public String getInfo() {
        if (displayTarget != null && lastDamage > 0) {
            return displayTarget.getName().getString() + ", " + df.format(lastDamage);
        }
        return null;
    }

    @Override
    public void onDisable() {
        crystalPos = null;
        tempPos = null;
        breakPos = null;
        displayTarget = null;
        currentResult.set(null);
        calculationInProgress = false;
    }

    @Override
    public void onEnable() {
        crystalPos = null;
        tempPos = null;
        breakPos = null;
        displayTarget = null;
        syncTimer.reset();
        lastBreakTimer.reset();
        currentResult.set(null);
        calculationInProgress = false;
        ripplePlaceTimer.reset();
        rippleBreakTimer.reset();
    }

    @Override
    public void onThread() {
        if (thread.getValue()) {
            startCalculationTask();
            updateFromCalculationResult();
        }
    }

    @Override
    public void onUpdate() {
        if (!thread.getValue()) {
            startCalculationTask();
        }
        updateFromCalculationResult();
        if (!onlyTick.getValue()) {
            doInteract();
        }
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (!thread.getValue()) {
            startCalculationTask();
        }
        updateFromCalculationResult();
        if (!onlyTick.getValue()) {
            doInteract();
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!thread.getValue()) {
            startCalculationTask();
        }
        updateFromCalculationResult();
        if (!onlyTick.getValue()) {
            doInteract();
        }
        if (displayTarget != null && !noPosTimer.passedMs(500)) {
            doRender(matrixStack, mc.getTickDelta(), displayTarget, mode.getValue());
        }
    }

    public void doRender(MatrixStack matrixStack, float partialTicks, Entity entity, TargetESP mode) {
        if (entity == null) return;
        switch (mode) {
            case Box -> {
                Box box = ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks)
                )).expand(0, 0.1, 0);
                Render3DUtil.draw3DBox(matrixStack, box, ColorUtil.fadeColor(color.getValue(), hitColor.getValue(), animation.get(0, animationTime.getValueInt(), ease.getValue())), false, true);
            }
            case Jello -> JelloUtil.drawJello(matrixStack, entity, color.getValue());
        }
    }

    // ==================== 主交互逻辑 ====================
    private void doInteract() {
        if (shouldReturn()) {
            return;
        }

        // 根据选择的算法执行不同的交互逻辑
        switch (placementAlgorithm.getValue()) {
            case iLnv_09:
                doiLnv_09Interact();
                break;
            case Ripple:
                doRippleInteract();
                break;
        }
    }

    // ==================== iLnv_09 算法逻辑 ====================
    private void doiLnv_09Interact() {
        if (breakPos != null) {
            doBreak(breakPos);
            breakPos = null;
        }

        if (crystalPos != null && !shouldBlockCrystalPlace()) {
            doCrystal(crystalPos);
        }
    }

    private void doiLnv_09Break(BlockPos pos) {
        noPosTimer.reset();
        if (!breakSetting.getValue()) return;
        if (displayTarget != null && displayTarget.hurtTime > waitHurt.getValueInt() && !syncTimer.passed(syncTimeout.getValue())) {
            return;
        }
        lastBreakTimer.reset();
        if (!switchTimer.passedMs((long) switchCooldown.getValue())) {
            return;
        }
        syncTimer.reset();

        for (EndCrystalEntity entity : BlockUtil.getEndCrystals(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1))) {
            if (entity.age < minAge.getValueInt()) continue;
            if (rotate.getValue() && onBreak.getValue()) {
                if (!faceVector(entity.getPos().add(0, yOffset.getValue(), 0))) return;
            }
            if (!CombatUtil.breakTimer.passedMs((long) breakDelay.getValue())) return;

            animation.to = 1;
            animation.from = 1;
            CombatUtil.breakTimer.reset();
            syncPos = pos;
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            mc.player.resetLastAttackedTicks();
            EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());

            if (breakRemove.getValue()) {
                mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
            }

            if (webSync.getValue() && displayTarget != null) {
                triggerWebSync(displayTarget, entity.getPos());
            }

            if (crystalPos != null && displayTarget != null && lastDamage >= getDamage(displayTarget) && afterBreak.getValue()) {
                if (!yawStep.getValue() || !checkFov.getValue() || iLnv_09.ROTATION.inFov(entity.getPos(), fov.getValueFloat())) {
                    if (!shouldBlockCrystalPlace()) {
                        doPlace(crystalPos);
                    }
                }
            }

            if (forceWeb.getValue() && AutoWeb.INSTANCE != null && AutoWeb.INSTANCE.isOn()) {
                AutoWeb.force = true;
            }

            if (rotate.getValue() && !yawStep.getValue() && AntiCheat.INSTANCE.snapBack.getValue()) {
                iLnv_09.ROTATION.snapBack();
            }
            return;
        }
    }

    private void doiLnv_09Place(BlockPos pos) {
        if (shouldBlockCrystalPlace()) {
            return;
        }

        noPosTimer.reset();
        if (!place.getValue()) return;
        if (!hasCrystalInHand() && !findCrystal()) {
            return;
        }
        if (!canTouch(pos.down())) {
            return;
        }

        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5);
        if (facing != Direction.UP && facing != Direction.DOWN) {
            vec = vec.add(0, 0.45, 0);
        }

        if (rotate.getValue()) {
            if (!faceVector(vec)) return;
        }

        if (!placeTimer.passedMs((long) placeDelay.getValue())) return;

        if (hasCrystalInHand()) {
            placeTimer.reset();
            syncPos = pos;
            placeCrystal(pos);
        } else {
            placeTimer.reset();
            syncPos = pos;
            int old = mc.player.getInventory().selectedSlot;
            int crystal = getCrystal();
            if (crystal == -1) return;
            doSwap(crystal);
            placeCrystal(pos);
            if (autoSwap.getValue() == SwapMode.Silent) {
                doSwap(old);
            } else if (autoSwap.getValue() == SwapMode.Inventory) {
                doSwap(crystal);
                EntityUtil.syncInventory();
            }
        }
    }

    // ==================== Ripple 算法逻辑 ====================
    private void doRippleInteract() {
        // Ripple模式：优先破坏，然后放置
        if (breakPos != null) {
            doRippleBreak(breakPos);
            breakPos = null;
        }

        if (crystalPos != null && !shouldBlockCrystalPlace()) {
            doRipplePlace(crystalPos);
        }
    }

    private void doRippleBreak(BlockPos pos) {
        // 使用Ripple模式自己的计时器来控制速度
        if (!rippleBreakTimer.passedMs((long) breakDelay.getValue())) return;

        // 调用iLnv_09模式的破坏逻辑，但使用Ripple的计时器
        noPosTimer.reset();
        if (!breakSetting.getValue()) return;
        if (displayTarget != null && displayTarget.hurtTime > waitHurt.getValueInt() && !syncTimer.passed(syncTimeout.getValue())) {
            return;
        }
        lastBreakTimer.reset();
        if (!switchTimer.passedMs((long) switchCooldown.getValue())) {
            return;
        }
        syncTimer.reset();

        for (EndCrystalEntity entity : BlockUtil.getEndCrystals(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1))) {
            if (entity.age < minAge.getValueInt()) continue;
            if (rotate.getValue() && onBreak.getValue()) {
                if (!faceVector(entity.getPos().add(0, yOffset.getValue(), 0))) return;
            }
            if (!CombatUtil.breakTimer.passedMs((long) breakDelay.getValue())) return;

            animation.to = 1;
            animation.from = 1;
            CombatUtil.breakTimer.reset();
            syncPos = pos;
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            mc.player.resetLastAttackedTicks();
            EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());

            // 使用Ripple模式自己的计时器重置
            rippleBreakTimer.reset();

            if (breakRemove.getValue()) {
                mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
            }

            if (webSync.getValue() && displayTarget != null) {
                triggerWebSync(displayTarget, entity.getPos());
            }

            if (crystalPos != null && displayTarget != null && lastDamage >= getDamage(displayTarget) && afterBreak.getValue()) {
                if (!yawStep.getValue() || !checkFov.getValue() || iLnv_09.ROTATION.inFov(entity.getPos(), fov.getValueFloat())) {
                    if (!shouldBlockCrystalPlace()) {
                        doRipplePlace(crystalPos);
                    }
                }
            }

            if (forceWeb.getValue() && AutoWeb.INSTANCE != null && AutoWeb.INSTANCE.isOn()) {
                AutoWeb.force = true;
            }

            if (rotate.getValue() && !yawStep.getValue() && AntiCheat.INSTANCE.snapBack.getValue()) {
                iLnv_09.ROTATION.snapBack();
            }
            return;
        }
    }
    private void doRipplePlace(BlockPos pos) {
        // 使用Ripple模式自己的计时器来控制速度
        if (!ripplePlaceTimer.passedMs((long) placeDelay.getValue())) return;

        // 调用iLnv_09模式的放置逻辑，但使用Ripple的计时器
        if (shouldBlockCrystalPlace()) {
            return;
        }

        noPosTimer.reset();
        if (!place.getValue()) return;
        if (!hasCrystalInHand() && !findCrystal()) {
            return;
        }
        if (!canTouch(pos.down())) {
            return;
        }

        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5);
        if (facing != Direction.UP && facing != Direction.DOWN) {
            vec = vec.add(0, 0.45, 0);
        }

        if (rotate.getValue()) {
            if (!faceVector(vec)) return;
        }

        if (!placeTimer.passedMs((long) placeDelay.getValue())) return;

        if (hasCrystalInHand()) {
            placeTimer.reset();
            syncPos = pos;
            placeCrystal(pos);
        } else {
            placeTimer.reset();
            syncPos = pos;
            int old = mc.player.getInventory().selectedSlot;
            int crystal = getCrystal();
            if (crystal == -1) return;
            doSwap(crystal);
            placeCrystal(pos);
            if (autoSwap.getValue() == SwapMode.Silent) {
                doSwap(old);
            } else if (autoSwap.getValue() == SwapMode.Inventory) {
                doSwap(crystal);
                EntityUtil.syncInventory();
            }
        }

        // 使用Ripple模式自己的计时器重置
        ripplePlaceTimer.reset();
    }

    // ==================== 通用方法 ====================
    private void doCrystal(BlockPos pos) {
        switch (placementAlgorithm.getValue()) {
            case iLnv_09:
                doiLnv_09Crystal(pos);
                break;
            case Ripple:
                doRippleCrystal(pos);
                break;
        }
    }

    private void doiLnv_09Crystal(BlockPos pos) {
        if (canPlaceCrystal(pos, false, false)) {
            doiLnv_09Place(pos);
        } else {
            doiLnv_09Break(pos);
        }
    }

    private void doRippleCrystal(BlockPos pos) {
        if (canPlaceCrystal(pos, false, false)) {
            doRipplePlace(pos);
        } else {
            doRippleBreak(pos);
        }
    }

    private void doBreak(BlockPos pos) {
        switch (placementAlgorithm.getValue()) {
            case iLnv_09:
                doiLnv_09Break(pos);
                break;
            case Ripple:
                doRippleBreak(pos);
                break;
        }
    }

    private void doPlace(BlockPos pos) {
        switch (placementAlgorithm.getValue()) {
            case iLnv_09:
                doiLnv_09Place(pos);
                break;
            case Ripple:
                doRipplePlace(pos);
                break;
        }
    }

    // ==================== 事件处理 ====================
    @EventHandler()
    public void onRotate(LookAtEvent event) {
        if (rotate.getValue() && yawStep.getValue() && directionVec != null && !noPosTimer.passed(1000)) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @EventHandler(priority = -199)
    public void onPacketSend(PacketEvent.Send event) {
        if (event.isCancelled()) return;
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer.reset();
        }
    }

    // ==================== 位置计算逻辑 ====================
    private void startCalculationTask() {
        if (shouldReturn() || calculationInProgress || !calcDelay.passedMs((long) updateDelay.getValue())) {
            return;
        }

        if (breakOnlyHasCrystal.getValue() && !hasCrystalInHand() && !findCrystal()) {
            lastBreakTimer.reset();
            return;
        }

        synchronized (calculationLock) {
            if (calculationInProgress) {
                return;
            }
            calculationInProgress = true;
        }

        calcDelay.reset();

        // 提交计算任务到线程池
        calculationExecutor.submit(() -> {
            try {
                CalculationResult result = calculateCrystalPositions();
                currentResult.set(result);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                synchronized (calculationLock) {
                    calculationInProgress = false;
                }
            }
        });
    }

    private void updateFromCalculationResult() {
        CalculationResult result = currentResult.getAndSet(null);
        if (result != null) {
            // 确保结果不是太旧的
            if (System.currentTimeMillis() - result.timestamp < 1000) {
                crystalPos = result.crystalPos;
                tempPos = result.crystalPos;
                breakPos = result.breakPos;
                displayTarget = result.displayTarget;
                lastDamage = result.tempDamage;
                tempDamage = result.tempDamage;

                if (doCrystal.getValue() && crystalPos != null && !shouldReturn() && !shouldBlockCrystalPlace()) {
                    doCrystal(crystalPos);
                }
            }
        }
    }

    private CalculationResult calculateCrystalPositions() {
        if (nullCheck()) {
            return new CalculationResult(null, null, null, 0);
        }

        BlockPos calculatedCrystalPos = null;
        BlockPos calculatedBreakPos = null;
        PlayerEntity calculatedDisplayTarget = null;
        float calculatedTempDamage = 0f;

        ArrayList<PlayerAndPredict> list = new ArrayList<>();
        for (PlayerEntity target : CombatUtil.getEnemies(targetRange.getValueFloat())) {
            if (target.hurtTime <= hurtTime.getValueInt()) {
                list.add(new PlayerAndPredict(target));
            }
        }

        PlayerAndPredict self = new PlayerAndPredict(mc.player);
        if (list.isEmpty()) {
            lastBreakTimer.reset();
        } else {
            // 位置搜索逻辑 - 寻找最佳放置位置
            for (BlockPos pos : BlockUtil.getSphere((float) range.getValue() + 1)) {
                if (!isValidCrystalPosition(pos)) continue;

                for (PlayerAndPredict pap : list) {
                    if (lite.getValue() && liteCheck(pos.toCenterPos().add(0, -0.5, 0), pap.predict.getPos())) {
                        continue;
                    }

                    float damage = calculateDamage(pos, pap.player, pap.predict);
                    if (calculatedCrystalPos == null || damage > calculatedTempDamage) {
                        float selfDamage = calculateDamage(pos, self.player, self.predict);

                        // 自伤检查
                        if (selfDamage > maxSelf.getValue()) continue;
                        if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                            continue;

                        // 伤害有效性检查
                        if (damage < EntityUtil.getHealth(pap.player)) {
                            if (damage < getDamage(pap.player)) continue;
                            if (smart.getValue()) {
                                if (getDamage(pap.player) <= forceMin.getValue()) {
                                    if (damage < selfDamage - 2.5) {
                                        continue;
                                    }
                                } else {
                                    if (damage < selfDamage) {
                                        continue;
                                    }
                                }
                            }
                        }

                        calculatedDisplayTarget = pap.player;
                        calculatedCrystalPos = pos;
                        calculatedTempDamage = damage;

                        // 蜘蛛网同步检查 - 在选择位置时触发
                        if (webSync.getValue() && !webSyncBreak.getValue()) {
                            triggerWebSync(pap.player, pos.toCenterPos());
                        }

                        // 重置noPosTimer以确保渲染显示
                        noPosTimer.reset();
                    }
                }
            }

            // 处理已存在的水晶 - 寻找最佳破坏位置
            calculatedBreakPos = processExistingCrystals(list, self);

            // 反包围逻辑
            if (antiSurround.getValue() && calculatedTempDamage <= antiSurroundMax.getValueFloat()) {
                BlockPos antiSurroundPos = handleAntiSurround(list, self);
                if (antiSurroundPos != null) {
                    calculatedCrystalPos = antiSurroundPos;
                    // 为反包围位置也重置计时器
                    noPosTimer.reset();
                }
            }

            // 如果没有找到合适的放置位置，但有破坏位置，确保显示目标正确
            if (calculatedCrystalPos == null && calculatedBreakPos != null && calculatedDisplayTarget == null) {
                // 从破坏位置重新计算显示目标
                for (PlayerAndPredict pap : list) {
                    for (Entity entity : mc.world.getEntities()) {
                        if (entity instanceof EndCrystalEntity crystal) {
                            if (new BlockPosX(crystal.getPos()).equals(calculatedBreakPos)) {
                                float damage = calculateDamage(crystal.getPos(), pap.player, pap.predict);
                                if (calculatedDisplayTarget == null || damage > calculatedTempDamage) {
                                    calculatedDisplayTarget = pap.player;
                                    calculatedTempDamage = damage;
                                    noPosTimer.reset();
                                }
                            }
                        }
                    }
                }
            }
        }

        return new CalculationResult(calculatedCrystalPos, calculatedBreakPos, calculatedDisplayTarget, calculatedTempDamage);
    }

    private BlockPos processExistingCrystals(ArrayList<PlayerAndPredict> list, PlayerAndPredict self) {
        BlockPos breakPosResult = null;
        float breakDamageResult = 0;
        PlayerEntity breakDisplayTarget = null;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                // 视线和距离检查
                if (!mc.player.canSee(crystal) && mc.player.getEyePos().distanceTo(crystal.getPos()) > wallRange.getValue())
                    continue;
                if (mc.player.getEyePos().distanceTo(crystal.getPos()) > range.getValue()) {
                    continue;
                }

                for (PlayerAndPredict pap : list) {
                    float damage = calculateDamage(crystal.getPos(), pap.player, pap.predict);
                    if (breakPosResult == null || damage > breakDamageResult) {
                        float selfDamage = calculateDamage(crystal.getPos(), self.player, self.predict);

                        // 自伤检查
                        if (selfDamage > maxSelf.getValue()) continue;
                        if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                            continue;

                        // 伤害有效性检查
                        if (damage < EntityUtil.getHealth(pap.player)) {
                            if (damage < getDamage(pap.player)) continue;
                            if (smart.getValue()) {
                                if (getDamage(pap.player) <= forceMin.getValue()) {
                                    if (damage < selfDamage - 2.5) {
                                        continue;
                                    }
                                } else {
                                    if (damage < selfDamage) {
                                        continue;
                                    }
                                }
                            }
                        }

                        breakPosResult = new BlockPosX(crystal.getPos());
                        breakDamageResult = damage;
                        breakDisplayTarget = pap.player;

                        // 重置计时器
                        noPosTimer.reset();
                    }
                }
            }
        }

        return breakPosResult;
    }

    private BlockPos handleAntiSurround(ArrayList<PlayerAndPredict> list, PlayerAndPredict self) {
        if (!antiSurround.getValue() || PacketMine.getBreakPos() == null ||
                PacketMine.progress < 0.9 || BlockUtil.hasEntity(PacketMine.getBreakPos(), false)) {
            return null;
        }

        for (PlayerAndPredict pap : list) {
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP) continue;
                BlockPos offsetPos = new BlockPosX(pap.player.getPos().add(0, 0.5, 0)).offset(i);
                if (offsetPos.equals(PacketMine.getBreakPos())) {
                    // 尝试主要方向
                    if (canPlaceCrystal(offsetPos.offset(i), false, false)) {
                        float selfDamage = calculateDamage(offsetPos.offset(i), self.player, self.predict);
                        if (selfDamage < maxSelf.getValue() && !(noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())) {
                            return offsetPos.offset(i);
                        }
                    }
                    // 尝试次要方向
                    for (Direction ii : Direction.values()) {
                        if (ii == Direction.DOWN || ii == i) continue;
                        if (canPlaceCrystal(offsetPos.offset(ii), false, false)) {
                            float selfDamage = calculateDamage(offsetPos.offset(ii), self.player, self.predict);
                            if (selfDamage < maxSelf.getValue() && !(noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())) {
                                return offsetPos.offset(ii);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    // ==================== 工具方法 ====================
    private boolean isValidCrystalPosition(BlockPos pos) {
        // 墙壁检查
        if (behindWall(pos)) return false;

        // 距离检查
        if (mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > range.getValue()) {
            return false;
        }

        // 可触摸检查
        if (!canTouch(pos.down())) return false;

        // 可放置检查
        return canPlaceCrystal(pos, true, false);
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        BlockPos boost2 = boost.up();

        // 基础方块检查
        if (!(getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)) {
            return false;
        }

        // 点击面检查
        if (BlockUtil.getClickSideStrict(obsPos) == null) {
            return false;
        }

        // 实体检查
        if (!noEntityBlockCrystal(boost, ignoreCrystal, ignoreItem)) {
            return false;
        }
        if (!noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem)) {
            return false;
        }

        // 空间检查
        if (!(mc.world.isAir(boost) || (hasCrystal(boost) && getBlock(boost) == Blocks.FIRE))) {
            return false;
        }

        // 低版本兼容性检查
        if (ClientSetting.INSTANCE.lowVersion.getValue() && !mc.world.isAir(boost2)) {
            return false;
        }

        return true;
    }

    private boolean liteCheck(Vec3d from, Vec3d to) {
        return !canSee(from, to) && !canSee(from, to.add(0, 1.8, 0));
    }

    private boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (!entity.isAlive() || (ignoreItem && entity instanceof ItemEntity) || (entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue()))
                continue;
            if (entity instanceof EndCrystalEntity) {
                if (!ignoreCrystal) return false;
                if (mc.player.canSee(entity) || mc.player.getEyePos().distanceTo(entity.getPos()) <= wallRange.getValue()) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    public boolean behindWall(BlockPos pos) {
        Vec3d testVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 2 * 0.85, pos.getZ() + 0.5);
        HitResult result = mc.world.raycast(new RaycastContext(EntityUtil.getEyesPos(), testVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (result == null || result.getType() == HitResult.Type.MISS) return false;
        return mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > wallRange.getValue();
    }

    private boolean canTouch(BlockPos pos) {
        Direction side = BlockUtil.getClickSideStrict(pos);
        return side != null && pos.toCenterPos().add(new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5)).distanceTo(mc.player.getEyePos()) <= range.getValue();
    }

    public float calculateDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
        return calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), player, predict);
    }

    public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        // 采矿忽略逻辑
        if (ignoreMine.getValue() && PacketMine.getBreakPos() != null) {
            if (mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()) {
                if (PacketMine.progress >= constantProgress.getValue() / 100) {
                    CombatUtil.modifyPos = PacketMine.getBreakPos();
                    CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                }
            }
        }

        // 地形忽略
        if (terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }

        // 根据CalcMode选择不同的伤害计算方法
        float damage = 0.0f;
        switch (calcMode.getValue()) {
            case Meteor:
                damage = (float) MeteorExplosionUtil.explosionDamage(player, pos, predict, 6.0f);
                break;
            case Thunder:
                damage = ThunderExplosionUtil.calculateDamage(pos, player, predict, 6.0f);
                break;
            case OyVey:
                damage = OyveyExplosionUtil.calculateDamage(pos.x, pos.y, pos.z, player, predict, 6.0f);
                break;
            case Edit:
                damage = ExplosionUtil.calculateDamage(pos.x, pos.y, pos.z, player, predict, 6.0f);
                break;
            case Mio:
                damage = MioExplosionUtil.calculateDamage(pos, player, predict, 6.0f);
                break;
        }

        // 重置修改状态
        CombatUtil.modifyPos = null;
        CombatUtil.terrainIgnore = false;

        return damage;
    }

    private float getDamage(PlayerEntity target) {
        // 慢速放置模式
        if (!PacketMine.INSTANCE.obsidian.isPressed() && slowPlace.getValue() && lastBreakTimer.passedMs((long) slowDelay.getValue()) && !PistonCrystal.INSTANCE.isOn()) {
            return slowMinDamage.getValueFloat();
        }

        // 强制放置模式
        if (forcePlace.getValue() && EntityUtil.getHealth(target) <= forceMaxHealth.getValue() && !PacketMine.INSTANCE.obsidian.isPressed() && !PistonCrystal.INSTANCE.isOn()) {
            return forceMin.getValueFloat();
        }

        // 护甲破坏模式
        if (armorBreaker.getValue()) {
            DefaultedList<ItemStack> armors = target.getInventory().armor;
            for (ItemStack armor : armors) {
                if (armor.isEmpty()) continue;
                if (EntityUtil.getDamagePercent(armor) > maxDurable.getValue()) continue;
                return armorBreakerDamage.getValueFloat();
            }
        }

        // 活塞水晶模式
        if (PistonCrystal.INSTANCE.isOn()) {
            return autoMinDamage.getValueFloat();
        }

        // 默认最小伤害
        return minDamage.getValueFloat();
    }

    public boolean findCrystal() {
        if (autoSwap.getValue() == SwapMode.Off) return false;
        return getCrystal() != -1;
    }

    private boolean hasCrystalInHand() {
        return mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) ||
                mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL);
    }

    private void placeCrystal(BlockPos pos) {
        if (ExplosionSpawn.INSTANCE != null) {
            ExplosionSpawn.INSTANCE.add(pos);
        }
        boolean offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        BlockUtil.clickBlock(obsPos, facing, false, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, swingMode.getValue());
    }

    private boolean faceVector(Vec3d directionVec) {
        if (!yawStep.getValue()) {
            iLnv_09.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.directionVec = directionVec;
            if (iLnv_09.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }

    private void doSwap(int slot) {
        if (autoSwap.getValue() == SwapMode.Silent || autoSwap.getValue() == SwapMode.Normal) {
            InventoryUtil.switchToSlot(slot);
        } else if (autoSwap.getValue() == SwapMode.Inventory) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }

    private int getCrystal() {
        if (autoSwap.getValue() == SwapMode.Silent || autoSwap.getValue() == SwapMode.Normal) {
            return InventoryUtil.findItem(Items.END_CRYSTAL);
        } else if (autoSwap.getValue() == SwapMode.Inventory) {
            return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        }
        return -1;
    }

    private boolean shouldReturn() {
        if (mc.player == null) return true;
        if (eatingPause.getValue() && mc.player.isUsingItem() || (Blink.INSTANCE != null && Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue())) {
            lastBreakTimer.reset();
            return true;
        }
        if (preferAnchor.getValue() && AutoAnchor.INSTANCE != null && AutoAnchor.INSTANCE.currentPos != null) {
            lastBreakTimer.reset();
            return true;
        }
        return false;
    }

    private boolean shouldBlockCrystalPlace() {
        return false;
    }

    // 蜘蛛网同步触发方法
    private void triggerWebSync(PlayerEntity target, Vec3d crystalPos) {
        if (!webSync.getValue() || AutoWeb.INSTANCE == null || !AutoWeb.INSTANCE.isOn()) {
            return;
        }

        // 检查目标血量条件
        float targetHealth = EntityUtil.getHealth(target);
        if (targetHealth > webSyncHealth.getValue()) {
            return;
        }

        // 检查距离条件
        double distance = target.getPos().distanceTo(crystalPos);
        if (distance > webSyncDistance.getValue()) {
            return;
        }

        // 检查是否应该触发（基于破坏设置）
        if (!webSyncBreak.getValue() && !lastBreakTimer.passedMs(500)) {
            return;
        }

        // 触发蜘蛛网强制模式
        AutoWeb.force = true;

        // 重置计时器避免频繁触发
        lastBreakTimer.reset();
    }

    // ==================== 内部类 ====================
    private enum Page {
        General, Interact, Misc, Rotation, Calc, Render
    }

    private class PlayerAndPredict {
        final PlayerEntity player;
        final PlayerEntity predict;

        private PlayerAndPredict(PlayerEntity player) {
            this.player = player;
            // 使用统一的预测ticks
            int predictTicksValue = predictTicks.getValueInt();

            if (predictTicksValue > 0) {
                predict = new PlayerEntity(mc.world, player.getBlockPos(), player.getYaw(), new GameProfile(UUID.fromString("66123666-1234-5432-6666-667563866600"), "PredictEntity339")) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }

                    @Override
                    public boolean isOnGround() {
                        return player.isOnGround();
                    }
                };
                predict.setPosition(player.getPos().add(CombatUtil.getMotionVec(player, predictTicksValue, true)));
                predict.setHealth(player.getHealth());
                predict.prevX = player.prevX;
                predict.prevZ = player.prevZ;
                predict.prevY = player.prevY;
                predict.setOnGround(player.isOnGround());
                predict.getInventory().clone(player.getInventory());
                predict.setPose(player.getPose());
                for (StatusEffectInstance se : new ArrayList<>(player.getStatusEffects())) {
                    predict.addStatusEffect(se);
                }
            } else {
                predict = player;
            }
        }
    }

    private class CrystalRender {
        @EventHandler
        public void onRender3D(Render3DEvent event) {
            try {
                // 安全检查
                if (mc.world == null || mc.player == null || !render.getValue()) {
                    return;
                }

                // 获取要渲染的位置
                BlockPos renderPos = getRenderPosition();
                if (renderPos == null) {
                    curVec3d = null;
                    return;
                }

                // 设置基础位置向量
                placeVec3d = renderPos.down().toCenterPos();
                if (placeVec3d == null) {
                    return;
                }

                // 计算淡入淡出效果
                calculateFadeEffect();

                if (currentFade == 0) {
                    curVec3d = null;
                    return;
                }

                // 计算平滑移动位置
                calculateSmoothPosition();

                // 根据渲染模式执行不同的渲染
                switch (renderMode.getValue()) {
                    case iLnv_09:
                        renderiLnv_09Mode(event.getMatrixStack(), renderPos);
                        break;
                    case Moon:
                        renderMoonMode(event.getMatrixStack(), renderPos);
                        break;
                }

                // 文本渲染（所有模式都支持）
                renderText(renderPos);

            } catch (Exception e) {
                System.err.println("CrystalRender error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private BlockPos getRenderPosition() {
            try {
                // 优先使用同步位置
                if (sync.getValue() && syncPos != null) {
                    return syncPos;
                }
                // 最后使用计算位置
                return crystalPos;
            } catch (Exception e) {
                return null;
            }
        }

        private void calculateFadeEffect() {
            try {
                boolean shouldFadeOut = noPosTimer.passedMs((long) (startFadeTime.getValue() * 1000));
                double targetFade = shouldFadeOut ? 0 : 0.5;

                if (fadeSpeed.getValue() >= 1) {
                    currentFade = targetFade;
                } else {
                    currentFade = AnimateUtil.animate(currentFade, targetFade, fadeSpeed.getValue() / 10);
                }
            } catch (Exception e) {
                currentFade = 0;
            }
        }

        private void calculateSmoothPosition() {
            try {
                if (curVec3d == null || sliderSpeed.getValue() >= 1) {
                    curVec3d = placeVec3d;
                } else {
                    curVec3d = new Vec3d(
                            AnimateUtil.animate(curVec3d.x, placeVec3d.x, sliderSpeed.getValue() / 10),
                            AnimateUtil.animate(curVec3d.y, placeVec3d.y, sliderSpeed.getValue() / 10),
                            AnimateUtil.animate(curVec3d.z, placeVec3d.z, sliderSpeed.getValue() / 10)
                    );
                }
            } catch (Exception e) {
                curVec3d = placeVec3d;
            }
        }

        private void renderiLnv_09Mode(MatrixStack matrixStack, BlockPos renderPos) {
            try {
                if (matrixStack == null || curVec3d == null) return;

                Box renderBox = createRenderBox();
                if (renderBox == null) return;

                // iLnv_09 模式：显示原本的 box 和 outline
                if (fill != null && fill.booleanValue && fill.getValue() != null) {
                    Color fillColor = ColorUtil.injectAlpha(fill.getValue(),
                            (int) (fill.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawFill(matrixStack, renderBox, fillColor);
                }

                if (box != null && box.booleanValue && box.getValue() != null) {
                    Color boxColor = ColorUtil.injectAlpha(box.getValue(),
                            (int) (box.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawBox(matrixStack, renderBox, boxColor, lineWidth.getValueFloat());
                }

            } catch (Exception e) {
                System.err.println("renderiLnv_09Mode error: " + e.getMessage());
            }
        }

        private void renderMoonMode(MatrixStack matrixStack, BlockPos renderPos) {
            try {
                if (matrixStack == null) return;

                // Moon 模式：只显示 UpBox，不显示基础渲染
                renderUpBox(matrixStack, renderPos);

            } catch (Exception e) {
                System.err.println("renderMoonMode error: " + e.getMessage());
            }
        }

        private Box createRenderBox() {
            try {
                if (curVec3d == null) return null;

                Box box = new Box(curVec3d, curVec3d);
                if (shrink.getValue()) {
                    return box.expand(currentFade);
                } else {
                    return box.expand(0.5);
                }
            } catch (Exception e) {
                return null;
            }
        }

        private void renderUpBox(MatrixStack matrixStack, BlockPos renderPos) {
            try {
                if (!upBox.getValue() || renderPos == null) return;

                // 安全检查
                if (upBoxFill == null || upBoxOutline == null) return;

                double upBoxHeightValue = upBoxHeight.getValue();
                Vec3d upBoxPos = calculateUpBoxPosition(renderPos);

                Box upBoxBox = new Box(
                        upBoxPos.x, upBoxPos.y, upBoxPos.z,
                        upBoxPos.x + 1, upBoxPos.y + upBoxHeightValue, upBoxPos.z + 1
                );

                // UpBox 填充
                if (upBoxFill.booleanValue && upBoxFill.getValue() != null) {
                    Color fillColor = ColorUtil.injectAlpha(upBoxFill.getValue(),
                            (int) (upBoxFill.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawFill(matrixStack, upBoxBox, fillColor);
                }

                // UpBox 边框
                if (upBoxOutline.booleanValue && upBoxOutline.getValue() != null) {
                    Color outlineColor = ColorUtil.injectAlpha(upBoxOutline.getValue(),
                            (int) (upBoxOutline.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawBox(matrixStack, upBoxBox, outlineColor, upBoxLineWidth.getValueFloat());
                }

            } catch (Exception e) {
                System.err.println("renderUpBox error: " + e.getMessage());
            }
        }

        private Vec3d calculateUpBoxPosition(BlockPos renderPos) {
            try {
                if (sliderSpeed.getValue() >= 1 || curVec3d == null) {
                    return new Vec3d(renderPos.getX(), renderPos.getY(), renderPos.getZ());
                } else {
                    return new Vec3d(
                            AnimateUtil.animate(curVec3d.x - 0.5, renderPos.getX(), sliderSpeed.getValue() / 10),
                            renderPos.getY(),
                            AnimateUtil.animate(curVec3d.z - 0.5, renderPos.getZ(), sliderSpeed.getValue() / 10)
                    );
                }
            } catch (Exception e) {
                return new Vec3d(renderPos.getX(), renderPos.getY(), renderPos.getZ());
            }
        }

        private void renderText(BlockPos renderPos) {
            try {
                if (lastDamage <= 0 || renderPos == null) return;

                switch (renderMode.getValue()) {
                    case iLnv_09:
                        renderiLnv_09Text();
                        break;
                    case Moon:
                        renderMoonText(renderPos);
                        break;
                }

            } catch (Exception e) {
                System.err.println("renderText error: " + e.getMessage());
            }
        }

        private void renderiLnv_09Text() {
            try {
                if (curVec3d == null || text == null || !text.booleanValue || text.getValue() == null) return;

                Color textColor = ColorUtil.injectAlpha(
                        text.getValue(),
                        (int) (text.getValue().getAlpha() * currentFade * 2D)
                );
                String damageText = df.format(lastDamage);
                Render3DUtil.drawText3D(damageText, curVec3d, textColor);
            } catch (Exception e) {
                System.err.println("renderiLnv_09Text error: " + e.getMessage());
            }
        }

        private void renderMoonText(BlockPos renderPos) {
            try {
                if (!upText.getValue() || upTextColor == null || upTextColor.getValue() == null) return;

                Vec3d textPos = calculateTextPosition(renderPos);
                Color textColor = calculateTextColor();

                String damageText = df.format(lastDamage);
                Render3DUtil.drawText3D(damageText, textPos, textColor);
            } catch (Exception e) {
                System.err.println("renderMoonText error: " + e.getMessage());
            }
        }

        private Vec3d calculateTextPosition(BlockPos renderPos) {
            try {
                double textHeight = upTextHeight.getValue();
                if (sliderSpeed.getValue() >= 1 || curVec3d == null) {
                    return new Vec3d(
                            renderPos.getX() + 0.5,
                            renderPos.getY() + textHeight,
                            renderPos.getZ() + 0.5
                    );
                } else {
                    return new Vec3d(
                            AnimateUtil.animate(curVec3d.x, renderPos.getX() + 0.5, sliderSpeed.getValue() / 10),
                            renderPos.getY() + textHeight,
                            AnimateUtil.animate(curVec3d.z, renderPos.getZ() + 0.5, sliderSpeed.getValue() / 10)
                    );
                }
            } catch (Exception e) {
                return new Vec3d(
                        renderPos.getX() + 0.5,
                        renderPos.getY() + upTextHeight.getValue(),
                        renderPos.getZ() + 0.5
                );
            }
        }

        private Color calculateTextColor() {
            try {
                Color baseColor;
                if (damageColor.getValue()) {
                    baseColor = getDamageColor(lastDamage);
                } else {
                    baseColor = upTextColor.getValue();
                }

                return ColorUtil.injectAlpha(
                        baseColor,
                        (int) (upTextColor.getValue().getAlpha() * currentFade * 2D)
                );
            } catch (Exception e) {
                return new Color(255, 255, 255, (int)(255 * currentFade * 2D));
            }
        }

        private Color getDamageColor(float damage) {
            try {
                if (damage >= 18.0f) {
                    return new Color(255, 0, 0); // 红色
                } else if (damage >= 12.0f) {
                    return new Color(255, 165, 0); // 橙色
                } else if (damage >= 8.0f) {
                    return new Color(255, 255, 0); // 黄色
                } else if (damage >= 5.0f) {
                    return new Color(0, 255, 0); // 绿色
                } else {
                    return new Color(40, 216, 230); // 淡蓝色
                }
            } catch (Exception e) {
                return new Color(255, 255, 255); // 默认白色
            }
        }
    }
}