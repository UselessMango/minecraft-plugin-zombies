package com.mango.zombies.gameplay;

import com.mango.zombies.PluginCore;
import com.mango.zombies.gameplay.base.BaseGameplayRegisterable;
import com.mango.zombies.gameplay.base.InventoryPickupItemEventRegisterable;
import com.mango.zombies.gameplay.base.ProjectileHitEventRegisterable;
import com.mango.zombies.schema.DamagerType;
import com.mango.zombies.schema.ProjectileConfigComponent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

import java.util.HashMap;
import java.util.UUID;

public class GameplayProjectile extends BaseGameplayRegisterable implements ProjectileHitEventRegisterable, InventoryPickupItemEventRegisterable {

    //region Fields
    private final GameplayPlayer gameplayPlayer;

    private final HashMap<String, Object> configuration = new HashMap<String, Object>();

    private final UUID uuid;
    //endregion

    //region Getters/Setters
    /**
     * Gets the projectile configuration.
     */
    public HashMap<String, Object> getConfiguration() {
        return configuration;
    }

    /**
     * Gets the UUID of this gameplay registerable.
     */
    public UUID getUUID() {
        return uuid;
    }
    //endregion

    //region Event Handlers
    /**
     * Called when an inventory picks up an item.
     * @param event The event caused by the pickup.
     */
    public void onInventoryPickUpItem(InventoryPickupItemEvent event) {
        event.setCancelled(true);
    }

    /**
     * Called when a projectile hits something.
     * @param event The event.
     */
    public void onProjectileHit(ProjectileHitEvent event) {

        if (!(event.getHitEntity() instanceof LivingEntity))
            return;

        LivingEntity impactEntity = (LivingEntity)event.getHitEntity();

        BaseGameplayRegisterable gameplayRegisterable = PluginCore.getGameplayService().findRegisterableByUUID(impactEntity.getUniqueId());

        if (!(gameplayRegisterable instanceof GameplayEnemy))
            return;

        GameplayEnemy gameplayEnemy = (GameplayEnemy)gameplayRegisterable;

        if (configuration.containsKey(ProjectileConfigComponent.DAMAGE))
            gameplayEnemy.damage(gameplayPlayer, (int)configuration.get(ProjectileConfigComponent.DAMAGE), DamagerType.GUNSHOT);

        unregister();
    }
    //endregion

    //region Constructors
    public GameplayProjectile(GameplayPlayer gameplayPlayer) {

        this.gameplayPlayer = gameplayPlayer;

        uuid = UUID.randomUUID();
    }
    //endregion
}
