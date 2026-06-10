package velo.radial.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import velo.radial.RadialClient;
import velo.radial.api.RadialSlot;
import velo.radial.api.RadialSlotModeRegistry;
import velo.radial.api.SlotMode;
import velo.radial.config.adapters.ColorTypeAdapter;
import velo.radial.config.adapters.SlotModeTypeAdapter;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class RadialConfig {
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
    public static final int CONFIG_VERSION = 2;

    public int version = 2;
    public int slotCount = 8;
    public int ringRadius = 75;
    public int innerPadding = 40;
    public int outerReach = 100;
    public int animationSpeedMs = 200;
    public ActivationMode activationMode = ActivationMode.CLICK;
    public boolean showActivationZone = true;
    public float sectorGap = 2f;

    public Color activationColor = new Color(0x44FFFFFF, true);
    public Color backgroundColor = new Color(0x66000000, true);

    public List<RadialSlot> slots = new ArrayList<>();

    public RadialConfig() {
        ensureSlotCapacity();
    }

    /**
     * Loads the config, handles version migrations, and validates data.
     */
    public static void load() {
        // Create a blank instance if one doesn't exist yet
        if (INSTANCE == null) {
            INSTANCE = new RadialConfig();
        }

        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            RadialConfig loaded = GSON.fromJson(reader, RadialConfig.class);
            if (loaded != null) {
                INSTANCE = loaded;
                INSTANCE.validate();
            }
        } catch (Exception e) {
            RadialClient.LOGGER.error("Failed to load config! Creating backup.", e);
            backupCorruptedConfig();
            INSTANCE = new RadialConfig();
            save();
        }

        if (INSTANCE != null && INSTANCE.version < CONFIG_VERSION) {
            handleMigration(INSTANCE);
        }
    }

    /**
     * Placeholder for future data transformations.
     */
    private static void handleMigration(RadialConfig loaded) {
        RadialClient.LOGGER.info("Migrating Radial Config from v{} to v{}", loaded.version, CONFIG_VERSION);
        loaded.version = CONFIG_VERSION;
        save();
    }

    /**
     * Saves the config using an Atomic Move pattern to prevent corruption.
     */
    public static void save() {
        try {
            try (FileWriter writer = new FileWriter(TEMP_FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
            Files.move(TEMP_FILE.toPath(), CONFIG_FILE.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            RadialClient.LOGGER.error("Critical error during config save!", e);
        }
    }

    private static void backupCorruptedConfig() {
        if (CONFIG_FILE.exists()) {
            File backup = new File(CONFIG_FILE.getAbsolutePath() + ".bak");
            CONFIG_FILE.renameTo(backup);
        }
    }

    /**
     * Clamps all values to valid ranges. This acts as a firewall against
     * corrupted or manually malformed JSON files.
     */
    public void validate() {
        this.slotCount = Math.max(2, Math.min(this.slotCount, 12));
        this.ringRadius = Math.max(30, Math.min(this.ringRadius, 200));
        this.innerPadding = Math.max(0, Math.min(this.innerPadding, 200));
        this.outerReach = Math.max(10, Math.min(this.outerReach, 300));
        this.animationSpeedMs = Math.max(0, Math.min(this.animationSpeedMs, 2000));
        this.sectorGap = Math.max(0, Math.min(this.sectorGap, 20));

        if (this.slots == null) {
            this.slots = new ArrayList<>();
        }

        if (this.activationMode == null) {
            this.activationMode = ActivationMode.CLICK;
        }

        for (RadialSlot slot : this.slots) {
            if (slot == null) continue;
            if (slot.name == null) slot.name = "";
            if (slot.mode == null) slot.mode = RadialSlotModeRegistry.getDefaultMode();
            if (slot.value == null) slot.value = "";
            if (slot.itemId == null) slot.itemId = "minecraft:air";
        }

        ensureSlotCapacity();
    }

    private void ensureSlotCapacity() {
        // Fetch it once outside the loop for performance
        SlotMode defaultMode = RadialSlotModeRegistry.getDefaultMode();

        while (slots.size() < 12) {
            slots.add(new RadialSlot(
                    "Empty Slot " + (slots.size() + 1),
                    defaultMode,
                    "",
                    "minecraft:air"
            ));
        }
    }

    public enum ActivationMode {
        CLICK,
        RELEASE // This represents the "Hover and release key" behavior
    }
}