package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.setting.Setting;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.client.PacketQueueManager;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Backtrack extends Module {

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Vulcan);
    private final Setting<Float> delay = new Setting<>("Delay", 150.0f, 50.0f, 500.0f, v -> mode.getValue() == Mode.Vulcan);
    private final Setting<Float> chance = new Setting<>("Chance", 100.0f, 0.0f, 100.0f);
    private final Setting<Integer> timeoutS = new Setting<>("TimeoutS", 10, 1, 30);

    private final ConcurrentLinkedQueue<DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();
    private Entity target;
    private Vec3d ghostPos;
    private Vec3d realPos;
    private long lastPacketTime = 0;
    private long targetActivationTime = 0;

    private boolean vanadiumFreezeActive = false;
    private final ConcurrentLinkedQueue<DelayedPacket> vanadiumPackets = new ConcurrentLinkedQueue<>();
    private boolean vanadiumActivatedForTarget = false;

    private Vec3d targetStartPos;
    private Vec3d targetEndPos;
    private long interpolationStartTime;

    public enum Mode {
        Vulcan, Vanadium
    }

    public Backtrack() {
        super("Backtrack", Module.Category.COMBAT);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        resetTarget();
        if (mc.world != null) {
            mc.world.getEntities().forEach(entity -> {
                if (entity instanceof LivingEntity) {
                    entity.lastRenderX = entity.getX();
                    entity.lastRenderY = entity.getY();
                    entity.lastRenderZ = entity.getZ();
                }
            });
        }
    }

    @EventHandler
    public void onAttack(EventAttack event) {
        if (!isEnabled()) return;
        if (Math.random() * 100.0 > chance.getValue()) return;

        Entity ent = event.getEntity();
        if (ent == null || !(ent instanceof LivingEntity) || ent == mc.player) return;

        if (target == null || target.getId() != ent.getId()) {
            target = ent;
            ghostPos = ent.getPos();
            realPos = ent.getPos();
            vanadiumActivatedForTarget = false;
            targetActivationTime = System.currentTimeMillis();
        }

        if (target != null && ghostPos != null && event.isPre()) {
            if (mode.getValue() == Mode.Vulcan) {
                targetStartPos = target.getPos();
                targetEndPos = ghostPos;
                interpolationStartTime = System.currentTimeMillis();
            }
            target.setPos(ghostPos.x, ghostPos.y, ghostPos.z);
            target.lastRenderX = ghostPos.x; target.lastRenderY = ghostPos.y; target.lastRenderZ = ghostPos.z;
            target.prevX = ghostPos.x; target.prevY = ghostPos.y; target.prevZ = ghostPos.z;
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (!isEnabled()) return;

        if (target != null && targetActivationTime != 0) {
            if (System.currentTimeMillis() - targetActivationTime > timeoutS.getValue() * 1000L) {
                resetTarget();
                return;
            }
        }

        if (target != null && (target.isRemoved() || !target.isAlive())) {
            resetTarget();
            return;
        }

        if (target != null && ghostPos != null) {
            updateTargetSmoothly();
        }

        flushOldPackets();
    }

    private void resetTarget() {
        if (target != null && realPos != null) {
            target.setPos(realPos.x, realPos.y, realPos.z);
            target.lastRenderX = realPos.x; target.lastRenderY = realPos.y; target.lastRenderZ = realPos.z;
            target.prevX = realPos.x; target.prevY = realPos.y; target.prevZ = realPos.z;
        }
        clear();
        flushAllPackets();
        flushAllVanadiumPackets();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (target != null) {
            double d = target.getWidth() / 2.0;
            double h = target.getHeight();
            if (realPos != null) {
                Box realBox = new Box(-d, 0.0, -d, d, h, d).offset(realPos);
                Render3DEngine.drawBoxOutline(realBox, new Color(255, 0, 0, 255), 2.0f);
            }
            if (ghostPos != null) {
                Box ghostBox = new Box(-d, 0.0, -d, d, h, d).offset(ghostPos);
                Render3DEngine.drawBoxOutline(ghostBox, new Color(0, 100, 255, 150), 1.5f);
            }
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!isEnabled() || target == null) return;

        Packet<?> packet = event.getPacket();

        // Pakiety, które zawsze przepuszczamy (Dźwięki, HP, Animacje) - aby słyszeć hity
        if (packet instanceof PlaySoundS2CPacket || packet instanceof EntityAnimationS2CPacket || packet instanceof EntityStatusS2CPacket) {
            return; 
        }

        if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof DisconnectS2CPacket) {
            resetTarget();
            return;
        }

        // SPRAWDZENIE CZY PAKIET DOTYCZY TARGETA
        boolean isTargetPacket = false;
        if (packet instanceof EntityPositionS2CPacket p && p.getId() == target.getId()) isTargetPacket = true;
        else if (packet instanceof EntityS2CPacket p) {
            Entity ent = p.getEntity(mc.world);
            if (ent != null && ent.getId() == target.getId()) isTargetPacket = true;
        } else if (packet instanceof EntityVelocityUpdateS2CPacket p && p.getId() == target.getId()) isTargetPacket = true;

        // Jeśli to NIE JEST pakiet targeta, nie dotykamy go (inni gracze chodzą płynnie)
        if (!isTargetPacket && (packet instanceof EntityPositionS2CPacket || packet instanceof EntityS2CPacket)) {
            return;
        }

        // Logika Real Pos (bez zmian w obliczeniach)
        if (isTargetPacket) {
            if (packet instanceof EntityPositionS2CPacket posPacket) {
                realPos = new Vec3d(posPacket.getX(), posPacket.getY(), posPacket.getZ());
            } else if (packet instanceof EntityS2CPacket entityPacket) {
                Vec3d base = realPos != null ? realPos : target.getPos();
                realPos = new Vec3d(base.x + entityPacket.getDeltaX() / 4096.0, base.y + entityPacket.getDeltaY() / 4096.0, base.z + entityPacket.getDeltaZ() / 4096.0);
            }
        }

        // Vanadium logic
        if (mode.getValue() == Mode.Vanadium && isTargetPacket) {
            if (!vanadiumActivatedForTarget) {
                vanadiumFreezeActive = true;
                vanadiumActivatedForTarget = true;
            }
            vanadiumPackets.add(new DelayedPacket(packet, System.currentTimeMillis()));
            event.cancel();
            return;
        }

        // Vulcan logic
        if (mode.getValue() == Mode.Vulcan && isTargetPacket) {
            delayedPackets.add(new DelayedPacket(packet, System.currentTimeMillis()));
            lastPacketTime = System.currentTimeMillis();
            event.cancel();
        }
    }

    private void flushOldPackets() {
        long currentTime = System.currentTimeMillis();
        if (mode.getValue() == Mode.Vulcan && !delayedPackets.isEmpty()) {
            delayedPackets.removeIf(delayedPacket -> {
                if (currentTime - delayedPacket.timestamp >= delay.getValue()) {
                    try { PacketQueueManager.processIncomingPacket(delayedPacket.packet); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            });
        }
    }

    private void flushAllPackets() {
        while (!delayedPackets.isEmpty()) {
            DelayedPacket dp = delayedPackets.poll();
            if (dp != null) PacketQueueManager.processIncomingPacket(dp.packet);
        }
    }

    private void flushAllVanadiumPackets() {
        while (!vanadiumPackets.isEmpty()) {
            DelayedPacket dp = vanadiumPackets.poll();
            if (dp != null) PacketQueueManager.processIncomingPacket(dp.packet);
        }
        vanadiumFreezeActive = false;
    }

    private void clear() {
        target = null; ghostPos = null; realPos = null; lastPacketTime = 0;
        targetStartPos = null; targetEndPos = null; interpolationStartTime = 0;
        targetActivationTime = 0; vanadiumFreezeActive = false; vanadiumActivatedForTarget = false;
    }

    private void updateTargetSmoothly() {
        if (target == null) return;
        if (mode.getValue() == Mode.Vanadium && vanadiumFreezeActive && ghostPos != null) {
            target.setPos(ghostPos.x, ghostPos.y, ghostPos.z);
            target.prevX = ghostPos.x; target.prevY = ghostPos.y; target.prevZ = ghostPos.z;
            target.lastRenderX = ghostPos.x; target.lastRenderY = ghostPos.y; target.lastRenderZ = ghostPos.z;
            target.setVelocity(0, 0, 0);
        } else if (mode.getValue() == Mode.Vulcan) {
            if (targetStartPos == null || targetEndPos == null) return;
            long elapsed = System.currentTimeMillis() - interpolationStartTime;
            if (elapsed < 1000) {
                float progress = elapsed / 1000.0f;
                progress = progress * progress * (3.0f - 2.0f * progress);
                Vec3d inter = targetStartPos.lerp(targetEndPos, progress);
                target.setPos(inter.x, inter.y, inter.z);
                target.prevX = inter.x; target.prevY = inter.y; target.prevZ = inter.z;
                target.lastRenderX = inter.x; target.lastRenderY = inter.y; target.lastRenderZ = inter.z;
            }
        }
    }

    private static class DelayedPacket {
        public final Packet<?> packet;
        public final long timestamp;
        public DelayedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet; this.timestamp = timestamp;
        }
    }
}
