package obtuseloot.obtuseengine;

import obtuseloot.combat.CombatCore;
import obtuseloot.events.EventCore;
import obtuseloot.events.ArtifactItemStorageListener;
import obtuseloot.events.PlayerJoinLoadListener;
import obtuseloot.events.PlayerStateCleanupListener;
import obtuseloot.events.ReputationFeedListener;
import obtuseloot.events.NonCombatAbilityListener;
import obtuseloot.events.LootInsertionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class ObtuseEngine {

    private final Plugin plugin;

    public ObtuseEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(new ReputationFeedListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new CombatCore(), plugin);
        Bukkit.getPluginManager().registerEvents(new EventCore(), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLoadListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerStateCleanupListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new ArtifactItemStorageListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new NonCombatAbilityListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new LootInsertionListener(), plugin);
        plugin.getLogger().info("ObtuseEngine online.");
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
