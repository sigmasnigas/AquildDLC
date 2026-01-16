package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import thunder.hack.features.modules.Module;
import thunder.hack.events.impl.EventTick;
import net.minecraft.client.option.GameOptions;

public class FastPlace extends Module {
    private final GameOptions gameOptions = mc.options;

    public FastPlace() {
        super("FastPlace", Category.MISC);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player != null && mc.world != null) {
            gameOptions.useKey.setPressed(true);
        }
    }

    @Override
    public void onDisable() {
        if (gameOptions != null) {
            gameOptions.useKey.setPressed(false);
        }
    }
}
