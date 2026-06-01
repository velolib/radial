package velo.radial.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class KeybindPickerScreen extends Screen {

    private static final int ENTRY_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<String> onSelect;

    private EditBox searchField;
    private List<KeyMapping> filteredKeys = new ArrayList<>();
    private int scrollOffset = 0;

    public KeybindPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.literal("Select Keybind"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        filteredKeys = Arrays.asList(minecraft.options.keyMappings);

        int searchWidth = Math.min(400, (int) (width * 0.8));
        int left = width / 2 - searchWidth / 2;

        searchField = new EditBox(
                font,
                left,
                15,
                searchWidth,
                20,
                Component.literal("Search...")
        );
        searchField.setHint(Component.literal("Search Keybinds..."));
        searchField.setResponder(this::updateSearch);

        addRenderableWidget(searchField);
        setInitialFocus(searchField);
    }

    private void updateSearch(String query) {

        String q = query.toLowerCase();

        filteredKeys = Arrays.stream(minecraft.options.keyMappings)
                .filter(key -> {
                    String actionName =
                            Component.translatable(key.getName()).getString().toLowerCase();
                    String category =
                            key.getCategory().label().getString().toLowerCase();

                    return actionName.contains(q) || category.contains(q);
                })
                .collect(Collectors.toList());

        scrollOffset = 0;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int startY = 45;
        int listWidth = Math.min(350, (int) (width * 0.9));
        int left = width / 2 - listWidth / 2;

        int maxEntries =
                Math.max(1, (height - startY - 20) / ENTRY_HEIGHT);

        for (int i = 0; i < maxEntries; i++) {
            int index = i + scrollOffset;
            if (index >= filteredKeys.size()) break;

            KeyMapping key = filteredKeys.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            boolean hovered =
                    mouseX >= left && mouseX <= left + listWidth &&
                            mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            int bgColor = hovered ? 0x80FFFFFF : 0x40000000;
            graphics.fill(left, y, left + listWidth, y + ENTRY_HEIGHT - 2, bgColor);

            String actionName = Component.translatable(key.getName()).getString();
            String boundKey =
                    Component.translatable(key.saveString()).getString();

            graphics.text(
                    font,
                    actionName + " [" + boundKey + "]",
                    left + 5,
                    y + 5,
                    0xFFFFFFFF
            );

            Component categoryLabel = key.getCategory().label();
            int catWidth = font.width(categoryLabel);

            graphics.text(
                    font,
                    categoryLabel,
                    left + listWidth - catWidth - 5,
                    y + 5,
                    0xFFAAAAAA
            );
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        int startY = 45;
        int listWidth = Math.min(350, (int) (width * 0.9));
        int left = width / 2 - listWidth / 2;

        double mx = minecraft.mouseHandler.xpos()
                * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth();
        double my = minecraft.mouseHandler.ypos()
                * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight();

        int maxEntries = (height - startY - 20) / ENTRY_HEIGHT;

        for (int i = 0; i < maxEntries; i++) {
            int y = startY + i * ENTRY_HEIGHT;

            if (mx >= left && mx <= left + listWidth &&
                    my >= y && my < y + ENTRY_HEIGHT) {

                int index = i + scrollOffset;
                if (index < filteredKeys.size()) {
                    onSelect.accept(filteredKeys.get(index).getName());
                    minecraft.setScreen(parent);
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        int maxEntries = (height - 45 - 20) / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, filteredKeys.size() - maxEntries);

        scrollOffset = Math.max(
                0,
                Math.min(maxScroll, scrollOffset - (int) verticalAmount)
        );
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
