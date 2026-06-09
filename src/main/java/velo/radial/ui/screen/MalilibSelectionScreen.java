package velo.radial.ui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import velo.radial.integration.MalilibIntegration;
import velo.radial.integration.MalilibIntegration.MalilibAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MalilibSelectionScreen extends Screen {

    private static final int ENTRY_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<MalilibAction> onSelect;
    private final Map<String, List<MalilibAction>> actionsByMod;

    private final List<Button> tabButtons = new ArrayList<>();
    private String currentTab = null;
    private List<MalilibAction> currentActions = new ArrayList<>();
    private List<MalilibAction> filteredActions = new ArrayList<>();
    private int scrollOffset = 0;
    private EditBox searchField;

    public MalilibSelectionScreen(Screen parent, Consumer<MalilibAction> onSelect) {
        super(Component.literal("Select Malilib Action"));
        this.parent = parent;
        this.onSelect = onSelect;

        List<MalilibAction> actions = MalilibIntegration.getAllActions();

        this.actionsByMod = actions.stream().collect(Collectors.groupingBy(
                MalilibAction::modName,
                TreeMap::new,
                Collectors.toList()
        ));

        if (!this.actionsByMod.isEmpty()) {
            this.setTab(this.actionsByMod.keySet().iterator().next());
        }
    }

    private void setTab(String tabName) {
        this.currentTab = tabName;
        this.currentActions = this.actionsByMod.getOrDefault(tabName, new ArrayList<>());

        if (this.searchField != null) {
            this.searchField.setValue("");
        }

        this.filteredActions = new ArrayList<>(this.currentActions);
        this.scrollOffset = 0;
    }

    private void updateSearch(String query) {
        String q = query.toLowerCase();
        this.filteredActions = this.currentActions.stream().filter(a ->
                a.name().toLowerCase().contains(q) || a.displayName().toLowerCase().contains(q)
        ).collect(Collectors.toList());
        this.scrollOffset = 0;
    }

    // --- Layout Helpers ---
    private int getListStartY() {
        return 65; // Pushed down slightly to accommodate tabs & search
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
        this.tabButtons.clear();

        if (actionsByMod.isEmpty()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.cancel"),
                    button -> onClose()
            ).bounds(width / 2 - 100, height - 28, 200, 20).build());
            return;
        }

        int tabWidth = Math.min(80, width / Math.max(1, actionsByMod.size()));
        int xOffset = (width - (tabWidth * actionsByMod.size())) / 2;

        for (String modName : actionsByMod.keySet()) {
            Button btn = Button.builder(
                    Component.literal(modName),
                    button -> {
                        setTab(modName);
                        updateTabButtonStates();
                    }
            ).bounds(xOffset, 10, tabWidth, 20).build();

            btn.active = !modName.equals(currentTab);
            this.tabButtons.add(btn);
            addRenderableWidget(btn);
            xOffset += tabWidth;
        }

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
        addRenderableWidget(this.searchField);

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(width / 2 - 100, height - 28, 200, 20).build());

        setInitialFocus(this.searchField);
    }

    private void updateTabButtonStates() {
        for (Button btn : this.tabButtons) {
            btn.active = !btn.getMessage().getString().equals(this.currentTab);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        if (actionsByMod.isEmpty()) {
            graphics.centeredText(font, "No Malilib mods found or no hotkeys available.", width / 2, height / 2, 0xFF5555);
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            return;
        }

        int startY = getListStartY();
        int listWidth = getListWidth();
        int left = getListLeft();
        int maxEntries = getMaxEntries();

        for (int i = 0; i < maxEntries; i++) {
            int index = i + scrollOffset;
            if (index >= filteredActions.size()) break;

            MalilibAction action = filteredActions.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            boolean hovered = mouseX >= left && mouseX <= left + listWidth && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            int bgColor = hovered ? 0x80FFFFFF : 0x40000000;
            graphics.fill(left, y, left + listWidth, y + ENTRY_HEIGHT - 2, bgColor);

            graphics.text(font, action.displayName(), left + 5, y + 5, 0xFFFFFFFF);

            int catWidth = font.width(action.category());
            graphics.text(font, action.category(), left + listWidth - catWidth - 5, y + 5, 0xFFAAAAAA);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (actionsByMod.isEmpty()) return super.mouseClicked(click, doubled);

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
                if (index < filteredActions.size()) {
                    onSelect.accept(filteredActions.get(index));
                    onClose();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredActions.size() - getMaxEntries());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}