package dev.velolib.radial.ui.screen.iconpicker.tabs;

import dev.velolib.radial.ui.screen.iconpicker.GridIconTab;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemIconTab extends GridIconTab<ItemIconTab.ItemSearchEntry> {

    private static List<ItemSearchEntry> ITEM_INDEX;
    private String lastQuery = "";
    private List<ItemSearchEntry> lastResults = new ArrayList<>();

    public ItemIconTab(Consumer<String> onSelect, Runnable onClose) {
        super(onSelect, onClose);
        ensureItemIndex();
    }

    @Override
    public Component getTitle() {
        return Component.translatable("screen.radial.editor.icon_picker.items");
    }

    @Override
    protected int getSlotSize() {
        return 20;
    }

    @Override
    protected List<ItemSearchEntry> search(String query) {
        if (query.isEmpty()) return ITEM_INDEX;

        List<ItemSearchEntry> source = (query.startsWith(lastQuery)) ? lastResults : ITEM_INDEX;
        List<ItemSearchEntry> results = new ArrayList<>();

        for (ItemSearchEntry entry : source) {
            if (entry.searchText().contains(query)) {
                results.add(entry);
            }
        }

        lastQuery = query;
        lastResults = results;
        return results;
    }

    @Override
    protected void renderIcon(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int mouseX,
            int mouseY,
            ItemSearchEntry item,
            boolean hovered) {
        graphics.fakeItem(item.stack(), x + 2, y + 2);
        if (hovered) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, item.stack(), mouseX, mouseY);
        }
    }

    @Override
    protected void selectIcon(ItemSearchEntry item) {
        onSelect.accept(item.id().toString());
        onClose.run();
    }

    @Override
    protected Component getItemNarration(ItemSearchEntry item) {
        return item.stack().getItemName();
    }

    private static void ensureItemIndex() {
        if (ITEM_INDEX != null) return;
        List<ItemSearchEntry> index = new ArrayList<>(BuiltInRegistries.ITEM.size());
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = item.getDefaultInstance();
            String name = stack.getItemName().getString();
            index.add(new ItemSearchEntry(item, stack, id, name, (id + " " + name).toLowerCase()));
        }
        ITEM_INDEX = List.copyOf(index);
    }

    public record ItemSearchEntry(Item item, ItemStack stack, Identifier id, String displayName, String searchText) {}
}
