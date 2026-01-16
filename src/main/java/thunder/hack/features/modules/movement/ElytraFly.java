package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class ElytraFly extends Module {
    private final Setting<Float> speed = new Setting<>("Speed", 1.0F, 0.1F, 5.0F);
    private final Setting<Mode> bypassMode = new Setting<>("Bypass", Mode.None);
    private final Setting<Boolean> useFireworks = new Setting<>("UseFireworks", true);

    public ElytraFly() {
        super("ElytraFly", Category.MOVEMENT);
    }

    private enum Mode {
        None, Grim
    }

    @EventHandler
    public void onTick() {
        if (mc.player == null || !mc.player.isFallFlying()) return;

        handleMovement();
        if (useFireworks.getValue()) {
            useFireworkBasedOnSpeed();
        }
    }

    private void handleMovement() {
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        double radians = Math.toRadians(yaw);
        double sinYaw = Math.sin(radians);
        double cosYaw = Math.cos(radians);

        double xSpeed = forward * cosYaw - strafe * sinYaw;
        double zSpeed = forward * sinYaw + strafe * cosYaw;

        mc.player.setVelocity(xSpeed * speed.getValue(), mc.player.getVelocity().y, zSpeed * speed.getValue());
    }

    private void useFireworkBasedOnSpeed() {
        if (mc.player.age % Math.max(1, (int) (20 / speed.getValue())) == 0) {
            useFirework();
        }
    }

    private void useFirework() {
        SearchInvResult hotbarFirework = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET);
        SearchInvResult inventoryFirework = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);

        InventoryUtility.saveSlot();

        if (hotbarFirework.found()) {
            hotbarFirework.switchTo();
        } else if (inventoryFirework.found()) {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    inventoryFirework.slot(),
                    mc.player.getInventory().selectedSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        } else {
            Managers.NOTIFICATION.publicity("ElytraFLY", "No fireworks found!", 1, Notification.Type.ERROR);
            return;
        }

        sendSequencedPacket(id -> {
            return new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch());
        });
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }
}
