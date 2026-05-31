package velo.radial;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import velo.radial.mixin.KeyBindingAccessor;
import velo.radial.ui.RadialScreen;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadialClient implements ClientModInitializer {

    public static final String MOD_ID = "radial";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    public static final KeyMapping OPEN_RADIAL =
            KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.radial.open",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    CATEGORY
            ));
    private static final Map<KeyMapping, Integer> keyPressQueue = new ConcurrentHashMap<>();
    private static boolean keyLocked = false;

    public static void lockKey() {
        keyLocked = true;
    }

    public static void scheduleKeyPress(KeyMapping key) {
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
            if (OPEN_RADIAL.isDown()) {
                if (!keyLocked && client.screen == null) {
                    client.setScreen(new RadialScreen());
                }
            } else {
                keyLocked = false;
            }

            while (OPEN_RADIAL.consumeClick()) {
            }

            // Process scheduled key presses
            if (!keyPressQueue.isEmpty()) {
                Iterator<Map.Entry<KeyMapping, Integer>> it =
                        keyPressQueue.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<KeyMapping, Integer> entry = it.next();
                    KeyMapping key = entry.getKey();
                    int ticksLeft = entry.getValue();

                    if (ticksLeft > 0) {
                        key.setDown(true);
                        entry.setValue(ticksLeft - 1);
                    } else {
                        key.setDown(false);
                        it.remove();
                    }
                }
            }
        });
    }
}
