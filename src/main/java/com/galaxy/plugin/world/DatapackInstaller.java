package com.galaxy.plugin.world;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public final class DatapackInstaller {

    private DatapackInstaller() {}

    private static final String[] FILES = {
            "galaxy-datapack/pack.mcmeta",
            "galaxy-datapack/data/galaxy/worldgen/biome/space.json",
    };

    public static void installFor(Plugin plugin, String worldName) {
        Logger log = plugin.getLogger();
        File container = Bukkit.getWorldContainer();
        File dpDir = new File(container, worldName + "/datapacks/galaxy");
        try {
            for (String resourcePath : FILES) {
                String relative = resourcePath.substring("galaxy-datapack/".length());
                File target = new File(dpDir, relative);
                target.getParentFile().mkdirs();
                try (InputStream in = plugin.getResource(resourcePath)) {
                    if (in == null) {
                        log.severe("Datapack resource not found in jar: " + resourcePath);
                        continue;
                    }
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            log.info("Datapack 'galaxy' installed at " + dpDir.getAbsolutePath());
        } catch (IOException e) {
            log.severe("Failed to install galaxy datapack into '" + worldName + "': " + e.getMessage());
        }
    }
}
