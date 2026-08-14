package dev.velolib.radial.ui.screen;

import dev.velolib.radial.api.ShortcutEntry;
import dev.velolib.radial.api.ShortcutRegistry;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class ShortcutSelectionScreen extends Screen {

    private static final int ENTRY_HEIGHT = 28;

    private final Screen parent;
    private final Consumer<Identifier> onSelect;

    private ShortcutList shortcutList;

    public ShortcutSelectionScreen(Screen parent, Consumer<Identifier> onSelect) {

        super(Component.literal("Select Shortcut"));

        this.parent = parent;
        this.onSelect = onSelect;
    }

    private int getListStartY() {
        return 45;
    }

    private int getListBottom() {
        return height - 40;
    }

    private int getListWidth() {
        return Math.min(350, (int) (width * 0.9));
    }

    private int getListHeight() {
        return Math.max(1, getListBottom() - getListStartY());
    }

    private int getListLeft() {
        return width / 2 - getListWidth() / 2;
    }

    @Override
    protected void init() {
        int listWidth = getListWidth();
        int listLeft = getListLeft();
        int listTop = getListStartY();
        int listHeight = getListHeight();

        EditBox searchField =
                new EditBox(font, listLeft, 15, listWidth, 20, Component.translatable("screen.radial.editor.search"));

        searchField.setHint(Component.translatable("screen.radial.editor.search"));

        searchField.setResponder(this::updateSearch);

        addRenderableWidget(searchField);

        shortcutList = new ShortcutList(Minecraft.getInstance(), listWidth, listHeight, listTop, ENTRY_HEIGHT);

        shortcutList.updateSizeAndPosition(listWidth, listHeight, listLeft, listTop);

        addRenderableWidget(shortcutList);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());

        setInitialFocus(searchField);

        updateSearch("");
    }

    private void updateSearch(String query) {
        if (shortcutList == null) {
            return;
        }

        String q = query.toLowerCase();

        List<ShortcutEntryItem> entries = ShortcutRegistry.getRegisteredShortcuts().entrySet().stream()
                .filter(entry -> {
                    String name = entry.getValue().name().getString().toLowerCase();

                    String id = entry.getKey().toString().toLowerCase();

                    return name.contains(q) || id.contains(q);
                })
                .map(entry -> new ShortcutEntryItem(entry, onSelect, this::onClose))
                .collect(Collectors.toList());

        shortcutList.replaceEntries(entries);
        shortcutList.setScrollAmount(0.0);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static class ShortcutList extends ObjectSelectionList<ShortcutEntryItem> {

        private ShortcutList(Minecraft minecraft, int width, int height, int y, int itemHeight) {

            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.min(330, getWidth() - 20);
        }
    }

    private static class ShortcutEntryItem extends ObjectSelectionList.Entry<ShortcutEntryItem> {

        private final Identifier id;
        private final ShortcutEntry entry;
        private final Consumer<Identifier> onSelect;
        private final Runnable onClose;

        private ShortcutEntryItem(
                Map.Entry<Identifier, ShortcutEntry> mapEntry, Consumer<Identifier> onSelect, Runnable onClose) {

            this.id = mapEntry.getKey();
            this.entry = mapEntry.getValue();
            this.onSelect = onSelect;
            this.onClose = onClose;
        }

        @Override
        public void extractContent(
                GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {

            Minecraft client = Minecraft.getInstance();

            int left = getContentX();
            int top = getContentY();
            int right = getContentRight();
            int bottom = getContentBottom();

            graphics.fill(left, top + 1, right, bottom - 1, hovered ? 0x80FFFFFF : 0x40000000);

            int textY = top + ((bottom - top) - client.font.lineHeight) / 2;

            String displayName = entry.name().getString();

            graphics.text(client.font, displayName, left + 8, textY, 0xFFFFFFFF);

            String idString = id.toString();

            int idWidth = client.font.width(idString);

            graphics.text(client.font, idString, right - idWidth - 8, textY, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

            if (event.button() != 0) {
                return false;
            }

            onSelect.accept(id);
            onClose.run();

            return true;
        }

        @Override
        public @NonNull Component getNarration() {
            return entry.name();
        }
    }
}
