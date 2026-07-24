package com.itinerant.caveinstability.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CaveInstabilityConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("caveinstability.json");

    private static CaveInstabilityConfig config = new CaveInstabilityConfig();

    private CaveInstabilityConfigManager() {
    }

    public static CaveInstabilityConfig getConfig() {
        return config;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                CaveInstabilityConfig loaded = GSON.fromJson(reader, CaveInstabilityConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}