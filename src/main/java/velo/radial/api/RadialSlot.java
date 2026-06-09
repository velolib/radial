package velo.radial.api;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class RadialSlot {

    public String name;
    public SlotMode mode;
    public String value;
    public String itemId;

    // Submenu configuration
    public List<RadialSlot> children = new ArrayList<>();
    public int childSlotCount = 8;

    private transient ItemStack cachedStack;

    public RadialSlot(String name, SlotMode mode, String value, String itemId) {
        this.name = name;
        this.mode = mode;
        this.value = value;
        this.itemId = itemId;
    }

    public void clearCache() {
        cachedStack = null;
    }

    public ItemStack getRenderStack() {
        if (cachedStack != null) {
            return cachedStack;
        }

        try {
            Minecraft client = Minecraft.getInstance();

            if (client.level == null) {
                cachedStack = new ItemStack(Items.AIR);
                return cachedStack;
            }

            HolderLookup.Provider registryLookup =
                    client.level.registryAccess();

            ItemInput result =
                    new ItemParser(registryLookup)
                            .parse(new StringReader(itemId));

            cachedStack = new ItemStack(result.item(), 1);
            cachedStack.applyComponentsAndValidate(result.components());
        } catch (Exception e) {
            cachedStack = new ItemStack(Items.BARRIER);
            cachedStack.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal("Invalid Item ID")
            );
        }

        return cachedStack;
    }
}
