package velo.radial.api;

import net.minecraft.resources.Identifier;
import velo.radial.modes.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class RadialSlotModes {
    private static final Map<Identifier, SlotMode> REGISTRY = new LinkedHashMap<>();

    private static final Identifier EMPTY_ID = Identifier.fromNamespaceAndPath("radial", "empty");

    // --- STATIC INITIALIZER ---
    // This runs automatically the first time this class is loaded into memory.
    static {
        register(EMPTY_ID, new EmptySlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "chat"), new ChatSlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "keybind"), new KeybindSlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "malilib"), new MalilibSlotMode());
        register(Identifier.fromNamespaceAndPath("radial", "submenu"), new SubmenuSlotMode());
    }

    public static void register(Identifier id, SlotMode mode) {
        REGISTRY.put(id, mode);
    }

    public static Map<Identifier, SlotMode> getRegisteredModes() {
        return REGISTRY;
    }

    public static SlotMode getDefaultMode() {
        SlotMode mode = REGISTRY.get(EMPTY_ID);
        if (mode == null) {
            throw new IllegalStateException("Critical error: radial:empty mode was not registered!");
        }
        return mode;
    }
}