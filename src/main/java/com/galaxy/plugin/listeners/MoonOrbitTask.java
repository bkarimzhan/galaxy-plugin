package com.galaxy.plugin.listeners;

import com.galaxy.plugin.planets.Planet;
import com.galaxy.plugin.planets.PlanetManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MoonOrbitTask extends BukkitRunnable {

    public record MoonDef(String id, String parentId, double orbitRadius,
                          double orbitPeriodSec, Material material, float scale,
                          double phaseRad) {}

    private static final List<MoonDef> MOONS = List.of(
            new MoonDef("moon",     "earth",   10,  18, Material.WHITE_CONCRETE,        1.4f, 0),
            new MoonDef("phobos",   "mars",    5,   9,  Material.GRAY_CONCRETE,         0.7f, 0),
            new MoonDef("deimos",   "mars",    7,   13, Material.LIGHT_GRAY_CONCRETE,   0.5f, Math.PI),
            new MoonDef("io",       "jupiter", 13,  10, Material.YELLOW_CONCRETE,       1.0f, 0),
            new MoonDef("europa",   "jupiter", 16,  16, Material.WHITE_TERRACOTTA,      1.0f, Math.PI / 2),
            new MoonDef("ganymede", "jupiter", 20,  24, Material.LIGHT_GRAY_CONCRETE,   1.2f, Math.PI),
            new MoonDef("callisto", "jupiter", 24,  32, Material.GRAY_CONCRETE,         1.1f, Math.PI * 1.5),
            new MoonDef("titan",    "saturn",  17,  20, Material.ORANGE_CONCRETE,       1.2f, 0),
            new MoonDef("titania",  "uranus",  10,  14, Material.LIGHT_BLUE_CONCRETE,   0.9f, 0),
            new MoonDef("triton",   "neptune", 11,  15, Material.CYAN_CONCRETE,         1.0f, 0)
    );

    private static final NamespacedKey TAG_KEY = new NamespacedKey("galaxy", "moon");

    public record MoonInstance(MoonDef def, UUID displayUUID) {}

    private final Plugin plugin;
    private final World spaceWorld;
    private final PlanetManager planets;
    private final List<MoonInstance> instances = new ArrayList<>();
    private final long startTick;

    private MoonOrbitTask(Plugin plugin, World spaceWorld, PlanetManager planets) {
        this.plugin = plugin;
        this.spaceWorld = spaceWorld;
        this.planets = planets;
        this.startTick = Bukkit.getCurrentTick();
    }

    public static MoonOrbitTask schedule(Plugin plugin, World spaceWorld, PlanetManager planets) {
        MoonOrbitTask t = new MoonOrbitTask(plugin, spaceWorld, planets);
        t.cleanupExisting();
        t.spawnAll();
        t.runTaskTimer(plugin, 1L, 1L);
        return t;
    }

    private void cleanupExisting() {
        for (Entity e : spaceWorld.getEntitiesByClass(BlockDisplay.class)) {
            if (e.getPersistentDataContainer().has(TAG_KEY, PersistentDataType.BYTE)) {
                e.remove();
            }
        }
    }

    private void spawnAll() {
        for (MoonDef def : MOONS) {
            Planet parent = planets.get(def.parentId());
            if (parent == null) continue;
            Location loc = computeLocation(parent, def, 0.0);
            BlockDisplay disp = (BlockDisplay) spaceWorld.spawnEntity(loc, org.bukkit.entity.EntityType.BLOCK_DISPLAY);
            disp.setBlock(def.material().createBlockData());
            disp.setTransformation(new Transformation(
                    new Vector3f(-def.scale() / 2f, -def.scale() / 2f, -def.scale() / 2f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(def.scale(), def.scale(), def.scale()),
                    new AxisAngle4f(0, 0, 1, 0)));
            disp.setBrightness(new Display.Brightness(15, 15));
            disp.setPersistent(false);
            disp.getPersistentDataContainer().set(TAG_KEY, PersistentDataType.BYTE, (byte) 1);
            instances.add(new MoonInstance(def, disp.getUniqueId()));
        }
        plugin.getLogger().info("Spawned " + instances.size() + " moon BlockDisplays.");
    }

    private Location computeLocation(Planet parent, MoonDef def, double seconds) {
        Location c = parent.sphereCenter();
        double angle = (seconds / def.orbitPeriodSec()) * 2 * Math.PI + def.phaseRad();
        double x = c.getX() + Math.cos(angle) * def.orbitRadius();
        double y = c.getY();
        double z = c.getZ() + Math.sin(angle) * def.orbitRadius();
        return new Location(spaceWorld, x, y, z);
    }

    @Override
    public void run() {
        double seconds = (Bukkit.getCurrentTick() - startTick) / 20.0;
        for (MoonInstance mi : instances) {
            Planet parent = planets.get(mi.def().parentId());
            if (parent == null) continue;
            Entity disp = Bukkit.getEntity(mi.displayUUID());
            if (disp == null) continue;
            Location target = computeLocation(parent, mi.def(), seconds);
            disp.teleport(target);
        }
    }

    public void shutdown() {
        cancel();
        for (MoonInstance mi : instances) {
            Entity disp = Bukkit.getEntity(mi.displayUUID());
            if (disp != null) disp.remove();
        }
        instances.clear();
    }
}
