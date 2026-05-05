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
    public static final String PLANET_SUN = "planet_sun";

    private final Logger log;

    public WorldBootstrap(Logger log) {
        this.log = log;
    }

    public World createSpace() {
        WorldCreator c = new WorldCreator(SPACE)
                .generator(new SpaceChunkGenerator())
                .environment(World.Environment.THE_END)
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
        w.setStorm(false);
        w.setThundering(false);
        w.setWeatherDuration(Integer.MAX_VALUE);
        w.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);

        int removed = 0;
        for (org.bukkit.entity.Entity e : w.getEntitiesByClass(org.bukkit.entity.EnderDragon.class)) {
            e.remove();
            removed++;
        }
        if (removed > 0) log.info("Removed " + removed + " auto-spawned ender_dragon(s) from '" + SPACE + "'.");

        org.bukkit.boss.DragonBattle battle = w.getEnderDragonBattle();
        if (battle != null) {
            org.bukkit.boss.BossBar bar = battle.getBossBar();
            if (bar != null) {
                bar.setTitle("§6§lSolar System §e★");
                bar.setColor(org.bukkit.boss.BarColor.YELLOW);
                bar.setProgress(1.0);
                bar.setVisible(true);
                log.info("Re-titled DragonBattle boss bar to 'Solar System'.");
            }
        }

        log.info("World '" + SPACE + "' ready (THE_END void — no sky, no clouds, no dragon).");
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

    public World createPlanetSun() {
        WorldCreator c = new WorldCreator(PLANET_SUN)
                .generator(new LavaChunkGenerator())
                .type(WorldType.FLAT)
                .generateStructures(false);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + PLANET_SUN + "'");
            return null;
        }
        w.setSpawnLocation(new Location(w, 0, 80, 0));
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(500);
        log.info("World '" + PLANET_SUN + "' ready (lava ocean, border 500).");
        return w;
    }
}
