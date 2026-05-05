package com.galaxy.plugin.world;

import org.bukkit.Material;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public final class TerrainPlanetGenerator extends ChunkGenerator {

    public record Profile(
            Material surface,
            Material foundation,
            Material accent,
            int baseY,
            double amp1, double freq1,
            double amp2, double freq2,
            int accentMinHeight,
            Biome biome) {}

    private static final int BEDROCK_Y = -64;

    private final Profile p;

    public TerrainPlanetGenerator(Profile p) {
        this.p = p;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
        long seed = info.getSeed();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int wx = chunkX * 16 + dx;
                int wz = chunkZ * 16 + dz;
                double n = (valueNoise(wx * p.freq1(), wz * p.freq1(), seed) - 0.5) * 2 * p.amp1()
                         + (valueNoise(wx * p.freq2(), wz * p.freq2(), seed ^ 0xC051B33EL) - 0.5) * 2 * p.amp2();
                int height = (int) Math.round(p.baseY() + n);
                data.setBlock(dx, BEDROCK_Y, dz, Material.BEDROCK);
                for (int y = BEDROCK_Y + 1; y < height; y++) {
                    data.setBlock(dx, y, dz, p.foundation());
                }
                Material top = (height >= p.accentMinHeight() && p.accent() != null)
                        ? p.accent() : p.surface();
                data.setBlock(dx, height, dz, top);
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
                return p.biome();
            }
            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
                return List.of(p.biome());
            }
        };
    }

    private static double valueNoise(double x, double z, long seed) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double xf = x - xi;
        double zf = z - zi;
        double v00 = hashed(xi,     zi,     seed);
        double v10 = hashed(xi + 1, zi,     seed);
        double v01 = hashed(xi,     zi + 1, seed);
        double v11 = hashed(xi + 1, zi + 1, seed);
        double sx = xf * xf * (3 - 2 * xf);
        double sz = zf * zf * (3 - 2 * zf);
        double v0 = v00 + (v10 - v00) * sx;
        double v1 = v01 + (v11 - v01) * sx;
        return v0 + (v1 - v0) * sz;
    }

    private static double hashed(int x, int z, long seed) {
        long h = (x * 374761393L + z * 668265263L + seed * 0x9E3779B1L) ^ 0x4D5FCE3CL;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }
}
