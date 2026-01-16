// todo: Fix this

/*
package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import thunder.hack.features.modules.Module;
import thunder.hack.events.impl.PacketEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Debuger extends Module {
    private final Map<String, Set<Integer>> debugPatterns = new HashMap<>();
    private final Map<String, Integer> detectedCounts = new HashMap<>();
    private final Map<String, Integer> totalChecks = new HashMap<>();
    private final Map<String, Long> packetTimings = new HashMap<>();
    private boolean scanning = false;

    public Debuger() {
        super("Debuger", Category.CLIENT);
        loadDebugPatterns();
    }

    private void loadDebugPatterns() {
        debugPatterns.put("Taka", Set.of(-23767, -23766, -23765, -23764, -23763, -23762, -23761));
        debugPatterns.put("Vulcan", Set.of(-23767, -23766, -23765, -23764, -23763, -23762));
        debugPatterns.put("GrimAC", Set.of(0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13));
        debugPatterns.put("Matrix", Set.of(32767, 32766, 32765, 32764, 32763, 32762, 32761));
        debugPatterns.put("Karhu", Set.of(-3000, -3001, -3002, -3003, -3004, -3005, -3006));
        debugPatterns.put("Verus", Set.of(-4000, -3999, -3998));
        debugPatterns.put("Frequency", Set.of(32767));
        debugPatterns.put("IntaveOld", Set.of(-1, -2));
        debugPatterns.put("IntaveLatest", Set.of(-20, -30));
        debugPatterns.put("Polar", Set.of(-1520, -250, -251, -252));
        debugPatterns.put("Sparky", Set.of(5401, -4350, 5402, -4351, 5403, -4352));
    }

    @Override
    public void onEnable() {
        sendMessage("Trwa skanowanie anticheatów...");
        scanning = true;
        detectedCounts.clear();
        totalChecks.clear();
        packetTimings.clear();

        for (String antiCheat : debugPatterns.keySet()) {
            detectedCounts.put(antiCheat, 0);
            totalChecks.put(antiCheat, 0);
        }

        new Thread(() -> {
            while (scanning) {
                sendTestPackets();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof KeepAliveS2CPacket packet) {
            int id = (int) packet.getId();
            for (Map.Entry<String, Set<Integer>> entry : debugPatterns.entrySet()) {
                String antiCheat = entry.getKey();
                if (entry.getValue().contains(id)) {
                    detectedCounts.put(antiCheat, detectedCounts.get(antiCheat) + 1);
                    sendMessage("Anticheat found! ID: " + id + " | Name: " + antiCheat + " | Method: Debug ID");
                }
                totalChecks.put(antiCheat, totalChecks.get(antiCheat) + 1);
            }
        }

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            checkBehavior("Vulcan", "Frequent teleport packets", 30);
            checkBehavior("GrimAC", "Extreme position values", 10);
            checkBehavior("Matrix", "Fast response time (<50ms)", 20);
            checkBehavior("Karhu", "Velocity-related behavior", 25);
            checkBehavior("Verus", "Randomized packet IDs", 40);
            checkBehavior("Frequency", "Constant packet ID patterns", 35);
            checkBehavior("IntaveOld", "Repeating low value patterns", 15);
            checkBehavior("IntaveLatest", "Random high-low packet values", 20);
            checkBehavior("Polar", "Decreasing random negative values", 30);
            checkBehavior("Sparky", "Alternating positive and negative IDs", 25);
        }

        if (event.getPacket() instanceof KeepAliveS2CPacket packet) {
            long currentTime = System.currentTimeMillis();
            if (packetTimings.containsKey("KeepAlive")) {
                long diff = currentTime - packetTimings.get("KeepAlive");
                if (diff < 50) {
                    checkBehavior("Matrix", "Fast response time (<50ms)", 20);
                }
            }
            packetTimings.put("KeepAlive", currentTime);
        }
    }

    private void checkBehavior(String antiCheat, String method, int chance) {
        detectedCounts.put(antiCheat, detectedCounts.get(antiCheat) + chance);
        totalChecks.put(antiCheat, totalChecks.get(antiCheat) + 100);
        sendMessage("Anticheat found! Name: " + antiCheat + " | Method: " + method);
    }

    private void sendTestPackets() {
        try {
            mc.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(
                            mc.player.getX(),
                            mc.player.getY() + 10,
                            mc.player.getZ(),
                            false
                    )
            );
            checkBehavior("GrimAC", "Test packet blocked player", 50);
        } catch (Exception e) {
            sendMessage("Błąd podczas wysyłania testowych pakietów!");
        }
    }

    @Override
    public void onDisable() {
        scanning = false;
        sendMessage("Anti cheat on this server:");

        for (Map.Entry<String, Integer> entry : detectedCounts.entrySet()) {
            String antiCheat = entry.getKey();
            int detected = entry.getValue();
            int total = totalChecks.getOrDefault(antiCheat, 1);
            int chance = (detected * 100) / total;

            String color = chance >= 50 ? "§a" : "§c";
            sendMessage(color + "- " + antiCheat + " | Chance: " + chance + "% | Method: Multiple methods");
        }
    }
}
*/
