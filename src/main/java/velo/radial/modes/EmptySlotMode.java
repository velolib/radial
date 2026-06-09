package velo.radial.modes;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.api.SlotMode;
import velo.radial.ui.screen.RadialSlotEditorScreen;

public class EmptySlotMode implements SlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.empty");
    }

    @Override
    public boolean shouldRenderIcon() {
        return false;
    }

    @Override
    public boolean activateOnRelease() {
        return false; // Don't trigger when releasing on empty slots
    }

    @Override
    public void performAction(RadialSlot slot, RadialScreenContext context) {
        // Do absolutely nothing. Do not even call context.closeScreen()!
    }

    @Override
    public void buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        // The empty mode has no custom widgets and consumes no space.
    }
}