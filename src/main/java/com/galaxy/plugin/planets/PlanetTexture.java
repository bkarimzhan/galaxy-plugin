package com.galaxy.plugin.planets;

import org.bukkit.Material;

public final class PlanetTexture {

    private PlanetTexture() {}

    public static Material at(String planetId, int dx, int dy, int dz, int radius, Material fallback) {
        return switch (planetId) {
            case "mercury" -> mercury(dx, dy, dz);
            case "venus"   -> venus(dx, dy, dz, radius);
            case "earth"   -> earth(dx, dy, dz, radius);
            case "mars"    -> mars(dx, dy, dz, radius);
            case "jupiter" -> jupiter(dx, dy, dz, radius);
            case "saturn"  -> saturn(dx, dy, dz, radius);
            case "uranus"  -> uranus(dx, dy, dz, radius);
            case "neptune" -> neptune(dx, dy, dz, radius);
            default -> fallback;
        };
    }

    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return Math.abs(h ^ (h >>> 16));
    }

    private static double lat(int dy, int radius) {
        return (double) dy / radius;
    }

    private static Material mercury(int dx, int dy, int dz) {
        int h = hash(dx, dy, dz) % 10;
        if (h < 5) return Material.STONE;
        if (h < 8) return Material.COBBLESTONE;
        return Material.ANDESITE;
    }

    private static Material venus(int dx, int dy, int dz, int r) {
        int band = (dy + r) / 2 % 3;
        return switch (band) {
            case 0 -> Material.ORANGE_TERRACOTTA;
            case 1 -> Material.YELLOW_TERRACOTTA;
            default -> Material.MAGMA_BLOCK;
        };
    }

    private static Material earth(int dx, int dy, int dz, int r) {
        double l = lat(dy, r);
        if (Math.abs(l) > 0.78) return Material.SNOW_BLOCK;
        int h = hash(dx / 2, dy / 2, dz / 2) % 100;
        return h < 35 ? Material.GREEN_CONCRETE : Material.BLUE_CONCRETE;
    }

    private static Material mars(int dx, int dy, int dz, int r) {
        double l = lat(dy, r);
        if (Math.abs(l) > 0.82) return Material.SNOW_BLOCK;
        int h = hash(dx, dy, dz) % 10;
        if (h < 4) return Material.RED_SAND;
        if (h < 7) return Material.RED_CONCRETE;
        return Material.RED_TERRACOTTA;
    }

    private static Material jupiter(int dx, int dy, int dz, int r) {
        int band = ((dy + r) / 3) % 4;
        Material base = switch (band) {
            case 0 -> Material.ORANGE_TERRACOTTA;
            case 1 -> Material.WHITE_TERRACOTTA;
            case 2 -> Material.BROWN_TERRACOTTA;
            default -> Material.YELLOW_TERRACOTTA;
        };
        if (dy > -r/3 && dy < r/3 && dx > 0 && hash(dx, dy, dz) % 30 < 4) {
            return Material.RED_CONCRETE;
        }
        return base;
    }

    private static Material saturn(int dx, int dy, int dz, int r) {
        int band = ((dy + r) / 2) % 3;
        return switch (band) {
            case 0 -> Material.SMOOTH_SANDSTONE;
            case 1 -> Material.SANDSTONE;
            default -> Material.YELLOW_TERRACOTTA;
        };
    }

    private static Material uranus(int dx, int dy, int dz, int r) {
        int h = hash(dx, dy, dz) % 10;
        if (h < 6) return Material.LIGHT_BLUE_CONCRETE;
        if (h < 9) return Material.CYAN_CONCRETE;
        return Material.LIGHT_BLUE_TERRACOTTA;
    }

    private static Material neptune(int dx, int dy, int dz, int r) {
        int h = hash(dx, dy, dz) % 10;
        if (h < 6) return Material.BLUE_CONCRETE;
        if (h < 9) return Material.CYAN_CONCRETE;
        return Material.LIGHT_BLUE_CONCRETE;
    }
}
