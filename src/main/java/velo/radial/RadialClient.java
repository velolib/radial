package velo.radial;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import velo.radial.mixin.KeyBindingAccessor;
import velo.radial.ui.RadialScreen;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadialClient implements ClientModInitializer {

    public static final String MOD_ID = "radial";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
    public static final KeyBinding OPEN_RADIAL =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.radial.open",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    CATEGORY
            ));
    private static final Map<KeyBinding, Integer> keyPressQueue = new ConcurrentHashMap<>();
    private static boolean keyLocked = false;

    public static void lockKey() {
        keyLocked = true;
    }

    public static void scheduleKeyPress(KeyBinding key) {
        if (key == null) return;

        KeyBindingAccessor accessor = (KeyBindingAccessor) key;
        accessor.setTimesPressed(accessor.getTimesPressed() + 1);

        // Keep the key pressed for a couple of ticks
        keyPressQueue.put(key, 2);
    }

    public static void devLogger(String message) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOGGER.info("DEV - [ {} ]", message);
        }
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Handle radial opening and lock state
            if (OPEN_RADIAL.isPressed()) {
                if (!keyLocked && client.currentScreen == null) {
                    client.setScreen(new RadialScreen());
                }
            } else {
                keyLocked = false;
            }

            while (OPEN_RADIAL.wasPressed()) {
            }

            // Process scheduled key presses
            if (!keyPressQueue.isEmpty()) {
                Iterator<Map.Entry<KeyBinding, Integer>> it =
                        keyPressQueue.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<KeyBinding, Integer> entry = it.next();
                    KeyBinding key = entry.getKey();
                    int ticksLeft = entry.getValue();

                    if (ticksLeft > 0) {
                        key.setPressed(true);
                        entry.setValue(ticksLeft - 1);
                    } else {
                        key.setPressed(false);
                        it.remove();
                    }
                }
            }
        });
    }
}
