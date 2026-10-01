package dev.donutclient.hud;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** Saves module toggles and HUD panel positions to config/donut-hud.properties. */
public final class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donut-hud.properties");

    /** Panel id -> {x, y} in scaled GUI pixels. */
    public static final Map<String, int[]> POS = new HashMap<>();

    private Config() {}

    private static String key(Modules.Module m) {
        return m.name.toLowerCase().replace(' ', '_');
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            Properties p = new Properties();
            try (var in = Files.newInputStream(FILE)) {
                p.load(in);
            }
            for (Modules.Module m : Modules.ALL) {
                String v = p.getProperty("module." + key(m));
                if (v != null) {
                    m.enabled = Boolean.parseBoolean(v);
                    m.fade = m.enabled ? 1f : 0f;
                }
            }
            for (String id : new String[]{"info", "world"}) {
                String x = p.getProperty("panel." + id + ".x");
                String y = p.getProperty("panel." + id + ".y");
                if (x != null && y != null) {
                    POS.put(id, new int[]{Integer.parseInt(x), Integer.parseInt(y)});
                }
            }
        } catch (Exception e) {
            DonutHudClient.LOGGER.warn("Could not load config", e);
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            for (Modules.Module m : Modules.ALL) {
                p.setProperty("module." + key(m), String.valueOf(m.enabled));
            }
            for (Map.Entry<String, int[]> e : POS.entrySet()) {
                p.setProperty("panel." + e.getKey() + ".x", String.valueOf(e.getValue()[0]));
                p.setProperty("panel." + e.getKey() + ".y", String.valueOf(e.getValue()[1]));
            }
            try (var out = Files.newOutputStream(FILE)) {
                p.store(out, "Donut Client HUD");
            }
        } catch (Exception e) {
            DonutHudClient.LOGGER.warn("Could not save config", e);
        }
    }
}
