package velo.radial.modes;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.integration.MalilibIntegration;
import velo.radial.ui.screen.MalilibSelectionScreen;
import velo.radial.ui.screen.RadialSlotEditorScreen;

import java.util.function.Consumer;

public class MalilibSlotMode extends IconEnabledSlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.malilib");
    }

    @Override
    public boolean isAvailable() {
        // Automatically hide this mode if MaLiLib isn't loaded!
        return FabricLoader.getInstance().isModLoaded("malilib");
    }

    @Override
    public void performAction(RadialSlot slot, RadialScreenContext context) {
        context.closeScreen();

        // Ensure Malilib is actually loaded before trying to hit the bridge class
        if (FabricLoader.getInstance().isModLoaded("malilib")) {
            MalilibIntegration.executeHotkey(slot.value);
        }
    }

    @Override
    public int buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int left, int startY, int width, Consumer<AbstractWidget> widgetAdder) {
        int HORIZ_GAP = 5;
        int BROWSE_BTN_WIDTH = 55;
        int ROW_HEIGHT = 20;
        int VERT_GAP = 38;
        int currentY = startY;

        int valueFieldWidth = width - BROWSE_BTN_WIDTH - HORIZ_GAP;

        StringWidget label = new StringWidget(left, currentY - 12, width, 10, Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        widgetAdder.accept(label);

        EditBox valueField = new EditBox(Minecraft.getInstance().font, left, currentY, valueFieldWidth, ROW_HEIGHT, Component.translatable("screen.radial.editor.value"));
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value != null ? slot.value : "");
        valueField.setResponder(v -> slot.value = v);
        widgetAdder.accept(valueField);

        Button valueBrowseButton = Button.builder(Component.translatable("screen.radial.editor.select"), _ -> {
            Minecraft.getInstance().setScreen(new MalilibSelectionScreen(screen, action -> {
                valueField.setValue(action.id());
                slot.value = action.id();
            }));
        }).bounds(left + valueFieldWidth + HORIZ_GAP, currentY, BROWSE_BTN_WIDTH, ROW_HEIGHT).build();
        widgetAdder.accept(valueBrowseButton);

        currentY += VERT_GAP;

        buildIconRow(screen, slot, left, currentY, width, widgetAdder);

        return (currentY - startY) + ROW_HEIGHT;
    }
}