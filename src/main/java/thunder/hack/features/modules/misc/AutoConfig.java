//package thunder.hack.features.modules.misc;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import thunder.hack.core.manager.client.ModuleManager;
//import thunder.hack.features.modules.Module;
//import thunder.hack.features.modules.combat.Aura;
//import thunder.hack.setting.Setting;
//
//import java.awt.*;
//import java.io.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//public class AutoConfig extends Module {
//    private final Path configPath = Paths.get(System.getProperty("user.home"), ".minecraft", "thunderhackrecode", "misc", "autoconfig.json");
//    private final Setting<Boolean> openConfig = new Setting<>("OpenConfig", false);
//    private final Setting<Boolean> loadConfig = new Setting<>("LoadConfig", false);
//
//    public AutoConfig() {
//        super("AutoConfig", Category.MISC);
//        ensureConfigFile();
//    }
//
//    private void ensureConfigFile() {
//        try {
//            if (!Files.exists(configPath)) {
//                Files.createDirectories(configPath.getParent());
//                Files.createFile(configPath);
//                try (Writer writer = new FileWriter(configPath.toFile())) {
//                    writer.write("{\n  \"example\": \"Paste your settings here\"\n}");
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void openConfigFile() {
//        try {
//            Desktop.getDesktop().open(configPath.toFile());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void loadConfigFromFile() {
//        try (Reader reader = new FileReader(configPath.toFile())) {
//            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
//            applySettings(config);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void applySettings(JsonObject config) {
//        if (config.has("KillAura")) {
//            JsonObject killAuraConfig = config.getAsJsonObject("KillAura");
//
//            Module auraModule = getModuleByName("Aura");
//            if (auraModule instanceof Aura aura) {
//                aura.attackRange.setValue(killAuraConfig.has("attackRange") ? killAuraConfig.get("attackRange").getAsFloat() : aura.attackRange.getValue());
//                aura.wallRange.setValue(killAuraConfig.has("wallRange") ? killAuraConfig.get("wallRange").getAsFloat() : aura.wallRange.getValue());
//                aura.fov.setValue(killAuraConfig.has("fov") ? killAuraConfig.get("fov").getAsInt() : aura.fov.getValue());
//                aura.rotationMode.setValue(killAuraConfig.has("rotationMode") ? Aura.Mode.valueOf(killAuraConfig.get("rotationMode").getAsString()) : aura.rotationMode.getValue());
//            }
//        }
//
//    }
//
//    private Module getModuleByName(String name) {
//        for (Module module : ModuleManager.getModules()) {
//            if (module.getName().equalsIgnoreCase(name)) {
//                return module;
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public void onUpdate() {
//        if (openConfig.getValue()) {
//            openConfigFile();
//            openConfig.setValue(false);
//        }
//
//        if (loadConfig.getValue()) {
//            loadConfigFromFile();
//            loadConfig.setValue(false);
//        }
//    }
//}
