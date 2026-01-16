package thunder.hack.features.modules.misc;

import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AntiTooManyPacket extends Module {

    public final Setting<Integer> maxPacketsPerTick = new Setting<>("MaxPacketsPerTick", 10, 1, 100);
    public final Setting<Boolean> debug = new Setting<>("Debug", false);
    private int packetCount = 0;

    public AntiTooManyPacket() {
        super("AntiTooManyPacket", Category.MISC);
    }

    @Override
    public void onEnable() {
        packetCount = 0;
    }

    @Override
    public void onDisable() {
        packetCount = 0;
    }

    @Override
    public void onUpdate() {
        if (packetCount > maxPacketsPerTick.getValue()) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("AntiPacket > Too many packets detected!")
                    .formatted(Formatting.RED));
        }

        if (debug.getValue()) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("AntiPacket Debug > Packets this tick: " + packetCount)
                    .formatted(Formatting.GRAY));
        }

        packetCount = 0;
    }

    @Override
    public void sendPacket(Packet<?> packet) {
        packetCount++;
        if (packetCount <= maxPacketsPerTick.getValue()) {
            super.sendPacket(packet);
        } else if (debug.getValue()) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("AntiPacket Debug > Packet blocked due to limit")
                    .formatted(Formatting.YELLOW));
        }
    }
}
