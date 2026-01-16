package thunder.hack.utility.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.QueuePacketEvent;
import thunder.hack.utility.Timer;


import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

import static thunder.hack.ThunderHack.EVENT_BUS;
import static thunder.hack.ThunderHack.mc;


public class PacketQueueManager {
    public static final PacketQueueManager INSTANCE = new PacketQueueManager();
    private static final ConcurrentLinkedQueue<PacketSnapshot> packetQueue = new ConcurrentLinkedQueue<>();
    private static final Timer flushTimer = new Timer();
    private static boolean isActive = false;

    public static class PacketSnapshot {
        public final Packet<?> packet;
        public final TransferOrigin origin;
        public final long timestamp;

        public PacketSnapshot(Packet<?> packet, TransferOrigin origin) {
            this.packet = packet;
            this.origin = origin;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public enum TransferOrigin {
        OUTGOING,
        INCOMING
    }

    public enum Action {
        QUEUE,
        PASS,
        FLUSH
    }

    public static void add(Packet<?> packet, TransferOrigin origin) {
        packetQueue.add(new PacketSnapshot(packet, origin));
    }

    public static void flush(Predicate<PacketSnapshot> flushWhen) {
        packetQueue.removeIf(snapshot -> {
            if (flushWhen.test(snapshot)) {
                flushSnapshot(snapshot);
                return true;
            }
            return false;
        });
    }

    public static void flush(int count) {
        int counter = 0;
        var iterator = packetQueue.iterator();

        while (iterator.hasNext() && counter < count) {
            PacketSnapshot snapshot = iterator.next();

            if (snapshot.packet instanceof PlayerMoveC2SPacket movePacket && movePacket.changesPosition()) {
                counter++;
            }

            flushSnapshot(snapshot);
            iterator.remove();
        }
    }

    public static void flushAll() {
        while (!packetQueue.isEmpty()) {
            PacketSnapshot snapshot = packetQueue.poll();
            flushSnapshot(snapshot);
        }
    }

    public static void setActive(boolean active) {
        isActive = active;
        if (!active) {
            flushAll();
        }
    }

    public static boolean isActive() {
        return isActive;
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!isActive || mc.player == null) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (packet instanceof ChatMessageC2SPacket ||
                packet instanceof TeleportConfirmC2SPacket ||
                packet instanceof KeepAliveC2SPacket ||
                packet instanceof AdvancementTabC2SPacket ||
                packet instanceof ClientStatusC2SPacket) {
            return;
        }

        QueuePacketEvent queueEvent = new QueuePacketEvent(packet, QueuePacketEvent.TransferOrigin.OUTGOING);
        EVENT_BUS.post(queueEvent);

        if (queueEvent.getAction() == Action.QUEUE) {
            event.cancel();
            add(packet, TransferOrigin.OUTGOING);
        } else if (queueEvent.getAction() == Action.FLUSH) {
            flushAll();
        }
    }

    public static void cancel() {
        Vec3d[] positions = getPositions();
        if (positions.length > 0 && mc.player != null) {
            Vec3d firstPos = positions[0];
            mc.player.setPos(firstPos.x, firstPos.y, firstPos.z);
        }

        packetQueue.removeIf(snapshot -> {
            if (!(snapshot.packet instanceof PlayerMoveC2SPacket)) {
                flushSnapshot(snapshot);
                return true;
            }
            return false;
        });

        packetQueue.clear();
    }

    private static void flushSnapshot(PacketSnapshot snapshot) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(snapshot.packet);
        }
    }

    public static boolean isLagging() {
        return !packetQueue.isEmpty();
    }

    public static int size() {
        return packetQueue.size();
    }

    public static boolean isAboveTime(long delay) {
        PacketSnapshot first = packetQueue.peek();
        if (first == null) return false;
        return System.currentTimeMillis() - first.timestamp >= delay;
    }

    public static Vec3d[] getPositions() {
        return packetQueue.stream()
                .map(snapshot -> snapshot.packet)
                .filter(PlayerMoveC2SPacket.class::isInstance)
                .map(PlayerMoveC2SPacket.class::cast)
                .filter(PlayerMoveC2SPacket::changesPosition)
                .map(packet -> new Vec3d(packet.getX(0), packet.getY(0), packet.getZ(0)))
                .toArray(Vec3d[]::new);
    }

    public static Vec3d getServerPosition() {
        Vec3d[] positions = getPositions();
        return positions.length > 0 ? positions[0] : null;
    }


    @SuppressWarnings("unchecked")
    public static void processIncomingPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() != null) {
            try {
                ((Packet<net.minecraft.client.network.ClientPlayNetworkHandler>) packet).apply(mc.getNetworkHandler());
            } catch (Exception e) {
            }
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (!isActive) return;

        if (flushTimer.passedMs(5000)) {
            if (!packetQueue.isEmpty()) {
                flushAll();
            }
            flushTimer.reset();
        }
    }
}