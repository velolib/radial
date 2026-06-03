package velo.radial.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

public class RadialConfigScreen {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final int SLOT_SIZE = 26;
    private static boolean showPreview = true;

    public static Screen create(Screen parent) {
        RadialConfig config = RadialConfig.INSTANCE;

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("screen.radial.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("screen.radial.config.category.layout"))
                        .tooltip(Component.translatable("screen.radial.config.description.main"))

                        // Toggle for showing preview, not saved to file
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("screen.radial.config.show_preview"))
                                .description(OptionDescription.of(Component.translatable("screen.radial.config.show_preview.tooltip")))
                                .binding(true, () -> showPreview, v -> showPreview = v)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        // Preview widget
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("screen.radial.config.preview"))
                                .binding(false, () -> false, _ -> {
                                })
                                .customController(opt -> new Controller<>() {
                                    private AbstractWidget widget;

                                    @Override
                                    public Option<Boolean> option() {
                                        return opt;
                                    }

                                    @Override
                                    public Component formatValue() {
                                        return Component.empty();
                                    }

                                    @Override
                                    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dimension) {
                                        if (widget == null) {
                                            widget = new AbstractWidget(dimension) {
                                                @Override
                                                public boolean isFocused() {
                                                    return false;
                                                }

                                                @Override
                                                public void setFocused(boolean focused) {
                                                }

                                                @Override
                                                public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                                                    if (showPreview) {
                                                        renderPreview(graphics, config);
                                                    }
                                                }
                                            };
                                        }
                                        return widget;
                                    }
                                })
                                .build())

                        // Config options that are actually saved to file
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.radial.config.category.settings"))

                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.slot_count"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.slot_count.tooltip")))
                                        .binding(8, () -> config.slotCount, v -> config.slotCount = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(3, 12).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.radius"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.radius.tooltip")))
                                        .binding(75, () -> config.ringRadius, v -> config.ringRadius = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(30, 200).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.inner_padding"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.inner_padding.tooltip")))
                                        .binding(40, () -> config.innerPadding, v -> config.innerPadding = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 200).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.outer_reach"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.outer_reach.tooltip")))
                                        .binding(100, () -> config.outerReach, v -> config.outerReach = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 300).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.animation_speed"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.animation_speed.tooltip")))
                                        .binding(200, () -> config.animationSpeedMs, v -> config.animationSpeedMs = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 2000).step(10))
                                        .build())
                                .option(Option.<RadialConfig.ActivationMode>createBuilder()
                                        .name(Component.translatable("screen.radial.config.activation_mode"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.activation_mode.tooltip")))
                                        .binding(
                                                RadialConfig.ActivationMode.CLICK,
                                                () -> RadialConfig.INSTANCE.activationMode,
                                                val -> RadialConfig.INSTANCE.activationMode = val
                                        )
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(RadialConfig.ActivationMode.class))
                                        .build())
                                .build())
                        .build())
                .save(RadialConfig::save)
                .build()
                .generateScreen(parent);
    }

    private static void renderPreview(GuiGraphicsExtractor graphics, RadialConfig config) {
        Minecraft client = Minecraft.getInstance();
        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;

        int r = config.ringRadius;
        int inner = Math.max(0, r - config.innerPadding);
        int outer = r + config.outerReach;
        int count = config.slotCount;

        renderSmoothDonut(graphics, cx, cy, inner, outer);

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i - Math.PI / 2;
            int slotX = cx + (int) (Math.cos(angle) * r) - SLOT_SIZE / 2;
            int slotY = cy + (int) (Math.sin(angle) * r) - SLOT_SIZE / 2;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
        }
    }

    private static void renderSmoothDonut(GuiGraphicsExtractor graphics, int cx, int cy, float inner, float outer) {
        if (outer <= 0 || inner >= outer) return;

        int baseColor = 0x44000000;
        int maxRadius = (int) Math.ceil(outer + 1);

        // We calculate 1 quadrant (bottom-right) and mirror it to the other 3
        for (int y = 0; y <= maxRadius; y++) {
            float py = y + 0.5f; // Measure from pixel center
            boolean inSolid = false;
            int solidXStart = 0;

            for (int x = 0; x <= maxRadius; x++) {
                float px = x + 0.5f;
                float dist = (float) Math.hypot(px, py);

                // Calculate AA alpha based on distance.
                // This creates a 1-pixel wide gradient at the inner and outer edges.
                float outerAlpha = Math.max(0.0f, Math.min(1.0f, outer - dist + 0.5f));
                float innerAlpha = Math.max(0.0f, Math.min(1.0f, dist - inner + 0.5f));
                float alphaMask = Math.min(outerAlpha, innerAlpha);

                if (alphaMask >= 0.99f) {
                    // We hit solid color. Mark the start X but don't draw yet.
                    if (!inSolid) {
                        inSolid = true;
                        solidXStart = x;
                    }
                } else {
                    // We hit an edge or transparent space
                    if (inSolid) {
                        // Flush the solid horizontal chunk for all 4 quadrants at once
                        drawFourQuadrantsSolid(graphics, cx, cy, solidXStart, x, y, baseColor);
                        inSolid = false;
                    }

                    if (alphaMask > 0.01f) {
                        // Draw the semi-transparent anti-aliased edge pixel
                        int edgeColor = applyAlpha(baseColor, alphaMask);
                        drawFourQuadrantsPixel(graphics, cx, cy, x, y, edgeColor);
                    }
                }
            }

            // Safety flush if the solid line somehow touched the bounding box edge
            if (inSolid) {
                drawFourQuadrantsSolid(graphics, cx, cy, solidXStart, maxRadius + 1, y, baseColor);
            }
        }
    }

    private static void drawFourQuadrantsSolid(GuiGraphicsExtractor graphics, int cx, int cy, int xStart, int xEnd, int y, int color) {
        if (xStart >= xEnd) return;
        // Bottom Right
        graphics.fill(cx + xStart, cy + y, cx + xEnd, cy + y + 1, color);
        // Bottom Left
        graphics.fill(cx - xEnd, cy + y, cx - xStart, cy + y + 1, color);
        // Top Right
        graphics.fill(cx + xStart, cy - y - 1, cx + xEnd, cy - y, color);
        // Top Left
        graphics.fill(cx - xEnd, cy - y - 1, cx - xStart, cy - y, color);
    }

    private static void drawFourQuadrantsPixel(GuiGraphicsExtractor graphics, int cx, int cy, int x, int y, int color) {
        // Bottom Right
        graphics.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
        // Bottom Left
        graphics.fill(cx - x - 1, cy + y, cx - x, cy + y + 1, color);
        // Top Right
        graphics.fill(cx + x, cy - y - 1, cx + x + 1, cy - y, color);
        // Top Left
        graphics.fill(cx - x - 1, cy - y - 1, cx - x, cy - y, color);
    }

    private static int applyAlpha(int baseColor, float alphaMultiplier) {
        int a = (baseColor >> 24) & 0xFF;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >>  8) & 0xFF;
        int b =  baseColor        & 0xFF;

        int newA = (int) (a * alphaMultiplier);
        return (newA << 24) | (r << 16) | (g << 8) | b;
    }
}