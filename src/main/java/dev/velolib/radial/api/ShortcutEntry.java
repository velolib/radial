package dev.velolib.radial.api;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public record ShortcutEntry(Component name, Consumer<Screen> openAction) {}
