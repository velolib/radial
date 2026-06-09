package velo.radial;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import velo.radial.config.RadialConfig;
import velo.radial.integration.MalilibIntegration;

public class RadialMain implements ModInitializer {
    @Override
    public void onInitialize() {
        // Load config here so it's available everywhere
        RadialConfig.load();

        if (FabricLoader.getInstance().isModLoaded("malilib")) {
            MalilibIntegration.init();
        }
    }
}