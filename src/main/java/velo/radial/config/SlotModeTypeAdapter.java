package velo.radial.config;

import com.google.gson.*;
import net.minecraft.resources.Identifier;
import velo.radial.api.RadialSlotModes;
import velo.radial.api.SlotMode;

import java.lang.reflect.Type;
import java.util.Map;

public class SlotModeTypeAdapter implements JsonSerializer<SlotMode>, JsonDeserializer<SlotMode> {

    @Override
    public SlotMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String idString = json.getAsString();

        // --- ON-THE-FLY MIGRATION ---
        // If the string doesn't have a colon (e.g., "EMPTY", "KEYBIND"), it's from v1.
        // We automatically convert it to the v2 namespaced format ("radial:empty").
        if (!idString.contains(":")) {
            idString = "radial:" + idString.toLowerCase();
        }

        // Parse the identifier safely
        Identifier id = Identifier.tryParse(idString);
        if (id != null) {
            SlotMode mode = RadialSlotModes.getRegisteredModes().get(id);
            if (mode != null) {
                return mode;
            }
        }

        // If the mode doesn't exist (e.g., they uninstalled an addon mod), fallback safely
        return RadialSlotModes.getDefaultMode();
    }

    @Override
    public JsonElement serialize(SlotMode src, Type typeOfSrc, JsonSerializationContext context) {
        // Reverse lookup: Find the Identifier for the given SlotMode instance
        for (Map.Entry<Identifier, SlotMode> entry : RadialSlotModes.getRegisteredModes().entrySet()) {
            // We check by class type to ensure we match the right mode safely
            if (entry.getValue().getClass() == src.getClass()) {
                return new JsonPrimitive(entry.getKey().toString());
            }
        }

        // Fallback if something went catastrophically wrong
        return new JsonPrimitive("radial:empty");
    }
}