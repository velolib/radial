package velo.radial.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import velo.radial.mode.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class SlotModeRegistry {
    private static final Map<Identifier, SlotMode> REGISTRY = new LinkedHashMap<>();
    private static final Identifier EMPTY_ID = Identifier.fromNamespaceAndPath("radial", "empty");
    private static boolean initialized = false;

    public static void register(Identifier id, SlotMode mode) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate registration for mode ID: " + id);
        }
        REGISTRY.put(id, mode);
    }

    public static Map<Identifier, SlotMode> getRegisteredModes() {
        return java.util.Collections.unmodifiableMap(REGISTRY);
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        register(EMPTY_ID, new EmptySlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "chat"), new ChatSlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "keybind"), new KeybindSlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "menu"), new ShortcutSlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "malilib"), new MalilibSlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "submenu"), new SubmenuSlotMode());

        FabricLoader.getInstance()
                .getEntrypointContainers("radial", RadialApiEntrypoint.class)
                .forEach(container -> container.getEntrypoint().registerSlotModes());
    }

    public static SlotMode getDefaultMode() {
        SlotMode mode = REGISTRY.get(EMPTY_ID);
        if (mode == null) {
            throw new IllegalStateException("Critical error: radial:empty mode was not registered!");
        }
        return mode;
    }
}