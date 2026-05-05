package com.galaxy.plugin.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.function.Supplier;

public final class SpawnListener implements Listener {

    private final Supplier<Location> spawnSupplier;

    public SpawnListener(Supplier<Location> spawnSupplier) {
        this.spawnSupplier = spawnSupplier;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent ev) {
        if (ev.getPlayer().hasPlayedBefore()) return;
        Location target = spawnSupplier.get();
        if (target != null) ev.getPlayer().teleport(target);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent ev) {
        if (ev.isBedSpawn() || ev.isAnchorSpawn()) return;
        Location target = spawnSupplier.get();
        if (target != null) ev.setRespawnLocation(target);
    }
}
