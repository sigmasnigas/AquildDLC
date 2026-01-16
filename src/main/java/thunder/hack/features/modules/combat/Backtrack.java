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

    private final ConcurrentLinkedQueue<DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();
    private Entity target;
    private Vec3d ghostPos;
    private Vec3d realPos;
    private long lastPacketTime = 0;
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
        if (target != null && realPos != null) {
            target.setPos(realPos.x, realPos.y, realPos.z);
            target.lastRenderX = realPos.x;
            target.lastRenderY = realPos.y;
            target.lastRenderZ = realPos.z;
            target.prevX = realPos.x;
            target.prevY = realPos.y;
            target.prevZ = realPos.z;
        }

        clear();
        flushAllPackets();
        flushAllVanadiumPackets();

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

        if (Math.random() * 100.0 > chance.getValue()) {
            return;
        }

        Entity ent = event.getEntity();

        if (ent == null || !(ent instanceof LivingEntity) || ent == mc.player) {
            return;
        }

        if (target == null || target.getId() != ent.getId()) {
            target = ent;
            ghostPos = ent.getPos();
            realPos = ent.getPos();
            vanadiumActivatedForTarget = false;
        }

        if (mode.getValue() == Mode.Vulcan && target != null && ghostPos != null && event.isPre()) {
            targetStartPos = target.getPos();
            targetEndPos = ghostPos;
            interpolationStartTime = System.currentTimeMillis();

            target.setPos(ghostPos.x, ghostPos.y, ghostPos.z);
            target.lastRenderX = ghostPos.x;
            target.lastRenderY = ghostPos.y;
            target.lastRenderZ = ghostPos.z;
            target.prevX = ghostPos.x;
            target.prevY = ghostPos.y;
            target.prevZ = ghostPos.z;
        }

        if (mode.getValue() == Mode.Vanadium && target != null && ghostPos != null && event.isPre()) {
            target.setPos(ghostPos.x, ghostPos.y, ghostPos.z);
            target.prevX = ghostPos.x;
            target.prevY = ghostPos.y;
            target.prevZ = ghostPos.z;
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (!isEnabled()) return;

        if (target != null && (target.isRemoved() || !target.isAlive())) {
            if (realPos != null) {
                target.setPos(realPos.x, realPos.y, realPos.z);
                target.lastRenderX = realPos.x;
                target.lastRenderY = realPos.y;
                target.lastRenderZ = realPos.z;
                target.prevX = realPos.x;
                target.prevY = realPos.y;
                target.prevZ = realPos.z;
            }

            clear();
            flushAllPackets();
            flushAllVanadiumPackets();
            return;
        }

        if (target != null && ghostPos != null) {
            updateTargetSmoothly();
        }

        flushOldPackets();
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
    public void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled()) return;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!isEnabled()) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof DisconnectS2CPacket) {
            clear();
            flushAllPackets();
            flushAllVanadiumPackets();
            return;
        }

        if (mode.getValue() == Mode.Vanadium && target != null && !vanadiumActivatedForTarget) {
            activateVanadiumFreeze();
            vanadiumActivatedForTarget = true;
        }

        if (mode.getValue() == Mode.Vanadium && vanadiumActivatedForTarget && target != null) {
            vanadiumPackets.add(new DelayedPacket(packet, System.currentTimeMillis()));

            event.cancel();
            return;
        }

        if (mode.getValue() != Mode.Vulcan) return;
        if (target == null) return;

        boolean isMovePacket = false;
        Vec3d newPos = null;

        if (packet instanceof EntityPositionS2CPacket posPacket && posPacket.getId() == target.getId()) {
            isMovePacket = true;
            newPos = new Vec3d(posPacket.getX(), posPacket.getY(), posPacket.getZ());
        }
        else if (packet instanceof EntityS2CPacket entityPacket) {
            Entity packetEntity = entityPacket.getEntity(mc.world);
            if (packetEntity != null && packetEntity.getId() == target.getId()) {
                isMovePacket = true;
                Vec3d base = realPos != null ? realPos : target.getPos();
                newPos = new Vec3d(
                        base.x + entityPacket.getDeltaX() / 4096.0,
                        base.y + entityPacket.getDeltaY() / 4096.0,
                        base.z + entityPacket.getDeltaZ() / 4096.0
                );
            }
        }
        else if (packet instanceof EntityVelocityUpdateS2CPacket velPacket && velPacket.getId() == target.getId()) {
            isMovePacket = true;
            newPos = realPos != null ? realPos : target.getPos();
        }

        if (!isMovePacket) return;

        realPos = newPos;

        event.cancel();
        delayedPackets.add(new DelayedPacket(packet, System.currentTimeMillis()));
        lastPacketTime = System.currentTimeMillis();
    }

    private void flushOldPackets() {
        long currentTime = System.currentTimeMillis();

        if (mode.getValue() == Mode.Vulcan && !delayedPackets.isEmpty()) {
            flushPacketsNormalMode(currentTime);
        }
    }

    private void flushPacketsNormalMode(long currentTime) {
        delayedPackets.removeIf(delayedPacket -> {
            if (currentTime - delayedPacket.timestamp >= delay.getValue()) {
                try {
                    PacketQueueManager.processIncomingPacket(delayedPacket.packet);
                } catch (Exception e) {
                }
                return true;
            }
            return false;
        });
    }

    private void flushAllPackets() {
        while (!delayedPackets.isEmpty()) {
            DelayedPacket delayedPacket = delayedPackets.poll();
            try {
                PacketQueueManager.processIncomingPacket(delayedPacket.packet);
            } catch (Exception e) {
            }
        }
    }

    private void flushAllVanadiumPackets() {
        while (!vanadiumPackets.isEmpty()) {
            DelayedPacket delayedPacket = vanadiumPackets.poll();

            try {
                PacketQueueManager.processIncomingPacket(delayedPacket.packet);
            } catch (Exception e) {
            }
        }
        vanadiumFreezeActive = false;
    }

    private void activateVanadiumFreeze() {
        if (!vanadiumFreezeActive) {
            vanadiumFreezeActive = true;
        }
    }

    private void clear() {
        target = null;
        ghostPos = null;
        realPos = null;
        lastPacketTime = 0;
        targetStartPos = null;
        targetEndPos = null;
        interpolationStartTime = 0;

        vanadiumFreezeActive = false;
        vanadiumActivatedForTarget = false;
    }

    public boolean isTargetInReach(Entity entity) {
        if (!isEnabled()) return false;
        if (target == null || target.getId() != entity.getId()) return false;
        if (ghostPos == null) return false;

        Vec3d playerPos = mc.player.getPos();
        double distance = playerPos.distanceTo(ghostPos);
        double interactionRange = mc.player.getEntityInteractionRange();
        double maxReach = interactionRange + 3.0;

        return distance <= maxReach;
    }

    public boolean shouldExtendReach(Entity entity) {
        return isEnabled() && target != null && target.getId() == entity.getId() && ghostPos != null;
    }

    public Vec3d getGhostPosition(Entity entity) {
        if (!isEnabled() || target == null || target.getId() != entity.getId()) return null;
        return ghostPos;
    }

    public boolean hasTarget(Entity entity) {
        return isEnabled() && target != null && target.getId() == entity.getId();
    }

    private void updateTargetSmoothly() {
        if (target == null) return;

        if (mode.getValue() == Mode.Vanadium && vanadiumFreezeActive && ghostPos != null) {
            target.setPos(ghostPos.x, ghostPos.y, ghostPos.z);
            target.prevX = ghostPos.x;
            target.prevY = ghostPos.y;
            target.prevZ = ghostPos.z;
            target.lastRenderX = ghostPos.x;
            target.lastRenderY = ghostPos.y;
            target.lastRenderZ = ghostPos.z;
            target.setVelocity(0, 0, 0);
            return;
        }

        if (mode.getValue() == Mode.Vulcan) {
            if (targetStartPos == null || targetEndPos == null) {
                Vec3d currentPos = target.getPos();
                if (ghostPos != null && currentPos.distanceTo(ghostPos) > 0.1) {
                    target.setPos(ghostPos.x, ghostPos.y, ghostPos.z);
                    target.lastRenderX = ghostPos.x;
                    target.lastRenderY = ghostPos.y;
                    target.lastRenderZ = ghostPos.z;
                    target.setVelocity(0, 0, 0);
                    target.prevX = ghostPos.x;
                    target.prevY = ghostPos.y;
                    target.prevZ = ghostPos.z;
                }
                return;
            }

            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - interpolationStartTime;

            if (elapsed < 1000) {
                float progress = elapsed / 1000.0f;
                progress = smoothStep(progress);

                Vec3d interpolatedPos = targetStartPos.lerp(targetEndPos, progress);

                target.setPos(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
                target.lastRenderX = interpolatedPos.x;
                target.lastRenderY = interpolatedPos.y;
                target.lastRenderZ = interpolatedPos.z;
                target.setVelocity(0, 0, 0);
                target.prevX = interpolatedPos.x;
                target.prevY = interpolatedPos.y;
                target.prevZ = interpolatedPos.z;
            } else {
                target.setPos(targetEndPos.x, targetEndPos.y, targetEndPos.z);
                targetStartPos = null;
                targetEndPos = null;
            }
        }
    }

    private float smoothStep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    @Override
    public String getDisplayInfo() {
        if (vanadiumFreezeActive) {
            return vanadiumPackets.size() + " VANADIUM";
        } else {
            return delayedPackets.size() + " (" + mode.getValue().name() + ")";
        }
    }

    private static class DelayedPacket {
        public final Packet<?> packet;
        public final long timestamp;

        public DelayedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}