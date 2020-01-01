package com.mango.zombies.gameplay;

import com.mango.zombies.Main;
import com.mango.zombies.entities.WeaponClassEntity;
import com.mango.zombies.entities.WeaponEntity;
import com.mango.zombies.entities.WeaponServiceCharacteristicEntity;
import com.mango.zombies.entities.WeaponServiceEntity;
import com.mango.zombies.schema.WeaponCharacteristic;
import com.mango.zombies.schema.WeaponService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class GameplayWeapon {

    //region Fields
    private int ammoInMagazine = WeaponClassEntity.DEFAULT_MAGAZINE_SIZE, availableAmmo = WeaponClassEntity.DEFAULT_TOTAL_AMMO_CAPACITY;
    private int projectileCount = WeaponClassEntity.DEFAULT_PROJECTILE_COUNT, packAPunchedProjectileCount = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_PROJECTILE_COUNT;
    private int reloadSpeed = WeaponClassEntity.DEFAULT_RELOAD_SPEED, packAPunchedReloadSpeed = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_RELOAD_SPEED;

    private int defaultAmmoInMagazine = WeaponClassEntity.DEFAULT_MAGAZINE_SIZE, defaultAvailableAmmo = WeaponClassEntity.DEFAULT_TOTAL_AMMO_CAPACITY;
    private int defaultPackAPunchedAmmoInMagazine = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_MAGAZINE_SIZE, defaultPackAPunchedAvailableAmmo = WeaponClassEntity.DEFAULT_PACK_A_PUNCHED_TOTAL_AMMO_CAPACITY;

    private Sound gunshotSound = Sound.valueOf(WeaponClassEntity.DEFAULT_GUNSHOT_USAGE_SOUND), packAPunchedGunshotSound = Sound.valueOf(WeaponClassEntity.DEFAULT_GUNSHOT_USAGE_SOUND);
    private Sound meleeSound = Sound.valueOf(WeaponClassEntity.DEFAULT_MELEE_USAGE_SOUND), packAPunchedMeleeSound = Sound.valueOf(WeaponClassEntity.DEFAULT_MELEE_USAGE_SOUND);
    private Sound outOfAmmoSound = Sound.valueOf(WeaponClassEntity.DEFAULT_OUT_OF_AMMO_SOUND), packAPunchedOutOfAmmoSound = Sound.valueOf(WeaponClassEntity.DEFAULT_OUT_OF_AMMO_SOUND);

    private WeaponServiceEntity gunshotService, packAPunchedGunshotService, meleeService, packAPunchedMeleeService;
    private boolean isPackAPunched, isReloading;
    private WeaponEntity weapon;
    //endregion

    //region Getters/Setters
    /**
     * Gets how much ammo is in the current magazine.
     */
    public int getAmmoInMagazine() {
        return ammoInMagazine;
    }

    /**
     * Sets how much ammo is in the current magazine.
     */
    public void setAmmoInMagazine(int ammoInMagazine) {
        this.ammoInMagazine = ammoInMagazine;
    }

    /**
     * Gets the default amount of ammo in a magazine for the weapon.
     */
    public int getDefaultAmmoInMagazine() {
        return isPackAPunched ?  defaultPackAPunchedAmmoInMagazine : defaultAmmoInMagazine;
    }

    /**
     * Gets the default total ammmo amount for the weapon.
     */
    public int getDefaultAvailableAmmo() {
        return isPackAPunched ? defaultAvailableAmmo : defaultPackAPunchedAvailableAmmo;
    }

    /**
     * Gets how much ammo the weapon has available, not including its current magazine.
     */
    public int getAvailableAmmo() {
        return availableAmmo;
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
     * Gets whether this weapon is Pack-A-Punched.
     */
    public boolean isPackAPunched() {
        return isPackAPunched;
    }

    /**
     * Gets whether the weapon is currently reloading.
     */
    public boolean isReloading() {
        return isReloading;
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
        return packAPunchedGunshotService != null || packAPunchedMeleeService != null;
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
     * Reloads this weapon.
     */
    public void reload(Player player, ItemStack itemStack) {

        if (gunshotService == null || isReloading || ammoInMagazine == getDefaultAmmoInMagazine())
            return;

        if (ammoInMagazine < 1 && availableAmmo < 1) {
            remindOfNoAmmo(player, itemStack, true);
            return;
        }

        isReloading = true;

        remindOfNoAmmo(player, itemStack, true);

        int defaultAmmo = getDefaultAmmoInMagazine();

        Main.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {

            if (availableAmmo > defaultAmmo) {

                ammoInMagazine = defaultAmmo;
                availableAmmo -= defaultAmmo;

            } else {

                ammoInMagazine = availableAmmo;
                availableAmmo = 0;
            }

            isReloading = false;

            setAmmoDisplay(itemStack);

        }, getReloadSpeed() * 20);
    }

    /**
     * Reminds the player that they have no ammo.
     */
    public void remindOfNoAmmo(Player player, ItemStack itemStack, boolean playSound) {

        setReloadingNoAmmoDisplay(itemStack);

        if (playSound)
            playOutOfAmmoSound(player);
    }

    /**
     * Shoots the weapon, if a gunshot service is available.
     */
    public void shoot(Player player, ItemStack itemStack) {

        if (gunshotService == null)
            return;

        if (isReloading) {
            playOutOfAmmoSound(player);
            return;
        }

        if (!canShoot()) {
            remindOfNoAmmo(player, itemStack,true);
            return;
        }

        playGunshotSound(player);

        double multiply = 2.5;
        int projectiles = getProjectileCount();

        for (int i = 0; i < projectiles; i++) {

            double finalMultiply = multiply;

            Snowball snowball = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(finalMultiply));

            multiply *= 0.8;
        }

        ammoInMagazine--;
        setAmmoDisplay(itemStack);

        if (ammoInMagazine < 1 && availableAmmo < 0) {
            remindOfNoAmmo(player, itemStack,false);
            return;
        }

        if (ammoInMagazine > 0)
            return;

        reload(player, itemStack);
        remindOfNoAmmo(player, itemStack,false);
    }
    //endregion

    //region Constructors
    public GameplayWeapon(WeaponEntity weapon) {

        this.weapon = weapon;

        findServices();

        breakdownGunshotService();
        breakdownPackAPunchedGunshotService();

        breakdownMeleeService();
        breakdownPackAPunchedMeleeService();
    }
    //endregion

    //region Private Methods
    private void breakdownGunshotService() {

        if (gunshotService == null)
            return;

        for (WeaponServiceCharacteristicEntity characteristic : gunshotService.getCharacteristics()) {

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.PROJECTILES_IN_CARTRIDGE)) {
                projectileCount = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.RELOAD_SPEED)) {
                reloadSpeed = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.MAGAZINE_SIZE)) {
                ammoInMagazine = (Integer)characteristic.getValue();
                defaultAmmoInMagazine = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.OUT_OF_AMMO_SOUND)) {
                outOfAmmoSound = Sound.valueOf((String)characteristic.getValue());
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.TOTAL_AMMO_CAPACITY)) {
                availableAmmo = (Integer)characteristic.getValue();
                defaultAvailableAmmo = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.USAGE_SOUND))
                gunshotSound = Sound.valueOf((String)characteristic.getValue());
        }
    }

    private void breakdownPackAPunchedGunshotService() {

        if (packAPunchedGunshotService == null)
            return;

        for (WeaponServiceCharacteristicEntity characteristic : packAPunchedGunshotService.getCharacteristics()) {

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.PROJECTILES_IN_CARTRIDGE)) {
                packAPunchedProjectileCount = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.RELOAD_SPEED)) {
                packAPunchedReloadSpeed = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.MAGAZINE_SIZE)) {
                defaultPackAPunchedAmmoInMagazine = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.OUT_OF_AMMO_SOUND)) {
                packAPunchedOutOfAmmoSound = Sound.valueOf((String)characteristic.getValue());
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.TOTAL_AMMO_CAPACITY)) {
                defaultPackAPunchedAvailableAmmo = (Integer)characteristic.getValue();
                continue;
            }

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.USAGE_SOUND))
                packAPunchedGunshotSound = Sound.valueOf((String)characteristic.getValue());
        }
    }

    private void breakdownMeleeService() {

        if (meleeService == null)
            return;

        for (WeaponServiceCharacteristicEntity characteristic : meleeService.getCharacteristics()) {

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.USAGE_SOUND))
                meleeSound = Sound.valueOf((String)characteristic.getValue());
        }
    }

    private void breakdownPackAPunchedMeleeService() {

        if (packAPunchedMeleeService == null)
            return;

        for (WeaponServiceCharacteristicEntity characteristic : packAPunchedMeleeService.getCharacteristics()) {

            if (characteristic.getTypeUUID().equals(WeaponCharacteristic.USAGE_SOUND))
                packAPunchedMeleeSound = Sound.valueOf((String)characteristic.getValue());
        }
    }

    private void findServices() {

        for (WeaponServiceEntity service : weapon.getServices()) {

            if (service.getTypeUUID().equals(WeaponService.GUNSHOT)) {

                if (service.doesRequirePackAPunch())
                    packAPunchedGunshotService = service;
                else
                    gunshotService = service;
            }

            if (service.getTypeUUID().equals(WeaponService.MELEE)) {

                if (service.doesRequirePackAPunch())
                    packAPunchedMeleeService = service;
                else
                    meleeService = service;
            }
        }
    }

    private void playGunshotSound(Player player) {
        player.playSound(player.getLocation(), getGunshotSound(), 10, 1);
    }

    private void playOutOfAmmoSound(Player player) {
        player.playSound(player.getLocation(), getOutOfAmmoSound(), 10, 1);
    }

    private void setAmmoDisplay(ItemStack itemStack) {

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.AQUA + ammoInMagazine + "/" + availableAmmo);
        itemStack.setItemMeta(itemMeta);
    }

    private void setReloadingNoAmmoDisplay(ItemStack itemStack) {

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + (isReloading ? "Reloading" : "No ammo"));
        itemStack.setItemMeta(itemMeta);
    }
    //endregion
}
