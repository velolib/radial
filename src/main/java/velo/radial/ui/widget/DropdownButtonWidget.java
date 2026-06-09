package velo.radial.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DropdownButtonWidget<T> extends AbstractWidget {
    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath("minecraft", "widget/text_field");
    private static final Identifier SPRITE_HIGHLIGHTED = Identifier.fromNamespaceAndPath("minecraft", "widget/text_field_highlighted");
    private final List<T> options;
    private final Function<T, Component> labelMapper;
    private final Consumer<T> onSelect;
    private final Consumer<DropdownMenuWidget<T>> menuRegistrar;
    private T selectedOption;
    private DropdownMenuWidget<T> activeMenu = null;

    public DropdownButtonWidget(int x, int y, int width, int height,
                                List<T> options, T initialSelection,
                                Function<T, Component> labelMapper, Consumer<T> onSelect,
                                Consumer<DropdownMenuWidget<T>> menuRegistrar) {
        super(x, y, width, height, labelMapper.apply(initialSelection));
        this.options = options;
        this.selectedOption = initialSelection;
        this.labelMapper = labelMapper;
        this.onSelect = onSelect;
        this.menuRegistrar = menuRegistrar;
    }

    public DropdownMenuWidget<T> getActiveMenu() {
        return this.activeMenu;
    }

    public boolean isMenuOpen() {
        return this.activeMenu != null;
    }

    public void closeMenu() {
        this.activeMenu = null;
    }

    public void updateSelection(T option) {
        this.selectedOption = option;
        this.setMessage(this.labelMapper.apply(option));
        this.onSelect.accept(option);
        this.closeMenu();
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.isActive() && this.isMouseOver(event.x(), event.y())) {
            this.onClick(event, doubleClick);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        // PLAY SOUND: Triggers the standard click noise when toggling the menu
        this.playDownSound(Minecraft.getInstance().getSoundManager());

        if (isMenuOpen()) {
            closeMenu();
        } else {
            this.activeMenu = new DropdownMenuWidget<>(
                    getX(), getY() + getHeight(), this.width, this.height,
                    this.options, this.selectedOption, this.labelMapper, this
            );
            this.menuRegistrar.accept(this.activeMenu);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        boolean open = isMenuOpen();
        Identifier currentSprite = (this.isFocused() || open) ? SPRITE_HIGHLIGHTED : SPRITE;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, currentSprite, getX(), getY(), width, height);

        int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        Component currentText = this.labelMapper.apply(this.selectedOption);

        graphics.text(font, currentText, getX() + 4, getY() + (getHeight() - 8) / 2, textColor);
        graphics.text(font, Component.literal(open ? "▲" : "▼"), getX() + width - 12, getY() + (getHeight() - 8) / 2, textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}