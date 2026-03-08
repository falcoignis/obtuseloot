package com.falcoignis.obtuseloot.obtuseengine;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class ObtuseEngine {

    private final Plugin plugin;
    private EventCore eventCore;
    private CombatCore combatCore;

    public ObtuseEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.eventCore = new EventCore();
        this.combatCore = new CombatCore();

        Bukkit.getPluginManager().registerEvents(eventCore, plugin);
        Bukkit.getPluginManager().registerEvents(combatCore, plugin);

        plugin.getLogger().info("ObtuseEngine online.");
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
