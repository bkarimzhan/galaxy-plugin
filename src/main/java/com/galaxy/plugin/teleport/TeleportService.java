package com.galaxy.plugin.teleport;

import com.galaxy.plugin.planets.Planet;
import com.galaxy.plugin.planets.PlanetManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class TeleportService {

    private final Logger log;
    private final World spaceWorld;
    private final PlanetManager planets;

    public TeleportService(Logger log, World spaceWorld, PlanetManager planets) {
        this.log = log;
        this.spaceWorld = spaceWorld;
        this.planets = planets;
    }

    public void toSpaceFromPlanet(Player player, String planetWorldName) {
        Planet p = planets.findByWorldName(planetWorldName);
        Location target;
        if (p == null) {
            target = spaceWorld.getSpawnLocation();
        } else {
            Location c = p.sphereCenter();
            target = new Location(spaceWorld,
                    c.getX(), c.getY(), c.getZ() + p.sphereRadius() + 5,
                    180f, 0f);
        }
        if (player.getVehicle() != null) {
            player.leaveVehicle();
        }
        player.teleport(target);
        player.setVelocity(target.getDirection().multiply(0));
        log.info("Teleport " + player.getName() + " → space @ " + target.toVector());
    }

    public void toPlanet(Player player, Planet planet) {
        World w = Bukkit.getWorld(planet.worldName());
        if (w == null) {
            log.warning("Planet world '" + planet.worldName() + "' not loaded — cannot teleport " + player.getName());
            return;
        }
        Location target = planet.spawnIn(w);
        if (player.getVehicle() != null) {
            player.leaveVehicle();
        }
        player.teleport(target);
        player.setVelocity(target.getDirection().multiply(0));
        log.info("Teleport " + player.getName() + " → " + planet.id() + " @ " + target.toVector());
    }

    public boolean isInSpace(Player p) {
        return p.getWorld().getName().equals(spaceWorld.getName());
    }

    public boolean isOnAnyPlanet(Player p) {
        return planets.findByWorldName(p.getWorld().getName()) != null;
    }
}
