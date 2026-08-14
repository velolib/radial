package dev.velolib.radial.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.velolib.radial.RadialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PhosphorIconCache {

    private static final Gson GSON = new Gson();

    private static final Type ICON_LIST_TYPE = new TypeToken<List<PhosphorIcon>>() {
    }.getType();

    private static List<PhosphorIcon> cachedIcons;

    private PhosphorIconCache() {
    }

    public static List<PhosphorIcon> getIcons() {

        if (cachedIcons != null) {
            return cachedIcons;
        }

        cachedIcons = new ArrayList<>();

        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        var identifier = net.minecraft.resources.Identifier.fromNamespaceAndPath("radial", "phosphor/icons.json");

        Optional<Resource> resource = manager.getResource(identifier);

        if (resource.isEmpty()) {

            RadialClient.LOGGER.error("Phosphor icon catalog not found: {}", identifier);

            return cachedIcons;
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {

            List<PhosphorIcon> icons = GSON.fromJson(reader, ICON_LIST_TYPE);

            if (icons != null) {
                cachedIcons = List.copyOf(icons);
            }

        } catch (Exception e) {

            RadialClient.LOGGER.error("Failed to load Phosphor icon catalog.", e);
        }

        return cachedIcons;
    }

    public record PhosphorIcon(String name, int codepoint, List<String> tags) {

        public String searchText() {

            if (tags == null || tags.isEmpty()) {

                return name.toLowerCase();
            }

            return (name + " " + String.join(" ", tags)).toLowerCase();
        }

        public String character() {

            return new String(Character.toChars(codepoint));
        }
    }
}