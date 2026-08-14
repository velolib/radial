package dev.velolib.radial.ui.screen.iconpicker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public interface IconTab {
    Component getTitle();

    // Allows the tab to register its own list/widgets to the main screen
    void setup(int width, int height, Consumer<Renderable> addRenderable, Consumer<GuiEventListener> addWidget);

    // Render custom backgrounds or overlays (like the inventory tab does)
    void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    void updateSearch(String query);

    boolean showSearchBar();
}