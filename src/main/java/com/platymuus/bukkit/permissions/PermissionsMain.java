package com.platymuus.bukkit.permissions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.ConfigurationNode;

/**
 * Main class for PermissionsBukkit.
 */
public class PermissionsMain extends JavaPlugin {
    
    private BlockListener blockListener = new BlockListener(this);
    private PlayerListener playerListener = new PlayerListener(this);
    private PermissionsCommand commandExecutor = new PermissionsCommand(this);
    
    private HashMap<Player, PermissionAttachment> permissions = new HashMap<Player, PermissionAttachment>();

    @Override
    public void onEnable() {
        // Write some default configuration
        if (!new File(getDataFolder(), "config.yml").exists()) {
            writeDefaultConfiguration();
        }
        
        // Commands
        getCommand("permissions").setExecutor(commandExecutor); 
        
        // Events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_KICK, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Normal, this);
        
        // Register everyone online right now
        for (Player p : getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }
        
        // How are you gentlemen
        getServer().getLogger().info(getDescription().getFullName() + " is now enabled");
    }

    @Override
    public void onDisable() {
        // Unregister everyone
        for (Player p : getServer().getOnlinePlayers()) {
            unregisterPlayer(p);
        }
        
        // Good day to you! I said good day!
        getServer().getLogger().info(getDescription().getFullName() + " is now disabled");
    }
    
    public void registerPlayer(Player player) {
        PermissionAttachment attachment = player.addAttachment(this);
        permissions.put(player, attachment);
        
        for (String permission : calculatePlayerPermissions(player.getName())) {
            if (permission.startsWith("-")) {
                attachment.setPermission(permission.substring(1), false);
            } else {
                attachment.setPermission(permission, true);
            }
        }
    }

    public void unregisterPlayer(Player player) {
        player.removeAttachment(permissions.get(player));
        permissions.remove(player);
    }

    void refreshPermissions() {
        getConfiguration().save();
        for (Player player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }
            
            for (String permission : calculatePlayerPermissions(player.getName())) {
                if (permission.startsWith("-")) {
                    attachment.setPermission(permission.substring(1), false);
                } else {
                    attachment.setPermission(permission, true);
                }
            }
            
            player.recalculatePermissions();
        }
    }
    
    private List<String> calculatePlayerPermissions(String player) {
        ConfigurationNode node = getConfiguration().getNode("users." + player);
        if (node == null) {
            return calculateGroupPermissions("default");
        }
        
        List<String> perms = node.getStringList("permissions", new ArrayList<String>());
        
        for (String group : node.getStringList("groups", new ArrayList<String>())) {
            for (String permission : calculateGroupPermissions(group)) {
                if (!perms.contains(permission)) {
                    perms.add(permission);
                }
            }
        }
        
        return perms;
    }
    
    private List<String> calculateGroupPermissions(String group) {
        ConfigurationNode node = getConfiguration().getNode("groups." + group);
        if (node == null) {
            return new ArrayList<String>();
        }
        
        List<String> perms = node.getStringList("permissions", new ArrayList<String>());
        
        for (String parent : node.getStringList("inherits", new ArrayList<String>())) {
            for (String permission : calculateGroupPermissions(parent)) {
                if (!perms.contains(permission)) {
                    perms.add(permission);
                }
            }
        }
        
        return perms;
    }

    private void writeDefaultConfiguration() {
        HashMap<String, Object> users = new HashMap<String, Object>();
        HashMap<String, Object> user = new HashMap<String, Object>();
        ArrayList<String> user_permissions = new ArrayList<String>();
        ArrayList<String> user_groups = new ArrayList<String>();
        
        HashMap<String, Object> groups = new HashMap<String, Object>();
        HashMap<String, Object> group_default = new HashMap<String, Object>();
        ArrayList<String> group_default_permissions = new ArrayList<String>();
        HashMap<String, Object> group_admin = new HashMap<String, Object>();
        ArrayList<String> group_admin_inherits = new ArrayList<String>();
        ArrayList<String> group_admin_permissions = new ArrayList<String>();
        
        user_permissions.add("permissions.example");
        user_groups.add("admin");
        user.put("permissions", user_permissions);
        user.put("groups", user_groups);
        users.put("ConspiracyWizard", user);
        
        group_default_permissions.add("permissions.build");
        group_default.put("permissions", group_default_permissions);
        group_admin_inherits.add("default");
        group_admin_permissions.add("permissions.*");
        group_admin.put("inherits", group_admin_inherits);
        group_admin.put("permissions", group_admin_permissions);
        groups.put("default", group_default);
        groups.put("admin", group_admin);
        
        getConfiguration().setProperty("users", users);
        getConfiguration().setProperty("groups", groups);
        
        getConfiguration().setHeader(
            "# PermissionsBukkit configuration file",
            "# ",
            "# A permission node is a string like 'permissions.build', usually starting",
            "# with the name of the plugin. Refer to a plugin's documentation for what",
            "# permissions it cares about. Permission nodes may also be specified here",
            "# starting with a minus (-), meaning the user or group will NOT have that",
            "# permission. Some plugins provide permission nodes that map to a group of",
            "# permissions - for example, PermissionsBukkit has 'permissions.*', which",
            "# automatically grants all admin permissions.",
            "# ",
            "# Users inherit permissions from the groups they are a part of. If a user is",
            "# not specified here, or does not have a 'groups' node, they will be in the",
            "# group 'default'. Permissions for individual users may also be specified by",
            "# using a 'permissions' node with a list of permission nodes, which will",
            "# override their group permissions.",
            "# ",
            "# Groups can be assigned to players and all their permissions will also be",
            "# assigned to those players. Groups can also inherit permissions from other",
            "# groups. Like user permissions, groups may override the permissions of their",
            "# parent group(s). Unlike users, groups do NOT automatically inherit from",
            "# default."
        );
        getConfiguration().save();
    }
    
}
