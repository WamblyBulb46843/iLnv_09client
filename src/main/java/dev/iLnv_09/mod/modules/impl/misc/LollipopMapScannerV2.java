package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.api.utils.math.Timer;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.EquipmentSlot;

public class LollipopMapScannerV2 extends Module {

    private enum State {
        IDLE,
        STARTING_ELYTRA,
        SCANNING,
        TURNING
    }

    // General Settings
    private final SliderSetting blockDistance = add(new SliderSetting("BlockDistance", 16, 1, 100, 1));
    private final SliderSetting initialDirection = add(new SliderSetting("InitialDirection", 0, 0, 3, 1)); // 0: E, 1: S, 2: W, 3: N

    // Flight Control
    private final BooleanSetting autoStartElytra = add(new BooleanSetting("AutoStartElytra", true));
    private final BooleanSetting autoForward = add(new BooleanSetting("AutoForward", true));
    private final BooleanSetting keepPitchLevel = add(new BooleanSetting("KeepPitchLevel", true));
    private final SliderSetting rotationSpeed = add(new SliderSetting("RotationSpeed", 10, 1, 20, 1));
    private final BooleanSetting avoidObstacles = add(new BooleanSetting("AvoidObstacles", true));

    // Rocket Settings
    private final BooleanSetting useRockets = add(new BooleanSetting("UseRockets", true));
    private final SliderSetting minSpeed = add(new SliderSetting("MinSpeed", 1.5, 0.5, 3.0, 0.1));

    // HUD
    private final BooleanSetting showHud = add(new BooleanSetting("ShowHUD", true));

    private static final float[] YAW_ANGLES = {270f, 0f, 90f, 180f}; // E, S, W, N

    private State currentState = State.IDLE;
    private Vec3d startPos = null;
    private Vec3d lastTurnPos = null;
    private int currentDirection = 0; // 0: E, 1: S, 2: W, 3: N
    private int legLength = 1;
    private int legsCompleted = 0;
    private float targetYaw; // New variable to store the target yaw for smooth turning
    private final Timer rocketTimer = new Timer();
    private final Timer avoidTimer = new Timer();
    private boolean obstacleDetected = false;

    public LollipopMapScannerV2() {
        super("LollipopMapScannerV2", Category.Misc);
        setChinese("棒棒糖扫图V2");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            if (!isWearingElytra()) {
                disable();
                return;
            }

            startPos = mc.player.getPos();
            lastTurnPos = startPos;
            currentState = State.STARTING_ELYTRA;
            currentDirection = (int) initialDirection.getValue();
            legLength = 1;
            legsCompleted = 0;
            obstacleDetected = false;

            targetYaw = YAW_ANGLES[currentDirection];

            if (Math.abs(getAngleDifference(mc.player.getYaw(), targetYaw)) > 1.0f) {
                currentState = State.TURNING; // Start with turning if not facing the initial direction
            } else {
                currentState = State.STARTING_ELYTRA;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentState = State.IDLE;
        startPos = null;
        if (mc.player != null && mc.player.input != null) {
            mc.player.input.movementForward = 0;
        }
        obstacleDetected = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        switch (currentState) {
            case IDLE:
                // Do nothing
                break;
            case STARTING_ELYTRA:
                handleElytraStart();
                break;
            case SCANNING:
                handleScanning();
                break;
            case TURNING:
                handleTurning();
                break;
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (mc.player == null || !showHud.getValue()) return;

        String statusStr;
        switch (currentState) {
            case SCANNING:
                statusStr = obstacleDetected ? "AVOIDING" : "SCANNING";
                break;
            default:
                statusStr = currentState.name();
                break;
        }

        String status = "Status: " + statusStr;
        String legLen = "Leg Length: " + legLength;
        String legsDone = "Legs Done: " + legsCompleted;

        drawContext.drawText(mc.textRenderer, status, 5, 5, -1, true);
        drawContext.drawText(mc.textRenderer, legLen, 5, 15, -1, true);
        drawContext.drawText(mc.textRenderer, legsDone, 5, 25, -1, true);
    }

    private void handleElytraStart() {
        if (!autoStartElytra.getValue()) {
            currentState = State.SCANNING;
            return;
        }

        if (!isWearingElytra()) {
            disable();
            return;
        }

        if (!mc.player.isFallFlying()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            } else if (mc.player.getVelocity().y < 0) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        } else {
            currentState = State.SCANNING;
        }
    }

    private void handleScanning() {
        if (autoForward.getValue()) {
            mc.player.input.movementForward = 1.0f;
        }

        if (mc.player.isFallFlying()) {
            if (avoidObstacles.getValue()) {
                if (isObstacleAhead() && !obstacleDetected) {
                    obstacleDetected = true;
                    avoidTimer.reset();
                }

                if (obstacleDetected) {
                    mc.player.setPitch(-30); // Fly up
                    if (avoidTimer.passedMs(1000)) { // Avoid for 1 second
                        obstacleDetected = false;
                    }
                } else if (keepPitchLevel.getValue()) {
                    mc.player.setPitch(0); // Maintain horizontal flight
                }
            } else if (keepPitchLevel.getValue()) {
                mc.player.setPitch(0); // Maintain horizontal flight
            }
        }

        if (useRockets.getValue() && mc.player.isFallFlying()) {
            double speed = mc.player.getVelocity().horizontalLength();
            if (speed < minSpeed.getValue() && rocketTimer.passedMs(1000)) {
                useRocket();
            }
        }

        double distance = blockDistance.getValue() * 16 * legLength;
        Vec3d currentPos = mc.player.getPos();
        double traveledDistance = 0;

        switch (currentDirection) {
            case 0: // East
                traveledDistance = currentPos.x - lastTurnPos.x;
                break;
            case 1: // South
                traveledDistance = currentPos.z - lastTurnPos.z;
                break;
            case 2: // West
                traveledDistance = lastTurnPos.x - currentPos.x;
                break;
            case 3: // North
                traveledDistance = lastTurnPos.z - currentPos.z;
                break;
        }

        if (traveledDistance >= distance) {
            startTurn();
        }
    }

    private boolean isObstacleAhead() {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(0);
        Vec3d rayEnd = playerPos.add(lookVec.multiply(15)); // Check 15 blocks ahead

        RaycastContext raycastContext = new RaycastContext(playerPos, rayEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        return mc.world.raycast(raycastContext).getType() != HitResult.Type.MISS;
    }

    /**
     * Finds and uses a firework rocket from the hotbar using packets to avoid client-side slot changes.
     */
    private void useRocket() {
        int rocketSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                rocketSlot = i;
                break;
            }
        }

        if (rocketSlot != -1) {
            int originalSlot = mc.player.getInventory().selectedSlot;

            if (rocketSlot != originalSlot) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(rocketSlot));
            }

            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id));
            rocketTimer.reset();

            if (rocketSlot != originalSlot) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
            }
        }
    }

    private void startTurn() {
        lastTurnPos = mc.player.getPos();
        currentDirection = (currentDirection + 1) % 4;
        legsCompleted++;

        if (legsCompleted >= 2) {
            legsCompleted = 0;
            legLength++;
        }

        targetYaw = YAW_ANGLES[currentDirection];

        currentState = State.TURNING;
    }

    private void handleTurning() {
        float angleDifference = getAngleDifference(mc.player.getYaw(), targetYaw);
        float rotationStep = rotationSpeed.getValueFloat();

        if (Math.abs(angleDifference) > 1.0f) { // Continue turning
            float rotationAmount = Math.min(rotationStep, Math.abs(angleDifference));
            float newYaw = mc.player.getYaw() + Math.copySign(rotationAmount, angleDifference);
            mc.player.setYaw(normalizeYaw(newYaw));
        } else { // Turning is complete
            mc.player.setYaw(targetYaw);
            currentState = State.SCANNING;
        }
    }

    private float normalizeYaw(float yaw) {
        return (yaw % 360 + 360) % 360;
    }

    private float getAngleDifference(float currentYaw, float targetYaw) {
        float diff = (targetYaw - currentYaw + 180) % 360 - 180;
        return diff < -180 ? diff + 360 : diff;
    }

    private boolean isWearingElytra() {
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return chestStack.getItem() == Items.ELYTRA;
    }
}