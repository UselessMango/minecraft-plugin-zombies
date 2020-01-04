package com.mango.zombies.commands;

import com.mango.zombies.base.PlayerOnlyCommandExecutor;
import com.mango.zombies.gameplay.GameplayWeapon;
import com.mango.zombies.services.GameplayService;
import com.mango.zombies.services.MessagingService;
import net.minecraft.server.v1_14_R1.Tuple;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GetWeaponCommandExecutor extends PlayerOnlyCommandExecutor {

	//region Constant Values
	public static final String CORRECT_USAGE_ERROR = "Correct usage: /getweapon [weapon ID]";
	//endregion

	//region Event Handlers
	@Override
	public boolean onSuccessfulCommand(Player player, Command command, String label, String[] args) {

		if (args.length != 1) {
			MessagingService.showError(player, CORRECT_USAGE_ERROR);
			return true;
		}

		Tuple<ItemStack, GameplayWeapon> weapon = GameplayService.giveWeapon(player, args[0]);

		if (weapon == null)
			return true;

		player.getInventory().addItem(weapon.a());
		MessagingService.showSuccess(player, weapon.b().getWeapon().getName() + " added to inventory.");

		return true;
	}
	//endregion
}