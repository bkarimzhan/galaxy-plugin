package com.galaxy.plugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

public final class EndStructureCleanup implements Listener {

    private final Plugin plugin;
    private final String spaceWorldName;

    public EndStructureCleanup(Plugin plugin, String spaceWorldName) {
        this.plugin = plugin;
        this.spaceWorldName = spaceWorldName;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent ev) {
        if (!ev.getWorld().getName().equals(spaceWorldName)) return;
        Chunk c = ev.getChunk();
        int cx = c.getX();
        int cz = c.getZ();
        World w = ev.getWorld();

        if (cx == 6 && cz == 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> wipeObsidianPlatform(w), 4L);
        }
        if (cx == 0 && cz == 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> wipeExitPortal(w), 4L);
        }
    }

    private void wipeObsidianPlatform(World w) {
        for (int x = 95; x <= 105; x++) {
            for (int y = 47; y <= 53; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() == Material.OBSIDIAN || b.getType() == Material.BEDROCK) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void wipeExitPortal(World w) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = 55; y <= 78; y++) {
                    Block b = w.getBlockAt(x, y, z);
                    Material m = b.getType();
                    if (m == Material.BEDROCK || m == Material.END_PORTAL || m == Material.TORCH || m == Material.END_STONE) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}
