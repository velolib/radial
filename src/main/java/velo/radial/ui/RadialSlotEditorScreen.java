package velo.radial.ui;

import velo.radial.config.RadialConfig;
import velo.radial.config.RadialSlot;
import velo.radial.config.SlotMode;

import java.util.ArrayList;
import java.util.Objects;
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

public class RadialSlotEditorScreen extends Screen {

    private static final Identifier SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final int SLOT_SIZE = 26;
    private static final int GAP = 38;

    private final RadialSlot slot;
    private final boolean isRoot;

    // Used to restore state when cancelling
    private final String oldName, oldValue, oldId;
    private final SlotMode oldMode;
    private final int oldChildCount;

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
        int baseY = height / 2 - 60;

        int contentWidth = Math.min(300, (int) (width * 0.9));
        int left = centerX - (contentWidth / 2);

        nameField = new EditBox(
                font, left, baseY, contentWidth, 20,
                Component.translatable("screen.radial.editor.name")
        );
        nameField.setMaxLength(Integer.MAX_VALUE);
        nameField.setValue(slot.name);
        nameField.setResponder(v -> slot.name = v);
        addRenderableWidget(nameField);

        modeButton = Button.builder(
                Component.nullToEmpty("Type: " + slot.mode.getTranslatedName().getString()),
                btn -> {
                    slot.mode = SlotMode.values()[
                            (slot.mode.ordinal() + 1) % SlotMode.values().length
                            ];

                    if (!isRoot && slot.mode == SlotMode.SUBMENU) {
                        slot.mode = SlotMode.values()[
                                (slot.mode.ordinal() + 1) % SlotMode.values().length
                                ];
                    }

                    btn.setMessage(Component.nullToEmpty("Type: " + slot.mode.getTranslatedName().getString()));
                    refreshWidgets();
                }
        ).bounds(left, baseY + GAP, contentWidth, 20).build();
        addRenderableWidget(modeButton);

        valueField = new EditBox(
                font, left, baseY + GAP * 2, contentWidth - 30, 20,
                Component.translatable("screen.radial.editor.value")
        );
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setValue(slot.value);
        valueField.setResponder(v -> slot.value = v);
        addRenderableWidget(valueField);

        valueBrowseButton = Button.builder(
                Component.literal("..."),
                btn -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new KeybindPickerScreen(this, id -> {
                            valueField.setValue(id);
                            slot.value = id;
                        }));
                    }
                }
        ).bounds(left + contentWidth - 25, baseY + GAP * 2, 25, 20).build();
        addRenderableWidget(valueBrowseButton);

        subCountSlider = new AbstractSliderButton(
                left,
                baseY + GAP * 2,
                contentWidth,
                20,
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

        int buttonWidth = 50;
        int iconFieldWidth = contentWidth - (buttonWidth * 2) - 10;

        iconField = new EditBox(
                font, left, baseY + GAP * 3, iconFieldWidth, 20,
                Component.translatable("screen.radial.editor.icon")
        );
        iconField.setMaxLength(Integer.MAX_VALUE);
        iconField.setValue(slot.itemId);
        iconField.setResponder(v -> {
            slot.itemId = v;
            slot.clearCache();
        });
        addRenderableWidget(iconField);

        browseIconButton = Button.builder(
                Component.translatable("screen.radial.editor.browse"),
                btn -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new ItemPickerScreen(this, id -> {
                            iconField.setValue(id);
                            slot.itemId = id;
                            slot.clearCache();
                        }));
                    }
                }
        ).bounds(left + iconFieldWidth + 5, baseY + GAP * 3, buttonWidth, 20).build();
        addRenderableWidget(browseIconButton);

        handButton = Button.builder(
                Component.translatable("screen.radial.editor.hand"),
                btn -> {
                    if (minecraft != null && minecraft.player != null) {
                        ItemStack stack = minecraft.player.getMainHandItem();
                        String id;
                        if (!stack.isEmpty()) {
                             id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        } else {
                            id = "minecraft:air";
                        }
                        iconField.setValue(id);
                        slot.itemId = id;
                        slot.clearCache();
                    }
                }
        ).bounds(left + iconFieldWidth + buttonWidth + 10, baseY + GAP * 3, buttonWidth, 20).build();
        addRenderableWidget(handButton);

        int actionY = baseY + GAP * 4 + 10;
        int halfWidth = (contentWidth - 10) / 2;

        saveButton = Button.builder(
                Component.translatable("screen.radial.editor.save"),
                btn -> {
                    RadialConfig.save();
                    if (minecraft != null) minecraft.setScreen(null);
                }
        ).bounds(left, actionY, halfWidth, 20).build();
        addRenderableWidget(saveButton);

        cancelButton = Button.builder(
                Component.translatable("screen.radial.editor.cancel"),
                btn -> onClose()
        ).bounds(left + halfWidth + 10, actionY, halfWidth, 20).build();
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

        if (isSub) {
            ensureChildren();
        }

        valueField.setVisible(!isEmpty && !isSub);
        valueBrowseButton.visible = isKey;

        subCountSlider.visible = isSub;

        iconField.setVisible(!isEmpty);
        browseIconButton.visible = !isEmpty;
        handButton.visible = !isEmpty;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int centerX = width / 2;
        int contentWidth = Math.min(300, (int) (width * 0.9));
        int left = centerX - (contentWidth / 2);
        int baseY = height / 2 - 60;

        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SLOT_TEXTURE,
                centerX - 13,
                height / 2 - 110,
                SLOT_SIZE,
                SLOT_SIZE
        );
        context.item(slot.getRenderStack(), centerX - 8, height / 2 - 105);

        context.text(
                font,
                Component.translatable("screen.radial.editor.name"),
                left,
                baseY - 12,
                0xFFAAAAAA
        );

        if (slot.mode == SlotMode.SUBMENU) {
            context.text(
                    font,
                    Component.translatable("screen.radial.editor.submenu"),
                    left,
                    baseY + GAP * 2 - 12,
                    0xFFAAAAAA
            );
        } else if (slot.mode != SlotMode.EMPTY) {
            context.text(
                    font,
                    Component.translatable("screen.radial.editor.value"),
                    left,
                    baseY + GAP * 2 - 12,
                    0xFFAAAAAA
            );
        }

        if (slot.mode != SlotMode.EMPTY) {
            context.text(
                    font,
                    Component.translatable("screen.radial.editor.icon"),
                    left,
                    baseY + GAP * 3 - 12,
                    0xFFAAAAAA
            );
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (saveButton != null && !saveButton.isFocused()) {
            slot.name = oldName;
            slot.value = oldValue;
            slot.itemId = oldId;
            slot.mode = oldMode;
            slot.childSlotCount = oldChildCount;
            slot.clearCache();
        }

        Objects.requireNonNull(minecraft).setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
