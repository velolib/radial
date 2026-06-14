package velo.radial.ui.screen;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import velo.radial.RadialClient;
import velo.radial.api.RadialSlot;
import velo.radial.api.SlotActionContext;
import velo.radial.config.RadialConfig;
import velo.radial.render.DonutRenderer;
import velo.radial.render.SlotRenderHelper;

import java.util.List;

/**
 * The in-game screen that opens when holding the Radial key.
 */
public class RadialScreen extends Screen {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final Identifier SELECTION_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/selection");
    private static final int SLOT_SIZE = 26;
    private final DonutRenderer donutRenderer = new DonutRenderer("main");
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

        if (GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_RELEASE) {
            if (RadialConfig.INSTANCE.activationMode == RadialConfig.ActivationMode.RELEASE) {
                if (hoveredSlot != -1 && hoveredSlot < activeSlots.size()) {
                    RadialSlot slot = activeSlots.get(hoveredSlot);
                    if (slot.mode.activateOnRelease()) {
                        performAction(slot);
                        return;
                    }
                }
            }
            onClose();
            return;
        }

        RadialConfig config = RadialConfig.INSTANCE;

        if (config.animationSpeedMs <= 0) {
            animProgress = 1.0f;
        } else {
            float speed = 50.0f / config.animationSpeedMs;
            if (animProgress < 1.0f) {
                animProgress = Math.min(1.0f, animProgress + (speed * delta));
            }
        }

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

        float innerRadiusLimit = Math.max(0, config.ringRadius - config.innerPadding);
        float outerRadiusLimit = config.ringRadius + config.outerReach;

        if (!backHovered && dist >= innerRadiusLimit && dist <= outerRadiusLimit) {
            double angle = Math.atan2(dy, dx) + Math.PI / 2;
            if (angle < 0) angle += Math.PI * 2;
            double sectorSize = (Math.PI * 2) / count;
            hoveredSlot = (int) ((angle + sectorSize / 2) / sectorSize) % count;
        } else {
            hoveredSlot = -1;
        }

        if (hoveredSlot >= activeSlots.size()) {
            hoveredSlot = -1;
        }

        // DRAW THE BACKGROUND DONUT
        if (RadialConfig.INSTANCE.showActivationZone && currentSlotCount > 0) {
            float inner = Math.max(0, config.ringRadius - config.innerPadding);
            float outer = config.ringRadius + config.outerReach;

            // Draw standard donut with animation ease, dynamic hover slot, and 2.0 supersampling
            donutRenderer.render(graphics, cx, cy, inner, outer, currentSlotCount, hoveredSlot, ease, 2.0f);
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
            graphics.text(font, Component.nullToEmpty(name), cx - font.width(name) / 2, cy - 4, (alpha << 24) | 0xFFFFFF);
        } else if (backHovered) {
            Component backText = Component.translatable("radial.ui.back");
            graphics.text(font, backText, cx - font.width(backText) / 2, cy - 4, 0xFFFFFFFF);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, float x, float y, boolean hovered, float ease, int index) {
        int alpha = (int) (ease * 255);
        int color = (alpha << 24) | 0xFFFFFF;

        graphics.pose().pushMatrix();

        graphics.pose().translate(x + SLOT_SIZE / 2.0f, y + SLOT_SIZE / 2.0f);

        graphics.pose().scale(ease, ease);

        graphics.pose().translate(-SLOT_SIZE / 2.0f, -SLOT_SIZE / 2.0f);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, 0, 0, SLOT_SIZE, SLOT_SIZE, color);

        if (hovered) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION_TEXTURE, 0, 0, SLOT_SIZE, SLOT_SIZE, color);
        }

        if (index >= 0 && index < activeSlots.size()) {
            RadialSlot slot = activeSlots.get(index);
            SlotRenderHelper.renderSlotIcon(graphics, slot, 0, 0);

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
            minecraft.setScreen(new SlotEditorScreen(activeSlots.get(hoveredSlot), activeSlots == rootSlots));
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private void performAction(RadialSlot slot) {
        slot.mode.performAction(slot, new SlotActionContext() {
            @Override
            public void closeScreen() {
                RadialClient.lockKey();
                RadialScreen.this.onClose();
            }

            @Override
            public void openSubmenu(List<RadialSlot> children, int slotCount) {
                if (activeSlots == rootSlots) {
                    activeSlots = children;
                    currentSlotCount = slotCount;
                    animProgress = 0f;
                }
            }

            @Override
            public boolean isRoot() {
                return activeSlots == rootSlots;
            }
        });
    }

    private void goBack() {
        activeSlots = rootSlots;
        currentSlotCount = RadialConfig.INSTANCE.slotCount;
        animProgress = 0f;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void removed() {
        super.removed();
        donutRenderer.close();
    }
}