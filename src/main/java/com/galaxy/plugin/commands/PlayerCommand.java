package com.galaxy.plugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

abstract class PlayerCommand implements CommandExecutor {

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command is for players.");
            return true;
        }
        executePlayer(p, args);
        return true;
    }

    protected abstract void executePlayer(Player player, String[] args);
}
