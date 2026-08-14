package dev.velolib.radial.ui.screen;

import dev.velolib.radial.integration.MalilibIntegration;
import dev.velolib.radial.integration.MalilibIntegration.MalilibAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.jspecify.annotations.NonNull;

public class MalilibSelectionScreen extends Screen {

    private static final int ENTRY_HEIGHT = 28;

    private final Screen parent;
    private final Consumer<MalilibAction> onSelect;
    private final Map<String, List<MalilibAction>> actionsByMod;

    private final List<Button> tabButtons = new ArrayList<>();

    private String currentTab;
    private List<MalilibAction> currentActions = new ArrayList<>();

    private EditBox searchField;
    private MalilibList malilibList;

    public MalilibSelectionScreen(Screen parent, Consumer<MalilibAction> onSelect) {

        super(Component.literal("Select Malilib Action"));

        this.parent = parent;
        this.onSelect = onSelect;

        List<MalilibAction> actions = MalilibIntegration.getAllActions();

        this.actionsByMod = actions.stream()
                .collect(Collectors.groupingBy(MalilibAction::modName, TreeMap::new, Collectors.toList()));

        if (!actionsByMod.isEmpty()) {
            currentTab = actionsByMod.keySet().iterator().next();
        }
    }

    private int getListStartY() {
        return 65;
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
        tabButtons.clear();

        if (actionsByMod.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose())
                    .bounds(width / 2 - 100, height - 28, 200, 20)
                    .build());

            return;
        }

        int tabWidth = Math.min(80, width / actionsByMod.size());

        int xOffset = (width - tabWidth * actionsByMod.size()) / 2;

        for (String modName : actionsByMod.keySet()) {
            Button button = Button.builder(Component.literal(modName), _ -> {
                        setTab(modName);
                        updateTabButtonStates();
                    })
                    .bounds(xOffset, 10, tabWidth, 20)
                    .build();

            button.active = !modName.equals(currentTab);

            tabButtons.add(button);
            addRenderableWidget(button);

            xOffset += tabWidth;
        }

        int listWidth = getListWidth();

        searchField = new EditBox(
                font,
                width / 2 - listWidth / 2,
                35,
                listWidth,
                20,
                Component.translatable("screen.radial.editor.search"));

        searchField.setHint(Component.translatable("screen.radial.editor.search"));

        searchField.setResponder(this::updateSearch);

        addRenderableWidget(searchField);

        malilibList =
                new MalilibList(Minecraft.getInstance(), listWidth, getListHeight(), getListStartY(), ENTRY_HEIGHT);

        malilibList.updateSizeAndPosition(listWidth, getListHeight(), getListLeft(), getListStartY());

        addRenderableWidget(malilibList);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());

        setInitialFocus(searchField);

        setTab(currentTab);
    }

    private void setTab(String tabName) {
        currentTab = tabName;

        currentActions = actionsByMod.getOrDefault(tabName, new ArrayList<>());

        if (searchField != null) {
            searchField.setValue("");
        }

        updateSearch("");
    }

    private void updateSearch(String query) {
        if (malilibList == null) {
            return;
        }

        String q = query.toLowerCase();

        List<MalilibEntry> entries = currentActions.stream()
                .filter(action -> action.name().toLowerCase().contains(q)
                        || action.displayName().toLowerCase().contains(q))
                .map(action -> new MalilibEntry(action, onSelect, this::onClose))
                .collect(Collectors.toList());

        malilibList.replaceEntries(entries);
        malilibList.setScrollAmount(0.0);
    }

    private void updateTabButtonStates() {
        for (Button button : tabButtons) {
            button.active = !button.getMessage().getString().equals(currentTab);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        if (actionsByMod.isEmpty()) {
            graphics.centeredText(
                    font, "No Malilib mods found or no hotkeys available.", width / 2, height / 2, 0xFF555555);

            super.extractRenderState(graphics, mouseX, mouseY, delta);

            return;
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static class MalilibList extends ObjectSelectionList<MalilibEntry> {

        private MalilibList(Minecraft minecraft, int width, int height, int y, int itemHeight) {

            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.min(330, getWidth() - 20);
        }
    }

    private static class MalilibEntry extends ObjectSelectionList.Entry<MalilibEntry> {

        private final MalilibAction action;
        private final Consumer<MalilibAction> onSelect;
        private final Runnable onClose;

        private MalilibEntry(MalilibAction action, Consumer<MalilibAction> onSelect, Runnable onClose) {

            this.action = action;
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

            graphics.text(client.font, action.displayName(), left + 8, textY, 0xFFFFFFFF);

            String category = action.category();

            int categoryWidth = client.font.width(category);

            graphics.text(client.font, category, right - categoryWidth - 8, textY, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

            if (event.button() != 0) {
                return false;
            }

            onSelect.accept(action);
            onClose.run();

            return true;
        }

        @Override
        public @NonNull Component getNarration() {
            return Component.literal(action.displayName());
        }
    }
}
