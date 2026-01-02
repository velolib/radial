package velo.radial.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class RadialConfigScreen {

    private static final Identifier SLOT_TEXTURE = Identifier.of("minecraft", "gamemode_switcher/slot");
    private static final int SLOT_SIZE = 26;
    private static boolean showPreview = true;

    public static Screen create(Screen parent) {
        RadialConfig config = RadialConfig.INSTANCE;

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("screen.radial.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("screen.radial.config.category.layout"))
                        .tooltip(Text.translatable("screen.radial.config.description.main"))

                        // Toggle for showing preview
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("screen.radial.config.show_preview"))
                                .description(OptionDescription.of(Text.translatable("screen.radial.config.show_preview.tooltip")))
                                .binding(true, () -> showPreview, v -> showPreview = v)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        // Preview widget
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("screen.radial.config.preview"))
                                .binding(false, () -> false, v -> {})
                                .customController(opt -> new ControllerBuilder<Boolean>() {
                                    @Override
                                    public Controller<Boolean> build() {
                                        return new Controller<Boolean>() {
                                            private AbstractWidget widget;

                                            @Override
                                            public Option<Boolean> option() {
                                                return opt;
                                            }

                                            @Override
                                            public Text formatValue() {
                                                return Text.empty();
                                            }

                                            @Override
                                            public AbstractWidget provideWidget(YACLScreen screen, dev.isxander.yacl3.api.utils.Dimension<Integer> dimension) {
                                                if (widget == null) {
                                                    widget = new AbstractWidget(dimension) {
                                                        @Override
                                                        public void setFocused(boolean focused) {}

                                                        @Override
                                                        public boolean isFocused() {
                                                            return false;
                                                        }

                                                        @Override
                                                        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                                                            if (showPreview) {
                                                                renderPreview(context, config);
                                                            }
                                                        }
                                                    };
                                                }
                                                return widget;
                                            }
                                        };
                                    }
                                }.build())
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(Text.translatable("screen.radial.config.category.settings"))

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("screen.radial.config.slot_count"))
                                        .description(OptionDescription.of(Text.translatable("screen.radial.config.slot_count.tooltip")))
                                        .binding(8, () -> config.slotCount, v -> config.slotCount = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(3, 12).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("screen.radial.config.radius"))
                                        .description(OptionDescription.of(Text.translatable("screen.radial.config.radius.tooltip")))
                                        .binding(75, () -> config.ringRadius, v -> config.ringRadius = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(30, 200).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("screen.radial.config.inner_padding"))
                                        .description(OptionDescription.of(Text.translatable("screen.radial.config.inner_padding.tooltip")))
                                        .binding(40, () -> config.innerPadding, v -> config.innerPadding = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 200).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("screen.radial.config.outer_reach"))
                                        .description(OptionDescription.of(Text.translatable("screen.radial.config.outer_reach.tooltip")))
                                        .binding(100, () -> config.outerReach, v -> config.outerReach = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 300).step(1))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("screen.radial.config.animation_speed"))
                                        .description(OptionDescription.of(Text.translatable("screen.radial.config.animation_speed.tooltip")))
                                        .binding(200, () -> config.animationSpeedMs, v -> config.animationSpeedMs = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 2000).step(10))
                                        .build())
                                .build())
                        .build())
                .save(RadialConfig::save)
                .build()
                .generateScreen(parent);
    }

    private static void renderPreview(DrawContext context, RadialConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;

        int r = config.ringRadius;
        int inner = Math.max(0, r - config.innerPadding);
        int outer = r + config.outerReach;
        int count = config.slotCount;

        renderSmoothDonut(context, cx, cy, inner, outer, 0x2200FF00);

        List<RadialSlot> slots = config.slots;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i - Math.PI / 2;
            int slotX = cx + (int) (Math.cos(angle) * r) - SLOT_SIZE / 2;
            int slotY = cy + (int) (Math.sin(angle) * r) - SLOT_SIZE / 2;

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            if (i < slots.size()) {
                ItemStack stack = slots.get(i).getRenderStack();
                context.drawItem(stack, slotX + 5, slotY + 5);
            }
        }
    }

    private static void renderSmoothDonut(DrawContext context, int cx, int cy, int inner, int outer, int color) {
        if (outer <= 0) return;
        for (int y = -outer; y <= outer; y++) {
            int xOuter = (int) Math.sqrt(outer * outer - y * y);
            int xInner = 0;
            if (Math.abs(y) < inner) xInner = (int) Math.sqrt(inner * inner - y * y);
            if (xOuter > xInner) {
                context.fill(cx - xOuter, cy + y, cx - xInner, cy + y + 1, color);
                context.fill(cx + xInner, cy + y, cx + xOuter, cy + y + 1, color);
            }
        }
    }
}