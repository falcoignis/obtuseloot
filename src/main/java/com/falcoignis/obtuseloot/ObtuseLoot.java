package com.falcoignis.obtuseloot;

import com.falcoignis.obtuseloot.obtuseengine.ObtuseEngine;

import org.bukkit.plugin.java.JavaPlugin;

public class ObtuseLoot extends JavaPlugin {

    private static ObtuseLoot instance;
    private ObtuseEngine engine;

    @Override
    public void onEnable() {
        instance = this;

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
}
