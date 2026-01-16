package thunder.hack.features.modules.player;

import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class FastUse extends Module {
    public FastUse() {
        super("FastUse", Category.PLAYER);
    }

    private final Setting<Integer> delay = new Setting<>("Delay", 0, 0, 5);
    public Setting<Boolean> blocks = new Setting<>("Blocks", false);
    public Setting<Boolean> crystals = new Setting<>("Crystals", false);
    public Setting<Boolean> xp = new Setting<>("XP", false);
    public Setting<Boolean> all = new Setting<>("All", true);

    public Setting<Boolean> fastSlotEnabled = new Setting<>("FastSlot", false);
    public Setting<Integer> slot = new Setting<>("Slot", 1, 1, 9, v -> fastSlotEnabled.getValue());

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        Item currentItem = mc.player.getMainHandStack().getItem();

        if (fastSlotEnabled.getValue() && mc.player.getInventory().selectedSlot + 1 != slot.getValue()) return;

        if (check(currentItem) && ((IMinecraftClient) mc).getUseCooldown() > delay.getValue()) {
            ((IMinecraftClient) mc).setUseCooldown(delay.getValue());
        }
    }

    public boolean check(Item item) {
        return (item instanceof BlockItem && blocks.getValue())
                || (item == Items.END_CRYSTAL && crystals.getValue())
                || (item == Items.EXPERIENCE_BOTTLE && xp.getValue())
                || (all.getValue());
    }
}
