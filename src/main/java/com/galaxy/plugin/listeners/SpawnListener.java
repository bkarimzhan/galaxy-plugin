package com.galaxy.plugin.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Set;
import java.util.function.Supplier;

public final class SpawnListener implements Listener {

    private final Supplier<Location> spawnSupplier;
    private final Set<String> rescueWorlds;

    public SpawnListener(Supplier<Location> spawnSupplier, Set<String> rescueWorlds) {
        this.spawnSupplier = spawnSupplier;
        this.rescueWorlds = rescueWorlds;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent ev) {
        Player p = ev.getPlayer();
        if (!p.hasPlayedBefore()) {
            Location target = spawnSupplier.get();
            if (target != null) p.teleport(target);
            return;
        }
        if (rescueWorlds.contains(p.getWorld().getName()) && p.getVehicle() == null) {
            Location target = spawnSupplier.get();
            if (target != null) {
                p.teleport(target);
                p.sendMessage("Recovered to spawn platform — you were in transit at logout.");
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent ev) {
        if (ev.isBedSpawn() || ev.isAnchorSpawn()) return;
        Location target = spawnSupplier.get();
        if (target != null) ev.setRespawnLocation(target);
    }
}
