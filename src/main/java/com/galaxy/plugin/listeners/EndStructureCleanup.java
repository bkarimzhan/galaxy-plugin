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

    private static final int SUN_CX = 0, SUN_CY = 100, SUN_CZ = 0;
    private static final int SUN_R2 = 28 * 28;

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

    public void wipeNow(World w) {
        wipeObsidianPlatform(w);
        wipeExitPortal(w);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            wipeObsidianPlatform(w);
            wipeExitPortal(w);
        }, 60L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            wipeExitPortal(w);
        }, 200L, 200L);
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
        int wiped = 0;
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = 50; y <= 135; y++) {
                    Block b = w.getBlockAt(x, y, z);
                    Material m = b.getType();
                    if (m == Material.BEDROCK || m == Material.END_PORTAL || m == Material.TORCH || m == Material.WALL_TORCH || m == Material.SOUL_TORCH || m == Material.SOUL_WALL_TORCH || m == Material.END_STONE || m == Material.DRAGON_EGG) {
                        int dx = x - SUN_CX;
                        int dy = y - SUN_CY;
                        int dz = z - SUN_CZ;
                        int d2 = dx * dx + dy * dy + dz * dz;
                        Material replacement = (d2 <= SUN_R2) ? Material.SHROOMLIGHT : Material.AIR;
                        b.setType(replacement, false);
                        wiped++;
                    }
                }
            }
        }
        if (wiped > 0) plugin.getLogger().info("EndCleanup: wiped " + wiped + " end-junk block(s) from space centre.");
    }
}
