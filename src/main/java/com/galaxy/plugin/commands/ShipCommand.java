package com.galaxy.plugin.commands;

import com.galaxy.plugin.ship.ShipController;
import org.bukkit.entity.Player;

public final class ShipCommand extends PlayerCommand {

    private final ShipController ships;

    public ShipCommand(ShipController ships) {
        this.ships = ships;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (player.getVehicle() != null) {
            player.sendMessage("You are already riding something.");
            return;
        }
        ships.spawnAndSeat(player);
        player.sendMessage("Look around and use WASD + Space (up) / Shift (down) to fly.");
    }
}
