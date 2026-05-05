package com.galaxy.plugin.listeners;

import com.galaxy.plugin.planets.Planet;
import com.galaxy.plugin.planets.PlanetManager;
import com.galaxy.plugin.teleport.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class ProximityTask extends BukkitRunnable {

    private static final int Y_LAUNCH_THRESHOLD = 300;
    private static final int APPROACH_PADDING = 3;
    private static final long PERIOD_TICKS = 5L;

    private final PlanetManager planets;
    private final TeleportService teleport;
    private final World spaceWorld;

    private ProximityTask(PlanetManager planets, TeleportService teleport, World spaceWorld) {
        this.planets = planets;
        this.teleport = teleport;
        this.spaceWorld = spaceWorld;
    }

    public static ProximityTask schedule(Plugin plugin, PlanetManager planets, TeleportService teleport, World spaceWorld) {
        ProximityTask t = new ProximityTask(planets, teleport, spaceWorld);
        t.runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);
        return t;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline()) continue;
            String worldName = p.getWorld().getName();

            if (worldName.equals(spaceWorld.getName())) {
                checkSpaceApproach(p);
                continue;
            }
            Planet planet = planets.findByWorldName(worldName);
            if (planet != null) {
                checkPlanetLaunch(p, planet);
            }
        }
    }

    private void checkPlanetLaunch(Player p, Planet planet) {
        if (p.getLocation().getY() > Y_LAUNCH_THRESHOLD) {
            teleport.toSpaceFromPlanet(p, planet.worldName());
        }
    }

    private void checkSpaceApproach(Player p) {
        Location loc = p.getLocation();
        for (Planet planet : planets.all().values()) {
            Location c = planet.sphereCenter();
            double trigger = planet.sphereRadius() + APPROACH_PADDING;
            if (loc.distanceSquared(c) <= trigger * trigger) {
                teleport.toPlanet(p, planet);
                return;
            }
        }
    }
}
