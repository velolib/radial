package velo.radial.config;

import net.minecraft.text.Text;

public enum SlotMode {
    EMPTY, CHAT, KEYBIND, SUBMENU, MALILIB;

    public Text getTranslatedName() {
        if (this == MALILIB) {
            return Text.literal("Malilib");
        }
        return Text.translatable("radial.mode." + this.name().toLowerCase());
    }
}