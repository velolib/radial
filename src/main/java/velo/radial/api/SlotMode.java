package velo.radial.api;

import net.minecraft.network.chat.Component;
import velo.radial.ui.RadialSlotEditorScreen;

import java.util.function.Consumer;

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
     * @param slot    The slot data.
     */
    default void onInitialize(RadialSlot slot) {
        // Optional: override in modes that need child slots or specific data setup
    }

    /**
     * Constructs the custom UI configuration widgets for this slot mode within the editor screen.
     * * <p>Implementations should use the provided {@code widgetAdder} to inject their specific
     * controls (e.g., {@link net.minecraft.client.gui.components.EditBox},
     * {@link net.minecraft.client.gui.components.Button}) into the screen's layout.
     * * <p>The provided coordinates are relative to the editor panel's content area. The implementation
     * is responsible for positioning its widgets vertically starting from {@code startY}.
     *
     * @param screen      The active editor screen instance.
     * @param slot        The {@link RadialSlot} currently being modified.
     * @param left        The absolute X-coordinate where the widget row begins.
     * @param startY      The Y-coordinate where the first widget should be rendered.
     * @param width       The maximum available horizontal space for the widgets.
     * @param widgetAdder A callback used to add rendered components to the screen's widget list.
     * Example: {@code widgetAdder.accept(new Button(...));}
     * * @return The total vertical height (in pixels) consumed by the added widgets. This value
     * is used by the editor to automatically offset the positions of subsequent UI elements.
     */
    int buildEditorWidgets(
            RadialSlotEditorScreen screen,
            RadialSlot slot,
            int left,
            int startY,
            int width,
            Consumer<net.minecraft.client.gui.components.AbstractWidget> widgetAdder
    );
}