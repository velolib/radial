package dev.velolib.radial.ui.screen.iconpicker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public abstract class GridIconTab<T> implements IconTab {
    protected final Consumer<String> onSelect;
    protected final Runnable onClose;

    private IconGridList listWidget;
    private List<T> currentResults = new ArrayList<>();

    public GridIconTab(Consumer<String> onSelect, Runnable onClose) {
        this.onSelect = onSelect;
        this.onClose = onClose;
    }

    protected abstract int getSlotSize();

    protected abstract List<T> search(String query);

    protected abstract void renderIcon(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, T item, boolean hovered);

    protected abstract void selectIcon(T item);
    // Renamed to avoid clashing with the Entry's getNarration() method
    protected abstract Component getItemNarration(T item);

    @Override
    public void setup(int width, int height, Consumer<Renderable> addRenderable, Consumer<GuiEventListener> addWidget) {
        int listWidth = Math.min(350, (int) (width * 0.9));
        int top = 65;
        int bottom = height - 40;
        int left = width / 2 - listWidth / 2;

        listWidget = new IconGridList(Minecraft.getInstance(), listWidth, Math.max(1, bottom - top), top, 24);
        listWidget.updateSizeAndPosition(listWidth, Math.max(1, bottom - top), left, top);

        addRenderable.accept(listWidget);
        addWidget.accept(listWidget);
        updateSearch("");
    }

    @Override
    public void updateSearch(String query) {
        currentResults = search(query.trim().toLowerCase());
        rebuildRows();
    }

    private void rebuildRows() {
        if (listWidget == null) return;

        int usableWidth = Math.max(1, listWidget.getRowWidth() - 20);
        int columns = Math.max(1, usableWidth / getSlotSize());

        List<IconGridEntry> rows = new ArrayList<>();
        for (int start = 0; start < currentResults.size(); start += columns) {
            int end = Math.min(start + columns, currentResults.size());
            rows.add(new IconGridEntry(new ArrayList<>(currentResults.subList(start, end))));
        }

        listWidget.replaceEntries(rows);
        listWidget.setScrollAmount(0.0);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        /* Managed by listWidget */
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false; /* Managed by listWidget */
    }

    @Override
    public boolean showSearchBar() {
        return true;
    }

    private class IconGridList extends ObjectSelectionList<IconGridEntry> {
        public IconGridList(Minecraft mc, int w, int h, int y, int rowHeight) {
            super(mc, w, h, y, rowHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.min(330, getWidth() - 20);
        }
    }

    private class IconGridEntry extends ObjectSelectionList.Entry<IconGridEntry> {
        private final List<T> items;

        private IconGridEntry(List<T> items) {
            this.items = items;
        }

        @Override
        public void extractContent(
                @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int left = getContentX();
            int top = getContentY();
            int slotSize = getSlotSize();
            int verticalOffset = Math.max(0, (24 - slotSize) / 2);

            for (int i = 0; i < items.size(); i++) {
                int x = left + i * slotSize;
                int y = top + verticalOffset;
                boolean slotHovered = mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize;

                if (slotHovered) {
                    graphics.fill(x, y, x + slotSize, y + slotSize, 0x40FFFFFF);
                }

                renderIcon(graphics, x, y, mouseX, mouseY, items.get(i), slotHovered);
            }
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0) return false;
            int slotSize = getSlotSize();
            int verticalOffset = Math.max(0, (24 - slotSize) / 2);

            for (int i = 0; i < items.size(); i++) {
                int x = getContentX() + i * slotSize;
                int y = getContentY() + verticalOffset;

                if (event.x() >= x && event.x() < x + slotSize && event.y() >= y && event.y() < y + slotSize) {
                    selectIcon(items.get(i));
                    return true;
                }
            }
            return false;
        }

        @Override
        public @NonNull Component getNarration() {
            return items.isEmpty() ? Component.literal("Empty row") : getItemNarration(items.getFirst());
        }
    }
}
