package dev.velolib.radial.mode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.velolib.radial.RadialClient;
import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.api.SlotActionContext;
import dev.velolib.radial.mixin.KeyMappingAccessor;
import dev.velolib.radial.mode.base.IconEnabledSlotMode;
import dev.velolib.radial.ui.screen.KeybindPickerScreen;
import dev.velolib.radial.ui.screen.SlotEditorScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.function.Consumer;

public class KeybindSlotMode extends IconEnabledSlotMode {
    public static final HashMap<KeyMapping, Consumer<Minecraft>> SPECIAL_ACTIONS = new HashMap<>();

    static {
        // TODO: add crash debug key

        SPECIAL_ACTIONS.put(new KeyMapping("key.screenshot", GLFW.GLFW_KEY_F2, KeyMapping.Category.MISC), client -> {
            Screenshot.grab(client, false);
        });

        SPECIAL_ACTIONS.put(new KeyMapping("key.debug.overlay", GLFW.GLFW_KEY_F3, KeyMapping.Category.DEBUG), client -> {
            client.getDebugOverlay().showDebugScreen();
        });
    }

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

        for (HashMap.Entry<KeyMapping, Consumer<Minecraft>> entry : SPECIAL_ACTIONS.entrySet()) {
            if (entry.getKey().getName().equals(slot.value)) {
                entry.getValue().accept(client);
                return;
            }
        }

        for (net.minecraft.client.KeyMapping key : client.options.keyMappings) {
            if (key.getName().equals(slot.value)) {
                // SAFETY CHECK: Abort if it's an internal radial key
                if (dev.velolib.radial.RadialClient.isRadialInternalKey(key)) return;

                if (slot.value.startsWith("key.debug")) {
                    InputConstants.Key inputKey = ((KeyMappingAccessor) key).getKey();
                    int keyCode = inputKey.getValue();
                    var dummyEvent = new KeyEvent(keyCode, 0, 0);
                    client.keyboardHandler.handleDebugKeys(dummyEvent);
                }

                RadialClient.scheduleKeyPress(key);
                break;
            }
        }
    }
}