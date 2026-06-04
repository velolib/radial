package velo.radial.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IconPickerScreen extends Screen {

    private enum TabType {
        ITEMS("screen.radial.editor.icon_picker.items"),
        INVENTORY("screen.radial.editor.icon_picker.inventory"),
        EFFECTS("screen.radial.editor.icon_picker.effects");

        final String translationKey;
        TabType(String key) { this.translationKey = key; }
    }

    private static final int SLOT_SIZE = 16;
    private static final List<TabType> TABS = List.of(TabType.ITEMS, TabType.INVENTORY, TabType.EFFECTS);

    private List<MobEffect> filteredEffects = new ArrayList<>();

    // Inventory UI Constants
    private static final Identifier INVENTORY_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    private static final int INV_WIDTH = 176;
    private static final int INV_HEIGHT = 166;

    private final Screen parent;
    private final Consumer<String> onSelect;

    private final List<Button> tabButtons = new ArrayList<>();
    private TabType currentTab;

    private EditBox searchField;
    private List<Item> filteredItems = new ArrayList<>();

    private int scrollOffset = 0;
    private int columns = 12;
    private int maxRows = 7;

    public IconPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.literal("Icon Selector"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.currentTab = TABS.getFirst();
    }

    private void setTab(TabType tabName) {
        this.currentTab = tabName;

        if (this.searchField != null) {
            this.searchField.setValue("");
            // Hide the search bar if we are in the Inventory tab
            this.searchField.visible = TabType.ITEMS == currentTab;
        }

        this.updateSearch("");
        this.scrollOffset = 0;
        this.updateTabButtonStates();
    }

    private void updateSearch(String query) {
        // TODO: Debounce or something, idk

        String q = query.toLowerCase();

        if (TabType.ITEMS == currentTab) {
            filteredItems = BuiltInRegistries.ITEM.stream()
                    .filter(item -> item.getDefaultInstance().getItemName().getString().toLowerCase().contains(q)
                            || BuiltInRegistries.ITEM.getKey(item).toString().contains(q))
                    .toList();
        } else if (TabType.EFFECTS == currentTab) {
            filteredEffects = BuiltInRegistries.MOB_EFFECT.stream()
                    .filter(effect -> Component.translatable(effect.getDescriptionId()).getString().toLowerCase().contains(q)
                            || Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect)).toString().contains(q))
                    .toList();
        }

        scrollOffset = 0;
    }

    // --- Layout Helpers ---
    private int getGridTop() {
        return 65;
    }

    private int getListWidth() {
        return Math.min(350, (int) (width * 0.9));
    }

    @Override
    protected void init() {
        this.tabButtons.clear();

        // Render Tabs
        int tabWidth = Math.min(80, width / Math.max(1, TABS.size()));
        int xOffset = (width - (tabWidth * TABS.size())) / 2;

        for (TabType tab : TABS) {
            Button btn = Button.builder(
                    Component.translatable(tab.translationKey),
                    _ -> {
                        setTab(tab);
                        updateTabButtonStates();
                    }
            ).bounds(xOffset, 10, tabWidth, 20).build();

            btn.active = (tab != currentTab);

            this.tabButtons.add(btn);
            addRenderableWidget(btn);
            xOffset += tabWidth;
        }

        // Render Search Field
        int searchWidth = getListWidth();
        this.searchField = new EditBox(
                font,
                width / 2 - searchWidth / 2,
                35,
                searchWidth,
                20,
                Component.translatable("screen.radial.editor.search")
        );

        this.searchField.setHint(Component.translatable("screen.radial.editor.search"));
        this.searchField.setResponder(this::updateSearch);
        this.searchField.visible = TabType.ITEMS == currentTab;
        this.addRenderableWidget(searchField);
        this.setInitialFocus(searchField);

        // Render Cancel Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                _ -> onClose()
        ).bounds(width / 2 - 100, height - 28, 200, 20).build());

        // Calculate Grid Bounds
        columns = Math.max(1, searchWidth / SLOT_SIZE);
        maxRows = Math.max(1, (height - getGridTop() - 40) / SLOT_SIZE);

        // Initial populate
        updateSearch(searchField.getValue());

        updateTabButtonStates();
    }

    private void updateTabButtonStates() {
        for (int i = 0; i < tabButtons.size(); i++) {
            TabType tab = TABS.get(i);
            tabButtons.get(i).active = (tab != currentTab);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        if (TabType.ITEMS == currentTab) {
            renderItemsTab(graphics, mouseX, mouseY);
        } else if (TabType.INVENTORY == currentTab) {
            renderInventoryTab(graphics, mouseX, mouseY);
        } else if (TabType.EFFECTS == currentTab) {
            renderEffectsTab(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderItemsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int startX = width / 2 - (columns * SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredItems.size()) break;

            int x = startX + (i % columns) * SLOT_SIZE;
            int y = getGridTop() + (i / columns) * SLOT_SIZE;

            ItemStack stack = new ItemStack(filteredItems.get(index));
            graphics.fakeItem(stack, x, y);

            if (isHovered(mouseX, mouseY, x, y, SLOT_SIZE)) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40FFFFFF);
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
    }

    private void renderInventoryTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) return;
        Inventory inv = minecraft.player.getInventory();

        int bgX = width / 2 - INV_WIDTH / 2;
        int bgY = height / 2 - INV_HEIGHT / 2 + 10;

        Component infoText = Component.translatable("screen.radial.editor.icon_picker.inventory.info");
        int textWidth = font.width(infoText);
        graphics.text(font, infoText, width / 2 - textWidth / 2, bgY - 15, 0xFFAAAAAA);

        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, bgX, bgY, 0, 0, INV_WIDTH, INV_HEIGHT, 256, 256, 0xFFFFFFFF);
        // 1. Hotbar (Indices 0 - 8)
        for (int i = 0; i < 9; i++) {
            int x = bgX + 7 + i * 18;
            int y = bgY + 141;
            drawInvSlot(graphics, mouseX, mouseY, x, y, inv.getNonEquipmentItems().get(i));
        }

        // 2. Main Inventory (Indices 9 - 35, rendered as 0 - 26)
        for (int i = 0; i < 27; i++) {
            int x = bgX + 7 + (i % 9) * 18;
            int y = bgY + 83 + (i / 9) * 18;
            drawInvSlot(graphics, mouseX, mouseY, x, y, inv.getNonEquipmentItems().get(i + 9));
        }

        // 3. Armor (Indices 0 - 3, rendered top-down: Head -> Boots)
        EquipmentSlot[] armorSlots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

        for (int i = 0; i < 4; i++) {
            int x = bgX + 7;
            int y = bgY + 7 + i * 18;

            // Fetch the armor piece directly from the player entity's equipment
            ItemStack armorStack = minecraft.player.getItemBySlot(armorSlots[i]);
            // Note: If you are on Fabric Yarn mappings, use .getEquippedStack(armorSlots[i]) instead

            drawInvSlot(graphics, mouseX, mouseY, x, y, armorStack);
        }

        // 4. Offhand
        drawInvSlot(graphics, mouseX, mouseY, bgX + 76, bgY + 61, inv.player.getOffhandItem());
    }

    private void drawInvSlot(GuiGraphicsExtractor graphics, int mx, int my, int x, int y, ItemStack stack) {
        if (!stack.isEmpty()) {
            graphics.fakeItem(stack, x + 1, y + 1);
        }

        // Highlight logic
        if (isHovered(mx, my, x, y, SLOT_SIZE)) {
            graphics.fill(x, y, x + 18, y + 18, 0x40FFFFFF);
            if (!stack.isEmpty()) {
                graphics.setTooltipForNextFrame(font, stack, mx, my);
            }
        }
    }

    private void renderEffectsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int startX = width / 2 - (columns * (SLOT_SIZE + 2)) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredEffects.size()) break;

            int x = startX + (i % columns) * (SLOT_SIZE + 2);
            int y = getGridTop() + (i / columns) * (SLOT_SIZE + 2);

            MobEffect effect = filteredEffects.get(index);

            String path = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect)).getPath();
            Identifier spriteId = Identifier.fromNamespaceAndPath("minecraft", "mob_effect/" + path);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, x, y, 18, 18);

            if (isHovered(mouseX, mouseY, x, y, SLOT_SIZE + 2)) {
                graphics.fill(x, y, x + (SLOT_SIZE + 2), y + (SLOT_SIZE + 2), 0x40FFFFFF);
                graphics.setTooltipForNextFrame(font, Component.translatable(effect.getDescriptionId()), mouseX, mouseY);
            }
        }
    }

    private boolean isHovered(double mx, double my, int x, int y, int size) {
        return mx >= x && mx < x + size && my >= y && my < y + size;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x();
        double my = click.y();

        if (TabType.ITEMS == currentTab) {
            int startX = width / 2 - (columns * SLOT_SIZE) / 2;

            for (int i = 0; i < maxRows * columns; i++) {
                int x = startX + (i % columns) * SLOT_SIZE;
                int y = getGridTop() + (i / columns) * SLOT_SIZE;

                if (isHovered(mx, my, x, y, SLOT_SIZE)) {
                    int index = i + scrollOffset * columns;
                    if (index < filteredItems.size()) {
                        onSelect.accept(BuiltInRegistries.ITEM.getKey(filteredItems.get(index)).toString());
                        onClose();
                        return true;
                    }
                }
            }
        } else if (TabType.INVENTORY == currentTab) {
            int bgX = width / 2 - INV_WIDTH / 2;
            int bgY = height / 2 - INV_HEIGHT / 2 + 10;

            // Hotbar (0 - 8)
            for (int i = 0; i < 9; i++) {
                if (isHovered(mx, my, bgX + 7 + i * 18, bgY + 141, SLOT_SIZE)) {
                    onSelect.accept("radial:slot.hotbar." + i);
                    onClose();
                    return true;
                }
            }

            // Main Inventory (0 - 26)
            for (int i = 0; i < 27; i++) {
                if (isHovered(mx, my, bgX + 7 + (i % 9) * 18, bgY + 83 + (i / 9) * 18, SLOT_SIZE)) {
                    onSelect.accept("radial:slot.inventory." + i);
                    onClose();
                    return true;
                }
            }

            // Armor
            String[] armorNames = {"head", "chest", "legs", "feet"};
            for (int i = 0; i < 4; i++) {
                if (isHovered(mx, my, bgX + 7, bgY + 7 + i * 18, SLOT_SIZE)) {
                    onSelect.accept("radial:slot.armor." + armorNames[i]);
                    onClose();
                    return true;
                }
            }

            // Offhand
            if (isHovered(mx, my, bgX + 76, bgY + 61, SLOT_SIZE)) {
                onSelect.accept("radial:slot.offhand");
                onClose();
                return true;
            }
        } else if (TabType.EFFECTS == currentTab) {
            int startX = width / 2 - (columns * (SLOT_SIZE + 2)) / 2;

            for (int i = 0; i < maxRows * columns; i++) {
                int x = startX + (i % columns) * (SLOT_SIZE + 2);
                int y = getGridTop() + (i / columns) * (SLOT_SIZE + 2);

                if (isHovered(mx, my, x, y, SLOT_SIZE)) {
                    int index = i + scrollOffset * columns;
                    if (index < filteredEffects.size()) {
                        String effectId = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(filteredEffects.get(index))).toString();
                        onSelect.accept("radial:effect." + effectId);
                        onClose();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Only allow scrolling in the Items tab
        if (TabType.ITEMS == currentTab) {
            int totalRows = (filteredItems.size() + columns - 1) / columns;
            int maxScroll = Math.max(0, totalRows - maxRows);

            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        }
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}