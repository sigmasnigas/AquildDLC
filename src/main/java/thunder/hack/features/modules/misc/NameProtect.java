package thunder.hack.features.modules.misc;

import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class NameProtect extends Module {
    public NameProtect() {
        super("NameProtect", Category.MISC);
    }

    public static Setting<Boolean> hideFriends = new Setting<>("Hide friends", true);
    public static Setting<String> customName = new Setting<>("CustomName", "dsc.gg/exploitcore");

    public static String getCustomName() {
        return ModuleManager.nameProtect.isEnabled() ? customName.getValue() : mc.getGameProfile().getName();
    }
}