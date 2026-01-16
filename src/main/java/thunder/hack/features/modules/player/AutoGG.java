package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.Timer;

import java.util.LinkedList;
import java.util.Queue;

public class AutoGG extends Module {
    private final Timer timer = new Timer();
    private final Queue<String> recentMessages = new LinkedList<>();
    private boolean waitingMode = false;

    public AutoGG() {
        super("AutoGG", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        recentMessages.clear();
        waitingMode = false;
        timer.reset();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (fullNullCheck()) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String messageText = packet.content().getString();
            String cleanText = messageText.replaceAll("§[0-9a-fk-or]", "");
            
            recentMessages.offer(cleanText);
            
            if (recentMessages.size() > 6) {
                recentMessages.poll();
            }
            
            if (!waitingMode && containsGameEnd(cleanText)) {
                mc.player.networkHandler.sendChatMessage("gg");
                waitingMode = true;
                timer.reset();
            }
        }
    }

    @Override
    public void onUpdate() {
        if (waitingMode && timer.passedS(20)) {
            waitingMode = false;
        }
    }

    private boolean containsGameEnd(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("you got:") || 
               lowerMessage.contains("thank you for playing") ||
               lowerMessage.contains("game length:");
    }
}