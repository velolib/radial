package dev.velolib.radial.mode;

import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.api.SlotActionContext;
import dev.velolib.radial.mode.base.IconEnabledSlotMode;
import dev.velolib.radial.ui.screen.SlotEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public class ChatSlotMode extends IconEnabledSlotMode {

    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.chat");
    }

    @Override
    public void buildEditorWidgets(SlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        // Group the Label and EditBox together closely
        LinearLayout valueGroup = LinearLayout.vertical().spacing(2);

        StringWidget label =
                new StringWidget(Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        valueGroup.addChild(label);

        EditBox valueField = new EditBox(
                Minecraft.getInstance().font, 0, 0, width, 20, Component.translatable("screen.radial.editor.value"));
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value != null ? slot.value : "");
        valueField.setResponder(v -> slot.value = v);
        valueGroup.addChild(valueField);

        // Add the group to the main container
        container.addChild(valueGroup);

        // 3. Icon Row
        buildIconRow(screen, slot, width, container);
    }

    @Override
    public void performAction(RadialSlot slot, SlotActionContext context) {
        context.closeScreen(); // Close the radial menu first

        if (slot.value == null || slot.value.isEmpty()) return;

        // Execute the chat message or command
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            if (slot.value.startsWith("/")) {
                connection.sendCommand(slot.value.substring(1)); // Remove the slash for commands
            } else {
                connection.sendChat(slot.value); // Send as normal chat
            }
        }
    }
}
