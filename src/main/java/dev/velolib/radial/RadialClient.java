package dev.velolib.radial;

import com.mojang.blaze3d.platform.InputConstants;
import dev.velolib.radial.api.ShortcutRegistry;
import dev.velolib.radial.api.SlotModeRegistry;
import dev.velolib.radial.config.RadialConfig;
import dev.velolib.radial.integration.MalilibIntegration;
import dev.velolib.radial.mixin.KeyMappingAccessor;
import dev.velolib.radial.ui.screen.RadialScreen;
import dev.velolib.radial.util.MacroExecutor;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLMouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadialClient implements ClientModInitializer {

    public static final String MOD_ID = "radial";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final KeyMapping OPEN_RADIAL = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key." + MOD_ID + ".open", InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY, 0));
    public static final KeyMapping BACK_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".back", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY, 1));
    public static final KeyMapping STOP_MACROS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".stop_macros",
            InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY,
            2));
    public static final KeyMapping[] SLOT_KEYS = new KeyMapping[12];

    static {
        for (int i = 0; i < 12; i++) {
            SLOT_KEYS[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key." + MOD_ID + ".slot." + (i + 1),
                    InputConstants.Type.KEYBOARD,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY,
                    12 + i));
        }
    }

    // How long a simulated key press is held, and how long it must be released before the next press of that key
    private static final int KEY_HOLD_TICKS = 2;
    private static final int KEY_RELEASE_TICKS = 1;

    private static final Map<KeyMapping, ScheduledKeyPress> keyPressQueue = new ConcurrentHashMap<>();
    private static boolean keyLocked = false;
    private static boolean sectorTexturesPrepared = false;

    /**
     * Press state of a single key. Repeated presses are queued so each one gets a distinct
     * press and release, instead of merging into one long hold.
     */
    private static final class ScheduledKeyPress {
        private int holdTicksLeft;
        private int releaseTicksLeft;
        private int queuedPresses;
    }

    public static void lockKey() {
        keyLocked = true;
    }

    /**
     * Helper to blacklist our internal keys from the picker and slot modes.
     */
    public static boolean isRadialInternalKey(KeyMapping key) {
        if (key == OPEN_RADIAL || key == BACK_KEY || key == STOP_MACROS) return true;
        for (KeyMapping slotKey : SLOT_KEYS) {
            if (key == slotKey) return true;
        }
        return false;
    }

    public static void scheduleKeyPress(KeyMapping key) {
        if (key == null) return;

        ScheduledKeyPress press = keyPressQueue.get(key);
        if (press == null) {
            press = new ScheduledKeyPress();
            keyPressQueue.put(key, press);
            startKeyPress(key, press);
        } else {
            // The key is still busy with an earlier press; run this one after it is released
            press.queuedPresses++;
        }
    }

    public static void clearQueuedKeyPresses() {
        keyPressQueue.values().forEach(press -> press.queuedPresses = 0);
    }

    private static void startKeyPress(KeyMapping key, ScheduledKeyPress press) {
        KeyMappingAccessor accessor = (KeyMappingAccessor) key;
        accessor.setClickCount(accessor.getClickCount() + 1);
        press.holdTicksLeft = KEY_HOLD_TICKS;
        press.releaseTicksLeft = KEY_RELEASE_TICKS;
    }

    private static void tickKeyPresses() {
        var it = keyPressQueue.entrySet().iterator();

        while (it.hasNext()) {
            var entry = it.next();
            KeyMapping key = entry.getKey();
            ScheduledKeyPress press = entry.getValue();

            if (press.holdTicksLeft > 0) {
                key.setDown(true);
                press.holdTicksLeft--;
                continue;
            }

            key.setDown(false);

            if (press.queuedPresses <= 0) {
                it.remove();
            } else if (press.releaseTicksLeft > 0) {
                // Stay released for at least one tick so the game sees two separate presses
                press.releaseTicksLeft--;
            } else {
                press.queuedPresses--;
                startKeyPress(key, press);
                key.setDown(true);
                press.holdTicksLeft--;
            }
        }
    }

    /**
     * Whether the physical key or mouse button is currently held, regardless of which screen is open.
     * Unbound keys always count as released.
     */
    public static boolean isPhysicallyDown(InputConstants.Key key) {
        int code = key.getValue();
        if (code == InputConstants.UNKNOWN.getValue()) return false;

        return switch (key.getType()) {
            case MOUSE -> (SDLMouse.SDL_GetMouseState(null, null) & (1 << (code - 1))) != 0;
            case KEYBOARD -> InputConstants.isKeyDown(code);
        };
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Radial Client...");

        // REGISTER CONFIG
        SlotModeRegistry.init();
        ShortcutRegistry.init();
        RadialConfig.load();
        MacroExecutor.init();

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
            if (!sectorTexturesPrepared && client.level != null) {
                RadialScreen.prepareRenderer();
                sectorTexturesPrepared = true;
            }
            if (OPEN_RADIAL.isDown()) {
                if (!keyLocked && client.gui.screen() == null) {
                    client.gui.setScreen(new RadialScreen());
                }
            } else {
                keyLocked = false;
            }

            //noinspection StatementWithEmptyBody
            while (OPEN_RADIAL.consumeClick()) {}

            while (STOP_MACROS.consumeClick()) {
                int stopped = MacroExecutor.cancelAll();
                clearQueuedKeyPresses();
                client.gui.hud.setOverlayMessage(Component.translatable("radial.macro.stopped", stopped), false);
            }

            if (!keyPressQueue.isEmpty()) {
                tickKeyPresses();
            }
        });
    }
}
