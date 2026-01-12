package dev.iLnv_09.mod.modules.settings;

import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;

public class CombatSetting
        extends Module {
    public static CombatSetting INSTANCE;
    public final EnumSetting<Page> page = this.add(new EnumSetting<Page>("Page", Page.Combat));
    public final BooleanSetting lowVersion = this.add(new BooleanSetting("1.12", false, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting raytracebypass = this.add(new BooleanSetting("RayTraceBypass", false, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting incRotate = this.add(new BooleanSetting("IncreasesRotate", true, () -> this.page.getValue() == Page.Combat));
    public final SliderSetting incStrike = this.add(new SliderSetting("IncStrike", 90.0, 0.0, 180.0, 1.0, () -> this.page.getValue() == Page.Combat).setSuffix("Int > Track"));
    public final EnumSetting<RotateMode> rotateManager = this.add(new EnumSetting<RotateMode>("RotateMode", RotateMode.Angle, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting rotateSync = this.add(new BooleanSetting("RotateSync", true, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting rotations = this.add(new BooleanSetting("ShowRotations", true, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting attackRotate = this.add(new BooleanSetting("AttackRotate", false, () -> this.page.getValue() == Page.Combat));
    public final EnumSetting<Placement> placement = this.add(new EnumSetting<Placement>("Placement", Placement.Vanilla, () -> this.page.getValue() == Page.Combat));
    public final SliderSetting rotateTime = this.add(new SliderSetting("RotateTime", 0.5, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Combat));
    public final SliderSetting maxrotateTime = this.add(new SliderSetting("YawStepTime", 500, 0, 1000, () -> this.page.getValue() == Page.Combat));
    public final SliderSetting attackDelay = this.add(new SliderSetting("AttackDelay", 0.2, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Combat));
    public final SliderSetting tp = this.add(new SliderSetting("TP", 50.0, 0.0, 300.0, 0.01, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting test = this.add(new BooleanSetting("Test", true, () -> this.page.getValue() == Page.Combat));
    public final SliderSetting boxSize = this.add(new SliderSetting("BoxSize", 0.6, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Combat));
    public final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<SwingSide>("SwingMode", SwingSide.Server, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting obsMode = this.add(new BooleanSetting("OBSServer", false, () -> this.page.getValue() == Page.Combat));
    public final BooleanSetting injblock = this.add(new BooleanSetting("InjectBlockRotate", true, () -> this.page.getValue() == Page.Inject));
    public final BooleanSetting injectSync = this.add(new BooleanSetting("InjectSync", false, () -> this.page.getValue() == Page.Inject));
    public final SliderSetting normalstep = this.add(new SliderSetting("Normal", 0.6, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Inject).setSuffix("/+YawSpeed"));
    public final SliderSetting injectstep = this.add(new SliderSetting("Injects", 0.6, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Inject).setSuffix("/+YawSpeed"));
    public final BooleanSetting yaw = this.add(new BooleanSetting("Yaw", true, () -> this.page.getValue() == Page.Inject));
    public final BooleanSetting pitch = this.add(new BooleanSetting("Pitch", true, () -> this.page.getValue() == Page.Inject));
    public final BooleanSetting syncpacket = this.add(new BooleanSetting("RotatePacket", true, () -> this.page.getValue() == Page.OffTrack));
    public final EnumSetting<RotateType> syncType = this.add(new EnumSetting<RotateType>("RotateType", RotateType.ChangesLook, () -> this.page.getValue() == Page.OffTrack));
    public final SliderSetting offstep = this.add(new SliderSetting("OffTarck", 0.3, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.OffTrack).setSuffix("/+YawSpeed"));
    public final BooleanSetting random = this.add(new BooleanSetting("RandomPitch", false, () -> this.page.getValue() == Page.OffTrack));
    public final SliderSetting lastYaw = this.add(new SliderSetting("LastYaw", 180.0, 0.1f, 360.0, 0.01f, () -> this.page.getValue() == Page.OffTrack));
    public final SliderSetting lastPitch = this.add(new SliderSetting("LastPitch", 180.0, 0.1f, 360.0, 0.01f, () -> this.page.getValue() == Page.OffTrack));
    public final BooleanSetting invSwapBypass = this.add(new BooleanSetting("InvSwapBypass", true, () -> this.page.getValue() == Page.InventStory));
    public final BooleanSetting inventorySync = this.add(new BooleanSetting("InventorySync", false, () -> this.page.getValue() == Page.InventStory));
    public final BooleanSetting checkArmor = this.add(new BooleanSetting("checkArmor", true, () -> this.page.getValue() == Page.InventStory));

    public CombatSetting() {
        super("CombatSetting", Category.Client);
        INSTANCE = this;
    }

    public CombatSetting(String name, Category category) {
        super(name, category);
    }

    public static double getOffset() {
        if (INSTANCE != null) {
            return CombatSetting.INSTANCE.boxSize.getValue() / 2.0;
        }
        return 0.3;
    }

    @Override
    public void enable() {
        this.state = true;
    }

    @Override
    public void disable() {
        this.state = false;
    }

    @Override
    public boolean isOn() {
        return this.state;
    }

    /*
     * Exception performing whole class analysis ignored.
     */
    public enum RotateType {
        ChangesLook,
        LastRotate

    }

    /*
     * Exception performing whole class analysis ignored.
     */
    public enum Page {
        Combat,
        OffTrack,
        Inject,
        Priorid,
        InventStory

    }

    /*
     * Exception performing whole class analysis ignored.
     */
    public enum RotateMode {
        Angle,
        Vec3d
    }
}