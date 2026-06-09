package velo.radial.ui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import velo.radial.api.RadialSlot;
import velo.radial.api.RadialSlotModes;
import velo.radial.api.SlotMode;
import velo.radial.config.RadialConfig;
import velo.radial.render.SlotRenderHelper;
import velo.radial.ui.widget.DropdownButtonWidget;
import velo.radial.ui.widget.DropdownMenuWidget;

import java.util.ArrayList;
import java.util.List;

public class RadialSlotEditorScreen extends Screen {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final int SLOT_SIZE = 26;

    // LAYOUT CONSTANTS
    private static final int ROW_HEIGHT = 20;
    private static final int VERT_GAP = 38;
    private static final int HORIZ_GAP = 5;

    private final RadialSlot slot;
    private final boolean isRoot;

    // State for reverting changes on cancel
    private final String oldName, oldValue, oldId;
    private final SlotMode oldMode;
    private final int oldChildCount;
    // Tracking dynamically injected widgets so we can clear them when the mode changes
    private final List<AbstractWidget> dynamicWidgets = new ArrayList<>();
    private boolean isSaved = false;
    // Universal Widgets
    private EditBox nameField;
    private DropdownButtonWidget<SlotMode> modeDropdown;
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

        int row1Y = height / 2 - 60;
        int row2Y = row1Y + VERT_GAP;

        // ROW 1: Name Field (Universal)
        nameField = new EditBox(
                font, left, row1Y, contentWidth, ROW_HEIGHT,
                Component.translatable("screen.radial.editor.name")
        );
        nameField.setMaxLength(Integer.MAX_VALUE);
        nameField.setValue(slot.name);
        nameField.setResponder(v -> slot.name = v);
        addRenderableWidget(nameField);

        // ROW 2: Mode Dropdown (Driven by the Registry)
        List<SlotMode> availableModes = RadialSlotModes.getRegisteredModes().values().stream()
                .filter(mode -> mode.isAvailable() && (isRoot || !mode.getTranslatedName().getString().toLowerCase().contains("submenu")))
                .toList();

        modeDropdown = new DropdownButtonWidget<>(
                left, row2Y, contentWidth, ROW_HEIGHT,
                availableModes, slot.mode, SlotMode::getTranslatedName,
                selectedMode -> {
                    slot.mode = selectedMode;
                    selectedMode.onInitialize(slot);
                    buildDynamicLayout(); // Re-trigger the layout generation
                },
                menuWidget -> this.addRenderableWidget(menuWidget)
        ) {
            @Override
            public void closeMenu() {
                if (this.isMenuOpen()) {
                    RadialSlotEditorScreen.this.removeWidget(this.getActiveMenu());
                }
                super.closeMenu();
            }
        };
        addRenderableWidget(modeDropdown);

        // Build the rest of the layout natively through the slot mode
        buildDynamicLayout();
    }

    /**
     * Clears old dynamic widgets and asks the current SlotMode to build its own layout.
     * Repositions the Save/Cancel buttons safely underneath them.
     */
    private void buildDynamicLayout() {
        int centerX = width / 2;
        int contentWidth = Math.min(300, (int) (width * 0.9));
        int left = centerX - (contentWidth / 2);

        // Clear existing dynamic widgets
        for (AbstractWidget widget : dynamicWidgets) {
            this.removeWidget(widget);
        }
        dynamicWidgets.clear();

        // Calculate starting point (Below the dropdown)
        int dynamicStartY = (height / 2 - 60) + (VERT_GAP * 2);

        // Invert control: Have the mode inject its widgets and tell us how much space it used
        int consumedHeight = slot.mode.buildEditorWidgets(this, slot, left, dynamicStartY, contentWidth, widget -> {
            this.dynamicWidgets.add(widget);
            this.addRenderableWidget(widget);
        });

        // Push action buttons cleanly underneath
        int actionY = dynamicStartY + consumedHeight + 10;
        int actionBtnWidth = (contentWidth - HORIZ_GAP) / 2;

        if (saveButton != null) this.removeWidget(saveButton);
        if (cancelButton != null) this.removeWidget(cancelButton);

        saveButton = Button.builder(
                Component.translatable("screen.radial.editor.save"),
                _ -> {
                    this.isSaved = true;
                    RadialConfig.save();
                    onClose();
                }
        ).bounds(left, actionY, actionBtnWidth, ROW_HEIGHT).build();
        addRenderableWidget(saveButton);

        cancelButton = Button.builder(
                Component.translatable("screen.radial.editor.cancel"),
                _ -> onClose()
        ).bounds(left + actionBtnWidth + HORIZ_GAP, actionY, actionBtnWidth, ROW_HEIGHT).build();
        addRenderableWidget(cancelButton);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int centerX = width / 2;
        int contentWidth = Math.min(300, (int) (width * 0.9));
        int left = centerX - (contentWidth / 2);

        int row1Y = height / 2 - 60;
        int row2Y = row1Y + VERT_GAP;

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

        // --- LABELS ---
        // We only render the universal labels here. The SlotModes render their own StringWidgets.
        graphics.text(font, Component.translatable("screen.radial.editor.name"), left, row1Y - 12, 0xFFAAAAAA);
        graphics.text(font, Component.translatable("screen.radial.editor.mode"), left, row2Y - 12, 0xFFAAAAAA);

        // --- THE MOUSE SPOOFING TRICK ---
        boolean hoveringMenu = this.modeDropdown != null
                && this.modeDropdown.isMenuOpen()
                && this.modeDropdown.getActiveMenu().isMouseOver(mouseX, mouseY);

        int passMouseX = hoveringMenu ? -999 : mouseX;
        int passMouseY = hoveringMenu ? -999 : mouseY;

        super.extractRenderState(graphics, passMouseX, passMouseY, delta);

        if (hoveringMenu) {
            this.modeDropdown.getActiveMenu().extractRenderState(graphics, mouseX, mouseY, delta);
        }
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
            // Optional: Revert custom dynamic properties map here if implemented
        }

        minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (this.modeDropdown != null && this.modeDropdown.isMenuOpen()) {
            DropdownMenuWidget<SlotMode> floatingMenu = this.modeDropdown.getActiveMenu();

            if (floatingMenu.isMouseOver(click.x(), click.y())) {
                floatingMenu.mouseClicked(click, doubled);
                return true;
            } else if (this.modeDropdown.isMouseOver(click.x(), click.y())) {
                // Let the click fall through so the button can close itself
            } else {
                this.modeDropdown.closeMenu();
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}