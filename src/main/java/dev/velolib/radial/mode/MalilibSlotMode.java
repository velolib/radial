package dev.velolib.radial.mode;

import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.api.SlotActionContext;
import dev.velolib.radial.integration.MalilibIntegration;
import dev.velolib.radial.mode.base.IconEnabledSlotMode;
import dev.velolib.radial.ui.screen.MalilibSelectionScreen;
import dev.velolib.radial.ui.screen.SlotEditorScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

public class MalilibSlotMode extends IconEnabledSlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.malilib");
    }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("malilib");
    }

    @Override
    public void performAction(RadialSlot slot, SlotActionContext context) {
        context.closeScreen();

        if (FabricLoader.getInstance().isModLoaded("malilib")) {
            MalilibIntegration.executeHotkey(slot.value);
        }
    }

    @Override
    public void buildEditorWidgets(SlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        int HORIZ_GAP = 5;
        int BROWSE_BTN_WIDTH = 55;
        int ROW_HEIGHT = 20;
        int valueFieldWidth = width - BROWSE_BTN_WIDTH - HORIZ_GAP;

        LinearLayout valueGroup = LinearLayout.vertical().spacing(2);

        StringWidget label =
                new StringWidget(Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        valueGroup.addChild(label);

        LinearLayout inputRow = LinearLayout.horizontal().spacing(HORIZ_GAP);

        EditBox valueField = new EditBox(
                Minecraft.getInstance().font,
                0,
                0,
                valueFieldWidth,
                ROW_HEIGHT,
                Component.translatable("screen.radial.editor.value"));
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value != null ? slot.value : "");
        valueField.setResponder(v -> slot.value = v);
        inputRow.addChild(valueField);

        Button valueBrowseButton = Button.builder(
                        Component.translatable("screen.radial.editor.select"),
                        _ -> Minecraft.getInstance().gui.setScreen(new MalilibSelectionScreen(screen, action -> {
                            valueField.setValue(action.id());
                            slot.value = action.id();
                        })))
                .bounds(0, 0, BROWSE_BTN_WIDTH, ROW_HEIGHT)
                .build();
        inputRow.addChild(valueBrowseButton);

        valueGroup.addChild(inputRow);
        container.addChild(valueGroup);

        buildIconRow(screen, slot, width, container);
    }
}
