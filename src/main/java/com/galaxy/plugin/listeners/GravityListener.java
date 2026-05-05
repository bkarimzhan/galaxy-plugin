package com.galaxy.plugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public final class GravityListener implements Listener {

    private static final double DEFAULT_GRAVITY = 0.08;

    private static final Map<String, Double> FACTORS = Map.of(
            "planet_mercury", 0.38,
            "planet_venus",   0.91,
            "planet_earth",   1.00,
            "planet_mars",    0.38,
            "planet_jupiter", 2.36,
            "planet_saturn",  0.92,
            "planet_uranus",  0.89,
            "planet_neptune", 1.14,
            "planet_sun",     5.00
    );

    public GravityListener(Plugin plugin) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent ev) {
        update(ev.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent ev) {
        update(ev.getPlayer());
    }

    private void update(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.GRAVITY);
        if (attr == null) return;
        double factor = FACTORS.getOrDefault(p.getWorld().getName(), 1.00);
        attr.setBaseValue(DEFAULT_GRAVITY * factor);
    }
}
