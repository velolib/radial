package dev.velolib.radial.mode;

import dev.velolib.radial.RadialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.api.SlotActionContext;
import dev.velolib.radial.mode.base.IconEnabledSlotMode;
import dev.velolib.radial.ui.screen.KeybindPickerScreen;
import dev.velolib.radial.ui.screen.SlotEditorScreen;

public class KeybindSlotMode extends IconEnabledSlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.keybind");
    }

    @Override
    public void buildEditorWidgets(SlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        int HORIZ_GAP = 5;
        int BROWSE_BTN_WIDTH = 55;
        int ROW_HEIGHT = 20;
        int valueFieldWidth = width - BROWSE_BTN_WIDTH - HORIZ_GAP;

        // Group the label and row together vertically
        LinearLayout valueGroup = LinearLayout.vertical().spacing(2);

        StringWidget label = new StringWidget(Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        valueGroup.addChild(label);

        // Horizontal row for the field + picker button
        LinearLayout inputRow = LinearLayout.horizontal().spacing(HORIZ_GAP);

        EditBox valueField = new EditBox(Minecraft.getInstance().font, 0, 0, valueFieldWidth, ROW_HEIGHT, Component.translatable("screen.radial.editor.value"));
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value != null ? slot.value : "");
        valueField.setResponder(v -> slot.value = v);
        inputRow.addChild(valueField);

        Button valueBrowseButton = Button.builder(Component.translatable("screen.radial.editor.select"), _ -> Minecraft.getInstance().gui.setScreen(new KeybindPickerScreen(screen, id -> {
            valueField.setValue(id);
            slot.value = id;
        }))).bounds(0, 0, BROWSE_BTN_WIDTH, ROW_HEIGHT).build();
        inputRow.addChild(valueBrowseButton);

        valueGroup.addChild(inputRow);
        container.addChild(valueGroup);

        // Icon Row
        buildIconRow(screen, slot, width, container);
    }

    @Override
    public void performAction(RadialSlot slot, SlotActionContext context) {
        context.closeScreen();

        Minecraft client = Minecraft.getInstance();
        for (net.minecraft.client.KeyMapping key : client.options.keyMappings) {
            if (key.getName().equals(slot.value)) {
                RadialClient.scheduleKeyPress(key);
                break;
            }
        }
    }
}