package com.galaxy.plugin.commands;

import com.galaxy.plugin.ship.ShipController;
import org.bukkit.entity.Player;

public final class UnshipCommand extends PlayerCommand {

    private final ShipController ships;

    public UnshipCommand(ShipController ships) {
        this.ships = ships;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (!ships.isShip(player.getVehicle())) {
            player.sendMessage("You are not on a ship.");
            return;
        }
        ships.dismountAndDespawn(player);
        player.sendMessage("Ship dismissed.");
    }
}
