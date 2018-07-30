package com.mango.zombies.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mango.zombies.Main;
import com.mango.zombies.assets.MapInterface;
import com.mango.zombies.entities.MapEntity;

public class EditMapDescriptionCommand implements CommandExecutor
{
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		// check if the command was not run by a player
		if (!(sender instanceof Player))
		{
			// let the sender know that they aren't able to run this command
			sender.sendMessage("[Zombies] This command can only be run by a player");
		}
		// the command was run by a player
		else
		{
			// check if 2 arguments were supplied
			if (args.length >= 2)
			{
				// define boolean for this check
				boolean doesExist = false;
				
				// check if the map is already present in the list
				for (MapEntity map : Main.mapList)
				{
					// invalidate the bool
					doesExist = false;
					
					// check if the map name is in the list of maps
					if (map.getName().equals(args[0]))
					{
						// validate the check boolean
						doesExist = true;
					}
					
					// check if the map exists
					if (doesExist)
					{
						String description = "";
						
						for (int i = 1; i < args.length; i++)
						{
							description = description + " " + args[i];
							description = description.trim();
						}
						
						// set the description
						map.setDescription(description);
						
						// re-write the map file
						boolean editSuccess = MapInterface.WriteMapFile(map);
						
						// check if the edit was successful
						if (editSuccess)
						{
							// notify the sender
							sender.sendMessage(ChatColor.GREEN
								+ "Set description of: "
								+ ChatColor.BOLD
								+ args[0]
								+ ChatColor.RESET
								+ ChatColor.GREEN
								+ " to: "
								+ ChatColor.ITALIC
								+ description);
						}
						// the edit failed
						else
						{
							// notify the sender
							sender.sendMessage(ChatColor.RED + "Could not save map edits");
						}
						
						break;
					}
				}
				
				// check if the map does not exist
				if (!doesExist)
				{
					// notify the sender
					sender.sendMessage(ChatColor.RED + "Map: " + args[0] + " does not exist");
				}
			}
			// not enough or too many arguments were supplied
			else
			{
				// notify the sender
				sender.sendMessage(ChatColor.RED + "Correct usage: /z_createmap <map name> <description>");
			}
		}
		
		return true;
	}
}