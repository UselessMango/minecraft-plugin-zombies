package com.mango.zombies.gameplay;

import com.mango.zombies.Main;
import com.mango.zombies.PluginCore;
import com.mango.zombies.Time;
import com.mango.zombies.entities.WeaponEntity;
import com.mango.zombies.entities.WeaponServiceEntity;
import com.mango.zombies.gameplay.base.BaseGameplayRegisterable;
import com.mango.zombies.gameplay.base.EntityDamageByEntityEventRegisterable;
import com.mango.zombies.gameplay.base.PlayerInteractEventRegisterable;
import com.mango.zombies.helper.HiddenStringUtils;
import com.mango.zombies.helper.SoundUtil;
import com.mango.zombies.schema.DamagerType;
import com.mango.zombies.schema.ProjectileConfigComponent;
import com.mango.zombies.schema.WeaponService;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameplayWeapon extends BaseGameplayRegisterable implements PlayerInteractEventRegisterable, EntityDamageByEntityEventRegisterable {

    //region Fields
    private boolean isPackAPunched;
    private boolean isReloading;

    private GameplayPlayer gameplayPlayer;

    private int availableAmmo;
    private int currentAmmo;

    private Player player;

    private final WeaponEntity weaponEntity;

    private final UUID uuid;
    //endregion

    //region Getters/Setters
    /**
     * Gets the gameplay player using this weapon, if any.
     */
    public GameplayPlayer getGameplayPlayer() {
        return gameplayPlayer;
    }

    /**
     * Sets the gameplay player using this weapon.
     */
    public void setGameplayPlayer(GameplayPlayer gameplayPlayer) {

        this.gameplayPlayer = gameplayPlayer;

        player = this.gameplayPlayer.getPlayer();
    }

    /**
     * Gets whether this weapon is Pack-A-Punched.
     */
    public boolean isPackAPunched() {
        return isPackAPunched;
    }

    /**
     * Gets the UUID of this gameplay registerable.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Gets the weapon entity.
     */
    public WeaponEntity getWeaponEntity() {
        return weaponEntity;
    }
    //endregion

    //region Event Handlers
    /**
     * Called when an entity is damaged by another entity.
     * @param event The event.
     */
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity))
            return;

        player = (Player)event.getDamager();
        LivingEntity livingEntity = (LivingEntity)event.getEntity();

        WeaponServiceEntity meleeService = weaponEntity.getService(WeaponService.MELEE, isPackAPunched);

        if (meleeService == null) {
            event.setCancelled(true);
            return;
        }

        BaseGameplayRegisterable gameplayEnemyRegisterable = PluginCore.getGameplayService().findRegisterableByUUID(livingEntity.getUniqueId());

        if (!(gameplayEnemyRegisterable instanceof GameplayEnemy))
            return;

        GameplayEnemy gameplayEnemy = (GameplayEnemy)gameplayEnemyRegisterable;

        boolean isWithinRangeX = isWithinRange(player.getLocation().getX(), livingEntity.getLocation().getX());
        boolean isWithinRangeY = isWithinRange(player.getLocation().getY(), livingEntity.getLocation().getY());
        boolean isWithinRangeZ = isWithinRange(player.getLocation().getZ(), livingEntity.getLocation().getZ());

        if (!isWithinRangeX || !isWithinRangeY || !isWithinRangeZ) {
            event.setCancelled(true);
            return;
        }

        livingEntity.setHealth(20);
        gameplayEnemy.damage(gameplayPlayer, meleeService.getDamage(), DamagerType.MELEE);
    }

    /**
     * Called when a player interacts with this weapon.
     * @param event The event caused by the interaction.
     */
    public void onPlayerInteract(PlayerInteractEvent event) {

        player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            shoot(player);
            return;
        }

        if (melee(player))
            return;

        reload(event.getPlayer());
        event.setCancelled(true);
    }
    //endregion

    //region Public Methods
    /**
     * Gets whether this weapon can be Pack-A-Punched.
     */
    public boolean canPackAPunch() {
        return !isPackAPunched && (weaponEntity.getService(WeaponService.GUNSHOT, true) != null || weaponEntity.getService(WeaponService.MELEE, true) != null);
    }

    /**
     * Gives a player this weapon as a usable item.
     * @param player The player to give the weapon to.
     */
    public void giveItemStack(Player player) {
        giveItemStack(player, -1);
    }

    /**
     * Gives a player this weapon as a usable item.
     * @param player The player to give the weapon to.
     * @param inventoryPosition The inventory position to put the item in, or -1.
     */
    public void giveItemStack(Player player, int inventoryPosition) {

        this.player = player;

        ItemStack itemStack = new ItemStack(weaponEntity.getItem(), 1);

        ItemMeta itemMeta = itemStack.getItemMeta();

        List<String> lore = new ArrayList<String>();
        lore.add(HiddenStringUtils.encodeString(uuid.toString()));

        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);

        setWeaponDisplay(itemStack, weaponEntity.getService(WeaponService.GUNSHOT, isPackAPunched) == null ? null : getAmmoStatus());

        if (inventoryPosition < 0)
            player.getInventory().addItem(itemStack);
        else
            player.getInventory().setItem(inventoryPosition, itemStack);
    }

    /**
     * Melees the weapon.
     * @param player The player meleeing the weapon.
     */
    public boolean melee(Player player) {

        this.player = player;

        WeaponServiceEntity meleeService = weaponEntity.getService(WeaponService.MELEE, isPackAPunched);

        if (meleeService == null)
            return false;

        SoundUtil.playGlobalSound(player.getLocation(), meleeService.getUsageSound(), 1f);

        return true;
    }

    /**
     * Reloads the weapon.
     * @param player The player reloading the weapon.
     */
    public boolean reload(Player player) {

        this.player = player;

        WeaponServiceEntity gunshotService = weaponEntity.getService(WeaponService.GUNSHOT, isPackAPunched);

        if (gunshotService == null)
            return false;

        if (isReloading || currentAmmo == weaponEntity.getMagazineCapacity(gunshotService))
            return false;

        SoundUtil.playGlobalSound(player.getLocation(), weaponEntity.getOutOfAmmoSound(gunshotService), 1f);

        if (availableAmmo < 1) {
            setWeaponDisplay(fetchItemStack(), currentAmmo > 0 ? getAmmoStatus() : getNoAmmoStatus());
            return false;
        }

        isReloading = true;

        setWeaponDisplay(fetchItemStack(), getReloadingStatus());

        Main instance = Main.getInstance();

        instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, this::reload_runnable, Time.fromSeconds(weaponEntity.getReloadSpeed(gunshotService)).totalTicks());

        return true;
    }

    /**
     * Shoots the weapon.
     * @param player The player who shot the weapon.
     */
    public boolean shoot(Player player) {

        this.player = player;

        WeaponServiceEntity gunshotService = weaponEntity.getService(WeaponService.GUNSHOT, isPackAPunched);

        if (gunshotService == null)
            return false;

        if (isReloading) {
            SoundUtil.playGlobalSound(player.getLocation(), weaponEntity.getOutOfAmmoSound(gunshotService), 1);
            return false;
        }

        if (currentAmmo < 1) {
            reload(player);
            return false;
        }

        SoundUtil.playGlobalSound(player.getLocation(), gunshotService.getUsageSound(), 4f);

        int damage = gunshotService.getDamage();

        double accuracy = weaponEntity.getAccuracy(gunshotService);
        int projectiles = weaponEntity.getProjectileCount(gunshotService);

        // The accuracy setting is actually an inaccuracy setting so it needs to be inverted before we use it on a projectile.
        accuracy = 1 - (accuracy / 100.0);

        for (int i = 0; i < projectiles; i++) {

            GameplayProjectile gameplayProjectile = new GameplayProjectile(gameplayPlayer);

            gameplayProjectile.getConfiguration().put(ProjectileConfigComponent.DAMAGE, damage);
            gameplayProjectile.register();

            Projectile projectile = player.launchProjectile(weaponEntity.getProjectileType(gunshotService));

            Vector vector = player.getLocation().getDirection().multiply(2.5);
            vector.add(new Vector(Math.random() * accuracy - accuracy, Math.random() * accuracy - accuracy, Math.random() * accuracy - accuracy));

            projectile.setVelocity(vector);
            projectile.setGravity(false);

            projectile.setCustomName(gameplayProjectile.getUUID().toString());
        }

        currentAmmo--;
        setWeaponDisplay(fetchItemStack(), getAmmoStatus());

        if (currentAmmo < 1)
            reload(player);

        return true;

    }
    //endregion

    //region Runnables
    private void reload_runnable() {

        int defaultAmmoInMagazine = weaponEntity.getMagazineCapacity(isPackAPunched);

        if (availableAmmo > defaultAmmoInMagazine) {

            availableAmmo -= defaultAmmoInMagazine - currentAmmo;
            currentAmmo = defaultAmmoInMagazine;

        } else {

            while (currentAmmo < defaultAmmoInMagazine && availableAmmo > 0) {
                currentAmmo++;
                availableAmmo--;
            }
        }

        isReloading = false;

        setWeaponDisplay(fetchItemStack(), getAmmoStatus());
    }
    //endregion

    //region Constructors
    public GameplayWeapon(WeaponEntity weaponEntity) {

        this.weaponEntity = weaponEntity;

        uuid = UUID.randomUUID();

        setInitialMagazineCount();
    }
    //endregion

    //region Private Methods
    private ItemStack fetchItemStack() {

        if (player == null)
            return null;

        for (int i = 0; i < player.getInventory().getSize(); i++) {

            ItemStack itemStack = player.getInventory().getItem(i);

            if (itemStack != null && HiddenStringUtils.extractUuidFromItemStack(itemStack).equals(uuid))
                return itemStack;
        }

        return null;
    }

    private String getAmmoStatus() {
        return PluginCore.getWeaponConfig().getAmmoIndicatorColor().toString() + currentAmmo + "/" + availableAmmo;
    }

    private String getNoAmmoStatus() {
        return PluginCore.getWeaponConfig().getOutOfAmmoIndicatorColor() + "No ammo";
    }

    private String getReloadingStatus() {
        return PluginCore.getWeaponConfig().getReloadingIndicatorColor() + "Reloading";
    }

    private boolean isWithinRange(double positionOne, double positionTwo) {
        return Math.max(positionOne, positionTwo) - Math.min(positionOne, positionTwo) < (double)3;
    }

    private void setInitialMagazineCount() {

        WeaponServiceEntity gunshotService = weaponEntity.getService(WeaponService.GUNSHOT, isPackAPunched);

        if (gunshotService == null)
            return;

        int initialMagazineCount = weaponEntity.getInitialMagazineCount(gunshotService);
        int magazineCapacity = weaponEntity.getMagazineCapacity(gunshotService);
        int totalAmmoCapacity = weaponEntity.getTotalAmmoCapacity(gunshotService);

        int availableAmmo = (initialMagazineCount - 1) * magazineCapacity;

        this.currentAmmo = magazineCapacity;
        this.availableAmmo = Math.min(availableAmmo, totalAmmoCapacity);
    }

    private void setWeaponDisplay(ItemStack itemStack, String status) {

        if (itemStack == null)
            return;

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null)
            return;

        String title = ChatColor.RESET + "" + weaponEntity.getWeaponColor() + weaponEntity.getName();

        if (status != null && !status.isEmpty())
            title += ": " + ChatColor.RESET + "" + status;

        itemMeta.setDisplayName(title);
        itemStack.setItemMeta(itemMeta);
    }
    //endregion
}
