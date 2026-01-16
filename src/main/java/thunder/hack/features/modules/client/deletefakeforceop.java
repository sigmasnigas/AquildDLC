package thunder.hack.features.modules.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import thunder.hack.features.modules.Module;

public class deletefakeforceop extends Module {

    public deletefakeforceop() {
        super("FakeOp", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        sendFakeOpMessage(mc.player);
        disable();
    }

    private void sendFakeOpMessage(ClientPlayerEntity player) {
        String message = "[Server: Made " + player.getName().getString() + " a server operator]";
        player.sendMessage(Text.literal(message).formatted(Formatting.GRAY, Formatting.ITALIC), false);
    }
}
