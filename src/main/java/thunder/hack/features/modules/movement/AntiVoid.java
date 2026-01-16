package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AntiVoid extends Module {
    public AntiVoid() {
        super("AntiVoid", Category.MOVEMENT);
    }

    private static final Setting<Mode> mode = new Setting<>("Mode", Mode.NCP);
    private final Setting<Boolean> sendPacket = new Setting<>("SendPacket", true, v -> mode.getValue() == Mode.NCP);
    private static final Setting<ExtraMode> extraMode = new Setting<>("Lava/Death", ExtraMode.Disabled);

    private enum Mode {NCP, Timer}
    private enum ExtraMode {Disabled, Lava, Death, Both}

    boolean timerFlag;

    @Override
    public void onDisable() {
        if (timerFlag)
            ThunderHack.TICK_TIMER = 1f;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(EventMove e) {
        if (fullNullCheck())
            return;

        if (shouldTriggerProtection()) {
            if (mode.getValue() == Mode.NCP) {
                e.cancel();
                e.setY(0);
                if (sendPacket.getValue())
                    sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            } else {
                ThunderHack.TICK_TIMER = 0.2f;
                timerFlag = true;
            }
        } else if (timerFlag) {
            ThunderHack.TICK_TIMER = 1f;
            timerFlag = false;
        }
    }

    private boolean shouldTriggerProtection() {
        ExtraMode currentExtraMode = extraMode.getValue();
        boolean isFallingToVoid = fallingToVoid();
        boolean isDangerAboveLava = dangerAboveLava();

        return (currentExtraMode == ExtraMode.Both && (isFallingToVoid || isDangerAboveLava)) ||
                (currentExtraMode == ExtraMode.Lava && isDangerAboveLava) ||
                (currentExtraMode == ExtraMode.Death && isFallingToVoid);
    }

    private boolean fallingToVoid() {
        for (int i = (int) mc.player.getY() - 1; i >= -64; i--) {
            BlockPos pos = BlockPos.ofFloored(mc.player.getX(), i, mc.player.getZ());
            if (!mc.world.isAir(pos)) {
                return false;
            }
        }
        return mc.player.fallDistance > 10;
    }

    private boolean dangerAboveLava() {
        for (int i = 1; i <= 5; i++) {
            BlockPos pos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - i, mc.player.getZ());
            if (mc.world.getBlockState(pos).isOf(Blocks.LAVA)) {
                return true;
            }
            if (!mc.world.isAir(pos)) {
                return false;
            }
        }
        return false;
    }
}
