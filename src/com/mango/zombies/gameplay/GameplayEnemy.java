package com.mango.zombies.gameplay;

import com.mango.zombies.Main;
import com.mango.zombies.PluginCore;
import com.mango.zombies.Time;
import com.mango.zombies.entities.EnemyEntity;
import com.mango.zombies.gameplay.base.GameplayRegisterable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GameplayEnemy extends GameplayRegisterable {

    //region Fields
    private boolean hasBeenDamaged, hasBeenSpawned;

    private final EnemyEntity enemyEntity;

    private GameplaySession gameplaySession;

    private int bleedOutTaskReference;
    private int currentHealth;
    private int originalHealth;

    private LivingEntity entity;

    private Location spawnLocation;
    //endregion

    //region Getters/Setters
    /**
     * Gets the health of the enemy.
     */
    public int getCurrentHealth() {
        return currentHealth;
    }

    /**
     * Sets the health of the enemy.
     */
    public void setCurrentHealth(int currentHealth) {

        this.currentHealth = currentHealth;
        originalHealth = this.currentHealth;
    }

    /**
     * Gets the entity.
     */
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * Gets the session that this enemy is a part of, if any.
     */
    public GameplaySession getGameplaySession() {
        return gameplaySession;
    }

    /**
     * Sets the session that this enemy is a part of, if any.
     */
    public void setGameplaySession(GameplaySession gameplaySession) {
        this.gameplaySession = gameplaySession;
    }

    /**
     * Gets the spawn location.
     */
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * Sets the spawn location.
     */
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    /**
     * Gets the UUID of this gameplay registerable.
     */
    public UUID getUUID() {
        return entity == null ? null : entity.getUniqueId();
    }
    //endregion

    //region Public Methods
    /**
     * Calculates and applies the enemy's health.
     * @param round The round to calculate the health for.
     */
    public void applyHealth(int round) {
        currentHealth = PluginCore.getGameplayService().calculateHealthForRound(round, enemyEntity.getRoundMultiplier(), enemyEntity.getMaxHealth());
    }

    /**
     * Damages the enemy.
     */
    public void damage(Player player, int damage, String weaponServiceType) {

        if (entity == null)
            return;

        hasBeenDamaged = true;

        currentHealth -= damage;

        spawnBloodEffect(damage / 10);

        int healthThreshold = (int)Math.round(originalHealth * enemyEntity.getBleedOutHealthMultiplier());

        if (currentHealth < healthThreshold && Math.random() * 100 <= enemyEntity.getBleedOutChance()) {

            Main instance = Main.getInstance();

            long time = Time.fromSeconds(1).totalTicks();

            bleedOutTaskReference = instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, this::bleedOut_runnable, time, time);
        }

        GameplayPlayer gameplayPlayer = GameplayPlayer.findGameplayPlayerForPlayer(player, false);

        if (currentHealth > 0) {

            if (gameplaySession != null && gameplayPlayer != null)
                gameplaySession.getGamemode().onEnemyDamaged(gameplayPlayer, this);

            return;
        }

        // Kill the enemy.
        entity.damage(100);

        if (gameplaySession != null && gameplayPlayer != null)
            gameplaySession.getGamemode().onEnemyKilled(gameplayPlayer, this, weaponServiceType);

        PluginCore.getGameplayService().unregister(this);
    }

    /**
     * Despawns the enemy.
     */
    public void despawn() {

        if (entity != null)
            entity.remove();

        PluginCore.getGameplayService().unregister(this);
    }

    /**
     * Spawns the enemy into the world.
     */
    public void spawn() {

        if (spawnLocation == null || hasBeenSpawned || enemyEntity.getEntityType().getEntityClass() == null)
            return;

        World world = spawnLocation.getWorld();

        if (world == null)
            return;

        Entity entity = world.spawn(spawnLocation, enemyEntity.getEntityType().getEntityClass());

        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            return;
        }

        hasBeenSpawned = true;

        this.entity = (LivingEntity)entity;

        PluginCore.getGameplayService().register(this);

        Main instance = Main.getInstance();

        instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, this::despawn_runnable, Time.fromMinutes(enemyEntity.getDespawnTime()).totalTicks());
    }
    //endregion

    //region Runnables
    private void bleedOut_runnable() {

        if (entity == null || currentHealth < 1)
            return;

        spawnBloodEffect(20);

        currentHealth -= (originalHealth * 0.05);

        if (currentHealth > 0)
            entity.damage(0);

        else {

            entity.damage(100);
            Main.getInstance().getServer().getScheduler().cancelTask(bleedOutTaskReference);
        }
    }

    private void despawn_runnable() {

        if (hasBeenDamaged)
            return;

        entity.remove();

        if (gameplaySession != null)
            gameplaySession.getGamemode().onEnemyDespawned(this);
    }
    //endregion

    //region Constructors
    public GameplayEnemy(EnemyEntity enemyEntity) {
        this.enemyEntity = enemyEntity;
    }
    //endregion

    //region Private Methods
    private void spawnBloodEffect(int count) {
        entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, Math.random() > 0.5 ? entity.getEyeLocation() : entity.getLocation(), count, Material.RED_WOOL.createBlockData());
    }
    //endregion
}
