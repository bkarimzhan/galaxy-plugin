package com.galaxy.plugin.world;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public final class MarsTerrainGenerator extends ChunkGenerator {

    private static final int BEDROCK_Y = -64;
    private static final int BASE_Y = 95;
    private static final int HEIGHT_RANGE = 30;
    private static final double TERRAIN_SCALE = 0.0035;
    private static final double RIDGE_STRENGTH = 0.45;
    private static final double WARP_STRENGTH = 14.0;
    private static final int VOLCANIC_PEAK_Y = 115;

    private static final int CRATER_CELL = 64;
    private static final int CRATER_MIN_R = 8;
    private static final int CRATER_MAX_R = 22;
    private static final int CRATER_MIN_DEPTH = 3;
    private static final int CRATER_MAX_DEPTH = 7;
    private static final double CRATER_RIM_MUL = 1.18;
    private static final int CRATER_SAFE_ZONE = 18;

    private SimplexOctaveGenerator base;
    private SimplexOctaveGenerator ridge;
    private SimplexOctaveGenerator warpX;
    private SimplexOctaveGenerator warpZ;
    private long initSeed = Long.MIN_VALUE;

    private void init(long seed) {
        if (initSeed == seed) return;
        initSeed = seed;
        base = new SimplexOctaveGenerator(seed, 6);
        base.setScale(TERRAIN_SCALE);
        ridge = new SimplexOctaveGenerator(seed ^ 0xCAFEBABEL, 4);
        ridge.setScale(TERRAIN_SCALE * 1.5);
        warpX = new SimplexOctaveGenerator(seed ^ 0xDEADBEEFL, 3);
        warpX.setScale(TERRAIN_SCALE * 0.5);
        warpZ = new SimplexOctaveGenerator(seed ^ 0xFEEDFACEL, 3);
        warpZ.setScale(TERRAIN_SCALE * 0.5);
    }

    private static int mix(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private double baseHeight(int wx, int wz) {
        double dx = warpX.noise(wx, wz, 0.5, 0.5, true) * WARP_STRENGTH;
        double dz = warpZ.noise(wx, wz, 0.5, 0.5, true) * WARP_STRENGTH;
        double x = wx + dx;
        double z = wz + dz;
        // SimplexOctaveGenerator.noise(...true) возвращает [-1, 1]; нормализуем к [0, 1].
        double n = (base.noise(x, z, 0.5, 0.5, true) + 1.0) * 0.5;
        double r = 1.0 - Math.abs(ridge.noise(x, z, 0.5, 0.5, true));
        r = r * r;
        double combined = n * (1.0 - RIDGE_STRENGTH) + r * RIDGE_STRENGTH;
        return BASE_Y + (combined - 0.5) * 2.0 * HEIGHT_RANGE;
    }

    private double craterDelta(int wx, int wz) {
        // Защита спавна: не ставим кратер близко к (0,0)
        if (wx * wx + wz * wz < CRATER_SAFE_ZONE * CRATER_SAFE_ZONE) return 0.0;

        double total = 0.0;
        int cx0 = Math.floorDiv(wx, CRATER_CELL);
        int cz0 = Math.floorDiv(wz, CRATER_CELL);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                int cx = cx0 + ox;
                int cz = cz0 + oz;
                int h = mix(cx, cz);
                // ~65% ячеек содержат кратер (порог по младшему байту)
                if ((h & 0xFF) > 165) continue;

                int radius = CRATER_MIN_R + ((h >>> 20) & 0x0F) % (CRATER_MAX_R - CRATER_MIN_R + 1);
                int depth = CRATER_MIN_DEPTH + ((h >>> 24) & 0x07) % (CRATER_MAX_DEPTH - CRATER_MIN_DEPTH + 1);
                int margin = radius + 4;
                int range = CRATER_CELL - 2 * margin;
                if (range < 1) range = 1;
                int centerX = cx * CRATER_CELL + margin + Math.floorMod((h >>> 8) & 0xFFFF, range);
                int centerZ = cz * CRATER_CELL + margin + Math.floorMod((h >>> 14) & 0xFFFF, range);

                double ddx = wx - centerX;
                double ddz = wz - centerZ;
                double d = Math.sqrt(ddx * ddx + ddz * ddz);

                if (d < radius) {
                    double t = d / radius;
                    total += -depth * (1.0 - t * t); // параболическая чаша
                } else if (d < radius * CRATER_RIM_MUL) {
                    double rimT = (d - radius) / (radius * (CRATER_RIM_MUL - 1.0));
                    total += (1.0 - rimT) * 1.5; // приподнятый край
                }
            }
        }
        return total;
    }

    private boolean isInsideCrater(int wx, int wz) {
        if (wx * wx + wz * wz < CRATER_SAFE_ZONE * CRATER_SAFE_ZONE) return false;
        int cx0 = Math.floorDiv(wx, CRATER_CELL);
        int cz0 = Math.floorDiv(wz, CRATER_CELL);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                int cx = cx0 + ox;
                int cz = cz0 + oz;
                int h = mix(cx, cz);
                if ((h & 0xFF) > 165) continue;
                int radius = CRATER_MIN_R + ((h >>> 20) & 0x0F) % (CRATER_MAX_R - CRATER_MIN_R + 1);
                int margin = radius + 4;
                int range = CRATER_CELL - 2 * margin;
                if (range < 1) range = 1;
                int centerX = cx * CRATER_CELL + margin + Math.floorMod((h >>> 8) & 0xFFFF, range);
                int centerZ = cz * CRATER_CELL + margin + Math.floorMod((h >>> 14) & 0xFFFF, range);
                double ddx = wx - centerX;
                double ddz = wz - centerZ;
                if (ddx * ddx + ddz * ddz < radius * radius) return true;
            }
        }
        return false;
    }

    private Material surfaceTop(int wx, int wz, int height, boolean inCrater) {
        // Дно кратера — обнажённая порода без пыли
        if (inCrater) {
            int h = mix(wx, wz) & 0xFF;
            if (h < 90) return Material.RED_TERRACOTTA;
            if (h < 170) return Material.ORANGE_TERRACOTTA;
            return Material.BROWN_TERRACOTTA;
        }
        // Высокие плато — открытый базальт (тёмные регионы Марса)
        if (height >= VOLCANIC_PEAK_Y) {
            int h = mix(wx, wz) & 0xFF;
            return h < 140 ? Material.BROWN_TERRACOTTA : Material.RED_TERRACOTTA;
        }
        // Равнины — пыльная корка с локальными выходами породы
        int h = mix(wx, wz) & 0xFF;
        if (h < 175) return Material.RED_SAND;
        if (h < 220) return Material.ORANGE_TERRACOTTA;
        return Material.RED_TERRACOTTA;
    }

    private Material subsurface(int wx, int y, int wz, int depth) {
        if (depth <= 2) return Material.ORANGE_TERRACOTTA;
        if (depth <= 5) return Material.RED_TERRACOTTA;
        // Глубокая порода — каменное основание (Марс — базальтовая планета под пыльным слоем)
        int h = mix(wx + y * 31, wz) & 0xFF;
        if (h < 70) return Material.GRANITE;
        if (h < 150) return Material.RED_SANDSTONE;
        return Material.STONE;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
        init(info.getSeed());
        int minY = data.getMinHeight();
        int bedrockY = Math.max(BEDROCK_Y, minY);
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int wx = chunkX * 16 + dx;
                int wz = chunkZ * 16 + dz;
                double terrain = baseHeight(wx, wz) + craterDelta(wx, wz);
                int height = (int) Math.round(terrain);
                boolean inCrater = isInsideCrater(wx, wz);

                data.setBlock(dx, bedrockY, dz, Material.BEDROCK);
                for (int y = bedrockY + 1; y < height; y++) {
                    data.setBlock(dx, y, dz, subsurface(wx, y, wz, height - y));
                }
                data.setBlock(dx, height, dz, surfaceTop(wx, wz, height, inCrater));
            }
        }
    }

    @Override public void generateSurface(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {}
    @Override public void generateBedrock(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {}
    @Override public void generateCaves(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {}
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo info) {
        return new BiomeProvider() {
            @Override
            public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
                return Biome.BADLANDS;
            }
            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
                return List.of(Biome.BADLANDS);
            }
        };
    }
}
