package velo.radial.config;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;

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
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.world == null) {
                cachedStack = new ItemStack(Items.AIR);
                return cachedStack;
            }

            RegistryWrapper.WrapperLookup registryLookup =
                    client.world.getRegistryManager();

            ItemStringReader.ItemResult result =
                    new ItemStringReader(registryLookup)
                            .consume(new StringReader(itemId));

            cachedStack = new ItemStack(result.item(), 1);
            cachedStack.applyChanges(result.components());
        } catch (Exception e) {
            cachedStack = new ItemStack(Items.BARRIER);
            cachedStack.set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Invalid Item ID")
            );
        }

        return cachedStack;
    }
}
