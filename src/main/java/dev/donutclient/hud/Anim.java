package dev.donutclient.hud;

/** Time-based smoothing helper; each instance keeps its own clock. */
public final class Anim {
    public float value;
    private long last = -1;

    public Anim(float start) {
        this.value = start;
    }

    public float update(float target, float speed) {
        long now = System.nanoTime();
        float dt = last < 0 ? 0f : Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        value = approach(value, target, dt, speed);
        return value;
    }

    public static float approach(float cur, float target, float dt, float speed) {
        float next = cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(target - next) < 0.005f ? target : next;
    }
}
