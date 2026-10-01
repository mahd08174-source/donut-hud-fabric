package dev.donutclient.hud;

import org.lwjgl.glfw.GLFW;

import java.util.Random;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_465;
import net.minecraft.class_7172;

/** Rounded category panels with animated pill toggles. Blur + panels fade in on open, out on close. */
public class ClickGuiScreen extends class_437 {
    private static final int BAR_BG   = 0xF00B0D16;
    private static final int PANEL_BG = 0xE6101320;
    private static final int BLUE     = 0xFF3D8BFF;
    private static final int TEXT     = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF8D93A8;

    /** Peak blur radius (vanilla setting is whole numbers 0..10). */
    private static final float MAX_BLUR = 7f;
    /** Seconds for the blur to go fully in or out. */
    private static final float BLUR_TIME = 0.28f;

    private static final int PANEL_W = 120;
    private static final int GAP = 10;
    private static final int START_X = 20;
    private static final int START_Y = 44;
    private static final int HEADER_H = 18;
    private static final int ROW_H = 16;
    private static final int R = 4;

    private final class_437 parent;
    private final Anim anim = new Anim(0f);
    private boolean closing = false;
    private boolean lastMouseDown = false;

    // blur is optional: if anything about it fails we just fall back to the dim
    private boolean blurWorks = true;
    private class_7172<Integer> blurOption;
    private int originalBlur = -1;
    private int appliedBlur = -1;
    private float blurProg = 0f;
    private long lastNanos = -1;
    private final Random rng = new Random();

    public ClickGuiScreen(class_437 parent) {
        super(class_2561.method_43470("Donut Client"));
        this.parent = parent;
        try {
            this.blurOption = class_310.method_1551().field_1690.method_57702();
            this.originalBlur = blurOption.method_41753();
        } catch (Throwable t) {
            blurWorks = false;
            DonutHudClient.LOGGER.error("Blur unavailable", t);
        }
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    /**
     * Vanilla draws its own dark gradient (and a blur) here before render() runs, and that gradient
     * pops in at full strength on the first frame. We skip it and draw a faded dim ourselves.
     */
    @Override
    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
    }

    /** Esc and Right Shift both fade out instead of vanishing. */
    @Override
    public void method_25419() {
        closing = true;
    }

    public void requestClose() {
        closing = true;
    }

    @Override
    public void method_25432() {
        DonutHud.suppressed = false;
        try {
            if (blurOption != null && originalBlur >= 0) blurOption.method_41748(originalBlur);
        } catch (Throwable t) {
            DonutHudClient.LOGGER.error("Could not restore blur", t);
        }
        Config.save();
        super.method_25432();
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        class_310 mc = class_310.method_1551();
        class_327 tr = mc.field_1772;

        Modules.update();
        DonutHud.suppressed = !closing;   // HUD fades out behind the menu, back in as it closes

        long now = System.nanoTime();
        float dt = lastNanos < 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        float a = anim.update(closing ? 0f : 1f, 12f);

        // blur progress moves linearly so every radius step is visible for an equal time
        float step = dt / BLUR_TIME;
        blurProg = closing ? Math.max(0f, blurProg - step) : Math.min(1f, blurProg + step);

        if (closing && a < 0.03f && blurProg <= 0.02f) {
            // return to whatever was open before (not containers, which would re-open badly)
            class_437 back = (parent instanceof class_465) ? null : parent;
            mc.method_1507(back);
            return;
        }

        if (blurWorks) {
            try {
                // smoothstep, then dither between the two nearest whole radii so the fade looks continuous
                float s = blurProg * blurProg * (3f - 2f * blurProg);
                float v = s * MAX_BLUR;
                int base = (int) Math.floor(v);
                int blur = base + (rng.nextFloat() < (v - base) ? 1 : 0);
                if (blur != appliedBlur) {
                    blurOption.method_41748(blur);
                    appliedBlur = blur;
                }
                if (blur > 0) ctx.method_71278();
            } catch (Throwable t) {
                blurWorks = false;
                DonutHudClient.LOGGER.error("Blur disabled after error", t);
            }
        }

        boolean down = GLFW.glfwGetMouseButton(mc.method_22683().method_4490(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean clicked = down && !lastMouseDown && !closing && a > 0.6f;
        lastMouseDown = down;

        int slide = (int) ((1f - a) * 8f);

        // dim the world (a little darker if blur isn't available)
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, Draw.alpha(blurWorks ? 0x77000000 : 0x99000000, a));

        // top bar: blue outline, blue underline
        Draw.roundRect(ctx, 9, 7 + slide, this.field_22789 - 18, 26, 6, Draw.alpha(BLUE, a * 0.55f));
        Draw.roundRect(ctx, 10, 8 + slide, this.field_22789 - 20, 24, 5, Draw.alpha(BAR_BG, a));
        ctx.method_25294(15, 8 + slide + 23, this.field_22789 - 15, 8 + slide + 24, Draw.alpha(BLUE, a));
        Draw.text(ctx, tr, class_2561.method_43470("DONUT CLIENT").method_27692(class_124.field_1067), 20, 15 + slide, TEXT, a, false);

        int x = START_X;
        for (String category : Modules.categories()) {
            var mods = Modules.in(category);
            int y = START_Y + slide;
            int height = HEADER_H + mods.size() * ROW_H + 4;

            // outline ring, panel, header, then blue lines on top
            Draw.roundRect(ctx, x - 1, y - 1, PANEL_W + 2, height + 2, R + 1, Draw.alpha(BLUE, a * 0.55f));
            Draw.roundRect(ctx, x, y, PANEL_W, height, R, Draw.alpha(PANEL_BG, a));
            Draw.roundTop(ctx, x, y, PANEL_W, HEADER_H, R, Draw.alpha(BAR_BG, a));
            ctx.method_25294(x + R, y, x + PANEL_W - R, y + 1, Draw.alpha(BLUE, a));                       // top line
            ctx.method_25294(x + 3, y + HEADER_H, x + PANEL_W - 3, y + HEADER_H + 1, Draw.alpha(BLUE, a * 0.85f)); // separator
            class_2561 title = class_2561.method_43470(category).method_27692(class_124.field_1067);
            Draw.text(ctx, tr, title, x + (PANEL_W - tr.method_27525(title)) / 2, y + 5, TEXT, a, false);

            int ry = y + HEADER_H + 3;
            for (Modules.Module m : mods) {
                boolean hover = mouseX >= x && mouseX < x + PANEL_W && mouseY >= ry && mouseY < ry + ROW_H;
                if (hover) Draw.roundRect(ctx, x + 3, ry, PANEL_W - 6, ROW_H, 3, Draw.alpha(0x22FFFFFF, a));
                if (hover && clicked) {
                    m.enabled = !m.enabled;
                    Config.save();
                }

                float f = m.fade;
                if (f > 0.02f) Draw.roundRect(ctx, x + 1, ry + 3, 2, ROW_H - 6, 1, Draw.alpha(BLUE, a * f));
                int nameColor = Draw.lerp(TEXT_DIM, TEXT, f);
                Draw.text(ctx, tr, m.name, x + 9, ry + 4, nameColor, a, false);
                drawToggle(ctx, x + PANEL_W - 30, ry + 4, f, a);
                ry += ROW_H;
            }
            x += PANEL_W + GAP;
        }

        Draw.text(ctx, tr, "Right Shift to close", 12, this.field_22790 - 14, TEXT_DIM, a, false);
    }

    private static void drawToggle(class_332 ctx, int x, int y, float f, float a) {
        int w = 22, h = 9;
        int track = Draw.lerp(0xFF23263A, 0xFF2F5FB8, f);
        Draw.roundRect(ctx, x, y, w, h, 4, Draw.alpha(track, a));
        int kx = Math.round(x + 1 + (w - 9) * f);
        int knob = Draw.lerp(0xFF8D93A8, 0xFFFFFFFF, f);
        Draw.roundRect(ctx, kx, y + 1, 7, 7, 3, Draw.alpha(knob, a));
    }
}
