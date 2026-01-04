package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.DurabilityEvent;
import dev.iLnv_09.mod.modules.Module;

public class TrueDurability extends Module {

    public TrueDurability() {
        super("TrueDurability", Category.Misc);
        setChinese("耐久度修正");
    }

    @EventHandler
    public void onDurability(DurabilityEvent event) {
        int dura = event.getItemDamage();
        if (event.getDamage() < 0) {
            dura = event.getDamage();
        }
        event.cancel();
        event.setDamage(dura);
    }
}
