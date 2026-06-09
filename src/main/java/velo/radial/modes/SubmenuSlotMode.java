package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.api.RadialSlotModes;
import velo.radial.ui.RadialSlotEditorScreen;

import java.util.ArrayList;
import java.util.function.Consumer;

public class SubmenuSlotMode extends IconEnabledSlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.submenu");
    }

    @Override
    public boolean activateOnRelease() {
        return false; // Submenus only open on click, not hover release
    }

    @Override
    public void performAction(RadialSlot slot, RadialScreenContext context) {
        if (context.isRoot()) {
            context.openSubmenu(slot.children, slot.childSlotCount);
        }
    }

    @Override
    public void onInitialize(RadialSlot slot) {
        if (slot.children == null) slot.children = new ArrayList<>();

        // Fills missing children if the slider was increased
        while (slot.children.size() < slot.childSlotCount) {
            slot.children.add(new RadialSlot("Sub Slot " + (slot.children.size() + 1), RadialSlotModes.getRegisteredModes().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("radial", "empty")), "", "minecraft:stone"));
        }
    }

    @Override
    public int buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int left, int startY, int width, Consumer<AbstractWidget> widgetAdder) {
        int ROW_HEIGHT = 20;
        int VERT_GAP = 38;
        int currentY = startY;

        // 1. Label
        StringWidget label = new StringWidget(left, currentY - 12, width, 10, Component.translatable("screen.radial.editor.submenu"), Minecraft.getInstance().font);
        widgetAdder.accept(label);

        // 2. Slider
        AbstractSliderButton subCountSlider = new AbstractSliderButton(left, currentY, width, ROW_HEIGHT, Component.translatable("screen.radial.editor.sub_size", slot.childSlotCount), (slot.childSlotCount - 2) / 10.0) {
            @Override
            protected void updateMessage() {
                int val = 2 + (int) Math.round(value * 10);
                setMessage(Component.translatable("screen.radial.editor.sub_size", val));
            }

            @Override
            protected void applyValue() {
                slot.childSlotCount = 2 + (int) Math.round(value * 10);
                onInitialize(slot); // Trigger the array resize logic
            }
        };
        widgetAdder.accept(subCountSlider);

        currentY += VERT_GAP;

        // 3. Icon Row
        buildIconRow(screen, slot, left, currentY, width, widgetAdder);

        // Return total height consumed (Slider + Gap + Icon Row)
        return (currentY - startY) + ROW_HEIGHT;
    }
}