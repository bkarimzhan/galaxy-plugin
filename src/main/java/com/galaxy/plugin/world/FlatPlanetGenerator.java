package com.galaxy.plugin.world;

import org.bukkit.Material;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public final class FlatPlanetGenerator extends ChunkGenerator {

    private static final int BEDROCK_Y = -64;
    private static final int SURFACE_Y = 64;

    private final Material surface;
    private final Material foundation;
    private final Biome biome;

    public FlatPlanetGenerator(Material surface, Material foundation, Biome biome) {
        this.surface = surface;
        this.foundation = foundation;
        this.biome = biome;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                data.setBlock(x, BEDROCK_Y, z, Material.BEDROCK);
                for (int y = BEDROCK_Y + 1; y < SURFACE_Y - 4; y++) {
                    data.setBlock(x, y, z, foundation);
                }
                for (int y = SURFACE_Y - 4; y < SURFACE_Y; y++) {
                    data.setBlock(x, y, z, surface);
                }
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
                return biome;
            }
            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
                return List.of(biome);
            }
        };
    }
}
