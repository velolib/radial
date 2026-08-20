package dev.velolib.radial.mixin;

import dev.velolib.radial.config.RadialConfig;
import dev.velolib.radial.ui.screen.RadialScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class OptionsMixin {

    @Inject(method = "getMenuBackgroundBlurriness", at = @At("HEAD"), cancellable = true)
    private void animateRadialBlur(CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = Minecraft.getInstance();

        // Check if our screen is the one currently open
        if (mc.gui.screen() instanceof RadialScreen radialScreen) {

            // Get the user's actual saved setting (usually 0-10)
            Options options = (Options) (Object) this;
            int maxBlur = options.menuBackgroundBlurriness().get();

            // Calculate the animation frame
            float progress = radialScreen.getGlobalRevealProgress(RadialConfig.INSTANCE);
            float easedProgress = radialScreen.easeOutQuint(progress);

            // Override what the game sees for the blur radius!
            cir.setReturnValue((int) (maxBlur * easedProgress));
        }
    }
}
