package com.platymuus.bukkit.permissions;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

class PermissionsRanker implements CommandExecutor {
	private PermissionsPlugin plugin;
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