package com.galaxy.plugin.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class WorldBootstrap {

    public static final String SPACE = "space";
    public static final String PLANET_EARTH = "planet_earth";
    public static final String PLANET_MARS = "planet_mars";
    public static final String PLANET_SUN = "planet_sun";
    public static final String PLANET_MERCURY = "planet_mercury";
    public static final String PLANET_VENUS = "planet_venus";
    public static final String PLANET_JUPITER = "planet_jupiter";
    public static final String PLANET_SATURN = "planet_saturn";
    public static final String PLANET_URANUS = "planet_uranus";
    public static final String PLANET_NEPTUNE = "planet_neptune";

    private final Logger log;
    private final NamespacedKey shipKey;

    public WorldBootstrap(Plugin plugin) {
        this.log = plugin.getLogger();
        this.shipKey = new NamespacedKey(plugin, "galaxy_ship");
    }

    public World createSpace(org.bukkit.plugin.Plugin plugin) {
        DatapackInstaller.installFor(plugin, PLANET_EARTH);
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
        applyLifelessRules(w);
        w.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setTime(18000);
        try {
            w.setViewDistance(32);
            w.setSimulationDistance(6);
            w.setSendViewDistance(32);
        } catch (Throwable t) {
            log.warning("Could not set extended view-distance for '" + SPACE + "': " + t.getMessage());
        }

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
                bar.setTitle("§6§lСолнечная система §e★");
                bar.setColor(org.bukkit.boss.BarColor.YELLOW);
                bar.setProgress(1.0);
                bar.setVisible(true);
                log.info("DragonBattle boss bar relabeled to 'Солнечная система'.");
            }
            try {
                battle.setRespawnPhase(org.bukkit.boss.DragonBattle.RespawnPhase.NONE);
            } catch (Throwable ignored) {}
        }

        log.info("World '" + SPACE + "' ready (THE_END void — no sky, no clouds, no dragon).");
        return w;
    }

    public World createPlanetEarth() {
        WorldCreator c = new WorldCreator(PLANET_EARTH)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(false);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + PLANET_EARTH + "'");
            return null;
        }
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(1000);
        applyLifelessRules(w);
        purgeAllMobs(w);
        log.info("World '" + PLANET_EARTH + "' ready (vanilla normal, border 1000, lifeless, no structures).");
        return w;
    }

    public World createPlanetMars() {
        WorldCreator c = new WorldCreator(PLANET_MARS)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .biomeProvider(new SingleBiomeProvider(Biome.ERODED_BADLANDS))
                .generateStructures(false);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + PLANET_MARS + "'");
            return null;
        }
        w.setSpawnLocation(new Location(w, 0, 120, 0));
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(1000);
        applyLifelessRules(w);
        purgeAllMobs(w);
        log.info("World '" + PLANET_MARS + "' ready (vanilla terrain, biome=eroded_badlands, lifeless).");
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
        applyLifelessRules(w);
        purgeAllMobs(w);
        log.info("World '" + PLANET_SUN + "' ready (lava ocean, border 500, lifeless).");
        return w;
    }

    public World createVanillaBiomePlanet(String name, Biome biome, int spawnY, int borderSize, String label) {
        WorldCreator c = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .biomeProvider(new SingleBiomeProvider(biome))
                .generateStructures(false);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + name + "'");
            return null;
        }
        w.setSpawnLocation(new Location(w, 0, spawnY, 0));
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(borderSize);
        applyLifelessRules(w);
        purgeAllMobs(w);
        log.info("World '" + name + "' ready (vanilla terrain, biome=" + biome.getKey() + ", " + label + ").");
        return w;
    }

    public World createFlatPlanet(String name, ChunkGenerator gen, int spawnY, int borderSize, String label) {
        WorldCreator c = new WorldCreator(name)
                .generator(gen)
                .type(WorldType.FLAT)
                .generateStructures(false);
        World w = Bukkit.createWorld(c);
        if (w == null) {
            log.severe("Failed to create world '" + name + "'");
            return null;
        }
        w.setSpawnLocation(new Location(w, 0, spawnY, 0));
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(borderSize);
        applyLifelessRules(w);
        purgeAllMobs(w);
        log.info("World '" + name + "' ready (" + label + ", border " + borderSize + ", lifeless).");
        return w;
    }

    private void applyLifelessRules(World w) {
        w.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING,    false);
        w.setGameRule(org.bukkit.GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(org.bukkit.GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(org.bukkit.GameRule.DO_INSOMNIA,        false);
        w.setGameRule(org.bukkit.GameRule.DISABLE_RAIDS,      true);
    }

    private void purgeAllMobs(World w) {
        int killed = 0;
        for (org.bukkit.entity.Entity e : w.getEntities()) {
            if (!(e instanceof org.bukkit.entity.Mob)) continue;
            if (e.getPersistentDataContainer().has(shipKey, PersistentDataType.BYTE)) continue;
            e.remove();
            killed++;
        }
        if (killed > 0) log.info("Purged " + killed + " stale mob(s) from '" + w.getName() + "'.");
    }
}
