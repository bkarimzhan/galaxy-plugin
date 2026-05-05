package com.galaxy.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class SpaceCommand extends PlayerCommand {

    private final String spaceWorldName;

    public SpaceCommand(String spaceWorldName) {
        this.spaceWorldName = spaceWorldName;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        World w = Bukkit.getWorld(spaceWorldName);
        if (w == null) {
            player.sendMessage("Space world is not loaded.");
            return;
        }
        if (player.getVehicle() != null) player.leaveVehicle();
        player.teleport(w.getSpawnLocation());
        player.sendMessage("Welcome to space.");
    }
}
