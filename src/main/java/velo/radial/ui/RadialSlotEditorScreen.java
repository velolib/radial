package velo.radial.ui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import velo.radial.config.RadialConfig;
import velo.radial.config.RadialSlot;
import velo.radial.config.SlotMode;

import java.util.ArrayList;
import java.util.Objects;

public class RadialSlotEditorScreen extends Screen {

    private static final Identifier SLOT_TEXTURE =
            Identifier.of("minecraft", "gamemode_switcher/slot");
    private static final int SLOT_SIZE = 26;
    private static final int GAP = 38;

    private final RadialSlot slot;
    private final boolean isRoot;

    // Used to restore state when cancelling
    private final String oldName, oldValue, oldId;
    private final SlotMode oldMode;
    private final int oldChildCount;

    private TextFieldWidget nameField;
    private TextFieldWidget valueField;
    private TextFieldWidget iconField;

    private SliderWidget subCountSlider;

    private ButtonWidget modeButton;
    private ButtonWidget browseIconButton;
    private ButtonWidget handButton;
    private ButtonWidget valueBrowseButton;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;

    public RadialSlotEditorScreen(RadialSlot slot, boolean isRoot) {
        super(Text.translatable("screen.radial.editor.title"));
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

        nameField = new TextFieldWidget(
                textRenderer, left, baseY, contentWidth, 20,
                Text.translatable("screen.radial.editor.name")
        );
        nameField.setMaxLength(Integer.MAX_VALUE);
        nameField.setText(slot.name);
        nameField.setChangedListener(v -> slot.name = v);
        addDrawableChild(nameField);

        modeButton = ButtonWidget.builder(
                Text.of("Type: " + slot.mode.getTranslatedName().getString()),
                btn -> {
                    slot.mode = SlotMode.values()[
                            (slot.mode.ordinal() + 1) % SlotMode.values().length
                            ];

                    if (!isRoot && slot.mode == SlotMode.SUBMENU) {
                        slot.mode = SlotMode.values()[
                                (slot.mode.ordinal() + 1) % SlotMode.values().length
                                ];
                    }

                    btn.setMessage(Text.of("Type: " + slot.mode.getTranslatedName().getString()));
                    refreshWidgets();
                }
        ).dimensions(left, baseY + GAP, contentWidth, 20).build();
        addDrawableChild(modeButton);

        valueField = new TextFieldWidget(
                textRenderer, left, baseY + GAP * 2, contentWidth - 30, 20,
                Text.translatable("screen.radial.editor.value")
        );
        valueField.setMaxLength(Integer.MAX_VALUE);
        valueField.setText(slot.value);
        valueField.setChangedListener(v -> slot.value = v);
        addDrawableChild(valueField);

        valueBrowseButton = ButtonWidget.builder(
                Text.literal("..."),
                btn -> {
                    if (client != null) {
                        client.setScreen(new KeybindPickerScreen(this, id -> {
                            valueField.setText(id);
                            slot.value = id;
                        }));
                    }
                }
        ).dimensions(left + contentWidth - 25, baseY + GAP * 2, 25, 20).build();
        addDrawableChild(valueBrowseButton);

        subCountSlider = new SliderWidget(
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
        addDrawableChild(subCountSlider);

        int buttonWidth = 50;
        int iconFieldWidth = contentWidth - (buttonWidth * 2) - 10;

        iconField = new TextFieldWidget(
                textRenderer, left, baseY + GAP * 3, iconFieldWidth, 20,
                Text.translatable("screen.radial.editor.icon")
        );
        iconField.setMaxLength(Integer.MAX_VALUE);
        iconField.setText(slot.itemId);
        iconField.setChangedListener(v -> {
            slot.itemId = v;
            slot.clearCache();
        });
        addDrawableChild(iconField);

        browseIconButton = ButtonWidget.builder(
                Text.translatable("screen.radial.editor.browse"),
                btn -> {
                    if (client != null) {
                        client.setScreen(new ItemPickerScreen(this, id -> {
                            iconField.setText(id);
                            slot.itemId = id;
                            slot.clearCache();
                        }));
                    }
                }
        ).dimensions(left + iconFieldWidth + 5, baseY + GAP * 3, buttonWidth, 20).build();
        addDrawableChild(browseIconButton);

        handButton = ButtonWidget.builder(
                Text.translatable("screen.radial.editor.hand"),
                btn -> {
                    if (client != null && client.player != null) {
                        ItemStack stack = client.player.getMainHandStack();
                        String id;
                        if (!stack.isEmpty()) {
                             id = Registries.ITEM.getId(stack.getItem()).toString();
                        } else {
                            id = "minecraft:air";
                        }
                        iconField.setText(id);
                        slot.itemId = id;
                        slot.clearCache();
                    }
                }
        ).dimensions(left + iconFieldWidth + buttonWidth + 10, baseY + GAP * 3, buttonWidth, 20).build();
        addDrawableChild(handButton);

        int actionY = baseY + GAP * 4 + 10;
        int halfWidth = (contentWidth - 10) / 2;

        saveButton = ButtonWidget.builder(
                Text.translatable("screen.radial.editor.save"),
                btn -> {
                    RadialConfig.save();
                    if (client != null) client.setScreen(null);
                }
        ).dimensions(left, actionY, halfWidth, 20).build();
        addDrawableChild(saveButton);

        cancelButton = ButtonWidget.builder(
                Text.translatable("screen.radial.editor.cancel"),
                btn -> close()
        ).dimensions(left + halfWidth + 10, actionY, halfWidth, 20).build();
        addDrawableChild(cancelButton);

        refreshWidgets();
    }

    private Text getSliderText(int value) {
        return Text.translatable("screen.radial.editor.sub_size", value);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int centerX = width / 2;
        int contentWidth = Math.min(300, (int) (width * 0.9));
        int left = centerX - (contentWidth / 2);
        int baseY = height / 2 - 60;

        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SLOT_TEXTURE,
                centerX - 13,
                height / 2 - 110,
                SLOT_SIZE,
                SLOT_SIZE
        );
        context.drawItem(slot.getRenderStack(), centerX - 8, height / 2 - 105);

        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.radial.editor.name"),
                left,
                baseY - 12,
                0xFFAAAAAA
        );

        if (slot.mode == SlotMode.SUBMENU) {
            context.drawTextWithShadow(
                    textRenderer,
                    Text.translatable("screen.radial.editor.submenu"),
                    left,
                    baseY + GAP * 2 - 12,
                    0xFFAAAAAA
            );
        } else if (slot.mode != SlotMode.EMPTY) {
            context.drawTextWithShadow(
                    textRenderer,
                    Text.translatable("screen.radial.editor.value"),
                    left,
                    baseY + GAP * 2 - 12,
                    0xFFAAAAAA
            );
        }

        if (slot.mode != SlotMode.EMPTY) {
            context.drawTextWithShadow(
                    textRenderer,
                    Text.translatable("screen.radial.editor.icon"),
                    left,
                    baseY + GAP * 3 - 12,
                    0xFFAAAAAA
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (saveButton != null && !saveButton.isFocused()) {
            slot.name = oldName;
            slot.value = oldValue;
            slot.itemId = oldId;
            slot.mode = oldMode;
            slot.childSlotCount = oldChildCount;
            slot.clearCache();
        }

        Objects.requireNonNull(client).setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
