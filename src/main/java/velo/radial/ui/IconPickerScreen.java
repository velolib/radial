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

    // Sizing Constants
    private static final int ITEM_SLOT_SIZE = 16;
    private static final int EFFECT_SLOT_SIZE = 18;
    private static final int INV_SLOT_SIZE = 18; // Vanilla inventory slot bounding box
    private static final List<TabType> TABS = List.of(TabType.ITEMS, TabType.INVENTORY, TabType.EFFECTS, TabType.GLYPHS);
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
    private List<MobEffect> filteredEffects = new ArrayList<>();
    private List<String> filteredGlyphs = new ArrayList<>();
    private int scrollOffset = 0;
    private int columns = 12;
    private int maxRows = 7;

    public IconPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.literal("Icon Selector"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.currentTab = TABS.getFirst();
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
                    _ -> setTab(tab)
            ).bounds(xOffset, 10, tabWidth, 20).build();

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
        this.addRenderableWidget(searchField);
        this.setInitialFocus(searchField);

        // Render Cancel Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                _ -> onClose()
        ).bounds(width / 2 - 100, height - 28, 200, 20).build());

        // Initial setup
        setTab(this.currentTab);
        updateSearch(searchField.getValue());
    }

    private void setTab(TabType tabName) {
        this.currentTab = tabName;
        this.scrollOffset = 0;

        if (this.searchField != null) {
            this.searchField.setValue("");
            this.searchField.visible = (currentTab != TabType.INVENTORY);
        }

        updateTabButtonStates();
        recalculateGrid();
        updateSearch("");
    }

    private void recalculateGrid() {
        int slotSize = currentTab == TabType.EFFECTS ? EFFECT_SLOT_SIZE : ITEM_SLOT_SIZE;
        this.columns = Math.max(1, getListWidth() / slotSize);
        this.maxRows = Math.max(1, (height - getGridTop() - 40) / slotSize);
    }

    private void updateTabButtonStates() {
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).active = (TABS.get(i) != currentTab);
        }
    }

    private void updateSearch(String query) {
        String q = query.toLowerCase();
        this.scrollOffset = 0;

        if (currentTab == TabType.ITEMS) {
            filteredItems = BuiltInRegistries.ITEM.stream()
                    .filter(item -> item.getDefaultInstance().getItemName().getString().toLowerCase().contains(q)
                            || BuiltInRegistries.ITEM.getKey(item).toString().contains(q))
                    .toList();
        } else if (currentTab == TabType.EFFECTS) {
            filteredEffects = BuiltInRegistries.MOB_EFFECT.stream()
                    .filter(effect -> Component.translatable(effect.getDescriptionId()).getString().toLowerCase().contains(q)
                            || Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect)).toString().contains(q))
                    .toList();
        } else if (currentTab == TabType.GLYPHS) {
            filteredGlyphs = velo.radial.util.GlyphCache.getGlyphs().stream()
                    .filter(g -> g.toLowerCase().contains(q))
                    .toList();
        }
    }

    // --- Layout Helpers ---
    private int getGridTop() {
        return 65;
    }

    private int getListWidth() {
        return Math.min(350, (int) (width * 0.9));
    }

    private boolean isHovered(double mx, double my, int x, int y, int size) {
        return mx >= x && mx < x + size && my >= y && my < y + size;
    }

    // --- Render Logic ---
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        switch (currentTab) {
            case ITEMS -> renderItemsTab(graphics, mouseX, mouseY);
            case INVENTORY -> renderInventoryTab(graphics, mouseX, mouseY);
            case EFFECTS -> renderEffectsTab(graphics, mouseX, mouseY);
            case GLYPHS -> renderGlyphsTab(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderItemsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int startX = width / 2 - (columns * ITEM_SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredItems.size()) break;

            int x = startX + (i % columns) * ITEM_SLOT_SIZE;
            int y = getGridTop() + (i / columns) * ITEM_SLOT_SIZE;

            ItemStack stack = new ItemStack(filteredItems.get(index));
            graphics.fakeItem(stack, x, y);

            if (isHovered(mouseX, mouseY, x, y, ITEM_SLOT_SIZE)) {
                graphics.fill(x, y, x + ITEM_SLOT_SIZE, y + ITEM_SLOT_SIZE, 0x40FFFFFF);
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
    }

    private void renderEffectsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int startX = width / 2 - (columns * EFFECT_SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredEffects.size()) break;

            int x = startX + (i % columns) * EFFECT_SLOT_SIZE;
            int y = getGridTop() + (i / columns) * EFFECT_SLOT_SIZE;

            MobEffect effect = filteredEffects.get(index);
            String path = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect)).getPath();
            Identifier spriteId = Identifier.fromNamespaceAndPath("minecraft", "mob_effect/" + path);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, x, y, EFFECT_SLOT_SIZE, EFFECT_SLOT_SIZE);

            if (isHovered(mouseX, mouseY, x, y, EFFECT_SLOT_SIZE)) {
                graphics.fill(x, y, x + EFFECT_SLOT_SIZE, y + EFFECT_SLOT_SIZE, 0x40FFFFFF);
                graphics.setTooltipForNextFrame(font, Component.translatable(effect.getDescriptionId()), mouseX, mouseY);
            }
        }
    }

    private void renderInventoryTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (minecraft.player == null) return;
        Inventory inv = minecraft.player.getInventory();

        int bgX = width / 2 - INV_WIDTH / 2;
        int bgY = height / 2 - INV_HEIGHT / 2 + 10;

        Component infoText = Component.translatable("screen.radial.editor.icon_picker.inventory.info");
        int textWidth = font.width(infoText);
        graphics.text(font, infoText, width / 2 - textWidth / 2, bgY - 15, 0xFFAAAAAA);

        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, bgX, bgY, 0, 0, INV_WIDTH, INV_HEIGHT, 256, 256, 0xFFFFFFFF);

        // Hotbar
        for (int i = 0; i < 9; i++) {
            drawInvSlot(graphics, mouseX, mouseY, bgX + 7 + i * 18, bgY + 141, inv.getNonEquipmentItems().get(i));
        }

        // Main Inventory
        for (int i = 0; i < 27; i++) {
            drawInvSlot(graphics, mouseX, mouseY, bgX + 7 + (i % 9) * 18, bgY + 83 + (i / 9) * 18, inv.getNonEquipmentItems().get(i + 9));
        }

        // Armor
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            drawInvSlot(graphics, mouseX, mouseY, bgX + 7, bgY + 7 + i * 18, minecraft.player.getItemBySlot(armorSlots[i]));
        }

        // Offhand
        drawInvSlot(graphics, mouseX, mouseY, bgX + 76, bgY + 61, inv.player.getOffhandItem());
    }

    private void drawInvSlot(GuiGraphicsExtractor graphics, int mx, int my, int x, int y, ItemStack stack) {
        if (!stack.isEmpty()) {
            graphics.fakeItem(stack, x + 1, y + 1);
        }
        if (isHovered(mx, my, x, y, INV_SLOT_SIZE)) {
            graphics.fill(x, y, x + INV_SLOT_SIZE, y + INV_SLOT_SIZE, 0x40FFFFFF);
            if (!stack.isEmpty()) {
                graphics.setTooltipForNextFrame(font, stack, mx, my);
            }
        }
    }

    private void renderGlyphsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int startX = width / 2 - (columns * ITEM_SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredGlyphs.size()) break;

            int x = startX + (i % columns) * ITEM_SLOT_SIZE;
            int y = getGridTop() + (i / columns) * ITEM_SLOT_SIZE;

            String glyph = filteredGlyphs.get(index);

            int textWidth = font.width(glyph);

            // Use Math.round with float division, and 8 for the visual height instead of lineHeight
            int textX = x + Math.round((ITEM_SLOT_SIZE - textWidth) / 2.0f);
            int textY = y + Math.round((ITEM_SLOT_SIZE - 8) / 2.0f);

            graphics.text(font, glyph, textX, textY, 0xFFFFFFFF);

            if (isHovered(mouseX, mouseY, x, y, ITEM_SLOT_SIZE)) {
                graphics.fill(x, y, x + ITEM_SLOT_SIZE, y + ITEM_SLOT_SIZE, 0x40FFFFFF);
            }
        }
    }

    // --- Interaction Logic ---
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x();
        double my = click.y();

        if (currentTab == TabType.ITEMS) {
            int startX = width / 2 - (columns * ITEM_SLOT_SIZE) / 2;
            for (int i = 0; i < maxRows * columns; i++) {
                int x = startX + (i % columns) * ITEM_SLOT_SIZE;
                int y = getGridTop() + (i / columns) * ITEM_SLOT_SIZE;

                if (isHovered(mx, my, x, y, ITEM_SLOT_SIZE)) {
                    int index = i + scrollOffset * columns;
                    if (index < filteredItems.size()) {
                        onSelect.accept(BuiltInRegistries.ITEM.getKey(filteredItems.get(index)).toString());
                        onClose();
                        return true;
                    }
                }
            }
        } else if (currentTab == TabType.INVENTORY) {
            int bgX = width / 2 - INV_WIDTH / 2;
            int bgY = height / 2 - INV_HEIGHT / 2 + 10;

            // Hotbar
            for (int i = 0; i < 9; i++) {
                if (isHovered(mx, my, bgX + 7 + i * 18, bgY + 141, INV_SLOT_SIZE)) {
                    onSelect.accept("radial:slot.hotbar." + i);
                    onClose();
                    return true;
                }
            }
            // Main Inventory
            for (int i = 0; i < 27; i++) {
                if (isHovered(mx, my, bgX + 7 + (i % 9) * 18, bgY + 83 + (i / 9) * 18, INV_SLOT_SIZE)) {
                    onSelect.accept("radial:slot.inventory." + i);
                    onClose();
                    return true;
                }
            }
            // Armor
            String[] armorNames = {"head", "chest", "legs", "feet"};
            for (int i = 0; i < 4; i++) {
                if (isHovered(mx, my, bgX + 7, bgY + 7 + i * 18, INV_SLOT_SIZE)) {
                    onSelect.accept("radial:slot.armor." + armorNames[i]);
                    onClose();
                    return true;
                }
            }
            // Offhand
            if (isHovered(mx, my, bgX + 76, bgY + 61, INV_SLOT_SIZE)) {
                onSelect.accept("radial:slot.offhand");
                onClose();
                return true;
            }
        } else if (currentTab == TabType.EFFECTS) {
            int startX = width / 2 - (columns * EFFECT_SLOT_SIZE) / 2;
            for (int i = 0; i < maxRows * columns; i++) {
                int x = startX + (i % columns) * EFFECT_SLOT_SIZE;
                int y = getGridTop() + (i / columns) * EFFECT_SLOT_SIZE;

                if (isHovered(mx, my, x, y, EFFECT_SLOT_SIZE)) {
                    int index = i + scrollOffset * columns;
                    if (index < filteredEffects.size()) {
                        String effectId = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(filteredEffects.get(index))).toString();
                        onSelect.accept("radial:effect." + effectId);
                        onClose();
                        return true;
                    }
                }
            }
        } else if (currentTab == TabType.GLYPHS) {
            int startX = width / 2 - (columns * ITEM_SLOT_SIZE) / 2;
            for (int i = 0; i < maxRows * columns; i++) {
                int x = startX + (i % columns) * ITEM_SLOT_SIZE;
                int y = getGridTop() + (i / columns) * ITEM_SLOT_SIZE;

                if (isHovered(mx, my, x, y, ITEM_SLOT_SIZE)) {
                    int index = i + scrollOffset * columns;
                    if (index < filteredGlyphs.size()) {
                        onSelect.accept("radial:glyph." + filteredGlyphs.get(index));
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
        if (currentTab != TabType.INVENTORY) {

            // Fix the listSize calculation
            int listSize = 0;
            if (currentTab == TabType.ITEMS) listSize = filteredItems.size();
            else if (currentTab == TabType.EFFECTS) listSize = filteredEffects.size();
            else if (currentTab == TabType.GLYPHS) listSize = filteredGlyphs.size();

            int totalRows = (listSize + columns - 1) / columns;
            int maxScroll = Math.max(0, totalRows - maxRows);

            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private enum TabType {
        ITEMS("screen.radial.editor.icon_picker.items"),
        INVENTORY("screen.radial.editor.icon_picker.inventory"),
        EFFECTS("screen.radial.editor.icon_picker.effects"),
        GLYPHS("screen.radial.editor.icon_picker.glyphs");

        final String translationKey;

        TabType(String key) {
            this.translationKey = key;
        }
    }
}