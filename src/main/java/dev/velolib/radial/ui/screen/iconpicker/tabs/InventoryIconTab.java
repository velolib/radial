package dev.velolib.radial.ui.screen.iconpicker.tabs;

import dev.velolib.radial.ui.screen.iconpicker.IconTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class InventoryIconTab implements IconTab {

    private static final Identifier INVENTORY_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    private static final int INV_WIDTH = 176;
    private static final int INV_HEIGHT = 166;
    private static final int INV_SLOT_SIZE = 18;

    private final Consumer<String> onSelect;
    private final Runnable onClose;
    private int screenWidth, screenHeight;

    public InventoryIconTab(Consumer<String> onSelect, Runnable onClose) {
        this.onSelect = onSelect;
        this.onClose = onClose;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("screen.radial.editor.icon_picker.inventory");
    }

    @Override
    public boolean showSearchBar() { return false; }

    @Override
    public void updateSearch(String query) { }

    @Override
    public void setup(int width, int height, Consumer<Renderable> addRenderable, Consumer<GuiEventListener> addWidget) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Inventory inventory = mc.player.getInventory();
        int bgX = screenWidth / 2 - INV_WIDTH / 2;
        int bgY = screenHeight / 2 - INV_HEIGHT / 2 + 10;

        Component infoText = Component.translatable("screen.radial.editor.icon_picker.inventory.info");
        graphics.text(mc.font, infoText, screenWidth / 2 - mc.font.width(infoText) / 2, bgY - 15, 0xFFAAAAAA);
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, bgX, bgY, 0, 0, INV_WIDTH, INV_HEIGHT, 256, 256, 0xFFFFFFFF);

        // Hotbar
        for (int i = 0; i < 9; i++) {
            drawInvSlot(graphics, mc, mouseX, mouseY, bgX + 7 + i * 18, bgY + 141, inventory.getNonEquipmentItems().get(i));
        }

        // Main Inventory
        for (int i = 0; i < 27; i++) {
            drawInvSlot(graphics, mc, mouseX, mouseY, bgX + 7 + (i % 9) * 18, bgY + 83 + (i / 9) * 18, inventory.getNonEquipmentItems().get(i + 9));
        }

        // Armor
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < armorSlots.length; i++) {
            drawInvSlot(graphics, mc, mouseX, mouseY, bgX + 7, bgY + 7 + i * 18, mc.player.getItemBySlot(armorSlots[i]));
        }

        // Offhand
        drawInvSlot(graphics, mc, mouseX, mouseY, bgX + 76, bgY + 61, inventory.player.getOffhandItem());
    }

    private void drawInvSlot(GuiGraphicsExtractor graphics, Minecraft mc, int mouseX, int mouseY, int x, int y, ItemStack stack) {
        if (!stack.isEmpty()) {
            graphics.fakeItem(stack, x + 1, y + 1);
        }
        if (isHovered(mouseX, mouseY, x, y)) {
            graphics.fill(x, y, x + INV_SLOT_SIZE, y + INV_SLOT_SIZE, 0x40FFFFFF);
            if (!stack.isEmpty()) {
                graphics.setTooltipForNextFrame(mc.font, stack, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bgX = screenWidth / 2 - INV_WIDTH / 2;
        int bgY = screenHeight / 2 - INV_HEIGHT / 2 + 10;

        // Hotbar check
        for (int i = 0; i < 9; i++) {
            if (isHovered(mouseX, mouseY, bgX + 7 + i * 18, bgY + 141)) {
                onSelect.accept("radial:slot.hotbar." + i);
                onClose.run();
                return true;
            }
        }

        // Main inventory check
        for (int i = 0; i < 27; i++) {
            if (isHovered(mouseX, mouseY, bgX + 7 + (i % 9) * 18, bgY + 83 + (i / 9) * 18)) {
                onSelect.accept("radial:slot.inventory." + i);
                onClose.run();
                return true;
            }
        }

        // Armor check
        String[] armorNames = {"head", "chest", "legs", "feet"};
        for (int i = 0; i < armorNames.length; i++) {
            if (isHovered(mouseX, mouseY, bgX + 7, bgY + 7 + i * 18)) {
                onSelect.accept("radial:slot.armor." + armorNames[i]);
                onClose.run();
                return true;
            }
        }

        // Offhand check
        if (isHovered(mouseX, mouseY, bgX + 76, bgY + 61)) {
            onSelect.accept("radial:slot.offhand");
            onClose.run();
            return true;
        }

        return false;
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + INV_SLOT_SIZE && mouseY >= y && mouseY < y + INV_SLOT_SIZE;
    }
}