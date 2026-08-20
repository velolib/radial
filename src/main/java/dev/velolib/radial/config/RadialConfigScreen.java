package dev.velolib.radial.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.velolib.radial.render.DonutRenderer;
import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class RadialConfigScreen {

    private static final Identifier SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");

    private static final Identifier SELECTION_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/selection");

    private static final DonutRenderer PREVIEW_RENDERER = new DonutRenderer("preview");

    private static final int SLOT_SIZE = 26;
    private static final float PREVIEW_HOVER_PUSH = 7.5F;
    private static final float PREVIEW_HOVER_SCALE = 0.1F;

    private static boolean showPreview = true;

    public static Screen create(Screen parent) {
        RadialConfig config = RadialConfig.INSTANCE;

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("screen.radial.config.title"))

                // SETTINGS
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("screen.radial.config.category.settings"))
                        .tooltip(Component.translatable("screen.radial.config.category.settings.tooltip"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("screen.radial.config.show_preview"))
                                .description(OptionDescription.of(
                                        Component.translatable("screen.radial.config.show_preview.tooltip")))
                                .binding(true, () -> showPreview, v -> showPreview = v)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.empty())
                                .binding(false, () -> false, _ -> {})
                                .customController(opt -> createPreviewController(opt, config))
                                .build())

                        // LAYOUT
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.radial.config.group.layout"))
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.slot_count"))
                                        .description(OptionDescription.of(
                                                Component.translatable("screen.radial.config.slot_count.tooltip")))
                                        .binding(8, () -> config.slotCount, v -> config.slotCount = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(2, 12)
                                                .step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.slot_radius"))
                                        .description(OptionDescription.of(
                                                Component.translatable("screen.radial.config.slot_radius.tooltip")))
                                        .binding(90, () -> config.slotRadius, v -> config.slotRadius = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(30, 300)
                                                .step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.radial_thickness"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.radial_thickness.tooltip")))
                                        .binding(90, () -> config.radialThickness, v -> config.radialThickness = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(20, 300)
                                                .step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.inner_detection_boundary"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.inner_detection_boundary.tooltip")))
                                        .binding(
                                                15,
                                                () -> config.innerDetectionBoundary,
                                                v -> config.innerDetectionBoundary = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, 200)
                                                .step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.outer_detection_boundary"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.outer_detection_boundary.tooltip")))
                                        .binding(
                                                25,
                                                () -> config.outerDetectionBoundary,
                                                v -> config.outerDetectionBoundary = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, 300)
                                                .step(1))
                                        .build())
                                .build())

                        // APPEARANCE
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.radial.config.group.appearance"))
                                .option(Option.<Color>createBuilder()
                                        .name(Component.translatable("screen.radial.config.background_color"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.background_color.tooltip")))
                                        .binding(
                                                new Color(0x661A150D, true),
                                                () -> config.backgroundColor,
                                                v -> config.backgroundColor = v)
                                        .controller(opt -> ColorControllerBuilder.create(opt)
                                                .allowAlpha(true))
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Component.translatable("screen.radial.config.activation_color"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.activation_color.tooltip")))
                                        .binding(
                                                new Color(0x66FFE7B0, true),
                                                () -> config.activationColor,
                                                v -> config.activationColor = v)
                                        .controller(opt -> ColorControllerBuilder.create(opt)
                                                .allowAlpha(true))
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Component.translatable("screen.radial.config.border_color"))
                                        .description(OptionDescription.of(
                                                Component.translatable("screen.radial.config.border_color.tooltip")))
                                        .binding(
                                                new Color(0x99FFE7B0, true),
                                                () -> config.borderColor,
                                                v -> config.borderColor = v)
                                        .controller(opt -> ColorControllerBuilder.create(opt)
                                                .allowAlpha(true))
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Component.translatable("screen.radial.config.highlight_border_color"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.highlight_border_color.tooltip")))
                                        .binding(
                                                new Color(0x99FFE7B0, true),
                                                () -> config.highlightBorderColor,
                                                v -> config.highlightBorderColor = v)
                                        .controller(opt -> ColorControllerBuilder.create(opt)
                                                .allowAlpha(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.radial.config.show_activation_zone"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.show_activation_zone.tooltip")))
                                        .binding(
                                                true,
                                                () -> config.showActivationZone,
                                                v -> config.showActivationZone = v)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.radial.config.draw_outer_borders"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.draw_outer_borders.tooltip")))
                                        .binding(true, () -> config.drawOuterBorders, v -> config.drawOuterBorders = v)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.radial.config.draw_sector_borders"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.draw_sector_borders.tooltip")))
                                        .binding(
                                                true, () -> config.drawSectorBorders, v -> config.drawSectorBorders = v)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Component.translatable("screen.radial.config.sector_border_width"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.sector_border_width.tooltip")))
                                        .binding(
                                                1.5f, () -> config.sectorBorderWidth, v -> config.sectorBorderWidth = v)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(0.0f, 5.0f)
                                                .step(0.1f))
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Component.translatable("screen.radial.config.sector_gap"))
                                        .description(OptionDescription.of(
                                                Component.translatable("screen.radial.config.sector_gap.tooltip")))
                                        .binding(3.0f, () -> config.sectorGap, v -> config.sectorGap = v)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(0.0f, 20.0f)
                                                .step(0.5f))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.radial.config.enable_background_blur"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.enable_background_blur.tooltip")))
                                        .binding(
                                                false,
                                                () -> config.enableBackgroundBlur,
                                                v -> config.enableBackgroundBlur = v)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .build())

                        // BEHAVIOR
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.radial.config.group.behavior"))
                                .option(Option.<RadialConfig.RevealAnimation>createBuilder()
                                        .name(Component.translatable("screen.radial.config.reveal_animation"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.reveal_animation.tooltip")))
                                        .binding(
                                                RadialConfig.RevealAnimation.ZOOM,
                                                () -> config.revealAnimation,
                                                v -> config.revealAnimation = v)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(RadialConfig.RevealAnimation.class))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.reveal_duration_ms"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.reveal_duration_ms.tooltip")))
                                        .binding(180, () -> config.revealDurationMs, v -> config.revealDurationMs = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, 2000)
                                                .step(10))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.radial.config.enable_hover_animation"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.enable_hover_animation.tooltip")))
                                        .binding(
                                                true,
                                                () -> config.enableHoverAnimation,
                                                v -> config.enableHoverAnimation = v)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable(
                                                "screen.radial.config.hover_animation_duration_ms"))
                                        .description(OptionDescription.of(Component.translatable(
                                                "screen.radial.config.hover_animation_duration_ms.tooltip")))
                                        .binding(
                                                120,
                                                () -> config.hoverAnimationDurationMs,
                                                v -> config.hoverAnimationDurationMs = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, 2000)
                                                .step(10))
                                        .build())
                                .option(Option.<RadialConfig.ActivationMode>createBuilder()
                                        .name(Component.translatable("screen.radial.config.activation_mode"))
                                        .description(OptionDescription.of(
                                                Component.translatable("screen.radial.config.activation_mode.tooltip")))
                                        .binding(
                                                RadialConfig.ActivationMode.CLICK,
                                                () -> config.activationMode,
                                                v -> config.activationMode = v)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(RadialConfig.ActivationMode.class))
                                        .build())
                                .build())
                        .build())

                // LIVE PREVIEW
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("screen.radial.config.category.preview"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.empty())
                                .binding(false, () -> false, _ -> {})
                                .customController(opt -> createPreviewController(opt, config))
                                .build())
                        .build())
                .save(RadialConfig::save)
                .build()
                .generateScreen(parent);
    }

    private static Controller<Boolean> createPreviewController(Option<Boolean> opt, RadialConfig config) {
        return new Controller<>() {
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
                        public void setFocused(boolean focused) {}

                        @Override
                        public void extractRenderState(
                                @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                            if (showPreview) {
                                renderPreview(graphics, config);
                            }
                        }
                    };
                }
                return widget;
            }
        };
    }

    private static float getVisibleInnerRadius(RadialConfig config) {
        return Math.max(0.0F, config.slotRadius - config.radialThickness / 2.0F);
    }

    private static float getVisibleOuterRadius(RadialConfig config) {
        return config.slotRadius + config.radialThickness / 2.0F;
    }

    private static float getDetectionInnerRadius(RadialConfig config) {
        return Math.max(0.0F, getVisibleInnerRadius(config) - config.innerDetectionBoundary);
    }

    private static float getDetectionOuterRadius(RadialConfig config) {
        float radius = getVisibleOuterRadius(config);
        if (config.enableHoverAnimation) {
            radius += PREVIEW_HOVER_PUSH;
        }
        return radius + config.outerDetectionBoundary;
    }

    /*
     * Detection-zone preview
     */
    private static void drawAnnulus(
            GuiGraphicsExtractor graphics, float cx, float cy, float innerRadius, float outerRadius) {
        if (outerRadius <= innerRadius) return;

        float outerSquared = outerRadius * outerRadius;
        float innerSquared = innerRadius * innerRadius;

        int minY = (int) Math.floor(cy - outerRadius);
        int maxY = (int) Math.ceil(cy + outerRadius);

        final int color = 0x3349A6FF;

        for (int y = minY; y <= maxY; y++) {
            float pixelY = y + 0.5F;
            float dy = pixelY - cy;
            float dySquared = dy * dy;
            float outerLimit = outerSquared - dySquared;

            if (outerLimit <= 0.0F) continue;

            float outerHalfWidth = (float) Math.sqrt(outerLimit);
            float innerHalfWidth = (dySquared >= innerSquared) ? 0.0F : (float) Math.sqrt(innerSquared - dySquared);

            int startLeft = (int) (cx - outerHalfWidth);
            int endRight = (int) (cx + outerHalfWidth);

            if (innerHalfWidth <= 0.0F) {
                // Completely solid row (above/below the inner hollow hole)
                graphics.fill(startLeft, y, endRight, y + 1, color);
            } else {
                // Hollow row (needs a left and right segment)
                int endLeft = (int) (cx - innerHalfWidth);
                int startRight = (int) (cx + innerHalfWidth);

                graphics.fill(startLeft, y, endLeft, y + 1, color);
                graphics.fill(startRight, y, endRight, y + 1, color);
            }
        }
    }

    /*
     * Preview rendering
     */
    private static void renderPreview(GuiGraphicsExtractor graphics, RadialConfig config) {
        Minecraft client = Minecraft.getInstance();

        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;

        float visibleInner = getVisibleInnerRadius(config);
        float visibleOuter = getVisibleOuterRadius(config);
        float detectionInner = getDetectionInnerRadius(config);
        float detectionOuter = getDetectionOuterRadius(config);
        int count = config.slotCount;

        if (count <= 0) return;

        PREVIEW_RENDERER.prepare(count, visibleInner, visibleOuter, 2.0F);

        int hoveredSlot = 0;

        // Draw outer/inner bounds
        if (detectionInner < visibleInner) {
            drawAnnulus(graphics, cx, cy, detectionInner, visibleInner);
        }

        if (detectionOuter > visibleOuter) {
            drawAnnulus(graphics, cx, cy, visibleOuter, detectionOuter);
        }

        if (config.showActivationZone) {
            for (int i = 0; i < count; i++) {
                boolean highlighted = (i == hoveredSlot);
                float hoverPush = (config.enableHoverAnimation && highlighted) ? PREVIEW_HOVER_PUSH : 0.0F;
                float slotAngle = (float) ((Math.PI * 2.0 / count) * i - Math.PI / 2.0);

                PREVIEW_RENDERER.renderSector(graphics, cx, cy, slotAngle, hoverPush, highlighted, 1.0F, 2.0F);
            }
        }

        for (int i = 0; i < count; i++) {
            boolean highlighted = (i == hoveredSlot);
            float hoverPush = (config.enableHoverAnimation && highlighted) ? PREVIEW_HOVER_PUSH : 0.0F;
            float slotAngle = (float) ((Math.PI * 2.0 / count) * i - Math.PI / 2.0);
            float slotRadius = config.slotRadius;
            float finalRadius = slotRadius + hoverPush;
            float slotX = (float) (cx + Math.cos(slotAngle) * finalRadius);
            float slotY = (float) (cy + Math.sin(slotAngle) * finalRadius);

            float hoverScale =
                    config.enableHoverAnimation ? 1.0F + PREVIEW_HOVER_SCALE * (highlighted ? 1.0F : 0.0F) : 1.0F;

            graphics.pose().pushMatrix();
            graphics.pose().translate(slotX, slotY);
            graphics.pose().scale(hoverScale, hoverScale);

            int drawOffset = -SLOT_SIZE / 2;

            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SLOT_TEXTURE,
                    drawOffset,
                    drawOffset,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    0xFFFFFFFF);

            if (highlighted) {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        SELECTION_TEXTURE,
                        drawOffset,
                        drawOffset,
                        SLOT_SIZE,
                        SLOT_SIZE,
                        0xFFFFFFFF);
            }

            graphics.pose().popMatrix();
        }
    }
}
