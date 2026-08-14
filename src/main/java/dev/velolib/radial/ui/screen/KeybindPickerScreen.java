package dev.velolib.radial.ui.screen;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class KeybindPickerScreen extends Screen {

    private static final int ENTRY_HEIGHT = 28;

    private final Screen parent;
    private final Consumer<String> onSelect;

    private KeybindList keybindList;

    public KeybindPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.literal("Select Keybind"));

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

        EditBox searchField = new EditBox(font, listLeft, 15, listWidth, 20, Component.translatable("screen.radial.editor.search"));

        searchField.setHint(Component.translatable("screen.radial.editor.search"));

        searchField.setResponder(this::updateSearch);

        addRenderableWidget(searchField);

        keybindList = new KeybindList(Minecraft.getInstance(), listWidth, listHeight, listTop, ENTRY_HEIGHT);

        keybindList.updateSizeAndPosition(listWidth, listHeight, listLeft, listTop);

        addRenderableWidget(keybindList);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());

        setInitialFocus(searchField);

        updateSearch("");
    }

    private void updateSearch(String query) {
        if (keybindList == null) {
            return;
        }

        String q = query.toLowerCase();

        List<KeybindEntry> entries = Arrays.stream(minecraft.options.keyMappings).filter(key -> {

            String actionName = Component.translatable(key.getName()).getString().toLowerCase();

            String category = key.getCategory().label().getString().toLowerCase();

            return actionName.contains(q) || category.contains(q);
        }).map(key -> new KeybindEntry(key, onSelect)).collect(Collectors.toList());

        keybindList.replaceEntries(entries);

        keybindList.setScrollAmount(0.0);
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

    private static class KeybindList extends ObjectSelectionList<KeybindEntry> {

        private KeybindList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.min(330, getWidth() - 20);
        }
    }

    private static class KeybindEntry extends ObjectSelectionList.Entry<KeybindEntry> {

        private final KeyMapping key;
        private final Consumer<String> onSelect;

        private KeybindEntry(KeyMapping key, Consumer<String> onSelect) {
            this.key = key;

            this.onSelect = onSelect;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            Minecraft client = Minecraft.getInstance();

            int left = getContentX();

            int top = getContentY();

            int right = getContentRight();

            int bottom = getContentBottom();

            graphics.fill(left, top + 1, right, bottom - 1, hovered ? 0x80FFFFFF : 0x40000000);

            String actionName = Component.translatable(key.getName()).getString();

            String boundKey = key.saveString();

            String display = actionName + " [" + boundKey + "]";

            int textY = top + ((bottom - top) - client.font.lineHeight) / 2;

            graphics.text(client.font, display, left + 8, textY, 0xFFFFFFFF);

            Component category = key.getCategory().label();

            int categoryWidth = client.font.width(category);

            graphics.text(client.font, category, right - categoryWidth - 8, textY, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0) {
                return false;
            }

            onSelect.accept(key.getName());

            return true;
        }

        @Override
        public @NonNull Component getNarration() {
            return Component.translatable(key.getName());
        }
    }
}