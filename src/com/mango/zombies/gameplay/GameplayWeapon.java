package com.mango.zombies.gameplay;

import com.mango.zombies.Main;
import com.mango.zombies.PluginCore;
import com.mango.zombies.Time;
import com.mango.zombies.entities.WeaponClassEntity;
import com.mango.zombies.entities.WeaponEntity;
import com.mango.zombies.entities.WeaponServiceCharacteristicEntity;
import com.mango.zombies.entities.WeaponServiceEntity;
import com.mango.zombies.helper.HiddenStringUtils;
import com.mango.zombies.listeners.ProjectileHitListener;
import com.mango.zombies.schema.WeaponCharacteristic;
import com.mango.zombies.schema.WeaponService;
import net.minecraft.server.v1_14_R1.BiomeMushroomIslandShore;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

public class GameplayWeapon implements Listener {

    //region Fields
    private int ammoInMagazine = WeaponClassEntity.DEFAULT_MAGAZINE_SIZE, availableAmmo = WeaponClassEntity.DEFAULT_TOTAL_AMMO_CAPACITY;
    private int projectileCount = WeaponClassEntity.DEFAULT_PROJECTILE_COUNT, packAPunchedProjectileCount = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_PROJECTILE_COUNT;
    private int reloadSpeed = WeaponClassEntity.DEFAULT_RELOAD_SPEED, packAPunchedReloadSpeed = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_RELOAD_SPEED;

    private int defaultAmmoInMagazine = WeaponClassEntity.DEFAULT_MAGAZINE_SIZE, defaultAvailableAmmo = WeaponClassEntity.DEFAULT_TOTAL_AMMO_CAPACITY;
    private int defaultPackAPunchedAmmoInMagazine = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_MAGAZINE_SIZE, defaultPackAPunchedAvailableAmmo = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_TOTAL_AMMO_CAPACITY;

    private Sound gunshotSound = WeaponClassEntity.DEFAULT_GUNSHOT_USAGE_SOUND, packAPunchedGunshotSound = WeaponClassEntity.DEFAULT_GUNSHOT_USAGE_SOUND;
    private Sound meleeSound = WeaponClassEntity.DEFAULT_MELEE_USAGE_SOUND, packAPunchedMeleeSound = WeaponClassEntity.DEFAULT_MELEE_USAGE_SOUND;
    private Sound outOfAmmoSound = WeaponClassEntity.DEFAULT_OUT_OF_AMMO_SOUND, packAPunchedOutOfAmmoSound = WeaponClassEntity.DEFAULT_OUT_OF_AMMO_SOUND;

    private WeaponServiceEntity gunshotService, packAPunchedGunshotService, meleeService, packAPunchedMeleeService;
    private boolean isPackAPunched, isReloading;

    private Player player;
    private WeaponEntity weapon;
    private UUID uuid;
    //endregion

    //region Getters/Setters
    /**
     * Gets the default amount of ammo in a magazine for the weapon.
     */
    public int getDefaultAmmoInMagazine() {
        return isPackAPunched ? defaultPackAPunchedAmmoInMagazine : defaultAmmoInMagazine;
    }

    /**
     * Gets the default total ammmo amount for the weapon.
     */
    public int getDefaultAvailableAmmo() {
        return isPackAPunched ? defaultAvailableAmmo : defaultPackAPunchedAvailableAmmo;
    }

    /**
     * Gets the sound emitted on gunshot.
     */
    public Sound getGunshotSound() {
        return isPackAPunched ? packAPunchedGunshotSound : gunshotSound;
    }

    /**
     * Gets the sound emitted on melee.
     */
    public Sound getMeleeSound() {
        return isPackAPunched ? packAPunchedMeleeSound : meleeSound;
    }

    /**
     * Gets the sound emitted when the player is out of ammo.
     */
    public Sound getOutOfAmmoSound() {
        return isPackAPunched ? packAPunchedOutOfAmmoSound : outOfAmmoSound;
    }

    /**
     * Gets the projectile count.
     */
    public int getProjectileCount() {
        return isPackAPunched ? packAPunchedProjectileCount : projectileCount;
    }

    /**
     * Gets the speed at which the weapon reloads in ticks.
     */
    public int getReloadSpeed() {
        return isPackAPunched ? packAPunchedReloadSpeed : reloadSpeed;
    }

    /**
     * Gets the weapon.
     */
    public WeaponEntity getWeapon() {
        return weapon;
    }
    //endregion

    //region Event Handlers
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity))
            return;

        player = (Player)event.getDamager();
        LivingEntity livingEntity = (LivingEntity)event.getEntity();

        if (fetchItemStack() == null)
            return;

        if (meleeService == null) {
            event.setCancelled(true);
            return;
        }

        GameplayEnemy enemy = null;

        for (GameplayEnemy queryEnemy : ProjectileHitListener.getGameplayEnemies()) {

            if (queryEnemy.getEntity().getUniqueId().equals(livingEntity.getUniqueId())) {
                enemy = queryEnemy;
                break;
            }
        }

        if (enemy == null)
            return;

        boolean isWithinRangeX = isWithinRange(player.getLocation().getX(), livingEntity.getLocation().getX(), 3);
        boolean isWithinRangeY = isWithinRange(player.getLocation().getY(), livingEntity.getLocation().getY(), 3);
        boolean isWithinRangeZ = isWithinRange(player.getLocation().getZ(), livingEntity.getLocation().getZ(), 3);

        if (!isWithinRangeX || !isWithinRangeY || !isWithinRangeZ) {
            event.setCancelled(true);
            return;
        }

        livingEntity.setHealth(20);
        enemy.damage(isPackAPunched ? packAPunchedMeleeService.getDamage() : meleeService.getDamage());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        player = event.getPlayer();
        ItemStack itemStack = fetchItemStack();

        if (itemStack == null)
            return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            shoot(event.getPlayer());
            return;
        }

        if (canMelee()) {
            melee(event.getPlayer());
            return;
        }

        reload(event.getPlayer());
        event.setCancelled(true);
    }
    //endregion

    //region Public Methods
    /**
     * Gets whether the weapon can be used to melee.
     */
    public boolean canMelee() {
        return meleeService != null;
    }

    /**
     * Gets whether the weapon can be Pack-A-Punched.
     */
    public boolean canPackAPunch() {

        if (isPackAPunched)
            return false;

        if (gunshotService != null && packAPunchedGunshotService != null)
            return true;

        return meleeService != null && packAPunchedMeleeService != null;
    }

    /**
     * Gets whether the weapon can be shot.
     */
    public boolean canShoot() {

        if (gunshotService == null || isReloading)
            return false;

        return ammoInMagazine > 0;
    }

    /**
     * Melees the weapon.
     */
    public void melee(Player player) {
        playMeleeSound(player);
    }

    /**
     * Plays the gunshot sound for the current weapon state.
     */
    public void playGunshotSound(Player player) {
        playSound(player, getGunshotSound(), 4);
    }

    /**
     * Plays the melee sound for the current weapon state.
     */
    public void playMeleeSound(Player player) {
        playSound(player, getMeleeSound(), 1);
    }

    /**
     * Plays the out of ammo sound for the current weapon state.
     */
    public void playOutOfAmmoSound(Player player) {
        playSound(player, getOutOfAmmoSound(), 1);
    }

    /**
     * Reloads this weapon.
     */
    public void reload(Player player) {

        if (gunshotService == null || isReloading || ammoInMagazine == getDefaultAmmoInMagazine())
            return;

        if (availableAmmo < 1) {

            if (ammoInMagazine < 1) {
                setWeaponDisplay(getReloadingNoAmmoStatus());
                return;
            }

            return;
        }

        isReloading = true;

        remindOfNoAmmo(player);

        Main instance = Main.getInstance();

        instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, this::reload_runnable, Time.fromSeconds(getReloadSpeed()).totalTicks());
    }

    /**
     * Shoots the weapon, if a gunshot service is available.
     */
    public void shoot(Player player) {

        if (gunshotService == null)
            return;

        if (isReloading) {
            playOutOfAmmoSound(player);
            return;
        }

        if (!canShoot()) {
            remindOfNoAmmo(player);
            return;
        }

        playGunshotSound(player);

        double accuracy;
        int damage;
        int projectiles = getProjectileCount();

        if (isPackAPunched && packAPunchedGunshotService != null) {

            accuracy = packAPunchedGunshotService.getAccuracy();
            damage = packAPunchedGunshotService.getDamage();

        } else {

            accuracy = gunshotService.getAccuracy();
            damage = gunshotService.getDamage();

        }

        // The accuracy setting is actually an inaccuracy setting so
        // it needs to be inverted before we use it on a projectile.
        accuracy = 1 - (accuracy / 100.0);

        for (int i = 0; i < projectiles; i++) {

            Snowball snowball = player.launchProjectile(Snowball.class);

            Vector vector = player.getLocation().getDirection().multiply(2.5);
            vector.add(new Vector(Math.random() * accuracy - accuracy, Math.random() * accuracy - accuracy, Math.random() * accuracy - accuracy));

            snowball.setVelocity(vector);
            snowball.setGravity(false);

            snowball.setCustomName("zombies:bullet:" + damage);
        }

        ammoInMagazine--;
        setWeaponDisplay(getAmmoStatus());

        if (ammoInMagazine < 1)
            reload(player);
    }
    //endregion

    //region Runnables
    private void reload_runnable() {

        int defaultAmmoInMagazine = getDefaultAmmoInMagazine();

        if (availableAmmo > defaultAmmoInMagazine) {

            availableAmmo -= defaultAmmoInMagazine - ammoInMagazine;
            ammoInMagazine = defaultAmmoInMagazine;

        } else {

            while (ammoInMagazine < defaultAmmoInMagazine && availableAmmo > 0) {
                ammoInMagazine++;
                availableAmmo--;
            }
        }

        isReloading = false;

        setWeaponDisplay(getAmmoStatus());
    }
    //endregion

    //region Constructors
    public GameplayWeapon(Player player, WeaponEntity weapon, UUID uuid) {

        this.player = player;
        this.weapon = weapon;
        this.uuid = uuid;

        findServices();

        breakdownGunshotService();
        breakdownPackAPunchedGunshotService();

        if (gunshotService != null) {
            setWeaponDisplay(getAmmoStatus());
            return;
        }

        setWeaponDisplay();
    }
    //endregion

    //region Private Methods
    private void breakdownGunshotService() {

        if (gunshotService == null)
            return;

        for (WeaponServiceCharacteristicEntity characteristic : gunshotService.getCharacteristics()) {

            if (characteristic.getType().equals(WeaponCharacteristic.PROJECTILES_IN_CARTRIDGE)) {
                projectileCount = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.RELOAD_SPEED)) {
                reloadSpeed = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.MAGAZINE_SIZE)) {
                ammoInMagazine = (Integer)characteristic.getValue();
                defaultAmmoInMagazine = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.OUT_OF_AMMO_SOUND)) {
                outOfAmmoSound = Sound.valueOf((String)characteristic.getValue());
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.TOTAL_AMMO_CAPACITY)) {
                availableAmmo = (Integer)characteristic.getValue();
                defaultAvailableAmmo = (Integer)characteristic.getValue();
            }
        }
    }

    private void breakdownPackAPunchedGunshotService() {

        if (packAPunchedGunshotService == null)
            return;

        for (WeaponServiceCharacteristicEntity characteristic : packAPunchedGunshotService.getCharacteristics()) {

            if (characteristic.getType().equals(WeaponCharacteristic.PROJECTILES_IN_CARTRIDGE)) {
                packAPunchedProjectileCount = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.RELOAD_SPEED)) {
                packAPunchedReloadSpeed = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.MAGAZINE_SIZE)) {
                defaultPackAPunchedAmmoInMagazine = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.OUT_OF_AMMO_SOUND)) {
                packAPunchedOutOfAmmoSound = Sound.valueOf((String)characteristic.getValue());
                continue;
            }

            if (characteristic.getType().equals(WeaponCharacteristic.TOTAL_AMMO_CAPACITY))
                defaultPackAPunchedAvailableAmmo = (Integer)characteristic.getValue();
        }
    }

    private UUID extractUuidFromItemStack(ItemStack itemStack) {

        if (itemStack == null
                || itemStack.getItemMeta() == null
                || itemStack.getItemMeta().getLore() == null
                || itemStack.getItemMeta().getLore().size() < 1
                || itemStack.getItemMeta().getLore().get(0) == null)
            return null;

        try{
            return UUID.fromString(HiddenStringUtils.extractHiddenString(itemStack.getItemMeta().getLore().get(0)));
        } catch (Exception e) {
            return null;
        }
    }

    private void findServices() {

        for (WeaponServiceEntity service : weapon.getServices()) {

            if (service.getType().equals(WeaponService.GUNSHOT)) {

                if (service.doesRequirePackAPunch())
                    packAPunchedGunshotService = service;
                else
                    gunshotService = service;
            }

            if (service.getType().equals(WeaponService.MELEE)) {

                if (service.doesRequirePackAPunch())
                    packAPunchedMeleeService = service;
                else
                    meleeService = service;
            }
        }
    }

    private String getAmmoStatus() {
        return PluginCore.getConfig().getAmmoIndicatorColor().toString() + ammoInMagazine + "/" + availableAmmo;
    }

    private ItemStack fetchItemStack() {

        if (player == null)
            return null;

        for (int i = 0; i < player.getInventory().getSize(); i++) {

            ItemStack itemStack = player.getInventory().getItem(i);

            if (itemStack != null && extractUuidFromItemStack(itemStack).equals(uuid))
                return itemStack;
        }

        return null;
    }

    private String getReloadingNoAmmoStatus() {

        if (isReloading)
            return PluginCore.getConfig().getReloadingIndicatorColor() + "Reloading";

        return PluginCore.getConfig().getOutOfAmmoIndicatorColor() + "No ammo";
    }

    private boolean isWithinRange(double positionOne, double positionTwo, double max) {
        return Math.max(positionOne, positionTwo) - Math.min(positionOne, positionTwo) < max;
    }

    private void playSound(Player player, Sound sound, float volume) {

        World world = Main.getInstance().getServer().getWorld(PluginCore.getConfig().getWorldName());

        if (world == null) {
            player.playSound(player.getLocation(), sound, volume, 1);
            return;
        }

        world.playSound(player.getLocation(), sound, volume, 1);
    }

    private void remindOfNoAmmo(Player player) {

        setWeaponDisplay(getReloadingNoAmmoStatus());

        playOutOfAmmoSound(player);
    }

    private void setWeaponDisplay() {

        ItemStack itemStack = fetchItemStack();

        if (itemStack == null)
            return;

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null)
            return;

        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.valueOf(weapon.getWeaponClass().getColor()) + weapon.getName());
        itemStack.setItemMeta(itemMeta);
    }

    private void setWeaponDisplay(String status) {

        ItemStack itemStack = fetchItemStack();;

        if (itemStack == null)
            return;

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null)
            return;

        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.valueOf(weapon.getWeaponClass().getColor()) + weapon.getName() + ": " + ChatColor.RESET + "" + status);
        itemStack.setItemMeta(itemMeta);
    }
    //endregion
}
