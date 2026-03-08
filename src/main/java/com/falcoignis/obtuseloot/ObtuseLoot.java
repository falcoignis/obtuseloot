package com.falcoignis.obtuseloot;

import com.falcoignis.obtuseloot.commands.ObtuseLootCommand;
import com.falcoignis.obtuseloot.config.RuntimeSettings;
import com.falcoignis.obtuseloot.obtuseengine.ObtuseEngine;

import org.bukkit.plugin.java.JavaPlugin;

public class ObtuseLoot extends JavaPlugin {

    private static ObtuseLoot instance;
    private ObtuseEngine engine;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        RuntimeSettings.load(getConfig());

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
