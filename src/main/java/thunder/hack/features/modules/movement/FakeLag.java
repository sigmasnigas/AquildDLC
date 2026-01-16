/*
package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class FakeLag extends Module {
    public final Setting<Float> basePing = new Setting<>("BasePing", 500f, 1f, 1500f);
    public final Setting<Float> auraPing = new Setting<>("AuraPing", 200f, 1f, 1500f);
    public final Setting<Boolean> simulateLag = new Setting<>("SimulateLag", true);
    public final Setting<Integer> lagDuration = new Setting<>("LagDuration", 1000, 100, 3000);
    public final Setting<RenderMode> renderMode = new Setting<>("RenderMode", RenderMode.None);
    public final Setting<Color> renderColor = new Setting<>("RenderColor", new Color(0, 255, 0, 128));
    public final Setting<Float> lineWidth = new Setting<>("LineWidth", 1.5f, 0.1f, 5.0f, v -> renderMode.getValue() == RenderMode.Box);

    private final ConcurrentHashMap<KeepAliveC2SPacket, Long> packets = new ConcurrentHashMap<>();
    private final Queue<PlayerMoveC2SPacket> movementPackets = new LinkedList<>();
    private final Timer lagTimer = new Timer();
    private boolean isLagging = false;
    private Vec3d lastPos = null;

    public FakeLag() {
        super("FakeLag", Category.MOVEMENT);
    }

    private enum RenderMode {
        None,
        Box,
        Circle
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof KeepAliveC2SPacket packet) {
            packets.put(packet, System.currentTimeMillis());
            event.cancel();
        }

        if (simulateLag.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (isLagging) {
                if (ModuleManager.aura.isEnabled() && Aura.target != null) {
                    flushMovementPackets();
                    isLagging = false;
                } else {
                    movementPackets.add(packet);
                    event.cancel();
                }
            }
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        long delay = ModuleManager.aura.isEnabled() && Aura.target != null ? auraPing.getValue().longValue() : basePing.getValue().longValue();

        new HashSet<>(packets.keySet()).forEach(packet -> {
            if (System.currentTimeMillis() - packets.get(packet) >= delay) {
                mc.getNetworkHandler().sendPacket(packet);
                packets.remove(packet);
            }
        });
    }

    @Override
    public void onUpdate() {
        if (simulateLag.getValue()) {
            if (lagTimer.passedMs(lagDuration.getValue())) {
                isLagging = !isLagging;

                if (!isLagging) {
                    flushMovementPackets();
                }

                lagTimer.reset();
            }
        }

        if (mc.player != null) {
            lastPos = isLagging ? lastPos : mc.player.getPos();
        }
    }

    private void flushMovementPackets() {
        while (!movementPackets.isEmpty()) {
            mc.getNetworkHandler().sendPacket(movementPackets.poll());
        }
    }

    @Override
    public void onDisable() {
        flushMovementPackets();
        packets.keySet().forEach(packet -> mc.getNetworkHandler().sendPacket(packet));
        packets.clear();
    }

    @EventHandler
    public void onRender3D(MatrixStack stack) {
        if (mc.player == null || mc.world == null || lastPos == null || renderMode.getValue() == RenderMode.None) return;

        if (renderMode.getValue() == RenderMode.Box) {
            Box hitBox = new Box(
                    lastPos.x - 0.3,
                    lastPos.y,
                    lastPos.z - 0.3,
                    lastPos.x + 0.3,
                    lastPos.y + 1.8,
                    lastPos.z + 0.3
            );
            Render3DEngine.drawBoxOutline(hitBox, renderColor.getValue(), lineWidth.getValue());
        }

        if (renderMode.getValue() == RenderMode.Circle) {
            ArrayList<Vec3d> points = new ArrayList<>();
            for (int i = 0; i <= 360; ++i) {
                points.add(new Vec3d(
                        lastPos.x + Math.sin(Math.toRadians(i)) * 0.5,
                        lastPos.y + 0.01,
                        lastPos.z + Math.cos(Math.toRadians(i)) * 0.5
                ));
            }

            for (int i = 0; i < points.size() - 1; ++i) {
                Render3DEngine.drawLine(points.get(i), points.get(i + 1), renderColor.getValue());
            }
        }
    }
}
*/
