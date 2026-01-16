package thunder.hack.features.modules.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PlayerOnlineList extends Module {
    public static List<PlayerInfo> onlinePlayers = new ArrayList<>();
    public static int totalPlayers = 0;

    public static Setting<Boolean> hideInPlayerList = new Setting<>("HideInPlayerList", false);


    public PlayerOnlineList() {
        super("PlayerOnlineList", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        fetchOnlinePlayers();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (onlinePlayers.isEmpty()) {
                client.player.sendMessage(Text.literal("No players online.").formatted(Formatting.RED), false);
            } else {
                client.player.sendMessage(Text.literal("=-=-=-=-=-=-=").formatted(Formatting.DARK_PURPLE)
                        .append(Text.literal(" Exploit Core ").formatted(Formatting.LIGHT_PURPLE))
                        .append(Text.literal("=-=-=-=-=-=-=").formatted(Formatting.DARK_PURPLE)), false);

                for (PlayerInfo player : onlinePlayers) {
                    client.player.sendMessage(
                            Text.literal(" | ").formatted(Formatting.GREEN)
                                    .append(Text.literal("Nick: ").formatted(Formatting.WHITE))
                                    .append(Text.literal(maskNick(player.nick)).formatted(Formatting.GREEN))
                                    .append(Text.literal(", ").formatted(Formatting.WHITE))
                                    .append(Text.literal("Server: ").formatted(Formatting.WHITE))
                                    .append(Text.literal(player.server).formatted(Formatting.GREEN))
                                    .append(Text.literal(", ").formatted(Formatting.WHITE))
                                    .append(Text.literal("Config: ").formatted(Formatting.WHITE))
                                    .append(Text.literal(player.config).formatted(Formatting.GREEN)),
                            false);
                }

                client.player.sendMessage(Text.literal("").formatted(Formatting.RESET), false);

                client.player.sendMessage(Text.literal("")
                                .append(Text.literal("              ").formatted(Formatting.RESET))
                                .append(Text.literal("Online: ").formatted(Formatting.GREEN))
                                .append(Text.literal(String.valueOf(totalPlayers)).formatted(Formatting.DARK_GREEN)),
                        false);

                client.player.sendMessage(Text.literal("").formatted(Formatting.RESET), false);

                client.player.sendMessage(Text.literal("=-=-=-=-=-=-=").formatted(Formatting.DARK_PURPLE)
                        .append(Text.literal(" Exploit Core ").formatted(Formatting.LIGHT_PURPLE))
                        .append(Text.literal("=-=-=-=-=-=-=").formatted(Formatting.DARK_PURPLE)), false);
            }
        }
        disable();
    }

    public static void fetchOnlinePlayers() {
        try {
            URL url = new URL("https://plagai.org/apimimi/api?viev");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();

            if (response.get("status").getAsString().equals("success")) {
                onlinePlayers.clear();
                totalPlayers = response.get("total_players").getAsInt();
                response.getAsJsonArray("users").forEach(element -> {
                    JsonObject user = element.getAsJsonObject();
                    String nick = user.get("nick").getAsString();
                    String server = user.get("server").getAsString();
                    String config = user.get("config").getAsString();
                    long timestamp = user.get("timestamp").getAsLong();

                    onlinePlayers.add(new PlayerInfo(nick, server, config, timestamp));
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String maskNick(String nick) {
        if (nick == null || nick.isEmpty()) return "*****";
        return nick.charAt(0) + "*****";
    }

    public static class PlayerInfo {
        public String nick, server, config;
        public long timestamp;

        public PlayerInfo(String nick, String server, String config, long timestamp) {
            this.nick = nick;
            this.server = server;
            this.config = config;
            this.timestamp = timestamp;
        }
    }
}
