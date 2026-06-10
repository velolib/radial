package velo.radial.api;

import java.util.List;

/**
 * A callback interface passed to SlotModes when they are clicked.
 * It allows external modes to safely interact with the radial screen's state.
 */
public interface SlotActionContext {
    /**
     * Closes the radial menu and locks the key to prevent immediate re-opening.
     */
    void closeScreen();

    /**
     * Transitions the radial menu into a submenu.
     */
    void openSubmenu(List<RadialSlot> children, int slotCount);

    /**
     * @return true if the menu is currently on the root ring.
     */
    boolean isRoot();
}