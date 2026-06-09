package velo.radial.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import velo.radial.api.RadialSlot;
import velo.radial.api.SlotMode;
import velo.radial.ui.IconPickerScreen;
import velo.radial.ui.RadialSlotEditorScreen;

import java.util.function.Consumer;

public abstract class IconEnabledSlotMode implements SlotMode {

    /**
     * Helper to build the 3 icon widgets (Text Field, Browse Button, Hand Button)
     * and the label above them.
     */
    protected int buildIconRow(RadialSlotEditorScreen screen, RadialSlot slot, int left, int startY, int width, Consumer<AbstractWidget> widgetAdder) {
        int HORIZ_GAP = 5;
        int ICON_BTN_WIDTH = 55;
        int ROW_HEIGHT = 20;
        int iconFieldWidth = width - (ICON_BTN_WIDTH * 2) - (HORIZ_GAP * 2);

        // Render the label (replaces the hardcoded extractRenderState text)
        StringWidget label = new StringWidget(left, startY - 12, width, 10, Component.translatable("screen.radial.editor.icon"), Minecraft.getInstance().font);
        widgetAdder.accept(label);

        // Icon EditBox
        EditBox iconField = new EditBox(Minecraft.getInstance().font, left, startY, iconFieldWidth, ROW_HEIGHT, Component.translatable("screen.radial.editor.icon"));
        iconField.setMaxLength(Integer.MAX_VALUE);
        iconField.setValue(slot.itemId != null ? slot.itemId : "minecraft:stone");
        iconField.setResponder(v -> {
            slot.itemId = v;
            slot.clearCache();
        });
        widgetAdder.accept(iconField);

        // Browse Button
        int browseIconX = left + iconFieldWidth + HORIZ_GAP;
        Button browseIconButton = Button.builder(Component.translatable("screen.radial.editor.browse"), _ -> {
            Minecraft.getInstance().setScreen(new IconPickerScreen(screen, id -> {
                iconField.setValue(id);
                slot.itemId = id;
                slot.clearCache();
            }));
        }).bounds(browseIconX, startY, ICON_BTN_WIDTH, ROW_HEIGHT).build();
        widgetAdder.accept(browseIconButton);

        // Hand Button
        int handIconX = browseIconX + ICON_BTN_WIDTH + HORIZ_GAP;
        Button handButton = Button.builder(Component.translatable("screen.radial.editor.hand"), _ -> {
            if (Minecraft.getInstance().player != null) {
                ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
                String id = !stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() : "minecraft:air";
                iconField.setValue(id);
                slot.itemId = id;
                slot.clearCache();
            }
        }).bounds(handIconX, startY, ICON_BTN_WIDTH, ROW_HEIGHT).build();
        widgetAdder.accept(handButton);

        return ROW_HEIGHT;
    }
}