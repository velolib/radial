package velo.radial;

import net.fabricmc.api.ModInitializer;
import velo.radial.config.RadialConfig;

public class RadialMain implements ModInitializer {
    @Override
    public void onInitialize() {
        // Load config here so it's available everywhere
        RadialConfig.load();
    }
}