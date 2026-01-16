package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AntiTrap extends Module {
    public AntiTrap() {
        super("AntiTrap", Category.MISC);
    }

    private final Setting<Boolean> tntMinecarts = new Setting<>("TNTMinecarts", true);
    private final Setting<Boolean> chestMinecarts = new Setting<>("ChestMinecarts", true);
    private final Setting<Boolean> armorStands = new Setting<>("ArmorStands", true);
    private final Setting<Boolean> allMinecarts = new Setting<>("AllMinecarts", false);

    @EventHandler
    public void onSync(EventSync e) {
        for (Entity entity : Managers.ASYNC.getAsyncEntities()) {
            if (entity instanceof TntMinecartEntity && tntMinecarts.getValue()) {
                mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
            } else if (entity instanceof ChestMinecartEntity && chestMinecarts.getValue()) {
                mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
            } else if (entity instanceof ArmorStandEntity && armorStands.getValue()) {
                mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
            } else if (entity instanceof AbstractMinecartEntity && allMinecarts.getValue()) {
                mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
            }
        }
    }
}
