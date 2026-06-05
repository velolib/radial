package velo.radial.config;

import net.minecraft.network.chat.Component;

public enum SlotMode {
    EMPTY, CHAT, KEYBIND, MALILIB, SUBMENU,
    ;

    public Component getTranslatedName() {
        return Component.translatable("radial.mode." + this.name().toLowerCase());
    }
}