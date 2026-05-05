package com.galaxy.plugin.planets;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public record Planet(
        String id,
        String worldName,
        Location sphereCenter,
        int sphereRadius,
        Material sphereShell,
        Material sphereCore,
        double spawnX,
        double spawnY,
        double spawnZ,
        float spawnYaw
) {
    public Location spawnIn(World w) {
        return new Location(w, spawnX, spawnY, spawnZ, spawnYaw, 0f);
    }
}
