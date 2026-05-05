package com.galaxy.plugin;

import com.galaxy.plugin.commands.LeavePlanetCommand;
import com.galaxy.plugin.commands.ShipCommand;
import com.galaxy.plugin.commands.SpaceCommand;
import com.galaxy.plugin.commands.UnshipCommand;
import com.galaxy.plugin.listeners.ProximityTask;
import com.galaxy.plugin.listeners.SpaceMobGuard;
import com.galaxy.plugin.listeners.SpawnListener;
import com.galaxy.plugin.planets.PlanetManager;
import com.galaxy.plugin.planets.SphereBuilder;
import com.galaxy.plugin.ship.ShipController;
import com.galaxy.plugin.teleport.TeleportService;
import com.galaxy.plugin.world.SpawnPlatform;
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
    private TeleportService teleportService;
    private ProximityTask proximityTask;
    private ShipController shipController;

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

        SphereBuilder sphereBuilder = new SphereBuilder(getLogger(), getDataFolder());
        sphereBuilder.buildAllOnce(spaceWorld, planetManager.all().values());

        SpawnPlatform spawnPlatform = new SpawnPlatform(getLogger(), getDataFolder());
        spawnPlatform.buildOnce(planetEarth);
        planetEarth.setSpawnLocation(spawnPlatform.locationIn(planetEarth));
        getServer().getPluginManager().registerEvents(
                new SpawnListener(() -> spawnPlatform.locationIn(planetEarth)), this);
        getServer().getPluginManager().registerEvents(
                new SpaceMobGuard(spaceWorld.getName()), this);

        teleportService = new TeleportService(getLogger(), spaceWorld, planetManager);

        shipController = new ShipController(this);
        teleportService.setShipController(shipController);
        getServer().getPluginManager().registerEvents(shipController, this);

        proximityTask = ProximityTask.schedule(this, planetManager, teleportService, spaceWorld);

        getCommand("space").setExecutor(new SpaceCommand(spaceWorld.getName()));
        getCommand("ship").setExecutor(new ShipCommand(shipController));
        getCommand("unship").setExecutor(new UnshipCommand(shipController));
        getCommand("leaveplanet").setExecutor(new LeavePlanetCommand(teleportService, planetManager));

        getLogger().info("Galaxy plugin enabled. Worlds: " + spaceWorld.getName()
                + ", " + planetEarth.getName() + ", " + planetMars.getName());
    }

    @Override
    public void onDisable() {
        if (proximityTask != null) proximityTask.cancel();
        if (shipController != null) shipController.shutdown();
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
    public TeleportService getTeleportService() { return teleportService; }
    public ShipController getShipController() { return shipController; }
}
