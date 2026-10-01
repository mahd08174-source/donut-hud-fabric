package dev.donutclient.hud;

import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;

/** Small drawing helpers: rounded rectangles, alpha, colour blending. */
public final class Draw {
    private Draw() {}

    public static int alpha(int argb, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int a = (int) (((argb >>> 24) & 0xFF) * f);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 0xFF) * (1 - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void roundRect(class_332 ctx, int x, int y, int w, int h, int r, int color) {
        if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            ctx.method_25294(x, y, x + w, y + h, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            ctx.method_25294(x + dx, y + i, x + w - dx, y + i + 1, color);
            ctx.method_25294(x + dx, y + h - 1 - i, x + w - dx, y + h - i, color);
        }
        ctx.method_25294(x, y + r, x + w, y + h - r, color);
    }

    /** Rounded on top corners only (for panel headers). */
    public static void roundTop(class_332 ctx, int x, int y, int w, int h, int r, int color) {
        roundRect(ctx, x, y, w, h, r, color);
        ctx.method_25294(x, y + r, x + w, y + h, color);
    }

    public static void text(class_332 ctx, class_327 tr, String s, int x, int y, int color, float a, boolean shadow) {
        if (a < 0.04f) return;
        ctx.method_51433(tr, s, x, y, alpha(color, a), shadow);
    }

    public static void text(class_332 ctx, class_327 tr, class_2561 t, int x, int y, int color, float a, boolean shadow) {
        if (a < 0.04f) return;
        ctx.method_51439(tr, t, x, y, alpha(color, a), shadow);
    }
}
