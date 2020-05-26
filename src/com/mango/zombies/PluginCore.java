package com.mango.zombies;

import com.mango.zombies.entities.*;
import com.mango.zombies.services.base.FilingService;
import com.mango.zombies.services.base.GameplayService;
import com.mango.zombies.services.base.MessagingService;

import java.util.ArrayList;
import java.util.List;

public class PluginCore {

    //region Static Fields
    private static ConfigEntity config;

    private static EnemyConfigEntity enemyConfig;

    private static FilingService filingService;

    private static GameplayConfigEntity gameplayConfig;

    private static GameplayService gameplayService;

    private static final List<EnemyEntity> enemies = new ArrayList<EnemyEntity>();
    private static final List<MapEntity> maps = new ArrayList<MapEntity>();
    private static final List<PerkEntity> perks = new ArrayList<PerkEntity>();
    private static final List<WeaponEntity> weapons = new ArrayList<WeaponEntity>();

    private static MapConfigEntity mapConfig;

    private static MessagingService messagingService;

    private static PerkConfigEntity perkConfig;

    private static WeaponConfigEntity weaponConfig;
    //endregion

    //region Static Getters/Setters
    /**
     * Gets the plugin's configuration.
     */
    public static ConfigEntity getConfig() {
        return config;
    }

    /**
     * Sets the plugin's configuration.
     */
    public static void setConfig(ConfigEntity config) {
        PluginCore.config = config;
    }

    /**
     * Gets the enemy configuration.
     */
    public static EnemyConfigEntity getEnemyConfig() {
        return enemyConfig;
    }

    /**
     * Sets the enemy configuration.
     */
    public static void setEnemyConfig(EnemyConfigEntity enemyConfig) {
        PluginCore.enemyConfig = enemyConfig;
    }

    /**
     * Gets the filing service.
     */
    public static FilingService getFilingService() {
        return filingService;
    }

    /**
     * Sets the filing service.
     */
    public static void setFilingService(FilingService filingService) {
        PluginCore.filingService = filingService;
    }

    /**
     * Gets the gamemode config.
     */
    public static GameplayConfigEntity getGameplayConfig() {
        return gameplayConfig;
    }

    /**
     * Sets the gamemode config.
     */
    public static void setGameplayConfig(GameplayConfigEntity gameplayConfig) {
        PluginCore.gameplayConfig = gameplayConfig;
    }

    /**
     * Gets the gameplay service.
     */
    public static GameplayService getGameplayService() {
        return gameplayService;
    }

    /**
     * Sets the gameplay service.
     */
    public static void setGameplayService(GameplayService gameplayService) {
        PluginCore.gameplayService = gameplayService;
    }

    /**
     * Gets the enemies.
     */
    public static EnemyEntity[] getEnemies() {
        return enemies.toArray(new EnemyEntity[0]);
    }

    /**
     * Gets the perks.
     */
    public static PerkEntity[] getPerks() {
        return perks.toArray(new PerkEntity[0]);
    }

    /**
     * Gets the maps.
     */
    public static MapEntity[] getMaps() {
        return maps.toArray(new MapEntity[0]);
    }

    /**
     * Gets the messaging service.
     */
    public static MessagingService getMessagingService() {
        return messagingService;
    }

    /**
     * Sets the messaging service.
     */
    public static void setMessagingService(MessagingService messagingService) {
        PluginCore.messagingService = messagingService;
    }

    /**
     * Gets the perk configuration.
     */
    public static PerkConfigEntity getPerkConfig() {
        return perkConfig;
    }

    /**
     * Sets the perk configuration.
     */
    public static void setPerkConfig(PerkConfigEntity perkConfig) {
        PluginCore.perkConfig = perkConfig;
    }

    /**
     * Gets the weapons.
     */
    public static WeaponEntity[] getWeapons() {
        return weapons.toArray(new WeaponEntity[0]);
    }

    /**
     * Gets the map configuration.
     */
    public static MapConfigEntity getMapConfig() {
        return mapConfig;
    }

    /**
     * Sets the map configuration.
     */
    public static void setMapConfig(MapConfigEntity mapConfig) {
        PluginCore.mapConfig = mapConfig;
    }

    /**
     * Gets the weapon configuration.
     */
    public static WeaponConfigEntity getWeaponConfig() {
        return weaponConfig;
    }

    /**
     * Sets the weapon configuration.
     */
    public static void setWeaponConfig(WeaponConfigEntity weaponConfig) {
        PluginCore.weaponConfig = weaponConfig;
    }
    //endregion

    //region Public Static Methods
    /**
     * Adds an enemy.
     */
    public static void addEnemy(EnemyEntity enemy) {
        enemies.add(enemy);
    }

    /**
     * Adds a map.
     */
    public static void addMap(MapEntity map) {
        maps.add(map);
    }

    /**
     * Adds a perk.
     */
    public static void addPerk(PerkEntity perk) {
        perks.add(perk);
    }

    /**
     * Adds a weapon.
     */
    public static void addWeapon(WeaponEntity weapon) {
        weapons.add(weapon);
    }

    /**
     * Ran by the auto save timer.
     */
    public static void autoSave() {

        Log.information("Auto save started.");

        filingService.saveEverything();

        Log.information("Auto save completed.");
    }

    /**
     * Removes an enemy.
     */
    public static void removeEnemy(EnemyEntity enemy) {
        enemies.remove(enemy);
    }

    /**
     * Removes a map.
     */
    public static void removeMap(MapEntity map) {
        maps.remove(map);
    }

    /**
     * Removes a perk.
     */
    public static void removePerk(PerkEntity perk) {
        perks.remove(perk);
    }

    /**
     * Removes a weapon.
     */
    public static void removeWeapon(WeaponEntity weapon) {
        weapons.remove(weapon);
    }
    //endregion
}