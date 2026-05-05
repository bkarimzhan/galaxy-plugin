package com.galaxy.plugin.planets;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

public final class SphereBuilder {

    private final Logger log;
    private final File flagFile;

    public SphereBuilder(Logger log, File pluginDataFolder) {
        this.log = log;
        this.flagFile = new File(pluginDataFolder, ".spheres-built");
    }

    public void buildAllOnce(World space, Iterable<Planet> planets) {
        if (flagFile.exists()) {
            log.info("Spheres flag-file present (" + flagFile.getName() + ") — skipping rebuild.");
            return;
        }
        log.info("Building star + planet spheres in '" + space.getName() + "'…");

        buildStar(space);
        for (Planet p : planets) {
            buildSphere(space, p);
        }

        try {
            flagFile.getParentFile().mkdirs();
            Files.writeString(flagFile.toPath(),
                    "Built on first plugin enable. Delete this file to force a rebuild on next startup.\n");
            log.info("Spheres built. Wrote flag-file: " + flagFile.getAbsolutePath());
        } catch (IOException e) {
            log.severe("Sphere build succeeded but failed to write flag-file: " + e.getMessage());
        }
    }

    private void buildStar(World w) {
        Location star = new Location(w, 0, 100, 0);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    setBlock(w, star.getBlockX() + dx, star.getBlockY() - 1, star.getBlockZ() + dz, Material.IRON_BLOCK);
                }
            }
        }
        setBlock(w, star.getBlockX(), star.getBlockY(), star.getBlockZ(), Material.BEACON);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 2) <= 2) {
                        setBlock(w, star.getBlockX() + dx, star.getBlockY() + dy, star.getBlockZ() + dz, Material.GLOWSTONE);
                    }
                }
            }
        }
        log.info("Star + beacon at " + star.toVector());
    }

    private void buildSphere(World w, Planet p) {
        Location c = p.sphereCenter();
        int r = p.sphereRadius();
        int r2 = r * r;
        int r2Inner = (r - 1) * (r - 1);

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 <= r2 && d2 > r2Inner) {
                        setBlock(w, c.getBlockX() + dx, c.getBlockY() + dy, c.getBlockZ() + dz, p.sphereShell());
                    }
                }
            }
        }
        setBlock(w, c.getBlockX(), c.getBlockY(), c.getBlockZ(), p.sphereCore());

        log.info("Mini-sphere '" + p.id() + "' built at " + c.toVector() + " r=" + r);
    }

    private static void setBlock(World w, int x, int y, int z, Material m) {
        Block b = w.getBlockAt(x, y, z);
        if (b.getType() != m) b.setType(m, false);
    }
}
