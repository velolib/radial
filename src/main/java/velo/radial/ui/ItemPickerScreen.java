package velo.radial.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemPickerScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int GRID_TOP = 45;
    private static final int HORIZONTAL_PADDING = 40;
    private static final int BOTTOM_PADDING = 20;

    private final Screen parent;
    private final Consumer<String> onSelect;

    private EditBox searchField;
    private List<Item> filteredItems;

    private int scrollOffset = 0;
    private int columns = 12;
    private int maxRows = 7;

    public ItemPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.literal("Item Selector"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.filteredItems = BuiltInRegistries.ITEM.stream().collect(Collectors.toList());
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(400, (int) (width * 0.8));
        this.searchField = new EditBox(
                font,
                width / 2 - searchWidth / 2,
                15,
                searchWidth,
                20,
                Component.translatable("gui.recipebook.search_hint")
        );

        this.searchField.setHint(Component.translatable("gui.recipebook.search_hint"));
        this.searchField.setResponder(this::updateSearch);
        this.addRenderableWidget(searchField);
        this.setInitialFocus(searchField);

        columns = Math.max(1, (width - HORIZONTAL_PADDING) / SLOT_SIZE);
        maxRows = Math.max(1, (height - GRID_TOP - BOTTOM_PADDING) / SLOT_SIZE);
    }

    private void updateSearch(String query) {
        String q = query.toLowerCase();
        filteredItems = BuiltInRegistries.ITEM.stream()
                .filter(item ->
                        item.getDefaultInstance().getItemName().getString().toLowerCase().contains(q)
                                || BuiltInRegistries.ITEM.getKey(item).toString().contains(q)
                )
                .toList();

        scrollOffset = 0;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int startX = width / 2 - (columns * SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredItems.size()) break;

            int x = startX + (i % columns) * SLOT_SIZE;
            int y = GRID_TOP + (i / columns) * SLOT_SIZE;

            ItemStack stack = new ItemStack(filteredItems.get(index));
            graphics.fakeItem(stack, x + 1, y + 1);

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40FFFFFF);
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {

        double mx = click.x();
        double my = click.y();

        int startX = width / 2 - (columns * SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int x = startX + (i % columns) * SLOT_SIZE;
            int y = GRID_TOP + (i / columns) * SLOT_SIZE;

            if (mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE) {
                int index = i + scrollOffset * columns;
                if (index < filteredItems.size()) {
                    onSelect.accept(BuiltInRegistries.ITEM.getKey(filteredItems.get(index)).toString());
                    minecraft.setScreen(parent);
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalRows = (filteredItems.size() + columns - 1) / columns;
        int maxScroll = Math.max(0, totalRows - maxRows);

        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
