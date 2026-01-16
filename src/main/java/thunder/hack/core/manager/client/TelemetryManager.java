package thunder.hack.core.manager.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.compress.utils.Lists;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.IManager;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.utility.Timer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static thunder.hack.core.Managers.TELEMETRY;

public class TelemetryManager implements IManager {
    private final Timer pingTimer = new Timer();
    private List<String> onlinePlayers = new ArrayList<>();
    private int totalPlayers = 0;

    public void onUpdate() {
        if (pingTimer.every(9000)) {
            fetchData();
        }
    }

    public void fetchData() {
        if (ClientSettings.telemetry.getValue()) {
            pingServer(mc.getSession().getUsername());
        }
        onlinePlayers = getOnlinePlayersList();
        totalPlayers = getTotalPlayers();
    }

    public void pingServer(String name) {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.thunderhack.net/v1/users/online?name=" + name))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpClient client = HttpClient.newHttpClient();
            client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
        }
    }

    public static int getTotalPlayers() {
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://plagai.org/apimimi/api?viev"))
                .GET()
                .build();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

            if (jsonResponse.has("total_players")) {
                return jsonResponse.get("total_players").getAsInt();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public static List<String> getOnlinePlayersList() {
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://plagai.org/apimimi/api?viev"))
                .GET()
                .build();
        final List<String> names = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            if (jsonResponse.has("users")) {
                jsonResponse.getAsJsonArray("users").forEach(e -> {
                    JsonObject user = e.getAsJsonObject();
                    if (user.has("nick")) {
                        names.add(user.get("nick").getAsString());
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return names;
    }



    public int getTotalPlayersCount() {
        return totalPlayers;
    }

    public List<String> getOnlinePlayers() {
        return Lists.newArrayList(onlinePlayers.iterator());
    }
}
