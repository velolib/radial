package velo.radial.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public record RadialMenuEntry(Component name, Consumer<Screen> openAction) {
}
