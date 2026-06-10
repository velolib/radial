package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.ui.screen.KeybindPickerScreen;
import velo.radial.ui.screen.RadialSlotEditorScreen;

public class KeybindSlotMode extends IconEnabledSlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.keybind");
    }

    @Override
    public void buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
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

        Button valueBrowseButton = Button.builder(Component.translatable("screen.radial.editor.select"), _ -> Minecraft.getInstance().setScreen(new KeybindPickerScreen(screen, id -> {
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
    public void performAction(RadialSlot slot, RadialScreenContext context) {
        context.closeScreen();

        Minecraft client = Minecraft.getInstance();
        for (net.minecraft.client.KeyMapping key : client.options.keyMappings) {
            if (key.getName().equals(slot.value)) {
                velo.radial.RadialClient.scheduleKeyPress(key);
                break;
            }
        }
    }
}