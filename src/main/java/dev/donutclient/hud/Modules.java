package dev.donutclient.hud;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Toggleable HUD modules shown in the ClickGUI. */
public final class Modules {
    public static final class Module {
        public final String name;
        public final String category;
        public boolean enabled;
        /** 0..1, eases toward enabled state for fade in/out. */
        public float fade;

        Module(String name, String category, boolean enabled) {
            this.name = name;
            this.category = category;
            this.enabled = enabled;
            this.fade = enabled ? 1f : 0f;
        }
    }

    public static final List<Module> ALL = new ArrayList<>();

    public static final Module WATERMARK   = add("Watermark", "HUD", true);
    public static final Module FPS         = add("FPS", "HUD", true);
    public static final Module PING        = add("Ping", "HUD", true);

    public static final Module COORDS      = add("Coords", "WORLD", true);
    public static final Module FACING      = add("Facing", "WORLD", true);
    public static final Module CLOCK       = add("Clock", "WORLD", false);

    public static final Module ACCENT_LINE = add("Accent Line", "CLIENT", true);
    public static final Module TEXT_SHADOW = add("Text Shadow", "CLIENT", false);

    private static long lastNanos = System.nanoTime();

    private Modules() {}

    private static Module add(String name, String category, boolean enabled) {
        Module m = new Module(name, category, enabled);
        ALL.add(m);
        return m;
    }

    /** Advances every module's fade toward its enabled state. Safe to call from several places per frame. */
    public static void update() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        for (Module m : ALL) {
            m.fade = Anim.approach(m.fade, m.enabled ? 1f : 0f, dt, 14f);
        }
    }

    public static Set<String> categories() {
        Set<String> set = new LinkedHashSet<>();
        for (Module m : ALL) set.add(m.category);
        return set;
    }

    public static List<Module> in(String category) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category.equals(category)) out.add(m);
        return out;
    }
}
