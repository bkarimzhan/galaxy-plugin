package com.galaxy.plugin.listeners;

import com.galaxy.plugin.planets.Planet;
import com.galaxy.plugin.planets.PlanetManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;

public final class SightTask extends BukkitRunnable {

    private static final long PERIOD_TICKS = 5L;
    private static final double MAX_DISTANCE = 320;

    private static final Map<String, String> DISPLAY = Map.ofEntries(
            Map.entry("sun", "Солнце"),
            Map.entry("mercury", "Меркурий"),
            Map.entry("venus", "Венера"),
            Map.entry("earth", "Земля"),
            Map.entry("mars", "Марс"),
            Map.entry("jupiter", "Юпитер"),
            Map.entry("saturn", "Сатурн"),
            Map.entry("uranus", "Уран"),
            Map.entry("neptune", "Нептун")
    );

    private final String spaceWorldName;
    private final PlanetManager planets;

    private SightTask(String spaceWorldName, PlanetManager planets) {
        this.spaceWorldName = spaceWorldName;
        this.planets = planets;
    }

    public static SightTask schedule(Plugin plugin, String spaceWorldName, PlanetManager planets) {
        SightTask t = new SightTask(spaceWorldName, planets);
        t.runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);
        return t;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(spaceWorldName)) continue;
            Location eye = p.getEyeLocation();
            Vector dir = eye.getDirection();
            RayTraceResult r = p.getWorld().rayTraceBlocks(eye, dir, MAX_DISTANCE);
            if (r == null || r.getHitBlock() == null) {
                p.sendActionBar(Component.empty());
                continue;
            }
            int hx = r.getHitBlock().getX();
            int hy = r.getHitBlock().getY();
            int hz = r.getHitBlock().getZ();
            Planet found = findPlanet(hx, hy, hz);
            if (found == null) {
                p.sendActionBar(Component.empty());
                continue;
            }
            double distance = eye.distance(r.getHitBlock().getLocation().add(0.5, 0.5, 0.5));
            String name = DISPLAY.getOrDefault(found.id(), found.id());
            p.sendActionBar(Component.text(name + " · " + (int) distance + " блоков", NamedTextColor.YELLOW));
        }
    }

    private Planet findPlanet(int x, int y, int z) {
        Planet best = null;
        int bestD2 = Integer.MAX_VALUE;
        for (Planet pl : planets.all().values()) {
            Location c = pl.sphereCenter();
            int dx = x - c.getBlockX();
            int dy = y - c.getBlockY();
            int dz = z - c.getBlockZ();
            int d2 = dx * dx + dy * dy + dz * dz;
            int trigger = pl.sphereRadius() + 12;
            if (d2 <= trigger * trigger && d2 < bestD2) {
                best = pl;
                bestD2 = d2;
            }
        }
        return best;
    }
}
