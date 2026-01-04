package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class AddFriend extends Module {
	public static AddFriend INSTANCE;

	public AddFriend() {
		super("AddFriend", Category.Misc);
		setChinese("加好友");
		INSTANCE = this;
	}

	@Override
	public void onEnable() {
		if (nullCheck()) {
			disable();
			return;
		}
		HitResult target = mc.crosshairTarget;
		if (target instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity player) {
			iLnv_09.FRIEND.friend(player);
		}
		disable();
	}
}