package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.api.RadialSlotModes;
import velo.radial.modes.base.IconEnabledSlotMode;
import velo.radial.ui.screen.RadialSlotEditorScreen;

import java.util.ArrayList;

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

        while (slot.children.size() < slot.childSlotCount) {
            slot.children.add(new RadialSlot("Sub Slot " + (slot.children.size() + 1), RadialSlotModes.getRegisteredModes().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("radial", "empty")), "", "minecraft:stone"));
        }
    }

    @Override
    public void buildEditorWidgets(RadialSlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        int ROW_HEIGHT = 20;

        LinearLayout subGroup = LinearLayout.vertical().spacing(2);

        StringWidget label = new StringWidget(Component.translatable("screen.radial.editor.submenu"), Minecraft.getInstance().font);
        subGroup.addChild(label);

        // Pass 0, 0 for X and Y, the layout will override it automatically
        AbstractSliderButton subCountSlider = new AbstractSliderButton(0, 0, width, ROW_HEIGHT, Component.translatable("screen.radial.editor.sub_size", slot.childSlotCount), (slot.childSlotCount - 2) / 10.0) {
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
        subGroup.addChild(subCountSlider);

        container.addChild(subGroup);

        // Icon Row
        buildIconRow(screen, slot, width, container);
    }
}