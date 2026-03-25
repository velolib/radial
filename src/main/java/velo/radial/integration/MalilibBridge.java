package velo.radial.integration;

import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;

import java.util.ArrayList;
import java.util.List;

public class MalilibBridge {

    public static class MalilibAction {
        public final String modName;
        public final String category;
        public final String name;
        public final String id;
        public final String displayName;

        public MalilibAction(String modName, String category, String name, String id, String displayName) {
            this.modName = modName;
            this.category = category;
            this.name = name;
            this.id = id;
            this.displayName = displayName;
        }
    }

    public static void init() {
        velo.radial.RadialClient.LOGGER.info("[Radial] Malilib logic active.");
    }

    public static List<MalilibAction> getAllActions() {
        List<MalilibAction> list = new ArrayList<>();
        try {
            for (KeybindCategory cat : InputEventHandler.getKeybindManager().getKeybindCategories()) {
                String mod = cat.getModName();
                String category = cat.getCategory();
                for (IHotkey hk : cat.getHotkeys()) {
                    String id = mod + ":" + category + ":" + hk.getName();
                    list.add(new MalilibAction(mod, category, hk.getName(), id, hk.getPrettyName()));
                }
            }
        } catch (Exception e) {
            velo.radial.RadialClient.LOGGER.error("Failed to fetch Malilib hotkeys", e);
        }
        return list;
    }

    public static void executeHotkey(String hotkeyId) {
        if (hotkeyId == null || hotkeyId.isEmpty()) return;
        String[] parts = hotkeyId.split(":", 3);
        if (parts.length < 3) return;
        
        String modName = parts[0];
        String catName = parts[1];
        String name = parts[2];
        
        try {
            for (KeybindCategory cat : InputEventHandler.getKeybindManager().getKeybindCategories()) {
                if (cat.getModName().equals(modName) && cat.getCategory().equals(catName)) {
                    for (IHotkey hk : cat.getHotkeys()) {
                        if (hk.getName().equals(name)) {
                            if (hk.getKeybind() instanceof fi.dy.masa.malilib.hotkeys.KeybindMulti multi) {
                                IHotkeyCallback callback = multi.getCallback();
                                if (callback != null) {
                                    callback.onKeyAction(KeyAction.PRESS, multi);
                                }
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            velo.radial.RadialClient.LOGGER.error("Failed to execute Malilib hotkey", e);
        }
    }
}
