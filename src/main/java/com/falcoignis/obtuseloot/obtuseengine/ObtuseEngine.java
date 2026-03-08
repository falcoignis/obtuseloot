package com.falcoignis.obtuseloot.obtuseengine;

import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.reputation.ReputationManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ObtuseEngine {

    private final Plugin plugin;

    public ObtuseEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(new EventCore(), plugin);
        Bukkit.getPluginManager().registerEvents(new CombatCore(), plugin);

        Bukkit.getScheduler().runTaskTimer(plugin,
            () -> {
                DriftEngine.periodicDriftCheck();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ReputationManager.get(player.getUniqueId()).decay();
                }
            },
            600,
            600
        );

        plugin.getLogger().info("ObtuseEngine online.");
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
