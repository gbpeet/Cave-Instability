package com.itinerant.caveinstability;

import com.itinerant.caveinstability.config.CaveInstabilityConfigManager;
import com.itinerant.caveinstability.sound.CaveInstabilitySounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class CaveInstabilityMod implements ModInitializer {
    public static final String MOD_ID = "caveinstability";

    @Override
    public void onInitialize() {
        CaveInstabilityConfigManager.load();
        CaveInstabilitySounds.register();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}