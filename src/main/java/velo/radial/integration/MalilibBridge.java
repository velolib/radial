package velo.radial.integration;

import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;

import java.util.ArrayList;
import java.util.List;

public class MalilibBridge {

    public record MalilibAction(String modName, String category, String name, String id, String displayName) {}

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

        try {
            for (KeybindCategory cat : InputEventHandler.getKeybindManager().getKeybindCategories()) {
                String mod = cat.getModName();
                String category = cat.getCategory();

                for (IHotkey hk : cat.getHotkeys()) {
                    // Reconstruct the ID instead of splitting the input string
                    String currentId = mod + ":" + category + ":" + hk.getName();

                    if (currentId.equals(hotkeyId)) {
                        // Cast is required: IKeybind doesn't store the callback, KeybindMulti does
                        if (hk.getKeybind() instanceof fi.dy.masa.malilib.hotkeys.KeybindMulti multi) {
                            IHotkeyCallback callback = multi.getCallback();
                            if (callback != null) {
                                callback.onKeyAction(KeyAction.PRESS, multi);
                            }
                        }
                        return; // Found and executed, exit early
                    }
                }
            }
        } catch (Exception e) {
            velo.radial.RadialClient.LOGGER.error("Failed to execute Malilib hotkey", e);
        }
    }
}