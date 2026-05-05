package com.galaxy.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class GalaxyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Galaxy plugin enabled (skeleton).");
    }

    @Override
    public void onDisable() {
        getLogger().info("Galaxy plugin disabled.");
    }
}
