package velo.radial.ui;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import velo.radial.RadialClient;
import velo.radial.config.RadialConfig;
import velo.radial.config.RadialSlot;
import velo.radial.config.SlotMode;

import java.util.List;
import java.util.Objects;

public class RadialScreen extends Screen {

    private static final Identifier SLOT_TEXTURE =
            Identifier.of("minecraft", "gamemode_switcher/slot");
    private static final Identifier SELECTION_TEXTURE =
            Identifier.of("minecraft", "gamemode_switcher/selection");
    private static final int SLOT_SIZE = 26;

    private final List<RadialSlot> rootSlots;
    private List<RadialSlot> activeSlots;

    private int currentSlotCount;
    private int hoveredSlot = -1;
    private boolean backHovered = false;

    private float animProgress = 0f;

    public RadialScreen() {
        super(Text.empty());
        this.rootSlots = RadialConfig.INSTANCE.slots;
        this.activeSlots = rootSlots;
        this.currentSlotCount = RadialConfig.INSTANCE.slotCount;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        int keyCode = KeyBindingHelper
                .getBoundKeyOf(RadialClient.OPEN_RADIAL)
                .getCode();

        long handle = MinecraftClient.getInstance()
                .getWindow()
                .getHandle();

        // Close when the hotkey is released
        if (GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_RELEASE) {
            close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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

        float ease = 1.0f - (float) Math.pow(1.0f - animProgress, 3);

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

        if (activeSlots != rootSlots) {
            drawSlot(context, backX, backY, backHovered, ease, -1);
        }

        for (int i = 0; i < count; i++) {
            double slotAngle = (Math.PI * 2 / count) * i - Math.PI / 2;
            float radius = config.ringRadius * ease;

            float x = (float) (cx + Math.cos(slotAngle) * radius - SLOT_SIZE / 2f);
            float y = (float) (cy + Math.sin(slotAngle) * radius - SLOT_SIZE / 2f);

            drawSlot(context, x, y, i == hoveredSlot, ease, i);
        }

        if (hoveredSlot != -1 && hoveredSlot < activeSlots.size()) {
            String name = activeSlots.get(hoveredSlot).name;
            int alpha = (int) (ease * 255);

            context.drawTextWithShadow(
                    textRenderer,
                    Text.of(name),
                    cx - textRenderer.getWidth(name) / 2,
                    cy - 4,
                    (alpha << 24) | 0xFFFFFF
            );
        } else if (backHovered) {
            Text backText = Text.translatable("radial.ui.back");
            context.drawTextWithShadow(
                    textRenderer,
                    backText,
                    cx - textRenderer.getWidth(backText) / 2,
                    cy - 4,
                    0xFFFFFFFF
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSlot(
            DrawContext context,
            float x,
            float y,
            boolean hovered,
            float ease,
            int index
    ) {
        int alpha = (int) (ease * 255);
        int color = (alpha << 24) | 0xFFFFFF;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);

        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SLOT_TEXTURE,
                0, 0,
                SLOT_SIZE,
                SLOT_SIZE,
                color
        );

        if (hovered) {
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    SELECTION_TEXTURE,
                    0, 0,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    color
            );
        }

        if (index >= 0 && index < activeSlots.size()) {
            context.drawItem(activeSlots.get(index).getRenderStack(), 5, 5);
        } else if (index == -1) {
            String icon = "✖";
            int xOff = (SLOT_SIZE - textRenderer.getWidth(icon)) / 2;
            int yOff = (SLOT_SIZE - 8) / 2;
            context.drawTextWithShadow(textRenderer, icon, xOff, yOff, color);
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            if (backHovered) {
                goBack();
                return true;
            }

            if (hoveredSlot != -1 && hoveredSlot < activeSlots.size()) {
                performAction(activeSlots.get(hoveredSlot));
            } else {
                close();
            }
            return true;
        }

        if (click.button() == 1 && hoveredSlot != -1) {
            if (client != null) {
                client.setScreen(
                        new RadialSlotEditorScreen(
                                activeSlots.get(hoveredSlot),
                                activeSlots == rootSlots
                        )
                );
            }
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

        MinecraftClient client = MinecraftClient.getInstance();

        if (slot.mode == SlotMode.CHAT) {
            if (slot.value.startsWith("/")) {
                Objects.requireNonNull(client.getNetworkHandler())
                        .sendChatCommand(slot.value.substring(1));
            } else {
                Objects.requireNonNull(client.getNetworkHandler())
                        .sendChatMessage(slot.value);
            }
        } else if (slot.mode == SlotMode.KEYBIND) {
            for (KeyBinding key : client.options.allKeys) {
                if (key.getId().equals(slot.value)) {
                    RadialClient.scheduleKeyPress(key);
                }
            }
        }

        RadialClient.lockKey();
        close();
    }

    private void goBack() {
        activeSlots = rootSlots;
        currentSlotCount = RadialConfig.INSTANCE.slotCount;
        animProgress = 0f;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        return;
    }
}
