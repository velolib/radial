package velo.radial.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import velo.radial.RadialClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GlyphCache {

    private static List<String> cachedGlyphs = null;

    private GlyphCache() {}

    public static List<String> getGlyphs() {
        if (cachedGlyphs != null) {
            return cachedGlyphs;
        }

        cachedGlyphs = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        // In modern MC, the actual bitmaps are stored in the include files
        Identifier targetFont = Identifier.fromNamespaceAndPath("minecraft", "font/include/default.json");

        Optional<Resource> resourceOpt = manager.getResource(targetFont);

        // Fallback for older versions if include/default.json doesn't exist
        if (resourceOpt.isEmpty()) {
            targetFont = Identifier.fromNamespaceAndPath("minecraft", "font/default.json");
            resourceOpt = manager.getResource(targetFont);
        }

        if (resourceOpt.isPresent()) {
            try (Reader reader = new BufferedReader(new InputStreamReader(resourceOpt.get().open()))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray providers = json.getAsJsonArray("providers");

                for (int i = 0; i < providers.size(); i++) {
                    JsonObject provider = providers.get(i).getAsJsonObject();

                    if (provider.has("type") && provider.get("type").getAsString().equals("bitmap")) {
                        JsonArray chars = provider.getAsJsonArray("chars");

                        for (int j = 0; j < chars.size(); j++) {
                            String row = chars.get(j).getAsString();

                            for (char c : row.toCharArray()) {
                                // Filter out empty space characters
                                if (c != '\u0000' && c != ' ') {
                                    String glyph = String.valueOf(c);
                                    if (!cachedGlyphs.contains(glyph)) { // Prevent duplicates
                                        cachedGlyphs.add(glyph);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                RadialClient.LOGGER.error("Failed to parse dynamic glyphs: {}", e.getMessage());
            }
        }

        // Safety Fallback: If parsing fails or yields nothing, provide a clean curated list
        if (cachedGlyphs.isEmpty()) {
            RadialClient.LOGGER.error("Glyph cache parsed empty, using fallback list.");
            cachedGlyphs.addAll(List.of(
                    "★", "☆", "♥", "♦", "♣", "♠", "☠", "☢", "☣", "⚠", "⚡",
                    "↑", "↓", "←", "→", "↕", "↔", "⟳", "✖", "✔", "⚙", "⌂", "✉",
                    "☺", "☻", "☼", "♀", "♂", "♪", "♫", "►", "◄", "⛄", "⛏"
            ));
        }

        return cachedGlyphs;
    }
}