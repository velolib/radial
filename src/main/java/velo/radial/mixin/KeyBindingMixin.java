package velo.radial.mixin;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import velo.radial.ui.RadialScreen;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void allowRadialMovement(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.currentScreen instanceof RadialScreen) {

            KeyBinding self = (KeyBinding) (Object) this;

            if (self.getCategory().equals(KeyBinding.Category.MOVEMENT)) {

                int keyCode = KeyBindingHelper.getBoundKeyOf(self).getCode();
                long handle = client.getWindow().getHandle();
                boolean isPhysicallyPressed = GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;

                cir.setReturnValue(isPhysicallyPressed);
            }
        }
    }
}