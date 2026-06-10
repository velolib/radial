package velo.radial.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.*;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShortcutRegistry {
    private static final Map<Identifier, ShortcutEntry> REGISTRY = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static void register(Identifier id, ShortcutEntry shortcutEntry) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate registration for shortcut ID: " + id);
        }
        REGISTRY.put(id, shortcutEntry);
    }

    public static Map<Identifier, ShortcutEntry> getRegisteredShortcuts() {
        return java.util.Collections.unmodifiableMap(REGISTRY);
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Main Options
        register(Identifier.fromNamespaceAndPath("radial", "options"), new ShortcutEntry(Component.translatable("menu.options"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new OptionsScreen(parent, client.options, true));
        }));

        // Video Settings
        register(Identifier.fromNamespaceAndPath("radial", "video_settings"), new ShortcutEntry(Component.translatable("options.videoTitle"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new VideoSettingsScreen(parent, client, client.options));
        }));

        // Audio Settings
        register(Identifier.fromNamespaceAndPath("radial", "sound_options"), new ShortcutEntry(Component.translatable("options.sounds.title"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new SoundOptionsScreen(parent, client.options));
        }));

        // Chat Settings
        register(Identifier.fromNamespaceAndPath("radial", "chat_options"), new ShortcutEntry(Component.translatable("options.chat.title"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new ChatOptionsScreen(parent, client.options));
        }));

        // Accessibility Settings
        register(Identifier.fromNamespaceAndPath("radial", "accessibility_options"), new ShortcutEntry(Component.translatable("options.accessibility.title"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new AccessibilityOptionsScreen(parent, client.options));
        }));

        // Skin Customization
        register(Identifier.fromNamespaceAndPath("radial", "skin_customization"), new ShortcutEntry(Component.translatable("options.skinCustomisation.title"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new SkinCustomizationScreen(parent, client.options));
        }));

        // Main Controls Screen
        register(Identifier.fromNamespaceAndPath("radial", "controls"), new ShortcutEntry(Component.translatable("options.controls"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new ControlsScreen(parent, client.options));
        }));

        // Keybinds specific screen
        register(Identifier.fromNamespaceAndPath("radial", "keybinds"), new ShortcutEntry(Component.translatable("controls.keybinds.title"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new KeyBindsScreen(parent, client.options));
        }));

        // Mouse Settings
        register(Identifier.fromNamespaceAndPath("radial", "mouse_settings"), new ShortcutEntry(Component.translatable("options.mouse_settings.title"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new MouseSettingsScreen(parent, client.options));
        }));

        // Language Select (Requires LanguageManager in constructor)
        register(Identifier.fromNamespaceAndPath("radial", "language"), new ShortcutEntry(Component.translatable("options.language"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new LanguageSelectScreen(parent, client.options, client.getLanguageManager()));
        }));

        // Online Options
        register(Identifier.fromNamespaceAndPath("radial", "online_options"), new ShortcutEntry(Component.translatable("options.online"), (parent) -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new OnlineOptionsScreen(parent, client.options));
        }));

        // Other Mods
        FabricLoader.getInstance()
                .getEntrypointContainers("radial", RadialApiEntrypoint.class)
                .forEach(container -> container.getEntrypoint().registerShortcuts());
    }
}