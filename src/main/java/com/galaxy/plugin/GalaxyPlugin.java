package com.galaxy.plugin;

import com.galaxy.plugin.commands.GotoCommand;
import com.galaxy.plugin.commands.LeavePlanetCommand;
import com.galaxy.plugin.commands.PlanetsCommand;
import com.galaxy.plugin.commands.ShipCommand;
import com.galaxy.plugin.commands.SpaceCommand;
import com.galaxy.plugin.commands.UnshipCommand;
import com.galaxy.plugin.listeners.EndStructureCleanup;
import com.galaxy.plugin.listeners.GravityListener;
import com.galaxy.plugin.listeners.MoonOrbitTask;
import com.galaxy.plugin.listeners.NightVisionListener;
import com.galaxy.plugin.listeners.ProximityTask;
import com.galaxy.plugin.listeners.SightTask;
import com.galaxy.plugin.listeners.SpaceMobGuard;
import com.galaxy.plugin.listeners.SpawnListener;
import com.galaxy.plugin.listeners.StarParticleTask;
import com.galaxy.plugin.planets.PlanetManager;
import com.galaxy.plugin.planets.SphereBuilder;
import com.galaxy.plugin.ship.ShipController;
import com.galaxy.plugin.teleport.TeleportService;
import com.galaxy.plugin.world.FlatPlanetGenerator;
import com.galaxy.plugin.world.LavaChunkGenerator;
import com.galaxy.plugin.world.SpawnPlatform;
import com.galaxy.plugin.world.WorldBootstrap;
import org.bukkit.Material;
import org.bukkit.block.Biome;
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
    private World planetSun;
    private PlanetManager planetManager;
    private TeleportService teleportService;
    private ProximityTask proximityTask;
    private ShipController shipController;
    private StarParticleTask starParticleTask;
    private SightTask sightTask;
    private MoonOrbitTask moonOrbitTask;

    @Override
    public void onEnable() {
        getLogger().info("Galaxy plugin starting…");

        WorldBootstrap wb = new WorldBootstrap(getLogger());
        spaceWorld = wb.createSpace(this);
        planetEarth = wb.createPlanetEarth();
        planetMars = wb.createPlanetMars();
        planetSun = wb.createPlanetSun();
        wb.createFlatPlanet(WorldBootstrap.PLANET_MERCURY, mercuryGen(), 65, 500, "stone-rocky");
        wb.createFlatPlanet(WorldBootstrap.PLANET_VENUS, venusGen(), 65, 500, "granite-volcanic");
        wb.createFlatPlanet(WorldBootstrap.PLANET_JUPITER, jupiterGen(), 65, 500, "gas-giant");
        wb.createFlatPlanet(WorldBootstrap.PLANET_SATURN, saturnGen(), 65, 500, "gas-giant");
        wb.createFlatPlanet(WorldBootstrap.PLANET_URANUS, uranusGen(), 65, 500, "ice-giant");
        wb.createFlatPlanet(WorldBootstrap.PLANET_NEPTUNE, neptuneGen(), 65, 500, "ice-giant");

        if (spaceWorld == null || planetEarth == null || planetMars == null || planetSun == null) {
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
                new SpawnListener(
                        () -> spawnPlatform.locationIn(planetEarth),
                        java.util.Set.of(spaceWorld.getName(), planetSun.getName())),
                this);
        getServer().getPluginManager().registerEvents(
                new SpaceMobGuard(spaceWorld.getName()), this);
        EndStructureCleanup endCleanup = new EndStructureCleanup(this, spaceWorld.getName());
        getServer().getPluginManager().registerEvents(endCleanup, this);
        endCleanup.wipeNow(spaceWorld);

        getServer().getPluginManager().registerEvents(
                new NightVisionListener(this, spaceWorld.getName()), this);
        getServer().getPluginManager().registerEvents(new GravityListener(this), this);

        teleportService = new TeleportService(getLogger(), spaceWorld, planetManager);

        shipController = new ShipController(this);
        teleportService.setShipController(shipController);
        getServer().getPluginManager().registerEvents(shipController, this);

        proximityTask = ProximityTask.schedule(this, planetManager, teleportService, spaceWorld);
        starParticleTask = StarParticleTask.schedule(this, spaceWorld.getName());
        sightTask = SightTask.schedule(this, spaceWorld.getName(), planetManager);
        moonOrbitTask = MoonOrbitTask.schedule(this, spaceWorld, planetManager);

        getCommand("space").setExecutor(new SpaceCommand(spaceWorld.getName()));
        getCommand("ship").setExecutor(new ShipCommand(shipController));
        getCommand("unship").setExecutor(new UnshipCommand(shipController));
        getCommand("leaveplanet").setExecutor(new LeavePlanetCommand(teleportService, planetManager));
        GotoCommand gotoCmd = new GotoCommand(teleportService, planetManager);
        getCommand("goto").setExecutor(gotoCmd);
        getCommand("goto").setTabCompleter(gotoCmd);
        getCommand("planets").setExecutor(new PlanetsCommand(teleportService, planetManager));

        getLogger().info("Galaxy plugin enabled. Worlds: " + spaceWorld.getName()
                + ", " + planetEarth.getName() + ", " + planetMars.getName());
    }

    @Override
    public void onDisable() {
        if (proximityTask != null) proximityTask.cancel();
        if (starParticleTask != null) starParticleTask.cancel();
        if (sightTask != null) sightTask.cancel();
        if (moonOrbitTask != null) moonOrbitTask.shutdown();
        if (shipController != null) shipController.shutdown();
        getLogger().info("Galaxy plugin disabled.");
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return switch (worldName) {
            case WorldBootstrap.SPACE -> new SpaceChunkGenerator();
            case WorldBootstrap.PLANET_MARS -> new DesertChunkGenerator();
            case WorldBootstrap.PLANET_SUN -> new LavaChunkGenerator();
            case WorldBootstrap.PLANET_MERCURY -> mercuryGen();
            case WorldBootstrap.PLANET_VENUS -> venusGen();
            case WorldBootstrap.PLANET_JUPITER -> jupiterGen();
            case WorldBootstrap.PLANET_SATURN -> saturnGen();
            case WorldBootstrap.PLANET_URANUS -> uranusGen();
            case WorldBootstrap.PLANET_NEPTUNE -> neptuneGen();
            default -> null;
        };
    }

    private static FlatPlanetGenerator mercuryGen() { return new FlatPlanetGenerator(Material.STONE, Material.STONE, Biome.STONY_PEAKS); }
    private static FlatPlanetGenerator venusGen()   { return new FlatPlanetGenerator(Material.GRANITE, Material.STONE, Biome.SAVANNA); }
    private static FlatPlanetGenerator jupiterGen() { return new FlatPlanetGenerator(Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA, Biome.WARM_OCEAN); }
    private static FlatPlanetGenerator saturnGen()  { return new FlatPlanetGenerator(Material.SMOOTH_SANDSTONE, Material.SANDSTONE, Biome.WARM_OCEAN); }
    private static FlatPlanetGenerator uranusGen()  { return new FlatPlanetGenerator(Material.PACKED_ICE, Material.BLUE_ICE, Biome.FROZEN_OCEAN); }
    private static FlatPlanetGenerator neptuneGen() { return new FlatPlanetGenerator(Material.BLUE_ICE, Material.PACKED_ICE, Biome.DEEP_FROZEN_OCEAN); }

    public World getSpaceWorld() { return spaceWorld; }
    public World getPlanetEarth() { return planetEarth; }
    public World getPlanetMars() { return planetMars; }
    public PlanetManager getPlanetManager() { return planetManager; }
    public TeleportService getTeleportService() { return teleportService; }
    public ShipController getShipController() { return shipController; }
}
