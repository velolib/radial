package dev.velolib.radial.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.isxander.yacl3.api.NameableEnum;
import dev.velolib.radial.RadialClient;
import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.api.SlotMode;
import dev.velolib.radial.api.SlotModeRegistry;
import dev.velolib.radial.config.adapters.ColorTypeAdapter;
import dev.velolib.radial.config.adapters.SlotModeTypeAdapter;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class RadialConfig {

    public static final int CONFIG_VERSION = 3;

    private static final File CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("radial.json").toFile();

    private static final File TEMP_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("radial.json.tmp").toFile();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Color.class, new ColorTypeAdapter())
            .registerTypeAdapter(SlotMode.class, new SlotModeTypeAdapter())
            .create();

    public static RadialConfig INSTANCE;

    public int version = CONFIG_VERSION;

    public int slotCount = 8;
    public int slotRadius = 90;
    public int radialThickness = 90;
    public int innerDetectionBoundary = 15;
    public int outerDetectionBoundary = 25;

    public RevealAnimation revealAnimation = RevealAnimation.ZOOM;
    public int revealDurationMs = 180;
    public int hoverAnimationDurationMs = 120;

    public ActivationMode activationMode = ActivationMode.CLICK;
    public boolean enableHoverAnimation = true;
    public boolean showActivationZone = true;

    public float sectorGap = 3.0f;
    public boolean drawSectorBorders = true;
    public float sectorBorderWidth = 1.5f;
    public boolean drawOuterBorders = true;

    public Color backgroundColor = new Color(0x661A150D, true);
    public Color activationColor = new Color(0x66FFE7B0, true);
    public Color borderColor = new Color(0x99FFE7B0, true);
    public Color highlightBorderColor = new Color(0x99FFE7B0, true);
    public boolean enableBackgroundBlur = false;

    public List<RadialSlot> slots = new ArrayList<>();

    // Only keeping legacy transients for fields that actually changed names/meanings
    private transient int legacyRingRadius = -1;
    private transient int legacyInnerPadding = -1;
    private transient int legacyOuterReach = -1;
    private transient int legacyAnimationSpeedMs = -1;

    public RadialConfig() {
        ensureSlotCapacity();
    }

    public static void load() {
        if (INSTANCE == null) {
            INSTANCE = new RadialConfig();
        }

        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            RadialConfig loaded = GSON.fromJson(root, RadialConfig.class);

            if (loaded != null) {
                // Manually extract legacy fields that no longer map directly to variables
                if (root.has("ringRadius") && root.get("ringRadius").isJsonPrimitive()) {
                    loaded.legacyRingRadius = root.get("ringRadius").getAsInt();
                }
                if (root.has("innerPadding") && root.get("innerPadding").isJsonPrimitive()) {
                    loaded.legacyInnerPadding = root.get("innerPadding").getAsInt();
                }
                if (root.has("outerReach") && root.get("outerReach").isJsonPrimitive()) {
                    loaded.legacyOuterReach = root.get("outerReach").getAsInt();
                }
                if (root.has("animationSpeedMs") && root.get("animationSpeedMs").isJsonPrimitive()) {
                    loaded.legacyAnimationSpeedMs = root.get("animationSpeedMs").getAsInt();
                }

                INSTANCE = loaded;

                if (INSTANCE.version < CONFIG_VERSION) {
                    handleMigration(INSTANCE);
                } else {
                    INSTANCE.validate();
                }
            }
        } catch (Exception e) {
            RadialClient.LOGGER.error("Failed to load config! Creating backup.", e);
            backupCorruptedConfig();
            INSTANCE = new RadialConfig();
            save();
        }
    }

    private static void handleMigration(RadialConfig loaded) {
        RadialClient.LOGGER.info("Migrating Radial Config from v{} to v{}", loaded.version, CONFIG_VERSION);

        // Map legacy values to new fields if they existed in the old config
        if (loaded.legacyRingRadius >= 0) {
            loaded.slotRadius = loaded.legacyRingRadius;
        }

        if (loaded.legacyInnerPadding >= 0 && loaded.legacyOuterReach >= 0) {
            loaded.radialThickness = loaded.legacyInnerPadding + loaded.legacyOuterReach;
        }

        if (loaded.legacyAnimationSpeedMs >= 0) {
            loaded.revealDurationMs = loaded.legacyAnimationSpeedMs;
            loaded.hoverAnimationDurationMs = loaded.legacyAnimationSpeedMs;
        }

        loaded.version = CONFIG_VERSION;
        loaded.validate();
        save();
    }

    public static void save() {
        try {
            try (FileWriter writer = new FileWriter(TEMP_FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
            Files.move(
                    TEMP_FILE.toPath(),
                    CONFIG_FILE.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            RadialClient.LOGGER.error("Critical error during config save!", e);
        }
    }

    private static void backupCorruptedConfig() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        File backup = new File(CONFIG_FILE.getAbsolutePath() + ".bak");
        CONFIG_FILE.renameTo(backup);
    }

    public void validate() {
        this.slotCount = Math.clamp(this.slotCount, 2, 12);
        this.slotRadius = Math.clamp(this.slotRadius, 30, 300);
        this.radialThickness = Math.clamp(this.radialThickness, 20, 300);
        this.innerDetectionBoundary = Math.clamp(this.innerDetectionBoundary, 0, 200);
        this.outerDetectionBoundary = Math.clamp(this.outerDetectionBoundary, 0, 300);
        this.revealDurationMs = Math.clamp(this.revealDurationMs, 0, 2000);
        this.hoverAnimationDurationMs = Math.clamp(this.hoverAnimationDurationMs, 0, 2000);
        this.sectorGap = Math.clamp(this.sectorGap, 0.0f, 20.0f);
        this.sectorBorderWidth = Math.clamp(this.sectorBorderWidth, 0.0f, 10.0f);

        if (this.slots == null) {
            this.slots = new ArrayList<>();
        }

        if (this.revealAnimation == null) {
            this.revealAnimation = RevealAnimation.ZOOM;
        }

        if (this.activationMode == null) {
            this.activationMode = ActivationMode.CLICK;
        }

        if (this.backgroundColor == null) {
            this.backgroundColor = new Color(0x661A150D, true);
        }

        if (this.activationColor == null) {
            this.activationColor = new Color(0x66FFE7B0, true);
        }

        if (this.borderColor == null) {
            this.borderColor = new Color(0x99FFE7B0, true);
        }

        if (this.highlightBorderColor == null) {
            this.highlightBorderColor = new Color(0x99FFE7B0, true);
        }

        for (RadialSlot slot : this.slots) {
            if (slot == null) continue;
            if (slot.name == null) slot.name = "";
            if (slot.mode == null) slot.mode = SlotModeRegistry.getDefaultMode();
            if (slot.value == null) slot.value = "";
            if (slot.itemId == null) slot.itemId = "minecraft:air";
        }

        ensureSlotCapacity();
    }

    private void ensureSlotCapacity() {
        SlotMode defaultMode = SlotModeRegistry.getDefaultMode();
        while (slots.size() < 12) {
            slots.add(new RadialSlot("Empty Slot " + (slots.size() + 1), defaultMode, "", "minecraft:air"));
        }
    }

    public enum RevealAnimation implements NameableEnum {
        ZOOM("screen.radial.config.reveal_animation.zoom"),
        STAGGERED_CLOCKWISE("screen.radial.config.reveal_animation.staggered_clockwise"),
        STAGGERED_COUNTERCLOCKWISE("screen.radial.config.reveal_animation.staggered_counterclockwise"),
        STAGGERED_BOTH("screen.radial.config.reveal_animation.staggered_both");

        private final Component displayName;

        RevealAnimation(String translationKey) {
            this.displayName = Component.translatable(translationKey);
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }
    }

    public enum ActivationMode implements NameableEnum {
        CLICK("screen.radial.config.activation_mode.click"),
        RELEASE("screen.radial.config.activation_mode.release"),
        SCROLL_CLICK("screen.radial.config.activation_mode.scroll_click"),
        SCROLL_RELEASE("screen.radial.config.activation_mode.scroll_release");

        private final Component displayName;

        ActivationMode(String translationKey) {
            this.displayName = Component.translatable(translationKey);
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }
    }
}
