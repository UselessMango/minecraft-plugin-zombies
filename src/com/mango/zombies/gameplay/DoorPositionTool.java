package com.mango.zombies.gameplay;

import com.mango.zombies.PluginCore;
import com.mango.zombies.entities.DoorEntity;
import com.mango.zombies.entities.LocationEntity;
import com.mango.zombies.entities.MapEntity;
import com.mango.zombies.gameplay.base.BasePositionTool;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DoorPositionTool extends BasePositionTool {

    //region Fields
    private LocationEntity bottom;
    private LocationEntity top;
    //endregion

    //region Getters/Setters
    /**
     * Gets the name of this position tool.
     */
    public String getPositionToolName() {
        return getMapEntity().getName() + " (door)";
    }

    /**
     * Gets the description on how to use this position tool.
     */
    public String getUsageDescription() {
        return "With the Position Tool, left click to set the top point and right click to set the bottom point. Changes will be applied once both points have been selected.";
    }
    //endregion

    //region Event Handlers
    /**
     * Called when a player left clicks a block.
     * @param player The player who clicked the block.
     * @param block The clicked block.
     * @param itemStack The item stack used to click the block.
     */
    @Override
    public void onPlayerLeftClickBlock(Player player, Block block, ItemStack itemStack) {

        top = new LocationEntity(block.getLocation());
        PluginCore.getMessagingService().success(player, "Top position set to: " + top.toString() + ".");

        saveDoorIfValid(player, itemStack);
    }

    /**
     * Called when a player right clicks a block.
     * @param player The player who clicked the block.
     * @param block The clicked block.
     * @param itemStack The item stack used to click the block.
     */
    @Override
    public void onPlayerRightClickBlock(Player player, Block block, ItemStack itemStack) {

        bottom = new LocationEntity(block.getLocation());
        PluginCore.getMessagingService().success(player, "Bottom position set to: " + bottom.toString() + ".");

        saveDoorIfValid(player, itemStack);
    }
    //endregion

    //region Constructors
    public DoorPositionTool(MapEntity mapEntity) {
        super(mapEntity);
    }
    //endregion

    //region Private Methods
    private void saveDoorIfValid(Player player, ItemStack itemStack) {

        if (top == null || bottom == null)
            return;

        MapEntity mapEntity = getMapEntity();
        DoorEntity door = new DoorEntity(top, bottom);

        mapEntity.addDoor(door);

        player.getInventory().remove(itemStack);

        PluginCore.getMessagingService().success(player, "Door added to " + mapEntity.getName() + ".");
    }
    //endregion
}
