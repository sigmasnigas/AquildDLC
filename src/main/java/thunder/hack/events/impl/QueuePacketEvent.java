package thunder.hack.events.impl;

import net.minecraft.network.packet.Packet;
import thunder.hack.events.Event;
import thunder.hack.utility.client.PacketQueueManager;


public class QueuePacketEvent extends Event {
    private final Packet<?> packet;
    private final TransferOrigin origin;
    private PacketQueueManager.Action action = PacketQueueManager.Action.PASS;

    public QueuePacketEvent(Packet<?> packet, TransferOrigin origin) {
        this.packet = packet;
        this.origin = origin;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public TransferOrigin getOrigin() {
        return origin;
    }

    public PacketQueueManager.Action getAction() {
        return action;
    }

    public void setAction(PacketQueueManager.Action action) {
        this.action = action;
    }

    public enum TransferOrigin {
        OUTGOING,
        INCOMING
    }
}