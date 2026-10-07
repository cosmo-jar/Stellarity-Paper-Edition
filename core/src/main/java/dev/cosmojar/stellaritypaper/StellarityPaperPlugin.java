package dev.cosmojar.stellaritypaper;

import dev.cosmojar.stellaritypaper.bootstrap.PluginBootstrap;
import dev.cosmojar.stellaritypaper.config.ConfigAutoUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class StellarityPaperPlugin extends JavaPlugin {

    private PluginBootstrap bootstrap;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        ConfigAutoUpdater.update(this, "config.yml");
        ConfigAutoUpdater.update(this, "messages_RU.yml");
        ConfigAutoUpdater.update(this, "messages_EN.yml");
        this.bootstrap = new PluginBootstrap(this);
        this.bootstrap.load();
    }

    @Override
    public void onEnable() {
        if (this.bootstrap == null) {
            this.bootstrap = new PluginBootstrap(this);
        }
        this.bootstrap.enable();
        int pluginId = 34547;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.disable();
        }
    }

    public java.io.File getPluginJarFile() {
        return super.getFile();
    }
}
