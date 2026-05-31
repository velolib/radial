package velo.radial.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import velo.radial.ui.RadialScreen;

@Mixin(Gui.class)
public class InGameHudMixin {

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void forceHideCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        // If our Radial Menu is open, cancel the crosshair render immediately.
        // This stops Vanilla AND most mixin-based crosshair mods (like Dynamic Crosshair)
        // because they usually inject into this exact method too.
        if (Minecraft.getInstance().screen instanceof RadialScreen) {
            ci.cancel();
        }
    }
}