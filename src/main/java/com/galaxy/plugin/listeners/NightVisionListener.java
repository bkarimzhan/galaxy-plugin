package com.galaxy.plugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class NightVisionListener implements Listener {

    private final String spaceWorldName;

    public NightVisionListener(Plugin plugin, String spaceWorldName) {
        this.spaceWorldName = spaceWorldName;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent ev) {
        update(ev.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent ev) {
        update(ev.getPlayer());
    }

    private void update(Player p) {
        boolean inSpace = p.getWorld().getName().equals(spaceWorldName);
        if (inSpace) {
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    -1,
                    0,
                    true,
                    false,
                    false));
        } else {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }
}
