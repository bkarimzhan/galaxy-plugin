package com.galaxy.plugin.commands;

import com.galaxy.plugin.planets.Planet;
import com.galaxy.plugin.planets.PlanetManager;
import com.galaxy.plugin.teleport.TeleportService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlanetsCommand extends PlayerCommand {

    private final TeleportService teleport;
    private final PlanetManager planets;

    public PlanetsCommand(TeleportService teleport, PlanetManager planets) {
        this.teleport = teleport;
        this.planets = planets;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        boolean inSpace = teleport.isInSpace(player);
        Location ref = inSpace ? player.getLocation() : null;

        List<Row> rows = new ArrayList<>();
        for (Planet p : planets.all().values()) {
            double dist = ref == null ? -1 : Math.sqrt(ref.distanceSquared(p.sphereCenter()));
            rows.add(new Row(p, dist));
        }
        if (ref != null) rows.sort((a, b) -> Double.compare(a.distance, b.distance));

        player.sendMessage("§6Solar system §7(use §e/goto <name>§7):");
        for (Row r : rows) {
            String distStr = r.distance < 0 ? "" : String.format(" §7(%.0f blocks away)", r.distance);
            String world = r.planet.worldName();
            player.sendMessage("  §e" + r.planet.id() + "§7 — " + world + distStr);
        }
    }

    private record Row(Planet planet, double distance) {}
}
