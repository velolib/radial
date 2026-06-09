package velo.radial.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import velo.radial.RadialClient;
import velo.radial.api.RadialScreenContext;
import velo.radial.api.RadialSlot;
import velo.radial.config.RadialConfig;

import java.util.List;

/**
 * The in-game screen that opens when holding the Radial key.
 */
public class RadialScreen extends Screen {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final Identifier SELECTION_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/selection");
    private static final int SLOT_SIZE = 26;

    private final List<RadialSlot> rootSlots;
    private List<RadialSlot> activeSlots;

    private int currentSlotCount;
    private int hoveredSlot = -1;
    private boolean backHovered = false;

    private DynamicTexture donutTexture;
    private Identifier donutTextureId;
    private int lastRenderedHoverSlot = -2;
    private int lastRenderedSlotCount = -1;

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

                    // REFACTORED: Ask the mode if it supports release-activation
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

        // Skip animation if speed is 0
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

        // Sector Detection Math
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
            renderOptimizedDonut(graphics, width / 2, height / 2,
                    Math.max(0, RadialConfig.INSTANCE.ringRadius - RadialConfig.INSTANCE.innerPadding),
                    (RadialConfig.INSTANCE.ringRadius + RadialConfig.INSTANCE.outerReach),
                    currentSlotCount, ease);
        }

        // Draw Center "Back" button if in submenu
        if (activeSlots != rootSlots) {
            drawSlot(graphics, backX, backY, backHovered, ease, -1);
        }

        // Draw Slots
        for (int i = 0; i < count; i++) {
            double slotAngle = (Math.PI * 2 / count) * i - Math.PI / 2;
            float radius = config.ringRadius * ease;

            float x = (float) (cx + Math.cos(slotAngle) * radius - SLOT_SIZE / 2f);
            float y = (float) (cy + Math.sin(slotAngle) * radius - SLOT_SIZE / 2f);

            drawSlot(graphics, x, y, i == hoveredSlot, ease, i);
        }

        // Draw Center Text
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

    private void renderOptimizedDonut(GuiGraphicsExtractor graphics, int cx, int cy, float inner, float outer, int count, float ease) {
        int size = (int) (outer) * 2 + 2;
        if (size <= 0) return;

        if (donutTexture == null || donutTexture.getPixels().getWidth() != size || count != lastRenderedSlotCount) {
            if (donutTexture != null) donutTexture.close();

            NativeImage image = new NativeImage(size, size, false);
            donutTexture = new DynamicTexture(() -> "", image);

            donutTextureId = Identifier.fromNamespaceAndPath("radial", "radial_donut");
            Minecraft.getInstance().getTextureManager().register(donutTextureId, donutTexture);
            lastRenderedHoverSlot = -2;
            lastRenderedSlotCount = count;
        }

        if (hoveredSlot != lastRenderedHoverSlot) {
            generateDonutPixels(donutTexture.getPixels(), inner, outer, count);
            donutTexture.upload();
            lastRenderedHoverSlot = hoveredSlot;
        }

        int alpha = (int) (ease * 255);
        int tintColor = (alpha << 24) | 0xFFFFFF;

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(ease, ease);

        int offset = -size / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, donutTextureId, offset, offset, 0, 0, size, size, size, size, tintColor);

        graphics.pose().popMatrix();
    }

    private void generateDonutPixels(NativeImage image, float inner, float outer, int count) {
        int size = image.getWidth();
        float center = size / 2.0f;
        double sectorSize = (Math.PI * 2) / count;

        int base = RadialConfig.INSTANCE.backgroundColor.getRGB();
        int hover = RadialConfig.INSTANCE.activationColor.getRGB();
        boolean showHover = RadialConfig.INSTANCE.showActivationZone;

        float gapWidth = RadialConfig.INSTANCE.sectorGap;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setPixel(x, y, 0x00000000);
            }
        }

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center + 0.5f;
                float dy = y - center + 0.5f;
                float distSq = dx * dx + dy * dy;

                if (distSq < (inner - 2) * (inner - 2) || distSq > (outer + 2) * (outer + 2)) {
                    continue;
                }

                float dist = (float) Math.sqrt(distSq);
                double angle = Math.atan2(dy, dx) + Math.PI / 2;
                if (angle < 0) angle += Math.PI * 2;

                float edgeDist = Math.min(dist - inner, outer - dist);
                float alphaMod = Math.max(0.0f, Math.min(1.0f, edgeDist + 0.5f));

                if (alphaMod <= 0.0f) continue;

                if (gapWidth > 0.0f) {
                    double relativeAngle = (angle + sectorSize / 2.0) % sectorSize;
                    double angularDistToNearestEdge = Math.min(relativeAngle, sectorSize - relativeAngle);
                    double pixelDistToSectorEdge = angularDistToNearestEdge * dist;
                    float gapAlpha = Math.max(0.0f, Math.min(1.0f, (float) (pixelDistToSectorEdge - (gapWidth / 2.0f) + 0.5f)));
                    alphaMod *= gapAlpha;
                }

                int color = base;
                if (showHover && hoveredSlot != -1) {
                    double targetAngle = hoveredSlot * sectorSize;
                    double angleDiff = Math.abs(angle - targetAngle);
                    if (angleDiff > Math.PI) angleDiff = Math.PI * 2 - angleDiff;

                    double angularDistToHoverEdge = angleDiff - (sectorSize / 2.0);
                    double pixelDistToHoverEdge = angularDistToHoverEdge * dist;

                    float hoverRatio = Math.max(0.0f, Math.min(1.0f, (float) (0.5f - pixelDistToHoverEdge)));
                    if (hoverRatio > 0.0f) {
                        color = blendColors(base, hover, hoverRatio);
                    }
                }

                color = applyAlpha(color, alphaMod);
                image.setPixelABGR(x, y, toABGR(color));
            }
        }
    }

    private int toABGR(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private int blendColors(int c1, int c2, float ratio) {
        float inv = 1.0f - ratio;
        return (int) (((c1 >> 24) & 0xFF) * inv + ((c2 >> 24) & 0xFF) * ratio) << 24 |
                (int) (((c1 >> 16) & 0xFF) * inv + ((c2 >> 16) & 0xFF) * ratio) << 16 |
                (int) (((c1 >> 8) & 0xFF) * inv + ((c2 >> 8) & 0xFF) * ratio) << 8 |
                (int) ((c1 & 0xFF) * inv + (c2 & 0xFF) * ratio);
    }

    private int applyAlpha(int color, float alpha) {
        return ((int) (((color >> 24) & 0xFF) * alpha) << 24) | (color & 0x00FFFFFF);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, float x, float y, boolean hovered, float ease, int index) {
        int alpha = (int) (ease * 255);
        int color = (alpha << 24) | 0xFFFFFF;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);

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
            minecraft.setScreen(new RadialSlotEditorScreen(activeSlots.get(hoveredSlot), activeSlots == rootSlots));
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private void performAction(RadialSlot slot) {
        slot.mode.performAction(slot, new RadialScreenContext() {
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
        if (donutTextureId != null) {
            Minecraft.getInstance().getTextureManager().release(donutTextureId);
            donutTexture.close();
            donutTexture = null;
        }
    }
}