package com.mango.zombies;

import com.mango.zombies.commands.*;
import com.mango.zombies.listeners.PlayerClickListener;
import com.mango.zombies.listeners.SignChangedListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.TimerTask;

public class Main extends JavaPlugin {

    //region Static Fields
    private static Main instance;
    //endregion

    //region Static Getters/Setters
    /**
     * Gets the plugin instance.
     */
    public static Main getInstance() {
        return instance;
    }
    //endregion

    //region Lifecycle
    @Override
    public void onEnable() {

        PluginCore.setDescriptionFile(getDescription());
        PluginCore.setupFolders(getDataFolder());

        PluginCore.importConfig();
        PluginCore.importMaps();
        PluginCore.importPerks();
        PluginCore.importWeaponClasses();
        PluginCore.importWeapons();

        PluginCore.enableMaps();

        registerCommands();
        registerEvents();

        int delay = PluginCore.getConfig().getAutoSaveTimerInterval() * 60 * 1000;

        PluginCore.getAutoSaveTimer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                PluginCore.autoSave();
            }
        }, delay, delay);

        instance = this;
    }

    @Override
    public void onDisable() {

        PluginCore.saveConfig();
        PluginCore.saveMaps();
        PluginCore.savePerks();
        PluginCore.saveWeaponClasses();
        PluginCore.saveWeapons();
    }
    //endregion

    //region Public Methods
    /**
     * Registers all plugin commands with corresponding executors.
     */
    public void registerCommands() {

        this.getCommand("info").setExecutor(new InfoCommandExecutor());
        this.getCommand("mapinfo").setExecutor(new MapInfoCommandExecutor());
        this.getCommand("createmap").setExecutor(new CreateMapCommandExecutor());
        this.getCommand("createweaponclass").setExecutor(new CreateWeaponClassCommandExecutor());
        this.getCommand("createweapon").setExecutor(new CreateWeaponCommandExecutor());
        this.getCommand("createperk").setExecutor(new CreatePerkCommandExecutor());
        this.getCommand("getweapon").setExecutor(new GetWeaponCommandExecutor());
        this.getCommand("setposition").setExecutor(new SetPositionCommandExecutor());
        this.getCommand("deletemap").setExecutor(new DeleteMapCommandExecutor());
    }

    /**
     * Registers all plugin event handlers.
     */
    public void registerEvents() {

        Bukkit.getPluginManager().registerEvents(new PlayerClickListener(), this);
        Bukkit.getPluginManager().registerEvents(new SignChangedListener(), this);
    }
    //endregion
}