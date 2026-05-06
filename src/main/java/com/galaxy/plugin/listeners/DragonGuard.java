package com.galaxy.plugin.listeners;

import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public final class DragonGuard implements Listener {

    private final String spaceWorldName;

    public DragonGuard(String spaceWorldName) {
        this.spaceWorldName = spaceWorldName;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent ev) {
        if (!(ev.getEntity() instanceof EnderDragon)) return;
        if (!ev.getLocation().getWorld().getName().equals(spaceWorldName)) return;
        ev.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent ev) {
        if (!(ev.getEntity() instanceof EnderDragon)) return;
        if (!ev.getLocation().getWorld().getName().equals(spaceWorldName)) return;
        ev.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChangeBlock(EntityChangeBlockEvent ev) {
        if (!(ev.getEntity() instanceof EnderDragon)) return;
        if (!ev.getEntity().getWorld().getName().equals(spaceWorldName)) return;
        ev.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent ev) {
        if (!(ev.getEntity() instanceof EnderDragon)) return;
        if (!ev.getEntity().getWorld().getName().equals(spaceWorldName)) return;
        ev.blockList().clear();
    }
}
