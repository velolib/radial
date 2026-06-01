package velo.radial.mixin;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import velo.radial.ui.RadialScreen;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void allowRadialMovement(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();

        if (client.screen instanceof RadialScreen) {

            KeyMapping self = (KeyMapping) (Object) this;

            if (self.getCategory().equals(KeyMapping.Category.MOVEMENT)) {

                int keyCode = KeyMappingHelper.getBoundKeyOf(self).getValue();
                long handle = client.getWindow().handle();
                boolean isPhysicallyPressed = GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;

                cir.setReturnValue(isPhysicallyPressed);
            }
        }
    }
}