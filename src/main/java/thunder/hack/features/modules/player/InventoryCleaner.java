package thunder.hack.features.modules.player;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.TickTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InventoryCleaner extends Module {
    public InventoryCleaner() {
        super("InventoryCleaner", Category.PLAYER);
    }

    public final Setting<ItemSelectSetting> items = new Setting<>("Items", new ItemSelectSetting(new ArrayList<>()));
    private final Setting<DropWhen> dropWhen = new Setting<>("DropWhen", DropWhen.NotInInventory);
    private final Setting<Integer> delay = new Setting<>("Delay", 20, 0, 200);
    private final Setting<Boolean> cleanChests = new Setting<>("CleanChests", false);

    private final TickTimer actionTimer = new TickTimer();
    private final Map<Integer, Integer> itemReceivedTicks = new HashMap<>();
    private boolean dirty;

    public void onRender3D(MatrixStack stack) {
        boolean inInv = mc.currentScreen instanceof InventoryScreen;

        trackInventoryChanges();

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest && cleanChests.getValue())
            for (int i = 0; i < chest.getInventory().size(); i++) {
                Slot slot = chest.getSlot(i);
                if (slot.hasStack() && dropThisShit(slot.getStack()) && !(mc.currentScreen.getTitle().getString().contains("Аукцион") || mc.currentScreen.getTitle().getString().contains("покупки")))
                    if (actionTimer.every(2)) {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.THROW, mc.player);
                        dirty = true;
                    }
            }

        if (dropWhen.getValue() == DropWhen.Inventory && !inInv) return;
        if (dropWhen.getValue() == DropWhen.NotInInventory && inInv) return;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack itemFromslot = mc.player.getInventory().getStack(slot);
            if (dropThisShit(itemFromslot) && shouldDropItem(slot))
                drop(slot);
        }

        if (dirty && actionTimer.passedTicks(5)) {
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            debug("after click cleaning...");
            dirty = false;
        }
    }

    private void trackInventoryChanges() {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack currentStack = mc.player.getInventory().getStack(slot);

            if (!currentStack.isEmpty() && dropThisShit(currentStack)) {
                if (!itemReceivedTicks.containsKey(slot)) {
                    itemReceivedTicks.put(slot, mc.player.age);
                }
            } else {
                itemReceivedTicks.remove(slot);
            }
        }
    }

    private boolean shouldDropItem(int slot) {
        Integer receivedTick = itemReceivedTicks.get(slot);
        if (receivedTick == null) return false;

        int ticksPassed = mc.player.age - receivedTick;
        return ticksPassed >= delay.getValue();
    }

    private void drop(int slot) {

        if (actionTimer.every(2)) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 1, SlotActionType.THROW, mc.player);
            itemReceivedTicks.remove(slot);
            dirty = true;
        }
    }

    private boolean dropThisShit(ItemStack stack) {
        return items.getValue().getItemsById().contains(stack.getItem().getTranslationKey().replace("block.minecraft.", "").replace("item.minecraft.", ""));
    }

    public enum DropWhen {
        Inventory, Always, NotInInventory
    }
}
