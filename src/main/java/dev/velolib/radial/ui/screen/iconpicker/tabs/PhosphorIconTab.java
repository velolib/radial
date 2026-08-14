package dev.velolib.radial.ui.screen.iconpicker.tabs;

import dev.velolib.radial.ui.screen.iconpicker.GridIconTab;
import dev.velolib.radial.util.PhosphorIconCache;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class PhosphorIconTab extends GridIconTab<PhosphorIconCache.PhosphorIcon> {

    private static final Identifier PHOSPHOR_FONT = Identifier.fromNamespaceAndPath("radial", "phosphor");

    public PhosphorIconTab(Consumer<String> onSelect, Runnable onClose) {
        super(onSelect, onClose);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("screen.radial.editor.icon_picker.phosphor");
    }

    @Override
    protected int getSlotSize() {
        return 20;
    }

    @Override
    protected List<PhosphorIconCache.PhosphorIcon> search(String query) {
        return PhosphorIconCache.getIcons().stream()
                .filter(icon -> query.isEmpty() || icon.searchText().contains(query))
                .toList();
    }

    @Override
    protected void renderIcon(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int mouseX,
            int mouseY,
            PhosphorIconCache.PhosphorIcon icon,
            boolean hovered) {
        Minecraft client = Minecraft.getInstance();
        Component component = Component.literal(icon.character())
                .setStyle(Style.EMPTY.withFont(new FontDescription.Resource(PHOSPHOR_FONT)));

        int textWidth = client.font.width(component);
        int textX = x + (getSlotSize() - textWidth) / 2;
        int textY = y + (getSlotSize() - client.font.lineHeight) / 2 + 5;

        graphics.text(client.font, component, textX, textY, 0xFFFFFFFF, false);

        if (hovered) {
            graphics.setTooltipForNextFrame(client.font, Component.literal(icon.name()), mouseX, mouseY);
        }
    }

    @Override
    protected void selectIcon(PhosphorIconCache.PhosphorIcon icon) {
        onSelect.accept("radial:icon." + icon.name());
        onClose.run();
    }

    @Override
    protected Component getItemNarration(PhosphorIconCache.PhosphorIcon icon) {
        return Component.literal(icon.name());
    }
}
