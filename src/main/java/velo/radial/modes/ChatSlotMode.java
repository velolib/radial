package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.ui.RadialSlotEditorScreen;

import java.util.function.Consumer;

public class ChatSlotMode extends IconEnabledSlotMode {

    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.chat");
    }

    @Override
    public int buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int left, int startY, int width, Consumer<AbstractWidget> widgetAdder) {
        int ROW_HEIGHT = 20;
        int VERT_GAP = 38;
        int currentY = startY;

        // 1. Label
        StringWidget label = new StringWidget(left, currentY - 12, width, 10, Component.translatable("screen.radial.editor.value"), Minecraft.getInstance().font);
        widgetAdder.accept(label);

        // 2. Value Input (Full width, no browse button)
        EditBox valueField = new EditBox(Minecraft.getInstance().font, left, currentY, width, ROW_HEIGHT, Component.translatable("screen.radial.editor.value"));
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value != null ? slot.value : "");
        valueField.setResponder(v -> slot.value = v);
        widgetAdder.accept(valueField);

        currentY += VERT_GAP;

        // 3. Icon Row
        buildIconRow(screen, slot, left, currentY, width, widgetAdder);

        return (currentY - startY) + ROW_HEIGHT;
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