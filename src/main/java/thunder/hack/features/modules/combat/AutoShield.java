package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.util.List;

public class AutoShield extends Module {

    public AutoShield() {
        super("AutoShield", Category.COMBAT);
    }

    private final Setting<Float> detectRange = new Setting<>("DetectRange", 5.0f, 1.0f, 8.0f);

    private boolean isBlocking = false;

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (!isHoldingShield()) {
            stopBlocking();
            return;
        }

        boolean shouldBlock = false;

        List<PlayerEntity> nearbyPlayers = mc.world.getEntitiesByClass(PlayerEntity.class, getBox(detectRange.getValue()), this::isEnemy);

        for (PlayerEntity player : nearbyPlayers) {
            if (isLookingAtUs(player)) {
                shouldBlock = true;
                break;
            }
        }

        if (shouldBlock) {
            startBlocking();
        } else {
            stopBlocking();
        }
    }

    private boolean isLookingAtUs(PlayerEntity player) {
        Vec3d eyes = player.getCameraPosVec(1.0f);
        Vec3d lookVec = player.getRotationVec(1.0f).normalize();
        Vec3d ourBody = mc.player.getPos().add(0, mc.player.getHeight() / 2, 0);
        Vec3d toUs = ourBody.subtract(eyes).normalize();

        double dot = lookVec.dotProduct(toUs);

        return dot > 0.906;
    }

    private boolean isHoldingShield() {
        return mc.player.getOffHandStack().getItem() == Items.SHIELD || mc.player.getMainHandStack().getItem() == Items.SHIELD;
    }

    private boolean isEnemy(PlayerEntity player) {
        return player != mc.player && player.isAlive() && !player.isInvisible();
    }

    private void startBlocking() {
        if (!isBlocking) {
            mc.options.useKey.setPressed(true);
            isBlocking = true;
        }
    }

    private void stopBlocking() {
        if (isBlocking) {
            mc.options.useKey.setPressed(false);
            isBlocking = false;
        }
    }

    private Box getBox(float range) {
        return new Box(
                mc.player.getX() - range, mc.player.getY() - 2, mc.player.getZ() - range,
                mc.player.getX() + range, mc.player.getY() + 2, mc.player.getZ() + range
        );
    }

    @Override
    public void onDisable() {
        stopBlocking();
    }
}
