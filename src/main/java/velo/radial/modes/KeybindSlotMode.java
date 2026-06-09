package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.ui.KeybindPickerScreen;
import velo.radial.ui.RadialSlotEditorScreen;

import java.util.function.Consumer;

public class KeybindSlotMode extends IconEnabledSlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.keybind");
    }

    @Override
    public int buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int left, int startY, int width, Consumer<AbstractWidget> widgetAdder) {
        int HORIZ_GAP = 5;
        int BROWSE_BTN_WIDTH = 55;
        int ROW_HEIGHT = 20;
        int VERT_GAP = 38;
        int currentY = startY;

        int valueFieldWidth = width - BROWSE_BTN_WIDTH - HORIZ_GAP;

        // 1. Label
        StringWidget label = new StringWidget(left, currentY - 12, width, 10, Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        widgetAdder.accept(label);

        // 2. Value Input
        EditBox valueField = new EditBox(Minecraft.getInstance().font, left, currentY, valueFieldWidth, ROW_HEIGHT, Component.translatable("screen.radial.editor.value"));
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value != null ? slot.value : "");
        valueField.setResponder(v -> slot.value = v);
        widgetAdder.accept(valueField);

        // 3. Picker Button
        Button valueBrowseButton = Button.builder(Component.translatable("screen.radial.editor.select"), _ -> {
            Minecraft.getInstance().setScreen(new KeybindPickerScreen(screen, id -> {
                valueField.setValue(id);
                slot.value = id;
            }));
        }).bounds(left + valueFieldWidth + HORIZ_GAP, currentY, BROWSE_BTN_WIDTH, ROW_HEIGHT).build();
        widgetAdder.accept(valueBrowseButton);

        currentY += VERT_GAP;

        // 4. Icon Row
        buildIconRow(screen, slot, left, currentY, width, widgetAdder);

        return (currentY - startY) + ROW_HEIGHT;
    }

    @Override
    public void performAction(RadialSlot slot, RadialScreenContext context) {
        context.closeScreen(); // Close the UI first

        // Your old execution logic goes here
        Minecraft client = Minecraft.getInstance();
        for (net.minecraft.client.KeyMapping key : client.options.keyMappings) {
            if (key.getName().equals(slot.value)) {
                velo.radial.RadialClient.scheduleKeyPress(key);
                break;
            }
        }
    }
}