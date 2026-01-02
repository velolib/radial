package velo.radial.config;

import net.minecraft.text.Text;

public enum SlotMode {
    EMPTY, CHAT, KEYBIND, SUBMENU;

    public Text getTranslatedName() {
        return Text.translatable("radial.mode." + this.name().toLowerCase());
    }
}