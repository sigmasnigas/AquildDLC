package thunder.hack;

import com.mojang.logging.LogUtils;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.*;
import thunder.hack.core.hooks.ManagerShutdownHook;
import thunder.hack.core.hooks.ModuleShutdownHook;
import thunder.hack.features.modules.client.PlayerOnlineList;
import thunder.hack.gui.notification.Notification;
import thunder.hack.utility.ThunderUtility;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class ThunderHack implements ModInitializer {
    public static final ModMetadata MOD_META;

    public static final String MOD_ID = "thunderhack";
    public static final String VERSION = "7";
    public static String GITHUB_HASH = "0";
    public static String BUILD_DATE = "1 Jan 1970";

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Runtime RUNTIME = Runtime.getRuntime();

    public static final boolean baritone = FabricLoader.getInstance().isModLoaded("baritone")
            || FabricLoader.getInstance().isModLoaded("baritone-meteor");

    public static final IEventBus EVENT_BUS = new EventBus();
    public static String[] contributors = new String[32];
    public static Color copy_color = new Color(-1);
    public static KeyListening currentKeyListener;
    public static boolean isOutdated = false;
    public static BlockPos gps_position;
    public static float TICK_TIMER = 1f;
    public static MinecraftClient mc;
    public static long initTime;

    public static Core core = new Core();
    private static Timer backgroundTask;

    static {
        MOD_META = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
    }

    @Override
    public void onInitialize() {
        mc = MinecraftClient.getInstance();
        initTime = System.currentTimeMillis();

        BUILD_DATE = ThunderUtility.readManifestField("Build-Timestamp");
        GITHUB_HASH = ThunderUtility.readManifestField("Git-Commit");
        ThunderUtility.syncVersion();

        EVENT_BUS.registerLambdaFactory("thunder.hack",
                (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        EVENT_BUS.subscribe(core);

        Managers.init();
        Managers.subscribe();

        Render2DEngine.initShaders();
        ModuleManager.rpc.startRpc();

        RUNTIME.addShutdownHook(new ManagerShutdownHook());
        RUNTIME.addShutdownHook(new ModuleShutdownHook());

        startBackgroundTask();
    }

    private void startBackgroundTask() {
        backgroundTask = new Timer("BackgroundAPIRequest", true);
        backgroundTask.schedule(new TimerTask() {
            @Override
            public void run() {
                sendApiRequest();
            }
        }, 0, 5000);
    }

    private void sendApiRequest() {
        try {
            String nick, server, config;
            
            if (PlayerOnlineList.hideInPlayerList.getValue()) {
                nick = "Hide";
                server = "Hide";
                config = "Hide";
            } else {
                nick = (mc.player != null && mc.player.getName() != null) ? mc.player.getName().getString() : mc.getSession().getUsername();
                server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "In main menu";
                config = ConfigManager.getCurrentConfigName();
            }

            String urlString = "https://plagai.org/apimimi/api?nick=" + nick + "&server=" + server + "&config=" + config;

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (var inputStream = connection.getInputStream();
                     var scanner = new java.util.Scanner(inputStream).useDelimiter("\\A")) {
                    String response = scanner.hasNext() ? scanner.next() : "No response";
                }
            }

            connection.disconnect();
        } catch (Exception e) {
            LOGGER.error("Error to request API, Report it to the exploitcore adminisctation: ", e);
            String message = Formatting.RED + "Error to request API, Report it to the exploitcore adminisctation:" + e;
            mc.player.sendMessage(Text.literal(message), false);
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
            Managers.NOTIFICATION.publicity("Connect API", "Error to request API, Report it to the exploitcore adminisctation", 5, Notification.Type.ERROR);
        }
    }

    public static boolean isFuturePresent() {
        return FabricLoader.getInstance().getModContainer("future").isPresent();
    }

    public enum KeyListening {
        ThunderGui, ClickGui, Search, Sliders, Strings
    }
}
