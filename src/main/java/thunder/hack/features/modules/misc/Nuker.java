package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAttackBlock;
import thunder.hack.events.impl.EventSetBlockState;
import thunder.hack.events.impl.EventSync;

import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.player.SpeedMine;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.world.ExplosionUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static net.minecraft.block.Blocks.*;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class Nuker extends Module {
    public Nuker() {
        super("Nuker", Category.MISC);
    }

    public final Setting<ItemSelectSetting> selectedBlocks = new Setting<>("SelectedBlocks", new ItemSelectSetting(new ArrayList<>()));
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Default);
    private final Setting<Boolean> silentAim = new Setting<>("SilentAim", false);
    private final Setting<Integer> packetLimit = new Setting<>("PacketLimit", 5, 1, 20);
    private final Setting<Integer> delay = new Setting<>("Delay", 25, 0, 1000);
    private final Setting<Float> customFOV = new Setting<>("CustomFOV", 90f, 30f, 180f, v -> mode.getValue() == Mode.FastSafe);
    private final Setting<Boolean> autoWalk = new Setting<>("AutoWalk", false, v -> mode.getValue() == Mode.FastSafe);
    private final Setting<BlockSelection> blocks = new Setting<>("Blocks", BlockSelection.Select);
    private final Setting<Boolean> noBack = new Setting<>("NoBack", false, v -> mode.getValue() == Mode.FastSafe);
    private final Setting<Float> reach = new Setting<>("Reach", 4.2f, 1.5f, 25f, v -> mode.getValue() == Mode.FastSafe);
    private final Setting<Boolean> ignoreWalls = new Setting<>("IgnoreWalls", false);
    private final Setting<Boolean> flatten = new Setting<>("Flatten", false);
    private final Setting<Boolean> creative = new Setting<>("Creative", false);
    private final Setting<Boolean> avoidLava = new Setting<>("AvoidLava", false);
    private final Setting<Float> range = new Setting<>("Range", 4.2f, 1.5f, 25f);
    private final Setting<ColorMode> colorMode = new Setting<>("ColorMode", ColorMode.Sync);
    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0x2250b4b4), v -> colorMode.getValue() == ColorMode.Custom);
    private final Setting<BypassMode> bypass = new Setting<>("Bypass", BypassMode.None);
    private final Setting<Boolean> gencash = new Setting<>("GenCash", false);

    private Timer packetTimer = new Timer();
    private Block targetBlockType;
    private BlockData blockData;
    private Timer breakTimer = new Timer();
    private float customFOVValue = 70f;
    private int tickCounter = 0;

    private NukerThread nukerThread = new NukerThread();
    private float rotationYaw, rotationPitch;

    @Override
    public void onEnable() {
        nukerThread = new NukerThread();
        nukerThread.setName("ThunderHack-NukerThread");
        nukerThread.setDaemon(true);
        nukerThread.start();

        if (autoWalk.getValue()) {
            mc.options.forwardKey.setPressed(true);
        }
    }

    @Override
    public void onDisable() {
        nukerThread.interrupt();
        customFOVValue = 70f;
        mc.options.forwardKey.setPressed(false);
    }

    @Override
    public void onUpdate() {
        if (!nukerThread.isAlive()) {
            nukerThread = new NukerThread();
            nukerThread.setName("ThunderHack-NukerThread");
            nukerThread.setDaemon(true);
            nukerThread.start();
        }
    }

    @EventHandler
    public void onBlockInteract(EventAttackBlock e) {
        if (mc.world.isAir(e.getBlockPos())) return;
        if (blocks.getValue().equals(BlockSelection.Select) && targetBlockType != mc.world.getBlockState(e.getBlockPos()).getBlock()) {
            targetBlockType = mc.world.getBlockState(e.getBlockPos()).getBlock();
            sendMessage(isRu() ? "Выбран блок: " + Formatting.AQUA + targetBlockType.getName().getString() : "Selected block: " + Formatting.AQUA + targetBlockType.getName().getString());
        }
    }

    @EventHandler
    public void onBlockDestruct(EventSetBlockState e) {
        if (blockData != null && e.getPos() == blockData.bp && e.getState().isAir()) {
            blockData = null;
            new Thread(() -> {
                if ((targetBlockType != null || blocks.getValue().equals(BlockSelection.All)) && !mc.options.attackKey.isPressed() && blockData == null) {
                    blockData = getNukerBlockPos();
                }
            }).start();
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if(rotationYaw != -999) {
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
            rotationYaw = -999;
        }
    }

    private boolean isInCustomFOV(BlockPos blockPos) {
        Vec3d blockVec = blockPos.toCenterPos();
        Vec3d eyesPos = InteractionUtility.getEyesPos(mc.player);
        Vec3d lookVec = mc.player.getRotationVec(1.0F).normalize();

        Vec3d toBlock = blockVec.subtract(eyesPos).normalize();
        double angle = Math.acos(lookVec.dotProduct(toBlock)) * (180 / Math.PI);
        return angle <= (customFOV.getValue() / 2);
    }

    private void handleAutoWalk() {
        if (blockData == null) {
            mc.options.forwardKey.setPressed(false);
            return;
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d direction = mc.player.getRotationVec(1.0F).normalize();

        BlockPos frontPos = BlockPos.ofFloored(playerPos.add(direction.x, 0, direction.z));
        BlockPos aboveFrontPos = frontPos.up();


        if (!isAllowed(mc.world.getBlockState(frontPos).getBlock())) {
            if (mc.world.getBlockState(frontPos).isFullCube(mc.world, frontPos)
                    && mc.world.getBlockState(aboveFrontPos).isAir()) {
                mc.player.jump();
            } else {
                findNewDirection();
                return;
            }
        }

        mc.options.forwardKey.setPressed(true);
    }



    private boolean areSelectedBlocksAhead(int blocksToCheck) {
        Vec3d direction = mc.player.getRotationVec(1.0F).normalize();
        Vec3d eyesPos = InteractionUtility.getEyesPos(mc.player);

        for (int i = 1; i <= blocksToCheck; i++) {
            Vec3d checkPos = eyesPos.add(direction.multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);

            if (isAllowed(mc.world.getBlockState(blockPos).getBlock())) {
                return true;
            }
        }
        return false;
    }


    private boolean hasSelectedBlocksInFOV() {
        int intRange = (int) (Math.floor(range.getValue()) + 1);
        Iterable<BlockPos> blocks = BlockPos.iterateOutwards(new BlockPos(BlockPos.ofFloored(mc.player.getPos()).up()), intRange, intRange, intRange);

        for (BlockPos b : blocks) {
            if (isAllowed(mc.world.getBlockState(b).getBlock()) && isInCustomFOV(b)) {
                return true;
            }
        }
        return false;
    }


    private void findNewDirection() {
        int intRange = (int) (Math.floor(range.getValue()) + 1);
        Iterable<BlockPos> blocks = BlockPos.iterateOutwards(new BlockPos(BlockPos.ofFloored(mc.player.getPos()).up()), intRange, intRange, intRange);

        BlockPos closestBlock = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos b : blocks) {
            if (isAllowed(mc.world.getBlockState(b).getBlock())) {
                double distance = mc.player.squaredDistanceTo(b.toCenterPos());
                if (distance < closestDistance) {
                    closestBlock = b;
                    closestDistance = distance;
                }
            }
        }

        if (closestBlock != null) {
            Vec3d direction = closestBlock.toCenterPos().subtract(mc.player.getPos()).normalize();
            float targetYaw = (float) (MathHelper.atan2(direction.z, direction.x) * (180 / Math.PI)) - 90;
            mc.player.setYaw(targetYaw);
            blockData = new BlockData(closestBlock, closestBlock.toCenterPos(), Direction.UP);
        }
    }

    private void smoothRotate(float targetYaw, float targetPitch) {
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;

        float maxDelta = 5.0f;
        float smoothYaw = currentYaw + MathHelper.clamp(deltaYaw, -maxDelta, maxDelta);
        float smoothPitch = currentPitch + MathHelper.clamp(deltaPitch, -maxDelta, maxDelta);

        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(smoothPitch);
    }


    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (blockData != null) {
            if ((mc.world.getBlockState(blockData.bp).getBlock() != targetBlockType && blocks.getValue().equals(BlockSelection.Select))
                    || PlayerUtility.squaredDistanceFromEyes(blockData.bp.toCenterPos()) > Math.pow(reach.getValue(), 2)
                    || !isInCustomFOV(blockData.bp)
                    || mc.world.isAir(blockData.bp)) {
                blockData = null;
            }

            if (blockData == null || mc.options.attackKey.isPressed()) return;

            if (bypass.getValue() == BypassMode.Vulcan) {
                handleBypass(blockData.bp);
            } else if (gencash.getValue() && isFullyGrownWheat(blockData.bp)) {
                breakBlock();
            } else if (!gencash.getValue()) {
                breakBlock();
            }
        }

        if (blockData == null || mc.options.attackKey.isPressed()) return;

        if (customFOV.getValue() != customFOVValue) {
            customFOVValue = customFOV.getValue();
        }

        if (autoWalk.getValue()) {
            handleAutoWalk();
        }

        float[] angle = InteractionUtility.calculateAngle(blockData.vec3d);
        rotationYaw = angle[0];
        rotationPitch = angle[1];
        ModuleManager.rotations.fixRotation = rotationYaw;


        if (mode.getValue() == Mode.FastSafe) {
            int intRange = (int) (Math.floor(range.getValue()) + 1);
            Iterable<BlockPos> blocks = BlockPos.iterateOutwards(new BlockPos(BlockPos.ofFloored(mc.player.getPos()).up()), intRange, intRange, intRange);

            Set<BlockPos> processedBlocks = new HashSet<>();
            int packetsSent = 0;

            for (BlockPos b : blocks) {
                if (packetsSent >= packetLimit.getValue()) break;

                if (flatten.getValue() && b.getY() < mc.player.getY()) continue;
                if (avoidLava.getValue() && checkLava(b)) continue;

                BlockState state = mc.world.getBlockState(b);

                if (noBack.getValue()) {
                    Vec3d direction = mc.player.getRotationVec(1.0F);
                    Vec3d toBlock = new Vec3d(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5).subtract(mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0));
                    if (direction.dotProduct(toBlock.normalize()) < 0.5) continue;
                }

                if (PlayerUtility.squaredDistanceFromEyes(b.toCenterPos()) <= Math.pow(reach.getValue(), 2) && isAllowed(state.getBlock()) && !processedBlocks.contains(b)) {
                    try {
                        sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, b, Direction.UP, id));
                        mc.interactionManager.breakBlock(b);
                        mc.player.swingHand(Hand.MAIN_HAND);

                        processedBlocks.add(b);
                        packetsSent++;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (mode.getValue() == Mode.FastAF) {
            int intRange = (int) (Math.floor(range.getValue()) + 1);
            Iterable<BlockPos> blocks_ = BlockPos.iterateOutwards(new BlockPos(BlockPos.ofFloored(mc.player.getPos()).up()), intRange, intRange, intRange);

            for (BlockPos b : blocks_) {
                if (flatten.getValue() && b.getY() < mc.player.getY())
                    continue;

                if (avoidLava.getValue() && checkLava(b))
                    continue;

                BlockState state = mc.world.getBlockState(b);

                if (PlayerUtility.squaredDistanceFromEyes(b.toCenterPos()) <= range.getPow2Value()) {
                    if (isAllowed(state.getBlock())) {
                        try {
                            sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, b, Direction.UP, id));
                            mc.interactionManager.breakBlock(b);
                            mc.player.swingHand(Hand.MAIN_HAND);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    private boolean isFullyGrownWheat(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.getBlock() == Blocks.WHEAT && state.get(Properties.AGE_7) == 7;
    }

    public synchronized void breakBlock() {
        if (blockData == null || mc.options.attackKey.isPressed()) return;
        if (ModuleManager.speedMine.isEnabled() && ModuleManager.speedMine.mode.getValue() == SpeedMine.Mode.Packet) {
            if (!ModuleManager.speedMine.alreadyActing(blockData.bp)) {
                mc.interactionManager.attackBlock(blockData.bp, blockData.dir);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            BlockPos cache = blockData.bp;
            mc.interactionManager.updateBlockBreakingProgress(blockData.bp, blockData.dir);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (creative.getValue())
                mc.interactionManager.breakBlock(cache);
        }
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        BlockPos renderBp = null;

        if (blockData != null && blockData.bp != null) {
            renderBp = blockData.bp;
        }

        if (renderBp != null) {
            Color color1 = colorMode.getValue() == ColorMode.Sync ? HudEditor.getColor(1) : color.getValue().getColorObject();
            int colorInt = color1.getRGB();
            Render3DEngine.drawBoxOutline(new Box(blockData.bp), color1, 2);
            Render3DEngine.drawFilledBox(stack, new Box(blockData.bp), Render2DEngine.injectAlpha(color1, 100));

            Vec3d start = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
            Vec3d end = blockData.vec3d;
            Render3DEngine.drawLine(start, end, 2.0f, colorInt);
        }

        if (mode.getValue() == Mode.Fast && breakTimer.passedMs(delay.getValue())) {
            breakBlock();
            breakTimer.reset();
        }
    }



    public BlockData getNukerBlockPos() {
        int intRange = (int) (Math.floor(range.getValue()) + 1);
        Iterable<BlockPos> blocks_ = BlockPos.iterateOutwards(new BlockPos(BlockPos.ofFloored(mc.player.getPos()).up()), intRange, intRange, intRange);

        for (BlockPos b : blocks_) {
            BlockState state = mc.world.getBlockState(b);
            if (flatten.getValue() && b.getY() < mc.player.getY())
                continue;
            if (PlayerUtility.squaredDistanceFromEyes(b.toCenterPos()) <= range.getPow2Value()) {
                if (avoidLava.getValue() && checkLava(b))
                    continue;
                if (isAllowed(state.getBlock())) {
                    if (ignoreWalls.getValue()) {
                        BlockHitResult result = ExplosionUtility.rayCastBlock(new RaycastContext(InteractionUtility.getEyesPos(mc.player), b.toCenterPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player), b);
                        if(result != null)
                            return new BlockData(b, result.getPos(), result.getSide());
                    } else {
                        for (float x1 = 0f; x1 <= 1f; x1 += 0.2f) {
                            for (float y1 = 0f; y1 <= 1; y1 += 0.2f) {
                                for (float z1 = 0f; z1 <= 1; z1 += 0.2f) {
                                    Vec3d p = new Vec3d(b.getX() + x1, b.getY() + y1, b.getZ() + z1);
                                    BlockHitResult bhr = mc.world.raycast(new RaycastContext(InteractionUtility.getEyesPos(mc.player), p, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
                                    if (bhr != null && bhr.getType() == HitResult.Type.BLOCK && bhr.getBlockPos().equals(b))
                                        return new BlockData(b, p, bhr.getSide());
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean checkLava(BlockPos base) {
        for (Direction dir : Direction.values())
            if (mc.world.getBlockState(base.offset(dir)).getBlock() == Blocks.LAVA)
                return true;
        return false;
    }

    public class NukerThread extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!Module.fullNullCheck()) {
                        while (Managers.ASYNC.ticking.get()) {
                        }

                        if ((targetBlockType != null || !blocks.getValue().equals(BlockSelection.Select)) && !mc.options.attackKey.isPressed() && blockData == null) {
                            blockData = getNukerBlockPos();
                        }
                    } else {
                        Thread.yield();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void handleBypass(BlockPos targetPos) {
        if (bypass.getValue() != BypassMode.Vulcan) return;

        Vec3d playerEyes = InteractionUtility.getEyesPos(mc.player);
        Vec3d target = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        float[] rotation = InteractionUtility.calculateAngle(target);

        if (silentAim.getValue()) {
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, Direction.UP)
            );
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, Direction.UP)
            );
            mc.player.setYaw(rotation[0]);
            mc.player.setPitch(rotation[1]);
        }

        mc.player.swingHand(Hand.MAIN_HAND);

        if (silentAim.getValue()) {
            mc.player.setYaw(mc.player.prevYaw);
            mc.player.setPitch(mc.player.prevPitch);
        }
    }





    private boolean isAllowed(Block block) {


        boolean allowed = selectedBlocks.getValue().getItemsById().contains(block.getTranslationKey().replace("block.minecraft.", ""));
        return switch (blocks.getValue()) {
            case All -> block != BEDROCK && block != AIR && block != CAVE_AIR && !(block instanceof FluidBlock);
            case Select -> block == targetBlockType;
            case WhiteList -> allowed;
            default -> !allowed && block != BEDROCK && block != AIR && block != CAVE_AIR && !(block instanceof FluidBlock);
        };
    }


    private enum Mode {
        Default, Fast, FastAF, FastSafe
    }

    private enum ColorMode {
        Custom, Sync
    }

    private enum BlockSelection {
        Select, All, BlackList, WhiteList
    }

    private enum BypassMode {
        None, Vulcan
    }

    public record BlockData(BlockPos bp, Vec3d vec3d, Direction dir) {
    }
}
