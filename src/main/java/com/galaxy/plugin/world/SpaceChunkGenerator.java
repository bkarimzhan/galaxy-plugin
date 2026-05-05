package com.galaxy.plugin.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public final class SpaceChunkGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
    }

    @Override
    public void generateSurface(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
    }

    @Override
    public void generateCaves(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
    }

    @Override
    public boolean shouldGenerateNoise() { return false; }

    @Override
    public boolean shouldGenerateSurface() { return false; }

    @Override
    public boolean shouldGenerateCaves() { return false; }

    @Override
    public boolean shouldGenerateDecorations() { return false; }

    @Override
    public boolean shouldGenerateMobs() { return false; }

    @Override
    public boolean shouldGenerateStructures() { return false; }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo info) {
        return new BiomeProvider() {
            @Override
            public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
                return Biome.THE_END;
            }

            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
                return List.of(Biome.THE_END);
            }
        };
    }
}
