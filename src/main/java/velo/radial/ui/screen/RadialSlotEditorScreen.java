package velo.radial.ui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import velo.radial.api.RadialSlot;
import velo.radial.api.RadialSlotModeRegistry;
import velo.radial.api.SlotMode;
import velo.radial.config.RadialConfig;
import velo.radial.render.SlotRenderHelper;
import velo.radial.ui.widget.DropdownButtonWidget;
import velo.radial.ui.widget.DropdownMenuWidget;

import java.util.List;

public class RadialSlotEditorScreen extends Screen {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final int SLOT_SIZE = 26;

    // LAYOUT CONSTANTS
    private static final int ROW_HEIGHT = 20;
    private static final int HORIZ_GAP = 5;

    private final RadialSlot slot;
    private final boolean isRoot;

    // State for reverting changes on cancel
    private final String oldName, oldValue, oldId;
    private final SlotMode oldMode;
    private final int oldChildCount;

    private boolean isSaved = false;

    // Universal Widgets
    private EditBox nameField;
    private DropdownButtonWidget<SlotMode> modeDropdown;

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
        int contentWidth = Math.min(300, (int) (width * 0.9));

        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);

        // --- ROW 1: Name ---
        LinearLayout nameGroup = LinearLayout.vertical().spacing(2);
        StringWidget nameLabel = new StringWidget(Component.translatable("screen.radial.editor.name"), font);
        nameGroup.addChild(nameLabel);

        nameField = new EditBox(
                font, 0, 0, contentWidth, ROW_HEIGHT,
                Component.translatable("screen.radial.editor.name")
        );
        nameField.setMaxLength(Integer.MAX_VALUE);
        nameField.setValue(slot.name);
        nameField.setResponder(v -> slot.name = v);
        nameGroup.addChild(nameField);

        mainLayout.addChild(nameGroup);

        // --- ROW 2: Mode ---
        LinearLayout modeGroup = LinearLayout.vertical().spacing(2);
        StringWidget modeLabel = new StringWidget(Component.translatable("screen.radial.editor.mode"), font);
        modeGroup.addChild(modeLabel);

        List<SlotMode> availableModes = RadialSlotModeRegistry.getRegisteredModes().values().stream()
                .filter(mode -> mode.isAvailable() && (isRoot || !mode.getTranslatedName().getString().toLowerCase().contains("submenu")))
                .toList();

        modeDropdown = new DropdownButtonWidget<>(
                0, 0, contentWidth, ROW_HEIGHT,
                availableModes, slot.mode, SlotMode::getTranslatedName,
                selectedMode -> {
                    slot.mode = selectedMode;
                    selectedMode.onInitialize(slot);
                    this.rebuildWidgets();
                },
                this::addRenderableWidget
        ) {
            @Override
            public void closeMenu() {
                if (this.isMenuOpen()) {
                    RadialSlotEditorScreen.this.removeWidget(this.getActiveMenu());
                }
                super.closeMenu();
            }
        };
        modeGroup.addChild(modeDropdown);
        mainLayout.addChild(modeGroup);

        // --- ROW 3: Dynamic Container ---
        // Also reduced to 8 to match main vertical gaps
        LinearLayout dynamicLayoutContainer = LinearLayout.vertical().spacing(8);
        slot.mode.buildEditorWidgets(this, slot, contentWidth, dynamicLayoutContainer);
        mainLayout.addChild(dynamicLayoutContainer);

        // --- ROW 4: Action Buttons ---
        LinearLayout actionGroup = LinearLayout.horizontal().spacing(HORIZ_GAP);
        int actionBtnWidth = (contentWidth - HORIZ_GAP) / 2;

        Button saveButton = Button.builder(
                Component.translatable("screen.radial.editor.save"),
                _ -> {
                    this.isSaved = true;
                    RadialConfig.save();
                    onClose();
                }
        ).bounds(0, 0, actionBtnWidth, ROW_HEIGHT).build();
        actionGroup.addChild(saveButton);

        Button cancelButton = Button.builder(
                Component.translatable("screen.radial.editor.cancel"),
                _ -> onClose()
        ).bounds(0, 0, actionBtnWidth, ROW_HEIGHT).build();
        actionGroup.addChild(cancelButton);

        mainLayout.addChild(actionGroup);

        // --- FINAL ASSEMBLY ---
        FrameLayout rootLayout = new FrameLayout();
        rootLayout.addChild(mainLayout);
        rootLayout.arrangeElements();

        // Offset the Y position down by 20 to ensure room at the top of the screen for the icon
        FrameLayout.centerInRectangle(rootLayout, 0, 20, width, height);
        rootLayout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int centerX = width / 2;

        // DYNAMIC ICON POSITIONING:
        // By calculating the icon's Y coordinate based on the `nameField` widget's actual Y coordinate,
        // the icon will perfectly hover 20 pixels above the form regardless of screen size!
        int iconY = (nameField != null) ? nameField.getY() - SLOT_SIZE - 20 : height / 2 - 110;

        // Draw background slot
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SLOT_TEXTURE,
                centerX - 13,
                iconY,
                SLOT_SIZE,
                SLOT_SIZE
        );

        SlotRenderHelper.renderSlotIcon(graphics, slot, centerX - 13, iconY);

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
        }

        minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (this.modeDropdown != null && this.modeDropdown.isMenuOpen()) {
            DropdownMenuWidget<SlotMode> floatingMenu = this.modeDropdown.getActiveMenu();

            if (floatingMenu.isMouseOver(click.x(), click.y())) {
                floatingMenu.mouseClicked(click, doubled);
                return true;
            } else //noinspection StatementWithEmptyBody
                if (this.modeDropdown.isMouseOver(click.x(), click.y())) {
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