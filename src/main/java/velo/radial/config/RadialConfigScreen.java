package velo.radial.config;

import com.mojang.blaze3d.platform.NativeImage;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class RadialConfigScreen {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final Identifier SELECTION_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/selection");
    private static final int SLOT_SIZE = 26;
    private static boolean showPreview = true;

    // Static Cache for the Config Preview Donut
    private static DynamicTexture previewTexture;
    private static Identifier previewTextureId;

    // Trackers to detect if sliders were dragged
    private static int lastSize = -1;
    private static int lastCount = -1;
    private static float lastGap = -1;
    private static float lastInner = -1;
    private static float lastOuter = -1;
    private static int lastBgColor = -1;
    private static int lastHoverColor = -1;
    private static boolean lastShowHover = true;

    public static Screen create(Screen parent) {
        RadialConfig config = RadialConfig.INSTANCE;

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("screen.radial.config.title"))

                // TAB 1: SETTINGS
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("screen.radial.config.category.settings"))
                        .tooltip(Component.translatable("screen.radial.config.description.main"))

                        // Toggle for showing preview, not saved to file
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("screen.radial.config.show_preview"))
                                .description(OptionDescription.of(Component.translatable("screen.radial.config.show_preview.tooltip")))
                                .binding(true, () -> showPreview, v -> showPreview = v)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        // Preview widget (Injected into the Settings tab)
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.empty()) // Blank name so it doesn't take up text space
                                .binding(false, () -> false, _ -> {
                                })
                                .customController(opt -> createPreviewController(opt, config))
                                .build())

                        // GROUP 1: DIMENSIONS & LAYOUT
                        // Controls the physical shape, size, and capacity of the ring.
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.radial.config.group.layout"))
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
                                .build())

                        // GROUP 2: APPEARANCE & COLORS
                        // Controls the visual style, colors, and borders.
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.radial.config.group.appearance"))
                                .option(Option.<Color>createBuilder()
                                        .name(Component.translatable("screen.radial.config.background_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.background_color.tooltip")))
                                        .binding(new Color(0x66000000, true),
                                                () -> config.backgroundColor,
                                                v -> config.backgroundColor = v)
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Component.translatable("screen.radial.config.activation_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.activation_color.tooltip")))
                                        .binding(new Color(0x44FFFFFF, true),
                                                () -> config.activationColor,
                                                v -> config.activationColor = v)
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.radial.config.show_activation_zone"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.show_activation_zone.tooltip")))
                                        .binding(true, () -> config.showActivationZone, v -> config.showActivationZone = v)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Component.translatable("screen.radial.config.sector_gap"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.sector_gap.tooltip")))
                                        .binding(2F, () -> config.sectorGap, v -> config.sectorGap = v)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 20f).step(0.5f))
                                        .build())
                                .build())

                        // GROUP 3: INTERACTION & BEHAVIOR
                        // Controls animations and how the user triggers actions.
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.radial.config.group.behavior"))
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
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.radial.config.animation_speed"))
                                        .description(OptionDescription.of(Component.translatable("screen.radial.config.animation_speed.tooltip")))
                                        .binding(200, () -> config.animationSpeedMs, v -> config.animationSpeedMs = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 2000).step(10))
                                        .build())
                                .build())
                        .build())

                // TAB 2: LIVE PREVIEW ONLY
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("screen.radial.config.preview")) // The name of the second Tab
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.empty()) // Invisible name
                                .binding(false, () -> false, _ -> {
                                })
                                // Re-using our extracted controller logic
                                .customController(opt -> createPreviewController(opt, config))
                                .build())
                        .build())

                .save(RadialConfig::save)
                .build()
                .generateScreen(parent);
    }

    /**
     * Helper method to generate the dummy widget that hooks the render loop into YACL
     */
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
        };
    }

    private static void renderPreview(GuiGraphicsExtractor graphics, RadialConfig config) {
        Minecraft client = Minecraft.getInstance();
        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;

        float inner = Math.max(0, config.ringRadius - config.innerPadding);
        float outer = config.ringRadius + config.outerReach;
        int count = config.slotCount;

        if (config.showActivationZone && count > 0) {
            renderOptimizedPreviewDonut(graphics, cx, cy, inner, outer, config);
        }

        // Draw preview slots
        for (int i = 0; i < count; i++) {
            double slotAngle = (Math.PI * 2 / count) * i - Math.PI / 2;
            int x = (int) (cx + Math.cos(slotAngle) * config.ringRadius - SLOT_SIZE / 2f);
            int y = (int) (cy + Math.sin(slotAngle) * config.ringRadius - SLOT_SIZE / 2f);

            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, 0, 0, SLOT_SIZE, SLOT_SIZE, 0xFFFFFFFF);

            // Force the first slot to appear hovered
            if (i == 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION_TEXTURE, 0, 0, SLOT_SIZE, SLOT_SIZE, 0xFFFFFFFF);
            }

            graphics.pose().popMatrix();
        }
    }

    private static void renderOptimizedPreviewDonut(GuiGraphicsExtractor graphics, int cx, int cy, float inner, float outer, RadialConfig config) {
        int size = (int) (outer) * 2 + 2;
        if (size <= 0) return;

        // Check if the texture buffer needs resizing
        if (previewTexture == null || previewTexture.getPixels().getWidth() != size) {
            if (previewTexture != null) previewTexture.close();

            NativeImage image = new NativeImage(size, size, false);
            previewTexture = new DynamicTexture(() -> "", image);

            previewTextureId = Identifier.fromNamespaceAndPath("radial", "config_preview_donut");
            Minecraft.getInstance().getTextureManager().register(previewTextureId, previewTexture);

            // Force a regeneration on next frame
            lastSize = -1;
        }

        // Check if ANY config variable changed that requires a visual redraw
        if (size != lastSize || config.slotCount != lastCount || config.sectorGap != lastGap ||
                inner != lastInner || outer != lastOuter ||
                config.backgroundColor.getRGB() != lastBgColor || config.activationColor.getRGB() != lastHoverColor ||
                config.showActivationZone != lastShowHover) {

            generatePreviewPixels(previewTexture.getPixels(), inner, outer, config);
            previewTexture.upload();

            // Update trackers
            lastSize = size;
            lastCount = config.slotCount;
            lastGap = config.sectorGap;
            lastInner = inner;
            lastOuter = outer;
            lastBgColor = config.backgroundColor.getRGB();
            lastHoverColor = config.activationColor.getRGB();
            lastShowHover = config.showActivationZone;
        }

        int offset = -size / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, previewTextureId, cx + offset, cy + offset, 0, 0, size, size, size, size, 0xFFFFFFFF);
    }

    private static void generatePreviewPixels(NativeImage image, float inner, float outer, RadialConfig config) {
        int size = image.getWidth();
        float center = size / 2.0f;
        int count = config.slotCount;
        double sectorSize = (Math.PI * 2) / count;

        int base = config.backgroundColor.getRGB();
        int hover = config.activationColor.getRGB();
        boolean showHover = config.showActivationZone;
        float gapWidth = config.sectorGap;

        // Force slot 0 to be the active hovered slot for the preview
        int hoveredSlot = 0;

        // Clear the image buffer to transparent
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

                // Anti-Alias Edges
                float edgeDist = Math.min(dist - inner, outer - dist);
                float alphaMod = Math.max(0.0f, Math.min(1.0f, edgeDist + 0.5f));

                if (alphaMod <= 0.0f) continue;

                // Sector Gaps
                if (gapWidth > 0.0f) {
                    double relativeAngle = (angle + sectorSize / 2.0) % sectorSize;
                    double angularDistToNearestEdge = Math.min(relativeAngle, sectorSize - relativeAngle);
                    double pixelDistToSectorEdge = angularDistToNearestEdge * dist;

                    float gapAlpha = Math.max(0.0f, Math.min(1.0f, (float) (pixelDistToSectorEdge - (gapWidth / 2.0f) + 0.5f)));
                    alphaMod *= gapAlpha;
                }

                // Hover Color Blending
                int color = base;
                if (showHover) {
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

    private static int toABGR(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int blendColors(int c1, int c2, float ratio) {
        float inv = 1.0f - ratio;
        return (int) (((c1 >> 24) & 0xFF) * inv + ((c2 >> 24) & 0xFF) * ratio) << 24 |
                (int) (((c1 >> 16) & 0xFF) * inv + ((c2 >> 16) & 0xFF) * ratio) << 16 |
                (int) (((c1 >> 8) & 0xFF) * inv + ((c2 >> 8) & 0xFF) * ratio) << 8 |
                (int) ((c1 & 0xFF) * inv + (c2 & 0xFF) * ratio);
    }

    private static int applyAlpha(int color, float alpha) {
        return ((int) (((color >> 24) & 0xFF) * alpha) << 24) | (color & 0x00FFFFFF);
    }
}