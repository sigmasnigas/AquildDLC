package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;

import static thunder.hack.utility.player.InteractionUtility.checkNearBlocks;

public class AutoDripStone extends Module {
    Setting<Integer> range = new Setting<>("Range", 3, 1, 5);
    Setting<Integer> delay = new Setting<>("Delay", 1, 1, 1500);
    Setting<Boolean> silentSwitch = new Setting<>("SilentSwitch", true);
    Timer timer = new Timer();

    public AutoDripStone() {
        super("AutoDripStone", Category.MISC);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || !timer.passedMs(delay.getValue())) return;

        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = -range.getValue(); x <= range.getValue(); x++) {
            for (int y = -range.getValue(); y <= range.getValue(); y++) {
                for (int z = -range.getValue(); z <= range.getValue(); z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);

                    if (state.getBlock() instanceof TrapdoorBlock && !state.get(TrapdoorBlock.OPEN)) {
                        if (handleDripstonePlacement(pos)) {
                            timer.reset();
                            return;
                        }
                    }
                }
            }
        }
    }

    boolean handleDripstonePlacement(BlockPos trapdoorPos) {
        BlockPos placePos = trapdoorPos.down();
        if (!mc.world.getBlockState(placePos).isAir()) return false;

        int dripstoneSlot = InventoryUtility.findInHotBar(i -> i.getItem() == Items.POINTED_DRIPSTONE).slot();
        if (dripstoneSlot == -1) return false;

        int prevSlot = mc.player.getInventory().selectedSlot;
        if (silentSwitch.getValue()) switchToSlotSilent(dripstoneSlot);
        else switchToSlot(dripstoneSlot);

        InteractionUtility.BlockPosWithFacing targetBlock = checkNearBlocks(placePos);
        if (targetBlock == null) return false;

        placeBlock(targetBlock);

        timer.reset();
        mc.player.getInventory().selectedSlot = prevSlot;

        holdRightClickOnTrapdoor(trapdoorPos);
        return true;
    }

    void holdRightClickOnTrapdoor(BlockPos trapdoorPos) {
        if (!timer.passedMs(1)) return;
        BlockHitResult hitResult = new BlockHitResult(mc.player.getPos(), Direction.UP, trapdoorPos, false);
        sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    void switchToSlotSilent(int slot) {
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    void switchToSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
    }

    void placeBlock(InteractionUtility.BlockPosWithFacing block) {
        BlockHitResult bhr = new BlockHitResult(block.position().toCenterPos().add(new Vec3d(block.facing().getUnitVector()).multiply(0.5)), block.facing(), block.position(), false);

        if (!timer.passedMs(1)) return;
        sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, 0));
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    public void sendPacket(Packet<?> packet) {
        mc.player.networkHandler.sendPacket(packet);
    }
}
