/*
package thunder.hack.features.modules.client;

import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;

public class Dev extends Module {

    private final ModuleManager moduleManager;

    public Dev(ModuleManager moduleManager) {
        super("Dev", Category.CLIENT);
        this.moduleManager = moduleManager;
    }

    @Override
    public void onEnable() {
        Rotations moveFixModule = getRotationsModule();
        if (moveFixModule != null) {
            moveFixModule.setMoveFixFocused();
        }
    }

    @Override
    public void onDisable() {
        Rotations moveFixModule = getRotationsModule();
        if (moveFixModule != null) {
            moveFixModule.setMoveFixFree();
        }
    }

    private Rotations getRotationsModule() {
        return (Rotations) Managers.MODULE.get("Rotations");
    }
}
*/
