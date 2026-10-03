package dev.donutclient.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_342;
import net.minecraft.class_408;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DonutHudClient implements ClientModInitializer {
    public static final String MOD_ID = "donut-hud";
    public static final Logger LOGGER = LoggerFactory.getLogger("Donut Client");

    private static boolean lastKeyDown = false;

    @Override
    public void onInitializeClient() {
        Config.load();
        HudElementRegistry.addLast(class_2960.method_60655(MOD_ID, "main"), DonutHud::render);

        // Right Shift toggles the ClickGUI (polled, so it works without keybind registration)
        ClientTickEvents.END_CLIENT_TICK.register(DonutHudClient::tick);
    }

    private static void tick(class_310 mc) {
        if (mc.method_22683() == null) return;

        AutoTyper.tick(mc);

        boolean down = GLFW.glfwGetKey(mc.method_22683().method_4490(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !lastKeyDown) {
            class_437 s = mc.field_1755;
            if (s instanceof ClickGuiScreen gui) {
                gui.requestClose();
            } else if (s == null || canOpenOver(s)) {
                mc.method_1507(new ClickGuiScreen(s));
            }
        }
        lastKeyDown = down;
    }

    /** Anywhere except where Right Shift is used for typing capitals. */
    private static boolean canOpenOver(class_437 s) {
        if (s instanceof class_408) return false;
        return !(s.method_25399() instanceof class_342);
    }
}
