package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;

public class WSPAM extends Module {
    public WSPAM() {
        super("WSPAM", Category.CLIENT);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Water);
    private final Setting<Integer> placeDelay = new Setting<>("PlaceDelay", 2, 1, 10);
    private final Setting<Integer> retrieveDelay = new Setting<>("RetrieveDelay", 1, 1, 10);
    private final Setting<Boolean> autoSwitch = new Setting<>("AutoSwitch", true);
    private final Setting<Boolean> onlyFalling = new Setting<>("OnlyFalling", false);
    private final Setting<Float> fallDistance = new Setting<>("FallDistance", 3.0f, 1.0f, 10.0f, v -> onlyFalling.getValue());
    private final Setting<Boolean> autoRetrieve = new Setting<>("AutoRetrieve", true, v -> mode.getValue() != Mode.Ladder);

    private enum Mode {
        Water, Cobweb, Ladder, Both
    }

    private final Timer actionTimer = new Timer();
    private boolean itemPlaced = false;
    private boolean shouldRetrieve = false;
    private int ticksAfterPlace = 0;
    private int prevSlot = -1;
    private BlockPos placedPos = null;

    @Override
    public void onEnable() {
        itemPlaced = false;
        shouldRetrieve = false;
        ticksAfterPlace = 0;
        prevSlot = -1;
        placedPos = null;
        actionTimer.reset();
    }

    @Override
    public void onDisable() {
        if (prevSlot != -1) {
            InventoryUtility.switchTo(prevSlot, true);
            prevSlot = -1;
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        if (onlyFalling.getValue() && mc.player.fallDistance < fallDistance.getValue()) {
            return;
        }

        switch (mode.getValue()) {
            case Water -> handleWaterSpam();
            case Cobweb -> handleCobwebSpam();
            case Ladder -> handleLadderSpam();
            case Both -> handleBothSpam();
        }
    }

    private void handleWaterSpam() {
        boolean hasWaterBucket = mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET || 
                                mc.player.getOffHandStack().getItem() == Items.WATER_BUCKET;
        boolean hasBucket = mc.player.getMainHandStack().getItem() == Items.BUCKET || 
                           mc.player.getOffHandStack().getItem() == Items.BUCKET;

        if (!hasWaterBucket && !hasBucket && autoSwitch.getValue()) {
            switchToItem(Items.WATER_BUCKET, Items.BUCKET);
            return;
        }

        if (!mc.options.useKey.isPressed()) {
            if (itemPlaced && shouldRetrieve && autoRetrieve.getValue()) {
                retrieveWater();
            }
            return;
        }

        if (hasWaterBucket && !itemPlaced) {
            if (actionTimer.passedMs(placeDelay.getValue() * 50)) {
                placeWater();
            }
        } else if (hasBucket && itemPlaced) {
            ticksAfterPlace++;
            if (ticksAfterPlace >= retrieveDelay.getValue()) {
                shouldRetrieve = true;
            }
        }

        if (itemPlaced && shouldRetrieve && hasBucket && autoRetrieve.getValue()) {
            retrieveWater();
        }
    }

    private void handleCobwebSpam() {
        boolean hasCobweb = mc.player.getMainHandStack().getItem() == Items.COBWEB || 
                           mc.player.getOffHandStack().getItem() == Items.COBWEB;

        if (!hasCobweb && autoSwitch.getValue()) {
            switchToItem(Items.COBWEB);
            return;
        }

        if (!mc.options.useKey.isPressed()) {
            if (itemPlaced && shouldRetrieve && autoRetrieve.getValue()) {
                retrieveCobweb();
            }
            return;
        }

        if (hasCobweb && !itemPlaced) {
            if (actionTimer.passedMs(placeDelay.getValue() * 50)) {
                placeCobweb();
            }
        } else if (itemPlaced) {
            ticksAfterPlace++;
            if (ticksAfterPlace >= retrieveDelay.getValue()) {
                shouldRetrieve = true;
            }
        }

        if (itemPlaced && shouldRetrieve && autoRetrieve.getValue()) {
            retrieveCobweb();
        }
    }

    private void handleLadderSpam() {
        boolean hasLadder = mc.player.getMainHandStack().getItem() == Items.LADDER || 
                           mc.player.getOffHandStack().getItem() == Items.LADDER;

        if (!hasLadder && autoSwitch.getValue()) {
            switchToItem(Items.LADDER);
            return;
        }

        if (!mc.options.useKey.isPressed()) {
            return;
        }

        if (hasLadder && !itemPlaced) {
            if (actionTimer.passedMs(placeDelay.getValue() * 50)) {
                placeLadder();
            }
        }
    }

    private void handleBothSpam() {
        boolean hasWaterBucket = mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET || 
                                mc.player.getOffHandStack().getItem() == Items.WATER_BUCKET;
        boolean hasBucket = mc.player.getMainHandStack().getItem() == Items.BUCKET || 
                           mc.player.getOffHandStack().getItem() == Items.BUCKET;
        boolean hasCobweb = mc.player.getMainHandStack().getItem() == Items.COBWEB || 
                           mc.player.getOffHandStack().getItem() == Items.COBWEB;

        if (!hasWaterBucket && !hasBucket && !hasCobweb && autoSwitch.getValue()) {
            switchToItem(Items.WATER_BUCKET, Items.BUCKET, Items.COBWEB);
            return;
        }

        if (!mc.options.useKey.isPressed()) {
            if (itemPlaced && shouldRetrieve && autoRetrieve.getValue()) {
                if (mc.world.getBlockState(placedPos).isLiquid()) {
                    retrieveWater();
                } else {
                    retrieveCobweb();
                }
            }
            return;
        }

        boolean needsWater = isOnGround() && mc.player.fallDistance > 2.0f;
        boolean needsCobweb = !needsWater && (isMovingFast() || isInCombat());

        if (!itemPlaced) {
            if (actionTimer.passedMs(placeDelay.getValue() * 50)) {
                if (needsWater && (hasWaterBucket || hasBucket)) {
                    if (hasWaterBucket) {
                        placeWater();
                    }
                } else if (needsCobweb && hasCobweb) {
                    placeCobweb();
                } else if (hasWaterBucket) {
                    placeWater();
                } else if (hasCobweb) {
                    placeCobweb();
                }
            }
        } else {
            ticksAfterPlace++;
            if (ticksAfterPlace >= retrieveDelay.getValue()) {
                shouldRetrieve = true;
            }

            if (shouldRetrieve && autoRetrieve.getValue()) {
                if (mc.world.getBlockState(placedPos).isLiquid() && hasBucket) {
                    retrieveWater();
                } else if (!mc.world.isAir(placedPos)) {
                    retrieveCobweb();
                }
            }
        }
    }

    private boolean isOnGround() {
        return mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos()).down()).isSolid() || 
               mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos()).down().down()).isSolid();
    }

    private boolean isMovingFast() {
        return mc.player.getVelocity().horizontalLength() > 0.2;
    }

    private boolean isInCombat() {
        return mc.player.getAttacker() != null || mc.player.hurtTime > 0;
    }

    private void switchToItem(net.minecraft.item.Item... items) {
        for (net.minecraft.item.Item item : items) {
            int itemSlot = InventoryUtility.findItemInHotBar(item).slot();
            if (itemSlot != -1) {
                if (prevSlot == -1) {
                    prevSlot = mc.player.getInventory().selectedSlot;
                }
                InventoryUtility.switchTo(itemSlot, true);
                return;
            }
        }
    }

    private void placeWater() {
        if (mc.player == null) return;

        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        BlockPos targetPos = null;

        if (mc.world.getBlockState(playerPos.down()).isSolid()) {
            targetPos = playerPos;
        } else if (mc.world.getBlockState(playerPos.down().down()).isSolid()) {
            targetPos = playerPos.down();
        }

        if (targetPos != null && mc.world.isAir(targetPos)) {
            mc.player.setPitch(90.0f);
            
            Hand hand = mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET ? 
                       Hand.MAIN_HAND : Hand.OFF_HAND;
            
            mc.interactionManager.interactItem(mc.player, hand);
            mc.player.swingHand(hand);
            
            itemPlaced = true;
            placedPos = targetPos;
            ticksAfterPlace = 0;
            actionTimer.reset();
        }
    }

    private void placeCobweb() {
        if (mc.player == null) return;

        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        BlockPos targetPos = null;

        if (mc.world.isAir(playerPos)) {
            targetPos = playerPos;
        } else if (mc.world.isAir(playerPos.down())) {
            targetPos = playerPos.down();
        }

        if (targetPos != null) {
            int cobwebSlot = InventoryUtility.findItemInHotBar(Items.COBWEB).slot();
            if (cobwebSlot != -1) {
                InteractionUtility.placeBlock(targetPos, InteractionUtility.Rotate.None, 
                    InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, 
                    cobwebSlot, false, true);
                
                itemPlaced = true;
                placedPos = targetPos;
                ticksAfterPlace = 0;
                actionTimer.reset();
            }
        }
    }

    private void placeLadder() {
        if (mc.player == null) return;

        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        BlockPos targetPos = playerPos;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            
            BlockPos wallPos = targetPos.offset(direction);
            if (mc.world.getBlockState(wallPos).isSolid() && mc.world.isAir(targetPos)) {
                int ladderSlot = InventoryUtility.findItemInHotBar(Items.LADDER).slot();
                if (ladderSlot != -1) {
                    InteractionUtility.placeBlock(targetPos, InteractionUtility.Rotate.None, 
                        InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, 
                        ladderSlot, false, true);
                    
                    itemPlaced = true;
                    placedPos = targetPos;
                    ticksAfterPlace = 0;
                    actionTimer.reset();
                    return;
                }
            }
        }
    }

    private void retrieveWater() {
        if (mc.player == null || placedPos == null) return;

        if (mc.world.getBlockState(placedPos).isLiquid()) {
            mc.player.setPitch(90.0f);
            
            Hand hand = mc.player.getMainHandStack().getItem() == Items.BUCKET ? 
                       Hand.MAIN_HAND : Hand.OFF_HAND;
            
            mc.interactionManager.interactItem(mc.player, hand);
            mc.player.swingHand(hand);
        }

        resetState();
    }

    private void retrieveCobweb() {
        if (mc.player == null || placedPos == null) return;

        if (!mc.world.isAir(placedPos)) {
            mc.interactionManager.attackBlock(placedPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        resetState();
    }

    private void resetState() {
        itemPlaced = false;
        shouldRetrieve = false;
        ticksAfterPlace = 0;
        placedPos = null;
        actionTimer.reset();

        if (prevSlot != -1) {
            InventoryUtility.switchTo(prevSlot, true);
            prevSlot = -1;
        }
    }

    @Override
    public String getDisplayInfo() {
        String modeStr = mode.getValue().toString();
        if (itemPlaced) {
            return modeStr + " Placed";
        } else if (mc.options.useKey.isPressed()) {
            return modeStr + " Ready";
        }
        return modeStr + " Waiting";
    }
}
