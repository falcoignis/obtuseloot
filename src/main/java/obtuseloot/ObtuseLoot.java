package obtuseloot;

import obtuseloot.commands.ObtuseLootCommand;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.names.NamePoolManager;
import obtuseloot.obtuseengine.ObtuseEngine;

import org.bukkit.plugin.java.JavaPlugin;

public class ObtuseLoot extends JavaPlugin {

    private static ObtuseLoot instance;
    private ObtuseEngine engine;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure default config.yml is persisted before loading runtime snapshots.
        saveDefaultConfig();
        RuntimeSettings.load(getConfig());

        // Ensure configurable name list files are present on disk and loaded.
        NamePoolManager.initialize(this);

        if (getCommand("obtuseloot") != null) {
            ObtuseLootCommand command = new ObtuseLootCommand(this);
            getCommand("obtuseloot").setExecutor(command);
            getCommand("obtuseloot").setTabCompleter(command);
        } else {
            getLogger().warning("Command 'obtuseloot' is missing from plugin.yml; command wiring skipped.");
        }

        engine = new ObtuseEngine(this);
        engine.initialize();

        getLogger().info("ObtuseLoot initialized.");
    }

    public static ObtuseLoot get() {
        return instance;
    }

    public ObtuseEngine getEngine() {
        return engine;
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.shutdown();
        }
    }
}
