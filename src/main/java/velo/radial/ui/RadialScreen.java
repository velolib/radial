package velo.radial.ui;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import velo.radial.RadialClient;
import velo.radial.config.RadialConfig;
import velo.radial.config.RadialSlot;
import velo.radial.config.SlotMode;

import java.util.List;

public class RadialScreen extends Screen {

    private static final Identifier SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final Identifier SELECTION_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/selection");
    private static final int SLOT_SIZE = 26;

    private final List<RadialSlot> rootSlots;
    private List<RadialSlot> activeSlots;

    private int currentSlotCount;
    private int hoveredSlot = -1;
    private boolean backHovered = false;

    private float animProgress = 0f;

    public RadialScreen() {
        super(Component.empty());
        this.rootSlots = RadialConfig.INSTANCE.slots;
        this.activeSlots = rootSlots;
        this.currentSlotCount = RadialConfig.INSTANCE.slotCount;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int keyCode = KeyMappingHelper.getBoundKeyOf(RadialClient.OPEN_RADIAL).getValue();
        long handle = Minecraft.getInstance().getWindow().handle();

// Check if the hotkey was just released
        if (GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_RELEASE) {

            if (RadialConfig.INSTANCE.activationMode == RadialConfig.ActivationMode.RELEASE) {

                if (hoveredSlot != -1 && hoveredSlot < activeSlots.size()) {
                    RadialSlot slot = activeSlots.get(hoveredSlot);

                    // FIX: Added `&& slot.mode != SlotMode.EMPTY`
                    if (slot.mode != SlotMode.SUBMENU && slot.mode != SlotMode.EMPTY) {
                        performAction(slot);
                        return;
                    }
                }
            }

            // Closes safely if hovering empty space, an empty slot, or a submenu
            onClose();
            return;
        }

        RadialConfig config = RadialConfig.INSTANCE;

        // Skip animation if speed is 0
        if (config.animationSpeedMs <= 0) {
            animProgress = 1.0f;
        } else {
            float speed = 50.0f / config.animationSpeedMs;
            if (animProgress < 1.0f) {
                animProgress = Math.min(1.0f, animProgress + (speed * delta));
            }
        }

        // 2. Performance: Replaced expensive Math.pow(..., 3) with simple multiplication
        float inverseProgress = 1.0f - animProgress;
        float ease = 1.0f - (inverseProgress * inverseProgress * inverseProgress);

        int cx = width / 2;
        int cy = height / 2;
        int count = currentSlotCount;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        int backX = cx - SLOT_SIZE / 2;
        int backY = cy - config.ringRadius - (SLOT_SIZE + 24);
        int hitboxPadding = 4;

        backHovered =
                activeSlots != rootSlots &&
                        mouseX >= backX - hitboxPadding &&
                        mouseX <= backX + SLOT_SIZE + hitboxPadding &&
                        mouseY >= backY - hitboxPadding &&
                        mouseY <= backY + SLOT_SIZE + hitboxPadding;

        if (!backHovered &&
                dist >= Math.max(0, config.ringRadius - config.innerPadding) &&
                dist <= config.ringRadius + config.outerReach) {

            double angle = Math.atan2(dy, dx) + Math.PI / 2;
            if (angle < 0) angle += Math.PI * 2;

            double sectorSize = (Math.PI * 2) / count;
            hoveredSlot = (int) ((angle + sectorSize / 2) / sectorSize) % count;
        } else {
            hoveredSlot = -1;
        }

        // 3. Bounds Check: Ensure hoveredSlot doesn't exceed the actual active slots size
        if (hoveredSlot >= activeSlots.size()) {
            hoveredSlot = -1;
        }

        if (activeSlots != rootSlots) {
            drawSlot(graphics, backX, backY, backHovered, ease, -1);
        }

        for (int i = 0; i < count; i++) {
            double slotAngle = (Math.PI * 2 / count) * i - Math.PI / 2;
            float radius = config.ringRadius * ease;

            float x = (float) (cx + Math.cos(slotAngle) * radius - SLOT_SIZE / 2f);
            float y = (float) (cy + Math.sin(slotAngle) * radius - SLOT_SIZE / 2f);

            drawSlot(graphics, x, y, i == hoveredSlot, ease, i);
        }

        if (hoveredSlot != -1) {
            String name = activeSlots.get(hoveredSlot).name;
            int alpha = (int) (ease * 255);

            graphics.text(
                    font,
                    Component.nullToEmpty(name),
                    cx - font.width(name) / 2,
                    cy - 4,
                    (alpha << 24) | 0xFFFFFF
            );
        } else if (backHovered) {
            Component backText = Component.translatable("radial.ui.back");
            graphics.text(
                    font,
                    backText,
                    cx - font.width(backText) / 2,
                    cy - 4,
                    0xFFFFFFFF
            );
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawSlot(
            GuiGraphicsExtractor graphics,
            float x,
            float y,
            boolean hovered,
            float ease,
            int index
    ) {
        int alpha = (int) (ease * 255);
        int color = (alpha << 24) | 0xFFFFFF;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SLOT_TEXTURE,
                0, 0,
                SLOT_SIZE,
                SLOT_SIZE,
                color
        );

        if (hovered) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SELECTION_TEXTURE,
                    0, 0,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    color
            );
        }

        if (index >= 0 && index < activeSlots.size()) {
            graphics.fakeItem(activeSlots.get(index).getRenderStack(), 5, 5);
        } else if (index == -1) {
            String icon = "✖";
            int xOff = (SLOT_SIZE - font.width(icon)) / 2;
            int yOff = (SLOT_SIZE - 8) / 2;
            graphics.text(font, icon, xOff, yOff, color);
        }

        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        // Doesn't matter which mode we are in, always execute when clicked.
        if (click.button() == 0) {
            if (backHovered) {
                goBack();
                return true;
            }

            if (hoveredSlot != -1 && hoveredSlot < activeSlots.size()) {
                performAction(activeSlots.get(hoveredSlot));
            } else {
                onClose();
            }
            return true;
        }

        if (click.button() == 1 && hoveredSlot != -1 && hoveredSlot < activeSlots.size()) {
            minecraft.setScreen(
                    new RadialSlotEditorScreen(
                            activeSlots.get(hoveredSlot),
                            activeSlots == rootSlots
                    )
            );
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private void performAction(RadialSlot slot) {
        if (slot.mode == SlotMode.EMPTY) return;

        if (slot.mode == SlotMode.SUBMENU && activeSlots == rootSlots) {
            activeSlots = slot.children;
            currentSlotCount = slot.childSlotCount;
            animProgress = 0f;
            return;
        }

        Minecraft client = Minecraft.getInstance();

        RadialClient.lockKey();
        onClose();

        // Execute *after* the screen is closed

        if (slot.mode == SlotMode.CHAT) {
            ClientPacketListener connection = client.getConnection();
            if (connection != null) {
                if (slot.value.startsWith("/")) {
                    connection.sendCommand(slot.value.substring(1));
                } else {
                    connection.sendChat(slot.value);
                }
            }
        } else if (slot.mode == SlotMode.KEYBIND) {
            for (KeyMapping key : client.options.keyMappings) {
                if (key.getName().equals(slot.value)) {
                    RadialClient.scheduleKeyPress(key);
                    break;
                }
            }
        } else if (slot.mode == SlotMode.MALILIB) {
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("malilib")) {
                velo.radial.integration.MalilibBridge.executeHotkey(slot.value);
            }
        }
    }

    private void goBack() {
        activeSlots = rootSlots;
        currentSlotCount = RadialConfig.INSTANCE.slotCount;
        animProgress = 0f;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}
}