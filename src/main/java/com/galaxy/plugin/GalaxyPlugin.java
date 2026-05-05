package com.galaxy.plugin;

import com.galaxy.plugin.planets.PlanetManager;
import com.galaxy.plugin.world.WorldBootstrap;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.galaxy.plugin.world.SpaceChunkGenerator;
import com.galaxy.plugin.world.DesertChunkGenerator;

import java.io.File;

public final class GalaxyPlugin extends JavaPlugin {

    private World spaceWorld;
    private World planetEarth;
    private World planetMars;
    private PlanetManager planetManager;

    @Override
    public void onEnable() {
        getLogger().info("Galaxy plugin starting…");

        WorldBootstrap wb = new WorldBootstrap(getLogger());
        spaceWorld = wb.createSpace();
        planetEarth = wb.createPlanetEarth();
        planetMars = wb.createPlanetMars();

        if (spaceWorld == null || planetEarth == null || planetMars == null) {
            getLogger().severe("Galaxy: world bootstrap failed — disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        planetManager = new PlanetManager(getLogger(), spaceWorld);
        File planetsYml = new File(getDataFolder(), "planets.yml");
        planetManager.loadFromFile(planetsYml, getResource("planets.yml"));

        if (planetManager.all().isEmpty()) {
            getLogger().severe("Galaxy: no planets loaded — disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Galaxy plugin enabled. Worlds: " + spaceWorld.getName()
                + ", " + planetEarth.getName() + ", " + planetMars.getName());
    }

    @Override
    public void onDisable() {
        getLogger().info("Galaxy plugin disabled.");
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if (WorldBootstrap.SPACE.equals(worldName)) return new SpaceChunkGenerator();
        if (WorldBootstrap.PLANET_MARS.equals(worldName)) return new DesertChunkGenerator();
        return null;
    }

    public World getSpaceWorld() { return spaceWorld; }
    public World getPlanetEarth() { return planetEarth; }
    public World getPlanetMars() { return planetMars; }
    public PlanetManager getPlanetManager() { return planetManager; }
}
