package com.galaxy.plugin.commands;

import com.galaxy.plugin.teleport.TeleportService;
import org.bukkit.entity.Player;

public final class LeavePlanetCommand extends PlayerCommand {

    private final TeleportService teleport;

    public LeavePlanetCommand(TeleportService teleport) {
        this.teleport = teleport;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (!teleport.isOnAnyPlanet(player)) {
            player.sendMessage("You are not on a planet.");
            return;
        }
        teleport.toSpaceFromPlanet(player, player.getWorld().getName());
        player.sendMessage("Returning to space.");
    }
}
