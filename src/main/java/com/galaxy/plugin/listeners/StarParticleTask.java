package com.galaxy.plugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public final class StarParticleTask extends BukkitRunnable {

    private static final int STARS_PER_TICK = 100;
    private static final long PERIOD_TICKS = 10L;
    private static final double R_MIN = 30;
    private static final double R_MAX = 240;

    private final String spaceWorldName;
    private final Random rng = new Random();

    private StarParticleTask(String spaceWorldName) {
        this.spaceWorldName = spaceWorldName;
    }

    public static StarParticleTask schedule(Plugin plugin, String spaceWorldName) {
        StarParticleTask t = new StarParticleTask(spaceWorldName);
        t.runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);
        return t;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(spaceWorldName)) continue;
            World w = p.getWorld();
            double px = p.getLocation().getX();
            double py = p.getLocation().getY();
            double pz = p.getLocation().getZ();

            for (int i = 0; i < STARS_PER_TICK; i++) {
                double r = R_MIN + rng.nextDouble() * (R_MAX - R_MIN);
                double theta = rng.nextDouble() * Math.PI * 2;
                double phi = Math.acos(2 * rng.nextDouble() - 1) - Math.PI / 2;
                double x = px + r * Math.cos(phi) * Math.cos(theta);
                double y = py + r * Math.sin(phi);
                double z = pz + r * Math.cos(phi) * Math.sin(theta);
                w.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0, null, true);
            }
        }
    }
}
