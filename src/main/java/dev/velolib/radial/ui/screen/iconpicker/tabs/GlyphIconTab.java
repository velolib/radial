package dev.velolib.radial.ui.screen.iconpicker.tabs;

import dev.velolib.radial.ui.screen.iconpicker.GridIconTab;
import dev.velolib.radial.util.GlyphCache;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class GlyphIconTab extends GridIconTab<String> {

    public GlyphIconTab(Consumer<String> onSelect, Runnable onClose) {
        super(onSelect, onClose);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("screen.radial.editor.icon_picker.glyphs");
    }

    @Override
    protected int getSlotSize() {
        return 20;
    }

    @Override
    protected List<String> search(String query) {
        return GlyphCache.getGlyphs().stream()
                .filter(glyph -> glyph.toLowerCase().contains(query))
                .toList();
    }

    @Override
    protected void renderIcon(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, String glyph, boolean hovered) {
        Minecraft client = Minecraft.getInstance();
        int textWidth = client.font.width(glyph);
        int textX = x + (getSlotSize() - textWidth) / 2;
        int textY = y + (getSlotSize() - client.font.lineHeight) / 2;

        graphics.text(client.font, glyph, textX, textY, 0xFFFFFFFF);
    }

    @Override
    protected void selectIcon(String glyph) {
        onSelect.accept("radial:glyph." + glyph);
        onClose.run();
    }

    @Override
    protected Component getItemNarration(String glyph) {
        return Component.literal(glyph);
    }
}
