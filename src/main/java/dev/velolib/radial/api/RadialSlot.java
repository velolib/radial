package dev.velolib.radial.api;

import dev.velolib.radial.render.SlotRenderHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

public class RadialSlot {

    public String name;
    public SlotMode mode;
    public String value;
    public String itemId;

    // Submenu configuration
    public List<RadialSlot> children = new ArrayList<>();
    public int childSlotCount = 8;

    // Macros
    public record Macro(SlotMode mode, String value) {}

    public List<Macro> macros = new ArrayList<>();

    // Optional "#RRGGBB" tint for Phosphor and glyph icons; null draws them white.
    // A string rather than an int because Gson skips the constructor, so a missing int would load as black.
    public String iconColor;

    private transient ItemStack cachedStack;
    private transient Integer cachedIconColor = null;

    public RadialSlot(String name, SlotMode mode, String value, String itemId) {
        this.name = name;
        this.mode = mode;
        this.value = value;
        this.itemId = itemId;
    }

    public void clearCache() {
        cachedStack = null;
        cachedIconColor = null;
    }

    /**
     * Returns the icon tint as 0xRRGGBB, or white when no valid color is set.
     */
    public int getIconColor() {
        if (cachedIconColor == null) {
            Integer parsed = parseIconColor(iconColor);
            cachedIconColor = parsed != null ? parsed : 0xFFFFFF;
        }

        return cachedIconColor;
    }

    /**
     * Parses "#RRGGBB" (the leading # is optional), or returns null if the text isn't a valid color.
     */
    public static Integer parseIconColor(String text) {
        if (text == null) return null;

        String hex = text.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) return null;

        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatIconColor(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /**
     * Fills in missing fields and drops invalid entries, recursing into submenu children.
     */
    public void sanitize() {
        if (name == null) name = "";
        if (mode == null) mode = SlotModeRegistry.getDefaultMode();
        if (value == null) value = "";
        if (itemId == null) itemId = "minecraft:air";

        Integer color = parseIconColor(iconColor);
        iconColor = color != null ? formatIconColor(color) : null;

        // The radial menu renders at most 12 slots (plus the back button) per ring
        childSlotCount = Math.clamp(childSlotCount, 2, 12);

        if (macros == null) macros = new ArrayList<>();
        macros.removeIf(macro -> macro == null || macro.mode() == null);

        if (children == null) children = new ArrayList<>();
        children.removeIf(Objects::isNull);
        for (RadialSlot child : children) {
            child.sanitize();
        }

        mode.onInitialize(this);
    }

    public ItemStack getRenderStack() {
        if (cachedStack == null) {
            cachedStack = SlotRenderHelper.parseItemStack(itemId);
        }

        return cachedStack;
    }
}
