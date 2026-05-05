package com.galaxy.plugin.world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SingleBiomeProvider extends BiomeProvider {

    private final Biome biome;

    public SingleBiomeProvider(Biome biome) {
        this.biome = biome;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
        return biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
        return List.of(biome);
    }
}
