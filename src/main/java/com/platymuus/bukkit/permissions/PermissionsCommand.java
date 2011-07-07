package com.platymuus.bukkit.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.util.config.ConfigurationNode;

/**
 * CommandExecutor for /permissions
 */
class PermissionsCommand implements CommandExecutor {
    
    private PermissionsMain plugin;

    public PermissionsCommand(PermissionsMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
        if (split.length < 1) {
            if (!checkPerm(sender, "help")) return true;
            return usage(sender, command);
        }
        
        String subcommand = split[0];
        if (subcommand.equals("reload")) {
            if (!checkPerm(sender, "reload")) return true;
            plugin.getConfiguration().load();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
            return true;
        } if (subcommand.equals("check")) {
            if (!checkPerm(sender, "check")) return true;
            if (split.length != 3) return usage(sender, command, subcommand);
            
            Player player = plugin.getServer().getPlayer(split[1]);
            String node = split[2];
            
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + split[1] + ChatColor.RED + " not found.");
            } else {
                String has = player.hasPermission(node) ? ChatColor.AQUA + "has" : ChatColor.LIGHT_PURPLE + "does not have";
                sender.sendMessage(ChatColor.GREEN + "Player " + player.getName() + " " + has + ChatColor.GREEN + " " + split[2]);
            }
            return true;
        } else if (subcommand.equals("info")) {
            if (!checkPerm(sender, "info")) return true;
            if (split.length != 2) return usage(sender, command, subcommand);
            
            String node = split[1];
            Permission perm = plugin.getServer().getPluginManager().getPermission(node);
            
            if (perm == null) {
                sender.sendMessage(ChatColor.RED + "Permission " + ChatColor.WHITE + node + ChatColor.RED + " not found.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Info on permission " + ChatColor.WHITE + perm.getName() + ChatColor.GREEN + ":");
                sender.sendMessage(ChatColor.GREEN + "Default: " + ChatColor.WHITE + perm.getDefault());
                if (perm.getDescription() != null && perm.getDescription().length() > 0) {
                    sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.WHITE + perm.getDescription());
                }
                if (perm.getChildren() != null && perm.getChildren().size() > 0) {
                    sender.sendMessage(ChatColor.GREEN + "Children: " + ChatColor.WHITE + perm.getChildren().size());
                }
            }
            return true;
        } else if (subcommand.equals("dump")) {
            if (!checkPerm(sender, "dump")) return true;
            if (split.length != 2) return usage(sender, command, subcommand);
            
            Player player = plugin.getServer().getPlayer(split[1]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + split[1] + ChatColor.RED + " not found.");
            } else {
                sender.sendMessage(ChatColor.RED + "Feature coming soon");
            }
            return true;
        } else if (subcommand.equals("group")) {
            if (split.length < 2) {
                if (!checkPerm(sender, "group.help")) return true;
                return usage(sender, command, subcommand);
            }
            groupCommand(sender, command, split);
            return true;
        } else if (subcommand.equals("player")) {
            if (split.length < 2) {
                if (!checkPerm(sender, "player.help")) return true;
                return usage(sender, command, subcommand);
            }
            playerCommand(sender, command, split);
            return true;
        } else {
            if (!checkPerm(sender, "help")) return true;
            return usage(sender, command);
        }
    }

    private boolean groupCommand(CommandSender sender, Command command, String[] split) {
        ConfigurationNode groupNode = plugin.getConfiguration().getNode("groups");
        ConfigurationNode userNode = plugin.getConfiguration().getNode("users");
        
        String subcommand = split[1];
        if (subcommand.equals("list")) {
            if (!checkPerm(sender, "group.list")) return true;
            if (split.length != 2) return usage(sender, command, "group list");
            
            String result = "", sep = "";
            for (String key : groupNode.getKeys()) {
                result += sep + key;
                sep = ", ";
            }
            sender.sendMessage(ChatColor.GREEN + "Groups: " + ChatColor.WHITE + result);
            return true;
        } else if (subcommand.equals("players")) {
            if (!checkPerm(sender, "group.players")) return true;
            if (split.length != 3) return usage(sender, command, "group players");
            String group = split[2];
            
            if (groupNode.getNode(group) == null) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }
            
            int count = 0;
            String text = "", sep = "";
            for (String user : userNode.getKeys()) {
                if (userNode.getStringList(user + ".groups", new ArrayList<String>()).contains(group)) {
                    ++count;
                    text += sep + user;
                    sep = ", ";
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Users in " + ChatColor.WHITE + group + ChatColor.GREEN + " (" + ChatColor.WHITE + count + ChatColor.GREEN + "): " + ChatColor.WHITE + text);
            return true;
        } else if (subcommand.equals("addperm")) {
            if (!checkPerm(sender, "group.addperm")) return true;
            if (split.length != 4) return usage(sender, command, "group addperm");
            String group = split[2];
            String perm = split[3];
            
            if (groupNode.getNode(group) == null) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }
            
            List<String> list = groupNode.getNode(group).getStringList("permissions", new ArrayList<String>());
            if (list.contains(perm)) {
                sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " already has " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
                return true;
            }
            list.add(perm);
            groupNode.getNode(group).setProperty("permissions", list);
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " now has " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("removeperm")) {
            if (!checkPerm(sender, "group.removeperm")) return true;
            if (split.length != 4) return usage(sender, command, "group removeperm");
            String group = split[2];
            String perm = split[3];
            
            if (groupNode.getNode(group) == null) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }
            
            List<String> list = groupNode.getNode(group).getStringList("permissions", new ArrayList<String>());
            if (!list.contains(perm)) {
                sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " does not have " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
                return true;
            }
            list.remove(perm);
            groupNode.getNode(group).setProperty("permissions", list);
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " no longer has " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
            return true;
        } else {
            if (!checkPerm(sender, "group.help")) return true;
            return usage(sender, command);
        }
    }

    private boolean playerCommand(CommandSender sender, Command command, String[] split) {
        ConfigurationNode groupNode = plugin.getConfiguration().getNode("groups");
        ConfigurationNode userNode = plugin.getConfiguration().getNode("users");
        
        String subcommand = split[1];
        if (subcommand.equals("groups")) {
            if (!checkPerm(sender, "player.groups")) return true;
            if (split.length != 3) return usage(sender, command, "player groups");
            String player = split[2];
            
            if (userNode.getNode(player) == null) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.RED + " is in the default group.");
                return true;
            }
            
            int count = 0;
            String text = "", sep = "";
            for (String group : userNode.getNode(player).getStringList("groups", new ArrayList<String>())) {
                ++count;
                text += sep + group;
                sep = ", ";
            }
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is in groups (" + ChatColor.WHITE + count + ChatColor.GREEN + "): " + ChatColor.WHITE + text);
            return true;
        } else if (subcommand.equals("addgroup")) {
            if (!checkPerm(sender, "player.addgroup")) return true;
            if (split.length != 4) return usage(sender, command, "player addgroup");
            String player = split[2];
            String group = split[3];
            
            if (userNode.getNode(player) == null) {
                createPlayerNode(player);
            }
            
            List<String> list = userNode.getNode(player).getStringList("group", new ArrayList<String>());
            if (list.contains(group)) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " was already in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
                return true;
            }
            list.add(group);
            userNode.getNode(player).setProperty("group", list);
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is now in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("removegroup")) {
            if (!checkPerm(sender, "player.removeperm")) return true;
            if (split.length != 4) return usage(sender, command, "player removeperm");
            String player = split[2];
            String group = split[3];
            
            if (userNode.getNode(player) == null) {
                createPlayerNode(player);
            }
            
            List<String> list = userNode.getNode(player).getStringList("groups", new ArrayList<String>());
            if (!list.contains(group)) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " was not in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
                return true;
            }
            list.remove(group);
            userNode.getNode(player).setProperty("groups", list);
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is no longer in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("addperm")) {
            if (!checkPerm(sender, "player.addperm")) return true;
            if (split.length != 4) return usage(sender, command, "player addperm");
            String player = split[2];
            String perm = split[3];
            
            if (userNode.getNode(player) == null) {
                createPlayerNode(player);
            }
            
            List<String> list = userNode.getNode(player).getStringList("permissions", new ArrayList<String>());
            if (list.contains(perm)) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " already had " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
                return true;
            }
            list.add(perm);
            userNode.getNode(player).setProperty("permissions", list);
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " now has " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("removeperm")) {
            if (!checkPerm(sender, "player.removeperm")) return true;
            if (split.length != 4) return usage(sender, command, "player removeperm");
            String player = split[2];
            String perm = split[3];
            
            if (userNode.getNode(player) == null) {
                createPlayerNode(player);
            }
            
            List<String> list = userNode.getNode(player).getStringList("permissions", new ArrayList<String>());
            if (!list.contains(perm)) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " did not have " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
                return true;
            }
            list.remove(perm);
            userNode.getNode(player).setProperty("permissions", list);
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " no longer has " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
            return true;
        } else {
            if (!checkPerm(sender, "player.help")) return true;
            return usage(sender, command);
        }
    }

    private void createPlayerNode(String player) {
        ArrayList<String> permissions = new ArrayList<String>();
        ArrayList<String> groups = new ArrayList<String>();
        groups.add("default");
        HashMap<String, Object> user = new HashMap<String, Object>();
        user.put("permissions", permissions);
        user.put("groups", groups);
        plugin.getConfiguration().getNode("users").setProperty(player, user);
    }
    
    // -- utilities --
    
    private boolean checkPerm(CommandSender sender, String subnode) {
        if (sender instanceof Player) {
            boolean ok = ((Player) sender).hasPermission("permissions." + subnode);
            if (!ok) {
                sender.sendMessage(ChatColor.RED + "You do not have permissions to do that.");
            }
            return ok;
        }
        return true;
    }
    
    private boolean usage(CommandSender sender, Command command) {
        sender.sendMessage(ChatColor.RED + "[====" + ChatColor.GREEN + " /permissons " + ChatColor.RED + "====]");
        for (String line : command.getUsage().split("\\n")) {
            if ((line.startsWith("/<command> group") && !line.startsWith("/<command> group -")) ||
                (line.startsWith("/<command> player") && !line.startsWith("/<command> player -"))) {
                continue;
            }
            sender.sendMessage(formatLine(line));
        }
        return true;
    }
    
    private boolean usage(CommandSender sender, Command command, String subcommand) {
        sender.sendMessage(ChatColor.RED + "[====" + ChatColor.GREEN + " /permissons " + subcommand + " " + ChatColor.RED + "====]");
        for (String line : command.getUsage().split("\\n")) {
            if (line.startsWith("/<command> " + subcommand)) {
                sender.sendMessage(formatLine(line));
            }
        }
        return true;
    }
    
    private String formatLine(String line) {
        int i = line.indexOf(" - ");
        String usage = line.substring(0, i);
        String desc = line.substring(i + 3);

        usage = usage.replace("<command>", "permissions");
        usage = usage.replaceAll("\\[[^]]+\\]", ChatColor.AQUA + "$0" + ChatColor.GREEN);
        usage = usage.replaceAll("<[^>]+>", ChatColor.LIGHT_PURPLE + "$0" + ChatColor.GREEN);

        return ChatColor.GREEN + usage + " - " + ChatColor.WHITE + desc;
    }
    
}
