package dev.velolib.radial.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class EncoderUtils {

    /**
     * Converts an ItemStack into a valid /give command string.
     *
     * @param stack      The ItemStack to convert.
     * @param registries The registry access (e.g., from server.registryAccess()).
     * @return A string formatted for the /give command.
     */
    @SuppressWarnings("unchecked")
    public static String toGiveCommandString(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "minecraft:air";
        }

        // 1. Get the base Item ID
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        StringBuilder command = new StringBuilder(itemId.toString());

        // 2. Fetch the patch containing all modifications made to this specific item vs its default state
        DataComponentPatch patch = stack.getComponentsPatch();

        // 3. Serialize the component patch if any modifications exist
        if (!patch.isEmpty()) {
            command.append("[");

            // Create a registry-aware NBT ops context
            RegistryOps<Tag> registryOps = registries.createSerializationContext(NbtOps.INSTANCE);
            boolean first = true;

            for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
                DataComponentType<?> type = entry.getKey();
                Identifier typeId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
                Optional<?> value = entry.getValue();

                if (value.isPresent()) {
                    // Added or modified component
                    Codec<Object> codec = (Codec<Object>) type.codec();

                    // Transient components lacking a codec cannot be expressed in a command
                    if (codec != null) {
                        if (!first) command.append(",");
                        first = false;

                        command.append(Objects.requireNonNull(typeId)).append("=");

                        // Encode the component object back to an NBT Tag
                        Tag tag = codec.encodeStart(registryOps, value.get()).getOrThrow();

                        // Tag#toString natively produces a compliant SNBT string in 1.21
                        command.append(tag);
                    }
                } else {
                    // A default component that was explicitly removed is prefixed with an exclamation mark
                    if (!first) command.append(",");
                    first = false;

                    command.append("!").append(Objects.requireNonNull(typeId).toString());
                }
            }
            command.append("]");
        }

        return command.toString();
    }
}