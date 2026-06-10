package velo.radial.modes.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import velo.radial.api.RadialSlot;
import velo.radial.api.SlotMode;
import velo.radial.ui.screen.IconPickerScreen;
import velo.radial.ui.screen.RadialSlotEditorScreen;

public abstract class IconEnabledSlotMode implements SlotMode {

    /**
     * Helper to build the 3 icon widgets (Text Field, Browse Button, Hand Button)
     * using modern layout managers.
     */
    protected void buildIconRow(RadialSlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        int HORIZ_GAP = 5;
        int ICON_BTN_WIDTH = 55;
        int ROW_HEIGHT = 20;
        int iconFieldWidth = width - (ICON_BTN_WIDTH * 2) - (HORIZ_GAP * 2);

        // Group the label and the row of inputs together
        LinearLayout iconGroup = LinearLayout.vertical().spacing(2);

        // 1. Label
        StringWidget label = new StringWidget(Component.translatable("screen.radial.editor.icon"), Minecraft.getInstance().font);
        iconGroup.addChild(label);

        // 2. Horizontal row for Field + Buttons
        LinearLayout inputRow = LinearLayout.horizontal().spacing(HORIZ_GAP);

        // Icon EditBox
        EditBox iconField = new EditBox(Minecraft.getInstance().font, 0, 0, iconFieldWidth, ROW_HEIGHT, Component.translatable("screen.radial.editor.icon"));
        iconField.setMaxLength(Integer.MAX_VALUE);
        iconField.setValue(slot.itemId != null ? slot.itemId : "minecraft:stone");
        iconField.setResponder(v -> {
            slot.itemId = v;
            slot.clearCache();
        });
        inputRow.addChild(iconField);

        // Browse Button
        Button browseIconButton = Button.builder(Component.translatable("screen.radial.editor.browse"), _ -> Minecraft.getInstance().setScreen(new IconPickerScreen(screen, id -> {
            iconField.setValue(id);
            slot.itemId = id;
            slot.clearCache();
        }))).bounds(0, 0, ICON_BTN_WIDTH, ROW_HEIGHT).build();
        inputRow.addChild(browseIconButton);

        // Hand Button
        Button handButton = Button.builder(Component.translatable("screen.radial.editor.hand"), _ -> {
            if (Minecraft.getInstance().player != null) {
                ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
                String id = !stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() : "minecraft:air";
                iconField.setValue(id);
                slot.itemId = id;
                slot.clearCache();
            }
        }).bounds(0, 0, ICON_BTN_WIDTH, ROW_HEIGHT).build();
        inputRow.addChild(handButton);

        // Add the horizontal row into the vertical group, then add the group to the main container
        iconGroup.addChild(inputRow);
        container.addChild(iconGroup);
    }
}