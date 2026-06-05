package velo.radial.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import velo.radial.RadialClient;

import java.awt.*;
import java.io.IOException;

public class ColorTypeAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter out, Color color) throws IOException {
        // Saves as #AARRGGBB
        out.value(String.format("#%08X", color.getRGB()));
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        String hex = in.nextString();
        try {
            // Remove the '#' if present
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            // Parse the hex string as a long to handle unsigned ARGB values correctly
            return new Color((int) Long.parseLong(cleanHex, 16), true);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            // Log the error to console so the user knows why their color reset
            RadialClient.LOGGER.error("[Radial] Failed to parse color '{}'. Falling back to transparent black.", hex);
            // Fallback: Return transparent black (0x00000000)
            return new Color(0, 0, 0, 0);
        }
    }
}