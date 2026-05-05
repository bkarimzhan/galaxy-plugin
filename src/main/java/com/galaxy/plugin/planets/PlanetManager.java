package com.galaxy.plugin.planets;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class PlanetManager {

    private final Logger log;
    private final World spaceWorld;
    private final Map<String, Planet> planets = new LinkedHashMap<>();

    public PlanetManager(Logger log, World spaceWorld) {
        this.log = log;
        this.spaceWorld = spaceWorld;
    }

    public void loadFromFile(File yaml, InputStream defaultIn) {
        if (!yaml.exists() && defaultIn != null) {
            try (InputStream in = defaultIn) {
                yaml.getParentFile().mkdirs();
                java.nio.file.Files.copy(in, yaml.toPath());
                log.info("planets.yml not present — wrote defaults.");
            } catch (Exception e) {
                log.severe("Failed to write default planets.yml: " + e.getMessage());
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(yaml);
        ConfigurationSection root = cfg.getConfigurationSection("planets");
        if (root == null) {
            log.severe("planets.yml has no `planets:` root section.");
            return;
        }
        planets.clear();
        for (String id : root.getKeys(false)) {
            ConfigurationSection p = root.getConfigurationSection(id);
            if (p == null) continue;
            try {
                planets.put(id, parse(id, p));
            } catch (Exception e) {
                log.severe("Failed to parse planet '" + id + "': " + e.getMessage());
            }
        }
        log.info("Loaded " + planets.size() + " planet(s) from planets.yml: " + planets.keySet());
    }

    private Planet parse(String id, ConfigurationSection p) {
        String world = require(p, "world");
        ConfigurationSection sph = p.getConfigurationSection("sphere");
        if (sph == null) throw new IllegalArgumentException("missing sphere block");
        List<Integer> center = sph.getIntegerList("center");
        if (center.size() != 3) throw new IllegalArgumentException("sphere.center must be [x,y,z]");
        int radius = sph.getInt("radius", 18);
        Material shell = Material.matchMaterial(sph.getString("shell", "GREEN_CONCRETE"));
        Material core = Material.matchMaterial(sph.getString("core", "SEA_LANTERN"));
        if (shell == null || core == null) throw new IllegalArgumentException("invalid block material");
        boolean skipBuild = sph.getBoolean("skipBuild", false);

        ConfigurationSection sp = p.getConfigurationSection("spawn");
        if (sp == null) throw new IllegalArgumentException("missing spawn block");
        List<Double> spLoc = sp.getDoubleList("location");
        if (spLoc.size() != 3) throw new IllegalArgumentException("spawn.location must be [x,y,z]");
        float yaw = (float) sp.getDouble("yaw", 0.0);

        Location centerLoc = new Location(spaceWorld, center.get(0), center.get(1), center.get(2));
        return new Planet(id, world, centerLoc, radius, shell, core, skipBuild,
                spLoc.get(0), spLoc.get(1), spLoc.get(2), yaw);
    }

    private static String require(ConfigurationSection s, String key) {
        String v = s.getString(key);
        if (v == null) throw new IllegalArgumentException("missing key: " + key);
        return v;
    }

    public Planet get(String id) { return planets.get(id); }
    public Map<String, Planet> all() { return planets; }
    public Planet findByWorldName(String name) {
        for (Planet p : planets.values()) if (p.worldName().equals(name)) return p;
        return null;
    }
}
