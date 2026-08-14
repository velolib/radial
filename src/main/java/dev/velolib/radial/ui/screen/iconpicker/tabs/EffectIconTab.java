package dev.velolib.radial.ui.screen.iconpicker.tabs;

import dev.velolib.radial.ui.screen.iconpicker.GridIconTab;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public class EffectIconTab extends GridIconTab<MobEffect> {

    public EffectIconTab(Consumer<String> onSelect, Runnable onClose) {
        super(onSelect, onClose);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("screen.radial.editor.icon_picker.effects");
    }

    @Override
    protected int getSlotSize() {
        return 20;
    }

    @Override
    protected List<MobEffect> search(String query) {
        return BuiltInRegistries.MOB_EFFECT.stream()
                .filter(effect -> {
                    String name = Component.translatable(effect.getDescriptionId())
                            .getString()
                            .toLowerCase();
                    String id = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect))
                            .toString()
                            .toLowerCase();
                    return name.contains(query) || id.contains(query);
                })
                .toList();
    }

    @Override
    protected void renderIcon(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, MobEffect effect, boolean hovered) {
        String path = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect))
                .getPath();
        Identifier spriteId = Identifier.fromNamespaceAndPath("minecraft", "mob_effect/" + path);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, x, y, getSlotSize(), getSlotSize());

        if (hovered) {
            graphics.setTooltipForNextFrame(
                    Minecraft.getInstance().font, Component.translatable(effect.getDescriptionId()), mouseX, mouseY);
        }
    }

    @Override
    protected void selectIcon(MobEffect effect) {
        String effectId = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect))
                .toString();
        onSelect.accept("radial:effect." + effectId);
        onClose.run();
    }

    @Override
    protected Component getItemNarration(MobEffect effect) {
        return Component.translatable(effect.getDescriptionId());
    }
}
