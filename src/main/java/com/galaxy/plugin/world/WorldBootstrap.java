package com.galaxy.plugin.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.logging.Logger;

public final class WorldBootstrap {

    public static final String SPACE = "space";
    public static final String PLANET_EARTH = "planet_earth";
    public static final String PLANET_MARS = "planet_mars";

    private final Logger log;

    public WorldBootstrap(Logger log) {
        this.log = log;
    }

    public World createSpace() {
        WorldCreator c = new WorldCreator(SPACE)
                .generator(new SpaceChunkGenerator())
                .type(WorldType.FLAT)
                .generateStructures(false);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + SPACE + "'");
            return null;
        }
        w.setSpawnLocation(new Location(w, 0, 100, 30));
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(5000);
        log.info("World '" + SPACE + "' ready (void).");
        return w;
    }

    public World createPlanetEarth() {
        WorldCreator c = new WorldCreator(PLANET_EARTH)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(true);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + PLANET_EARTH + "'");
            return null;
        }
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(1000);
        log.info("World '" + PLANET_EARTH + "' ready (vanilla normal, border 1000).");
        return w;
    }

    public World createPlanetMars() {
        WorldCreator c = new WorldCreator(PLANET_MARS)
                .generator(new DesertChunkGenerator())
                .type(WorldType.FLAT)
                .generateStructures(false);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + PLANET_MARS + "'");
            return null;
        }
        w.setSpawnLocation(new Location(w, 0, 65, 0));
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(1000);
        log.info("World '" + PLANET_MARS + "' ready (flat desert, border 1000).");
        return w;
    }
}
