package com.platymuus.bukkit.permissions;

import java.util.Arrays;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class PermissionsRanker implements CommandExecutor {
	private PermissionsPlugin plugin;
	public static Logger log = Logger.getLogger("Minecraft");
	public String noPermission = ChatColor.RED
			+ "You don't have Permission to use this!";

	public PermissionsRanker(PermissionsPlugin plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String cmdLine,
			String[] split) {
		try {
			if (sender.hasPermission("permissions.rank." + split[1])) {
				String player = split[0].toLowerCase();
				Player target = plugin.getServer().getPlayer(split[0]);
				String PlayerName = sender.getName();
				String carget = plugin.getServer().getPlayer(split[0]).getDisplayName();
				String[] groups = split[1].split(",");	
				if (plugin.getNode("users/" + player) == null) {
					createPlayerNode(player);
				}						
				plugin.getNode("users/" + player).set("groups",
						Arrays.asList(groups));
				plugin.refreshPermissions();
				sender.sendMessage(ChatColor.GREEN + "You've changed " + carget
						+ "'s rank to " + split[1] + ".");			
				target.sendMessage(ChatColor.GREEN + "[PB]" +ChatColor.AQUA + sender.getServer().getPlayer(PlayerName).getDisplayName() + " changed your rank to " +
					  split[1] + "!");
				log.info("[Permissions] " + sender.getServer().getPlayer(PlayerName).getDisplayName() 
						+ " changed" + carget + "'s to " + split[1] + ".");
				return true;
			} else {
				sender.sendMessage(this.noPermission);
				return true;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			sender.sendMessage(ChatColor.RED
					+ "Wrong syntax! Usage: /rank [Player] [Rank]");
			return true;
		} catch (NullPointerException e) {
			sender.sendMessage(ChatColor.RED
					+ "That player is not online!");
			return true;
		}
	}

	private void createPlayerNode(String player) {
		plugin.getNode("users").createSection(player);
		plugin.getNode("users/" + player).set("groups",
				Arrays.asList("default"));
	}

	@SuppressWarnings("unused")
	private void createPlayerNode(String player, String subnode) {
		plugin.getConfig().createSection("users/" + player + "/" + subnode);
	}
}