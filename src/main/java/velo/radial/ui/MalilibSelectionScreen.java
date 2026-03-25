package velo.radial.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import velo.radial.integration.MalilibBridge;
import velo.radial.integration.MalilibBridge.MalilibAction;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MalilibSelectionScreen extends Screen {

    private static final int ENTRY_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<MalilibAction> onSelect;
    private final Map<String, List<MalilibAction>> actionsByMod;

    private String currentTab = null;
    private List<MalilibAction> currentActions = new ArrayList<>();
    private List<MalilibAction> filteredActions = new ArrayList<>();
    private int scrollOffset = 0;
    
    private TextFieldWidget searchField;

    public MalilibSelectionScreen(Screen parent, Consumer<MalilibAction> onSelect) {
        super(Text.literal("Select Malilib Action"));
        this.parent = parent;
        this.onSelect = onSelect;
        
        List<MalilibAction> actions = MalilibBridge.getAllActions();
        this.actionsByMod = actions.stream().collect(Collectors.groupingBy(a -> a.modName));

        if (!this.actionsByMod.isEmpty()) {
            this.setTab(this.actionsByMod.keySet().iterator().next());
        }
    }

    private void setTab(String tabName) {
        this.currentTab = tabName;
        this.currentActions = this.actionsByMod.getOrDefault(tabName, new ArrayList<>());
        
        if (this.searchField != null) {
            this.searchField.setText("");
        }
        
        this.filteredActions = new ArrayList<>(this.currentActions);
        this.scrollOffset = 0;
    }

    private void updateSearch(String query) {
        String q = query.toLowerCase();
        this.filteredActions = this.currentActions.stream().filter(a -> 
            a.name.toLowerCase().contains(q) || a.displayName.toLowerCase().contains(q)
        ).collect(Collectors.toList());
        this.scrollOffset = 0;
    }

    @Override
    protected void init() {
        if (actionsByMod.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.cancel"),
                    button -> close()
            ).dimensions(width / 2 - 100, height - 28, 200, 20).build());
            return;
        }

        int tabWidth = Math.min(80, width / Math.max(1, actionsByMod.size()));
        int xOffset = (width - (tabWidth * actionsByMod.size())) / 2;

        for (String modName : actionsByMod.keySet()) {
            boolean isSelected = modName.equals(currentTab);
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal(modName),
                    button -> {
                        setTab(modName);
                        this.clearChildren();
                        this.init(); // Rebuild buttons to update their 'active/disabled' state
                    }
            ).dimensions(xOffset, 10, tabWidth, 20).build();

            if (isSelected) {
                btn.active = false;
            }

            addDrawableChild(btn);
            xOffset += tabWidth;
        }

        int searchWidth = Math.min(350, (int) (width * 0.9));
        this.searchField = new TextFieldWidget(
                textRenderer,
                width / 2 - searchWidth / 2,
                35,
                searchWidth,
                20,
                Text.literal("Search...")
        );
        this.searchField.setPlaceholder(Text.literal("Search actions..."));
        this.searchField.setChangedListener(this::updateSearch);
        addDrawableChild(this.searchField);

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                button -> close()
        ).dimensions(width / 2 - 100, height - 28, 200, 20).build());
        
        setInitialFocus(this.searchField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        if (actionsByMod.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No Malilib mods found or no hotkeys available.", width / 2, height / 2, 0xFF5555);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int startY = 60;
        int listWidth = Math.min(350, (int) (width * 0.9));
        int left = width / 2 - listWidth / 2;

        int maxEntries = Math.max(1, (height - startY - 40) / ENTRY_HEIGHT);

        for (int i = 0; i < maxEntries; i++) {
            int index = i + scrollOffset;
            if (index >= filteredActions.size()) break;

            MalilibAction action = filteredActions.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            boolean hovered = mouseX >= left && mouseX <= left + listWidth && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            int bgColor = hovered ? 0x80FFFFFF : 0x40000000;
            context.fill(left, y, left + listWidth, y + ENTRY_HEIGHT - 2, bgColor);

            context.drawTextWithShadow(textRenderer, action.displayName, left + 5, y + 5, 0xFFFFFFFF);

            int catWidth = textRenderer.getWidth(action.category);
            context.drawTextWithShadow(textRenderer, action.category, left + listWidth - catWidth - 5, y + 5, 0xFFAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (actionsByMod.isEmpty()) return super.mouseClicked(click, doubled);

        int startY = 60;
        int listWidth = Math.min(350, (int) (width * 0.9));
        int left = width / 2 - listWidth / 2;

        double mx = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double my = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        int maxEntries = (height - startY - 40) / ENTRY_HEIGHT;

        for (int i = 0; i < maxEntries; i++) {
            int y = startY + i * ENTRY_HEIGHT;

            if (mx >= left && mx <= left + listWidth && my >= y && my < y + ENTRY_HEIGHT) {
                int index = i + scrollOffset;
                if (index < filteredActions.size()) {
                    onSelect.accept(filteredActions.get(index));
                    close();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxEntries = (height - 60 - 40) / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, filteredActions.size() - maxEntries);

        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        return true;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
