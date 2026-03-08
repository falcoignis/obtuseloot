package com.obtuseloot;

import com.obtuseloot.commands.ObtuseLootCommand;
import com.obtuseloot.config.RuntimeSettings;
import com.obtuseloot.names.NamePoolManager;
import com.obtuseloot.obtuseengine.ObtuseEngine;

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
            getCommand("obtuseloot").setExecutor(new ObtuseLootCommand());
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
