package com.galaxy.plugin.commands;

import com.galaxy.plugin.planets.Planet;
import com.galaxy.plugin.planets.PlanetManager;
import com.galaxy.plugin.teleport.TeleportService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GotoCommand extends PlayerCommand implements TabCompleter {

    private final TeleportService teleport;
    private final PlanetManager planets;

    public GotoCommand(TeleportService teleport, PlanetManager planets) {
        this.teleport = teleport;
        this.planets = planets;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("Usage: /goto <planet>. Try /planets for the list.");
            return;
        }
        Planet target = planets.get(args[0].toLowerCase());
        if (target == null) {
            player.sendMessage("Unknown planet '" + args[0] + "'. /planets for the list.");
            return;
        }
        teleport.toPlanet(player, target);
        player.sendMessage("Warping to " + target.id() + ".");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String id : planets.all().keySet()) {
            if (id.startsWith(prefix)) matches.add(id);
        }
        return matches;
    }
}
