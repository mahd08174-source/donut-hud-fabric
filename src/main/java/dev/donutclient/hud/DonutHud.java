package dev.donutclient.hud;

import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1041;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_408;
import net.minecraft.class_640;
import net.minecraft.class_9779;

/**
 * Rounded dark panels with blue outline, top line and header separator.
 * Fades with module toggles, hides while the ClickGUI is open, and can be dragged while chat is open.
 */
public final class DonutHud {
    private static final int PANEL      = 0xD9101320;
    private static final int HEADER     = 0xF00B0D16;
    private static final int BLUE       = 0xFF3D8BFF;
    private static final int TEXT       = 0xFFE6E8EF;
    private static final int TEXT_DIM   = 0xFF8D93A8;

    private static final int PAD = 6;
    private static final int ROW = 11;
    private static final int R = 4;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Anim HUD_ALPHA = new Anim(0f);

    /** Set by the ClickGUI so the HUD fades away behind it. */
    public static boolean suppressed = false;

    // drag state
    private static String dragging = null;
    private static int dragDX, dragDY;
    private static boolean lastDown = false;
    private static boolean chatOpen = false;
    private static boolean down = false;
    private static boolean pressed = false;
    private static double mouseX, mouseY;

    private record Row(String key, String value, float alpha) {}

    private DonutHud() {}

    public static void render(class_332 ctx, class_9779 tick) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null) return;

        Modules.update();
        float target = (mc.field_1690.field_1842 || suppressed) ? 0f : 1f;
        float hud = HUD_ALPHA.update(target, 7f);

        // --- input: only while chat is open (cursor is unlocked) ---
        class_1041 win = mc.method_22683();
        chatOpen = mc.field_1755 instanceof class_408;
        mouseX = mc.field_1729.method_1603() * win.method_4486() / (double) win.method_4480();
        mouseY = mc.field_1729.method_1604() * win.method_4502() / (double) win.method_4507();
        boolean d = chatOpen && GLFW.glfwGetMouseButton(win.method_4490(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        pressed = d && !lastDown && hud > 0.5f;
        lastDown = d;
        down = d;
        if (!d && dragging != null) {
            dragging = null;
            Config.save();
        }

        if (hud < 0.02f) return;

        class_327 tr = mc.field_1772;

        List<Row> info = new ArrayList<>();
        addRow(info, Modules.FPS, "FPS", String.valueOf(mc.method_47599()));
        addRow(info, Modules.PING, "Ping", pingText(mc));
        float infoFade = Math.max(Modules.WATERMARK.fade, maxAlpha(info));
        if (infoFade > 0.02f) {
            String title = Modules.WATERMARK.enabled ? "DONUT CLIENT" : "INFO";
            drawPanel(ctx, tr, win, "info", title, info, 6, 6, infoFade * hud);
        }

        var p = mc.field_1724;
        List<Row> world = new ArrayList<>();
        if (Modules.COORDS.fade > 0.02f) {
            addRow(world, Modules.COORDS, "X", fmt(p.method_23317()));
            addRow(world, Modules.COORDS, "Y", fmt(p.method_23318()));
            addRow(world, Modules.COORDS, "Z", fmt(p.method_23321()));
        }
        addRow(world, Modules.FACING, "Facing", cap(p.method_5735().method_15434()));
        addRow(world, Modules.CLOCK, "Time", LocalTime.now().format(CLOCK));
        float worldFade = maxAlpha(world);
        if (worldFade > 0.02f) {
            drawPanel(ctx, tr, win, "world", "WORLD", world, 6, 56, worldFade * hud);
        }

        if (chatOpen) {
            Draw.text(ctx, tr, "Drag panels to move them", 6, win.method_4502() - 34, TEXT_DIM, hud, false);
        }
    }

    private static void addRow(List<Row> rows, Modules.Module m, String key, String value) {
        if (m.fade > 0.02f) rows.add(new Row(key, value, m.fade));
    }

    private static float maxAlpha(List<Row> rows) {
        float max = 0f;
        for (Row r : rows) max = Math.max(max, r.alpha());
        return max;
    }

    private static void drawPanel(class_332 ctx, class_327 tr, class_1041 win, String id, String title,
                                  List<Row> rows, int defX, int defY, float a) {
        boolean shadow = Modules.TEXT_SHADOW.enabled;

        class_2561 t = class_2561.method_43470(title).method_27692(class_124.field_1067);
        int width = tr.method_27525(t) + PAD * 2;
        for (Row r : rows) {
            width = Math.max(width, tr.method_1727(r.key()) + 14 + tr.method_1727(r.value()) + PAD * 2);
        }
        width = Math.max(width, 96);

        int headerH = ROW + 5;
        int height = headerH + (rows.isEmpty() ? 0 : rows.size() * ROW + PAD);

        int[] pos = Config.POS.computeIfAbsent(id, k -> new int[]{defX, defY});

        // dragging
        boolean hover = chatOpen
                && mouseX >= pos[0] && mouseX < pos[0] + width
                && mouseY >= pos[1] && mouseY < pos[1] + height;
        if (pressed && hover && dragging == null) {
            dragging = id;
            dragDX = (int) mouseX - pos[0];
            dragDY = (int) mouseY - pos[1];
        }
        if (id.equals(dragging) && down) {
            pos[0] = Math.max(0, Math.min((int) mouseX - dragDX, win.method_4486() - width));
            pos[1] = Math.max(0, Math.min((int) mouseY - dragDY, win.method_4502() - height));
        }
        int x = pos[0], y = pos[1];

        // blue outline ring: always faint (with Accent Line), brighter when draggable / being dragged
        float line = Modules.ACCENT_LINE.fade;
        float ring = line * 0.55f;
        if (chatOpen && (hover || id.equals(dragging))) ring = id.equals(dragging) ? 1f : 0.85f;
        if (ring > 0.02f) {
            Draw.roundRect(ctx, x - 1, y - 1, width + 2, height + 2, R + 1, Draw.alpha(BLUE, a * ring));
        }

        Draw.roundRect(ctx, x, y, width, height, R, Draw.alpha(PANEL, a));
        Draw.roundTop(ctx, x, y, width, headerH, R, Draw.alpha(HEADER, a));
        if (line > 0.02f) {
            ctx.method_25294(x + R, y, x + width - R, y + 1, Draw.alpha(BLUE, a * line));                        // top line
            if (!rows.isEmpty()) {
                ctx.method_25294(x + 3, y + headerH, x + width - 3, y + headerH + 1, Draw.alpha(BLUE, a * line * 0.85f)); // separator
            }
        }

        Draw.text(ctx, tr, t, x + (width - tr.method_27525(t)) / 2, y + 4, TEXT, a, shadow);

        int ry = y + headerH + 3;
        for (Row r : rows) {
            float ra = a * r.alpha();
            Draw.text(ctx, tr, r.key(), x + PAD, ry, TEXT_DIM, ra, shadow);
            Draw.text(ctx, tr, r.value(), x + width - PAD - tr.method_1727(r.value()), ry, TEXT, ra, shadow);
            ry += ROW;
        }
    }

    private static String pingText(class_310 mc) {
        if (mc.method_1562() == null) return "-";
        class_640 e = mc.method_1562().method_2871(mc.field_1724.method_5667());
        return e == null ? "-" : e.method_2959() + " ms";
    }

    private static String fmt(double d) { return String.format("%.1f", d); }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
