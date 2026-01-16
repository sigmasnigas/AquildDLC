
package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.util.Random;

public class Disabler extends Module {
    private final Random random = new Random();
    public final Setting<Boolean> vulcanLimit = new Setting<>("VulcanLimit", false);

    public Disabler() {
        super("Disabler", Category.MISC);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!vulcanLimit.getValue() || mc.player == null || mc.getNetworkHandler() == null) return;

        // Orginal autor: TrimoneWasTaken

        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isDead() || mc.player.isClimbing() || mc.player.getAbilities().flying) {
            return;
        }

        if (isTellyBridging() && mc.player.age % 9 == 0 && random.nextFloat() <= 0.7f) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
        }

        if (mc.player.isOnGround() && isTowering() && random.nextFloat() <= 0.2f) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
        }

        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));

        if (mc.player.age % 9 == 0 && mc.player.isOnGround() && !isTellyBridging() && !isTowering()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
        }
    }

    private boolean isTellyBridging() {
        return mc.player.getVelocity().lengthSquared() > 0.1 && mc.options.sneakKey.isPressed();
    }

    private boolean isTowering() {
        return mc.options.jumpKey.isPressed() && mc.player.isOnGround();
    }
}
