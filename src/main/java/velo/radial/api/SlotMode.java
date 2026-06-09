package velo.radial.api;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import velo.radial.ui.screen.RadialSlotEditorScreen;

public interface SlotMode {
    Component getTranslatedName();

    /**
     * Determines if this mode should show up in the selection menu.
     * Useful for checking if optional dependency mods (like MaLiLib) are loaded.
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Determines if this slot mode has a visual icon that should be rendered.
     */
    default boolean shouldRenderIcon() {
        return true;
    }

    /**
     * Should this mode execute its action if the radial menu hotkey is released while hovering?
     * (Defaults to true. Things like Submenus or Empty slots should return false).
     */
    default boolean activateOnRelease() {
        return true;
    }

    /**
     * Called when the user selects this slot in the radial menu.
     *
     * @param slot    The slot data.
     * @param context Helper to close the screen or open submenus.
     */
    default void performAction(RadialSlot slot, RadialScreenContext context) {
        // Most standard actions just want to close the screen and do their thing
        context.closeScreen();
    }

    /**
     * Called when a slot is assigned this mode or when the config is loaded.
     * Use this to initialize default data structures (like sub-slot lists).
     *
     * @param slot The slot data.
     */
    default void onInitialize(RadialSlot slot) {
        // Optional: override in modes that need child slots or specific data setup
    }

    /**
     * Called by the editor screen to construct the dynamic UI elements specific to this slot mode.
     * Implementers should instantiate their custom widgets (text fields, buttons, sliders) and append
     * them directly to the provided {@code container}.
     * <p>
     * <b>Note:</b> Manual X and Y positioning is ignored by the layout system. You only need to
     * define the width/height of your widgets and use {@code container.addChild(...)}. For complex rows,
     * nest a horizontal {@link LinearLayout} inside the container.
     *
     * @param screen    The parent editor screen (useful for opening sub-menus or pickers).
     * @param slot      The active radial slot being edited, used to read existing data and save new input.
     * @param width     The maximum available width for the layout block.
     * @param container The vertical layout container where the generated widgets should be added.
     */
    void buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container);
}