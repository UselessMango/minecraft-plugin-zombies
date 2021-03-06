package com.mango.zombies.gameplay;

import com.mango.zombies.PluginCore;
import com.mango.zombies.entities.EnemyEntity;
import com.mango.zombies.gameplay.base.BaseGameplayRegisterable;
import com.mango.zombies.gameplay.base.BlockBreakEventRegisterable;
import com.mango.zombies.gameplay.base.PlayerInteractEventRegisterable;
import com.mango.zombies.helper.HiddenStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpawningTool extends BaseGameplayRegisterable implements PlayerInteractEventRegisterable, BlockBreakEventRegisterable {

    //region Fields
    private final EnemyEntity enemyEntity;

    private final int spawnRound;

    private final UUID uuid;
    //endregion

    //region Event Handlers
    /**
     * Called when a block breaks.
     * @param event The event caused by the break.
     */
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    /**
     * Called when a player interacts with this weapon.
     * @param event The event caused by the interaction.
     */
    public void onPlayerInteract(PlayerInteractEvent event) {

        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null)
            return;

        GameplayEnemy gameplayEnemy = new GameplayEnemy(enemyEntity);
        gameplayEnemy.setSpawnLocation(clickedBlock.getLocation().add(0, 1, 0));

        gameplayEnemy.setInitialHealth(PluginCore.getGameplayService().calculateHealthForRound(spawnRound, enemyEntity.getRoundMultiplier(), enemyEntity.getMaxHealth()));

        gameplayEnemy.spawn();
    }
    //endregion

    //region Getters/Setters
    /**
     * Gets the UUID of this gameplay registerable.
     */
    public UUID getUUID() {
        return uuid;
    }
    //endregion

    //region Public Methods
    /**
     * Gives a player this weapon as a usable item.
     * @param player The player to give the weapon to.
     */
    public void giveItemStack(Player player) {

        ItemStack itemStack = new ItemStack(PluginCore.getConfig().getSpawningToolItem(), 1);

        ItemMeta itemMeta = itemStack.getItemMeta();

        List<String> lore = new ArrayList<String>();
        lore.add(HiddenStringUtils.encodeString(uuid.toString()));

        itemMeta.setLore(lore);

        itemMeta.setDisplayName(ChatColor.AQUA + "Spawning Tool: " + ChatColor.RESET + "" + ChatColor.GREEN + enemyEntity.getId() + " (Spawn round: " + spawnRound + ")");

        itemStack.setItemMeta(itemMeta);

        player.getInventory().addItem(itemStack);
    }
    //endregion

    //region Constructors
    public SpawningTool(EnemyEntity enemyEntity, int spawnRound) {

        this.enemyEntity = enemyEntity;
        this.spawnRound = spawnRound;

        uuid = UUID.randomUUID();
    }
    //endregion
}
