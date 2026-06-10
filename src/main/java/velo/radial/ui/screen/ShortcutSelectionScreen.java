package velo.radial.ui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import velo.radial.api.ShortcutEntry;
import velo.radial.api.ShortcutRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ShortcutSelectionScreen extends Screen {

    private static final int ENTRY_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<Identifier> onSelect;

    private EditBox searchField;
    // Now stores the Map.Entry so we have both the Identifier and the ShortcutEntry object
    private List<Map.Entry<Identifier, ShortcutEntry>> filteredScreens = new ArrayList<>();
    private int scrollOffset = 0;

    public ShortcutSelectionScreen(Screen parent, Consumer<Identifier> onSelect) {
        super(Component.literal("Select Shortcut"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    private void updateSearch(String query) {
        String q = query.toLowerCase();

        filteredScreens = ShortcutRegistry.getRegisteredShortcuts().entrySet().stream()
                .filter(mapEntry -> {
                    String name = mapEntry.getValue().name().getString().toLowerCase();
                    String id = mapEntry.getKey().toString().toLowerCase();

                    return name.contains(q) || id.contains(q);
                })
                .collect(Collectors.toList());

        scrollOffset = 0;
    }

    // --- Layout Helpers ---
    private int getListStartY() {
        return 45;
    }

    private int getListWidth() {
        return Math.min(350, (int) (width * 0.9));
    }

    private int getListLeft() {
        return width / 2 - getListWidth() / 2;
    }

    private int getMaxEntries() {
        return Math.max(1, (height - getListStartY() - 40) / ENTRY_HEIGHT);
    }

    @Override
    protected void init() {
        // Initialize the full list from the registry
        filteredScreens = new ArrayList<>(ShortcutRegistry.getRegisteredShortcuts().entrySet());

        int searchWidth = getListWidth();
        int left = getListLeft();

        searchField = new EditBox(
                font,
                left,
                15,
                searchWidth,
                20,
                Component.translatable("screen.radial.editor.search")
        );
        searchField.setHint(Component.translatable("screen.radial.editor.search"));
        searchField.setResponder(this::updateSearch);

        addRenderableWidget(searchField);
        setInitialFocus(searchField);

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                _ -> onClose()
        ).bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int startY = getListStartY();
        int listWidth = getListWidth();
        int left = getListLeft();
        int maxEntries = getMaxEntries();

        for (int i = 0; i < maxEntries; i++) {
            int index = i + scrollOffset;
            if (index >= filteredScreens.size()) break;

            Map.Entry<Identifier, ShortcutEntry> mapEntry = filteredScreens.get(index);
            Identifier id = mapEntry.getKey();
            ShortcutEntry entry = mapEntry.getValue();

            int y = startY + i * ENTRY_HEIGHT;
            boolean hovered = mouseX >= left && mouseX <= left + listWidth && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            int bgColor = hovered ? 0x80FFFFFF : 0x40000000;
            graphics.fill(left, y, left + listWidth, y + ENTRY_HEIGHT - 2, bgColor);

            // Draw Display Name on the left
            String displayName = entry.name().getString();
            graphics.text(
                    font,
                    displayName,
                    left + 5,
                    y + 5,
                    0xFFFFFFFF
            );

            // Draw the Identifier (namespace:path) right-aligned
            String idString = id.toString();
            int idWidth = font.width(idString);
            graphics.text(
                    font,
                    idString,
                    left + listWidth - idWidth - 5,
                    y + 5,
                    0xFFAAAAAA
            );
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        int startY = getListStartY();
        int listWidth = getListWidth();
        int left = getListLeft();
        int maxEntries = getMaxEntries();

        double mx = click.x();
        double my = click.y();

        for (int i = 0; i < maxEntries; i++) {
            int y = startY + i * ENTRY_HEIGHT;

            if (mx >= left && mx <= left + listWidth && my >= y && my < y + ENTRY_HEIGHT) {
                int index = i + scrollOffset;
                if (index < filteredScreens.size()) {
                    // Send the Identifier back up using getKey()
                    onSelect.accept(filteredScreens.get(index).getKey());
                    onClose();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredScreens.size() - getMaxEntries());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}