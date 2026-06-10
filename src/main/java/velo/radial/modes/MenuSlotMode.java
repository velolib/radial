package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import velo.radial.api.RadialMenuEntry;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.modes.base.IconEnabledSlotMode;
import velo.radial.ui.screen.MenuSelectionScreen;
import velo.radial.ui.screen.RadialSlotEditorScreen;

public class MenuSlotMode extends IconEnabledSlotMode {

    @Override
    public Component getTranslatedName() {
        return Component.translatable("mode.radial.menu");
    }

    @Override
    public void performAction(RadialSlot slot, RadialScreenContext context) {
        // 1. Ensure the slot actually has a value configured
        if (slot.value == null || slot.value.isBlank()) {
            return;
        }

        // 2. Parse the string back into an Identifier safely
        Identifier menuId = Identifier.tryParse(slot.value);
        if (menuId == null) {
            return;
        }

        // 3. Look up the corresponding RadialMenuEntry in our registry
        RadialMenuEntry entry = velo.radial.api.RadialMenuEntryRegistry.getRegisteredMenus().get(menuId);

        // 4. If the menu exists and has an action, execute it
        if (entry != null && entry.openAction() != null) {
            // Since the openAction() usually contains Minecraft.getInstance().setScreen(...),
            // calling this will automatically close the radial menu and open the target screen.
            entry.openAction().accept(null);
        }
    }

    @Override
    public void buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        int HORIZ_GAP = 5;
        int BROWSE_BTN_WIDTH = 55;
        int ROW_HEIGHT = 20;
        int valueFieldWidth = width - BROWSE_BTN_WIDTH - HORIZ_GAP;

        LinearLayout valueGroup = LinearLayout.vertical().spacing(2);

        StringWidget label = new StringWidget(Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        valueGroup.addChild(label);

        LinearLayout inputRow = LinearLayout.horizontal().spacing(HORIZ_GAP);

        EditBox valueField = new EditBox(Minecraft.getInstance().font, 0, 0, valueFieldWidth, ROW_HEIGHT, Component.translatable("screen.radial.editor.value"));
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value != null ? slot.value : "");
        valueField.setResponder(v -> slot.value = v);
        inputRow.addChild(valueField);

        // UPDATE: The callback now directly receives the Identifier
        Button valueBrowseButton = Button.builder(Component.translatable("screen.radial.editor.select"), _ ->
                Minecraft.getInstance().setScreen(new MenuSelectionScreen(screen, (Identifier selectedId) -> {
                    String idString = selectedId.toString();
                    valueField.setValue(idString);
                    slot.value = idString;
                }))
        ).bounds(0, 0, BROWSE_BTN_WIDTH, ROW_HEIGHT).build();

        inputRow.addChild(valueBrowseButton);

        valueGroup.addChild(inputRow);
        container.addChild(valueGroup);

        buildIconRow(screen, slot, width, container);
    }
}