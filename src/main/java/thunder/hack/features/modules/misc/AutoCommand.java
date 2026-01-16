package thunder.hack.features.modules.misc;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

public class AutoCommand extends Module {

    private final Setting<String> command = new Setting<>("Command", "sklep");
    private final Setting<Integer> interval = new Setting<>("Interval", 20, 0, 600);
    private final Setting<Boolean> repeat = new Setting<>("Repeat", false);
    private final Timer timer = new Timer();

    public AutoCommand() {
        super("AutoCommand", Category.MISC);
    }

    @Override
    public void onEnable() {
        mc.player.networkHandler.sendCommand(command.getValue());
        if (!repeat.getValue()) {
            disable();
        }
        timer.reset();
    }

    @Override
    public void onUpdate() {
        if (repeat.getValue() && timer.passedMs(interval.getValue() * 1000L)) {
            mc.player.networkHandler.sendCommand(command.getValue());
            timer.reset();
        }
    }
}
