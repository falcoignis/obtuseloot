package com.obtuseloot.obtuseengine;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class ObtuseEngine {

    private final Plugin plugin;

    public ObtuseEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(new EventCore(), plugin);
        Bukkit.getPluginManager().registerEvents(new CombatCore(), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerStateCleanupListener(), plugin);

        plugin.getLogger().info("ObtuseEngine online.");
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
