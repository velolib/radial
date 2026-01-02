package velo.radial.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class KeybindPickerScreen extends Screen {

    private static final int ENTRY_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<String> onSelect;

    private TextFieldWidget searchField;
    private List<KeyBinding> filteredKeys = new ArrayList<>();
    private int scrollOffset = 0;

    public KeybindPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Text.literal("Select Keybind"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        if (client != null) {
            filteredKeys = Arrays.asList(client.options.allKeys);
        }

        int searchWidth = Math.min(400, (int) (width * 0.8));
        int left = width / 2 - searchWidth / 2;

        searchField = new TextFieldWidget(
                textRenderer,
                left,
                15,
                searchWidth,
                20,
                Text.literal("Search...")
        );
        searchField.setPlaceholder(Text.literal("Search Keybinds..."));
        searchField.setChangedListener(this::updateSearch);

        addDrawableChild(searchField);
        setInitialFocus(searchField);
    }

    private void updateSearch(String query) {
        if (client == null) return;

        String q = query.toLowerCase();

        filteredKeys = Arrays.stream(client.options.allKeys)
                .filter(key -> {
                    String actionName =
                            Text.translatable(key.getId()).getString().toLowerCase();
                    String category =
                            key.getCategory().getLabel().getString().toLowerCase();

                    return actionName.contains(q) || category.contains(q);
                })
                .collect(Collectors.toList());

        scrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int startY = 45;
        int listWidth = Math.min(350, (int) (width * 0.9));
        int left = width / 2 - listWidth / 2;

        int maxEntries =
                Math.max(1, (height - startY - 20) / ENTRY_HEIGHT);

        for (int i = 0; i < maxEntries; i++) {
            int index = i + scrollOffset;
            if (index >= filteredKeys.size()) break;

            KeyBinding key = filteredKeys.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            boolean hovered =
                    mouseX >= left && mouseX <= left + listWidth &&
                            mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            int bgColor = hovered ? 0x80FFFFFF : 0x40000000;
            context.fill(left, y, left + listWidth, y + ENTRY_HEIGHT - 2, bgColor);

            String actionName = Text.translatable(key.getId()).getString();
            String boundKey =
                    Text.translatable(key.getBoundKeyTranslationKey()).getString();

            context.drawTextWithShadow(
                    textRenderer,
                    actionName + " [" + boundKey + "]",
                    left + 5,
                    y + 5,
                    0xFFFFFFFF
            );

            Text categoryLabel = key.getCategory().getLabel();
            int catWidth = textRenderer.getWidth(categoryLabel);

            context.drawTextWithShadow(
                    textRenderer,
                    categoryLabel,
                    left + listWidth - catWidth - 5,
                    y + 5,
                    0xFFAAAAAA
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int startY = 45;
        int listWidth = Math.min(350, (int) (width * 0.9));
        int left = width / 2 - listWidth / 2;

        double mx = client.mouse.getX()
                * client.getWindow().getScaledWidth()
                / client.getWindow().getWidth();
        double my = client.mouse.getY()
                * client.getWindow().getScaledHeight()
                / client.getWindow().getHeight();

        int maxEntries = (height - startY - 20) / ENTRY_HEIGHT;

        for (int i = 0; i < maxEntries; i++) {
            int y = startY + i * ENTRY_HEIGHT;

            if (mx >= left && mx <= left + listWidth &&
                    my >= y && my < y + ENTRY_HEIGHT) {

                int index = i + scrollOffset;
                if (index < filteredKeys.size()) {
                    onSelect.accept(filteredKeys.get(index).getId());
                    client.setScreen(parent);
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
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
