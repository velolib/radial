package dev.velolib.radial.ui.screen.iconpicker;

import dev.velolib.radial.ui.screen.iconpicker.tabs.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IconPickerScreen extends Screen {
    private final Screen parent;

    private final List<IconTab> tabs = new ArrayList<>();

    private IconTab currentTab;

    public IconPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.literal("Icon Selector"));
        this.parent = parent;

        // Register Tabs
        tabs.add(new ItemIconTab(onSelect, this::onClose));
        tabs.add(new InventoryIconTab(onSelect, this::onClose));
        tabs.add(new EffectIconTab(onSelect, this::onClose));
        tabs.add(new PhosphorIconTab(onSelect, this::onClose));
        tabs.add(new GlyphIconTab(onSelect, this::onClose));

        this.currentTab = tabs.getFirst();
    }

    @Override
    protected void init() {
        clearWidgets();

        int tabWidth = Math.min(80, width / Math.max(1, tabs.size()));
        int xOffset = (width - tabWidth * tabs.size()) / 2;

        for (IconTab tab : tabs) {
            Button button = Button.builder(tab.getTitle(), _ -> setTab(tab))
                    .bounds(xOffset, 10, tabWidth, 20).build();

            button.active = (tab != currentTab);
            addRenderableWidget(button);

            xOffset += tabWidth;
        }

        int listWidth = Math.min(350, (int) (width * 0.9));
        EditBox searchField = new EditBox(font, width / 2 - listWidth / 2, 35, listWidth, 20, Component.translatable("screen.radial.editor.search"));
        searchField.setResponder(query -> {
            if (currentTab != null) currentTab.updateSearch(query);
        });

        addRenderableWidget(searchField);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());

        currentTab.setup(width, height,
                renderable -> addRenderableWidget((AbstractWidget) renderable),
                listener -> {
                    if (!this.children().contains(listener)) {
                        addWidget((AbstractWidget) listener);
                    }
                }
        );

        searchField.visible = currentTab.showSearchBar();
        searchField.setValue("");
        setInitialFocus(searchField);
    }

    private void setTab(IconTab tab) {
        this.currentTab = tab;
        this.rebuildWidgets(); // Triggers init() again to cleanly swap widgets
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        // Let the tab render its background/custom UI
        currentTab.render(graphics, mouseX, mouseY, delta);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (currentTab.mouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}