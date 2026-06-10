package velo.radial.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import velo.radial.render.DonutRenderer;

import java.awt.*;

public class RadialConfigScreen {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    private static final Identifier SELECTION_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "gamemode_switcher/selection");
    private static final DonutRenderer PREVIEW_RENDERER = new DonutRenderer("preview");
    private static final int SLOT_SIZE = 26;
    private static boolean showPreview = true;

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
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(2, 12).step(1))
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
            // Draw the preview donut using supersampling scale of 2.0f, full ease (1.0f), hovering slot 0
            PREVIEW_RENDERER.render(graphics, cx, cy, inner, outer, count, 0, 1.0f, 2.0f);
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
}