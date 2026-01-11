package dev.iLnv_09.mod.modules.impl.combat;

import com.mojang.authlib.GameProfile;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.LookAtEvent;
import dev.iLnv_09.api.events.impl.PacketEvent;
import dev.iLnv_09.api.events.impl.Render3DEvent;
import dev.iLnv_09.api.events.impl.UpdateWalkingPlayerEvent;
import dev.iLnv_09.api.utils.combat.CombatUtil;
import dev.iLnv_09.api.utils.combat.Sn0wUtil;
import dev.iLnv_09.api.utils.entity.EntityUtil;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.math.*;
import dev.iLnv_09.api.utils.render.ColorUtil;
import dev.iLnv_09.api.utils.render.JelloUtil;
import dev.iLnv_09.api.utils.render.Render3DUtil;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.asm.accessors.IEntity;
import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.client.AntiCheat;
import dev.iLnv_09.mod.modules.impl.client.ClientSetting;
import dev.iLnv_09.mod.modules.impl.exploit.Blink;
import dev.iLnv_09.mod.modules.impl.player.AutoPot;
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
import java.util.ArrayList;
import java.util.UUID;

import static dev.iLnv_09.api.utils.world.BlockUtil.getBlock;
import static dev.iLnv_09.api.utils.world.BlockUtil.hasCrystal;

public class AutoCrystal extends Module {
    public static AutoCrystal INSTANCE;
    public static BlockPos crystalPos;
    public final Timer lastBreakTimer = new Timer();
    private final Timer placeTimer = new Timer(), noPosTimer = new Timer(), switchTimer = new Timer(), calcDelay = new Timer();

    // 颜色模式枚举
    public enum ColorMode {
        Custom,
        Rainbow
    }

    // 渲染模式枚举
    public enum RenderMode {
        Alien, Moon
    }

    // 推人模式枚举
    public enum PushMode {
        Song,
        Xin
    }

    // 计算模式枚举 - 新增
    public enum CalcMode {
        Alien,        // 原有的Alien计算逻辑
        iLnv_09,      // 原有的iLnv_09计算逻辑
        Sn0w       // 新的Sn0w计算逻辑
    }

    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    // General settings
    private final BooleanSetting preferAnchor = add(new BooleanSetting("PreferAnchor", true, () -> page.getValue() == Page.General));
    private final BooleanSetting breakOnlyHasCrystal = add(new BooleanSetting("OnlyHold", true, () -> page.getValue() == Page.General));
    private final EnumSetting<SwingSide> swingMode = add(new EnumSetting<>("Swing", SwingSide.All, () -> page.getValue() == Page.General));
    private final BooleanSetting eatingPause = add(new BooleanSetting("EatingPause", true, () -> page.getValue() == Page.General));
    private final SliderSetting switchCooldown = add(new SliderSetting("SwitchPause", 100, 0, 1000, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting targetRange = add(new SliderSetting("TargetRange", 12.0, 0.0, 20.0, () -> page.getValue() == Page.General).setSuffix("m"));
    private final SliderSetting updateDelay = add(new SliderSetting("UpdateDelay", 50, 0, 1000, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting wallRange = add(new SliderSetting("WallRange", 6.0, 0.0, 6.0, () -> page.getValue() == Page.General).setSuffix("m"));

    // 新增设置
    private final BooleanSetting preferExp = add(new BooleanSetting("PreferExp", false, () -> page.getValue() == Page.General));
    private final BooleanSetting preferPot = add(new BooleanSetting("PreferPot", false, () -> page.getValue() == Page.General));
    private final BooleanSetting syncPush = add(new BooleanSetting("SyncPush", false, () -> page.getValue() == Page.General).setParent());
    private final EnumSetting<PushMode> pushMode = add(new EnumSetting<>("PushMode", PushMode.Song, () -> page.getValue() == Page.General && syncPush.isOpen()));

    // WebSync设置
    private final BooleanSetting webSync = add(new BooleanSetting("WebSync", true, () -> page.getValue() == Page.General));
    private final SliderSetting webSyncHealth = add(new SliderSetting("WebSyncHealth", 8.0, 0.0, 20.0, () -> page.getValue() == Page.General && webSync.getValue()).setSuffix("hp"));
    private final BooleanSetting webSyncBreak = add(new BooleanSetting("WebSyncBreak", true, () -> page.getValue() == Page.General && webSync.getValue()));
    private final SliderSetting webSyncDistance = add(new SliderSetting("WebSyncDistance", 3.0, 1.0, 6.0, () -> page.getValue() == Page.General && webSync.getValue()).setSuffix("m"));

    //Rotate
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotation).setParent());
    private final BooleanSetting onBreak = add(new BooleanSetting("OnBreak", false, () -> rotate.isOpen() && page.getValue() == Page.Rotation));
    private final SliderSetting yOffset = add(new SliderSetting("YOffset", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && onBreak.getValue() && page.getValue() == Page.Rotation));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, () -> rotate.isOpen() && page.getValue() == Page.Rotation));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotation));
    private final BooleanSetting checkFov = add(new BooleanSetting("OnlyLooking", true, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotation));
    private final SliderSetting fov = add(new SliderSetting("Fov", 30, 0, 50, () -> rotate.isOpen() && yawStep.getValue() && checkFov.getValue() && page.getValue() == Page.Rotation));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10, 0, 100, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotation));

    //Place
    private final SliderSetting autoMinDamage = add(new SliderSetting("PistonMin", 5.0, 0.0, 36.0, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final SliderSetting minDamage = add(new SliderSetting("Min", 5.0, 0.0, 36.0, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final SliderSetting maxSelf = add(new SliderSetting("Self", 12.0, 0.0, 36.0, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final SliderSetting range = add(new SliderSetting("Range", 5.0, 0.0, 6, () -> page.getValue() == Page.Interact).setSuffix("m"));
    private final SliderSetting noSuicide = add(new SliderSetting("NoSuicide", 3.0, 0.0, 10.0, () -> page.getValue() == Page.Interact).setSuffix("hp"));
    private final BooleanSetting smart = add(new BooleanSetting("Smart", true, () -> page.getValue() == Page.Interact));
    private final BooleanSetting place = add(new BooleanSetting("Place", true, () -> page.getValue() == Page.Interact).setParent());
    private final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 300, 0, 1000, () -> page.getValue() == Page.Interact && place.isOpen()).setSuffix("ms"));
    private final EnumSetting<SwapMode> autoSwap = add(new EnumSetting<>("AutoSwap", SwapMode.Off, () -> page.getValue() == Page.Interact && place.isOpen()));
    private final BooleanSetting afterBreak = add(new BooleanSetting("AfterBreak", true, () -> page.getValue() == Page.Interact && place.isOpen()));
    private final BooleanSetting breakSetting = add(new BooleanSetting("Break", true, () -> page.getValue() == Page.Interact).setParent());
    private final SliderSetting breakDelay = add(new SliderSetting("BreakDelay", 300, 0, 1000, () -> page.getValue() == Page.Interact && breakSetting.isOpen()).setSuffix("ms"));
    private final SliderSetting minAge = add(new SliderSetting("MinAge", 0, 0, 20, () -> page.getValue() == Page.Interact && breakSetting.isOpen()).setSuffix("tick"));
    private final BooleanSetting breakRemove = add(new BooleanSetting("Remove", false, () -> page.getValue() == Page.Interact && breakSetting.isOpen()));
    private final BooleanSetting onlyTick = add(new BooleanSetting("OnlyTick", true, () -> page.getValue() == Page.Interact));

    //Render
    private final EnumSetting<RenderMode> renderMode = add(new EnumSetting<>("RenderMode", RenderMode.Alien, () -> page.getValue() == Page.Render));
    private final BooleanSetting textEnabled = add(new BooleanSetting("Text", true, () -> page.getValue() == Page.Render && renderMode.getValue() != RenderMode.Moon));
    private final ColorSetting textColor = add(new ColorSetting("TextColor", new Color(-1), () -> page.getValue() == Page.Render && textEnabled.getValue()));
    private final BooleanSetting showSelfDamage = add(new BooleanSetting("ShowSelfDamage", false, () -> page.getValue() == Page.Render && textEnabled.getValue()));
    private final BooleanSetting render = add(new BooleanSetting("Render", true, () -> page.getValue() == Page.Render));
    private final BooleanSetting sync = add(new BooleanSetting("Sync", true, () -> page.getValue() == Page.Render && render.getValue()));
    private final BooleanSetting shrink = add(new BooleanSetting("Shrinking", true, () -> page.getValue() == Page.Render && render.getValue()));
    private final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("ColorMode", ColorMode.Custom, () -> page.getValue() == Page.Render && render.getValue()));
    private final SliderSetting rainbowSpeed = add(new SliderSetting("RainbowSpeed", 4, 1, 10, 0.1, () -> page.getValue() == Page.Render && render.getValue() && colorMode.getValue() == ColorMode.Rainbow));
    private final SliderSetting saturation = add(new SliderSetting("Saturation", 130.0f, 1.0f, 255.0f, () -> page.getValue() == Page.Render && render.getValue() && colorMode.getValue() == ColorMode.Rainbow));
    private final SliderSetting rainbowDelay = add(new SliderSetting("Delay", 350, 0, 1000, () -> page.getValue() == Page.Render && render.getValue() && colorMode.getValue() == ColorMode.Rainbow));
    private final ColorSetting box = add(new ColorSetting("Outline", new Color(255, 255, 255, 255), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Alien && colorMode.getValue() != ColorMode.Rainbow).injectBoolean(true));
    private final SliderSetting lineWidth = add(new SliderSetting("LineWidth", 1.5d, 0.01d, 3d, 0.01, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Alien));
    private final ColorSetting fill = add(new ColorSetting("Box", new Color(255, 255, 255, 100), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Alien && colorMode.getValue() != ColorMode.Rainbow).injectBoolean(true));
    private final SliderSetting sliderSpeed = add(new SliderSetting("SliderSpeed", 0.2, 0.01, 1, 0.01, () -> page.getValue() == Page.Render && render.getValue()));
    private final SliderSetting startFadeTime = add(new SliderSetting("StartFade", 0.3d, 0d, 2d, 0.01, () -> page.getValue() == Page.Render && render.getValue()).setSuffix("s"));
    private final SliderSetting fadeSpeed = add(new SliderSetting("FadeSpeed", 0.2d, 0.01d, 1d, 0.01, () -> page.getValue() == Page.Render && render.getValue()));
    private final EnumSetting<TargetESP> mode = add(new EnumSetting<>("TargetESP", TargetESP.Jello, () -> page.getValue() == Page.Render));
    private final ColorSetting color = add(new ColorSetting("TargetColor", new Color(255, 255, 255, 50), () -> page.getValue() == Page.Render));
    private final ColorSetting hitColor = add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> page.getValue() == Page.Render));
    public final SliderSetting animationTime = add(new SliderSetting("AnimationTime", 200, 0, 2000, 1, () -> page.getValue() == Page.Render && mode.is(TargetESP.Box)));
    public final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> page.getValue() == Page.Render && mode.is(TargetESP.Box)));
    private final BooleanSetting upBox = add(new BooleanSetting("UpBox", true, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon));
    private final SliderSetting upBoxHeight = add(new SliderSetting("UpBoxHeight", 0.1, 0.1, 2.0, 0.1, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue()));
    private final ColorSetting upBoxOutline = add(new ColorSetting("UpBoxOutline", new Color(255, 255, 255, 255), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue() && colorMode.getValue() != ColorMode.Rainbow).injectBoolean(true));
    private final ColorSetting upBoxFill = add(new ColorSetting("UpBoxFill", new Color(255, 255, 255, 50), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue() && colorMode.getValue() != ColorMode.Rainbow).injectBoolean(true));
    private final SliderSetting upBoxLineWidth = add(new SliderSetting("UpBoxLineWidth", 1.5d, 0.01d, 3d, 0.01, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upBox.getValue()));
    private final BooleanSetting upText = add(new BooleanSetting("UpText", true, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon));
    private final SliderSetting upTextHeight = add(new SliderSetting("UpTextHeight", 0.2, 0.5, 3.0, 0.1, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upText.getValue()));
    private final ColorSetting upTextColor = add(new ColorSetting("UpTextColor", new Color(255, 255, 255, 255), () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon && upText.getValue()).injectBoolean(true));
    private final BooleanSetting damageColor = add(new BooleanSetting("DamageColor", true, () -> page.getValue() == Page.Render && render.getValue() && renderMode.getValue() == RenderMode.Moon));
    //Calc
    private final BooleanSetting thread = add(new BooleanSetting("Thread", true, () -> page.getValue() == Page.Calc));
    private final BooleanSetting doCrystal = add(new BooleanSetting("ThreadInteract", false, () -> page.getValue() == Page.Calc));
    private final BooleanSetting lite = add(new BooleanSetting("LessCPU", false, () -> page.getValue() == Page.Calc));
    private final EnumSetting<CalcMode> calcMode = add(new EnumSetting<>("CalcMode", CalcMode.iLnv_09, () -> page.getValue() == Page.Calc)); // 新增的CalcMode设置
    private final SliderSetting predictTicks = add(new SliderSetting("Predict", 4, 0, 10, () -> page.getValue() == Page.Calc).setSuffix("ticks"));
    private final BooleanSetting terrainIgnore = add(new BooleanSetting("TerrainIgnore", true, () -> page.getValue() == Page.Calc));

    //Misc
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

    private final DecimalFormat damageFormat = new DecimalFormat("0.0");
    private final Timer fadeTimer = new Timer();
    private double textAlpha = 0.0;
    private Vec3d lastTextPos = null;

    public enum TargetESP {
        Box,
        Jello,
        None
    }

    public AutoCrystal() {
        super("AutoCrystal", Category.Combat);
        setChinese("自动水晶");
        INSTANCE = this;
        iLnv_09.EVENT_BUS.subscribe(new CrystalRender());
    }

    public static boolean canSee(Vec3d from, Vec3d to) {
        HitResult result = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    DecimalFormat df = new DecimalFormat("0.0");

    @Override
    public String getInfo() {
        if (displayTarget != null && lastDamage > 0) {
            return df.format(lastDamage);
        }
        return null;
    }

    @Override
    public void onDisable() {
        crystalPos = null;
        tempPos = null;
        breakPos = null;
        displayTarget = null;
        textAlpha = 0.0;
        lastTextPos = null;
    }

    @Override
    public void onEnable() {
        crystalPos = null;
        tempPos = null;
        breakPos = null;
        displayTarget = null;
        syncTimer.reset();
        lastBreakTimer.reset();
        textAlpha = 0.0;
        lastTextPos = null;
    }

    @Override
    public void onThread() {
        if (thread.getValue()) {
            updateCrystalPos();
        }
    }

    @Override
    public void onUpdate() {
        if (!thread.getValue()) {
            updateCrystalPos();
        }

        // 检查优先级：PreferPot -> PreferExp -> SyncPush
        if (preferPot.getValue() && AutoPot.INSTANCE != null && AutoPot.INSTANCE.isOn()) {
            return;
        }

        if (preferExp.getValue() && AutoEXP.INSTANCE != null && AutoEXP.INSTANCE.isOn()) {
            return;
        }

        if (syncPush.getValue() && Autopiston.INSTANCE != null && Autopiston.INSTANCE.isOn()) {
            handleSyncPush();
            return;
        }

        doInteract();
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (!thread.getValue()) updateCrystalPos();

        if (preferPot.getValue() && AutoPot.INSTANCE != null && AutoPot.INSTANCE.isOn()) {
            return;
        }

        if (preferExp.getValue() && AutoEXP.INSTANCE != null && AutoEXP.INSTANCE.isOn()) {
            return;
        }

        if (syncPush.getValue() && Autopiston.INSTANCE != null && Autopiston.INSTANCE.isOn()) {
            handleSyncPush();
            return;
        }

        if (!onlyTick.getValue()) doInteract();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!thread.getValue()) updateCrystalPos();

        if (preferPot.getValue() && AutoPot.INSTANCE != null && AutoPot.INSTANCE.isOn()) {
            return;
        }

        if (preferExp.getValue() && AutoEXP.INSTANCE != null && AutoEXP.INSTANCE.isOn()) {
            return;
        }

        if (syncPush.getValue() && Autopiston.INSTANCE != null && Autopiston.INSTANCE.isOn()) {
            handleSyncPush();
            return;
        }

        if (!onlyTick.getValue()) doInteract();

        if (displayTarget != null && !noPosTimer.passedMs(500)) {
            doRender(matrixStack, mc.getTickDelta(), displayTarget, mode.getValue());
        }
    }

    public void doRender(MatrixStack matrixStack, float partialTicks, Entity entity, TargetESP mode) {
        switch (mode) {
            case Box -> Render3DUtil.draw3DBox(matrixStack, ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks))).expand(0, 0.1, 0), ColorUtil.fadeColor(color.getValue(), hitColor.getValue(), animation.get(0, animationTime.getValueInt(), ease.getValue())), false, true);
            case Jello -> JelloUtil.drawJello(matrixStack, entity, color.getValue());
        }
    }

    private void doInteract() {
        if (shouldReturn()) {
            return;
        }

        // 根据CalcMode选择交互逻辑
        switch (calcMode.getValue()) {
            case iLnv_09:
                doiLnv_09Interact();
                break;
            case Alien:
                doAlienInteract();
                break;
            case Sn0w:
                doSn0wInteract();  // 添加Sn0w交互
                break;
        }
    }
    // Sn0w交互逻辑
    private void doSn0wInteract() {
        if (breakPos != null) {
            doBreak(breakPos);
            breakPos = null;
        }

        if (crystalPos != null) {
            doCrystal(crystalPos);
        }
    }
    // iLnv_09交互逻辑
    private void doiLnv_09Interact() {
        if (breakPos != null) {
            doBreak(breakPos);
            breakPos = null;
        }

        if (crystalPos != null) {
            doCrystal(crystalPos);
        }
    }

    // Alien交互逻辑 - 从1.java中整合
    private void doAlienInteract() {
        if (breakPos != null) {
            doBreak(breakPos);
            breakPos = null;
        }

        if (crystalPos != null) {
            doCrystal(crystalPos);
        }
    }

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

    // 处理同步推人
    private void handleSyncPush() {
        if (Autopiston.INSTANCE == null || !Autopiston.INSTANCE.isOn()) {
            return;
        }

        if (displayTarget == null) {
            return;
        }

        float targetHealth = EntityUtil.getHealth(displayTarget);
        boolean shouldPush = false;

        switch (pushMode.getValue()) {
            case Song:
                shouldPush = targetHealth <= 25.0f;
                break;

            case Xin:
                shouldPush = (targetHealth >= 23.0f && targetHealth <= 36.0f) ||
                        (targetHealth >= 0.0f && targetHealth <= 11.0f);
                break;
        }

        if (shouldPush) {
            Autopiston.dopush = true;
        }
    }

    private void updateCrystalPos() {
        getCrystalPos();
        lastDamage = tempDamage;
        crystalPos = tempPos;
    }

    private boolean shouldReturn() {
        if (eatingPause.getValue() && mc.player.isUsingItem() || (Blink.INSTANCE != null && Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue())) {
            lastBreakTimer.reset();
            return true;
        }
        if (preferAnchor.getValue() && AutoAnchor.INSTANCE != null && AutoAnchor.INSTANCE.currentPos != null) {
            lastBreakTimer.reset();
            return true;
        }

        if (preferPot.getValue() && AutoPot.INSTANCE != null && AutoPot.INSTANCE.isOn()) {
            lastBreakTimer.reset();
            return true;
        }

        if (preferExp.getValue() && AutoEXP.INSTANCE != null && AutoEXP.INSTANCE.isOn()) {
            lastBreakTimer.reset();
            return true;
        }

        if (syncPush.getValue() && Autopiston.INSTANCE != null && Autopiston.INSTANCE.isOn()) {
            lastBreakTimer.reset();
            return true;
        }

        return false;
    }

    private void getCrystalPos() {
        if (nullCheck()) {
            lastBreakTimer.reset();
            tempPos = null;
            return;
        }
        if (!calcDelay.passedMs((long) updateDelay.getValue())) return;
        if (breakOnlyHasCrystal.getValue() && !mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL) && !findCrystal()) {
            lastBreakTimer.reset();
            tempPos = null;
            return;
        }
        boolean shouldReturn = shouldReturn();
        calcDelay.reset();
        breakPos = null;
        breakDamage = 0;
        tempPos = null;
        tempDamage = 0f;
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
            // 根据计算模式选择位置计算逻辑
            switch (calcMode.getValue()) {
                case iLnv_09:
                    calculatePositionsiLnv_09(list, self);
                    break;
                case Alien:
                    calculatePositionsAlien(list, self);
                    break;
                case Sn0w:
                    calculatePositionsSn0w(list, self);  // 添加Sn0w分支
                    break;
            }

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity crystal) {
                    if (!mc.player.canSee(crystal) && mc.player.getEyePos().distanceTo(crystal.getPos()) > wallRange.getValue())
                        continue;
                    if (mc.player.getEyePos().distanceTo(crystal.getPos()) > range.getValue()) {
                        continue;
                    }
                    for (PlayerAndPredict pap : list) {
                        float damage = calculateDamage(crystal.getPos(), pap.player, pap.predict);
                        if (breakPos == null || damage > breakDamage) {
                            float selfDamage = calculateDamage(crystal.getPos(), self.player, self.predict);
                            if (selfDamage > maxSelf.getValue()) continue;
                            if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                                continue;
                            if (damage < EntityUtil.getHealth(pap.player)) {
                                if (damage < getDamage(pap.player)) continue;
                                if (smart.getValue()) {
                                    if (getDamage(pap.player) == forceMin.getValue()) {
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
                            breakPos = new BlockPosX(crystal.getPos());
                            if (damage > tempDamage) {
                                displayTarget = pap.player;
                            }
                        }
                    }
                }
            }

            if (doCrystal.getValue() && breakPos != null && !shouldReturn) {
                doBreak(breakPos);
                breakPos = null;
            }

            // 抗包围逻辑
            if (antiSurround.getValue() && PacketMine.getBreakPos() != null && PacketMine.progress >= 0.9 && !BlockUtil.hasEntity(PacketMine.getBreakPos(), false)) {
                if (tempDamage <= antiSurroundMax.getValueFloat()) {
                    handleAntiSurround(list, self);
                }
            }
        }

        if (doCrystal.getValue() && tempPos != null && !shouldReturn) {
            doCrystal(tempPos);
        }
    }

    // iLnv_09位置计算逻辑 - 原有的iLnv_09计算逻辑
    private void calculatePositionsiLnv_09(ArrayList<PlayerAndPredict> list, PlayerAndPredict self) {
        for (BlockPos pos : BlockUtil.getSphere((float) range.getValue() + 1)) {
            if (behindWall(pos)) continue;
            if (mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > range.getValue()) {
                continue;
            }
            if (!canTouch(pos.down())) continue;
            if (!canPlaceCrystal(pos, true, false)) continue;
            for (PlayerAndPredict pap : list) {
                if (lite.getValue() && liteCheck(pos.toCenterPos().add(0, -0.5, 0), pap.predict.getPos())) {
                    continue;
                }
                float damage = calculateDamage(pos, pap.player, pap.predict);
                if (tempPos == null || damage > tempDamage) {
                    float selfDamage = calculateDamage(pos, self.player, self.predict);
                    if (selfDamage > maxSelf.getValue()) continue;
                    if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                        continue;
                    if (damage < EntityUtil.getHealth(pap.player)) {
                        if (damage < getDamage(pap.player)) continue;
                        if (smart.getValue()) {
                            if (getDamage(pap.player) == forceMin.getValue()) {
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
                    displayTarget = pap.player;
                    tempPos = pos;
                    tempDamage = damage;

                    if (webSync.getValue() && !webSyncBreak.getValue()) {
                        triggerWebSync(pap.player, pos.toCenterPos());
                    }
                }
            }
        }
    }

    // Alien位置计算逻辑 - 从1.java中整合
    private void calculatePositionsAlien(ArrayList<PlayerAndPredict> list, PlayerAndPredict self) {
        for (BlockPos pos : BlockUtil.getSphere((float) range.getValue() + 1)) {
            if (!isValidPositionAlien(pos)) continue;

            for (PlayerAndPredict pap : list) {
                float damage = calculateDamageAlien(pos.toCenterPos(), pap.player, pap.predict);
                if (tempPos == null || damage > tempDamage) {
                    float selfDamage = calculateDamageAlien(pos.toCenterPos(), self.player, self.predict);
                    if (selfDamage > maxSelf.getValue()) continue;
                    if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                        continue;
                    if (damage < EntityUtil.getHealth(pap.player)) {
                        if (damage < getDamage(pap.player)) continue;
                        if (smart.getValue()) {
                            if (getDamage(pap.player) == forceMin.getValue()) {
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
                    displayTarget = pap.player;
                    tempPos = pos;
                    tempDamage = damage;

                    if (webSync.getValue() && !webSyncBreak.getValue()) {
                        triggerWebSync(pap.player, pos.toCenterPos());
                    }
                }
            }
        }
    }

    // Alien位置有效性检查
    private boolean isValidPositionAlien(BlockPos pos) {
        if (behindWall(pos)) return false;
        if (mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > range.getValue()) {
            return false;
        }
        if (!canTouch(pos.down())) return false;
        return canPlaceCrystal(pos, true, false);
    }

    private void handleAntiSurround(ArrayList<PlayerAndPredict> list, PlayerAndPredict self) {
        for (PlayerAndPredict pap : list) {
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP) continue;
                BlockPos offsetPos = new BlockPosX(pap.player.getPos().add(0, 0.5, 0)).offset(i);
                if (offsetPos.equals(PacketMine.getBreakPos())) {
                    if (canPlaceCrystal(offsetPos.offset(i), false, false)) {
                        float selfDamage = calculateDamage(offsetPos.offset(i), self.player, self.predict);
                        if (selfDamage < maxSelf.getValue() && !(noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())) {
                            tempPos = offsetPos.offset(i);
                            return;
                        }
                    }
                    for (Direction ii : Direction.values()) {
                        if (ii == Direction.DOWN || ii == i) continue;
                        if (canPlaceCrystal(offsetPos.offset(ii), false, false)) {
                            float selfDamage = calculateDamage(offsetPos.offset(ii), self.player, self.predict);
                            if (selfDamage < maxSelf.getValue() && !(noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())) {
                                tempPos = offsetPos.offset(ii);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        // 根据计算模式选择放置检查
        switch (calcMode.getValue()) {
            case iLnv_09:
                return canPlaceCrystaliLnv_09(pos, ignoreCrystal, ignoreItem);
            case Alien:
                return canPlaceCrystalAlien(pos, ignoreCrystal, ignoreItem);
            case Sn0w:
                return canPlaceCrystalSn0w(pos, ignoreCrystal, ignoreItem);  // 调用Sn0w放置检查
            default:
                return false;
        }
    }
    private boolean canPlaceCrystalSn0w(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        // 使用Sn0wUtil.canPlaceCrystalSimple
        return Sn0wUtil.canPlaceCrystalSimple(pos);
    }
    // Alien放置检查逻辑 - 从1.java中整合
    private boolean canPlaceCrystalAlien(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        // 原有的Alien放置检查逻辑
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        BlockPos boost2 = boost.up();

        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
                && BlockUtil.getClickSideStrict(obsPos) != null
                && noEntityBlockCrystal(boost, ignoreCrystal, ignoreItem)
                && noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem)
                && (mc.world.isAir(boost) || hasCrystal(boost) && getBlock(boost) == Blocks.FIRE)
                && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost2));
    }

    // iLnv_09放置检查逻辑 - 原有的iLnv_09逻辑
    private boolean canPlaceCrystaliLnv_09(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        // 如果使用Sn0w计算模式，调用新的放置检查
        if (calcMode.getValue() == CalcMode.Sn0w) {
            return Sn0wUtil.canPlaceCrystalSimple(pos);
        }

        // 原有的放置检查逻辑
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        BlockPos boost2 = boost.up();

        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
                && BlockUtil.getClickSideStrict(obsPos) != null
                && noEntityBlockCrystal(boost, ignoreCrystal, ignoreItem)
                && noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem)
                && (mc.world.isAir(boost) || hasCrystal(boost) && getBlock(boost) == Blocks.FIRE)
                && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost2));
    }

    private boolean liteCheck(Vec3d from, Vec3d to) {
        return !canSee(from, to) && !canSee(from, to.add(0, 1.8, 0));
    }

    private boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (!entity.isAlive() || ignoreItem && entity instanceof ItemEntity || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue())
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

    private void doCrystal(BlockPos pos) {
        if (canPlaceCrystal(pos, false, false)) {
            doPlace(pos);
        } else {
            doBreak(pos);
        }
    }

    public float calculateDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
        return calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), player, predict);
    }

    public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        // 根据计算模式选择伤害计算逻辑
        switch (calcMode.getValue()) {
            case iLnv_09:
                return calculateDamageiLnv_09(pos, player, predict);
            case Alien:
                return calculateDamageAlien(pos, player, predict);
            case Sn0w:
                return calculateDamageSn0w(pos, player, predict);  // 调用Sn0w计算
            default:
                return 0.0f;
        }
    }

    // iLnv_09伤害计算逻辑 - 原有的iLnv_09计算逻辑
    private float calculateDamageiLnv_09(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        if (ignoreMine.getValue() && PacketMine.getBreakPos() != null) {
            if (mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()) {
                if (PacketMine.progress >= constantProgress.getValue() / 100) {
                    CombatUtil.modifyPos = PacketMine.getBreakPos();
                    CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                }
            }
        }

        if (terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }

        float damage = ExplosionUtil.calculateDamage(pos.getX(), pos.getY(), pos.getZ(), player, predict, 6);

        CombatUtil.modifyPos = null;
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    // Alien伤害计算逻辑 - 从1.java中整合
    private float calculateDamageAlien(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        if (ignoreMine.getValue() && PacketMine.getBreakPos() != null) {
            if (mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()) {
                if (PacketMine.progress >= constantProgress.getValue() / 100) {
                    CombatUtil.modifyPos = PacketMine.getBreakPos();
                    CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                }
            }
        }

        if (terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }

        float damage = ExplosionUtil.calculateDamage(pos.getX(), pos.getY(), pos.getZ(), player, predict, 6);

        CombatUtil.modifyPos = null;
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    // Sn0w伤害计算逻辑 - 修复
    private float calculateDamageSn0w(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        // 使用Sn0wUtil.calculateDamage方法
        Entity targetEntity = (player != null) ? player : mc.player;

        // 获取挖矿忽略状态
        boolean miningIgnore = getMiningIgnore();

        // 调用Sn0wUtil.calculateDamage
        float damage = Sn0wUtil.calculateDamage(
                targetEntity,
                pos,
                terrainIgnore.getValue(),
                miningIgnore
        );

        return damage;
    }

    // 获取挖矿忽略状态（适配CatAura逻辑）
    private boolean getMiningIgnore() {
        if (ignoreMine.getValue() && PacketMine.getBreakPos() != null) {
            if (mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()) {
                return PacketMine.progress >= constantProgress.getValue() / 100;
            }
        }
        return false;
    }

    private double getDamage(PlayerEntity target) {
        if (!PacketMine.INSTANCE.obsidian.isPressed() && slowPlace.getValue() && lastBreakTimer.passedMs((long) slowDelay.getValue()) && !PistonCrystal.INSTANCE.isOn()) {
            return slowMinDamage.getValue();
        }
        if (forcePlace.getValue() && EntityUtil.getHealth(target) <= forceMaxHealth.getValue() && !PacketMine.INSTANCE.obsidian.isPressed() && !PistonCrystal.INSTANCE.isOn()) {
            return forceMin.getValue();
        }
        if (armorBreaker.getValue()) {
            DefaultedList<ItemStack> armors = target.getInventory().armor;
            for (ItemStack armor : armors) {
                if (armor.isEmpty()) continue;
                if (EntityUtil.getDamagePercent(armor) > maxDurable.getValue()) continue;
                return armorBreakerDamage.getValue();
            }
        }
        if (PistonCrystal.INSTANCE.isOn()) {
            return autoMinDamage.getValueFloat();
        }
        return minDamage.getValue();
    }

    public boolean findCrystal() {
        if (autoSwap.getValue() == SwapMode.Off) return false;
        return getCrystal() != -1;
    }

    private void doBreak(BlockPos pos) {
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

            if (webSync.getValue() && displayTarget != null && webSyncBreak.getValue()) {
                triggerWebSync(displayTarget, entity.getPos());
            }

            if (crystalPos != null && displayTarget != null && lastDamage >= getDamage(displayTarget) && afterBreak.getValue()) {
                if (!yawStep.getValue() || !checkFov.getValue() || iLnv_09.ROTATION.inFov(entity.getPos(), fov.getValueFloat())) {
                    doPlace(crystalPos);
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

    private void doPlace(BlockPos pos) {
        noPosTimer.reset();
        if (!place.getValue()) return;
        if (!mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL) && !findCrystal()) {
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
        if (mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) || mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)) {
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

    // 蜘蛛网同步触发方法
    private void triggerWebSync(PlayerEntity target, Vec3d crystalPos) {
        if (!webSync.getValue() || AutoWeb.INSTANCE == null || !AutoWeb.INSTANCE.isOn()) {
            return;
        }

        float targetHealth = EntityUtil.getHealth(target);
        if (targetHealth > webSyncHealth.getValue()) {
            return;
        }

        double distance = target.getPos().distanceTo(crystalPos);
        if (distance > webSyncDistance.getValue()) {
            return;
        }

        if (!webSyncBreak.getValue() && !lastBreakTimer.passedMs(500)) {
            return;
        }

        AutoWeb.force = true;
        lastBreakTimer.reset();
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

    private enum Page {
        General, Interact, Misc, Rotation, Calc, Render
    }

    private enum SwapMode {
        Off, Normal, Silent, Inventory
    }

    private class PlayerAndPredict {
        final PlayerEntity player;
        final PlayerEntity predict;

        private PlayerAndPredict(PlayerEntity player) {
            this.player = player;
            if (predictTicks.getValueFloat() > 0) {
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
                predict.setPosition(player.getPos().add(CombatUtil.getMotionVec(player, predictTicks.getValueInt(), true)));
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
                if (mc.world == null || mc.player == null || !render.getValue()) {
                    return;
                }

                BlockPos renderPos = getRenderPosition();
                if (renderPos == null) {
                    curVec3d = null;
                    return;
                }

                placeVec3d = renderPos.down().toCenterPos();
                if (placeVec3d == null) {
                    return;
                }

                calculateFadeEffect();

                if (currentFade == 0) {
                    curVec3d = null;
                    return;
                }

                calculateSmoothPosition();

                switch (renderMode.getValue()) {
                    case Alien:
                        renderAlienMode(event.getMatrixStack(), renderPos);
                        break;
                    case Moon:
                        renderMoonMode(event.getMatrixStack(), renderPos);
                        break;
                }

                renderDamageText(event.getMatrixStack(), renderPos);

            } catch (Exception e) {
                System.err.println("CrystalRender error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private BlockPos getRenderPosition() {
            try {
                if (sync.getValue() && syncPos != null) {
                    return syncPos;
                }
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

        private void renderDamageText(MatrixStack matrixStack, BlockPos renderPos) {
            try {
                if (!textEnabled.getValue() || lastDamage <= 0) {
                    return;
                }

                Vec3d textPos = calculateTextPosition(renderPos);
                if (textPos == null) return;

                Color textColor = calculateTextColor();
                if (textColor == null) return;

                String damageText = formatDamageText();
                if (damageText == null || damageText.isEmpty()) return;

                Render3DUtil.drawText3D(damageText, textPos, textColor);

            } catch (Exception e) {
                System.err.println("renderDamageText error: " + e.getMessage());
            }
        }

        private Vec3d calculateTextPosition(BlockPos renderPos) {
            try {
                if (renderPos == null || curVec3d == null) return null;

                if (renderMode.getValue() == RenderMode.Moon && upText.getValue()) {
                    double heightOffset = upTextHeight.getValue();
                    if (sliderSpeed.getValue() >= 1) {
                        return new Vec3d(
                                renderPos.getX() + 0.5,
                                renderPos.getY() + heightOffset,
                                renderPos.getZ() + 0.5
                        );
                    } else {
                        return new Vec3d(
                                AnimateUtil.animate(curVec3d.x, renderPos.getX() + 0.5, sliderSpeed.getValue() / 10),
                                renderPos.getY() + heightOffset,
                                AnimateUtil.animate(curVec3d.z, renderPos.getZ() + 0.5, sliderSpeed.getValue() / 10)
                        );
                    }
                } else if (renderMode.getValue() == RenderMode.Alien) {
                    return curVec3d;
                } else {
                    double heightOffset = 1.2;
                    if (sliderSpeed.getValue() >= 1) {
                        return new Vec3d(
                                renderPos.getX() + 0.5,
                                renderPos.getY() + heightOffset,
                                renderPos.getZ() + 0.5
                        );
                    } else {
                        return new Vec3d(
                                AnimateUtil.animate(curVec3d.x, renderPos.getX() + 0.5, sliderSpeed.getValue() / 10),
                                renderPos.getY() + heightOffset,
                                AnimateUtil.animate(curVec3d.z, renderPos.getZ() + 0.5, sliderSpeed.getValue() / 10)
                        );
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        private Color calculateTextColor() {
            try {
                Color baseColor;

                if (renderMode.getValue() == RenderMode.Moon && damageColor.getValue()) {
                    baseColor = getDamageColor(lastDamage);
                } else {
                    baseColor = textColor != null ? textColor.getValue() : new Color(255, 255, 255);
                }

                return ColorUtil.injectAlpha(
                        baseColor,
                        (int) (baseColor.getAlpha() * currentFade * 2D)
                );
            } catch (Exception e) {
                return new Color(255, 255, 255, (int)(255 * currentFade * 2D));
            }
        }

        private Color getDamageColor(float damage) {
            try {
                if (damage >= 18.0f) {
                    return new Color(255, 0, 0);
                } else if (damage >= 12.0f) {
                    return new Color(255, 165, 0);
                } else if (damage >= 8.0f) {
                    return new Color(255, 255, 0);
                } else if (damage >= 5.0f) {
                    return new Color(0, 255, 0);
                } else {
                    return new Color(40, 216, 230);
                }
            } catch (Exception e) {
                return new Color(255, 255, 255);
            }
        }

        private String formatDamageText() {
            try {
                String damageText = df.format(lastDamage);
                if (showSelfDamage.getValue()) {
                    float selfDamage = calculateSelfDamage();
                    damageText = damageText + "/" + df.format(selfDamage);
                }
                return damageText;
            } catch (Exception e) {
                return df.format(lastDamage);
            }
        }

        private float calculateSelfDamage() {
            try {
                if (mc.player == null) return 0.0f;

                BlockPos renderPos = getRenderPosition();
                if (renderPos != null) {
                    PlayerAndPredict self = new PlayerAndPredict(mc.player);
                    return calculateDamage(renderPos, self.player, self.predict);
                }
                return 0.0f;
            } catch (Exception e) {
                return 0.0f;
            }
        }

        private void renderAlienMode(MatrixStack matrixStack, BlockPos renderPos) {
            try {
                if (matrixStack == null || curVec3d == null) return;

                Box renderBox = createRenderBox();
                if (renderBox == null) return;

                Color fillColorBase, boxColorBase;
                int counter = (int)(renderPos.getX() + renderPos.getY() + renderPos.getZ());

                if (colorMode.getValue() == ColorMode.Rainbow) {
                    fillColorBase = getRainbowColor(counter);
                    boxColorBase = getRainbowColor(counter);
                } else {
                    fillColorBase = fill != null ? fill.getValue() : new Color(255, 255, 255, 100);
                    boxColorBase = box != null ? box.getValue() : new Color(255, 255, 255, 255);
                }

                if (fill != null && fill.booleanValue && fillColorBase != null) {
                    Color fillColor = ColorUtil.injectAlpha(fillColorBase,
                            (int) (fillColorBase.getAlpha() * currentFade * 2D));
                    Render3DUtil.drawFill(matrixStack, renderBox, fillColor);
                }

                if (box != null && box.booleanValue && boxColorBase != null) {
                    Color boxColor = ColorUtil.injectAlpha(boxColorBase,
                            (int) (boxColorBase.getAlpha() * currentFade * 2D));
                    Render3DUtil.drawBox(matrixStack, renderBox, boxColor, lineWidth.getValueFloat());
                }

            } catch (Exception e) {
                System.err.println("renderAlienMode error: " + e.getMessage());
            }
        }

        private void renderMoonMode(MatrixStack matrixStack, BlockPos renderPos) {
            try {
                if (matrixStack == null) return;

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

                if (upBoxFill == null || upBoxOutline == null) return;

                double upBoxHeightValue = upBoxHeight.getValue();
                Vec3d upBoxPos = calculateUpBoxPosition(renderPos);

                Box upBoxBox = new Box(
                        upBoxPos.x, upBoxPos.y, upBoxPos.z,
                        upBoxPos.x + 1, upBoxPos.y + upBoxHeightValue, upBoxPos.z + 1
                );

                int counter = (int)(renderPos.getX() + renderPos.getY() + renderPos.getZ());
                Color fillColorBase, outlineColorBase;

                if (colorMode.getValue() == ColorMode.Rainbow) {
                    fillColorBase = getRainbowColor(counter);
                    outlineColorBase = getRainbowColor(counter);
                } else {
                    fillColorBase = upBoxFill.getValue();
                    outlineColorBase = upBoxOutline.getValue();
                }

                if (upBoxFill.booleanValue && fillColorBase != null) {
                    Color fillColor = ColorUtil.injectAlpha(fillColorBase,
                            (int) (fillColorBase.getAlpha() * currentFade * 2D));
                    Render3DUtil.drawFill(matrixStack, upBoxBox, fillColor);
                }

                if (upBoxOutline.booleanValue && outlineColorBase != null) {
                    Color outlineColor = ColorUtil.injectAlpha(outlineColorBase,
                            (int) (outlineColorBase.getAlpha() * currentFade * 2D));
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

        private Color getRainbowColor(int delay) {
            if (colorMode.getValue() == ColorMode.Rainbow) {
                double rainbowState = Math.ceil((System.currentTimeMillis() * rainbowSpeed.getValue() + delay * rainbowDelay.getValue()) / 20.0);
                return Color.getHSBColor((float) (rainbowState % 360.0 / 360), saturation.getValueFloat() / 255.0f, 1.0f);
            }
            return Color.WHITE;
        }
    }
    // Sn0w位置计算逻辑
    private void calculatePositionsSn0w(ArrayList<PlayerAndPredict> list, PlayerAndPredict self) {
        // 使用Sn0w特有的位置计算逻辑
        for (BlockPos pos : BlockUtil.getSphere((float) range.getValue() + 1)) {
            if (behindWall(pos)) continue;
            if (mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > range.getValue()) {
                continue;
            }
            if (!canTouch(pos.down())) continue;
            if (!canPlaceCrystalSn0w(pos, true, false)) continue;  // 使用Sn0w放置检查

            for (PlayerAndPredict pap : list) {
                if (lite.getValue() && liteCheck(pos.toCenterPos().add(0, -0.5, 0), pap.predict.getPos())) {
                    continue;
                }

                float damage = calculateDamageSn0w(pos.toCenterPos().add(0, -0.5, 0), pap.player, pap.predict);  // 使用Sn0w伤害计算

                if (tempPos == null || damage > tempDamage) {
                    float selfDamage = calculateDamageSn0w(pos.toCenterPos(), self.player, self.predict);  // 使用Sn0w伤害计算

                    if (selfDamage > maxSelf.getValue()) continue;
                    if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                        continue;

                    if (damage < EntityUtil.getHealth(pap.player)) {
                        if (damage < getDamage(pap.player)) continue;
                        if (smart.getValue()) {
                            if (getDamage(pap.player) == forceMin.getValue()) {
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

                    displayTarget = pap.player;
                    tempPos = pos;
                    tempDamage = damage;

                    if (webSync.getValue() && !webSyncBreak.getValue()) {
                        triggerWebSync(pap.player, pos.toCenterPos());
                    }
                }
            }
        }
    }
}