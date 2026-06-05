package velo.radial.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import velo.radial.config.RadialConfig;
import velo.radial.config.RadialSlot;
import velo.radial.config.SlotMode;

import java.util.ArrayList;

public class RadialSlotEditorScreen extends Screen {

    private static final Identifier SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final int SLOT_SIZE = 26;

    // LAYOUT CONSTANTS
    private static final int ROW_HEIGHT = 20;
    private static final int VERT_GAP = 38;
    private static final int HORIZ_GAP = 5;
    private static final int BROWSE_BTN_WIDTH = 55;
    private static final int ICON_BTN_WIDTH = 55;

    private final RadialSlot slot;
    private final boolean isRoot;

    private final String oldName, oldValue, oldId;
    private final SlotMode oldMode;
    private final int oldChildCount;

    private boolean isSaved = false;

    private EditBox nameField;
    private EditBox valueField;
    private EditBox iconField;

    private AbstractSliderButton subCountSlider;

    private Button modeButton;
    private Button browseIconButton;
    private Button handButton;
    private Button valueBrowseButton;
    private Button saveButton;
    private Button cancelButton;

    public RadialSlotEditorScreen(RadialSlot slot, boolean isRoot) {
        super(Component.translatable("screen.radial.editor.title"));
        this.slot = slot;
        this.isRoot = isRoot;

        this.oldName = slot.name;
        this.oldValue = slot.value;
        this.oldId = slot.itemId;
        this.oldMode = slot.mode;
        this.oldChildCount = slot.childSlotCount;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int contentWidth = Math.min(300, (int) (width * 0.9));
        int left = centerX - (contentWidth / 2);

        // Base Y coordinates for rows
        int row1Y = height / 2 - 60;
        int row2Y = row1Y + VERT_GAP;
        int row3Y = row2Y + VERT_GAP;
        int row4Y = row3Y + VERT_GAP;
        int row5Y = row4Y + VERT_GAP + 10; // Extra gap for action buttons

        // ROW 1: Name
        nameField = new EditBox(
                font, left, row1Y, contentWidth, ROW_HEIGHT,
                Component.translatable("screen.radial.editor.name")
        );
        nameField.setMaxLength(Integer.MAX_VALUE);
        nameField.setValue(slot.name);
        nameField.setResponder(v -> slot.name = v);
        addRenderableWidget(nameField);

        // ROW 2: Mode
        modeButton = Button.builder(
                Component.nullToEmpty("Type: " + slot.mode.getTranslatedName().getString()),
                btn -> {
                    do {
                        slot.mode = SlotMode.values()[(slot.mode.ordinal() + 1) % SlotMode.values().length];
                    } while ((!isRoot && slot.mode == SlotMode.SUBMENU) ||
                            (slot.mode == SlotMode.MALILIB && !FabricLoader.getInstance().isModLoaded("malilib")));

                    btn.setMessage(Component.nullToEmpty("Type: " + slot.mode.getTranslatedName().getString()));
                    refreshWidgets();
                }
        ).bounds(left, row2Y, contentWidth, ROW_HEIGHT).build();
        addRenderableWidget(modeButton);

        // ROW 3: Value / Slider
        int valueFieldWidthWithBtn = contentWidth - BROWSE_BTN_WIDTH - HORIZ_GAP;

        valueField = new EditBox(
                font, left, row3Y, valueFieldWidthWithBtn, ROW_HEIGHT,
                Component.translatable("screen.radial.editor.value")
        );
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value);
        valueField.setResponder(v -> slot.value = v);
        addRenderableWidget(valueField);

        valueBrowseButton = Button.builder(
                Component.translatable("screen.radial.editor.select"),
                _ -> {
                    if (slot.mode == SlotMode.MALILIB) {
                        minecraft.setScreen(new MalilibSelectionScreen(this, action -> {
                            valueField.setValue(action.id());
                            slot.value = action.id();
                        }));
                    } else {
                        minecraft.setScreen(new KeybindPickerScreen(this, id -> {
                            valueField.setValue(id);
                            slot.value = id;
                        }));
                    }
                }
        ).bounds(left + valueFieldWidthWithBtn + HORIZ_GAP, row3Y, BROWSE_BTN_WIDTH, ROW_HEIGHT).build();
        addRenderableWidget(valueBrowseButton);

        subCountSlider = new AbstractSliderButton(
                left, row3Y, contentWidth, ROW_HEIGHT,
                getSliderText(slot.childSlotCount),
                (slot.childSlotCount - 3) / 9.0
        ) {
            @Override
            protected void updateMessage() {
                int val = 3 + (int) (value * 9);
                setMessage(getSliderText(val));
            }

            @Override
            protected void applyValue() {
                slot.childSlotCount = 3 + (int) (value * 9);
                if (slot.mode == SlotMode.SUBMENU) {
                    ensureChildren();
                }
            }
        };
        addRenderableWidget(subCountSlider);

        // ROW 4: Icon
        int iconFieldWidth = contentWidth - (ICON_BTN_WIDTH * 2) - (HORIZ_GAP * 2);

        iconField = new EditBox(
                font, left, row4Y, iconFieldWidth, ROW_HEIGHT,
                Component.translatable("screen.radial.editor.icon")
        );
        iconField.setMaxLength(Integer.MAX_VALUE);
        iconField.setValue(slot.itemId);
        iconField.setResponder(v -> {
            slot.itemId = v;
            slot.clearCache();
        });
        addRenderableWidget(iconField);

        int browseIconX = left + iconFieldWidth + HORIZ_GAP;
        browseIconButton = Button.builder(
                Component.translatable("screen.radial.editor.browse"),
                _ -> minecraft.setScreen(new IconPickerScreen(this, id -> {
                    iconField.setValue(id);
                    slot.itemId = id;
                    slot.clearCache();
                }))
        ).bounds(browseIconX, row4Y, ICON_BTN_WIDTH, ROW_HEIGHT).build();
        addRenderableWidget(browseIconButton);

        int handIconX = browseIconX + ICON_BTN_WIDTH + HORIZ_GAP;
        handButton = Button.builder(
                Component.translatable("screen.radial.editor.hand"),
                _ -> {
                    if (minecraft.player != null) {
                        ItemStack stack = minecraft.player.getMainHandItem();
                        String id = !stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() : "minecraft:air";
                        iconField.setValue(id);
                        slot.itemId = id;
                        slot.clearCache();
                    }
                }
        ).bounds(handIconX, row4Y, ICON_BTN_WIDTH, ROW_HEIGHT).build();
        addRenderableWidget(handButton);

        // ROW 5: Actions
        int actionBtnWidth = (contentWidth - HORIZ_GAP) / 2;

        saveButton = Button.builder(
                Component.translatable("screen.radial.editor.save"),
                _ -> {
                    this.isSaved = true;
                    RadialConfig.save();
                    onClose();
                }
        ).bounds(left, row5Y, actionBtnWidth, ROW_HEIGHT).build();
        addRenderableWidget(saveButton);

        cancelButton = Button.builder(
                Component.translatable("screen.radial.editor.cancel"),
                _ -> onClose()
        ).bounds(left + actionBtnWidth + HORIZ_GAP, row5Y, actionBtnWidth, ROW_HEIGHT).build();
        addRenderableWidget(cancelButton);

        refreshWidgets();
    }

    private Component getSliderText(int value) {
        return Component.translatable("screen.radial.editor.sub_size", value);
    }

    private void ensureChildren() {
        if (slot.children == null) slot.children = new ArrayList<>();

        while (slot.children.size() < slot.childSlotCount) {
            slot.children.add(
                    new RadialSlot(
                            "Sub Slot " + (slot.children.size() + 1),
                            SlotMode.EMPTY,
                            "",
                            "minecraft:stone"
                    )
            );
        }
    }

    private void refreshWidgets() {
        boolean isSub = slot.mode == SlotMode.SUBMENU;
        boolean isEmpty = slot.mode == SlotMode.EMPTY;
        boolean isKey = slot.mode == SlotMode.KEYBIND;
        boolean isMalilib = slot.mode == SlotMode.MALILIB;

        if (isSub) {
            ensureChildren();
        }

        valueField.setVisible(!isEmpty && !isSub);
        valueBrowseButton.visible = isKey || isMalilib;

        // Dynamically adjust value field width if the browse button is visible
        int contentWidth = Math.min(300, (int) (width * 0.9));
        int valueFieldWidthWithBtn = contentWidth - BROWSE_BTN_WIDTH - HORIZ_GAP;
        valueField.setWidth(valueBrowseButton.visible ? valueFieldWidthWithBtn : contentWidth);

        subCountSlider.visible = isSub;

        iconField.setVisible(!isEmpty);
        browseIconButton.visible = !isEmpty;
        handButton.visible = !isEmpty;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int centerX = width / 2;
        int contentWidth = Math.min(300, (int) (width * 0.9));
        int left = centerX - (contentWidth / 2);

        int row1Y = height / 2 - 60;
        int row3Y = row1Y + VERT_GAP * 2;
        int row4Y = row1Y + VERT_GAP * 3;

        // Draw background slot
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SLOT_TEXTURE,
                centerX - 13,
                height / 2 - 110,
                SLOT_SIZE,
                SLOT_SIZE
        );

        SlotRenderHelper.renderSlotIcon(graphics, slot, centerX - 13, height / 2 - 110);

        // Labels
        graphics.text(font, Component.translatable("screen.radial.editor.name"), left, row1Y - 12, 0xFFAAAAAA);

        if (slot.mode == SlotMode.SUBMENU) {
            graphics.text(font, Component.translatable("screen.radial.editor.submenu"), left, row3Y - 12, 0xFFAAAAAA);
        } else if (slot.mode != SlotMode.EMPTY) {
            graphics.text(font, Component.translatable("screen.radial.editor.value"), left, row3Y - 12, 0xFFAAAAAA);
            graphics.text(font, Component.translatable("screen.radial.editor.icon"), left, row4Y - 12, 0xFFAAAAAA);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (!this.isSaved) {
            slot.name = oldName;
            slot.value = oldValue;
            slot.itemId = oldId;
            slot.mode = oldMode;
            slot.childSlotCount = oldChildCount;
            slot.clearCache();
        }

        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}