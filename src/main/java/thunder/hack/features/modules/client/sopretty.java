package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;

import java.util.List;
import java.util.Random;

public class sopretty extends Module {

    private final Random random = new Random();
    private boolean switchDir = false;

    public sopretty() {
        super("ForceOP", Category.CLIENT);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        ClientPlayerEntity player = mc.player;

        Vec3d velocity = new Vec3d(player.getVelocity().x * (random.nextBoolean() ? 5 : -5), 2, player.getVelocity().z * (random.nextBoolean() ? 5 : -5));
        player.setVelocity(velocity);

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getX(), player.getY() + random.nextInt(5, 20), player.getZ(), false));

        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING));

        for (int i = 0; i < 50; i++) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getX(), player.getY() + (i * 2), player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                    player.getX() + (random.nextBoolean() ? 1.5 : -1.5),
                    player.getY(),
                    player.getZ() + (random.nextBoolean() ? 1.5 : -1.5),
                    player.getYaw(),
                    player.getPitch(),
                    false
            ));
        }

        List<Entity> entities = mc.world.getOtherEntities(player, new Box(player.getX() - 3, player.getY() - 3, player.getZ() - 3, player.getX() + 3, player.getY() + 3, player.getZ() + 3));
        for (Entity entity : entities) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false));
        }

        if (random.nextInt(5) == 0) {
            BlockPos blockUnder = player.getBlockPos().down();
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockUnder, player.getHorizontalFacing()));
        }

        if (random.nextBoolean()) {
            switchDir = !switchDir;
            Vec3d motion = new Vec3d((switchDir ? 0.5 : -0.5), 0, (switchDir ? 0.5 : -0.5));
            player.addVelocity(motion.x, motion.y, motion.z);
        }
    }

    private void sendFakeOpMessage(ClientPlayerEntity player) {
        String message = "[Server: Made " + player.getName().getString() + " a server operator]";
        player.sendMessage(Text.literal(message).formatted(Formatting.GRAY, Formatting.ITALIC), false);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        sendFakeOpMessage(mc.player);
    }
}
