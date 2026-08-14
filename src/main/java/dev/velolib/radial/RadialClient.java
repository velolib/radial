package dev.velolib.radial;

import com.mojang.blaze3d.platform.InputConstants;
import dev.velolib.radial.api.ShortcutRegistry;
import dev.velolib.radial.api.SlotModeRegistry;
import dev.velolib.radial.config.RadialConfig;
import dev.velolib.radial.integration.MalilibIntegration;
import dev.velolib.radial.mixin.KeyMappingAccessor;
import dev.velolib.radial.ui.screen.RadialScreen;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadialClient implements ClientModInitializer {

    public static final String MOD_ID = "radial";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final KeyMapping OPEN_RADIAL = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key." + MOD_ID + ".open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY, 0));
    public static final KeyMapping BACK_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".back", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY, 1));
    public static final KeyMapping[] SLOT_KEYS = new KeyMapping[12];

    static {
        for (int i = 0; i < 12; i++) {
            SLOT_KEYS[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key." + MOD_ID + ".slot." + (i + 1),
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY,
                    12 + i));
        }
    }

    private static final Map<KeyMapping, Integer> keyPressQueue = new ConcurrentHashMap<>();
    private static boolean keyLocked = false;

    public static void lockKey() {
        keyLocked = true;
    }

    /**
     * Helper to blacklist our internal keys from the picker and slot modes.
     */
    public static boolean isRadialInternalKey(KeyMapping key) {
        if (key == OPEN_RADIAL || key == BACK_KEY) return true;
        for (KeyMapping slotKey : SLOT_KEYS) {
            if (key == slotKey) return true;
        }
        return false;
    }

    public static void scheduleKeyPress(KeyMapping key) {
        if (key == null) return;
        KeyMappingAccessor accessor = (KeyMappingAccessor) key;
        accessor.setClickCount(accessor.getClickCount() + 1);
        keyPressQueue.put(key, 2);
    }

    public static void devLogger(String message) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOGGER.info("DEV - [ {} ]", message);
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Radial Client...");

        // REGISTER CONFIG
        SlotModeRegistry.init();
        ShortcutRegistry.init();
        RadialConfig.load();

        if (FabricLoader.getInstance().isModLoaded("malilib")) {
            MalilibIntegration.init();
        }

        // REGISTER HUD & EVENTS
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, tracker) -> {
            if (!(Minecraft.getInstance().gui.screen() instanceof RadialScreen)) {
                original.extractRenderState(graphics, tracker);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_RADIAL.isDown()) {
                if (!keyLocked && client.gui.screen() == null) {
                    RadialScreen.prepareRenderer();
                    client.gui.setScreen(new RadialScreen());
                }
            } else {
                keyLocked = false;
            }

            //noinspection StatementWithEmptyBody
            while (OPEN_RADIAL.consumeClick()) {}

            if (!keyPressQueue.isEmpty()) {
                var it = keyPressQueue.entrySet().iterator();

                while (it.hasNext()) {
                    var entry = it.next();
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
