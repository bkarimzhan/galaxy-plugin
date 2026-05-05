package com.galaxy.plugin.commands;

import com.galaxy.plugin.planets.Planet;
import com.galaxy.plugin.planets.PlanetManager;
import com.galaxy.plugin.teleport.TeleportService;
import org.bukkit.entity.Player;

public final class LeavePlanetCommand extends PlayerCommand {

    private final TeleportService teleport;
    private final PlanetManager planets;

    public LeavePlanetCommand(TeleportService teleport, PlanetManager planets) {
        this.teleport = teleport;
        this.planets = planets;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (teleport.isOnAnyPlanet(player)) {
            teleport.toSpaceFromPlanet(player, player.getWorld().getName());
            player.sendMessage("Returning to space.");
            return;
        }
        if (teleport.isInSpace(player)) {
            Planet nearest = nearest(player);
            if (nearest != null) {
                teleport.toPlanet(player, nearest);
                player.sendMessage("Warping to nearest planet: " + nearest.id());
                return;
            }
        }
        player.sendMessage("Nothing to leave from here.");
    }

    private Planet nearest(Player p) {
        Planet best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Planet pl : planets.all().values()) {
            double d2 = pl.sphereCenter().distanceSquared(p.getLocation());
            if (d2 < bestD2) { bestD2 = d2; best = pl; }
        }
        return best;
    }
}
