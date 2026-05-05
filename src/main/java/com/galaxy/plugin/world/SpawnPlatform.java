package com.galaxy.plugin.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

public final class SpawnPlatform {

    public static final int CENTER_X = 0;
    public static final int CENTER_Y = 80;
    public static final int CENTER_Z = 0;
    public static final int RADIUS = 4;
    public static final int FOUNDATION_DEPTH = 16;

    private final Logger log;
    private final File flagFile;

    public SpawnPlatform(Logger log, File pluginDataFolder) {
        this.log = log;
        this.flagFile = new File(pluginDataFolder, ".spawn-platform-built");
    }

    public Location locationIn(World world) {
        return new Location(world, CENTER_X + 0.5, CENTER_Y + 1, CENTER_Z + 0.5, 0f, 0f);
    }

    public void buildOnce(World planetEarth) {
        if (flagFile.exists()) {
            log.info("Spawn-platform flag-file present — skipping rebuild.");
            return;
        }
        log.info("Building spawn platform in '" + planetEarth.getName() + "' at "
                + CENTER_X + "," + CENTER_Y + "," + CENTER_Z);

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = CENTER_X + dx;
                int z = CENTER_Z + dz;
                boolean edge = Math.abs(dx) == RADIUS || Math.abs(dz) == RADIUS;
                Material top = edge ? Material.SMOOTH_STONE_SLAB : Material.QUARTZ_BLOCK;
                set(planetEarth, x, CENTER_Y, z, top);

                for (int dy = 1; dy <= FOUNDATION_DEPTH; dy++) {
                    if (Math.abs(dx) == RADIUS && Math.abs(dz) == RADIUS) {
                        set(planetEarth, x, CENTER_Y - dy, z, Material.STONE_BRICKS);
                    } else if (!edge) {
                        set(planetEarth, x, CENTER_Y - dy, z, Material.AIR);
                    }
                }
            }
        }

        set(planetEarth, CENTER_X, CENTER_Y + 1, CENTER_Z, Material.LIGHT);
        set(planetEarth, CENTER_X - 1, CENTER_Y + 1, CENTER_Z, Material.AIR);
        set(planetEarth, CENTER_X + 1, CENTER_Y + 1, CENTER_Z, Material.AIR);
        set(planetEarth, CENTER_X, CENTER_Y + 1, CENTER_Z - 1, Material.AIR);
        set(planetEarth, CENTER_X, CENTER_Y + 1, CENTER_Z + 1, Material.AIR);

        set(planetEarth, CENTER_X - RADIUS, CENTER_Y + 1, CENTER_Z, Material.GLOWSTONE);
        set(planetEarth, CENTER_X + RADIUS, CENTER_Y + 1, CENTER_Z, Material.GLOWSTONE);
        set(planetEarth, CENTER_X, CENTER_Y + 1, CENTER_Z - RADIUS, Material.GLOWSTONE);
        set(planetEarth, CENTER_X, CENTER_Y + 1, CENTER_Z + RADIUS, Material.GLOWSTONE);

        try {
            flagFile.getParentFile().mkdirs();
            Files.writeString(flagFile.toPath(),
                    "Built on first plugin enable. Delete this file to force a rebuild.\n");
            log.info("Spawn platform built. Flag: " + flagFile.getAbsolutePath());
        } catch (IOException e) {
            log.severe("Failed to write spawn-platform flag-file: " + e.getMessage());
        }
    }

    private static void set(World w, int x, int y, int z, Material m) {
        Block b = w.getBlockAt(x, y, z);
        if (b.getType() != m) b.setType(m, false);
    }
}
