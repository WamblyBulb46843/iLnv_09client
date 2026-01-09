package dev.iLnv_09.mod.modules.impl.combat;

import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;

public class AutoBow extends Module {
    
    private final SliderSetting fireDelay = add(new SliderSetting("FireDelay", 0.5, 0.1, 3.0, 0.01).setSuffix("s"));
    private final SliderSetting chargeTime = add(new SliderSetting("ChargeTime", 0.8, 0.5, 2.0, 0.01).setSuffix("s"));
    private final BooleanSetting autoAim = add(new BooleanSetting("AutoAim", false));
    private final BooleanSetting message = add(new BooleanSetting("Message", true));
    
    private final Timer chargeTimer = new Timer();
    private final Timer fireTimer = new Timer();
    
    private boolean isCharging = false;
    private boolean isFiring = false;
    
    public AutoBow() {
        super("AutoBow", "Automatically fires arrows when holding bow", Category.Combat);
        setChinese("自动弓箭");
    }
    
    @Override
    public void onEnable() {
        isCharging = false;
        isFiring = false;
        chargeTimer.reset();
        fireTimer.reset();
    }
    
    @Override
    public void onDisable() {
        // 清理状态
        isCharging = false;
        isFiring = false;
    }
    
    @Override
    public void onUpdate() {
        if (nullCheck()) return;
        
        // 检查是否拿着弓
        if (!isHoldingBow()) {
            resetState();
            return;
        }
        
        // 检查是否长按右键（使用物品）
        if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() == Items.BOW) {
            handleBowShooting();
        } else {
            resetState();
        }
    }
    
    private void handleBowShooting() {
        // 如果没有在蓄力，开始蓄力
        if (!isCharging) {
            isCharging = true;
            chargeTimer.reset();
        }
        
        // 检查蓄力时间是否足够
        if (isCharging && chargeTimer.passedMs((long) (chargeTime.getValue() * 1000))) {
            // 检查是否可以发射（间隔时间）
            if (!isFiring || fireTimer.passedMs((long) (fireDelay.getValue() * 1000))) {
                fireArrow();
                isFiring = true;
                fireTimer.reset();
            }
        }
    }
    
    private void fireArrow() {
        if (nullCheck()) return;
        
        // 发送释放弓箭的包
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
            mc.player.getBlockPos(),
            mc.player.getHorizontalFacing()
        ));

        // 可选：发送聊天消息
        if (message.getValue()) {
            mc.player.sendMessage(Text.of("§fArrow fired！"), true);
        }
        
        // 如果启用了自动瞄准，可以在这里添加瞄准逻辑á
        if (autoAim.getValue()) {
            // 这里可以调用你的自动瞄准系统
            // 例如: aimAtNearestTarget();
        }
    }
    
    private boolean isHoldingBow() {
        return mc.player.getMainHandStack().getItem() == Items.BOW || 
               mc.player.getOffHandStack().getItem() == Items.BOW;
    }
    
    private void resetState() {
        isCharging = false;
        isFiring = false;
    }
    
    // 可选：添加自动瞄准方法（如果需要）
    /*
    private void aimAtNearestTarget() {
        // 这里实现自动瞄准逻辑
        // 可以参考原代码中的瞄准方法
    }
    */
}