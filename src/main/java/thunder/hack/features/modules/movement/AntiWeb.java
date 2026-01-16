package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventCollision;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class AntiWeb extends Module {
    public AntiWeb() {
        super("AntiWeb", Category.MOVEMENT);
    }

    public static final Setting<Mode> mode = new Setting<>("Mode", Mode.Solid);
    public static final Setting<Boolean> grim = new Setting<>("Grim", false, v -> mode.is(Mode.Ignore));
    public static final Setting<Float> timer = new Setting<>("Timer", 20f, 1f, 50f, v -> mode.getValue() == Mode.Timer);
    public Setting<Float> speed = new Setting<>("Speed", 0.3f, 0.0f, 10.0f, v -> mode.getValue() == Mode.Fly);
    public static final Setting<Boolean> jumpBypass = new Setting<>("JumpBypass", false, v -> mode.getValue() == Mode.Ignore);

    private boolean timerEnabled = false;
    private boolean sprintBlocked = false;

    public enum Mode {
        Timer, Solid, Ignore, Fly
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (isNearCobweb(2.0)) {
            if (mc.options.jumpKey.isPressed()) {
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    sprintBlocked = true;
                }
            } else if (mc.player.isOnGround() && sprintBlocked) {
                mc.player.setSprinting(true);
                sprintBlocked = false;
            }
        }

        if (Managers.PLAYER.isInWeb()) {
            if (mode.getValue() == Mode.Timer) {
                if (mc.player.isOnGround()) {
                    ThunderHack.TICK_TIMER = 1f;
                } else {
                    ThunderHack.TICK_TIMER = timer.getValue();
                    timerEnabled = true;
                }
            }
            if (mode.getValue() == Mode.Fly) {
                final double[] dir = MovementUtility.forward(speed.getValue());
                mc.player.setVelocity(dir[0], 0, dir[1]);
                if (mc.options.jumpKey.isPressed())
                    mc.player.setVelocity(mc.player.getVelocity().add(0, speed.getValue(), 0));
                if (mc.options.sneakKey.isPressed())
                    mc.player.setVelocity(mc.player.getVelocity().add(0, -speed.getValue(), 0));
            }
        }

        if (!Managers.PLAYER.isInWeb() && timerEnabled) {
            timerEnabled = false;
            ThunderHack.TICK_TIMER = 1f;
        }
    }

    private boolean isNearCobweb(double maxDistance) {
        Vec3d playerPos = mc.player.getPos();
        BlockPos playerBlockPos = mc.player.getBlockPos();

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = playerBlockPos.add(x, y, z);
                    if (mc.world.getBlockState(checkPos).getBlock() instanceof CobwebBlock) {
                        Vec3d blockCenter = Vec3d.ofCenter(checkPos);
                        if (playerPos.distanceTo(blockCenter) <= maxDistance) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onCollide(EventCollision e) {
        if (e.getState().getBlock() instanceof CobwebBlock && mode.getValue() == Mode.Solid)
            e.setState(Blocks.DIRT.getDefaultState());
    }
}
