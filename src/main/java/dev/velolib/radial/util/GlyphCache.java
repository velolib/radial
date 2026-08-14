package dev.velolib.radial.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.velolib.radial.RadialClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class GlyphCache {

    private static List<String> cachedGlyphs;

    private GlyphCache() {}

    public static void invalidate() {
        cachedGlyphs = null;
    }

    public static List<String> getGlyphs() {

        if (cachedGlyphs != null) {
            return cachedGlyphs;
        }

        cachedGlyphs = new ArrayList<>();

        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        Identifier targetFont = Identifier.fromNamespaceAndPath("minecraft", "font/include/default.json");

        Optional<Resource> resourceOpt = manager.getResource(targetFont);

        if (resourceOpt.isEmpty()) {

            targetFont = Identifier.fromNamespaceAndPath("minecraft", "font/default.json");

            resourceOpt = manager.getResource(targetFont);
        }

        if (resourceOpt.isPresent()) {

            try (Reader reader =
                    new BufferedReader(new InputStreamReader(resourceOpt.get().open(), StandardCharsets.UTF_8))) {

                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                JsonArray providers = json.getAsJsonArray("providers");

                for (int i = 0; i < providers.size(); i++) {

                    JsonObject provider = providers.get(i).getAsJsonObject();

                    if (!provider.has("type")
                            || !provider.get("type").getAsString().equals("bitmap")) {
                        continue;
                    }

                    JsonArray chars = provider.getAsJsonArray("chars");

                    for (int j = 0; j < chars.size(); j++) {

                        String row = chars.get(j).getAsString();

                        for (char c : row.toCharArray()) {

                            if (c != '\u0000' && c != ' ') {

                                String glyph = String.valueOf(c);

                                if (!cachedGlyphs.contains(glyph)) {

                                    cachedGlyphs.add(glyph);
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {

                RadialClient.LOGGER.error("Failed to parse dynamic glyphs.", e);
            }
        }

        if (cachedGlyphs.isEmpty()) {

            RadialClient.LOGGER.error("Glyph cache parsed empty, using fallback list.");

            cachedGlyphs.addAll(List.of(
                    "★", "☆", "♥", "♦", "♣", "♠", "☠", "☢", "☣", "⚠", "⚡", "↑", "↓", "←", "→", "↕", "↔", "⟳", "✖", "✔",
                    "⚙", "⌂", "✉", "☺", "☻", "☼", "♀", "♂", "♪", "♫", "►", "◄", "⛄", "⛏"));
        }

        cachedGlyphs = List.copyOf(cachedGlyphs);

        return cachedGlyphs;
    }
}
