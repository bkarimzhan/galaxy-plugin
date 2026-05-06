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

public final class TerrainPlanetGenerator extends ChunkGenerator {

    public record Profile(
            Material surface,
            Material foundation,
            Material accent,
            Biome biome,
            int baseY,
            int heightRange,
            double terrainScale,
            double ridgeStrength,
            double warpStrength,
            int accentMinHeight) {}

    private static final int BEDROCK_Y = -64;
    private static final int BASE_OCTAVES = 6;
    private static final int RIDGE_OCTAVES = 4;
    private static final int WARP_OCTAVES = 3;

    private final Profile p;
    private SimplexOctaveGenerator base;
    private SimplexOctaveGenerator ridge;
    private SimplexOctaveGenerator warpX;
    private SimplexOctaveGenerator warpZ;
    private long initSeed = Long.MIN_VALUE;

    public TerrainPlanetGenerator(Profile p) {
        this.p = p;
    }

    private void init(long seed) {
        if (initSeed == seed) return;
        initSeed = seed;
        base = new SimplexOctaveGenerator(seed, BASE_OCTAVES);
        base.setScale(p.terrainScale());
        ridge = new SimplexOctaveGenerator(seed ^ 0xCAFEBABEL, RIDGE_OCTAVES);
        ridge.setScale(p.terrainScale() * 1.5);
        warpX = new SimplexOctaveGenerator(seed ^ 0xDEADBEEFL, WARP_OCTAVES);
        warpX.setScale(p.terrainScale() * 0.5);
        warpZ = new SimplexOctaveGenerator(seed ^ 0xFEEDFACEL, WARP_OCTAVES);
        warpZ.setScale(p.terrainScale() * 0.5);
    }

    private int heightAt(int wx, int wz) {
        double dx = warpX.noise(wx, wz, 0.5, 0.5, true) * p.warpStrength();
        double dz = warpZ.noise(wx, wz, 0.5, 0.5, true) * p.warpStrength();
        double x = wx + dx;
        double z = wz + dz;
        double n = base.noise(x, z, 0.5, 0.5, true);
        double r = 1.0 - Math.abs(ridge.noise(x, z, 0.5, 0.5, true));
        r = r * r;
        double combined = n * (1.0 - p.ridgeStrength()) + r * p.ridgeStrength();
        return p.baseY() + (int) Math.round((combined - 0.5) * 2.0 * p.heightRange());
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
                int height = heightAt(wx, wz);
                data.setBlock(dx, bedrockY, dz, Material.BEDROCK);
                for (int y = bedrockY + 1; y < height; y++) {
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
}
