package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.modes.base.IconEnabledSlotMode;
import velo.radial.ui.screen.RadialSlotEditorScreen;

public class ChatSlotMode extends IconEnabledSlotMode {

    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.chat");
    }

    @Override
    public void buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        // Group the Label and EditBox together closely
        LinearLayout valueGroup = LinearLayout.vertical().spacing(2);

        StringWidget label = new StringWidget(Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        valueGroup.addChild(label);

        EditBox valueField = new EditBox(Minecraft.getInstance().font, 0, 0, width, 20, Component.translatable("screen.radial.editor.value"));
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
    public void performAction(RadialSlot slot, RadialScreenContext context) {
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