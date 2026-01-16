package thunder.hack.features.modules.misc;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.MovementUtility;

import java.util.Arrays;
import java.util.List;

public class AutoChange extends Module {
    private final Setting<Boolean> requireOpenEq = new Setting<>("RequireOpenEQ", true);
    private final Setting<Integer> delay = new Setting<>("Delay", 100, 0, 1000);
    private final Setting<Boolean> requireStill = new Setting<>("RequireStill", true);
    private final Setting<Boolean> debug = new Setting<>("Debug", true);
    
    private final Timer timer = new Timer();
    private final Timer debugTimer = new Timer();
    
    private static final List<Item> WOOD_LOGS = Arrays.asList(
        Items.OAK_LOG,
        Items.SPRUCE_LOG,
        Items.BIRCH_LOG,
        Items.JUNGLE_LOG,
        Items.ACACIA_LOG,
        Items.DARK_OAK_LOG,
        Items.MANGROVE_LOG,
        Items.CHERRY_LOG,
        Items.CRIMSON_STEM,
        Items.WARPED_STEM,
        Items.STRIPPED_OAK_LOG,
        Items.STRIPPED_SPRUCE_LOG,
        Items.STRIPPED_BIRCH_LOG,
        Items.STRIPPED_JUNGLE_LOG,
        Items.STRIPPED_ACACIA_LOG,
        Items.STRIPPED_DARK_OAK_LOG,
        Items.STRIPPED_MANGROVE_LOG,
        Items.STRIPPED_CHERRY_LOG,
        Items.STRIPPED_CRIMSON_STEM,
        Items.STRIPPED_WARPED_STEM,
        Items.OAK_WOOD,
        Items.SPRUCE_WOOD,
        Items.BIRCH_WOOD,
        Items.JUNGLE_WOOD,
        Items.ACACIA_WOOD,
        Items.DARK_OAK_WOOD,
        Items.MANGROVE_WOOD,
        Items.CHERRY_WOOD,
        Items.CRIMSON_HYPHAE,
        Items.WARPED_HYPHAE,
        Items.STRIPPED_OAK_WOOD,
        Items.STRIPPED_SPRUCE_WOOD,
        Items.STRIPPED_BIRCH_WOOD,
        Items.STRIPPED_JUNGLE_WOOD,
        Items.STRIPPED_ACACIA_WOOD,
        Items.STRIPPED_DARK_OAK_WOOD,
        Items.STRIPPED_MANGROVE_WOOD,
        Items.STRIPPED_CHERRY_WOOD,
        Items.STRIPPED_CRIMSON_HYPHAE,
        Items.STRIPPED_WARPED_HYPHAE
    );
    
    public AutoChange() {
        super("AutoChange", Category.MISC);
    }
    
    @Override
    public void onUpdate() {
        if (fullNullCheck()) return;
        
        boolean isCraftingScreen = mc.player.currentScreenHandler instanceof CraftingScreenHandler;
        boolean isPlayerScreen = mc.player.currentScreenHandler instanceof PlayerScreenHandler;
        
        if (requireOpenEq.getValue() && !isCraftingScreen && !isPlayerScreen) {
            return;
        }
        
        if (requireStill.getValue() && MovementUtility.isMoving()) {
            return;
        }
        
        if (!timer.every(delay.getValue())) {
            return;
        }
        
        if (isCraftingScreen || isPlayerScreen) {
            if (mc.player.currentScreenHandler.getSlot(0).hasStack()) {

                
                try {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
                    Managers.NOTIFICATION.publicity("", "Drewno przerobione!", 5, Notification.Type.WARNING);

                    return;
                } catch (Exception e) {
                }
            }
            
            int craftingStartSlot = 1;
            int craftingEndSlot = 5;
            
            if (isCraftingScreen) {
                craftingStartSlot = 1;
                craftingEndSlot = 10;
            }
            
            int woodSlotInInventory = findWoodInInventory();
            
            if (woodSlotInInventory != -1) {
                int emptyCraftingSlot = findEmptyCraftingSlot(craftingStartSlot, craftingEndSlot);
                
                if (emptyCraftingSlot != -1) {
                    if (debug.getValue()) {
                        Item woodItem = mc.player.getInventory().getStack(woodSlotInInventory).getItem();
                        sendDebugMessage("Przenoszę drewno ze slotu " + woodSlotInInventory + " do crafting slotu " + emptyCraftingSlot + ": " + woodItem.toString());
                    }
                    
                    int inventorySlotId = woodSlotInInventory < 9 ? 36 + woodSlotInInventory : woodSlotInInventory;
                    
                    try {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, inventorySlotId, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, emptyCraftingSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, inventorySlotId, 0, SlotActionType.PICKUP, mc.player);
                        
                        if (debug.getValue()) {
                            sendDebugMessage("Przeniesiono drewno do crafting slotu!");
                        }
                    } catch (Exception e) {
                        if (debug.getValue()) {
                            sendDebugMessage("Błąd przy przenoszeniu: " + e.getMessage());
                        }
                    }
                } else {
                    if (debug.getValue() && debugTimer.every(3000)) {
                        sendDebugMessage("Brak wolnych slotów w craftingu!");
                    }
                }
            } else {
                if (debug.getValue() && debugTimer.every(3000)) {
                    sendDebugMessage("Brak drewna w inventory!");
                }
            }
        }
    }
    
    private int findWoodInInventory() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) continue;
            
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (WOOD_LOGS.contains(item)) {
                return i;
            }
        }
        return -1;
    }
    
    private int findEmptyCraftingSlot(int startSlot, int endSlot) {
        for (int i = startSlot; i < endSlot; i++) {
            if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) {
                return i;
            }
        }
        return -1;
    }
    
    private void sendDebugMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[" + Formatting.BLUE + "AutoChange" + Formatting.RESET + "] " + message), false);
        }
    }
}
