package velo.radial.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IconPickerScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final List<String> TABS = List.of("Items");

    private final Screen parent;
    private final Consumer<String> onSelect;

    private final List<Button> tabButtons = new ArrayList<>();
    private String currentTab = TABS.get(0);

    private EditBox searchField;
    private List<Item> filteredItems = new ArrayList<>();

    private int scrollOffset = 0;
    private int columns = 12;
    private int maxRows = 7;

    public IconPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.literal("Icon Selector"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    private void setTab(String tabName) {
        this.currentTab = tabName;

        if (this.searchField != null) {
            this.searchField.setValue("");
        }

        this.updateSearch("");
        this.scrollOffset = 0;
        this.updateTabButtonStates();
    }

    private void updateSearch(String query) {
        String q = query.toLowerCase();

        if ("Items".equals(currentTab)) {
            filteredItems = BuiltInRegistries.ITEM.stream()
                    .filter(item ->
                            item.getDefaultInstance().getItemName().getString().toLowerCase().contains(q)
                                    || BuiltInRegistries.ITEM.getKey(item).toString().contains(q)
                    )
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

        for (String tabName : TABS) {
            Button btn = Button.builder(
                    Component.literal(tabName),
                    button -> setTab(tabName)
            ).bounds(xOffset, 10, tabWidth, 20).build();

            btn.active = !tabName.equals(currentTab);
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
                Component.translatable("gui.recipebook.search_hint")
        );

        this.searchField.setHint(Component.translatable("gui.recipebook.search_hint"));
        this.searchField.setResponder(this::updateSearch);
        this.addRenderableWidget(searchField);
        this.setInitialFocus(searchField);

        // Render Cancel Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(width / 2 - 100, height - 28, 200, 20).build());

        // Calculate Grid Bounds
        columns = Math.max(1, searchWidth / SLOT_SIZE);
        maxRows = Math.max(1, (height - getGridTop() - 40) / SLOT_SIZE);

        // Initial populate
        updateSearch(searchField.getValue());
    }

    private void updateTabButtonStates() {
        for (Button btn : this.tabButtons) {
            btn.active = !btn.getMessage().getString().equals(this.currentTab);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        if ("Items".equals(currentTab)) {
            int startX = width / 2 - (columns * SLOT_SIZE) / 2;

            for (int i = 0; i < maxRows * columns; i++) {
                int index = i + scrollOffset * columns;
                if (index >= filteredItems.size()) break;

                int x = startX + (i % columns) * SLOT_SIZE;
                int y = getGridTop() + (i / columns) * SLOT_SIZE;

                ItemStack stack = new ItemStack(filteredItems.get(index));
                graphics.fakeItem(stack, x + 1, y + 1);

                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40FFFFFF);
                    graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
                }
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x();
        double my = click.y();

        if ("Items".equals(currentTab)) {
            int startX = width / 2 - (columns * SLOT_SIZE) / 2;

            for (int i = 0; i < maxRows * columns; i++) {
                int x = startX + (i % columns) * SLOT_SIZE;
                int y = getGridTop() + (i / columns) * SLOT_SIZE;

                if (mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE) {
                    int index = i + scrollOffset * columns;
                    if (index < filteredItems.size()) {
                        onSelect.accept(BuiltInRegistries.ITEM.getKey(filteredItems.get(index)).toString());
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
        if ("Items".equals(currentTab)) {
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