package velo.radial.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import velo.radial.RadialClient;

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
            .create();
    public static RadialConfig INSTANCE = new RadialConfig();
    public int version = 1;
    public int slotCount = 8;
    public int ringRadius = 75;
    public int innerPadding = 40;
    public int outerReach = 100;
    public int animationSpeedMs = 200;
    public ActivationMode activationMode = ActivationMode.CLICK;
    public boolean showActivationZone = true;
    public float sectorGap = 2;
    public Color activationColor = new Color(255, 255, 0, 50);
    public Color backgroundColor = new Color(0, 0, 0, 50);
    public List<RadialSlot> slots = new ArrayList<>();

    public RadialConfig() {
        ensureSlotCapacity();
    }

    /**
     * Loads the config, handles version migrations, and validates data.
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            RadialConfig loaded = GSON.fromJson(reader, RadialConfig.class);
            if (loaded != null) {
                // Handle version upgrades
                if (loaded.version < INSTANCE.version) {
                    handleMigration(loaded);
                }

                INSTANCE = loaded;
                INSTANCE.validate();
            }
        } catch (Exception e) {
            RadialClient.LOGGER.error("Failed to load config! Creating backup.");
            backupCorruptedConfig();
            INSTANCE = new RadialConfig();
            save();
        }
    }

    /**
     * Placeholder for future data transformations.
     */
    private static void handleMigration(RadialConfig loaded) {
        // Logic for converting version 1 to 2, etc., goes here.
        // This currently does nothing but update the version number.
        loaded.version = INSTANCE.version;
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
            if (slot.mode == null) slot.mode = SlotMode.EMPTY;
            if (slot.value == null) slot.value = "";
            if (slot.itemId == null) slot.itemId = "minecraft:air";
        }

        ensureSlotCapacity();
    }

    private void ensureSlotCapacity() {
        while (slots.size() < 12) {
            slots.add(new RadialSlot(
                    "Empty Slot " + (slots.size() + 1),
                    SlotMode.EMPTY,
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