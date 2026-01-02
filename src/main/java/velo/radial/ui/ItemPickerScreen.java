package velo.radial.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

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

    private TextFieldWidget searchField;
    private List<Item> filteredItems;

    private int scrollOffset = 0;
    private int columns = 12;
    private int maxRows = 7;

    public ItemPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Text.literal("Item Selector"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.filteredItems = Registries.ITEM.stream().collect(Collectors.toList());
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(400, (int) (width * 0.8));
        this.searchField = new TextFieldWidget(
                textRenderer,
                width / 2 - searchWidth / 2,
                15,
                searchWidth,
                20,
                Text.literal("Search...")
        );

        this.searchField.setPlaceholder(Text.translatable("gui.recipebook.search_hint"));
        this.searchField.setChangedListener(this::updateSearch);
        this.addDrawableChild(searchField);
        this.setInitialFocus(searchField);

        columns = Math.max(1, (width - HORIZONTAL_PADDING) / SLOT_SIZE);
        maxRows = Math.max(1, (height - GRID_TOP - BOTTOM_PADDING) / SLOT_SIZE);
    }

    private void updateSearch(String query) {
        String q = query.toLowerCase();
        filteredItems = Registries.ITEM.stream()
                .filter(item ->
                        item.getName().getString().toLowerCase().contains(q)
                                || Registries.ITEM.getId(item).toString().contains(q)
                )
                .collect(Collectors.toList());

        scrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int startX = width / 2 - (columns * SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredItems.size()) break;

            int x = startX + (i % columns) * SLOT_SIZE;
            int y = GRID_TOP + (i / columns) * SLOT_SIZE;

            ItemStack stack = new ItemStack(filteredItems.get(index));
            context.drawItem(stack, x + 1, y + 1);

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40FFFFFF);
                context.drawItemTooltip(textRenderer, stack, mouseX, mouseY);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        assert client != null;

        double mx = click.x();
        double my = click.y();

        int startX = width / 2 - (columns * SLOT_SIZE) / 2;

        for (int i = 0; i < maxRows * columns; i++) {
            int x = startX + (i % columns) * SLOT_SIZE;
            int y = GRID_TOP + (i / columns) * SLOT_SIZE;

            if (mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE) {
                int index = i + scrollOffset * columns;
                if (index < filteredItems.size()) {
                    onSelect.accept(Registries.ITEM.getId(filteredItems.get(index)).toString());
                    client.setScreen(parent);
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
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }
}
