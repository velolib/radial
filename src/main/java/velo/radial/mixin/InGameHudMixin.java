package velo.radial.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import velo.radial.ui.RadialScreen;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void forceHideCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // If our Radial Menu is open, cancel the crosshair render immediately.
        // This stops Vanilla AND most mixin-based crosshair mods (like Dynamic Crosshair)
        // because they usually inject into this exact method too.
        if (MinecraftClient.getInstance().currentScreen instanceof RadialScreen) {
            ci.cancel();
        }
    }
}