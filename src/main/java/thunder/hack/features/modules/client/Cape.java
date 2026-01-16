package thunder.hack.features.modules.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public final class Cape extends Module {
    public Cape() {
        super("Cape", Category.CLIENT);
    }

    public final Setting<CapeType> capeType = new Setting<>("Cape", CapeType.Star);
    public final Setting<Boolean> elytraTexture = new Setting<>("ElytraTexture", true);
    public final Setting<Boolean> onlySelf = new Setting<>("OnlySelf", true);
    public final Setting<Boolean> friends = new Setting<>("Friends", true, v -> onlySelf.getValue());

    public enum CapeType {
        Star("starcape.png"),
        Tester("tester.png"),
        FBGroup("fbgroup.png"),
        Dev("dev.png"),
        Exploitcore("test.png"),
        BKGroup("bkgroup.png"),
        OFF("none");

        private final String fileName;

        CapeType(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public Identifier getCapeTexture(AbstractClientPlayerEntity player) {
        if (!isEnabled() || capeType.getValue() == CapeType.OFF) {
            return null;
        }

        if (onlySelf.getValue()) {
            if (player != mc.player) {
                if (!friends.getValue() || !Managers.FRIEND.isFriend(player)) {
                    return null;
                }
            }
        }

        return Identifier.of("thunderhack", "textures/capes/" + capeType.getValue().getFileName());
    }
} 