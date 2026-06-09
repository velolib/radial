package velo.radial.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import velo.radial.api.RadialSlot;

import java.util.Objects;

public final class SlotRenderHelper {

    private SlotRenderHelper() {
        // Utility class, do not instantiate
    }

    /**
     * Consolidates dynamic item resolution (hotbar, armor, offhand, etc.)
     */
    public static ItemStack resolveDynamicItem(String itemId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return null;

        if (itemId != null && itemId.startsWith("radial:slot.")) {
            String[] parts = itemId.split("\\.");
            if (parts.length >= 2) {
                String type = parts[1];
                Inventory inv = minecraft.player.getInventory();

                try {
                    switch (type) {
                        case "hotbar":
                            if (parts.length >= 3) {
                                int hbIndex = Integer.parseInt(parts[2]);
                                if (hbIndex >= 0 && hbIndex < 9) {
                                    return inv.getNonEquipmentItems().get(hbIndex);
                                }
                            }
                            break;
                        case "inventory":
                            if (parts.length >= 3) {
                                int invIndex = Integer.parseInt(parts[2]);
                                if (invIndex >= 0 && invIndex < 27) {
                                    return inv.getNonEquipmentItems().get(invIndex + 9);
                                }
                            }
                            break;
                        case "armor":
                            if (parts.length >= 3) {
                                String armorSlot = parts[2];
                                switch (armorSlot) {
                                    case "head":
                                        return minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
                                    case "chest":
                                        return minecraft.player.getItemBySlot(EquipmentSlot.CHEST);
                                    case "legs":
                                        return minecraft.player.getItemBySlot(EquipmentSlot.LEGS);
                                    case "feet":
                                        return minecraft.player.getItemBySlot(EquipmentSlot.FEET);
                                }
                            }
                            break;
                        case "offhand":
                            return minecraft.player.getOffhandItem();
                    }
                } catch (NumberFormatException ignored) {
                    // Silently fail
                }
            }
            return ItemStack.EMPTY;
        }
        return null;
    }

    /**
     * Determines the correct stack to display (Dynamic vs sTATIC)
     */
    public static ItemStack getDisplayStack(RadialSlot slot) {
        ItemStack dynamic = resolveDynamicItem(slot.itemId);
        if (dynamic != null) return dynamic;

        return slot.getRenderStack();
    }

    /**
     * Handles the inner icon/item rendering for a slot.
     * Note: The X and Y coordinates should be the top-left of the 26x26 slot.
     */
    public static void renderSlotIcon(GuiGraphicsExtractor graphics, RadialSlot slot, float x, float y) {
        if (slot == null || slot.itemId == null || !slot.mode.shouldRenderIcon()) return;

        if (slot.itemId.startsWith("radial:glyph.")) {
            String glyph = slot.itemId.substring(13);

            Minecraft client = Minecraft.getInstance();
            int textWidth = client.font.width(glyph);

            // Use Math.round for precise horizontal centering of odd-width characters
            int centerX = (int) x + Math.round((26 - textWidth) / 2.0f);

            // Hardcode 8 instead of client.font.lineHeight (9) to account for the empty bottom descender pixel
            int centerY = (int) y + Math.round((26 - 8) / 2.0f);

            graphics.text(client.font, glyph, centerX, centerY, 0xFFFFFFFF);

        } else if (slot.itemId.startsWith("radial:effect.")) {
            String effectKey = slot.itemId.substring(14);
            Identifier id = Identifier.parse(effectKey);

            BuiltInRegistries.MOB_EFFECT.get(id).ifPresent(holder -> {
                MobEffect effect = holder.value();
                String path = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect)).getPath();
                Identifier spriteId = Identifier.fromNamespaceAndPath("minecraft", "mob_effect/" + path);

                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, (int) x + 4, (int) y + 4, 18, 18);
            });
        } else {
            ItemStack stack = getDisplayStack(slot);
            if (stack != null && !stack.isEmpty()) {
                graphics.fakeItem(stack, (int) x + 5, (int) y + 5);
            }
        }
    }
}