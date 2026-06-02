package velo.radial.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

                        // Toggle for showing preview
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("screen.radial.config.show_preview"))
                                .description(OptionDescription.of(Component.translatable("screen.radial.config.show_preview.tooltip")))
                                .binding(true, () -> showPreview, v -> showPreview = v)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        // Preview widget
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("screen.radial.config.preview"))
                                .binding(false, () -> false, _ -> {})
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
                                                public void setFocused(boolean focused) {
                                                }

                                                @Override
                                                public boolean isFocused() {
                                                    return false;
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

    private static void renderSmoothDonut(GuiGraphicsExtractor graphics, int cx, int cy, int inner, int outer) {
        if (outer <= 0) return;

        // TODO: Add anti-aliasing
        for (int y = -outer; y <= outer; y++) {
            int xOuter = (int) Math.sqrt(outer * outer - y * y);
            int xInner = 0;
            if (Math.abs(y) < inner) {
                xInner = (int) Math.sqrt(inner * inner - y * y);
            }
            if (xOuter > xInner) {
                // Draw horizontal lines 1 pixel tall
                graphics.fill(cx - xOuter, cy + y, cx - xInner, cy + y + 1, 0x44000000);
                graphics.fill(cx + xInner, cy + y, cx + xOuter, cy + y + 1, 0x44000000);
            }
        }
    }
}