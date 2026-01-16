package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import thunder.hack.events.impl.EventDeath;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

public class KillCommand extends Module {
    private final Setting<String> commandTemplate = new Setting<>("CommandTemplate", "msg %s L dsc.gg/mdpstresser najlepsza stressarka + bombamod LLL");
    private final Setting<Boolean> repeat = new Setting<>("Repeat", false);
    private final Setting<Integer> repeatCount = new Setting<>("RepeatCount", 1, 1, 10, v -> repeat.getValue());
    private final Setting<Integer> repeatDelay = new Setting<>("RepeatDelay", 500, 100, 5000, v -> repeat.getValue());
    private final Setting<Boolean> noKillRandom = new Setting<>("NoKillRandom", false);
    private final Setting<Integer> randomDelay = new Setting<>("RandomDelay", 5000, 1000, 30000, v -> noKillRandom.getValue());

    private final Timer repeatTimer = new Timer();
    private final Timer randomTimer = new Timer();
    private int repeatIndex = 0;
    private String pendingCommand;

    public KillCommand() {
        super("KillCommand", Category.MISC);
    }

    @EventHandler
    public void onPlayerDeath(EventDeath event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ClientPlayerEntity) return;

        String playerName = player.getName().getString();
        String command = String.format(commandTemplate.getValue(), playerName);

        sendChatCommand(command);

        if (repeat.getValue()) {
            repeatIndex = 1;
            pendingCommand = command;
            repeatTimer.reset();
        }
    }

    @Override
    public void onUpdate() {
        if (repeat.getValue() && repeatIndex > 0 && repeatIndex < repeatCount.getValue()) {
            if (repeatTimer.passedMs(repeatDelay.getValue())) {
                sendChatCommand(pendingCommand);
                repeatIndex++;
                repeatTimer.reset();
            }
        }

        if (noKillRandom.getValue() && randomTimer.passedMs(randomDelay.getValue())) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || player.isDead() || !player.isAlive()) continue;

                String randomCommand = String.format(commandTemplate.getValue(), player.getName().getString());
                sendChatCommand(randomCommand);
                randomTimer.reset();
                break;
            }
        }
    }

    public void sendChatCommand(String command) {
        mc.getNetworkHandler().sendChatCommand(command);
    }
}
