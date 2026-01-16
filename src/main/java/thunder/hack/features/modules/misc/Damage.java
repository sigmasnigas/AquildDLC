package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class Damage extends Module {
    private final Setting<DamageMode> mode = new Setting<>("Mode", DamageMode.NCP);
    private final Setting<Integer> damage = new Setting<>("Damage", 1, 0, 20);

    public Damage() {
        super("Damage", Category.MISC);
    }

    @Override
    public void onEnable() {
        damage(mode.getValue());
        disable();
    }

    private void damage(DamageMode mode) {
        mode.run();
    }

    private enum DamageMode {
        NCP("NCP", () -> {
            if (mc.player == null || mc.getNetworkHandler() == null) return;

            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            for (int i = 0; i < 5; i++) {
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y + 3.0, z, false));
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, false));
            }

            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, true));
        }),

        AAC("AAC", () -> {
            if (mc.player == null || mc.getNetworkHandler() == null) return;

            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 3.5, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
        }),

        Verus("Verus", () -> {
            if (mc.player == null || mc.getNetworkHandler() == null) return;

            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 5, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
        });

        private final String name;
        private final Runnable action;

        DamageMode(String name, Runnable action) {
            this.name = name;
            this.action = action;
        }

        public void run() {
            action.run();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
