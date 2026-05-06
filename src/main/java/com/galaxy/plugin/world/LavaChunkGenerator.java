package com.galaxy.plugin.world;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public final class LavaChunkGenerator extends ChunkGenerator {

    private static final int BEDROCK_Y = -64;
    private static final int LAVA_TOP_Y = 62;
    private static final int SURFACE_Y = 63;

    private static final BlockData LAVA_SOURCE;
    static {
        Levelled l = (Levelled) Material.LAVA.createBlockData();
        l.setLevel(0);
        LAVA_SOURCE = l;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                data.setBlock(x, BEDROCK_Y, z, Material.BEDROCK);
                for (int y = BEDROCK_Y + 1; y <= LAVA_TOP_Y; y++) {
                    data.setBlock(x, y, z, LAVA_SOURCE);
                }
                data.setBlock(x, SURFACE_Y, z, Material.MAGMA_BLOCK);
            }
        }
    }

    @Override
    public void generateSurface(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {}

    @Override
    public void generateBedrock(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {}

    @Override
    public void generateCaves(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {}

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
                return Biome.NETHER_WASTES;
            }

            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
                return List.of(Biome.NETHER_WASTES);
            }
        };
    }
}
