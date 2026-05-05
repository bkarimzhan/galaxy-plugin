package com.galaxy.plugin.listeners;

import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class SpaceMobGuard implements Listener {

    private final String spaceWorldName;

    public SpaceMobGuard(String spaceWorldName) {
        this.spaceWorldName = spaceWorldName;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent ev) {
        if (!ev.getLocation().getWorld().getName().equals(spaceWorldName)) return;
        if (ev.getEntity() instanceof EnderDragon) {
            ev.setCancelled(true);
        }
    }
}
